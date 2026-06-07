package com.zhisheng.mvp.production.dto;

import java.time.LocalDate;

public record ProductionWorkOrderQuery(
        String status,
        String workOrderNo,
        Long orderItemId,
        String keyword,
        LocalDate plannedStartFrom,
        LocalDate plannedStartTo,
        LocalDate requiredDeliveryFrom,
        LocalDate requiredDeliveryTo,
        Boolean routeLinked,
        Long page,
        Long pageSize) {
}
