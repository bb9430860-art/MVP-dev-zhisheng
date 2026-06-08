package com.zhisheng.mvp.process.dto;

import java.util.List;

public record StepMaterialRequirementReplaceRequest(
        List<StepMaterialRequirementRequest> materials) {
}
