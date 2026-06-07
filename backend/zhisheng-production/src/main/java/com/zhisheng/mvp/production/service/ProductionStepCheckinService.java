package com.zhisheng.mvp.production.service;

import com.zhisheng.mvp.production.dto.ProductionStepCheckinResult;
import com.zhisheng.mvp.production.dto.ProductionStepExecutionResponse;
import com.zhisheng.mvp.production.entity.ProductionRouteInstance;
import com.zhisheng.mvp.production.entity.ProductionStepCheckin;
import com.zhisheng.mvp.production.entity.ProductionStepInstance;
import com.zhisheng.mvp.production.exception.ProductionStepCheckinException;
import com.zhisheng.mvp.production.exception.ProductionStepExecutionException;
import com.zhisheng.mvp.production.mapper.ProductionRouteInstanceMapper;
import com.zhisheng.mvp.production.mapper.ProductionStepCheckinMapper;
import com.zhisheng.mvp.production.mapper.ProductionStepInstanceMapper;
import com.zhisheng.mvp.production.port.CurrentProductionUserContext;
import com.zhisheng.mvp.production.port.CurrentProductionUserPort;
import com.zhisheng.mvp.production.port.ProductionFileBindingPort;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"dev", "test"})
public class ProductionStepCheckinService {

    private static final String BIZ_TYPE_PRODUCTION_STEP_CHECKIN = "PRODUCTION_STEP_CHECKIN";
    private static final String CHECKIN_TYPE_COMPLETE = "COMPLETE";
    private static final String STEP_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final int MAX_FILE_IDS = 3;

    private final ProductionStepInstanceMapper stepInstanceMapper;
    private final ProductionRouteInstanceMapper routeInstanceMapper;
    private final ProductionStepCheckinMapper checkinMapper;
    private final CurrentProductionUserPort currentProductionUserPort;
    private final ProductionFileBindingPort productionFileBindingPort;
    private final ProductionStepExecutionService productionStepExecutionService;

    public ProductionStepCheckinService(
            ProductionStepInstanceMapper stepInstanceMapper,
            ProductionRouteInstanceMapper routeInstanceMapper,
            ProductionStepCheckinMapper checkinMapper,
            CurrentProductionUserPort currentProductionUserPort,
            ProductionFileBindingPort productionFileBindingPort,
            ProductionStepExecutionService productionStepExecutionService) {
        this.stepInstanceMapper = stepInstanceMapper;
        this.routeInstanceMapper = routeInstanceMapper;
        this.checkinMapper = checkinMapper;
        this.currentProductionUserPort = currentProductionUserPort;
        this.productionFileBindingPort = productionFileBindingPort;
        this.productionStepExecutionService = productionStepExecutionService;
    }

    @Transactional
    public ProductionStepCheckinResult completeWithCheckin(
            Long stepInstanceId,
            List<Long> fileIds,
            String remark) {
        CurrentProductionUserContext currentUser = currentProductionUserPort.currentUser();
        List<Long> normalizedFileIds = normalizeFileIds(fileIds);
        String normalizedRemark = normalizeRemark(remark);
        ProductionStepInstance step = requiredStep(stepInstanceId, currentUser.tenantId());
        requiredFrozenRoute(step.getRouteInstanceId(), currentUser.tenantId());
        ensureInProgress(step);
        ensureEvidence(step, normalizedFileIds, normalizedRemark);

        ProductionStepCheckin checkin = createCheckin(step, currentUser, normalizedFileIds, normalizedRemark);
        checkinMapper.insert(checkin);
        try {
            productionFileBindingPort.bindFiles(
                    currentUser.tenantId(),
                    BIZ_TYPE_PRODUCTION_STEP_CHECKIN,
                    checkin.getId(),
                    normalizedFileIds);
        } catch (ProductionStepCheckinException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ProductionStepCheckinException("FILE_BIND_FAILED");
        }

        ProductionStepExecutionResponse completeResult;
        try {
            completeResult = productionStepExecutionService.completeStep(stepInstanceId);
        } catch (ProductionStepExecutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ProductionStepCheckinException(exception.getMessage());
        }

        return new ProductionStepCheckinResult(
                stepInstanceId,
                step.getRouteInstanceId(),
                completeResult.status(),
                checkin.getId(),
                normalizedFileIds,
                completeResult.productionProgress());
    }

