package com.zhisheng.mvp.production.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.production.dto.ProductionProgressResponse;
import com.zhisheng.mvp.production.dto.ProductionStepDetailResponse;
import com.zhisheng.mvp.production.dto.ProductionStepExecutionResponse;
import com.zhisheng.mvp.production.dto.ProductionTaskResponse;
import com.zhisheng.mvp.production.service.ProductionStepExecutionService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/api/production")
public class ProductionStepExecutionController {

    private final ProductionStepExecutionService productionStepExecutionService;

    public ProductionStepExecutionController(ProductionStepExecutionService productionStepExecutionService) {
        this.productionStepExecutionService = productionStepExecutionService;
    }

    @GetMapping("/tasks/my")
    public ApiResponse<List<ProductionTaskResponse>> myTasks() {
        return ApiResponse.success(productionStepExecutionService.myTasks());
    }

    @GetMapping("/step-instances/{stepInstanceId}")
    public ApiResponse<ProductionStepDetailResponse> stepDetail(
            @PathVariable("stepInstanceId") Long stepInstanceId) {
        return ApiResponse.success(productionStepExecutionService.stepDetail(stepInstanceId));
    }

    @PostMapping("/step-instances/{stepInstanceId}/start")
    public ApiResponse<ProductionStepExecutionResponse> startStep(
            @PathVariable("stepInstanceId") Long stepInstanceId) {
        return ApiResponse.success(productionStepExecutionService.startStep(stepInstanceId));
    }

    @PostMapping("/step-instances/{stepInstanceId}/complete")
    public ApiResponse<ProductionStepExecutionResponse> completeStep(
            @PathVariable("stepInstanceId") Long stepInstanceId) {
        return ApiResponse.success(productionStepExecutionService.completeStep(stepInstanceId));
    }

    @GetMapping("/route-instances/{routeInstanceId}/progress")
    public ApiResponse<ProductionProgressResponse> progress(
            @PathVariable("routeInstanceId") Long routeInstanceId) {
        return ApiResponse.success(productionStepExecutionService.progress(routeInstanceId));
    }
}
