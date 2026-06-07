package com.zhisheng.mvp.production.dto;

import java.util.List;

public record ProductionStepCheckinResult(
        Long stepInstanceId,
        Long routeInstanceId,
        String status,
        Long checkinId,
        List<Long> fileIds,
        Integer productionProgress) {
}