    private List<Long> normalizeFileIds(List<Long> fileIds) {
        List<Long> normalized = fileIds == null ? List.of() : List.copyOf(fileIds);
        if (normalized.size() > MAX_FILE_IDS) {
            throw new ProductionStepCheckinException("FILE_IDS_TOO_MANY");
        }
        Set<Long> unique = new HashSet<>();
        for (Long fileId : normalized) {
            if (fileId == null || fileId <= 0) {
                throw new ProductionStepCheckinException("FILE_ID_INVALID");
            }
            if (!unique.add(fileId)) {
                throw new ProductionStepCheckinException("FILE_ID_DUPLICATED");
            }
        }
        return normalized;
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String trimmed = remark.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductionStepInstance requiredStep(Long stepInstanceId, Long tenantId) {
        ProductionStepInstance step = stepInstanceMapper.selectById(stepInstanceId);
        if (step == null || !tenantId.equals(step.getTenantId()) || Boolean.TRUE.equals(step.getDeleted())) {
            throw new ProductionStepCheckinException("STEP_INSTANCE_NOT_FOUND");
        }
        return step;
    }

    private ProductionRouteInstance requiredFrozenRoute(Long routeInstanceId, Long tenantId) {
        ProductionRouteInstance route = routeInstanceMapper.selectById(routeInstanceId);
        if (route == null || !tenantId.equals(route.getTenantId()) || Boolean.TRUE.equals(route.getDeleted())) {
            throw new ProductionStepCheckinException("STEP_INSTANCE_NOT_FOUND");
        }
        if (!Boolean.TRUE.equals(route.getFrozen())) {
            throw new ProductionStepCheckinException("PRODUCTION_ROUTE_NOT_FROZEN");
        }
        return route;
    }

    private void ensureInProgress(ProductionStepInstance step) {
        if (!STEP_STATUS_IN_PROGRESS.equals(step.getStatus())) {
            throw new ProductionStepCheckinException("STEP_NOT_IN_PROGRESS");
        }
    }

    private void ensureEvidence(ProductionStepInstance step, List<Long> fileIds, String remark) {
        if (Boolean.TRUE.equals(step.getPhotoRequired()) && fileIds.isEmpty()) {
            throw new ProductionStepCheckinException("PHOTO_REQUIRED");
        }
        if (Boolean.TRUE.equals(step.getRemarkRequired()) && remark == null) {
            throw new ProductionStepCheckinException("REMARK_REQUIRED");
        }
    }

    private ProductionStepCheckin createCheckin(
            ProductionStepInstance step,
            CurrentProductionUserContext currentUser,
            List<Long> fileIds,
            String remark) {
        ProductionStepCheckin checkin = new ProductionStepCheckin();
        checkin.setTenantId(currentUser.tenantId());
        checkin.setStepInstanceId(step.getId());
        checkin.setRouteInstanceId(step.getRouteInstanceId());
        checkin.setOrderId(step.getOrderId());
        checkin.setOrderItemId(step.getOrderItemId());
        checkin.setOperatorId(currentUser.currentUserId());
        checkin.setCheckinType(CHECKIN_TYPE_COMPLETE);
        checkin.setRemark(remark);
        checkin.setFileIdsJson(toJsonArray(fileIds));
        checkin.setCreatedBy(currentUser.currentUserId());
        checkin.setCreatedAt(LocalDateTime.now());
        checkin.setDeleted(false);
        checkin.setDeleteMarker(0L);
        return checkin;
    }

    private String toJsonArray(List<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(",", fileIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList()) + "]";
    }
}
