package com.zhisheng.mvp.production.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhisheng.mvp.production.dto.ProductionProgressResponse;
import com.zhisheng.mvp.production.dto.ProductionStepExecutionResponse;
import com.zhisheng.mvp.production.dto.ProductionTaskResponse;
import com.zhisheng.mvp.production.entity.ProductionRouteInstance;
import com.zhisheng.mvp.production.entity.ProductionStepInstance;
import com.zhisheng.mvp.production.exception.ProductionStepExecutionException;
import com.zhisheng.mvp.production.mapper.ProductionRouteInstanceMapper;
import com.zhisheng.mvp.production.mapper.ProductionStepInstanceMapper;
import com.zhisheng.mvp.production.port.CurrentProductionUserContext;
import com.zhisheng.mvp.production.port.CurrentProductionUserPort;
import com.zhisheng.mvp.production.port.OrderItemProductionPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"dev", "test"})
public class ProductionStepExecutionService {

    private static final String ROUTE_STATUS_DISPATCHED = "DISPATCHED";
    private static final String ROUTE_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String ROUTE_STATUS_COMPLETED = "COMPLETED";
    private static final String STEP_STATUS_PENDING = "PENDING";
    private static final String STEP_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STEP_STATUS_COMPLETED = "COMPLETED";

    private final ProductionRouteInstanceMapper routeInstanceMapper;
    private final ProductionStepInstanceMapper stepInstanceMapper;
    private final CurrentProductionUserPort currentProductionUserPort;
    private final OrderItemProductionPort orderItemProductionPort;

    public ProductionStepExecutionService(
            ProductionRouteInstanceMapper routeInstanceMapper,
            ProductionStepInstanceMapper stepInstanceMapper,
            CurrentProductionUserPort currentProductionUserPort,
            OrderItemProductionPort orderItemProductionPort) {
        this.routeInstanceMapper = routeInstanceMapper;
        this.stepInstanceMapper = stepInstanceMapper;
        this.currentProductionUserPort = currentProductionUserPort;
        this.orderItemProductionPort = orderItemProductionPort;
    }

    @Transactional(readOnly = true)
    public List<ProductionTaskResponse> myTasks() {
        CurrentProductionUserContext currentUser = currentProductionUserPort.currentUser();
        List<ProductionStepInstance> assignedTasks = stepInstanceMapper.selectList(
                new LambdaQueryWrapper<ProductionStepInstance>()
                        .eq(ProductionStepInstance::getTenantId, currentUser.tenantId())
                        .eq(ProductionStepInstance::getAssignedUserId, currentUser.currentUserId())
                        .eq(ProductionStepInstance::getStatus, STEP_STATUS_PENDING)
                        .eq(ProductionStepInstance::getDeleted, false));
        List<ProductionStepInstance> roleTasks = currentUser.roles().isEmpty()
                ? Collections.emptyList()
                : stepInstanceMapper.selectList(new LambdaQueryWrapper<ProductionStepInstance>()
                        .eq(ProductionStepInstance::getTenantId, currentUser.tenantId())
                        .isNull(ProductionStepInstance::getAssignedUserId)
                        .in(ProductionStepInstance::getAssignedRole, currentUser.roles())
                        .eq(ProductionStepInstance::getStatus, STEP_STATUS_PENDING)
                        .eq(ProductionStepInstance::getDeleted, false));

        return java.util.stream.Stream.concat(assignedTasks.stream(), roleTasks.stream())
                .filter(step -> {
                    ProductionRouteInstance route = routeInstanceMapper.selectById(step.getRouteInstanceId());
                    return route != null
                            && currentUser.tenantId().equals(route.getTenantId())
                            && Boolean.TRUE.equals(route.getFrozen())
                            && !Boolean.TRUE.equals(route.getDeleted());
                })
                .sorted(Comparator.comparing(ProductionStepInstance::getStepOrder)
                        .thenComparing(ProductionStepInstance::getId))
                .map(this::toTaskResponse)
                .toList();
    }

