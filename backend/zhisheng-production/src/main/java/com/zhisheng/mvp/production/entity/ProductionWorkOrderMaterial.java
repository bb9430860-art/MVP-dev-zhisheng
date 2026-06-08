package com.zhisheng.mvp.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("production_work_order_material")
public class ProductionWorkOrderMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long workOrderId;
    private Long orderId;
    private Long orderItemId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private BigDecimal requiredQty;
    private BigDecimal availableQtySnapshot;
    private BigDecimal shortageQty;
    private String readinessStatus;
    private LocalDateTime readinessCheckedAt;
    private String readinessMessage;
    private String usageStage;
    private Long relatedStepTemplateId;
    private Long relatedStepInstanceId;
    private String requirementStatus;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean deleted;
    private Long deleteMarker;

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

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getRequiredQty() {
        return requiredQty;
    }

    public void setRequiredQty(BigDecimal requiredQty) {
        this.requiredQty = requiredQty;
    }

    public BigDecimal getAvailableQtySnapshot() {
        return availableQtySnapshot;
    }

    public void setAvailableQtySnapshot(BigDecimal availableQtySnapshot) {
        this.availableQtySnapshot = availableQtySnapshot;
    }

    public BigDecimal getShortageQty() {
        return shortageQty;
    }

    public void setShortageQty(BigDecimal shortageQty) {
        this.shortageQty = shortageQty;
    }

    public String getReadinessStatus() {
        return readinessStatus;
    }

    public void setReadinessStatus(String readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public LocalDateTime getReadinessCheckedAt() {
        return readinessCheckedAt;
    }

    public void setReadinessCheckedAt(LocalDateTime readinessCheckedAt) {
        this.readinessCheckedAt = readinessCheckedAt;
    }

    public String getReadinessMessage() {
        return readinessMessage;
    }

    public void setReadinessMessage(String readinessMessage) {
        this.readinessMessage = readinessMessage;
    }

    public String getUsageStage() {
        return usageStage;
    }

    public void setUsageStage(String usageStage) {
        this.usageStage = usageStage;
    }

    public Long getRelatedStepTemplateId() {
        return relatedStepTemplateId;
    }

    public void setRelatedStepTemplateId(Long relatedStepTemplateId) {
        this.relatedStepTemplateId = relatedStepTemplateId;
    }

    public Long getRelatedStepInstanceId() {
        return relatedStepInstanceId;
    }

    public void setRelatedStepInstanceId(Long relatedStepInstanceId) {
        this.relatedStepInstanceId = relatedStepInstanceId;
    }

    public String getRequirementStatus() {
        return requirementStatus;
    }

    public void setRequirementStatus(String requirementStatus) {
        this.requirementStatus = requirementStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Long getDeleteMarker() {
        return deleteMarker;
    }

    public void setDeleteMarker(Long deleteMarker) {
        this.deleteMarker = deleteMarker;
    }
}
