package com.zhisheng.mvp.production.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhisheng.mvp.process.entity.ProcessRouteTemplate;
import com.zhisheng.mvp.process.entity.ProcessStepTemplate;
import com.zhisheng.mvp.process.mapper.ProcessRouteTemplateMapper;
import com.zhisheng.mvp.process.mapper.ProcessStepTemplateMapper;
import com.zhisheng.mvp.production.dto.DispatchConfigFromTemplateRequest;
import com.zhisheng.mvp.production.dto.DispatchConfigResponse;
import com.zhisheng.mvp.production.dto.DispatchConfigStepResponse;
import com.zhisheng.mvp.production.dto.DispatchProductionRequest;
import com.zhisheng.mvp.production.dto.DispatchStepRequest;
import com.zhisheng.mvp.production.dto.OrderItemConfigContextResponse;
import com.zhisheng.mvp.production.dto.OrderItemProductionResponse;
import com.zhisheng.mvp.production.dto.ProductionDispatchResponse;
import com.zhisheng.mvp.production.dto.ProductionSummaryResponse;
import com.zhisheng.mvp.production.dto.WorkOrderDispatchContextResponse;
import com.zhisheng.mvp.production.entity.ProductionRouteInstance;
import com.zhisheng.mvp.production.entity.ProductionStepInstance;
import com.zhisheng.mvp.production.entity.ProductionWorkOrder;
import com.zhisheng.mvp.production.enums.ProductionWorkOrderStatus;
import com.zhisheng.mvp.production.exception.ProductionDispatchException;
import com.zhisheng.mvp.production.mapper.ProductionRouteInstanceMapper;
import com.zhisheng.mvp.production.mapper.ProductionStepInstanceMapper;
import com.zhisheng.mvp.production.mapper.ProductionWorkOrderMapper;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.port.OrderItemProductionPort;
import com.zhisheng.mvp.production.port.OrderItemReadPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Profile({"dev", "test"})
public class ProductionDispatchService {

    private static final long DEFAULT_TENANT_ID = 1L;
    private static final String STATUS_DISPATCHED = "DISPATCHED";
    private static final String STEP_STATUS_PENDING = "PENDING";

    private final OrderItemReadPort orderItemReadPort;
    private final OrderItemProductionPort orderItemProductionPort;
    private final ProcessRouteTemplateMapper routeTemplateMapper;
    private final ProcessStepTemplateMapper stepTemplateMapper;
    private final ProductionRouteInstanceMapper routeInstanceMapper;
    private final ProductionStepInstanceMapper stepInstanceMapper;
    private final ProductionWorkOrderMapper workOrderMapper;
    private final ProductionWorkOrderService workOrderService;

    public ProductionDispatchService(
            OrderItemReadPort orderItemReadPort,
            OrderItemProductionPort orderItemProductionPort,
            ProcessRouteTemplateMapper routeTemplateMapper,
            ProcessStepTemplateMapper stepTemplateMapper,
            ProductionRouteInstanceMapper routeInstanceMapper,
            ProductionStepInstanceMapper stepInstanceMapper,
            ProductionWorkOrderMapper workOrderMapper,
            ProductionWorkOrderService workOrderService) {
        this.orderItemReadPort = orderItemReadPort;
        this.orderItemProductionPort = orderItemProductionPort;
        this.routeTemplateMapper = routeTemplateMapper;
        this.stepTemplateMapper = stepTemplateMapper;
        this.routeInstanceMapper = routeInstanceMapper;
        this.stepInstanceMapper = stepInstanceMapper;
        this.workOrderMapper = workOrderMapper;
        this.workOrderService = workOrderService;
    }

    @Transactional(readOnly = true)
    public OrderItemConfigContextResponse configContext(Long orderItemId) {
        OrderItemProductionContext orderItem = requiredOrderItem(orderItemId);
        return new OrderItemConfigContextResponse(
                OrderItemProductionResponse.from(orderItem),
                isDispatched(orderItem));
    }

