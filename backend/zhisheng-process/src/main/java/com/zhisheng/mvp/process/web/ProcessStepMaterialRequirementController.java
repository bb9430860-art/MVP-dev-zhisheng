package com.zhisheng.mvp.process.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.process.dto.StepMaterialRequirementReplaceRequest;
import com.zhisheng.mvp.process.dto.StepMaterialRequirementResponse;
import com.zhisheng.mvp.process.service.ProcessStepMaterialRequirementService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process/route-templates/{routeTemplateId}")
public class ProcessStepMaterialRequirementController {

    private final ProcessStepMaterialRequirementService materialRequirementService;

    public ProcessStepMaterialRequirementController(
            ProcessStepMaterialRequirementService materialRequirementService) {
        this.materialRequirementService = materialRequirementService;
    }

    @GetMapping("/step-materials")
    public ApiResponse<List<StepMaterialRequirementResponse>> listByRoute(
            @PathVariable("routeTemplateId") Long routeTemplateId) {
        return ApiResponse.success(materialRequirementService.listByRoute(routeTemplateId));
    }

    @GetMapping("/steps/{stepTemplateId}/materials")
    public ApiResponse<List<StepMaterialRequirementResponse>> listByStep(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @PathVariable("stepTemplateId") Long stepTemplateId) {
        return ApiResponse.success(materialRequirementService.listByStep(routeTemplateId, stepTemplateId));
    }

    @PutMapping("/steps/{stepTemplateId}/materials")
    public ApiResponse<List<StepMaterialRequirementResponse>> replaceStepMaterials(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @PathVariable("stepTemplateId") Long stepTemplateId,
            @RequestBody StepMaterialRequirementReplaceRequest request) {
        return ApiResponse.success(materialRequirementService.replaceStepMaterials(
                routeTemplateId,
                stepTemplateId,
                request));
    }
}
