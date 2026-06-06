package com.zhisheng.mvp.production.dto;

import java.util.List;

public record DispatchConfigResponse(
        Long routeTemplateId,
        String routeCode,
        String routeName,
        String productType,
        String description,
        List<DispatchConfigStepResponse> steps) {
}