    @Transactional(readOnly = true)
    public DispatchConfigResponse createConfigFromTemplate(
            Long orderItemId,
            DispatchConfigFromTemplateRequest request) {
        OrderItemProductionContext orderItem = requiredOrderItem(orderItemId);
        ensureNotDispatched(orderItem);
        if (request == null || request.routeTemplateId() == null) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_NOT_AVAILABLE");
        }
        ProcessRouteTemplate routeTemplate = availableRouteTemplate(request.routeTemplateId());
        List<ProcessStepTemplate> steps = activeTemplateSteps(routeTemplate.getId());
        if (steps.isEmpty()) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_HAS_NO_ENABLED_STEPS");
        }

        return new DispatchConfigResponse(
                routeTemplate.getId(),
                routeTemplate.getRouteCode(),
                routeTemplate.getRouteName(),
                routeTemplate.getProductType(),
                routeTemplate.getDescription(),
                steps.stream()
                        .map(this::toDispatchConfigStep)
                        .toList());
    }

    @Transactional(readOnly = true)
    public WorkOrderDispatchContextResponse workOrderDispatchContext(Long workOrderId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        OrderItemProductionContext orderItem = requiredOrderItem(workOrder.getOrderItemId());
        ensureWorkOrderMatchesOrderItem(workOrder, orderItem);
        return new WorkOrderDispatchContextResponse(
                workOrderService.detail(workOrderId),
                OrderItemProductionResponse.from(orderItem),
                workOrder.getProductionRouteInstanceId() != null || isDispatched(orderItem));
    }

    @Transactional(readOnly = true)
    public DispatchConfigResponse createWorkOrderConfigFromTemplate(
            Long workOrderId,
            DispatchConfigFromTemplateRequest request) {
        requiredDispatchableWorkOrder(workOrderId);
        if (request == null || request.routeTemplateId() == null) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_NOT_AVAILABLE");
        }
        ProcessRouteTemplate routeTemplate = availableRouteTemplate(request.routeTemplateId());
        List<ProcessStepTemplate> steps = activeTemplateSteps(routeTemplate.getId());
        if (steps.isEmpty()) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_HAS_NO_ENABLED_STEPS");
        }

        return new DispatchConfigResponse(
                routeTemplate.getId(),
                routeTemplate.getRouteCode(),
                routeTemplate.getRouteName(),
                routeTemplate.getProductType(),
                routeTemplate.getDescription(),
                steps.stream()
                        .map(this::toDispatchConfigStep)
                        .toList());
    }

    @Transactional(readOnly = true)
    public ProductionSummaryResponse summary(Long orderItemId) {
        OrderItemProductionContext orderItem = requiredOrderItem(orderItemId);
        ProductionRouteInstance routeInstance = activeRouteInstance(orderItem.id());
        if (routeInstance == null) {
            return new ProductionSummaryResponse(
                    orderItem.id(),
                    orderItem.productionStatus(),
                    orderItem.productionRouteInstanceId(),
                    orderItem.productionProgress(),
                    0,
                    0,
                    null,
                    false,
                    false);
        }

        List<ProductionStepInstance> steps = stepInstanceMapper.selectList(
                new LambdaQueryWrapper<ProductionStepInstance>()
                        .eq(ProductionStepInstance::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ProductionStepInstance::getRouteInstanceId, routeInstance.getId())
                        .eq(ProductionStepInstance::getDeleted, false)
                        .orderByAsc(ProductionStepInstance::getStepOrder)
                        .orderByAsc(ProductionStepInstance::getId));
        int completedSteps = (int) steps.stream()
                .filter(step -> "COMPLETED".equals(step.getStatus()))
                .count();
        String currentStepName = steps.stream()
                .filter(step -> !"COMPLETED".equals(step.getStatus()))
                .map(ProductionStepInstance::getStepName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new ProductionSummaryResponse(
                orderItem.id(),
                orderItem.productionStatus(),
                routeInstance.getId(),
                routeInstance.getProductionProgress(),
                steps.size(),
                completedSteps,
                currentStepName,
                true,
                routeInstance.getFrozen());
    }

    @Transactional
    public ProductionDispatchResponse dispatch(Long orderItemId, DispatchProductionRequest request) {
        if (orderItemId == null) {
            throw new ProductionDispatchException("ORDER_ITEM_NOT_FOUND");
        }
        if (request == null || request.routeTemplateId() == null) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_NOT_AVAILABLE");
        }
        if (request.steps() == null || request.steps().isEmpty()) {
            throw new ProductionDispatchException("DISPATCH_STEPS_REQUIRED");
        }

        OrderItemProductionContext orderItem = requiredOrderItem(orderItemId);
        ensureNotDispatched(orderItem);

        ProcessRouteTemplate routeTemplate = availableRouteTemplate(request.routeTemplateId());
        ensureTemplateHasEnabledSteps(routeTemplate.getId());
        List<DispatchStepRequest> steps = validateAndNormalizeSteps(routeTemplate.getId(), request.steps());

        ProductionRouteInstance routeInstance = createRouteInstance(orderItem, routeTemplate, request);
        try {
            routeInstanceMapper.insert(routeInstance);
        } catch (DuplicateKeyException exception) {
            throw new ProductionDispatchException("ORDER_ITEM_ALREADY_DISPATCHED");
        }

        for (DispatchStepRequest step : steps) {
            stepInstanceMapper.insert(createStepInstance(orderItem, routeInstance.getId(), step));
        }

        orderItemProductionPort.markDispatched(
                orderItemId,
                STATUS_DISPATCHED,
                BigDecimal.ZERO,
                routeInstance.getId());

        return new ProductionDispatchResponse(
                routeInstance.getId(),
                orderItemId,
                STATUS_DISPATCHED,
                true,
                steps.size());
    }

    @Transactional
    public ProductionDispatchResponse dispatchWorkOrder(
            Long workOrderId,
            DispatchProductionRequest request,
            Long operatorId) {
        if (request == null || request.routeTemplateId() == null) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_NOT_AVAILABLE");
        }
        if (request.steps() == null || request.steps().isEmpty()) {
            throw new ProductionDispatchException("DISPATCH_STEPS_REQUIRED");
        }

        ProductionWorkOrder workOrder = requiredDispatchableWorkOrder(workOrderId);
        OrderItemProductionContext orderItem = requiredOrderItem(workOrder.getOrderItemId());
        ensureWorkOrderMatchesOrderItem(workOrder, orderItem);
        ensureOrderItemRouteNotLinkedToAnotherInstance(workOrder, orderItem);

        ProcessRouteTemplate routeTemplate = availableRouteTemplate(request.routeTemplateId());
        ensureTemplateHasEnabledSteps(routeTemplate.getId());
        List<DispatchStepRequest> steps = validateAndNormalizeSteps(routeTemplate.getId(), request.steps());

        ProductionRouteInstance routeInstance = createRouteInstance(orderItem, routeTemplate, request, operatorId);
        try {
            routeInstanceMapper.insert(routeInstance);
        } catch (DuplicateKeyException exception) {
            throw new ProductionDispatchException("WORK_ORDER_ALREADY_DISPATCHED");
        }

        for (DispatchStepRequest step : steps) {
            stepInstanceMapper.insert(createStepInstance(orderItem, routeInstance.getId(), step, operatorId));
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getId, workOrder.getId())
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionWorkOrder::getStatus, ProductionWorkOrderStatus.RELEASED.name())
                .isNull(ProductionWorkOrder::getProductionRouteInstanceId)
                .eq(ProductionWorkOrder::getDeleted, false)
                .set(ProductionWorkOrder::getProductionRouteInstanceId, routeInstance.getId())
                .set(ProductionWorkOrder::getStatus, ProductionWorkOrderStatus.IN_PROGRESS.name())
                .set(ProductionWorkOrder::getUpdatedAt, now)
                .set(ProductionWorkOrder::getUpdatedBy, operatorId));
        if (updated != 1) {
            throw new ProductionDispatchException("WORK_ORDER_ALREADY_DISPATCHED");
        }

        orderItemProductionPort.markDispatched(
                orderItem.id(),
                STATUS_DISPATCHED,
                BigDecimal.ZERO,
                routeInstance.getId());

        return new ProductionDispatchResponse(
                routeInstance.getId(),
                orderItem.id(),
                STATUS_DISPATCHED,
                true,
                steps.size());
    }

    private OrderItemProductionContext requiredOrderItem(Long orderItemId) {
        if (orderItemId == null) {
            throw new ProductionDispatchException("ORDER_ITEM_NOT_FOUND");
        }
        return orderItemReadPort.findById(orderItemId)
                .orElseThrow(() -> new ProductionDispatchException("ORDER_ITEM_NOT_FOUND"));
    }

    private ProductionWorkOrder requiredWorkOrder(Long workOrderId) {
        if (workOrderId == null) {
            throw new ProductionDispatchException("WORK_ORDER_NOT_FOUND");
        }
        ProductionWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null
                || !Long.valueOf(DEFAULT_TENANT_ID).equals(workOrder.getTenantId())
                || Boolean.TRUE.equals(workOrder.getDeleted())) {
            throw new ProductionDispatchException("WORK_ORDER_NOT_FOUND");
        }
        return workOrder;
    }

    private ProductionWorkOrder requiredDispatchableWorkOrder(Long workOrderId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        if (workOrder.getProductionRouteInstanceId() != null) {
            throw new ProductionDispatchException("WORK_ORDER_ALREADY_DISPATCHED");
        }
        if (ProductionWorkOrderStatus.CANCELLED.name().equals(workOrder.getStatus())) {
            throw new ProductionDispatchException("WORK_ORDER_CANCELLED");
        }
        if (ProductionWorkOrderStatus.COMPLETED.name().equals(workOrder.getStatus())) {
            throw new ProductionDispatchException("WORK_ORDER_COMPLETED");
        }
        if (ProductionWorkOrderStatus.IN_PROGRESS.name().equals(workOrder.getStatus())) {
            throw new ProductionDispatchException("WORK_ORDER_ALREADY_DISPATCHED");
        }
        if (!ProductionWorkOrderStatus.RELEASED.name().equals(workOrder.getStatus())) {
            throw new ProductionDispatchException("WORK_ORDER_NOT_RELEASED");
        }
        return workOrder;
    }

    private void ensureWorkOrderMatchesOrderItem(
            ProductionWorkOrder workOrder,
            OrderItemProductionContext orderItem) {
        if (!workOrder.getOrderItemId().equals(orderItem.id())
                || !workOrder.getOrderId().equals(orderItem.orderId())) {
            throw new ProductionDispatchException("WORK_ORDER_ROUTE_LINK_CONFLICT");
        }
    }

    private void ensureOrderItemRouteNotLinkedToAnotherInstance(
            ProductionWorkOrder workOrder,
            OrderItemProductionContext orderItem) {
        if (orderItem.productionRouteInstanceId() == null) {
            return;
        }
        if (!orderItem.productionRouteInstanceId().equals(workOrder.getProductionRouteInstanceId())) {
            throw new ProductionDispatchException("WORK_ORDER_ROUTE_LINK_CONFLICT");
        }
    }

    private ProcessRouteTemplate availableRouteTemplate(Long routeTemplateId) {
        ProcessRouteTemplate routeTemplate = routeTemplateMapper.selectById(routeTemplateId);
        if (routeTemplate == null
                || !Boolean.TRUE.equals(routeTemplate.getEnabled())
                || Boolean.TRUE.equals(routeTemplate.getDeleted())) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_NOT_AVAILABLE");
        }
        return routeTemplate;
    }

    private void ensureNotDispatched(OrderItemProductionContext orderItem) {
        if (isDispatched(orderItem)) {
            throw new ProductionDispatchException("ORDER_ITEM_ALREADY_DISPATCHED");
        }
    }

    private boolean isDispatched(OrderItemProductionContext orderItem) {
        if (orderItem.productionRouteInstanceId() != null || STATUS_DISPATCHED.equals(orderItem.productionStatus())) {
            return true;
        }
        return activeRouteInstance(orderItem.id()) != null;
    }

    private ProductionRouteInstance activeRouteInstance(Long orderItemId) {
        return routeInstanceMapper.selectOne(new LambdaQueryWrapper<ProductionRouteInstance>()
                .eq(ProductionRouteInstance::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionRouteInstance::getOrderItemId, orderItemId)
                .eq(ProductionRouteInstance::getDeleted, false)
                .last("limit 1"));
    }

    private void ensureTemplateHasEnabledSteps(Long routeTemplateId) {
        Long activeStepCount = stepTemplateMapper.selectCount(new LambdaQueryWrapper<ProcessStepTemplate>()
                .eq(ProcessStepTemplate::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProcessStepTemplate::getRouteTemplateId, routeTemplateId)
                .eq(ProcessStepTemplate::getEnabled, true)
                .eq(ProcessStepTemplate::getDeleted, false));
        if (activeStepCount == 0) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_HAS_NO_ENABLED_STEPS");
        }
    }

    private List<ProcessStepTemplate> activeTemplateSteps(Long routeTemplateId) {
        return stepTemplateMapper.selectList(new LambdaQueryWrapper<ProcessStepTemplate>()
                .eq(ProcessStepTemplate::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProcessStepTemplate::getRouteTemplateId, routeTemplateId)
                .eq(ProcessStepTemplate::getEnabled, true)
                .eq(ProcessStepTemplate::getDeleted, false)
                .orderByAsc(ProcessStepTemplate::getStepOrder)
                .orderByAsc(ProcessStepTemplate::getId));
    }

    private DispatchConfigStepResponse toDispatchConfigStep(ProcessStepTemplate step) {
        return new DispatchConfigStepResponse(
                "template-step-" + step.getId(),
                step.getId(),
                step.getStepCode(),
                step.getStepName(),
                step.getStepOrder(),
                step.getAssignedRole(),
                null,
                step.getPhotoRequired(),
                step.getRemarkRequired(),
                step.getMobileEnabled(),
                step.getEstimatedHours(),
                step.getOperationInstruction());
    }

    private List<DispatchStepRequest> validateAndNormalizeSteps(
            Long routeTemplateId,
            List<DispatchStepRequest> steps) {
        Set<Integer> orders = new HashSet<>();
        for (DispatchStepRequest step : steps) {
            if (step == null) {
                throw new ProductionDispatchException("DISPATCH_STEPS_REQUIRED");
            }
            if (!StringUtils.hasText(step.stepName())) {
                throw new ProductionDispatchException("STEP_NAME_REQUIRED");
            }
            if (!StringUtils.hasText(step.assignedRole())) {
                throw new ProductionDispatchException("ASSIGNED_ROLE_REQUIRED");
            }
            if (step.stepOrder() == null || step.stepOrder() <= 0 || !orders.add(step.stepOrder())) {
                throw new ProductionDispatchException("STEP_ORDER_INVALID");
            }
            if (step.sourceStepTemplateId() != null) {
                ensureSourceStepAvailable(routeTemplateId, step.sourceStepTemplateId());
            }
        }
        return steps.stream()
                .sorted(Comparator.comparing(DispatchStepRequest::stepOrder))
                .toList();
    }

    private void ensureSourceStepAvailable(Long routeTemplateId, Long sourceStepTemplateId) {
        ProcessStepTemplate sourceStep = stepTemplateMapper.selectById(sourceStepTemplateId);
        if (sourceStep == null
                || !routeTemplateId.equals(sourceStep.getRouteTemplateId())
                || !Boolean.TRUE.equals(sourceStep.getEnabled())
                || Boolean.TRUE.equals(sourceStep.getDeleted())) {
            throw new ProductionDispatchException("STEP_TEMPLATE_NOT_AVAILABLE");
        }
    }

    private ProductionRouteInstance createRouteInstance(
            OrderItemProductionContext orderItem,
            ProcessRouteTemplate routeTemplate,
            DispatchProductionRequest request) {
        return createRouteInstance(orderItem, routeTemplate, request, null);
    }

    private ProductionRouteInstance createRouteInstance(
            OrderItemProductionContext orderItem,
            ProcessRouteTemplate routeTemplate,
            DispatchProductionRequest request,
            Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        ProductionRouteInstance routeInstance = new ProductionRouteInstance();
        routeInstance.setTenantId(DEFAULT_TENANT_ID);
        routeInstance.setOrderId(orderItem.orderId());
        routeInstance.setOrderItemId(orderItem.id());
        routeInstance.setSourceRouteTemplateId(routeTemplate.getId());
        routeInstance.setSourceRouteTemplateVersion(routeTemplate.getVersion());
        routeInstance.setRouteCodeSnapshot(routeTemplate.getRouteCode());
        routeInstance.setRouteNameSnapshot(StringUtils.hasText(request.routeName())
                ? request.routeName()
                : routeTemplate.getRouteName());
        routeInstance.setProductTypeSnapshot(routeTemplate.getProductType());
        routeInstance.setRouteDescriptionSnapshot(routeTemplate.getDescription());
        routeInstance.setStatus(STATUS_DISPATCHED);
        routeInstance.setProductionProgress(BigDecimal.ZERO);
        routeInstance.setFrozen(true);
        routeInstance.setDispatchedBy(operatorId);
        routeInstance.setDispatchedAt(now);
        routeInstance.setIdempotencyKey(request.idempotencyKey());
        routeInstance.setCreatedBy(operatorId);
        routeInstance.setCreatedAt(now);
        routeInstance.setUpdatedBy(operatorId);
        routeInstance.setUpdatedAt(now);
        routeInstance.setDeleted(false);
        routeInstance.setDeleteMarker(0L);
        return routeInstance;
    }

    private ProductionStepInstance createStepInstance(
            OrderItemProductionContext orderItem,
            Long routeInstanceId,
            DispatchStepRequest step) {
        return createStepInstance(orderItem, routeInstanceId, step, null);
    }

    private ProductionStepInstance createStepInstance(
            OrderItemProductionContext orderItem,
            Long routeInstanceId,
            DispatchStepRequest step,
            Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        ProductionStepInstance stepInstance = new ProductionStepInstance();
        stepInstance.setTenantId(DEFAULT_TENANT_ID);
        stepInstance.setRouteInstanceId(routeInstanceId);
        stepInstance.setOrderId(orderItem.orderId());
        stepInstance.setOrderItemId(orderItem.id());
        stepInstance.setSourceStepTemplateId(step.sourceStepTemplateId());
        stepInstance.setStepCodeSnapshot(step.stepCode());
        stepInstance.setStepName(step.stepName());
        stepInstance.setStepOrder(step.stepOrder());
        stepInstance.setAssignedRole(step.assignedRole());
        stepInstance.setAssignedUserId(step.assignedUserId());
        stepInstance.setPhotoRequired(Boolean.TRUE.equals(step.photoRequired()));
        stepInstance.setRemarkRequired(Boolean.TRUE.equals(step.remarkRequired()));
        stepInstance.setMobileEnabled(step.mobileEnabled() == null || step.mobileEnabled());
        stepInstance.setEstimatedHours(step.estimatedHours());
        stepInstance.setOperationInstruction(step.operationInstruction());
        stepInstance.setStatus(STEP_STATUS_PENDING);
        stepInstance.setFrozen(true);
        stepInstance.setCreatedBy(operatorId);
        stepInstance.setCreatedAt(now);
        stepInstance.setUpdatedBy(operatorId);
        stepInstance.setUpdatedAt(now);
        stepInstance.setDeleted(false);
        stepInstance.setDeleteMarker(0L);
        return stepInstance;
    }
}
