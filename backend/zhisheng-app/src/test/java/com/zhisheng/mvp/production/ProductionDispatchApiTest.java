package com.zhisheng.mvp.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhisheng.mvp.production.adapter.MockOrderItemAdapter;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import java.math.BigDecimal;
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
class ProductionDispatchApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockOrderItemAdapter orderItemAdapter;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from production_step_instance");
        jdbcTemplate.update("delete from production_route_instance");
        jdbcTemplate.update("delete from process_step_template");
        jdbcTemplate.update("delete from process_route_template");
        orderItemAdapter.reset();
    }

    @Test
    void configContextReturnsOrderItemAndDispatchState() throws Exception {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                2001L,
                601L,
                "入口精神堡垒",
                "SPIRIT_FORTRESS",
                BigDecimal.ONE));

        mockMvc.perform(get("/api/production/order-items/{orderItemId}/config-context", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.orderItem.id").value(2001))
                .andExpect(jsonPath("$.data.orderItem.orderId").value(601))
                .andExpect(jsonPath("$.data.orderItem.itemName").value("入口精神堡垒"))
                .andExpect(jsonPath("$.data.orderItem.productType").value("SPIRIT_FORTRESS"))
                .andExpect(jsonPath("$.data.orderItem.productionStatus").value("NOT_DISPATCHED"))
                .andExpect(jsonPath("$.data.dispatched").value(false));
    }

    @Test
    void fromTemplateReturnsEditableDispatchConfigFromEnabledSteps() throws Exception {
        long routeId = insertRouteTemplate("RT-API-TEMPLATE", "API路线", "SPIRIT_FORTRESS", true, false);
        long firstStepId = insertStepTemplate(routeId, "STEP-DESIGN", "设计深化", 1, "DESIGNER", false, true, false);
        insertStepTemplate(routeId, "STEP-WELD", "焊接", 2, "WORKER", true, false, true);
        long disabledStepId = insertStepTemplate(routeId, "STEP-DISABLED", "停用工序", 3, "WORKER", false, false, true);
        jdbcTemplate.update("update process_step_template set enabled = 0 where id = ?", disabledStepId);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                2002L,
                602L,
                "模板产品",
                "SPIRIT_FORTRESS",
                BigDecimal.ONE));

        mockMvc.perform(post("/api/production/order-items/{orderItemId}/dispatch-config/from-template", 2002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("routeTemplateId", routeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.routeTemplateId").value(routeId))
                .andExpect(jsonPath("$.data.routeName").value("API路线"))
                .andExpect(jsonPath("$.data.steps.length()").value(2))
                .andExpect(jsonPath("$.data.steps[0].sourceStepTemplateId").value(firstStepId))
                .andExpect(jsonPath("$.data.steps[0].stepName").value("设计深化"))
                .andExpect(jsonPath("$.data.steps[0].assignedRole").value("DESIGNER"))
                .andExpect(jsonPath("$.data.steps[0].remarkRequired").value(true));
    }

    @Test
    void dispatchEndpointCreatesFrozenInstancesAndSummaryReflectsDispatch() throws Exception {
        long routeId = insertRouteTemplate("RT-API-DISPATCH", "下发路线", "GENERAL", true, false);
        long stepId = insertStepTemplate(routeId, "STEP-QC", "质检", 1, "QC", true, true, true);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                2003L,
                603L,
                "下发产品",
                "GENERAL",
                BigDecimal.ONE));

        Map<String, Object> payload = Map.of(
                "routeTemplateId", routeId,
                "routeName", "下发路线-已调整",
                "steps", List.of(Map.of(
                        "clientStepId", "step-1",
                        "sourceStepTemplateId", stepId,
                        "stepCode", "STEP-QC",
                        "stepName", "终检",
                        "stepOrder", 1,
                        "assignedRole", "QC",
                        "photoRequired", true,
                        "remarkRequired", true,
                        "mobileEnabled", true)));

        String response = mockMvc.perform(post("/api/production/order-items/{orderItemId}/dispatch", 2003L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderItemId").value(2003))
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.data.frozen").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long routeInstanceId = objectMapper.readTree(response).get("data").get("routeInstanceId").asLong();

        mockMvc.perform(get("/api/production/order-items/{orderItemId}/summary", 2003L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderItemId").value(2003))
                .andExpect(jsonPath("$.data.productionStatus").value("DISPATCHED"))
                .andExpect(jsonPath("$.data.productionRouteInstanceId").value(routeInstanceId))
                .andExpect(jsonPath("$.data.progress").value(0))
                .andExpect(jsonPath("$.data.totalSteps").value(1))
                .andExpect(jsonPath("$.data.completedSteps").value(0))
                .andExpect(jsonPath("$.data.currentStepName").value("终检"))
                .andExpect(jsonPath("$.data.dispatched").value(true))
                .andExpect(jsonPath("$.data.frozen").value(true));
    }

    @Test
    void summaryForNotDispatchedOrderItemDoesNotCreateInstance() throws Exception {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                2004L,
                604L,
                "未下发产品",
                "GENERAL",
                BigDecimal.ONE));

        mockMvc.perform(get("/api/production/order-items/{orderItemId}/summary", 2004L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderItemId").value(2004))
                .andExpect(jsonPath("$.data.dispatched").value(false))
                .andExpect(jsonPath("$.data.totalSteps").value(0));

        Integer routeCount = jdbcTemplate.queryForObject(
                "select count(*) from production_route_instance where order_item_id = ?",
                Integer.class,
                2004L);
        assertThat(routeCount).isZero();
    }

    @Test
    void businessErrorsUseUnifiedEnvelope() throws Exception {
        mockMvc.perform(get("/api/production/order-items/{orderItemId}/config-context", 9999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ORDER_ITEM_NOT_FOUND"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
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
}
