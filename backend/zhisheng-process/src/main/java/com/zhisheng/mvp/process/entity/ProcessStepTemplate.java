package com.zhisheng.mvp.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("process_step_template")
public class ProcessStepTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long routeTemplateId;
    private String stepCode;
    private String stepName;
    private Integer stepOrder;
    private String assignedRole;
    private Boolean photoRequired;
    private Boolean remarkRequired;
    private Boolean mobileEnabled;
    private BigDecimal estimatedHours;
    private String operationInstruction;
    private Boolean enabled;
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

    public Long getRouteTemplateId() {
        return routeTemplateId;
    }

    public void setRouteTemplateId(Long routeTemplateId) {
        this.routeTemplateId = routeTemplateId;
    }

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
