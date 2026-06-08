package com.zhisheng.mvp.inventory.dto;

import com.zhisheng.mvp.inventory.entity.MaterialItem;
import java.time.LocalDateTime;

public record MaterialItemResponse(
        Long id,
        String materialCode,
        String materialName,
        String spec,
        String unit,
        String category,
        Boolean enabled,
        String remark,
        LocalDateTime updatedAt) {

    public static MaterialItemResponse from(MaterialItem material) {
        return new MaterialItemResponse(
                material.getId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                material.getSpec(),
                material.getUnit(),
                material.getCategory(),
                material.getEnabled(),
                material.getRemark(),
                material.getUpdatedAt());
    }
}
