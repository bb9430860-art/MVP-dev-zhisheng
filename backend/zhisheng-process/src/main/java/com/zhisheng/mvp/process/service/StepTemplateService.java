package com.zhisheng.mvp.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhisheng.mvp.process.dto.StepTemplateRequest;
import com.zhisheng.mvp.process.dto.StepTemplateResponse;
import com.zhisheng.mvp.process.entity.ProcessStepTemplate;
import com.zhisheng.mvp.process.mapper.ProcessStepTemplateMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StepTemplateService {

    private static final long DEFAULT_TENANT_ID = 1L;

    private final ProcessStepTemplateMapper stepMapper;
    private final RouteTemplateService routeTemplateService;

    public StepTemplateService(
            ProcessStepTemplateMapper stepMapper,
            RouteTemplateService routeTemplateService) {
        this.stepMapper = stepMapper;
        this.routeTemplateService = routeTemplateService;
    }

    @Transactional
    public StepTemplateResponse create(Long routeTemplateId, StepTemplateRequest request) {
        routeTemplateService.requiredRoute(routeTemplateId);
        ProcessStepTemplate step = new ProcessStepTemplate();
        step.setTenantId(DEFAULT_TENANT_ID);
        step.setRouteTemplateId(routeTemplateId);
        apply(step, request);
        step.setEnabled(request.enabled() == null || request.enabled());
        step.setStepOrder(maxNonDeletedOrder(routeTemplateId) + 1);
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        step.setDeleted(false);
        step.setDeleteMarker(0L);
        stepMapper.insert(step);
        normalizeOrders(routeTemplateId);
        return StepTemplateResponse.from(requiredStep(routeTemplateId, step.getId()));
    }

    @Transactional(readOnly = true)
    public List<StepTemplateResponse> list(Long routeTemplateId) {
        routeTemplateService.requiredRoute(routeTemplateId);
        return nonDeletedSteps(routeTemplateId).stream()
                .map(StepTemplateResponse::from)
                .toList();
    }

    @Transactional
    public StepTemplateResponse update(Long routeTemplateId, Long stepId, StepTemplateRequest request) {
        ProcessStepTemplate step = requiredStep(routeTemplateId, stepId);
        apply(step, request);
        if (request.enabled() != null) {
            step.setEnabled(request.enabled());
        }
        step.setUpdatedAt(LocalDateTime.now());
        stepMapper.updateById(step);
        normalizeOrders(routeTemplateId);
        return StepTemplateResponse.from(requiredStep(routeTemplateId, stepId));
    }

    @Transactional
    public StepTemplateResponse setEnabled(Long routeTemplateId, Long stepId, boolean enabled) {
        ProcessStepTemplate step = requiredStep(routeTemplateId, stepId);
        step.setEnabled(enabled);
        step.setUpdatedAt(LocalDateTime.now());
        stepMapper.updateById(step);
        normalizeOrders(routeTemplateId);
        return StepTemplateResponse.from(requiredStep(routeTemplateId, stepId));
    }

    @Transactional
    public void delete(Long routeTemplateId, Long stepId) {
        ProcessStepTemplate step = requiredStep(routeTemplateId, stepId);
        step.setDeleted(true);
        step.setEnabled(false);
        step.setDeleteMarker(step.getId());
        step.setUpdatedAt(LocalDateTime.now());
        stepMapper.updateById(step);
        normalizeOrders(routeTemplateId);
    }

    @Transactional
    public List<StepTemplateResponse> moveUp(Long routeTemplateId, Long stepId) {
        List<ProcessStepTemplate> active = activeSteps(routeTemplateId);
        int index = indexOf(active, stepId);
        if (index > 0) {
            swapOrder(active.get(index), active.get(index - 1));
        }
        normalizeOrders(routeTemplateId);
        return list(routeTemplateId);
    }

    @Transactional
    public List<StepTemplateResponse> moveDown(Long routeTemplateId, Long stepId) {
        List<ProcessStepTemplate> active = activeSteps(routeTemplateId);
        int index = indexOf(active, stepId);
        if (index < active.size() - 1) {
            swapOrder(active.get(index), active.get(index + 1));
        }
        normalizeOrders(routeTemplateId);
        return list(routeTemplateId);
    }

    @Transactional
    public List<StepTemplateResponse> reorder(Long routeTemplateId, List<Long> stepIds) {
        if (stepIds == null || stepIds.isEmpty()) {
            throw new IllegalArgumentException("stepIds is required");
        }
        Set<Long> unique = new HashSet<>(stepIds);
        if (unique.size() != stepIds.size()) {
            throw new IllegalArgumentException("stepIds contains duplicated id");
        }
        List<ProcessStepTemplate> active = activeSteps(routeTemplateId);
        Set<Long> activeIds = new HashSet<>(active.stream().map(ProcessStepTemplate::getId).toList());
        if (!activeIds.equals(unique)) {
            throw new IllegalArgumentException("stepIds must match active steps in this route");
        }

        for (ProcessStepTemplate step : active) {
            step.setStepOrder(-step.getId().intValue());
            stepMapper.updateById(step);
        }
        for (int i = 0; i < stepIds.size(); i++) {
            ProcessStepTemplate step = stepMapper.selectById(stepIds.get(i));
            step.setStepOrder(i + 1);
            step.setUpdatedAt(LocalDateTime.now());
            stepMapper.updateById(step);
        }
        normalizeOrders(routeTemplateId);
        return list(routeTemplateId);
    }

    private void apply(ProcessStepTemplate step, StepTemplateRequest request) {
        if (!StringUtils.hasText(request.stepCode())) {
            throw new IllegalArgumentException("stepCode is required");
        }
        if (!StringUtils.hasText(request.stepName())) {
            throw new IllegalArgumentException("stepName is required");
        }
        if (!StringUtils.hasText(request.assignedRole())) {
            throw new IllegalArgumentException("assignedRole is required");
        }
        step.setStepCode(request.stepCode());
        step.setStepName(request.stepName());
        step.setAssignedRole(request.assignedRole());
        step.setPhotoRequired(Boolean.TRUE.equals(request.photoRequired()));
        step.setRemarkRequired(Boolean.TRUE.equals(request.remarkRequired()));
        step.setMobileEnabled(request.mobileEnabled() == null || request.mobileEnabled());
        step.setEstimatedHours(request.estimatedHours());
        step.setOperationInstruction(request.operationInstruction());
    }

    private ProcessStepTemplate requiredStep(Long routeTemplateId, Long stepId) {
        ProcessStepTemplate step = stepMapper.selectById(stepId);
        if (step == null
                || Boolean.TRUE.equals(step.getDeleted())
                || !routeTemplateId.equals(step.getRouteTemplateId())) {
            throw new IllegalArgumentException("Step template not found");
        }
        return step;
    }

    private List<ProcessStepTemplate> activeSteps(Long routeTemplateId) {
        routeTemplateService.requiredRoute(routeTemplateId);
        return stepMapper.selectList(new LambdaQueryWrapper<ProcessStepTemplate>()
                .eq(ProcessStepTemplate::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProcessStepTemplate::getRouteTemplateId, routeTemplateId)
                .eq(ProcessStepTemplate::getEnabled, true)
                .eq(ProcessStepTemplate::getDeleted, false)
                .orderByAsc(ProcessStepTemplate::getStepOrder)
                .orderByAsc(ProcessStepTemplate::getId));
    }

    private List<ProcessStepTemplate> nonDeletedSteps(Long routeTemplateId) {
        return stepMapper.selectList(new LambdaQueryWrapper<ProcessStepTemplate>()
                .eq(ProcessStepTemplate::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProcessStepTemplate::getRouteTemplateId, routeTemplateId)
                .eq(ProcessStepTemplate::getDeleted, false)
                .orderByAsc(ProcessStepTemplate::getStepOrder)
                .orderByAsc(ProcessStepTemplate::getId));
    }

    private int indexOf(List<ProcessStepTemplate> steps, Long stepId) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(stepId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Step template not found in active steps");
    }

    private int maxNonDeletedOrder(Long routeTemplateId) {
        return nonDeletedSteps(routeTemplateId).stream()
                .map(ProcessStepTemplate::getStepOrder)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private void swapOrder(ProcessStepTemplate left, ProcessStepTemplate right) {
        int leftOrder = left.getStepOrder();
        int rightOrder = right.getStepOrder();
        left.setStepOrder(-left.getId().intValue());
        stepMapper.updateById(left);
        right.setStepOrder(leftOrder);
        right.setUpdatedAt(LocalDateTime.now());
        stepMapper.updateById(right);
        left.setStepOrder(rightOrder);
        left.setUpdatedAt(LocalDateTime.now());
        stepMapper.updateById(left);
    }

    private void normalizeOrders(Long routeTemplateId) {
        List<ProcessStepTemplate> active = activeSteps(routeTemplateId);
        List<ProcessStepTemplate> disabled = new ArrayList<>(stepMapper.selectList(
                new LambdaQueryWrapper<ProcessStepTemplate>()
                        .eq(ProcessStepTemplate::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ProcessStepTemplate::getRouteTemplateId, routeTemplateId)
                        .eq(ProcessStepTemplate::getEnabled, false)
                        .eq(ProcessStepTemplate::getDeleted, false)
                        .orderByAsc(ProcessStepTemplate::getStepOrder)
                        .orderByAsc(ProcessStepTemplate::getId)));

        List<ProcessStepTemplate> all = new ArrayList<>();
        all.addAll(active);
        all.addAll(disabled);
        for (ProcessStepTemplate step : all) {
            step.setStepOrder(-step.getId().intValue());
            stepMapper.updateById(step);
        }
        int order = 1;
        for (ProcessStepTemplate step : all) {
            step.setStepOrder(order++);
            step.setUpdatedAt(LocalDateTime.now());
            stepMapper.updateById(step);
        }
    }
}
