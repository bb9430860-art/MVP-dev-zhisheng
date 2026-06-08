package com.zhisheng.mvp.production;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhisheng.mvp.production.adapter.MockCurrentProductionUserAdapter;
import com.zhisheng.mvp.production.adapter.MockOrderItemAdapter;
import com.zhisheng.mvp.production.port.OrderItemCandidateContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class WorkOrderMaterialReadinessOnCreateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockOrderItemAdapter orderItemAdapter;

    @Autowired
    private MockCurrentProductionUserAdapter currentUserAdapter;

    @BeforeEach
    void cleanDatabase() {
        deleteIfExists("inventory_transaction");
        deleteIfExists("inventory_stock");
        deleteIfExists("material_item");
        deleteIfExists("production_work_order_material");
        deleteIfExists("production_work_order");
        deleteIfExists("production_step_instance");
        deleteIfExists("production_route_instance");
        deleteIfExists("process_step_material_requirement_template");
        deleteIfExists("process_step_template");
        deleteIfExists("process_route_template");
        orderItemAdapter.reset();
        orderItemAdapter.resetProductionWriteBackCount();
        currentUserAdapter.setCurrentUser(201L, 1L, List.of("PRODUCTION_MANAGER"));
    }

    @Test
    void previewCreateReturnsStepGroupedReadinessWithoutWritingWorkOrderOrInventory() throws Exception {
        long readyMaterialId = insertMaterial("READY-MAT", "Ready plate", "sheet");
        long shortageMaterialId = insertMaterial("SHORT-MAT", "Short LED", "pcs");
        long noStockMaterialId = insertMaterial("NO-STOCK-MAT", "No stock bolt", "pcs");
        insertStock(readyMaterialId, "READY-MAT", "Ready plate", "sheet", "10.0000");
        insertStock(shortageMaterialId, "SHORT-MAT", "Short LED", "pcs", "2.0000");
        long routeId = insertRouteTemplate("RT-READINESS-PREVIEW", true);
        long cutStepId = insertStepTemplate(routeId, "STEP-CUT", "Cutting", 1);
        long assemblyStepId = insertStepTemplate(routeId, "STEP-ASSY", "Assembly", 2);
        insertMaterialTemplate(routeId, cutStepId, readyMaterialId, "READY-MAT", "Ready plate", "sheet",
                "CUTTING", "2.0000", "1.0000", "0.0000");
        insertMaterialTemplate(routeId, assemblyStepId, shortageMaterialId, "SHORT-MAT", "Short LED", "pcs",
                "ASSEMBLY", "2.0000", null, "0.0000");
        insertMaterialTemplate(routeId, assemblyStepId, noStockMaterialId, "NO-STOCK-MAT", "No stock bolt", "pcs",
                "ASSEMBLY", null, "1.0000", null);
        insertMaterialTemplate(routeId, assemblyStepId, null, "READY-MAT", "Ready plate text only", "sheet",
                "ASSEMBLY", null, "1.0000", null);
        putOrderItem(6101L, 9101L, "ORD-READINESS-PREVIEW", "Readiness preview product", "3.00");

        BigDecimal readyStockBefore = stockAvailableQty(readyMaterialId);
        long transactionCountBefore = countRows("inventory_transaction");

        mockMvc.perform(post("/api/production/work-orders/material-readiness/preview-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "orderItemId", 6101L,
                                "routeTemplateId", routeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantitySnapshot").value(3.0))
                .andExpect(jsonPath("$.data.itemsByStep.length()").value(2))
                .andExpect(jsonPath("$.data.itemsByStep[0].stepTemplateId").value(cutStepId))
                .andExpect(jsonPath("$.data.itemsByStep[0].stepOrder").value(1))
                .andExpect(jsonPath("$.data.itemsByStep[0].stepName").value("Cutting"))
                .andExpect(jsonPath("$.data.itemsByStep[0].materials[0].requiredQty").value(7.0))
                .andExpect(jsonPath("$.data.itemsByStep[0].materials[0].availableQty").value(10.0))
                .andExpect(jsonPath("$.data.itemsByStep[0].materials[0].shortageQty").value(0))
                .andExpect(jsonPath("$.data.itemsByStep[0].materials[0].readinessStatus").value("READY"))
                .andExpect(jsonPath("$.data.itemsByStep[1].materials[0].readinessStatus").value("SHORTAGE"))
                .andExpect(jsonPath("$.data.itemsByStep[1].materials[0].shortageQty").value(4.0))
                .andExpect(jsonPath("$.data.itemsByStep[1].materials[1].readinessStatus").value("NO_STOCK_RECORD"))
                .andExpect(jsonPath("$.data.itemsByStep[1].materials[2].readinessStatus").value("UNLINKED_MATERIAL"))
                .andExpect(jsonPath("$.data.itemsByStep[1].materials[2].availableQty").doesNotExist())
                .andExpect(jsonPath("$.data.summary.totalLines").value(4))
                .andExpect(jsonPath("$.data.summary.readyLines").value(1))
                .andExpect(jsonPath("$.data.summary.shortageLines").value(1))
                .andExpect(jsonPath("$.data.summary.unlinkedLines").value(1))
                .andExpect(jsonPath("$.data.summary.noStockRecordLines").value(1));

        assertThat(countRows("production_work_order")).isZero();
        assertThat(countRows("production_work_order_material")).isZero();
        assertThat(countRows("inventory_transaction")).isEqualTo(transactionCountBefore);
        assertThat(stockAvailableQty(readyMaterialId)).isEqualByComparingTo(readyStockBefore);
    }

    @Test
    void createWithMaterialReadinessWritesDraftMaterialsAndSnapshotsWithoutBlockingShortage() throws Exception {
        long shortageMaterialId = insertMaterial("SHORT-CREATE", "Short create material", "pcs");
        insertStock(shortageMaterialId, "SHORT-CREATE", "Short create material", "pcs", "1.0000");
        long routeId = insertRouteTemplate("RT-READINESS-CREATE", true);
        long stepId = insertStepTemplate(routeId, "STEP-CREATE", "Create step", 1);
        insertMaterialTemplate(routeId, stepId, shortageMaterialId, "SHORT-CREATE", "Short create material", "pcs",
                "ASSEMBLY", "2.0000", null, null);
        putOrderItem(6102L, 9102L, "ORD-READINESS-CREATE", "Readiness create product", "3.00");

        BigDecimal stockBefore = stockAvailableQty(shortageMaterialId);
        long transactionCountBefore = countRows("inventory_transaction");

        String response = mockMvc.perform(post("/api/production/work-orders/create-with-material-readiness")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.ofEntries(
                                entry("orderItemId", 6102L),
                                entry("routeTemplateId", routeId),
                                entry("applyGeneratedMaterials", true),
                                entry("workOrderFields", Map.of(
                                        "priority", "NORMAL",
                                        "instructionTitle", "Create with readiness",
                                        "productionRequirement", "Readiness prompt only",
                                        "plannedStartDate", LocalDate.now().plusDays(1).toString(),
                                        "plannedFinishDate", LocalDate.now().plusDays(7).toString(),
                                        "customerAcceptanceRequired", false))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.materials.length()").value(1))
                .andExpect(jsonPath("$.data.materials[0].materialId").value(shortageMaterialId))
                .andExpect(jsonPath("$.data.materials[0].relatedStepTemplateId").value(stepId))
                .andExpect(jsonPath("$.data.materials[0].relatedStepInstanceId").doesNotExist())
                .andExpect(jsonPath("$.data.materials[0].requiredQty").value(6.0))
                .andExpect(jsonPath("$.data.materials[0].availableQtySnapshot").value(1.0))
                .andExpect(jsonPath("$.data.materials[0].shortageQty").value(5.0))
                .andExpect(jsonPath("$.data.materials[0].readinessStatus").value("SHORTAGE"))
                .andExpect(jsonPath("$.data.materials[0].readinessMessage").value("库存不足"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long workOrderId = data(response).get("id").asLong();
        Map<String, Object> material = jdbcTemplate.queryForMap(
                "select * from production_work_order_material where work_order_id = ?",
                workOrderId);
        assertThat(material.get("READINESS_STATUS")).isEqualTo("SHORTAGE");
        assertThat((BigDecimal) material.get("AVAILABLE_QTY_SNAPSHOT")).isEqualByComparingTo("1.0000");
        assertThat((BigDecimal) material.get("SHORTAGE_QTY")).isEqualByComparingTo("5.0000");
        assertThat(material.get("READINESS_CHECKED_AT")).isNotNull();
        assertThat(material.get("READINESS_MESSAGE")).isEqualTo("库存不足");
        assertThat(countRows("inventory_transaction")).isEqualTo(transactionCountBefore);
        assertThat(stockAvailableQty(shortageMaterialId)).isEqualByComparingTo(stockBefore);
        assertThat(countRows("production_route_instance")).isZero();
        assertThat(countRows("production_step_instance")).isZero();
        assertThat(orderItemAdapter.productionWriteBackCount()).isZero();
    }

    private void putOrderItem(Long orderItemId, Long orderId, String orderNo, String itemName, String quantity) {
        orderItemAdapter.putDemoOrderItem(new OrderItemCandidateContext(
                orderItemId,
                1L,
                orderId,
                orderNo,
                "PROJECT",
                "ENTERPRISE",
                21L,
                "Production owner",
                itemName,
                "standard",
                "set",
                new BigDecimal(quantity),
                "readonly remark",
                "GENERAL",
                "NOT_DISPATCHED",
                BigDecimal.ZERO,
                null));
    }

    private long insertRouteTemplate(String code, boolean enabled) {
        jdbcTemplate.update(
                """
                insert into process_route_template (
                    tenant_id, route_code, route_name, product_type, description,
                    enabled, version, deleted, delete_marker
                ) values (1, ?, ?, 'GENERAL', ?, ?, 0, 0, 0)
                """,
                code,
                code + " name",
                code + " description",
                enabled);
        return jdbcTemplate.queryForObject(
                "select id from process_route_template where route_code = ?",
                Long.class,
                code);
    }

    private long insertStepTemplate(long routeId, String code, String name, int order) {
        jdbcTemplate.update(
                """
                insert into process_step_template (
                    tenant_id, route_template_id, step_code, step_name, step_order,
                    assigned_role, photo_required, remark_required, mobile_enabled,
                    enabled, deleted, delete_marker
                ) values (1, ?, ?, ?, ?, 'WORKER', 0, 0, 1, 1, 0, 0)
                """,
                routeId,
                code,
                name,
                order);
        return jdbcTemplate.queryForObject(
                "select id from process_step_template where route_template_id = ? and step_code = ?",
                Long.class,
                routeId,
                code);
    }

    private long insertMaterial(String code, String name, String unit) {
        jdbcTemplate.update(
                """
                insert into material_item (
                    tenant_id, material_code, material_name, spec, unit,
                    enabled, deleted, delete_marker
                ) values (1, ?, ?, 'standard', ?, 1, 0, '')
                """,
                code,
                name,
                unit);
        return jdbcTemplate.queryForObject(
                "select id from material_item where material_code = ?",
                Long.class,
                code);
    }

    private void insertStock(long materialId, String code, String name, String unit, String availableQty) {
        BigDecimal qty = new BigDecimal(availableQty);
        jdbcTemplate.update(
                """
                insert into inventory_stock (
                    tenant_id, material_id, material_code_snapshot, material_name_snapshot,
                    unit_snapshot, on_hand_qty, reserved_qty, available_qty
                ) values (1, ?, ?, ?, ?, ?, 0, ?)
                """,
                materialId,
                code,
                name,
                unit,
                qty,
                qty);
    }

    private void insertMaterialTemplate(
            long routeId,
            long stepId,
            Long materialId,
            String materialCode,
            String materialName,
            String unit,
            String usageStage,
            String baseQtyPerUnit,
            String fixedQty,
            String lossRate) {
        jdbcTemplate.update(
                """
                insert into process_step_material_requirement_template (
                    tenant_id, route_template_id, step_template_id, material_id, material_code,
                    material_name, spec, unit, base_qty_per_unit, fixed_qty,
                    loss_rate, usage_stage, remark, enabled, deleted, delete_marker
                ) values (1, ?, ?, ?, ?, ?, 'standard', ?, ?, ?, ?, ?, 'template remark', 1, 0, 0)
                """,
                routeId,
                stepId,
                materialId,
                materialCode,
                materialName,
                unit,
                baseQtyPerUnit == null ? null : new BigDecimal(baseQtyPerUnit),
                fixedQty == null ? null : new BigDecimal(fixedQty),
                lossRate == null ? null : new BigDecimal(lossRate),
                usageStage);
    }

    private BigDecimal stockAvailableQty(long materialId) {
        return jdbcTemplate.queryForObject(
                "select available_qty from inventory_stock where material_id = ?",
                BigDecimal.class,
                materialId);
    }

    private long countRows(String tableName) {
        if (!tableExists(tableName)) {
            return 0L;
        }
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private void deleteIfExists(String tableName) {
        if (tableExists(tableName)) {
            jdbcTemplate.update("delete from " + tableName);
        }
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
