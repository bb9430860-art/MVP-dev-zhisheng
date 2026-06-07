package com.zhisheng.mvp.production.dto;

import java.time.LocalDate;

public record ProductionWorkOrderUpdateRequest(
        String priority,
        String instructionTitle,
        String instructionRemark,
        String productionRequirement,
        String qualityRequirement,
        String packagingRequirement,
        String shippingRequirement,
        String deliveryRequirement,
        LocalDate plannedStartDate,
        LocalDate plannedFinishDate,
        LocalDate requiredDeliveryDate,
        String deadlineRemark,
        String equipmentModel,
        String technicalConfigSummary,
        String technicalConfigRemark,
        String technicalConfigJson,
        Long responsibleUserId,
        Long handlerUserId,
        Long productionManagerId,
        Long primaryWorkerId,
        Boolean customerAcceptanceRequired,
        String acceptanceRemark) {
}
