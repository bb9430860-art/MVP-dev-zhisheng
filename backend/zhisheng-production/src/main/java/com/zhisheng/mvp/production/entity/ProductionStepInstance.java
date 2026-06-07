package com.zhisheng.mvp.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("production_step_instance")
public class ProductionStepInstance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long routeInstanceId;
    private Long orderId;
    private Long orderItemId;
    private Long sourceStepTemplateId;
    private String stepCodeSnapshot;
    private String stepName;
    private Integer stepOrder;
    private String assignedRole;
    private Long assignedUserId;
    private Boolean photoRequired;
    private Boolean remarkRequired;
    private Boolean mobileEnabled;
    private BigDecimal estimatedHours;
    private String operationInstruction;
    private String status;
    private Boolean frozen;
    private LocalDateTime startedAt;
    private Long startedBy;
    private LocalDateTime completedAt;
    private Long completedBy;
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

    public Long getRouteInstanceId() {
        return routeInstanceId;
    }

    public void setRouteInstanceId(Long routeInstanceId) {
        this.routeInstanceId = routeInstanceId;
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

    public Long getSourceStepTemplateId() {
        return sourceStepTemplateId;
    }

    public void setSourceStepTemplateId(Long sourceStepTemplateId) {
        this.sourceStepTemplateId = sourceStepTemplateId;
    }

    public String getStepCodeSnapshot() {
        return stepCodeSnapshot;
    }

    public void setStepCodeSnapshot(String stepCodeSnapshot) {
        this.stepCodeSnapshot = stepCodeSnapshot;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(String assignedRole) {
        this.assignedRole = assignedRole;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public Boolean getPhotoRequired() {
        return photoRequired;
    }

    public void setPhotoRequired(Boolean photoRequired) {
        this.photoRequired = photoRequired;
    }

    public Boolean getRemarkRequired() {
        return remarkRequired;
    }

    public void setRemarkRequired(Boolean remarkRequired) {
        this.remarkRequired = remarkRequired;
    }

    public Boolean getMobileEnabled() {
        return mobileEnabled;
    }

    public void setMobileEnabled(Boolean mobileEnabled) {
        this.mobileEnabled = mobileEnabled;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(BigDecimal estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public String getOperationInstruction() {
        return operationInstruction;
    }

    public void setOperationInstruction(String operationInstruction) {
        this.operationInstruction = operationInstruction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getFrozen() {
        return frozen;
    }

    public void setFrozen(Boolean frozen) {
        this.frozen = frozen;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public Long getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(Long startedBy) {
        this.startedBy = startedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Long completedBy) {
        this.completedBy = completedBy;
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
