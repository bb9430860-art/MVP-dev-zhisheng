package com.zhisheng.mvp.process.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.process.dto.EnabledRequest;
import com.zhisheng.mvp.process.dto.RouteTemplateOptionResponse;
import com.zhisheng.mvp.process.dto.RouteTemplateRequest;
import com.zhisheng.mvp.process.dto.RouteTemplateResponse;
import com.zhisheng.mvp.process.service.RouteTemplateService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process/route-templates")
public class RouteTemplateController {

    private final RouteTemplateService routeTemplateService;

    public RouteTemplateController(RouteTemplateService routeTemplateService) {
        this.routeTemplateService = routeTemplateService;
    }

    @GetMapping
    public ApiResponse<List<RouteTemplateResponse>> list() {
        return ApiResponse.success(routeTemplateService.list());
    }

    @PostMapping
    public ApiResponse<RouteTemplateResponse> create(@RequestBody RouteTemplateRequest request) {
        return ApiResponse.success(routeTemplateService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RouteTemplateResponse> get(@PathVariable("id") Long id) {
        return ApiResponse.success(routeTemplateService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<RouteTemplateResponse> update(
            @PathVariable("id") Long id,
            @RequestBody RouteTemplateRequest request) {
        return ApiResponse.success(routeTemplateService.update(id, request));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<RouteTemplateResponse> setEnabled(
            @PathVariable("id") Long id,
            @RequestBody EnabledRequest request) {
        return ApiResponse.success(routeTemplateService.setEnabled(id, Boolean.TRUE.equals(request.enabled())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        routeTemplateService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/options")
    public ApiResponse<List<RouteTemplateOptionResponse>> options(
            @RequestParam(name = "productType", required = false) String productType) {
        return ApiResponse.success(routeTemplateService.options(productType));
    }
}
