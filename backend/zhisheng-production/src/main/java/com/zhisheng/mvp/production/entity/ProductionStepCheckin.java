package com.zhisheng.mvp.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("production_step_checkin")
public class ProductionStepCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long stepInstanceId;
    private Long routeInstanceId;
    private Long orderId;
    private Long orderItemId;
    private Long operatorId;
    private String checkinType;
    private String remark;
    private String fileIdsJson;
    private Long createdBy;
    private LocalDateTime createdAt;
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

    public Long getStepInstanceId() {
        return stepInstanceId;
    }

    public void setStepInstanceId(Long stepInstanceId) {
        this.stepInstanceId = stepInstanceId;
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

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getCheckinType() {
        return checkinType;
    }

    public void setCheckinType(String checkinType) {
        this.checkinType = checkinType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getFileIdsJson() {
        return fileIdsJson;
    }

    public void setFileIdsJson(String fileIdsJson) {
        this.fileIdsJson = fileIdsJson;
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