    @Transactional
    public ProductionStepExecutionResponse startStep(Long stepInstanceId) {
        CurrentProductionUserContext currentUser = currentProductionUserPort.currentUser();
        ProductionStepInstance step = requiredStep(stepInstanceId, currentUser.tenantId());
        ProductionRouteInstance route = requiredFrozenRoute(step.getRouteInstanceId(), currentUser.tenantId());
        ensureCanStart(currentUser, step);
        ensurePreviousStepsCompleted(currentUser.tenantId(), step);

        LocalDateTime now = LocalDateTime.now();
        int updated = stepInstanceMapper.update(null, new LambdaUpdateWrapper<ProductionStepInstance>()
                .eq(ProductionStepInstance::getId, step.getId())
                .eq(ProductionStepInstance::getTenantId, currentUser.tenantId())
                .eq(ProductionStepInstance::getStatus, STEP_STATUS_PENDING)
                .eq(ProductionStepInstance::getDeleted, false)
                .set(ProductionStepInstance::getStatus, STEP_STATUS_IN_PROGRESS)
                .set(ProductionStepInstance::getStartedAt, now)
                .set(ProductionStepInstance::getStartedBy, currentUser.currentUserId())
                .set(ProductionStepInstance::getUpdatedAt, now)
                .set(ProductionStepInstance::getUpdatedBy, currentUser.currentUserId()));
        if (updated != 1) {
            ProductionStepInstance latest = requiredStep(stepInstanceId, currentUser.tenantId());
            if (STEP_STATUS_IN_PROGRESS.equals(latest.getStatus())) {
                throw new ProductionStepExecutionException("STEP_ALREADY_STARTED");
            }
            throw new ProductionStepExecutionException("STEP_NOT_PENDING");
        }

        if (ROUTE_STATUS_DISPATCHED.equals(route.getStatus())) {
            routeInstanceMapper.update(null, new LambdaUpdateWrapper<ProductionRouteInstance>()
                    .eq(ProductionRouteInstance::getId, route.getId())
                    .eq(ProductionRouteInstance::getTenantId, currentUser.tenantId())
                    .set(ProductionRouteInstance::getStatus, ROUTE_STATUS_IN_PROGRESS)
                    .set(ProductionRouteInstance::getUpdatedAt, now)
                    .set(ProductionRouteInstance::getUpdatedBy, currentUser.currentUserId()));
        }
        updateOrderItemProductionProgress(
                route.getOrderItemId(),
                ROUTE_STATUS_IN_PROGRESS,
                route.getProductionProgress() == null ? BigDecimal.ZERO : route.getProductionProgress());

        return new ProductionStepExecutionResponse(
                step.getId(),
                route.getId(),
                STEP_STATUS_IN_PROGRESS,
                toIntProgress(route.getProductionProgress()));
    }

    @Transactional
    public ProductionStepExecutionResponse completeStep(Long stepInstanceId) {
        CurrentProductionUserContext currentUser = currentProductionUserPort.currentUser();
        ProductionStepInstance step = requiredStep(stepInstanceId, currentUser.tenantId());
        ProductionRouteInstance route = requiredFrozenRoute(step.getRouteInstanceId(), currentUser.tenantId());
        ensureCanComplete(currentUser, step);

        LocalDateTime now = LocalDateTime.now();
        stepInstanceMapper.update(null, new LambdaUpdateWrapper<ProductionStepInstance>()
                .eq(ProductionStepInstance::getId, step.getId())
                .eq(ProductionStepInstance::getTenantId, currentUser.tenantId())
                .eq(ProductionStepInstance::getStatus, STEP_STATUS_IN_PROGRESS)
                .eq(ProductionStepInstance::getDeleted, false)
                .set(ProductionStepInstance::getStatus, STEP_STATUS_COMPLETED)
                .set(ProductionStepInstance::getCompletedAt, now)
                .set(ProductionStepInstance::getCompletedBy, currentUser.currentUserId())
                .set(ProductionStepInstance::getUpdatedAt, now)
                .set(ProductionStepInstance::getUpdatedBy, currentUser.currentUserId()));

        ProductionProgressResponse progress = recalculateProgress(route.getId(), currentUser);
        return new ProductionStepExecutionResponse(
                step.getId(),
                route.getId(),
                STEP_STATUS_COMPLETED,
                progress.progress());
    }

