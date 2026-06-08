package com.zhisheng.mvp.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("inventory_stock")
public class InventoryStock {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long materialId;
    private String materialCodeSnapshot;
    private String materialNameSnapshot;
    private String specSnapshot;
    private String unitSnapshot;
    private BigDecimal onHandQty;
    private BigDecimal reservedQty;
    private BigDecimal availableQty;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialCodeSnapshot() {
        return materialCodeSnapshot;
    }

    public void setMaterialCodeSnapshot(String materialCodeSnapshot) {
        this.materialCodeSnapshot = materialCodeSnapshot;
    }

    public String getMaterialNameSnapshot() {
        return materialNameSnapshot;
    }

    public void setMaterialNameSnapshot(String materialNameSnapshot) {
        this.materialNameSnapshot = materialNameSnapshot;
    }

    public String getSpecSnapshot() {
        return specSnapshot;
    }

    public void setSpecSnapshot(String specSnapshot) {
        this.specSnapshot = specSnapshot;
    }

    public String getUnitSnapshot() {
        return unitSnapshot;
    }

    public void setUnitSnapshot(String unitSnapshot) {
        this.unitSnapshot = unitSnapshot;
    }

    public BigDecimal getOnHandQty() {
        return onHandQty;
    }

    public void setOnHandQty(BigDecimal onHandQty) {
        this.onHandQty = onHandQty;
    }

    public BigDecimal getReservedQty() {
        return reservedQty;
    }

    public void setReservedQty(BigDecimal reservedQty) {
        this.reservedQty = reservedQty;
    }

    public BigDecimal getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(BigDecimal availableQty) {
        this.availableQty = availableQty;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
