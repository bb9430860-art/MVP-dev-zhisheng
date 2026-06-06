package com.zhisheng.mvp.production.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.production.dto.DispatchConfigFromTemplateRequest;
import com.zhisheng.mvp.production.dto.DispatchConfigResponse;
import com.zhisheng.mvp.production.dto.DispatchProductionRequest;
import com.zhisheng.mvp.production.dto.OrderItemConfigContextResponse;
import com.zhisheng.mvp.production.dto.ProductionDispatchResponse;
import com.zhisheng.mvp.production.dto.ProductionSummaryResponse;
import com.zhisheng.mvp.production.service.ProductionDispatchService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/api/production/order-items/{orderItemId}")
public class ProductionDispatchController {

    private final ProductionDispatchService productionDispatchService;

    public ProductionDispatchController(ProductionDispatchService productionDispatchService) {
        this.productionDispatchService = productionDispatchService;
    }

    @GetMapping("/config-context")
    public ApiResponse<OrderItemConfigContextResponse> configContext(
            @PathVariable("orderItemId") Long orderItemId) {
        return ApiResponse.success(productionDispatchService.configContext(orderItemId));
    }

    @PostMapping("/dispatch-config/from-template")
    public ApiResponse<DispatchConfigResponse> fromTemplate(
            @PathVariable("orderItemId") Long orderItemId,
            @RequestBody DispatchConfigFromTemplateRequest request) {
        return ApiResponse.success(productionDispatchService.createConfigFromTemplate(orderItemId, request));
    }

    @PostMapping("/dispatch")
    public ApiResponse<ProductionDispatchResponse> dispatch(
            @PathVariable("orderItemId") Long orderItemId,
            @RequestBody DispatchProductionRequest request) {
        return ApiResponse.success(productionDispatchService.dispatch(orderItemId, request));
    }

    @GetMapping("/summary")
    public ApiResponse<ProductionSummaryResponse> summary(
            @PathVariable("orderItemId") Long orderItemId) {
        return ApiResponse.success(productionDispatchService.summary(orderItemId));
    }
}
