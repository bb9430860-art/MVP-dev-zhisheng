package com.zhisheng.mvp.inventory.dto;

public record MaterialItemRequest(
        String materialCode,
        String materialName,
        String spec,
        String unit,
        String category,
        Boolean enabled,
        String remark) {
}
