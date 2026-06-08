package com.zhisheng.mvp.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("inventory_transaction")
public class InventoryTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long materialId;
    private String transactionType;
    private BigDecimal qty;
    private BigDecimal beforeOnHandQty;
    private BigDecimal afterOnHandQty;
    private BigDecimal beforeReservedQty;
    private BigDecimal afterReservedQty;
    private String referenceType;
    private Long referenceId;
    private String reason;
    private String remark;
    private Long operatorId;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private String idempotencyKey;

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

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getBeforeOnHandQty() {
        return beforeOnHandQty;
    }

    public void setBeforeOnHandQty(BigDecimal beforeOnHandQty) {
        this.beforeOnHandQty = beforeOnHandQty;
    }

    public BigDecimal getAfterOnHandQty() {
        return afterOnHandQty;
    }

    public void setAfterOnHandQty(BigDecimal afterOnHandQty) {
        this.afterOnHandQty = afterOnHandQty;
    }

    public BigDecimal getBeforeReservedQty() {
        return beforeReservedQty;
    }

    public void setBeforeReservedQty(BigDecimal beforeReservedQty) {
        this.beforeReservedQty = beforeReservedQty;
    }

    public BigDecimal getAfterReservedQty() {
        return afterReservedQty;
    }

    public void setAfterReservedQty(BigDecimal afterReservedQty) {
        this.afterReservedQty = afterReservedQty;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
