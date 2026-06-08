package com.zhisheng.mvp.inventory.dto;

import com.zhisheng.mvp.inventory.entity.InventoryStock;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryStockResponse(
        Long id,
        Long materialId,
        String materialCode,
        String materialName,
        String spec,
        String unit,
        BigDecimal onHandQty,
        BigDecimal reservedQty,
        BigDecimal availableQty,
        LocalDateTime updatedAt) {

    public static InventoryStockResponse from(InventoryStock stock) {
        return new InventoryStockResponse(
                stock.getId(),
                stock.getMaterialId(),
                stock.getMaterialCodeSnapshot(),
                stock.getMaterialNameSnapshot(),
                stock.getSpecSnapshot(),
                stock.getUnitSnapshot(),
                stock.getOnHandQty(),
                stock.getReservedQty(),
                stock.getAvailableQty(),
                stock.getUpdatedAt());
    }
}
