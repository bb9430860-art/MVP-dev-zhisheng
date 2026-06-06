package com.zhisheng.mvp.production.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhisheng.mvp.process.entity.ProcessRouteTemplate;
import com.zhisheng.mvp.process.entity.ProcessStepTemplate;
import com.zhisheng.mvp.process.mapper.ProcessRouteTemplateMapper;
import com.zhisheng.mvp.process.mapper.ProcessStepTemplateMapper;
import com.zhisheng.mvp.production.dto.DispatchProductionRequest;
import com.zhisheng.mvp.production.dto.DispatchStepRequest;
import com.zhisheng.mvp.production.dto.ProductionDispatchResponse;
import com.zhisheng.mvp.production.entity.ProductionRouteInstance;
import com.zhisheng.mvp.production.entity.ProductionStepInstance;
import com.zhisheng.mvp.production.exception.ProductionDispatchException;
import com.zhisheng.mvp.production.mapper.ProductionRouteInstanceMapper;
import com.zhisheng.mvp.production.mapper.ProductionStepInstanceMapper;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.port.OrderItemProductionPort;
import com.zhisheng.mvp.production.port.OrderItemReadPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
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

    public ProductionDispatchService(
            OrderItemReadPort orderItemReadPort,
            OrderItemProductionPort orderItemProductionPort,
            ProcessRouteTemplateMapper routeTemplateMapper,
            ProcessStepTemplateMapper stepTemplateMapper,
            ProductionRouteInstanceMapper routeInstanceMapper,
            ProductionStepInstanceMapper stepInstanceMapper) {
        this.orderItemReadPort = orderItemReadPort;
        this.orderItemProductionPort = orderItemProductionPort;
        this.routeTemplateMapper = routeTemplateMapper;
        this.stepTemplateMapper = stepTemplateMapper;
        this.routeInstanceMapper = routeInstanceMapper;
        this.stepInstanceMapper = stepInstanceMapper;
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

        OrderItemProductionContext orderItem = orderItemReadPort.findById(orderItemId)
                .orElseThrow(() -> new ProductionDispatchException("ORDER_ITEM_NOT_FOUND"));
        ensureNotDispatched(orderItem);

        ProcessRouteTemplate routeTemplate = routeTemplateMapper.selectById(request.routeTemplateId());
        if (routeTemplate == null
                || !Boolean.TRUE.equals(routeTemplate.getEnabled())
                || Boolean.TRUE.equals(routeTemplate.getDeleted())) {
            throw new ProductionDispatchException("ROUTE_TEMPLATE_NOT_AVAILABLE");
        }
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

    private void ensureNotDispatched(OrderItemProductionContext orderItem) {
        if (orderItem.productionRouteInstanceId() != null || STATUS_DISPATCHED.equals(orderItem.productionStatus())) {
            throw new ProductionDispatchException("ORDER_ITEM_ALREADY_DISPATCHED");
        }
        Long activeCount = routeInstanceMapper.selectCount(new LambdaQueryWrapper<ProductionRouteInstance>()
                .eq(ProductionRouteInstance::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionRouteInstance::getOrderItemId, orderItem.id())
                .eq(ProductionRouteInstance::getDeleted, false));
        if (activeCount > 0) {
            throw new ProductionDispatchException("ORDER_ITEM_ALREADY_DISPATCHED");
        }
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
        routeInstance.setDispatchedAt(now);
        routeInstance.setIdempotencyKey(request.idempotencyKey());
        routeInstance.setCreatedAt(now);
        routeInstance.setUpdatedAt(now);
        routeInstance.setDeleted(false);
        routeInstance.setDeleteMarker(0L);
        return routeInstance;
    }

    private ProductionStepInstance createStepInstance(
            OrderItemProductionContext orderItem,
            Long routeInstanceId,
            DispatchStepRequest step) {
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
        stepInstance.setCreatedAt(now);
        stepInstance.setUpdatedAt(now);
        stepInstance.setDeleted(false);
        stepInstance.setDeleteMarker(0L);
        return stepInstance;
    }
}
