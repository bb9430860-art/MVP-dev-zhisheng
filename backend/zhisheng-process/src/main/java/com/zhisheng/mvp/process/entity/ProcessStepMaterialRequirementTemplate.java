package com.zhisheng.mvp.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("process_step_material_requirement_template")
public class ProcessStepMaterialRequirementTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long routeTemplateId;
    private Long stepTemplateId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private BigDecimal baseQtyPerUnit;
    private BigDecimal fixedQty;
    private BigDecimal lossRate;
    private String requiredQtyExpression;
    private String usageStage;
    private String remark;
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

    public Long getStepTemplateId() {
        return stepTemplateId;
    }

    public void setStepTemplateId(Long stepTemplateId) {
        this.stepTemplateId = stepTemplateId;
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

    public BigDecimal getBaseQtyPerUnit() {
        return baseQtyPerUnit;
    }

    public void setBaseQtyPerUnit(BigDecimal baseQtyPerUnit) {
        this.baseQtyPerUnit = baseQtyPerUnit;
    }

    public BigDecimal getFixedQty() {
        return fixedQty;
    }

    public void setFixedQty(BigDecimal fixedQty) {
        this.fixedQty = fixedQty;
    }

    public BigDecimal getLossRate() {
        return lossRate;
    }

    public void setLossRate(BigDecimal lossRate) {
        this.lossRate = lossRate;
    }

    public String getRequiredQtyExpression() {
        return requiredQtyExpression;
    }

    public void setRequiredQtyExpression(String requiredQtyExpression) {
        this.requiredQtyExpression = requiredQtyExpression;
    }

    public String getUsageStage() {
        return usageStage;
    }

    public void setUsageStage(String usageStage) {
        this.usageStage = usageStage;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
