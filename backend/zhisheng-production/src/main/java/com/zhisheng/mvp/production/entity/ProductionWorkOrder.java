package com.zhisheng.mvp.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("production_work_order")
public class ProductionWorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String workOrderNo;
    private Long orderId;
    private Long orderItemId;
    private Long productionRouteInstanceId;
    private String orderItemNameSnapshot;
    private String productTypeSnapshot;
    private BigDecimal quantitySnapshot;
    private String status;
    private String priority;
    private String instructionTitle;
    private String instructionRemark;
    private String productionRequirement;
    private String qualityRequirement;
    private String packagingRequirement;
    private String shippingRequirement;
    private String deliveryRequirement;
    private LocalDate plannedStartDate;
    private LocalDate plannedFinishDate;
    private LocalDate requiredDeliveryDate;
    private String deadlineRemark;
    private String equipmentModel;
    private String technicalConfigSummary;
    private String technicalConfigRemark;
    private String technicalConfigJson;
    private Long responsibleUserId;
    private Long handlerUserId;
    private Long productionManagerId;
    private Long primaryWorkerId;
    private Long releasedBy;
    private LocalDateTime releasedAt;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private Long productionSignedBy;
    private LocalDateTime productionSignedAt;
    private Long warehouseConfirmedBy;
    private LocalDateTime warehouseConfirmedAt;
    private Long qualityConfirmedBy;
    private LocalDateTime qualityConfirmedAt;
    private Boolean customerAcceptanceRequired;
    private String acceptanceRemark;
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

    public String getWorkOrderNo() {
        return workOrderNo;
    }

    public void setWorkOrderNo(String workOrderNo) {
        this.workOrderNo = workOrderNo;
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

    public Long getProductionRouteInstanceId() {
        return productionRouteInstanceId;
    }

    public void setProductionRouteInstanceId(Long productionRouteInstanceId) {
        this.productionRouteInstanceId = productionRouteInstanceId;
    }

    public String getOrderItemNameSnapshot() {
        return orderItemNameSnapshot;
    }

    public void setOrderItemNameSnapshot(String orderItemNameSnapshot) {
        this.orderItemNameSnapshot = orderItemNameSnapshot;
    }

    public String getProductTypeSnapshot() {
        return productTypeSnapshot;
    }

    public void setProductTypeSnapshot(String productTypeSnapshot) {
        this.productTypeSnapshot = productTypeSnapshot;
    }

    public BigDecimal getQuantitySnapshot() {
        return quantitySnapshot;
    }

    public void setQuantitySnapshot(BigDecimal quantitySnapshot) {
        this.quantitySnapshot = quantitySnapshot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getInstructionTitle() {
        return instructionTitle;
    }

    public void setInstructionTitle(String instructionTitle) {
        this.instructionTitle = instructionTitle;
    }

    public String getInstructionRemark() {
        return instructionRemark;
    }

    public void setInstructionRemark(String instructionRemark) {
        this.instructionRemark = instructionRemark;
    }

    public String getProductionRequirement() {
        return productionRequirement;
    }

    public void setProductionRequirement(String productionRequirement) {
        this.productionRequirement = productionRequirement;
    }

    public String getQualityRequirement() {
        return qualityRequirement;
    }

    public void setQualityRequirement(String qualityRequirement) {
        this.qualityRequirement = qualityRequirement;
    }

    public String getPackagingRequirement() {
        return packagingRequirement;
    }

    public void setPackagingRequirement(String packagingRequirement) {
        this.packagingRequirement = packagingRequirement;
    }

    public String getShippingRequirement() {
        return shippingRequirement;
    }

    public void setShippingRequirement(String shippingRequirement) {
        this.shippingRequirement = shippingRequirement;
    }

    public String getDeliveryRequirement() {
        return deliveryRequirement;
    }

    public void setDeliveryRequirement(String deliveryRequirement) {
        this.deliveryRequirement = deliveryRequirement;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(LocalDate plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public LocalDate getPlannedFinishDate() {
        return plannedFinishDate;
    }

    public void setPlannedFinishDate(LocalDate plannedFinishDate) {
        this.plannedFinishDate = plannedFinishDate;
    }

    public LocalDate getRequiredDeliveryDate() {
        return requiredDeliveryDate;
    }

    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) {
        this.requiredDeliveryDate = requiredDeliveryDate;
    }

    public String getDeadlineRemark() {
        return deadlineRemark;
    }

    public void setDeadlineRemark(String deadlineRemark) {
        this.deadlineRemark = deadlineRemark;
    }

    public String getEquipmentModel() {
        return equipmentModel;
    }

    public void setEquipmentModel(String equipmentModel) {
        this.equipmentModel = equipmentModel;
    }

    public String getTechnicalConfigSummary() {
        return technicalConfigSummary;
    }

    public void setTechnicalConfigSummary(String technicalConfigSummary) {
        this.technicalConfigSummary = technicalConfigSummary;
    }

    public String getTechnicalConfigRemark() {
        return technicalConfigRemark;
    }

    public void setTechnicalConfigRemark(String technicalConfigRemark) {
        this.technicalConfigRemark = technicalConfigRemark;
    }

    public String getTechnicalConfigJson() {
        return technicalConfigJson;
    }

    public void setTechnicalConfigJson(String technicalConfigJson) {
        this.technicalConfigJson = technicalConfigJson;
    }

    public Long getResponsibleUserId() {
        return responsibleUserId;
    }

    public void setResponsibleUserId(Long responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
    }

    public Long getHandlerUserId() {
        return handlerUserId;
    }

    public void setHandlerUserId(Long handlerUserId) {
        this.handlerUserId = handlerUserId;
    }

    public Long getProductionManagerId() {
        return productionManagerId;
    }

    public void setProductionManagerId(Long productionManagerId) {
        this.productionManagerId = productionManagerId;
    }

    public Long getPrimaryWorkerId() {
        return primaryWorkerId;
    }

    public void setPrimaryWorkerId(Long primaryWorkerId) {
        this.primaryWorkerId = primaryWorkerId;
    }

    public Long getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(Long releasedBy) {
        this.releasedBy = releasedBy;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Long confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Long getProductionSignedBy() {
        return productionSignedBy;
    }

    public void setProductionSignedBy(Long productionSignedBy) {
        this.productionSignedBy = productionSignedBy;
    }

    public LocalDateTime getProductionSignedAt() {
        return productionSignedAt;
    }

    public void setProductionSignedAt(LocalDateTime productionSignedAt) {
        this.productionSignedAt = productionSignedAt;
    }

    public Long getWarehouseConfirmedBy() {
        return warehouseConfirmedBy;
    }

    public void setWarehouseConfirmedBy(Long warehouseConfirmedBy) {
        this.warehouseConfirmedBy = warehouseConfirmedBy;
    }

    public LocalDateTime getWarehouseConfirmedAt() {
        return warehouseConfirmedAt;
    }

    public void setWarehouseConfirmedAt(LocalDateTime warehouseConfirmedAt) {
        this.warehouseConfirmedAt = warehouseConfirmedAt;
    }

    public Long getQualityConfirmedBy() {
        return qualityConfirmedBy;
    }

    public void setQualityConfirmedBy(Long qualityConfirmedBy) {
        this.qualityConfirmedBy = qualityConfirmedBy;
    }

    public LocalDateTime getQualityConfirmedAt() {
        return qualityConfirmedAt;
    }

    public void setQualityConfirmedAt(LocalDateTime qualityConfirmedAt) {
        this.qualityConfirmedAt = qualityConfirmedAt;
    }

    public Boolean getCustomerAcceptanceRequired() {
        return customerAcceptanceRequired;
    }

    public void setCustomerAcceptanceRequired(Boolean customerAcceptanceRequired) {
        this.customerAcceptanceRequired = customerAcceptanceRequired;
    }

    public String getAcceptanceRemark() {
        return acceptanceRemark;
    }

    public void setAcceptanceRemark(String acceptanceRemark) {
        this.acceptanceRemark = acceptanceRemark;
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
