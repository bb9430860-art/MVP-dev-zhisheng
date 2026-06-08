package com.zhisheng.mvp.inventory.dto;

import java.util.List;

public record InventoryPageResponse<T>(
        List<T> items,
        long total,
        int page,
        int pageSize) {
}