    @Transactional
    public ProductionProgressResponse progress(Long routeInstanceId) {
        CurrentProductionUserContext currentUser = currentProductionUserPort.currentUser();
        requiredFrozenRoute(routeInstanceId, currentUser.tenantId());
        return recalculateProgress(routeInstanceId, currentUser);
    }

    private ProductionStepInstance requiredStep(Long stepInstanceId, Long tenantId) {
        ProductionStepInstance step = stepInstanceMapper.selectById(stepInstanceId);
        if (step == null || !tenantId.equals(step.getTenantId()) || Boolean.TRUE.equals(step.getDeleted())) {
            throw new ProductionStepExecutionException("STEP_INSTANCE_NOT_FOUND");
        }
        return step;
    }

    private ProductionRouteInstance requiredFrozenRoute(Long routeInstanceId, Long tenantId) {
        ProductionRouteInstance route = routeInstanceMapper.selectById(routeInstanceId);
        if (route == null || !tenantId.equals(route.getTenantId()) || Boolean.TRUE.equals(route.getDeleted())) {
            throw new ProductionStepExecutionException("STEP_INSTANCE_NOT_FOUND");
        }
        if (!Boolean.TRUE.equals(route.getFrozen())) {
            throw new ProductionStepExecutionException("PRODUCTION_ROUTE_NOT_FROZEN");
        }
        return route;
    }

    private void ensureCanStart(CurrentProductionUserContext currentUser, ProductionStepInstance step) {
        if (STEP_STATUS_IN_PROGRESS.equals(step.getStatus())) {
            throw new ProductionStepExecutionException("STEP_ALREADY_STARTED");
        }
        if (STEP_STATUS_COMPLETED.equals(step.getStatus())) {
            throw new ProductionStepExecutionException("STEP_ALREADY_COMPLETED");
        }
        if (!STEP_STATUS_PENDING.equals(step.getStatus())) {
            throw new ProductionStepExecutionException("STEP_NOT_PENDING");
        }
        if (!canExecutePendingStep(currentUser, step)) {
            throw new ProductionStepExecutionException("STEP_NOT_ASSIGNED_TO_CURRENT_USER");
        }
    }

    private void ensureCanComplete(CurrentProductionUserContext currentUser, ProductionStepInstance step) {
        if (STEP_STATUS_COMPLETED.equals(step.getStatus())) {
            throw new ProductionStepExecutionException("STEP_ALREADY_COMPLETED");
        }
        if (!STEP_STATUS_IN_PROGRESS.equals(step.getStatus())) {
            throw new ProductionStepExecutionException("STEP_NOT_IN_PROGRESS");
        }
        if (step.getAssignedUserId() == null) {
            if (!Objects.equals(step.getStartedBy(), currentUser.currentUserId())) {
                throw new ProductionStepExecutionException("STEP_NOT_ASSIGNED_TO_CURRENT_USER");
            }
            return;
        }
        if (!Objects.equals(step.getAssignedUserId(), currentUser.currentUserId())) {
            throw new ProductionStepExecutionException("STEP_NOT_ASSIGNED_TO_CURRENT_USER");
        }
    }

    private boolean canExecutePendingStep(CurrentProductionUserContext currentUser, ProductionStepInstance step) {
        if (step.getAssignedUserId() != null) {
            return Objects.equals(step.getAssignedUserId(), currentUser.currentUserId());
        }
        return currentUser.roles().contains(step.getAssignedRole());
    }

