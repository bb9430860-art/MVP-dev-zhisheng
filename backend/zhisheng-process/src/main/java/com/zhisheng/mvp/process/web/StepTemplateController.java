package com.zhisheng.mvp.process.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.process.dto.EnabledRequest;
import com.zhisheng.mvp.process.dto.ReorderStepsRequest;
import com.zhisheng.mvp.process.dto.StepTemplateRequest;
import com.zhisheng.mvp.process.dto.StepTemplateResponse;
import com.zhisheng.mvp.process.service.StepTemplateService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process/route-templates/{routeTemplateId}/steps")
public class StepTemplateController {

    private final StepTemplateService stepTemplateService;

    public StepTemplateController(StepTemplateService stepTemplateService) {
        this.stepTemplateService = stepTemplateService;
    }

    @GetMapping
    public ApiResponse<List<StepTemplateResponse>> list(@PathVariable("routeTemplateId") Long routeTemplateId) {
        return ApiResponse.success(stepTemplateService.list(routeTemplateId));
    }

    @PostMapping
    public ApiResponse<StepTemplateResponse> create(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @RequestBody StepTemplateRequest request) {
        return ApiResponse.success(stepTemplateService.create(routeTemplateId, request));
    }

    @PutMapping("/{stepId}")
    public ApiResponse<StepTemplateResponse> update(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @PathVariable("stepId") Long stepId,
            @RequestBody StepTemplateRequest request) {
        return ApiResponse.success(stepTemplateService.update(routeTemplateId, stepId, request));
    }

    @PatchMapping("/{stepId}/enabled")
    public ApiResponse<StepTemplateResponse> setEnabled(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @PathVariable("stepId") Long stepId,
            @RequestBody EnabledRequest request) {
        return ApiResponse.success(stepTemplateService.setEnabled(
                routeTemplateId,
                stepId,
                Boolean.TRUE.equals(request.enabled())));
    }

    @DeleteMapping("/{stepId}")
    public ApiResponse<Void> delete(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @PathVariable("stepId") Long stepId) {
        stepTemplateService.delete(routeTemplateId, stepId);
        return ApiResponse.success();
    }

    @PutMapping("/{stepId}/move-up")
    public ApiResponse<List<StepTemplateResponse>> moveUp(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @PathVariable("stepId") Long stepId) {
        return ApiResponse.success(stepTemplateService.moveUp(routeTemplateId, stepId));
    }

    @PutMapping("/{stepId}/move-down")
    public ApiResponse<List<StepTemplateResponse>> moveDown(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @PathVariable("stepId") Long stepId) {
        return ApiResponse.success(stepTemplateService.moveDown(routeTemplateId, stepId));
    }

    @PutMapping("/reorder")
    public ApiResponse<List<StepTemplateResponse>> reorder(
            @PathVariable("routeTemplateId") Long routeTemplateId,
            @RequestBody ReorderStepsRequest request) {
        return ApiResponse.success(stepTemplateService.reorder(routeTemplateId, request.stepIds()));
    }
}
