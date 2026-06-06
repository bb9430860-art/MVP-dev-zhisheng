package com.zhisheng.mvp.production.dto;

import java.util.List;

public record DispatchProductionRequest(
        Long routeTemplateId,
        String idempotencyKey,
        String routeName,
        List<DispatchStepRequest> steps) {
}
