package com.zhisheng.mvp.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("production_route_instance")
public class ProductionRouteInstance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orderId;
    private Long orderItemId;
    private Long sourceRouteTemplateId;
    private Integer sourceRouteTemplateVersion;
    private String routeCodeSnapshot;
    private String routeNameSnapshot;
    private String productTypeSnapshot;
    private String routeDescriptionSnapshot;
    private String status;
    private BigDecimal productionProgress;
    private Boolean frozen;
    private Long dispatchedBy;
    private LocalDateTime dispatchedAt;
    private String idempotencyKey;
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

    public Long getSourceRouteTemplateId() {
        return sourceRouteTemplateId;
    }

    public void setSourceRouteTemplateId(Long sourceRouteTemplateId) {
        this.sourceRouteTemplateId = sourceRouteTemplateId;
    }

    public Integer getSourceRouteTemplateVersion() {
        return sourceRouteTemplateVersion;
    }

    public void setSourceRouteTemplateVersion(Integer sourceRouteTemplateVersion) {
        this.sourceRouteTemplateVersion = sourceRouteTemplateVersion;
    }

    public String getRouteCodeSnapshot() {
        return routeCodeSnapshot;
    }

    public void setRouteCodeSnapshot(String routeCodeSnapshot) {
        this.routeCodeSnapshot = routeCodeSnapshot;
    }

    public String getRouteNameSnapshot() {
        return routeNameSnapshot;
    }

    public void setRouteNameSnapshot(String routeNameSnapshot) {
        this.routeNameSnapshot = routeNameSnapshot;
    }

    public String getProductTypeSnapshot() {
        return productTypeSnapshot;
    }

    public void setProductTypeSnapshot(String productTypeSnapshot) {
        this.productTypeSnapshot = productTypeSnapshot;
    }

    public String getRouteDescriptionSnapshot() {
        return routeDescriptionSnapshot;
    }

    public void setRouteDescriptionSnapshot(String routeDescriptionSnapshot) {
        this.routeDescriptionSnapshot = routeDescriptionSnapshot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getProductionProgress() {
        return productionProgress;
    }

    public void setProductionProgress(BigDecimal productionProgress) {
        this.productionProgress = productionProgress;
    }

    public Boolean getFrozen() {
        return frozen;
    }

    public void setFrozen(Boolean frozen) {
        this.frozen = frozen;
    }

    public Long getDispatchedBy() {
        return dispatchedBy;
    }

    public void setDispatchedBy(Long dispatchedBy) {
        this.dispatchedBy = dispatchedBy;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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
