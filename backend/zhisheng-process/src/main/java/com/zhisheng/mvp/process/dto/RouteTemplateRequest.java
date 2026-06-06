package com.zhisheng.mvp.process.dto;

public record RouteTemplateRequest(
        String routeCode,
        String routeName,
        String productType,
        String description,
        Boolean enabled) {
}
