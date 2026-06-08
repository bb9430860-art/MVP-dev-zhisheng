package com.zhisheng.mvp.production;

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
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
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
class ProductionWorkOrderDispatchIntegrationTest {

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
        jdbcTemplate.update("delete from process_step_template");
        jdbcTemplate.update("delete from process_route_template");
        orderItemAdapter.reset();
        orderItemAdapter.resetProductionWriteBackCount();
        currentUserAdapter.setCurrentUser(201L, 1L, List.of("PRODUCTION_MANAGER"));
    }

    @Test
    void dispatchContextAndTemplateConfigUseWorkOrderId() throws Exception {
        long routeTemplateId = insertRouteTemplate("RT-WO-CONFIG", "工单配置路线", "SPIRIT_FORTRESS", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-CONFIG", "配置工序", 1, "WORKER", false, false, true);
        orderItemAdapter.putDemoOrderItem(candidate(4007L, 807L, "ORD-CONFIG", "配置产品"));
        long workOrderId = createReleasedWorkOrder(4007L);

        mockMvc.perform(get("/api/production/work-orders/{workOrderId}/dispatch-context", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workOrder.id").value(workOrderId))
                .andExpect(jsonPath("$.data.workOrder.status").value("RELEASED"))
                .andExpect(jsonPath("$.data.orderItem.id").value(4007))
                .andExpect(jsonPath("$.data.dispatched").value(false))
                .andExpect(jsonPath("$.data.workOrder.dealAmount").doesNotExist())
                .andExpect(jsonPath("$.data.orderItem.unitPrice").doesNotExist());

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/dispatch-config/from-template", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("routeTemplateId", routeTemplateId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeTemplateId").value(routeTemplateId))
                .andExpect(jsonPath("$.data.routeName").value("工单配置路线"))
                .andExpect(jsonPath("$.data.steps.length()").value(1))
                .andExpect(jsonPath("$.data.steps[0].sourceStepTemplateId").value(stepTemplateId));
    }

    @Test
    void workOrderDispatchDoesNotLetOrderItemProductionStatusBlockReleasedUnlinkedWorkOrder() throws Exception {
        long routeTemplateId = insertRouteTemplate("RT-WO-STATUS-CONTEXT", "订单项状态上下文路线", "GENERAL", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-STATUS-CONTEXT", "订单项状态上下文工序", 1, "WORKER", false, false, true);

        orderItemAdapter.putDemoOrderItem(candidate(
                4008L,
                808L,
                "ORD-ITEM-IN-PROGRESS",
                "订单项生产中产品",
                "IN_PROGRESS",
                null));
        long inProgressStatusWorkOrderId = createReleasedWorkOrder(4008L);

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/dispatch", inProgressStatusWorkOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dispatchPayload(routeTemplateId, stepTemplateId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));

        orderItemAdapter.putDemoOrderItem(candidate(
                4009L,
                809L,
                "ORD-ITEM-DISPATCHED",
                "订单项已下发状态产品",
                "DISPATCHED",
                null));
        long dispatchedStatusWorkOrderId = createReleasedWorkOrder(4009L);

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/dispatch", dispatchedStatusWorkOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dispatchPayload(routeTemplateId, stepTemplateId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));
    }

    @Test
    void workOrderDispatchRejectsOrderItemRouteLinkedToAnotherInstance() throws Exception {
        long routeTemplateId = insertRouteTemplate("RT-WO-ORDER-ITEM-LINK", "订单项已有关联路线", "GENERAL", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-ORDER-ITEM-LINK", "订单项已有关联工序", 1, "WORKER", false, false, true);
        orderItemAdapter.putDemoOrderItem(candidate(
                4010L,
                810L,
                "ORD-ORDER-ITEM-LINK",
                "订单项已有关联产品",
                "IN_PROGRESS",
                99001L));
        long workOrderId = createReleasedWorkOrder(4010L);

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/dispatch", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dispatchPayload(routeTemplateId, stepTemplateId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WORK_ORDER_ROUTE_LINK_CONFLICT"));
    }

    @Test
    void releasedWorkOrderDispatchCreatesFrozenInstancesLinksWorkOrderAndWritesProductionFields() throws Exception {
        long routeTemplateId = insertRouteTemplate("RT-WO-DISPATCH", "工单下发路线", "SPIRIT_FORTRESS", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-WO", "工单工序", 1, "WORKER", false, true, true);
        orderItemAdapter.putDemoOrderItem(candidate(4001L, 801L, "ORD-WO-DISPATCH", "工单下发产品"));
        long workOrderId = createReleasedWorkOrder(4001L);
        OrderItemProductionContext before = orderItemAdapter.getDemoOrderItem(4001L);
        int inventoryTransactionCountBefore = countRows("inventory_transaction");

        String response = mockMvc.perform(post("/api/production/work-orders/{workOrderId}/dispatch", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dispatchPayload(routeTemplateId, stepTemplateId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderItemId").value(4001))
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.data.frozen").value(true))
                .andExpect(jsonPath("$.data.stepCount").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long routeInstanceId = data(response).get("routeInstanceId").asLong();

        Map<String, Object> workOrder = row("select * from production_work_order where id = ?", workOrderId);
        assertThat(workOrder.get("STATUS")).isEqualTo("IN_PROGRESS");
        assertThat(((Number) workOrder.get("PRODUCTION_ROUTE_INSTANCE_ID")).longValue()).isEqualTo(routeInstanceId);

        Map<String, Object> route = row("select * from production_route_instance where id = ?", routeInstanceId);
        assertThat(Boolean.parseBoolean(String.valueOf(route.get("FROZEN")))
                || Integer.valueOf(1).equals(route.get("FROZEN"))).isTrue();
        assertThat(route.get("STATUS")).isEqualTo("DISPATCHED");
        assertThat(route.containsKey("WORK_ORDER_ID")).isFalse();

        List<Map<String, Object>> steps = jdbcTemplate.queryForList(
                "select * from production_step_instance where route_instance_id = ?",
                routeInstanceId);
        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).get("STATUS")).isEqualTo("PENDING");
        assertThat(steps.get(0).get("STEP_NAME")).isEqualTo("确认后工序");

        OrderItemProductionContext after = orderItemAdapter.getDemoOrderItem(4001L);
        assertThat(orderItemAdapter.productionWriteBackCount()).isEqualTo(1);
        assertThat(after.productionStatus()).isEqualTo("DISPATCHED");
        assertThat(after.productionProgress()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(after.productionRouteInstanceId()).isEqualTo(routeInstanceId);
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.orderId()).isEqualTo(before.orderId());
        assertThat(after.itemName()).isEqualTo(before.itemName());
        assertThat(after.productType()).isEqualTo(before.productType());
        assertThat(after.quantity()).isEqualByComparingTo(before.quantity());
        assertThat(countRows("inventory_transaction")).isEqualTo(inventoryTransactionCountBefore);
    }

    @Test
    void onlyReleasedWorkOrderCanDispatch() throws Exception {
        long routeTemplateId = insertRouteTemplate("RT-WO-STATUS", "状态路线", "GENERAL", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-STATUS", "状态工序", 1, "WORKER", false, false, true);

        long draftId = createWorkOrderFor(4002L, 802L, "ORD-DRAFT", "DRAFT 产品");
        expectDispatchRejected(draftId, routeTemplateId, stepTemplateId, "WORK_ORDER_NOT_RELEASED");

        long cancelledId = createReleasedWorkOrderFor(4003L, 803L, "ORD-CANCEL", "CANCELLED 产品");
        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/cancel", cancelledId))
                .andExpect(status().isOk());
        expectDispatchRejected(cancelledId, routeTemplateId, stepTemplateId, "WORK_ORDER_CANCELLED");

        long completedId = createReleasedWorkOrderFor(4004L, 804L, "ORD-COMPLETE", "COMPLETED 产品");
        jdbcTemplate.update("update production_work_order set status = 'COMPLETED' where id = ?", completedId);
        expectDispatchRejected(completedId, routeTemplateId, stepTemplateId, "WORK_ORDER_COMPLETED");

        long inProgressId = createReleasedWorkOrderFor(4006L, 806L, "ORD-IN-PROGRESS", "IN_PROGRESS 产品");
        jdbcTemplate.update("update production_work_order set status = 'IN_PROGRESS' where id = ?", inProgressId);
        expectDispatchRejected(inProgressId, routeTemplateId, stepTemplateId, "WORK_ORDER_ALREADY_DISPATCHED");
    }

    @Test
    void linkedOrInProgressWorkOrderRejectsRepeatedDispatch() throws Exception {
        long routeTemplateId = insertRouteTemplate("RT-WO-REPEAT", "重复路线", "GENERAL", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-REPEAT", "重复工序", 1, "WORKER", false, false, true);
        long workOrderId = createReleasedWorkOrderFor(4005L, 805L, "ORD-REPEAT", "重复产品");

        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/dispatch", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dispatchPayload(routeTemplateId, stepTemplateId))))
                .andExpect(status().isOk());

        expectDispatchRejected(workOrderId, routeTemplateId, stepTemplateId, "WORK_ORDER_ALREADY_DISPATCHED");
        assertThat(countRows("production_route_instance")).isEqualTo(1);
    }

    private void expectDispatchRejected(
            long workOrderId,
            long routeTemplateId,
            long stepTemplateId,
            String message) throws Exception {
        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/dispatch", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dispatchPayload(routeTemplateId, stepTemplateId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    private long createReleasedWorkOrder(Long orderItemId) throws Exception {
        String response = mockMvc.perform(post("/api/production/work-orders/from-order-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createPayload(orderItemId))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long workOrderId = data(response).get("id").asLong();
        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/release", workOrderId))
                .andExpect(status().isOk());
        orderItemAdapter.resetProductionWriteBackCount();
        return workOrderId;
    }

    private long createReleasedWorkOrderFor(
            Long orderItemId,
            Long orderId,
            String orderNo,
            String itemName) throws Exception {
        long workOrderId = createWorkOrderFor(orderItemId, orderId, orderNo, itemName);
        mockMvc.perform(post("/api/production/work-orders/{workOrderId}/release", workOrderId))
                .andExpect(status().isOk());
        orderItemAdapter.resetProductionWriteBackCount();
        return workOrderId;
    }

    private long createWorkOrderFor(
            Long orderItemId,
            Long orderId,
            String orderNo,
            String itemName) throws Exception {
        orderItemAdapter.putDemoOrderItem(candidate(orderItemId, orderId, orderNo, itemName));
        String response = mockMvc.perform(post("/api/production/work-orders/from-order-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createPayload(orderItemId))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return data(response).get("id").asLong();
    }

    private Map<String, Object> dispatchPayload(Long routeTemplateId, Long stepTemplateId) {
        return Map.of(
                "routeTemplateId", routeTemplateId,
                "routeName", "工单确认路线",
                "steps", List.of(Map.ofEntries(
                        Map.entry("clientStepId", "client-1"),
                        Map.entry("sourceStepTemplateId", stepTemplateId),
                        Map.entry("stepCode", "STEP-WO"),
                        Map.entry("stepName", "确认后工序"),
                        Map.entry("stepOrder", 1),
                        Map.entry("assignedRole", "WORKER"),
                        Map.entry("photoRequired", false),
                        Map.entry("remarkRequired", true),
                        Map.entry("mobileEnabled", true),
                        Map.entry("estimatedHours", 1),
                        Map.entry("operationInstruction", "按工单执行"))));
    }

    private Map<String, Object> createPayload(Long orderItemId) {
        return Map.ofEntries(
                Map.entry("orderItemId", orderItemId),
                Map.entry("priority", "NORMAL"),
                Map.entry("instructionTitle", "生产下发工单"),
                Map.entry("productionRequirement", "按工单生产"),
                Map.entry("plannedStartDate", LocalDate.now().plusDays(1).toString()),
                Map.entry("plannedFinishDate", LocalDate.now().plusDays(7).toString()),
                Map.entry("requiredDeliveryDate", LocalDate.now().plusDays(10).toString()),
                Map.entry("customerAcceptanceRequired", false),
                Map.entry("materials", List.of(Map.of(
                        "materialName", "板材",
                        "requiredQty", 1,
                        "unit", "张"))));
    }

    private OrderItemCandidateContext candidate(Long orderItemId, Long orderId, String orderNo, String itemName) {
        return candidate(orderItemId, orderId, orderNo, itemName, "NOT_DISPATCHED", null);
    }

    private OrderItemCandidateContext candidate(
            Long orderItemId,
            Long orderId,
            String orderNo,
            String itemName,
            String productionStatus,
            Long productionRouteInstanceId) {
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
                productionStatus,
                BigDecimal.ZERO,
                productionRouteInstanceId);
    }

    private long insertRouteTemplate(
            String routeCode,
            String routeName,
            String productType,
            boolean enabled,
            boolean deleted) {
        jdbcTemplate.update(
                """
                insert into process_route_template (
                    tenant_id,
                    route_code,
                    route_name,
                    product_type,
                    description,
                    enabled,
                    version,
                    deleted,
                    delete_marker
                ) values (?, ?, ?, ?, ?, ?, 0, ?, 0)
                """,
                1L,
                routeCode,
                routeName,
                productType,
                routeName + " description",
                enabled,
                deleted);
        return jdbcTemplate.queryForObject(
                "select id from process_route_template where route_code = ?",
                Long.class,
                routeCode);
    }

    private long insertStepTemplate(
            long routeTemplateId,
            String stepCode,
            String stepName,
            int stepOrder,
            String assignedRole,
            boolean photoRequired,
            boolean remarkRequired,
            boolean mobileEnabled) {
        jdbcTemplate.update(
                """
                insert into process_step_template (
                    tenant_id,
                    route_template_id,
                    step_code,
                    step_name,
                    step_order,
                    assigned_role,
                    photo_required,
                    remark_required,
                    mobile_enabled,
                    enabled,
                    deleted,
                    delete_marker
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, 0)
                """,
                1L,
                routeTemplateId,
                stepCode,
                stepName,
                stepOrder,
                assignedRole,
                photoRequired,
                remarkRequired,
                mobileEnabled);
        return jdbcTemplate.queryForObject(
                "select id from process_step_template where route_template_id = ? and step_code = ?",
                Long.class,
                routeTemplateId,
                stepCode);
    }

    private Map<String, Object> row(String sql, Object... args) {
        return jdbcTemplate.queryForMap(sql, args);
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