    private void ensurePreviousStepsCompleted(Long tenantId, ProductionStepInstance step) {
        Long unfinishedPrevious = stepInstanceMapper.selectCount(new LambdaQueryWrapper<ProductionStepInstance>()
                .eq(ProductionStepInstance::getTenantId, tenantId)
                .eq(ProductionStepInstance::getRouteInstanceId, step.getRouteInstanceId())
                .eq(ProductionStepInstance::getDeleted, false)
                .lt(ProductionStepInstance::getStepOrder, step.getStepOrder())
                .ne(ProductionStepInstance::getStatus, STEP_STATUS_COMPLETED));
        if (unfinishedPrevious > 0) {
            throw new ProductionStepExecutionException("PREVIOUS_STEP_NOT_COMPLETED");
        }
    }

    private ProductionProgressResponse recalculateProgress(
            Long routeInstanceId,
            CurrentProductionUserContext currentUser) {
        ProductionRouteInstance route = requiredFrozenRoute(routeInstanceId, currentUser.tenantId());
        List<ProductionStepInstance> steps = stepInstanceMapper.selectList(
                new LambdaQueryWrapper<ProductionStepInstance>()
                        .eq(ProductionStepInstance::getTenantId, currentUser.tenantId())
                        .eq(ProductionStepInstance::getRouteInstanceId, routeInstanceId)
                        .eq(ProductionStepInstance::getDeleted, false));
        int totalSteps = steps.size();
        if (totalSteps == 0) {
            throw new ProductionStepExecutionException("PRODUCTION_PROGRESS_INVALID_TOTAL_STEPS");
        }
        int completedSteps = (int) steps.stream()
                .filter(step -> STEP_STATUS_COMPLETED.equals(step.getStatus()))
                .count();
        int progress = completedSteps == totalSteps
                ? 100
                : Math.floorDiv(completedSteps * 100, totalSteps);
        String routeStatus = completedSteps == totalSteps ? ROUTE_STATUS_COMPLETED : ROUTE_STATUS_IN_PROGRESS;
        BigDecimal progressValue = BigDecimal.valueOf(progress);

        routeInstanceMapper.update(null, new LambdaUpdateWrapper<ProductionRouteInstance>()
                .eq(ProductionRouteInstance::getId, route.getId())
                .eq(ProductionRouteInstance::getTenantId, currentUser.tenantId())
                .set(ProductionRouteInstance::getStatus, routeStatus)
                .set(ProductionRouteInstance::getProductionProgress, progressValue)
                .set(ProductionRouteInstance::getUpdatedAt, LocalDateTime.now())
                .set(ProductionRouteInstance::getUpdatedBy, currentUser.currentUserId()));
        updateOrderItemProductionProgress(route.getOrderItemId(), routeStatus, progressValue);

        return new ProductionProgressResponse(routeInstanceId, totalSteps, completedSteps, progress, routeStatus);
    }

    private void updateOrderItemProductionProgress(
            Long orderItemId,
            String productionStatus,
            BigDecimal productionProgress) {
        try {
            orderItemProductionPort.updateProductionProgress(orderItemId, productionStatus, productionProgress);
        } catch (RuntimeException exception) {
            throw new ProductionStepExecutionException("ORDER_ITEM_WRITEBACK_FAILED");
        }
    }

    private ProductionTaskResponse toTaskResponse(ProductionStepInstance step) {
        return new ProductionTaskResponse(
                step.getId(),
                step.getRouteInstanceId(),
                step.getOrderItemId(),
                step.getStepName(),
                step.getStepOrder(),
                step.getAssignedRole(),
                step.getAssignedUserId(),
                step.getStatus());
    }

    private int toIntProgress(BigDecimal progress) {
        if (progress == null) {
            return 0;
        }
        return progress.intValue();
    }
}
