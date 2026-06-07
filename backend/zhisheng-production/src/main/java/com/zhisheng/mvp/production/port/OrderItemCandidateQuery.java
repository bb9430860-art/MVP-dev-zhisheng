package com.zhisheng.mvp.production.port;

public record OrderItemCandidateQuery(
        String keyword,
        String productType,
        String productionStatus,
        String orderNo,
        String orderType,
        String customerType) {
}
