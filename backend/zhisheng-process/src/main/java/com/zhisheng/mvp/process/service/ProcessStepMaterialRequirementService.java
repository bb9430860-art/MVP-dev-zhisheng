package com.zhisheng.mvp.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhisheng.mvp.process.dto.StepMaterialRequirementReplaceRequest;
import com.zhisheng.mvp.process.dto.StepMaterialRequirementRequest;
import com.zhisheng.mvp.process.dto.StepMaterialRequirementResponse;
import com.zhisheng.mvp.process.entity.ProcessRouteTemplate;
import com.zhisheng.mvp.process.entity.ProcessStepMaterialRequirementTemplate;
import com.zhisheng.mvp.process.entity.ProcessStepTemplate;
import com.zhisheng.mvp.process.mapper.ProcessRouteTemplateMapper;
import com.zhisheng.mvp.process.mapper.ProcessStepMaterialRequirementTemplateMapper;
import com.zhisheng.mvp.process.mapper.ProcessStepTemplateMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProcessStepMaterialRequirementService {

    private static final long DEFAULT_TENANT_ID = 1L;
    private static final Long DEFAULT_TENANT_ID_VALUE = 1L;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProcessRouteTemplateMapper routeMapper;
    private final ProcessStepTemplateMapper stepMapper;
    private final ProcessStepMaterialRequirementTemplateMapper materialMapper;

    public ProcessStepMaterialRequirementService(
            ProcessRouteTemplateMapper routeMapper,
            ProcessStepTemplateMapper stepMapper,
            ProcessStepMaterialRequirementTemplateMapper materialMapper) {
        this.routeMapper = routeMapper;
        this.stepMapper = stepMapper;
        this.materialMapper = materialMapper;
    }

    @Transactional(readOnly = true)
    public List<StepMaterialRequirementResponse> listByRoute(Long routeTemplateId) {
        requiredRoute(routeTemplateId);
        return materialMapper.selectList(new LambdaQueryWrapper<ProcessStepMaterialRequirementTemplate>()
                        .eq(ProcessStepMaterialRequirementTemplate::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ProcessStepMaterialRequirementTemplate::getRouteTemplateId, routeTemplateId)
                        .eq(ProcessStepMaterialRequirementTemplate::getDeleted, false)
                        .orderByAsc(ProcessStepMaterialRequirementTemplate::getStepTemplateId)
                        .orderByAsc(ProcessStepMaterialRequirementTemplate::getId))
                .stream()
                .map(StepMaterialRequirementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StepMaterialRequirementResponse> listByStep(Long routeTemplateId, Long stepTemplateId) {
        requiredStep(routeTemplateId, stepTemplateId);
        return activeMaterials(routeTemplateId, stepTemplateId).stream()
                .map(StepMaterialRequirementResponse::from)
                .toList();
    }

    @Transactional
    public List<StepMaterialRequirementResponse> replaceStepMaterials(
            Long routeTemplateId,
            Long stepTemplateId,
            StepMaterialRequirementReplaceRequest request) {
        ProcessStepTemplate step = requiredStep(routeTemplateId, stepTemplateId);
        List<StepMaterialRequirementRequest> materials = request == null || request.materials() == null
                ? Collections.emptyList()
                : request.materials();
        materials.forEach(this::validate);

        LocalDateTime now = LocalDateTime.now();
        List<ProcessStepMaterialRequirementTemplate> existing = activeMaterials(routeTemplateId, stepTemplateId);
        for (ProcessStepMaterialRequirementTemplate material : existing) {
            material.setDeleted(true);
            material.setEnabled(false);
            material.setDeleteMarker(material.getId());
            material.setUpdatedAt(now);
            materialMapper.updateById(material);
        }

        for (StepMaterialRequirementRequest requestLine : materials) {
            ProcessStepMaterialRequirementTemplate material = new ProcessStepMaterialRequirementTemplate();
            material.setTenantId(DEFAULT_TENANT_ID);
            material.setRouteTemplateId(routeTemplateId);
            material.setStepTemplateId(step.getId());
            material.setMaterialId(requestLine.materialId());
            material.setMaterialCode(blankToNull(requestLine.materialCode()));
            material.setMaterialName(requestLine.materialName().trim());
            material.setSpec(blankToNull(requestLine.spec()));
            material.setUnit(requestLine.unit().trim());
            material.setBaseQtyPerUnit(requestLine.baseQtyPerUnit());
            material.setFixedQty(requestLine.fixedQty());
            material.setLossRate(requestLine.lossRate());
            material.setRequiredQtyExpression(blankToNull(requestLine.requiredQtyExpression()));
            material.setUsageStage(blankToNull(requestLine.usageStage()));
            material.setRemark(blankToNull(requestLine.remark()));
            material.setEnabled(requestLine.enabled() == null || requestLine.enabled());
            material.setCreatedAt(now);
            material.setUpdatedAt(now);
            material.setDeleted(false);
            material.setDeleteMarker(0L);
            materialMapper.insert(material);
        }

        return listByStep(routeTemplateId, stepTemplateId);
    }

    private ProcessRouteTemplate requiredRoute(Long routeTemplateId) {
        ProcessRouteTemplate route = routeMapper.selectById(routeTemplateId);
        if (route == null
                || Boolean.TRUE.equals(route.getDeleted())
                || !DEFAULT_TENANT_ID_VALUE.equals(route.getTenantId())) {
            throw new IllegalArgumentException("PROCESS_ROUTE_TEMPLATE_NOT_FOUND");
        }
        return route;
    }

    private ProcessStepTemplate requiredStep(Long routeTemplateId, Long stepTemplateId) {
        requiredRoute(routeTemplateId);
        ProcessStepTemplate step = stepMapper.selectById(stepTemplateId);
        if (step == null
                || Boolean.TRUE.equals(step.getDeleted())
                || !DEFAULT_TENANT_ID_VALUE.equals(step.getTenantId())
                || !routeTemplateId.equals(step.getRouteTemplateId())) {
            throw new IllegalArgumentException("PROCESS_STEP_TEMPLATE_NOT_FOUND");
        }
        return step;
    }

    private List<ProcessStepMaterialRequirementTemplate> activeMaterials(Long routeTemplateId, Long stepTemplateId) {
        return materialMapper.selectList(new LambdaQueryWrapper<ProcessStepMaterialRequirementTemplate>()
                .eq(ProcessStepMaterialRequirementTemplate::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProcessStepMaterialRequirementTemplate::getRouteTemplateId, routeTemplateId)
                .eq(ProcessStepMaterialRequirementTemplate::getStepTemplateId, stepTemplateId)
                .eq(ProcessStepMaterialRequirementTemplate::getDeleted, false)
                .orderByAsc(ProcessStepMaterialRequirementTemplate::getId));
    }

    private void validate(StepMaterialRequirementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("STEP_MATERIAL_REQUIREMENT_INVALID");
        }
        if (!StringUtils.hasText(request.materialName())) {
            throw new IllegalArgumentException("MATERIAL_NAME_REQUIRED");
        }
        if (!StringUtils.hasText(request.unit())) {
            throw new IllegalArgumentException("MATERIAL_UNIT_REQUIRED");
        }
        validateNonNegative(request.baseQtyPerUnit(), "MATERIAL_QUANTITY_RULE_INVALID");
        validateNonNegative(request.fixedQty(), "MATERIAL_QUANTITY_RULE_INVALID");
        validateNonNegative(request.lossRate(), "MATERIAL_LOSS_RATE_INVALID");

        boolean hasBaseQty = isPositive(request.baseQtyPerUnit());
        boolean hasFixedQty = isPositive(request.fixedQty());
        boolean hasExpression = StringUtils.hasText(request.requiredQtyExpression());
        if (!hasBaseQty && !hasFixedQty && !hasExpression) {
            throw new IllegalArgumentException("MATERIAL_QUANTITY_RULE_INVALID");
        }
    }

    private void validateNonNegative(BigDecimal value, String errorCode) {
        if (value != null && value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(ZERO) > 0;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
