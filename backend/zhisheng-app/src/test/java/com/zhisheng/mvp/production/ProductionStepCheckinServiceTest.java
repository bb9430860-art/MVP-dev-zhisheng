package com.zhisheng.mvp.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zhisheng.mvp.production.adapter.MockCurrentProductionUserAdapter;
import com.zhisheng.mvp.production.adapter.MockOrderItemAdapter;
import com.zhisheng.mvp.production.adapter.MockProductionFileBindingAdapter;
import com.zhisheng.mvp.production.dto.ProductionStepCheckinResult;
import com.zhisheng.mvp.production.exception.ProductionStepCheckinException;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.service.ProductionStepCheckinService;
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
class ProductionStepCheckinServiceTest {

    @Autowired
    private ProductionStepCheckinService checkinService;

    @Autowired
    private MockProductionFileBindingAdapter fileBindingAdapter;

    @Autowired
    private MockCurrentProductionUserAdapter currentUserAdapter;

    @Autowired
    private MockOrderItemAdapter orderItemAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from production_step_checkin");
        jdbcTemplate.update("delete from production_step_instance");
        jdbcTemplate.update("delete from production_route_instance");
        orderItemAdapter.reset();
        fileBindingAdapter.reset();
        currentUserAdapter.setCurrentUser(201L, 1L, List.of("WORKER"));
    }

    @Test
    void photoRequiredRequiresAtLeastOneFileId() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "IN_PROGRESS", true, false);
        markStartedBy(stepId, 201L);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        assertThatThrownBy(() -> checkinService.completeWithCheckin(stepId, List.of(), null))
                .isInstanceOf(ProductionStepCheckinException.class)
                .hasMessage("PHOTO_REQUIRED");

        assertThat(stepStatus(stepId)).isEqualTo("IN_PROGRESS");
        assertThat(checkinCount()).isZero();
        assertThat(fileBindingAdapter.bindCallCount()).isZero();
    }

    @Test
    void remarkRequiredRequiresTrimmedNonBlankRemark() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "IN_PROGRESS", false, true);
        markStartedBy(stepId, 201L);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        assertThatThrownBy(() -> checkinService.completeWithCheckin(stepId, List.of(), "   "))
                .isInstanceOf(ProductionStepCheckinException.class)
                .hasMessage("REMARK_REQUIRED");

        assertThat(stepStatus(stepId)).isEqualTo("IN_PROGRESS");
        assertThat(checkinCount()).isZero();
    }

    @Test
    void fileIdsMustNotExceedThreeMustBePositiveAndMustNotDuplicate() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "IN_PROGRESS", false, false);
        markStartedBy(stepId, 201L);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        assertThatThrownBy(() -> checkinService.completeWithCheckin(stepId, List.of(1L, 2L, 3L, 4L), null))
                .isInstanceOf(ProductionStepCheckinException.class)
                .hasMessage("FILE_IDS_TOO_MANY");

        assertThatThrownBy(() -> checkinService.completeWithCheckin(stepId, List.of(1L, 0L), null))
                .isInstanceOf(ProductionStepCheckinException.class)
                .hasMessage("FILE_ID_INVALID");

        assertThatThrownBy(() -> checkinService.completeWithCheckin(stepId, List.of(1L, 1L), null))
                .isInstanceOf(ProductionStepCheckinException.class)
                .hasMessage("FILE_ID_DUPLICATED");

        assertThat(stepStatus(stepId)).isEqualTo("IN_PROGRESS");
        assertThat(checkinCount()).isZero();
        assertThat(fileBindingAdapter.bindCallCount()).isZero();
    }

    @Test
    void optionalPhotoAndRemarkAllowEmptyEvidenceAndCompleteStep() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "IN_PROGRESS", false, false);
        markStartedBy(stepId, 201L);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));

        ProductionStepCheckinResult result = checkinService.completeWithCheckin(stepId, List.of(), null);

        assertThat(result.stepInstanceId()).isEqualTo(stepId);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.productionProgress()).isEqualTo(100);
        assertThat(stepStatus(stepId)).isEqualTo("COMPLETED");
        assertThat(routeProgress(routeId)).isEqualByComparingTo("100");
        assertThat(orderItemAdapter.getDemoOrderItem(1001L).productionProgress()).isEqualByComparingTo("100");
        assertThat(fileBindingAdapter.bindCallCount()).isEqualTo(1);
        assertThat(fileBindingAdapter.lastFileIds()).isEmpty();
    }

    @Test
    void completeWithCheckinCreatesEvidenceBindsFilesCompletesStepAndPreservesFrozenStructure() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long firstStepId = insertStep(routeId, 1001L, 1, "Design", "WORKER", null, "COMPLETED", false, false);
        long stepId = insertStep(routeId, 1001L, 2, "Cut", "WORKER", null, "IN_PROGRESS", true, true);
        markStartedBy(stepId, 201L);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));
        Map<String, Object> before = stepRow(stepId);

        ProductionStepCheckinResult result =
                checkinService.completeWithCheckin(stepId, List.of(101L, 102L), "  done  ");

        assertThat(result.stepInstanceId()).isEqualTo(stepId);
        assertThat(result.routeInstanceId()).isEqualTo(routeId);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.productionProgress()).isEqualTo(100);
        assertThat(result.checkinId()).isNotNull();
        assertThat(result.fileIds()).containsExactly(101L, 102L);

        Map<String, Object> checkin = checkinRow(result.checkinId());
        assertThat(checkin.get("STEP_INSTANCE_ID")).isEqualTo(stepId);
        assertThat(checkin.get("ROUTE_INSTANCE_ID")).isEqualTo(routeId);
        assertThat(checkin.get("ORDER_ITEM_ID")).isEqualTo(1001L);
        assertThat(checkin.get("OPERATOR_ID")).isEqualTo(201L);
        assertThat(checkin.get("REMARK")).isEqualTo("done");
        assertThat(checkin.get("FILE_IDS_JSON")).isEqualTo("[101,102]");
        assertThat(checkin.get("CHECKIN_TYPE")).isEqualTo("COMPLETE");

        assertThat(fileBindingAdapter.bindCallCount()).isEqualTo(1);
        assertThat(fileBindingAdapter.lastTenantId()).isEqualTo(1L);
        assertThat(fileBindingAdapter.lastBizType()).isEqualTo("PRODUCTION_STEP_CHECKIN");
        assertThat(fileBindingAdapter.lastBizId()).isEqualTo(result.checkinId());
        assertThat(fileBindingAdapter.lastFileIds()).containsExactly(101L, 102L);

        assertThat(stepStatus(stepId)).isEqualTo("COMPLETED");
        assertThat(routeStatus(routeId)).isEqualTo("COMPLETED");
        assertThat(routeProgress(routeId)).isEqualByComparingTo("100");
        assertThat(orderItemAdapter.getDemoOrderItem(1001L).productionStatus()).isEqualTo("COMPLETED");

        Map<String, Object> after = stepRow(stepId);
        assertStructureUnchanged(before, after);
        assertThat(stepStatus(firstStepId)).isEqualTo("COMPLETED");
    }

    @Test
    void fileBindingFailureDoesNotCompleteStepOrLeaveCheckinRecord() {
        long routeId = insertRoute(1001L, "IN_PROGRESS", true);
        long stepId = insertStep(routeId, 1001L, 1, "Cut", "WORKER", null, "IN_PROGRESS", true, false);
        markStartedBy(stepId, 201L);
        orderItemAdapter.putDemoOrderItem(dispatchedOrderItem(1001L, routeId));
        fileBindingAdapter.failNextBind();

        assertThatThrownBy(() -> checkinService.completeWithCheckin(stepId, List.of(101L), null))
                .isInstanceOf(ProductionStepCheckinException.class)
                .hasMessage("FILE_BIND_FAILED");

        assertThat(stepStatus(stepId)).isEqualTo("IN_PROGRESS");
        assertThat(checkinCount()).isZero();
    }

    @Test
    void migrationCreatesOnlyProductionStepCheckinAndNoOutOfScopeTables() {
        assertThat(tableExists("production_step_checkin")).isTrue();
        assertThat(tableExists("file_asset")).isFalse();
        assertThat(tableExists("inventory_stock")).isFalse();
        assertThat(tableExists("attendance_record")).isFalse();
        assertThat(tableExists("dashboard")).isFalse();
        assertThat(tableExists("orders")).isFalse();
        assertThat(tableExists("order_item")).isFalse();
        assertThat(tableExists("contribution_account")).isFalse();
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
                "RT-CHECKIN",
                "Checkin route",
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
            String status,
            boolean photoRequired,
            boolean remarkRequired) {
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
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, 1, 0, 0)
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
                photoRequired,
                remarkRequired,
                stepName + " instruction",
                status);
        return jdbcTemplate.queryForObject(
                "select id from production_step_instance where route_instance_id = ? and step_order = ?",
                Long.class,
                routeId,
                stepOrder);
    }

    private void markStartedBy(Long stepId, Long userId) {
        jdbcTemplate.update("update production_step_instance set started_by = ? where id = ?", userId, stepId);
    }

    private Map<String, Object> stepRow(Long stepId) {
        return jdbcTemplate.queryForMap("select * from production_step_instance where id = ?", stepId);
    }

    private Map<String, Object> checkinRow(Long checkinId) {
        return jdbcTemplate.queryForMap("select * from production_step_checkin where id = ?", checkinId);
    }

    private long checkinCount() {
        return jdbcTemplate.queryForObject("select count(*) from production_step_checkin", Long.class);
    }

    private String stepStatus(Long stepId) {
        return jdbcTemplate.queryForObject(
                "select status from production_step_instance where id = ?",
                String.class,
                stepId);
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

    private void assertStructureUnchanged(Map<String, Object> before, Map<String, Object> after) {
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
}
