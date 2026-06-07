package com.zhisheng.mvp.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zhisheng.mvp.production.adapter.MockCurrentProductionUserAdapter;
import com.zhisheng.mvp.production.adapter.MockOrderItemAdapter;
import com.zhisheng.mvp.production.dto.ProductionProgressResponse;
import com.zhisheng.mvp.production.dto.ProductionStepExecutionResponse;
import com.zhisheng.mvp.production.dto.ProductionTaskResponse;
import com.zhisheng.mvp.production.exception.ProductionStepExecutionException;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.service.ProductionStepExecutionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductionStepExecutionServiceTest {

    @Autowired
    private ProductionStepExecutionService executionService;

    @Autowired
    private MockCurrentProductionUserAdapter currentUserAdapter;

    @Autowired
    private MockOrderItemAdapter orderItemAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from production_step_instance");
        jdbcTemplate.update("delete from production_route_instance");
        orderItemAdapter.reset();
        currentUserAdapter.setCurrentUser(201L, 1L, List.of("WORKER"));
    }

    @Test
    void myTasksReturnsAssignedUserAndMatchingRolePendingTasksOnly() {
        long routeId = insertRoute(1001L, "DISPATCHED", true);
        long assignedToCurrentUser = insertStep(routeId, 1001L, 1, "Assigned", "WORKER", 201L, "PENDING");
        long matchingRole = insertStep(routeId, 1001L, 2, "Role task", "WORKER", null, "PENDING");
        insertStep(routeId, 1001L, 3, "Other user", "WORKER", 999L, "PENDING");
        insertStep(routeId, 1001L, 4, "Other role", "QC", null, "PENDING");
        insertStep(routeId, 1001L, 5, "Already started", "WORKER", null, "IN_PROGRESS");
        long unfrozenRouteId = insertRoute(1002L, "DISPATCHED", false);
        insertStep(unfrozenRouteId, 1002L, 1, "Unfrozen", "WORKER", null, "PENDING");

        List<ProductionTaskResponse> tasks = executionService.myTasks();

        assertThat(tasks)
                .extracting(ProductionTaskResponse::stepInstanceId)
                .containsExactly(assignedToCurrentUser, matchingRole);
    }

    @Test
    void startFirstStepWritesExecutionFieldsAndMovesRouteAndOrderToInProgress() {
        long routeId = insertRoute(1001L, "DISPATCHED", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        ProductionStepExecutionResponse response = executionService.startStep(stepId);

        assertThat(response.stepInstanceId()).isEqualTo(stepId);
        assertThat(response.status()).isEqualTo("IN_PROGRESS");

        Map<String, Object> row = stepRow(stepId);
        assertThat(row.get("STATUS")).isEqualTo("IN_PROGRESS");
        assertThat(row.get("STARTED_AT")).isNotNull();
        assertThat(row.get("STARTED_BY")).isEqualTo(201L);
        assertThat(row.get("ASSIGNED_USER_ID")).isNull();

        assertThat(routeStatus(routeId)).isEqualTo("IN_PROGRESS");
        assertThat(orderItemAdapter.getDemoOrderItem(1001L).productionStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void laterStepCannotStartBeforePreviousActiveStepCompletes() {
        long routeId = insertRoute(1001L, "DISPATCHED", true);
        insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "PENDING");
        long secondStepId = insertStep(routeId, 1001L, 2, "Paint", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        assertThatThrownBy(() -> executionService.startStep(secondStepId))
                .isInstanceOf(ProductionStepExecutionException.class)
                .hasMessage("PREVIOUS_STEP_NOT_COMPLETED");
    }

    @Test
    void repeatedStartReturnsAlreadyStartedOrNotPending() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", 201L, "IN_PROGRESS");
        jdbcTemplate.update("update production_step_instance set started_by = ? where id = ?", 201L, stepId);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        assertThatThrownBy(() -> executionService.startStep(stepId))
                .isInstanceOf(ProductionStepExecutionException.class)
                .hasMessage("STEP_ALREADY_STARTED");
    }

    @Test
    void completeInProgressStepWritesExecutionFieldsAndCalculatesIntegerProgress() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long firstStepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "COMPLETED");
        long secondStepId = insertStep(routeId, 1001L, 2, "Paint", "WORKER", null, "IN_PROGRESS");
        jdbcTemplate.update("update production_step_instance set started_by = ? where id = ?", 201L, secondStepId);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        ProductionStepExecutionResponse response = executionService.completeStep(secondStepId);

        assertThat(response.stepInstanceId()).isEqualTo(secondStepId);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(stepRow(secondStepId).get("COMPLETED_AT")).isNotNull();
        assertThat(stepRow(secondStepId).get("COMPLETED_BY")).isEqualTo(201L);

        ProductionProgressResponse progress = executionService.progress(routeId);
        assertThat(progress.completedSteps()).isEqualTo(2);
        assertThat(progress.totalSteps()).isEqualTo(2);
        assertThat(progress.progress()).isEqualTo(100);
        assertThat(routeStatus(routeId)).isEqualTo("COMPLETED");
        assertThat(routeProgress(routeId)).isEqualByComparingTo("100");
        assertThat(orderItemAdapter.getDemoOrderItem(1001L).productionStatus()).isEqualTo("COMPLETED");
        assertThat(orderItemAdapter.getDemoOrderItem(1001L).productionProgress()).isEqualByComparingTo("100");

        assertThat(stepRow(firstStepId).get("STATUS")).isEqualTo("COMPLETED");
    }

    @Test
    void completePendingOrCompletedStepReturnsSpecificErrors() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long pendingStepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "PENDING");
        long completedStepId = insertStep(routeId, 1001L, 2, "Paint", "WORKER", null, "COMPLETED");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        assertThatThrownBy(() -> executionService.completeStep(pendingStepId))
                .isInstanceOf(ProductionStepExecutionException.class)
                .hasMessage("STEP_NOT_IN_PROGRESS");

        assertThatThrownBy(() -> executionService.completeStep(completedStepId))
                .isInstanceOf(ProductionStepExecutionException.class)
                .hasMessage("STEP_ALREADY_COMPLETED");
    }

    @Test
    void roleBasedTaskStartedByOneWorkerCannotBeCompletedByAnotherWorker() {
        long routeId = insertRoute(1001L, "DISPATCHED", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        executionService.startStep(stepId);

        currentUserAdapter.setCurrentUser(202L, 1L, List.of("WORKER"));
        assertThatThrownBy(() -> executionService.completeStep(stepId))
                .isInstanceOf(ProductionStepExecutionException.class)
                .hasMessage("STEP_NOT_ASSIGNED_TO_CURRENT_USER");
    }

    @Test
    void progressUsesFloorIntegerAndRejectsZeroTotalSteps() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        insertStep(routeId, 1001L, 1, "Cut", "WORKER", 201L, "COMPLETED");
        insertStep(routeId, 1001L, 2, "Paint", "WORKER", 201L, "PENDING");
        insertStep(routeId, 1001L, 3, "Pack", "WORKER", 201L, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        ProductionProgressResponse progress = executionService.progress(routeId);

        assertThat(progress.progress()).isEqualTo(33);

        long emptyRouteId = insertRoute(1002L, "IN_PROGRESS", true);
        assertThatThrownBy(() -> executionService.progress(emptyRouteId))
                .isInstanceOf(ProductionStepExecutionException.class)
                .hasMessage("PRODUCTION_PROGRESS_INVALID_TOTAL_STEPS");
    }

    @Test
    void startAndCompleteDoNotModifyFrozenStructureFieldsAndDoNotRequirePhotoOrRemark() {
        long routeId = insertRoute(1001L, "DISPATCHED", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "PENDING");
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));
        Map<String, Object> before = stepRow(stepId);

        executionService.startStep(stepId);
        executionService.completeStep(stepId);

        Map<String, Object> after = stepRow(stepId);
        assertThat(after.get("STEP_ORDER")).isEqualTo(before.get("STEP_ORDER"));
        assertThat(after.get("STEP_NAME")).isEqualTo(before.get("STEP_NAME"));
        assertThat(after.get("ASSIGNED_ROLE")).isEqualTo(before.get("ASSIGNED_ROLE"));
        assertThat(after.get("ASSIGNED_USER_ID")).isEqualTo(before.get("ASSIGNED_USER_ID"));
        assertThat(after.get("PHOTO_REQUIRED")).isEqualTo(before.get("PHOTO_REQUIRED"));
        assertThat(after.get("REMARK_REQUIRED")).isEqualTo(before.get("REMARK_REQUIRED"));
        assertThat(after.get("MOBILE_ENABLED")).isEqualTo(before.get("MOBILE_ENABLED"));
        assertThat(after.get("SOURCE_STEP_TEMPLATE_ID")).isEqualTo(before.get("SOURCE_STEP_TEMPLATE_ID"));
        assertThat(after.get("STEP_CODE_SNAPSHOT")).isEqualTo(before.get("STEP_CODE_SNAPSHOT"));
        assertThat(after.get("OPERATION_INSTRUCTION")).isEqualTo(before.get("OPERATION_INSTRUCTION"));
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

    private Map<String, Object> stepRow(Long stepId) {
        return jdbcTemplate.queryForMap("select * from production_step_instance where id = ?", stepId);
    }

    private String routeStatus(Long routeId) {
        return jdbcTemplate.queryForObject(
                "select status from production_route_instance where id = ?",
                String.class,
                routeId);
    }

    private BigDecimal routeProgress(Long routeId) {
        return jdbcTemplate.queryForObject(
                "select production_progress from production_route_instance where id = ?",
                BigDecimal.class,
                routeId);
    }
}
