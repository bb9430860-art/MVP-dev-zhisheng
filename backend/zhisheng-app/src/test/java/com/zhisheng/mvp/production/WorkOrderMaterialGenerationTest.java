package com.zhisheng.mvp.production;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class WorkOrderMaterialGenerationTest {

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
    void previewsGeneratedMaterialsWithoutWritingWorkOrderMaterialsOrInventory() throws Exception {
        long routeId = insertRouteTemplate("RT-MAT-PREVIEW", true);
        long cutStepId = insertStepTemplate(routeId, "STEP-CUT", "Cutting", 1);
        long packStepId = insertStepTemplate(routeId, "STEP-PACK", "Packing", 2);
        insertMaterialTemplate(routeId, cutStepId, "AL-PLATE", "Aluminum plate", "sheet", "CUTTING",
                "2.0000", "1.0000", "0.0500", null, true);
        insertMaterialTemplate(routeId, packStepId, "FOAM", "Packing foam", "roll", "PACKING",
                null, "1.0000", null, "qty * 2", true);
        insertMaterialTemplate(routeId, packStepId, "DISABLED", "Disabled material", "pcs", "PACKING",
                "99.0000", null, null, null, false);
        long workOrderId = createWorkOrder(5001L, 901L, "ORD-MAT-PREVIEW", "Preview product", "3.00");
        long workOrderMaterialCountBefore = countRows("production_work_order_material");

        mockMvc.perform(get("/api/production/work-orders/{workOrderId}/material-generation/preview", workOrderId)
                        .param("routeTemplateId", String.valueOf(routeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(2))
                .andExpect(jsonPath("$.data.replacedCount").value(0))
                .andExpect(jsonPath("$.data.generatedMaterials[0].stepTemplateId").value(cutStepId))
                .andExpect(jsonPath("$.data.generatedMaterials[0].stepName").value("Cutting"))
                .andExpect(jsonPath("$.data.generatedMaterials[0].stepOrder").value(1))
                .andExpect(jsonPath("$.data.generatedMaterials[0].materialCode").value("AL-PLATE"))
                .andExpect(jsonPath("$.data.generatedMaterials[0].materialName").value("Aluminum plate"))
                .andExpect(jsonPath("$.data.generatedMaterials[0].unit").value("sheet"))
                .andExpect(jsonPath("$.data.generatedMaterials[0].usageStage").value("CUTTING"))
                .andExpect(jsonPath("$.data.generatedMaterials[0].requiredQty").value(7.35))
                .andExpect(jsonPath("$.data.generatedMaterials[0].relatedStepTemplateId").value(cutStepId))
                .andExpect(jsonPath("$.data.generatedMaterials[0].relatedStepInstanceId").doesNotExist())
                .andExpect(jsonPath("$.data.generatedMaterials[1].materialName").value("Packing foam"))
                .andExpect(jsonPath("$.data.generatedMaterials[1].requiredQty").value(1.0))
                .andExpect(jsonPath("$.data.warnings[0]").value("required_qty_expression ignored in MVP: qty * 2"));

        assertThat(countRows("production_work_order_material")).isEqualTo(workOrderMaterialCountBefore);
        assertThat(countRows("inventory_transaction")).isZero();
        assertThat(countRows("production_route_instance")).isZero();
        assertThat(countRows("production_step_instance")).isZero();
        assertThat(orderItemAdapter.productionWriteBackCount()).isZero();
    }

    @Test
    void appliesGeneratedMaterialsToDraftWorkOrderAndReplacesExistingMaterials() throws Exception {
        long routeId = insertRouteTemplate("RT-MAT-APPLY", true);
        long stepId = insertStepTemplate(routeId, "STEP-ASSY", "Assembly", 1);
        insertMaterialTemplate(routeId, stepId, "LED", "LED module", "pcs", "ASSEMBLY",
                "4.0000", null, null, null, true);
        long workOrderId = createWorkOrder(5002L, 902L, "ORD-MAT-APPLY", "Apply product", "2.00");
        assertThat(countRows("production_work_order_material")).isEqualTo(1L);
        String statusBefore = workOrderStatus(workOrderId);

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/materials/generate-from-template", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "routeTemplateId", routeId,
                                "replaceExisting", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(1))
                .andExpect(jsonPath("$.data.replacedCount").value(1))
                .andExpect(jsonPath("$.data.generatedMaterials[0].materialCode").value("LED"))
                .andExpect(jsonPath("$.data.generatedMaterials[0].requiredQty").value(8.0))
                .andExpect(jsonPath("$.data.generatedMaterials[0].relatedStepTemplateId").value(stepId))
                .andExpect(jsonPath("$.data.generatedMaterials[0].relatedStepInstanceId").doesNotExist());

        assertThat(countRows("production_work_order_material")).isEqualTo(1L);
        Map<String, Object> material = row("select * from production_work_order_material where work_order_id = ?", workOrderId);
        assertThat(material.get("MATERIAL_NAME")).isEqualTo("LED module");
        assertThat((BigDecimal) material.get("REQUIRED_QTY")).isEqualByComparingTo("8.0000");
        assertThat(material.get("RELATED_STEP_TEMPLATE_ID")).isEqualTo(stepId);
        assertThat(material.get("RELATED_STEP_INSTANCE_ID")).isNull();
        assertThat(workOrderStatus(workOrderId)).isEqualTo(statusBefore);
        assertThat(countRows("inventory_transaction")).isZero();
        assertThat(countRows("production_route_instance")).isZero();
        assertThat(countRows("production_step_instance")).isZero();
        assertThat(orderItemAdapter.productionWriteBackCount()).isZero();
    }

    @Test
    void rejectsInvalidGenerationRequests() throws Exception {
        long routeId = insertRouteTemplate("RT-MAT-REJECT", true);
        long stepId = insertStepTemplate(routeId, "STEP-REJECT", "Reject step", 1);
        insertMaterialTemplate(routeId, stepId, "BOLT", "Bolt", "pcs", "ASSEMBLY",
                null, "1.0000", null, null, true);
        long workOrderId = createWorkOrder(5003L, 903L, "ORD-MAT-REJECT", "Reject product", "1.00");

        mockMvc.perform(get("/api/production/work-orders/{workOrderId}/material-generation/preview", workOrderId)
                        .param("routeTemplateId", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("PROCESS_ROUTE_TEMPLATE_NOT_FOUND"));

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/materials/generate-from-template", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "routeTemplateId", routeId,
                                "replaceExisting", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WORK_ORDER_MATERIAL_REPLACE_REJECTED"));

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/release", workOrderId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/materials/generate-from-template", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "routeTemplateId", routeId,
                                "replaceExisting", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WORK_ORDER_NOT_DRAFT"));

        long emptyRouteId = insertRouteTemplate("RT-MAT-EMPTY", true);
        long emptyWorkOrderId = createWorkOrder(5004L, 904L, "ORD-MAT-EMPTY", "Empty product", "1.00");
        mockMvc.perform(get("/api/production/work-orders/{workOrderId}/material-generation/preview", emptyWorkOrderId)
                        .param("routeTemplateId", String.valueOf(emptyRouteId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WORK_ORDER_MATERIAL_GENERATION_EMPTY"));
    }

    private long createWorkOrder(
            Long orderItemId,
            Long orderId,
            String orderNo,
            String itemName,
            String quantity) throws Exception {
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
        String response = mockMvc.perform(post("/api/production/work-orders/from-order-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.ofEntries(
                                entry("orderItemId", orderItemId),
                                entry("priority", "NORMAL"),
                                entry("instructionTitle", "Material generation work order"),
                                entry("productionRequirement", "Generate demand only"),
                                entry("plannedStartDate", LocalDate.now().plusDays(1).toString()),
                                entry("plannedFinishDate", LocalDate.now().plusDays(7).toString()),
                                entry("requiredDeliveryDate", LocalDate.now().plusDays(10).toString()),
                                entry("customerAcceptanceRequired", false),
                                entry("materials", List.of(Map.of(
                                        "materialName", "Manual material",
                                        "unit", "pcs",
                                        "requiredQty", 1)))))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        orderItemAdapter.resetProductionWriteBackCount();
        return data(response).get("id").asLong();
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

    private void insertMaterialTemplate(
            long routeId,
            long stepId,
            String materialCode,
            String materialName,
            String unit,
            String usageStage,
            String baseQtyPerUnit,
            String fixedQty,
            String lossRate,
            String expression,
            boolean enabled) {
        jdbcTemplate.update(
                """
                insert into process_step_material_requirement_template (
                    tenant_id, route_template_id, step_template_id, material_code,
                    material_name, spec, unit, base_qty_per_unit, fixed_qty,
                    loss_rate, required_qty_expression, usage_stage, remark,
                    enabled, deleted, delete_marker
                ) values (1, ?, ?, ?, ?, 'standard', ?, ?, ?, ?, ?, ?, 'template remark', ?, 0, 0)
                """,
                routeId,
                stepId,
                materialCode,
                materialName,
                unit,
                baseQtyPerUnit == null ? null : new BigDecimal(baseQtyPerUnit),
                fixedQty == null ? null : new BigDecimal(fixedQty),
                lossRate == null ? null : new BigDecimal(lossRate),
                expression,
                usageStage,
                enabled);
    }

    private String workOrderStatus(long workOrderId) {
        return jdbcTemplate.queryForObject(
                "select status from production_work_order where id = ?",
                String.class,
                workOrderId);
    }

    private Map<String, Object> row(String sql, Object... args) {
        return jdbcTemplate.queryForMap(sql, args);
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
