package com.zhisheng.mvp.inventory;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryMaterialCoreTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        if (tableExists("inventory_transaction")) {
            jdbcTemplate.update("delete from inventory_transaction");
        }
        if (tableExists("inventory_stock")) {
            jdbcTemplate.update("delete from inventory_stock");
        }
        if (tableExists("material_item")) {
            jdbcTemplate.update("delete from material_item");
        }
        if (tableExists("production_work_order_material")) {
            jdbcTemplate.update("delete from production_work_order_material");
        }
        if (tableExists("production_step_instance")) {
            jdbcTemplate.update("delete from production_step_instance");
        }
        if (tableExists("production_route_instance")) {
            jdbcTemplate.update("delete from production_route_instance");
        }
    }

    @Test
    void managesMaterialItemsAndRejectsInvalidValues() throws Exception {
        mockMvc.perform(post("/api/inventory/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialCode", "AL-001",
                                "materialName", "Aluminum plate",
                                "spec", "2mm",
                                "unit", "sheet",
                                "category", "PLATE",
                                "remark", "standard material"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialCode").value("AL-001"))
                .andExpect(jsonPath("$.data.materialName").value("Aluminum plate"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(post("/api/inventory/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialCode", "AL-001",
                                "materialName", "Duplicated code",
                                "unit", "sheet"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("MATERIAL_CODE_DUPLICATED"));

        mockMvc.perform(post("/api/inventory/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("materialCode", "BAD-NAME", "unit", "pcs"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("MATERIAL_NAME_REQUIRED"));

        mockMvc.perform(post("/api/inventory/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("materialCode", "BAD-UNIT", "materialName", "Bolt"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("MATERIAL_UNIT_REQUIRED"));

        long materialId = materialIdByCode("AL-001");
        mockMvc.perform(put("/api/inventory/materials/{materialId}", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialCode", "AL-001",
                                "materialName", "Aluminum plate updated",
                                "spec", "3mm",
                                "unit", "sheet",
                                "category", "UPDATED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialName").value("Aluminum plate updated"))
                .andExpect(jsonPath("$.data.spec").value("3mm"));

        mockMvc.perform(get("/api/inventory/materials")
                        .param("keyword", "Aluminum")
                        .param("enabled", "true")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void recordsManualStockOperationsWithBeforeAndAfterQuantities() throws Exception {
        long materialId = createMaterial("LED-001", "LED module", "pcs");

        mockMvc.perform(post("/api/inventory/transactions/manual-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "10.5000", "first inbound", "IN-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("MANUAL_IN"))
                .andExpect(jsonPath("$.data.beforeOnHandQty").value(0))
                .andExpect(jsonPath("$.data.afterOnHandQty").value(10.5))
                .andExpect(jsonPath("$.data.beforeReservedQty").value(0))
                .andExpect(jsonPath("$.data.afterReservedQty").value(0));

        mockMvc.perform(post("/api/inventory/transactions/manual-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "1", "duplicate submit", "IN-1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVENTORY_IDEMPOTENCY_KEY_DUPLICATED"));

        mockMvc.perform(post("/api/inventory/transactions/manual-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "2.2500", "manual use", "OUT-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("MANUAL_OUT"))
                .andExpect(jsonPath("$.data.beforeOnHandQty").value(10.5))
                .andExpect(jsonPath("$.data.afterOnHandQty").value(8.25));

        mockMvc.perform(post("/api/inventory/transactions/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.ofEntries(
                                entry("materialId", materialId),
                                entry("adjustmentQty", "1.7500"),
                                entry("direction", "IN"),
                                entry("reason", "count up"),
                                entry("idempotencyKey", "ADJ-IN-1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("ADJUST_IN"))
                .andExpect(jsonPath("$.data.afterOnHandQty").value(10.0));

        mockMvc.perform(post("/api/inventory/transactions/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.ofEntries(
                                entry("materialId", materialId),
                                entry("adjustmentQty", "3.0000"),
                                entry("direction", "OUT"),
                                entry("reason", "count down"),
                                entry("idempotencyKey", "ADJ-OUT-1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("ADJUST_OUT"))
                .andExpect(jsonPath("$.data.afterOnHandQty").value(7.0));

        mockMvc.perform(get("/api/inventory/stocks/{materialId}", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialId").value(materialId))
                .andExpect(jsonPath("$.data.onHandQty").value(7.0))
                .andExpect(jsonPath("$.data.reservedQty").value(0))
                .andExpect(jsonPath("$.data.availableQty").value(7.0));

        mockMvc.perform(get("/api/inventory/transactions")
                        .param("materialId", String.valueOf(materialId))
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.items[0].transactionType").value("ADJUST_OUT"));

        assertThat(countRows("inventory_stock")).isEqualTo(1L);
        assertThat(countRows("inventory_transaction")).isEqualTo(4L);
        assertThat(countRows("production_work_order_material")).isZero();
        assertThat(countRows("production_route_instance")).isZero();
    }

    @Test
    void rejectsDisabledMaterialsInvalidQuantityAndInsufficientStock() throws Exception {
        long materialId = createMaterial("STEEL-001", "Steel tube", "m");

        mockMvc.perform(post("/api/inventory/transactions/manual-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "1", "no stock", "OUT-NO-STOCK"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVENTORY_STOCK_NOT_FOUND"));

        mockMvc.perform(post("/api/inventory/transactions/manual-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "0", "zero", "ZERO-QTY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVENTORY_QTY_INVALID"));

        mockMvc.perform(post("/api/inventory/transactions/manual-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "5", "inbound", "STEEL-IN-1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventory/transactions/manual-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "6", "too much", "STEEL-OUT-TOO-MUCH"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVENTORY_INSUFFICIENT_STOCK"));

        mockMvc.perform(post("/api/inventory/materials/{materialId}/disable", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(post("/api/inventory/transactions/manual-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(stockPayload(materialId, "1", "disabled", "DISABLED-IN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("MATERIAL_DISABLED"));

        mockMvc.perform(get("/api/inventory/stocks")
                        .param("keyword", "Steel")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].onHandQty").value(5))
                .andExpect(jsonPath("$.data.items[0].availableQty").value(5));

        assertThat(countRows("inventory_transaction")).isEqualTo(1L);
        assertThat(countRows("production_work_order_material")).isZero();
    }

    private long createMaterial(String code, String name, String unit) throws Exception {
        String response = mockMvc.perform(post("/api/inventory/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialCode", code,
                                "materialName", name,
                                "unit", unit,
                                "spec", "standard"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return data(response).get("id").asLong();
    }

    private Map<String, Object> stockPayload(
            long materialId,
            String qty,
            String reason,
            String idempotencyKey) {
        return Map.ofEntries(
                entry("materialId", materialId),
                entry("qty", qty),
                entry("reason", reason),
                entry("remark", reason + " remark"),
                entry("idempotencyKey", idempotencyKey));
    }

    private long materialIdByCode(String materialCode) {
        return jdbcTemplate.queryForObject(
                "select id from material_item where material_code = ?",
                Long.class,
                materialCode);
    }

    private long countRows(String tableName) {
        if (!tableExists(tableName)) {
            return 0L;
        }
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = lower(?)",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private JsonNode data(String response) throws Exception {
        return objectMapper.readTree(response).get("data");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
