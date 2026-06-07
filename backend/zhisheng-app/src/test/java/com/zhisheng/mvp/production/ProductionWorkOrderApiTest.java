package com.zhisheng.mvp.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.hamcrest.Matchers;
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
class ProductionWorkOrderApiTest {

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
        jdbcTemplate.update("delete from production_work_order_material");
        jdbcTemplate.update("delete from production_work_order");
        jdbcTemplate.update("delete from production_step_instance");
        jdbcTemplate.update("delete from production_route_instance");
        orderItemAdapter.reset();
        orderItemAdapter.resetProductionWriteBackCount();
        currentUserAdapter.setCurrentUser(201L, 1L, List.of("PRODUCTION_MANAGER"));
    }

    @Test
    void candidatesReturnOrderContextActiveWorkOrderStateAndNoAmountFields() throws Exception {
        orderItemAdapter.putDemoOrderItem(candidate(3001L, 701L, "WO-CAND-001", "候选精神堡垒"));
        long activeWorkOrderId = createWorkOrder(3001L);

        String response = mockMvc.perform(get("/api/production/work-orders/order-items/candidates")
                        .param("keyword", "精神堡垒"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].orderId").value(701))
                .andExpect(jsonPath("$.data.items[0].orderNo").value("WO-CAND-001"))
                .andExpect(jsonPath("$.data.items[0].orderType").value("PROJECT"))
                .andExpect(jsonPath("$.data.items[0].customerType").value("ENTERPRISE"))
                .andExpect(jsonPath("$.data.items[0].dealOwnerName").value("生产候选业务员"))
                .andExpect(jsonPath("$.data.items[0].orderItemId").value(3001))
                .andExpect(jsonPath("$.data.items[0].itemName").value("候选精神堡垒"))
                .andExpect(jsonPath("$.data.items[0].spec").value("3000mm x 1200mm"))
                .andExpect(jsonPath("$.data.items[0].unit").value("套"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].remark").value("只读备注"))
                .andExpect(jsonPath("$.data.items[0].hasActiveWorkOrder").value(true))
                .andExpect(jsonPath("$.data.items[0].activeWorkOrderId").value(activeWorkOrderId))
                .andExpect(jsonPath("$.data.items[0].activeWorkOrderNo").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].dealAmount").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].unitPrice").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].subtotal").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("dealAmount", "unitPrice", "subtotal");
    }

    @Test
    void createListDetailUpdateMaterialsReleaseAndCancelFollowApiBoundaries() throws Exception {
        orderItemAdapter.putDemoOrderItem(candidate(3002L, 702L, "WO-CREATE-001", "创建工单产品"));

        String createResponse = mockMvc.perform(post("/api/production/work-orders/from-order-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createPayload(3002L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.orderItemId").value(3002))
                .andExpect(jsonPath("$.data.productionRouteInstanceId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long workOrderId = data(createResponse).get("id").asLong();

        assertThat(orderItemAdapter.productionWriteBackCount()).isZero();
        assertThat(countRows("production_route_instance")).isZero();
        assertThat(tableExists("inventory_transaction")).isFalse();

        mockMvc.perform(post("/api/production/work-orders/from-order-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createPayload(3002L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM"));

        mockMvc.perform(get("/api/production/work-orders")
                        .param("status", "DRAFT")
                        .param("workOrderNo", data(createResponse).get("workOrderNo").asText())
                        .param("keyword", "创建工单")
                        .param("routeLinked", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(workOrderId))
                .andExpect(jsonPath("$.data.items[0].routeLinked").value(false))
                .andExpect(jsonPath("$.data.items[0].orderNo").value("WO-CREATE-001"))
                .andExpect(jsonPath("$.data.items[0].dealAmount").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].unitPrice").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].subtotal").doesNotExist());

        mockMvc.perform(get("/api/production/work-orders/{workOrderId}", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(workOrderId))
                .andExpect(jsonPath("$.data.orderNo").value("WO-CREATE-001"))
                .andExpect(jsonPath("$.data.orderType").value("PROJECT"))
                .andExpect(jsonPath("$.data.customerType").value("ENTERPRISE"))
                .andExpect(jsonPath("$.data.materials.length()").value(1))
                .andExpect(jsonPath("$.data.materials[0].materialName").value("镀锌板"))
                .andExpect(jsonPath("$.data.dealAmount").doesNotExist())
                .andExpect(jsonPath("$.data.unitPrice").doesNotExist())
                .andExpect(jsonPath("$.data.subtotal").doesNotExist());

        mockMvc.perform(put("/api/production/work-orders/{workOrderId}", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "priority", "HIGH",
                                "instructionTitle", "更新后的生产指令",
                                "productionRequirement", "更新生产要求",
                                "plannedStartDate", LocalDate.now().plusDays(2).toString(),
                                "plannedFinishDate", LocalDate.now().plusDays(9).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.instructionTitle").value("更新后的生产指令"));

        mockMvc.perform(put("/api/production/work-orders/{workOrderId}/materials", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("materials", List.of(Map.of(
                                "materialName", "亚克力板",
                                "requiredQty", 2.5,
                                "unit", "张",
                                "usageStage", "组装"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials.length()").value(1))
                .andExpect(jsonPath("$.data.materials[0].materialName").value("亚克力板"));

        mockMvc.perform(put("/api/production/work-orders/{workOrderId}/materials", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("materials", List.of(Map.of(
                                "materialName", " ",
                                "requiredQty", 1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("MATERIAL_REQUIREMENT_INVALID"));

        mockMvc.perform(put("/api/production/work-orders/{workOrderId}/materials", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("materials", List.of(Map.of(
                                "materialName", "亚克力板",
                                "requiredQty", 0))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("MATERIAL_REQUIREMENT_INVALID"));

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/release", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RELEASED"))
                .andExpect(jsonPath("$.data.releasedBy").value(201));

        mockMvc.perform(put("/api/production/work-orders/{workOrderId}", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("priority", "LOW"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WORK_ORDER_EDIT_NOT_ALLOWED"));

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/cancel", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(orderItemAdapter.productionWriteBackCount()).isZero();
        assertThat(countRows("production_route_instance")).isZero();
        assertThat(tableExists("inventory_transaction")).isFalse();
    }

    @Test
    void cancelInProgressIsRejectedAndLinkRouteDoesNotMutateRouteStructure() throws Exception {
        orderItemAdapter.putDemoOrderItem(candidate(3003L, 703L, "WO-LINK-001", "关联路线产品"));
        long workOrderId = createWorkOrder(3003L);
        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/release", workOrderId))
                .andExpect(status().isOk());
        markWorkOrderInProgress(workOrderId);

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/cancel", workOrderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WORK_ORDER_INVALID_STATUS_TRANSITION"));

        long draftWorkOrderId = createWorkOrderForDifferentItem();
        long routeId = insertRoute(3004L, 1L, true);
        Map<String, Object> routeBefore = routeRow(routeId);

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/link-route-instance", draftWorkOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productionRouteInstanceId", routeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productionRouteInstanceId").value(routeId));

        Map<String, Object> routeAfter = routeRow(routeId);
        assertThat(routeAfter.get("FROZEN")).isEqualTo(routeBefore.get("FROZEN"));
        assertThat(routeAfter.get("ROUTE_NAME_SNAPSHOT")).isEqualTo(routeBefore.get("ROUTE_NAME_SNAPSHOT"));
        assertThat(routeAfter.get("STATUS")).isEqualTo(routeBefore.get("STATUS"));
    }

    private long createWorkOrderForDifferentItem() throws Exception {
        orderItemAdapter.putDemoOrderItem(candidate(3004L, 704L, "WO-LINK-002", "可关联产品"));
        return createWorkOrder(3004L);
    }

    private long createWorkOrder(Long orderItemId) throws Exception {
        String response = mockMvc.perform(post("/api/production/work-orders/from-order-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createPayload(orderItemId))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return data(response).get("id").asLong();
    }

    private Map<String, Object> createPayload(Long orderItemId) {
        return Map.ofEntries(
                Map.entry("orderItemId", orderItemId),
                Map.entry("priority", "NORMAL"),
                Map.entry("instructionTitle", "生产指令"),
                Map.entry("productionRequirement", "按图生产"),
                Map.entry("qualityRequirement", "质检"),
                Map.entry("packagingRequirement", "木箱"),
                Map.entry("shippingRequirement", "按计划发货"),
                Map.entry("deliveryRequirement", "安装前到场"),
                Map.entry("plannedStartDate", LocalDate.now().plusDays(1).toString()),
                Map.entry("plannedFinishDate", LocalDate.now().plusDays(7).toString()),
                Map.entry("requiredDeliveryDate", LocalDate.now().plusDays(10).toString()),
                Map.entry("equipmentModel", "ZC-100"),
                Map.entry("technicalConfigSummary", "标准配置"),
                Map.entry("technicalConfigJson", "{\"cncSystem\":\"standard\"}"),
                Map.entry("responsibleUserId", 11L),
                Map.entry("handlerUserId", 12L),
                Map.entry("productionManagerId", 13L),
                Map.entry("customerAcceptanceRequired", false),
                Map.entry("materials", List.of(Map.of(
                        "materialId", 301L,
                        "materialCode", "MAT-001",
                        "materialName", "镀锌板",
                        "spec", "1.2mm",
                        "unit", "张",
                        "requiredQty", 5.5,
                        "usageStage", "下料"))));
    }

    private OrderItemCandidateContext candidate(Long orderItemId, Long orderId, String orderNo, String itemName) {
        return new OrderItemCandidateContext(
                orderItemId,
                1L,
                orderId,
                orderNo,
                "PROJECT",
                "ENTERPRISE",
                21L,
                "生产候选业务员",
                itemName,
                "3000mm x 1200mm",
                "套",
                BigDecimal.ONE,
                "只读备注",
                "SPIRIT_FORTRESS",
                "NOT_DISPATCHED",
                BigDecimal.ZERO,
                null);
    }

    private void markWorkOrderInProgress(long workOrderId) {
        jdbcTemplate.update(
                "update production_work_order set status = 'IN_PROGRESS' where id = ?",
                workOrderId);
    }

    private long insertRoute(Long orderItemId, Long tenantId, boolean frozen) {
        jdbcTemplate.update(
                """
                insert into production_route_instance (
                    tenant_id,
                    order_id,
                    order_item_id,
                    source_route_template_id,
                    route_code_snapshot,
                    route_name_snapshot,
                    status,
                    production_progress,
                    frozen,
                    deleted,
                    delete_marker
                ) values (?, ?, ?, ?, ?, ?, ?, 0, ?, 0, 0)
                """,
                tenantId,
                704L,
                orderItemId,
                301L,
                "RT-WO-API",
                "API work order route",
                "DISPATCHED",
                frozen);
        return jdbcTemplate.queryForObject(
                "select max(id) from production_route_instance",
                Long.class);
    }

    private Map<String, Object> routeRow(Long routeId) {
        return jdbcTemplate.queryForMap("select * from production_route_instance where id = ?", routeId);
    }

    private int countRows(String tableName) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where lower(table_name) = lower(?)
                """,
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
