package com.zhisheng.mvp.production.adapter;

import com.zhisheng.mvp.production.exception.ProductionDispatchException;
import com.zhisheng.mvp.production.port.OrderItemCandidateContext;
import com.zhisheng.mvp.production.port.OrderItemCandidateQuery;
import com.zhisheng.mvp.production.port.OrderItemCandidateReadPort;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.port.OrderItemProductionPort;
import com.zhisheng.mvp.production.port.OrderItemReadPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile({"dev", "test"})
public class MockOrderItemAdapter implements OrderItemReadPort, OrderItemProductionPort, OrderItemCandidateReadPort {

    private final Map<Long, OrderItemProductionContext> orderItems = new ConcurrentHashMap<>();
    private final Map<Long, OrderItemCandidateContext> candidates = new ConcurrentHashMap<>();
    private final AtomicInteger productionWriteBackCount = new AtomicInteger();

    public MockOrderItemAdapter() {
        seedDemoOrderItems();
    }

    // TODO: replace with customer-line order_item contract
    @Override
    public Optional<OrderItemProductionContext> findById(Long orderItemId) {
        return Optional.ofNullable(orderItems.get(orderItemId));
    }

    // TODO: replace with customer-line project_order/order_item read contract
    @Override
    public List<OrderItemCandidateContext> listCandidates(OrderItemCandidateQuery query) {
        return candidates.values().stream()
                .filter(candidate -> matches(candidate, query))
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
    }

    // TODO: replace with customer-line project_order/order_item read contract
    @Override
    public Optional<OrderItemCandidateContext> findCandidateById(Long orderItemId) {
        return Optional.ofNullable(candidates.get(orderItemId));
    }

    // TODO: replace with customer-line order_item contract
    @Override
    public void markDispatched(
            Long orderItemId,
            String productionStatus,
            BigDecimal productionProgress,
            Long routeInstanceId) {
        productionWriteBackCount.incrementAndGet();
        OrderItemProductionContext existing = orderItems.get(orderItemId);
        if (existing == null) {
            throw new ProductionDispatchException("ORDER_ITEM_NOT_FOUND");
        }
        OrderItemProductionContext updated = existing.withProductionFields(
                productionStatus,
                productionProgress,
                routeInstanceId);
        orderItems.put(orderItemId, updated);
        syncCandidate(updated);
    }

    // TODO: replace with customer-line order_item contract
    @Override
    public void updateProductionProgress(
            Long orderItemId,
            String productionStatus,
            BigDecimal productionProgress) {
        productionWriteBackCount.incrementAndGet();
        OrderItemProductionContext existing = orderItems.get(orderItemId);
        if (existing == null) {
            throw new ProductionDispatchException("ORDER_ITEM_NOT_FOUND");
        }
        OrderItemProductionContext updated = existing.withProductionFields(
                productionStatus,
                productionProgress,
                existing.productionRouteInstanceId());
        orderItems.put(orderItemId, updated);
        syncCandidate(updated);
    }

    public void putDemoOrderItem(OrderItemProductionContext orderItem) {
        orderItems.put(orderItem.id(), orderItem);
        candidates.put(orderItem.id(), new OrderItemCandidateContext(
                orderItem.id(),
                1L,
                orderItem.orderId(),
                "ORD-" + orderItem.orderId(),
                "PROJECT",
                "ENTERPRISE",
                null,
                null,
                orderItem.itemName(),
                null,
                null,
                orderItem.quantity(),
                null,
                orderItem.productType(),
                orderItem.productionStatus(),
                orderItem.productionProgress(),
                orderItem.productionRouteInstanceId()));
    }

    public void putDemoOrderItem(OrderItemCandidateContext candidate) {
        candidates.put(candidate.id(), candidate);
        orderItems.put(candidate.id(), candidate.toProductionContext());
    }

    public OrderItemProductionContext getDemoOrderItem(Long orderItemId) {
        return orderItems.get(orderItemId);
    }

    public void reset() {
        orderItems.clear();
        candidates.clear();
        productionWriteBackCount.set(0);
    }

    public int productionWriteBackCount() {
        return productionWriteBackCount.get();
    }

    public void resetProductionWriteBackCount() {
        productionWriteBackCount.set(0);
    }

    private boolean matches(OrderItemCandidateContext candidate, OrderItemCandidateQuery query) {
        if (query == null) {
            return true;
        }
        if (StringUtils.hasText(query.productType())
                && !query.productType().equals(candidate.productType())) {
            return false;
        }
        if (StringUtils.hasText(query.productionStatus())
                && !query.productionStatus().equals(candidate.productionStatus())) {
            return false;
        }
        if (StringUtils.hasText(query.orderNo())
                && (candidate.orderNo() == null || !candidate.orderNo().contains(query.orderNo()))) {
            return false;
        }
        if (StringUtils.hasText(query.orderType())
                && !query.orderType().equals(candidate.orderType())) {
            return false;
        }
        if (StringUtils.hasText(query.customerType())
                && !query.customerType().equals(candidate.customerType())) {
            return false;
        }
        if (!StringUtils.hasText(query.keyword())) {
            return true;
        }
        String keyword = query.keyword();
        return contains(candidate.itemName(), keyword)
                || contains(candidate.remark(), keyword)
                || contains(candidate.spec(), keyword)
                || contains(candidate.orderNo(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private void syncCandidate(OrderItemProductionContext updated) {
        OrderItemCandidateContext existing = candidates.get(updated.id());
        if (existing == null) {
            putDemoOrderItem(updated);
            return;
        }
        candidates.put(updated.id(), new OrderItemCandidateContext(
                existing.id(),
                existing.tenantId(),
                existing.orderId(),
                existing.orderNo(),
                existing.orderType(),
                existing.customerType(),
                existing.dealOwnerId(),
                existing.dealOwnerName(),
                existing.itemName(),
                existing.spec(),
                existing.unit(),
                existing.quantity(),
                existing.remark(),
                existing.productType(),
                updated.productionStatus(),
                updated.productionProgress(),
                updated.productionRouteInstanceId()));
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
