package com.zhisheng.mvp.process.dto;

import com.zhisheng.mvp.process.entity.ProcessRouteTemplate;

public record RouteTemplateResponse(
        Long id,
        Long tenantId,
        String routeCode,
        String routeName,
        String productType,
        String description,
        Boolean enabled,
        Boolean deleted) {

    public static RouteTemplateResponse from(ProcessRouteTemplate route) {
        return new RouteTemplateResponse(
                route.getId(),
                route.getTenantId(),
                route.getRouteCode(),
                route.getRouteName(),
                route.getProductType(),
                route.getDescription(),
                route.getEnabled(),
                route.getDeleted());
    }
}
