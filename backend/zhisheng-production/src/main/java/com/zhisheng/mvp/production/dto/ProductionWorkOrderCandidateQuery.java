package com.zhisheng.mvp.production.dto;

public record ProductionWorkOrderCandidateQuery(
        String keyword,
        String productType,
        String productionStatus,
        String orderNo,
        String orderType,
        String customerType,
        Boolean hasActiveWorkOrder,
        Long page,
        Long pageSize) {
}
