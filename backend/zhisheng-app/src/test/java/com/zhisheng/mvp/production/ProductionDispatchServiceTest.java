package com.zhisheng.mvp.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zhisheng.mvp.production.adapter.MockOrderItemAdapter;
import com.zhisheng.mvp.production.dto.DispatchProductionRequest;
import com.zhisheng.mvp.production.dto.DispatchStepRequest;
import com.zhisheng.mvp.production.dto.ProductionDispatchResponse;
import com.zhisheng.mvp.production.exception.ProductionDispatchException;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.service.ProductionDispatchService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductionDispatchServiceTest {

    @Autowired
    private ProductionDispatchService dispatchService;

    @Autowired
    private MockOrderItemAdapter orderItemAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from production_step_instance");
        jdbcTemplate.update("delete from production_route_instance");
        jdbcTemplate.update("delete from process_step_template");
        jdbcTemplate.update("delete from process_route_template");
        orderItemAdapter.reset();
    }

    @Test
    void dispatchCreatesFrozenRouteAndPendingStepSnapshotsAndWritesProductionFields() {
        long routeTemplateId = insertRouteTemplate("RT-DISPATCH", "Dispatch route", "SPIRIT_FORTRESS", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-DESIGN", "Design", 1, "DESIGNER", true, false, true);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1001L,
                501L,
                "Entrance spirit fortress",
                "SPIRIT_FORTRESS",
                new BigDecimal("1.00")));

        ProductionDispatchResponse response = dispatchService.dispatch(1001L, request(routeTemplateId, List.of(
                step(stepTemplateId, "STEP-DESIGN", "Design final", 1, "DESIGNER"))));

        assertThat(response.routeInstanceId()).isNotNull();
        assertThat(response.orderItemId()).isEqualTo(1001L);
        assertThat(response.status()).isEqualTo("DISPATCHED");
        assertThat(response.frozen()).isTrue();
        assertThat(response.stepCount()).isEqualTo(1);

        Boolean routeFrozen = jdbcTemplate.queryForObject(
                "select frozen from production_route_instance where id = ?",
                Boolean.class,
                response.routeInstanceId());
        String routeStatus = jdbcTemplate.queryForObject(
                "select status from production_route_instance where id = ?",
                String.class,
                response.routeInstanceId());
        String stepStatus = jdbcTemplate.queryForObject(
                "select status from production_step_instance where route_instance_id = ?",
                String.class,
                response.routeInstanceId());
        String stepName = jdbcTemplate.queryForObject(
                "select step_name from production_step_instance where route_instance_id = ?",
                String.class,
                response.routeInstanceId());

        assertThat(routeFrozen).isTrue();
        assertThat(routeStatus).isEqualTo("DISPATCHED");
        assertThat(stepStatus).isEqualTo("PENDING");
        assertThat(stepName).isEqualTo("Design final");

        OrderItemProductionContext orderItem = orderItemAdapter.getDemoOrderItem(1001L);
        assertThat(orderItem.productionStatus()).isEqualTo("DISPATCHED");
        assertThat(orderItem.productionProgress()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(orderItem.productionRouteInstanceId()).isEqualTo(response.routeInstanceId());
    }

    @Test
    void repeatedDispatchReturnsOrderItemAlreadyDispatched() {
        long routeTemplateId = insertRouteTemplate("RT-REPEAT", "Repeat route", "GENERAL", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-A", "Step A", 1, "WORKER", false, false, true);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1002L,
                502L,
                "Repeated item",
                "GENERAL",
                BigDecimal.ONE));

        dispatchService.dispatch(1002L, request(routeTemplateId, List.of(
                step(stepTemplateId, "STEP-A", "Step A", 1, "WORKER"))));

        assertThatThrownBy(() -> dispatchService.dispatch(1002L, request(routeTemplateId, List.of(
                        step(stepTemplateId, "STEP-A", "Step A", 1, "WORKER")))))
                .isInstanceOf(ProductionDispatchException.class)
                .hasMessage("ORDER_ITEM_ALREADY_DISPATCHED");

        Integer routeCount = jdbcTemplate.queryForObject(
                "select count(*) from production_route_instance where order_item_id = ?",
                Integer.class,
                1002L);
        assertThat(routeCount).isEqualTo(1);
    }

    @Test
    void laterTemplateChangesDoNotAffectInstanceSnapshots() {
        long routeTemplateId = insertRouteTemplate("RT-SNAPSHOT", "Snapshot route", "GENERAL", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-SNAPSHOT", "Snapshot step", 1, "QC", true, true, true);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1003L,
                503L,
                "Snapshot item",
                "GENERAL",
                BigDecimal.ONE));

        ProductionDispatchResponse response = dispatchService.dispatch(1003L, request(routeTemplateId, List.of(
                step(stepTemplateId, "STEP-SNAPSHOT", "Configured snapshot step", 1, "QC"))));

        jdbcTemplate.update(
                "update process_route_template set route_name = ? where id = ?",
                "Changed route",
                routeTemplateId);
        jdbcTemplate.update(
                "update process_step_template set step_name = ? where id = ?",
                "Changed step",
                stepTemplateId);

        String routeNameSnapshot = jdbcTemplate.queryForObject(
                "select route_name_snapshot from production_route_instance where id = ?",
                String.class,
                response.routeInstanceId());
        String stepName = jdbcTemplate.queryForObject(
                "select step_name from production_step_instance where route_instance_id = ?",
                String.class,
                response.routeInstanceId());

        assertThat(routeNameSnapshot).isEqualTo("Snapshot route");
        assertThat(stepName).isEqualTo("Configured snapshot step");
    }

    @Test
    void emptyStepsAreRejected() {
        long routeTemplateId = insertRouteTemplate("RT-EMPTY", "Empty route", "GENERAL", true, false);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1004L,
                504L,
                "Empty item",
                "GENERAL",
                BigDecimal.ONE));

        assertThatThrownBy(() -> dispatchService.dispatch(1004L, request(routeTemplateId, List.of())))
                .isInstanceOf(ProductionDispatchException.class)
                .hasMessage("DISPATCH_STEPS_REQUIRED");
    }

    @Test
    void disabledOrDeletedRouteTemplateCannotBeDispatched() {
        long disabledRouteId = insertRouteTemplate("RT-DISABLED", "Disabled route", "GENERAL", false, false);
        long deletedRouteId = insertRouteTemplate("RT-DELETED", "Deleted route", "GENERAL", true, true);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1005L,
                505L,
                "Disabled item",
                "GENERAL",
                BigDecimal.ONE));
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1006L,
                506L,
                "Deleted item",
                "GENERAL",
                BigDecimal.ONE));

        assertThatThrownBy(() -> dispatchService.dispatch(1005L, request(disabledRouteId, List.of(
                        step(null, "STEP-X", "Step X", 1, "WORKER")))))
                .isInstanceOf(ProductionDispatchException.class)
                .hasMessage("ROUTE_TEMPLATE_NOT_AVAILABLE");

        assertThatThrownBy(() -> dispatchService.dispatch(1006L, request(deletedRouteId, List.of(
                        step(null, "STEP-X", "Step X", 1, "WORKER")))))
                .isInstanceOf(ProductionDispatchException.class)
                .hasMessage("ROUTE_TEMPLATE_NOT_AVAILABLE");
    }

    @Test
    void mockOrderItemAdapterOnlyWritesProductionFields() {
        long routeTemplateId = insertRouteTemplate("RT-LIMITED-WRITE", "Limited write route", "GENERAL", true, false);
        long stepTemplateId = insertStepTemplate(routeTemplateId, "STEP-LIMITED", "Limited step", 1, "WORKER", false, true, true);
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1007L,
                507L,
                "Limited item",
                "GENERAL",
                new BigDecimal("2.00")));

        dispatchService.dispatch(1007L, request(routeTemplateId, List.of(
                step(stepTemplateId, "STEP-LIMITED", "Limited step", 1, "WORKER"))));

        OrderItemProductionContext orderItem = orderItemAdapter.getDemoOrderItem(1007L);
        assertThat(orderItem.id()).isEqualTo(1007L);
        assertThat(orderItem.orderId()).isEqualTo(507L);
        assertThat(orderItem.itemName()).isEqualTo("Limited item");
        assertThat(orderItem.productType()).isEqualTo("GENERAL");
        assertThat(orderItem.quantity()).isEqualByComparingTo("2.00");
        assertThat(orderItem.productionStatus()).isEqualTo("DISPATCHED");
        assertThat(orderItem.productionProgress()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(orderItem.productionRouteInstanceId()).isNotNull();
    }

    private DispatchProductionRequest request(Long routeTemplateId, List<DispatchStepRequest> steps) {
        return new DispatchProductionRequest(routeTemplateId, null, null, steps);
    }

    private DispatchStepRequest step(
            Long sourceStepTemplateId,
            String stepCode,
            String stepName,
            Integer stepOrder,
            String assignedRole) {
        return new DispatchStepRequest(
                "client-" + stepOrder,
                sourceStepTemplateId,
                stepCode,
                stepName,
                stepOrder,
                assignedRole,
                null,
                false,
                true,
                true,
                BigDecimal.ONE,
                stepName + " instruction");
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
