package com.zhisheng.mvp.production.dto;

import java.util.List;

public record ProductionWorkOrderMaterialsUpdateRequest(
        List<ProductionWorkOrderMaterialRequest> materials) {
}
