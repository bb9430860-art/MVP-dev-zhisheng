package com.zhisheng.mvp.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zhisheng.mvp.production.adapter.MockOrderItemAdapter;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCreateRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderMaterialRequest;
import com.zhisheng.mvp.production.entity.ProductionWorkOrder;
import com.zhisheng.mvp.production.exception.ProductionWorkOrderException;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.service.ProductionWorkOrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class ProductionWorkOrderServiceTest {

    @Autowired
    private ProductionWorkOrderService workOrderService;

    @Autowired
    private MockOrderItemAdapter orderItemAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from production_work_order_material");
        jdbcTemplate.update("delete from production_work_order");
        jdbcTemplate.update("delete from production_step_instance");
        jdbcTemplate.update("delete from production_route_instance");
        orderItemAdapter.reset();
    }

    @Test
    void createFromOrderItemCreatesDraftWorkOrderWithSnapshotMaterialsAndNumber() {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1001L,
                501L,
                "Entrance spirit fortress",
                "SPIRIT_FORTRESS",
                new BigDecimal("2.00")));

        ProductionWorkOrder workOrder = workOrderService.createFromOrderItem(createRequest(1001L), 201L);

        assertThat(workOrder.getId()).isNotNull();
        assertThat(workOrder.getWorkOrderNo()).matches("WO-\\d{8}-\\d{4}");
        assertThat(workOrder.getWorkOrderNo()).isEqualTo("WO-" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-0001");
        assertThat(workOrder.getOrderId()).isEqualTo(501L);
        assertThat(workOrder.getOrderItemId()).isEqualTo(1001L);
        assertThat(workOrder.getOrderItemNameSnapshot()).isEqualTo("Entrance spirit fortress");
        assertThat(workOrder.getProductTypeSnapshot()).isEqualTo("SPIRIT_FORTRESS");
        assertThat(workOrder.getQuantitySnapshot()).isEqualByComparingTo("2.00");
        assertThat(workOrder.getStatus()).isEqualTo("DRAFT");
        assertThat(workOrder.getTechnicalConfigJson()).contains("cncSystem");

        Integer materialCount = jdbcTemplate.queryForObject(
                "select count(*) from production_work_order_material where work_order_id = ?",
                Integer.class,
                workOrder.getId());
        assertThat(materialCount).isEqualTo(1);

        Map<String, Object> material = jdbcTemplate.queryForMap(
                "select * from production_work_order_material where work_order_id = ?",
                workOrder.getId());
        assertThat(material.get("MATERIAL_NAME")).isEqualTo("Galvanized sheet");
        assertThat((BigDecimal) material.get("REQUIRED_QTY")).isEqualByComparingTo("5.50");
        assertThat(material.get("REQUIREMENT_STATUS")).isEqualTo("DRAFT");
    }

    @Test
    void duplicateActiveWorkOrderForSameTenantAndOrderItemIsRejected() {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1002L,
                502L,
                "Duplicate item",
                "GENERAL",
                BigDecimal.ONE));
        workOrderService.createFromOrderItem(createRequest(1002L), 201L);

        assertThatThrownBy(() -> workOrderService.createFromOrderItem(createRequest(1002L), 201L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM");
    }

    @Test
    void completedAndCancelledWorkOrdersAreNonActiveForUniqueness() {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1003L,
                503L,
                "Reusable item",
                "GENERAL",
                BigDecimal.ONE));
        ProductionWorkOrder completed = workOrderService.createFromOrderItem(createRequest(1003L), 201L);
        workOrderService.release(completed.getId(), 201L);
        workOrderService.markInProgress(completed.getId(), 201L);
        workOrderService.complete(completed.getId(), 201L);

        ProductionWorkOrder second = workOrderService.createFromOrderItem(createRequest(1003L), 201L);
        workOrderService.cancel(second.getId(), 201L);

        ProductionWorkOrder third = workOrderService.createFromOrderItem(createRequest(1003L), 201L);

        assertThat(third.getId()).isNotEqualTo(completed.getId());
        assertThat(third.getStatus()).isEqualTo("DRAFT");
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from production_work_order where order_item_id = ?",
                Integer.class,
                1003L);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void releaseStartCompleteAndCancelFollowStateMachine() {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1004L,
                504L,
                "State item",
                "GENERAL",
                BigDecimal.ONE));
        ProductionWorkOrder draft = workOrderService.createFromOrderItem(createRequest(1004L), 201L);

        ProductionWorkOrder released = workOrderService.release(draft.getId(), 202L);
        assertThat(released.getStatus()).isEqualTo("RELEASED");
        assertThat(released.getReleasedBy()).isEqualTo(202L);
        assertThat(released.getReleasedAt()).isNotNull();

        assertThatThrownBy(() -> workOrderService.transition(released.getId(), "DRAFT", 202L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("WORK_ORDER_INVALID_STATUS_TRANSITION");

        ProductionWorkOrder inProgress = workOrderService.markInProgress(released.getId(), 203L);
        assertThat(inProgress.getStatus()).isEqualTo("IN_PROGRESS");

        assertThatThrownBy(() -> workOrderService.cancel(inProgress.getId(), 203L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("WORK_ORDER_INVALID_STATUS_TRANSITION");

        ProductionWorkOrder completed = workOrderService.complete(inProgress.getId(), 204L);
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");

        assertThatThrownBy(() -> workOrderService.cancel(completed.getId(), 204L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("WORK_ORDER_COMPLETED");
    }

    @Test
    void cancelDraftAndReleasedWorkOrders() {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1005L,
                505L,
                "Draft cancel item",
                "GENERAL",
                BigDecimal.ONE));
        ProductionWorkOrder draft = workOrderService.createFromOrderItem(createRequest(1005L), 201L);
        assertThat(workOrderService.cancel(draft.getId(), 201L).getStatus()).isEqualTo("CANCELLED");

        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1006L,
                506L,
                "Released cancel item",
                "GENERAL",
                BigDecimal.ONE));
        ProductionWorkOrder released = workOrderService.release(
                workOrderService.createFromOrderItem(createRequest(1006L), 201L).getId(),
                201L);
        assertThat(workOrderService.cancel(released.getId(), 201L).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void materialRequirementRequiresNameAndPositiveQuantity() {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1007L,
                507L,
                "Material item",
                "GENERAL",
                BigDecimal.ONE));

        assertThatThrownBy(() -> workOrderService.createFromOrderItem(createRequest(
                        1007L,
                        List.of(new ProductionWorkOrderMaterialRequest(
                                null,
                                null,
                                " ",
                                "1.2mm",
                                "sheet",
                                BigDecimal.ONE,
                                "Cut",
                                null,
                                null,
                                null))),
                        201L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("MATERIAL_REQUIREMENT_INVALID");

        assertThatThrownBy(() -> workOrderService.createFromOrderItem(createRequest(
                        1007L,
                        List.of(new ProductionWorkOrderMaterialRequest(
                                null,
                                null,
                                "Steel",
                                "1.2mm",
                                "sheet",
                                BigDecimal.ZERO,
                                "Cut",
                                null,
                                null,
                                null))),
                        201L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("MATERIAL_REQUIREMENT_INVALID");
    }

    @Test
    void createWorkOrderDoesNotModifyOrderCoreOrCreateInventoryTables() {
        OrderItemProductionContext original = OrderItemProductionContext.notDispatched(
                1008L,
                508L,
                "Order boundary item",
                "GENERAL",
                new BigDecimal("3.00"));
        orderItemAdapter.putDemoOrderItem(original);

        workOrderService.createFromOrderItem(createRequest(1008L), 201L);

        OrderItemProductionContext after = orderItemAdapter.getDemoOrderItem(1008L);
        assertThat(after.id()).isEqualTo(original.id());
        assertThat(after.orderId()).isEqualTo(original.orderId());
        assertThat(after.itemName()).isEqualTo(original.itemName());
        assertThat(after.productType()).isEqualTo(original.productType());
        assertThat(after.quantity()).isEqualByComparingTo(original.quantity());
        assertThat(after.productionStatus()).isEqualTo(original.productionStatus());
        assertThat(after.productionProgress()).isEqualByComparingTo(original.productionProgress());
        assertThat(after.productionRouteInstanceId()).isEqualTo(original.productionRouteInstanceId());
        assertThat(tableExists("inventory_transaction")).isFalse();
    }

    @Test
    void linkRouteInstanceRequiresSameTenantAndOrderItemAndDoesNotMutateFrozenRouteStructure() {
        orderItemAdapter.putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1009L,
                509L,
                "Link item",
                "GENERAL",
                BigDecimal.ONE));
        ProductionWorkOrder workOrder = workOrderService.release(
                workOrderService.createFromOrderItem(createRequest(1009L), 201L).getId(),
                201L);
        long routeId = insertRoute(1009L, 1L, true);
        long mismatchedOrderItemRouteId = insertRoute(9999L, 1L, true);
        long mismatchedTenantRouteId = insertRoute(1009L, 2L, true);
        Map<String, Object> routeBefore = routeRow(routeId);

        assertThatThrownBy(() -> workOrderService.linkRouteInstance(workOrder.getId(), mismatchedOrderItemRouteId, 201L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("WORK_ORDER_ROUTE_LINK_CONFLICT");
        assertThatThrownBy(() -> workOrderService.linkRouteInstance(workOrder.getId(), mismatchedTenantRouteId, 201L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("WORK_ORDER_ROUTE_LINK_CONFLICT");

        ProductionWorkOrder linked = workOrderService.linkRouteInstance(workOrder.getId(), routeId, 201L);

        assertThat(linked.getProductionRouteInstanceId()).isEqualTo(routeId);
        Map<String, Object> routeAfter = routeRow(routeId);
        assertThat(routeAfter.get("FROZEN")).isEqualTo(routeBefore.get("FROZEN"));
        assertThat(routeAfter.get("ROUTE_NAME_SNAPSHOT")).isEqualTo(routeBefore.get("ROUTE_NAME_SNAPSHOT"));
        assertThat(routeAfter.get("STATUS")).isEqualTo(routeBefore.get("STATUS"));
    }

    @Test
    void missingOrderItemReturnsOrderItemNotFound() {
        assertThatThrownBy(() -> workOrderService.createFromOrderItem(createRequest(404L), 201L))
                .isInstanceOf(ProductionWorkOrderException.class)
                .hasMessage("ORDER_ITEM_NOT_FOUND");
    }

    private ProductionWorkOrderCreateRequest createRequest(Long orderItemId) {
        return createRequest(orderItemId, List.of(new ProductionWorkOrderMaterialRequest(
                301L,
                "MAT-001",
                "Galvanized sheet",
                "1.2mm",
                "sheet",
                new BigDecimal("5.50"),
                "Cut",
                null,
                null,
                "prepare before cutting")));
    }

    private ProductionWorkOrderCreateRequest createRequest(
            Long orderItemId,
            List<ProductionWorkOrderMaterialRequest> materials) {
        return new ProductionWorkOrderCreateRequest(
                orderItemId,
                "NORMAL",
                "Production instruction",
                "Follow confirmed drawing",
                "Produce according to approved file",
                "QC before packing",
                "Wooden crate",
                "Deliver to site",
                "Deliver before installation",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(10),
                "urgent if delayed",
                "ZC-100",
                "standard CNC config",
                "technical remark",
                "{\"cncSystem\":\"standard\"}",
                11L,
                12L,
                13L,
                null,
                false,
                "acceptance after install",
                materials);
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
                509L,
                orderItemId,
                301L,
                "RT-WO",
                "Work order route",
                "DISPATCHED",
                frozen);
        return jdbcTemplate.queryForObject(
                "select max(id) from production_route_instance",
                Long.class);
    }

    private Map<String, Object> routeRow(Long routeId) {
        return jdbcTemplate.queryForMap("select * from production_route_instance where id = ?", routeId);
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
}
