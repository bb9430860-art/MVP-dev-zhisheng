package com.zhisheng.mvp.production;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhisheng.mvp.production.adapter.MockCurrentProductionUserAdapter;
import com.zhisheng.mvp.production.adapter.MockOrderItemAdapter;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionStepExecutionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockOrderItemAdapter orderItemAdapter;

    @Autowired
    private MockCurrentProductionUserAdapter currentUserAdapter;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from production_step_instance");
        jdbcTemplate.update("delete from production_route_instance");
        orderItemAdapter.reset();
        currentUserAdapter.setCurrentUser(201L, 1L, List.of("WORKER"));
    }

    @Test
    void myTasksReturnsEnvelopeAndExecutableTasks() throws Exception {
        long routeId = insertRoute(3001L, "DISPATCHED", true);
        long assignedStepId = insertStep(routeId, 3001L, 1, "Cut", "WORKER", 201L, "PENDING");
        long roleStepId = insertStep(routeId, 3001L, 2, "Paint", "WORKER", null, "PENDING");
        insertStep(routeId, 3001L, 3, "QC", "QC", null, "PENDING");

        mockMvc.perform(get("/api/production/tasks/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].stepInstanceId").value(assignedStepId))
                .andExpect(jsonPath("$.data[0].stepName").value("Cut"))
                .andExpect(jsonPath("$.data[0].photoRequired").value(true))
                .andExpect(jsonPath("$.data[0].remarkRequired").value(true))
                .andExpect(jsonPath("$.data[0].canStart").value(true))
                .andExpect(jsonPath("$.data[0].canComplete").value(false))
                .andExpect(jsonPath("$.data[1].stepInstanceId").value(roleStepId));
    }

    @Test
    void stepDetailReturnsEnvelopeAndMetadata() throws Exception {
        long routeId = insertRoute(3002L, "DISPATCHED", true);
        long stepId = insertStep(routeId, 3002L, 1, "Cut", "WORKER", null, "PENDING");

        mockMvc.perform(get("/api/production/step-instances/{stepInstanceId}", stepId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.stepInstanceId").value(stepId))
                .andExpect(jsonPath("$.data.routeInstanceId").value(routeId))
                .andExpect(jsonPath("$.data.orderItemId").value(3002))
                .andExpect(jsonPath("$.data.stepOrder").value(1))
                .andExpect(jsonPath("$.data.stepName").value("Cut"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.photoRequired").value(true))
                .andExpect(jsonPath("$.data.remarkRequired").value(true))
                .andExpect(jsonPath("$.data.mobileEnabled").value(true))
                .andExpect(jsonPath("$.data.canStart").value(true))
                .andExpect(jsonPath("$.data.canComplete").value(false));
    }

    @Test
    void startAndCompleteEndpointsUpdateStateAndProgress() throws Exception {
        long routeId = insertRoute(3003L, "DISPATCHED", true);
        long stepId = insertStep(routeId, 3003L, 1, "Cut", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(3003L, routeId));

        mockMvc.perform(post("/api/production/step-instances/{stepInstanceId}/start", stepId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.stepInstanceId").value(stepId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/production/step-instances/{stepInstanceId}/complete", stepId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.stepInstanceId").value(stepId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.productionProgress").value(100));

        mockMvc.perform(get("/api/production/route-instances/{routeInstanceId}/progress", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.routeInstanceId").value(routeId))
                .andExpect(jsonPath("$.data.totalSteps").value(1))
                .andExpect(jsonPath("$.data.completedSteps").value(1))
                .andExpect(jsonPath("$.data.progress").value(100))
                .andExpect(jsonPath("$.data.routeStatus").value("COMPLETED"));
    }

    @Test
    void startLaterStepBeforePreviousCompleteReturnsEnvelopeError() throws Exception {
        long routeId = insertRoute(3004L, "DISPATCHED", true);
        insertStep(routeId, 3004L, 1, "Cut", "WORKER", null, "PENDING");
        long secondStepId = insertStep(routeId, 3004L, 2, "Paint", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(3004L, routeId));

        mockMvc.perform(post("/api/production/step-instances/{stepInstanceId}/start", secondStepId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("PREVIOUS_STEP_NOT_COMPLETED"));
    }

    @Test
    void repeatedStartReturnsEnvelopeError() throws Exception {
        long routeId = insertRoute(3005L, "IN_PROGRESS", true);
        long stepId = insertStep(routeId, 3005L, 1, "Cut", "WORKER", 201L, "IN_PROGRESS");
        jdbcTemplate.update("update production_step_instance set started_by = ? where id = ?", 201L, stepId);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(3005L, routeId));

        mockMvc.perform(post("/api/production/step-instances/{stepInstanceId}/start", stepId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("STEP_ALREADY_STARTED"));
    }

    @Test
    void pendingCompleteReturnsEnvelopeError() throws Exception {
        long routeId = insertRoute(3006L, "DISPATCHED", true);
        long stepId = insertStep(routeId, 3006L, 1, "Cut", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(3006L, routeId));

        mockMvc.perform(post("/api/production/step-instances/{stepInstanceId}/complete", stepId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("STEP_NOT_IN_PROGRESS"));
    }

    @Test
    void roleBasedStartedByRestrictionReturnsEnvelopeError() throws Exception {
        long routeId = insertRoute(3007L, "DISPATCHED", true);
        long stepId = insertStep(routeId, 3007L, 1, "Cut", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(3007L, routeId));

        mockMvc.perform(post("/api/production/step-instances/{stepInstanceId}/start", stepId))
                .andExpect(status().isOk());

        currentUserAdapter.setCurrentUser(202L, 1L, List.of("WORKER"));
        mockMvc.perform(post("/api/production/step-instances/{stepInstanceId}/complete", stepId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("STEP_NOT_ASSIGNED_TO_CURRENT_USER"));
    }

    private OrderItemProductionContext dispatchedOrderItem(Long orderItemId, Long routeId) {
        return OrderItemProductionContext.notDispatched(
                        orderItemId,
                        501L,
                        "Production item",
                        "GENERAL",
                        BigDecimal.ONE)
                .withProductionFields("DISPATCHED", BigDecimal.ZERO, routeId);
    }

    private long insertRoute(Long orderItemId, String status, boolean frozen) {
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
                1L,
                501L,
                orderItemId,
                301L,
                "RT-EXEC",
                "Execution route",
                status,
                frozen);
        return jdbcTemplate.queryForObject(
                "select id from production_route_instance where order_item_id = ?",
                Long.class,
                orderItemId);
    }

    private long insertStep(
            Long routeId,
            Long orderItemId,
            int stepOrder,
            String stepName,
            String assignedRole,
            Long assignedUserId,
            String status) {
        jdbcTemplate.update(
                """
                insert into production_step_instance (
                    tenant_id,
                    route_instance_id,
                    order_id,
                    order_item_id,
                    source_step_template_id,
                    step_code_snapshot,
                    step_name,
                    step_order,
                    assigned_role,
                    assigned_user_id,
                    photo_required,
                    remark_required,
                    mobile_enabled,
                    operation_instruction,
                    status,
                    frozen,
                    deleted,
                    delete_marker
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 1, ?, ?, 1, 0, 0)
                """,
                1L,
                routeId,
                501L,
                orderItemId,
                401L + stepOrder,
                "STEP-" + stepOrder,
                stepName,
                stepOrder,
                assignedRole,
                assignedUserId,
                stepName + " instruction",
                status);
        return jdbcTemplate.queryForObject(
                "select id from production_step_instance where route_instance_id = ? and step_order = ?",
                Long.class,
                routeId,
                stepOrder);
    }
}
