package com.zhisheng.mvp.production.adapter;

import com.zhisheng.mvp.production.exception.ProductionDispatchException;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.port.OrderItemProductionPort;
import com.zhisheng.mvp.production.port.OrderItemReadPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class MockOrderItemAdapter implements OrderItemReadPort, OrderItemProductionPort {

    private final Map<Long, OrderItemProductionContext> orderItems = new ConcurrentHashMap<>();

    public MockOrderItemAdapter() {
        seedDemoOrderItems();
    }

    // TODO: replace with customer-line order_item contract
    @Override
    public Optional<OrderItemProductionContext> findById(Long orderItemId) {
        return Optional.ofNullable(orderItems.get(orderItemId));
    }

    // TODO: replace with customer-line order_item contract
    @Override
    public void markDispatched(
            Long orderItemId,
            String productionStatus,
            BigDecimal productionProgress,
            Long routeInstanceId) {
        OrderItemProductionContext existing = orderItems.get(orderItemId);
        if (existing == null) {
            throw new ProductionDispatchException("ORDER_ITEM_NOT_FOUND");
        }
        orderItems.put(orderItemId, existing.withProductionFields(
                productionStatus,
                productionProgress,
                routeInstanceId));
    }

    // TODO: replace with customer-line order_item contract
    @Override
    public void updateProductionProgress(
            Long orderItemId,
            String productionStatus,
            BigDecimal productionProgress) {
        OrderItemProductionContext existing = orderItems.get(orderItemId);
        if (existing == null) {
            throw new ProductionDispatchException("ORDER_ITEM_NOT_FOUND");
        }
        orderItems.put(orderItemId, existing.withProductionFields(
                productionStatus,
                productionProgress,
                existing.productionRouteInstanceId()));
    }

    public void putDemoOrderItem(OrderItemProductionContext orderItem) {
        orderItems.put(orderItem.id(), orderItem);
    }

    public OrderItemProductionContext getDemoOrderItem(Long orderItemId) {
        return orderItems.get(orderItemId);
    }

    public void reset() {
        orderItems.clear();
    }

    private void seedDemoOrderItems() {
        putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1001L,
                501L,
                "入口精神堡垒",
                "SPIRIT_FORTRESS",
                BigDecimal.ONE));
        putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1002L,
                501L,
                "楼层牌 A 栋",
                "FLOOR_SIGN",
                BigDecimal.ONE));
        putDemoOrderItem(OrderItemProductionContext.notDispatched(
                1003L,
                501L,
                "发光字门头",
                "ILLUMINATED_LETTER",
                BigDecimal.ONE));
    }
}
