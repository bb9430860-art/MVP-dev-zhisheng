package com.zhisheng.mvp.production.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCreateRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderMaterialRequest;
import com.zhisheng.mvp.production.entity.ProductionRouteInstance;
import com.zhisheng.mvp.production.entity.ProductionWorkOrder;
import com.zhisheng.mvp.production.entity.ProductionWorkOrderMaterial;
import com.zhisheng.mvp.production.enums.ProductionWorkOrderStatus;
import com.zhisheng.mvp.production.exception.ProductionWorkOrderException;
import com.zhisheng.mvp.production.mapper.ProductionRouteInstanceMapper;
import com.zhisheng.mvp.production.mapper.ProductionWorkOrderMapper;
import com.zhisheng.mvp.production.mapper.ProductionWorkOrderMaterialMapper;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.port.OrderItemReadPort;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Profile({"dev", "test"})
public class ProductionWorkOrderService {

    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int MAX_NO_RETRY = 3;

    private final ProductionWorkOrderMapper workOrderMapper;
    private final ProductionWorkOrderMaterialMapper materialMapper;
    private final ProductionRouteInstanceMapper routeInstanceMapper;
    private final OrderItemReadPort orderItemReadPort;
    private final WorkOrderNoGenerator workOrderNoGenerator;

    public ProductionWorkOrderService(
            ProductionWorkOrderMapper workOrderMapper,
            ProductionWorkOrderMaterialMapper materialMapper,
            ProductionRouteInstanceMapper routeInstanceMapper,
            OrderItemReadPort orderItemReadPort,
            WorkOrderNoGenerator workOrderNoGenerator) {
        this.workOrderMapper = workOrderMapper;
        this.materialMapper = materialMapper;
        this.routeInstanceMapper = routeInstanceMapper;
        this.orderItemReadPort = orderItemReadPort;
        this.workOrderNoGenerator = workOrderNoGenerator;
    }

    @Transactional
    public ProductionWorkOrder createFromOrderItem(ProductionWorkOrderCreateRequest request, Long operatorId) {
        if (request == null || request.orderItemId() == null) {
            throw new ProductionWorkOrderException("ORDER_ITEM_NOT_FOUND");
        }
        OrderItemProductionContext orderItem = orderItemReadPort.findById(request.orderItemId())
                .orElseThrow(() -> new ProductionWorkOrderException("ORDER_ITEM_NOT_FOUND"));
        ensureNoActiveWorkOrder(orderItem.id());
        validateMaterials(request.materials());

        ProductionWorkOrder workOrder = null;
        for (int attempt = 0; attempt < MAX_NO_RETRY; attempt++) {
            workOrder = createWorkOrder(orderItem, request, operatorId);
            workOrder.setWorkOrderNo(workOrderNoGenerator.nextNo(DEFAULT_TENANT_ID));
            try {
                workOrderMapper.insert(workOrder);
                break;
            } catch (DuplicateKeyException exception) {
                if (attempt == MAX_NO_RETRY - 1) {
                    throw exception;
                }
            }
        }
        if (workOrder == null || workOrder.getId() == null) {
            throw new ProductionWorkOrderException("WORK_ORDER_NO_GENERATION_FAILED");
        }

        insertMaterials(workOrder, request.materials(), operatorId);
        return workOrderMapper.selectById(workOrder.getId());
    }

    @Transactional
    public ProductionWorkOrder release(Long workOrderId, Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        if (!ProductionWorkOrderStatus.DRAFT.name().equals(workOrder.getStatus())) {
            ensureTerminalStatusNotMutated(workOrder);
            throw new ProductionWorkOrderException("WORK_ORDER_INVALID_STATUS_TRANSITION");
        }
        LocalDateTime now = LocalDateTime.now();
        updateStatus(workOrder.getId(), ProductionWorkOrderStatus.RELEASED.name(), operatorId, now);
        workOrderMapper.update(null, new LambdaUpdateWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getId, workOrder.getId())
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .set(ProductionWorkOrder::getReleasedBy, operatorId)
                .set(ProductionWorkOrder::getReleasedAt, now)
                .set(ProductionWorkOrder::getConfirmedBy, operatorId)
                .set(ProductionWorkOrder::getConfirmedAt, now));
        return requiredWorkOrder(workOrderId);
    }

    @Transactional
    public ProductionWorkOrder markInProgress(Long workOrderId, Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        if (!ProductionWorkOrderStatus.RELEASED.name().equals(workOrder.getStatus())) {
            ensureTerminalStatusNotMutated(workOrder);
            throw new ProductionWorkOrderException("WORK_ORDER_INVALID_STATUS_TRANSITION");
        }
        updateStatus(workOrder.getId(), ProductionWorkOrderStatus.IN_PROGRESS.name(), operatorId, LocalDateTime.now());
        return requiredWorkOrder(workOrderId);
    }

    @Transactional
    public ProductionWorkOrder complete(Long workOrderId, Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        if (!ProductionWorkOrderStatus.IN_PROGRESS.name().equals(workOrder.getStatus())) {
            ensureTerminalStatusNotMutated(workOrder);
            throw new ProductionWorkOrderException("WORK_ORDER_INVALID_STATUS_TRANSITION");
        }
        updateStatus(workOrder.getId(), ProductionWorkOrderStatus.COMPLETED.name(), operatorId, LocalDateTime.now());
        return requiredWorkOrder(workOrderId);
    }

    @Transactional
    public ProductionWorkOrder cancel(Long workOrderId, Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        ensureTerminalStatusNotMutated(workOrder);
        if (!ProductionWorkOrderStatus.DRAFT.name().equals(workOrder.getStatus())
                && !ProductionWorkOrderStatus.RELEASED.name().equals(workOrder.getStatus())) {
            throw new ProductionWorkOrderException("WORK_ORDER_INVALID_STATUS_TRANSITION");
        }
        updateStatus(workOrder.getId(), ProductionWorkOrderStatus.CANCELLED.name(), operatorId, LocalDateTime.now());
        return requiredWorkOrder(workOrderId);
    }

    @Transactional
    public ProductionWorkOrder transition(Long workOrderId, String targetStatus, Long operatorId) {
        return switch (ProductionWorkOrderStatus.valueOf(targetStatus)) {
            case RELEASED -> release(workOrderId, operatorId);
            case IN_PROGRESS -> markInProgress(workOrderId, operatorId);
            case COMPLETED -> complete(workOrderId, operatorId);
            case CANCELLED -> cancel(workOrderId, operatorId);
            case DRAFT -> throw new ProductionWorkOrderException("WORK_ORDER_INVALID_STATUS_TRANSITION");
        };
    }

    @Transactional
    public ProductionWorkOrder linkRouteInstance(Long workOrderId, Long routeInstanceId, Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        ensureTerminalStatusNotMutated(workOrder);
        ProductionRouteInstance route = routeInstanceMapper.selectById(routeInstanceId);
        if (route == null
                || !isDefaultTenant(route.getTenantId())
                || Boolean.TRUE.equals(route.getDeleted())
                || !workOrder.getOrderItemId().equals(route.getOrderItemId())) {
            throw new ProductionWorkOrderException("WORK_ORDER_ROUTE_LINK_CONFLICT");
        }
        workOrderMapper.update(null, new LambdaUpdateWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getId, workOrder.getId())
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .set(ProductionWorkOrder::getProductionRouteInstanceId, routeInstanceId)
                .set(ProductionWorkOrder::getUpdatedAt, LocalDateTime.now())
                .set(ProductionWorkOrder::getUpdatedBy, operatorId));
        return requiredWorkOrder(workOrderId);
    }

    private boolean isDefaultTenant(Long tenantId) {
        return Long.valueOf(DEFAULT_TENANT_ID).equals(tenantId);
    }

    private void ensureNoActiveWorkOrder(Long orderItemId) {
        Long activeCount = workOrderMapper.selectCount(new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionWorkOrder::getOrderItemId, orderItemId)
                .eq(ProductionWorkOrder::getDeleted, false)
                .in(ProductionWorkOrder::getStatus, List.of(
                        ProductionWorkOrderStatus.DRAFT.name(),
                        ProductionWorkOrderStatus.RELEASED.name(),
                        ProductionWorkOrderStatus.IN_PROGRESS.name())));
        if (activeCount > 0) {
            throw new ProductionWorkOrderException("WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM");
        }
    }

    private ProductionWorkOrder requiredWorkOrder(Long workOrderId) {
        ProductionWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null
                || !Long.valueOf(DEFAULT_TENANT_ID).equals(workOrder.getTenantId())
                || Boolean.TRUE.equals(workOrder.getDeleted())) {
            throw new ProductionWorkOrderException("WORK_ORDER_NOT_FOUND");
        }
        return workOrder;
    }

    private void validateMaterials(List<ProductionWorkOrderMaterialRequest> materials) {
        if (materials == null) {
            return;
        }
        for (ProductionWorkOrderMaterialRequest material : materials) {
            if (material == null
                    || !StringUtils.hasText(material.materialName())
                    || material.requiredQty() == null
                    || material.requiredQty().signum() <= 0) {
                throw new ProductionWorkOrderException("MATERIAL_REQUIREMENT_INVALID");
            }
        }
    }

    private ProductionWorkOrder createWorkOrder(
            OrderItemProductionContext orderItem,
            ProductionWorkOrderCreateRequest request,
            Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        ProductionWorkOrder workOrder = new ProductionWorkOrder();
        workOrder.setTenantId(DEFAULT_TENANT_ID);
        workOrder.setOrderId(orderItem.orderId());
        workOrder.setOrderItemId(orderItem.id());
        workOrder.setOrderItemNameSnapshot(orderItem.itemName());
        workOrder.setProductTypeSnapshot(orderItem.productType());
        workOrder.setQuantitySnapshot(orderItem.quantity());
        workOrder.setStatus(ProductionWorkOrderStatus.DRAFT.name());
        workOrder.setPriority(request.priority());
        workOrder.setInstructionTitle(request.instructionTitle());
        workOrder.setInstructionRemark(request.instructionRemark());
        workOrder.setProductionRequirement(request.productionRequirement());
        workOrder.setQualityRequirement(request.qualityRequirement());
        workOrder.setPackagingRequirement(request.packagingRequirement());
        workOrder.setShippingRequirement(request.shippingRequirement());
        workOrder.setDeliveryRequirement(request.deliveryRequirement());
        workOrder.setPlannedStartDate(request.plannedStartDate());
        workOrder.setPlannedFinishDate(request.plannedFinishDate());
        workOrder.setRequiredDeliveryDate(request.requiredDeliveryDate());
        workOrder.setDeadlineRemark(request.deadlineRemark());
        workOrder.setEquipmentModel(request.equipmentModel());
        workOrder.setTechnicalConfigSummary(request.technicalConfigSummary());
        workOrder.setTechnicalConfigRemark(request.technicalConfigRemark());
        workOrder.setTechnicalConfigJson(request.technicalConfigJson());
        workOrder.setResponsibleUserId(request.responsibleUserId());
        workOrder.setHandlerUserId(request.handlerUserId());
        workOrder.setProductionManagerId(request.productionManagerId());
        workOrder.setPrimaryWorkerId(request.primaryWorkerId());
        workOrder.setCustomerAcceptanceRequired(Boolean.TRUE.equals(request.customerAcceptanceRequired()));
        workOrder.setAcceptanceRemark(request.acceptanceRemark());
        workOrder.setCreatedBy(operatorId);
        workOrder.setCreatedAt(now);
        workOrder.setUpdatedBy(operatorId);
        workOrder.setUpdatedAt(now);
        workOrder.setDeleted(false);
        workOrder.setDeleteMarker(0L);
        return workOrder;
    }

    private void insertMaterials(
            ProductionWorkOrder workOrder,
            List<ProductionWorkOrderMaterialRequest> materials,
            Long operatorId) {
        if (materials == null) {
            return;
        }
        for (ProductionWorkOrderMaterialRequest request : materials) {
            materialMapper.insert(createMaterial(workOrder, request, operatorId));
        }
    }

    private ProductionWorkOrderMaterial createMaterial(
            ProductionWorkOrder workOrder,
            ProductionWorkOrderMaterialRequest request,
            Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        ProductionWorkOrderMaterial material = new ProductionWorkOrderMaterial();
        material.setTenantId(workOrder.getTenantId());
        material.setWorkOrderId(workOrder.getId());
        material.setOrderId(workOrder.getOrderId());
        material.setOrderItemId(workOrder.getOrderItemId());
        material.setMaterialId(request.materialId());
        material.setMaterialCode(request.materialCode());
        material.setMaterialName(request.materialName().trim());
        material.setSpec(request.spec());
        material.setUnit(request.unit());
        material.setRequiredQty(request.requiredQty());
        material.setUsageStage(request.usageStage());
        material.setRelatedStepTemplateId(request.relatedStepTemplateId());
        material.setRelatedStepInstanceId(request.relatedStepInstanceId());
        material.setRequirementStatus(ProductionWorkOrderStatus.DRAFT.name());
        material.setRemark(request.remark());
        material.setCreatedBy(operatorId);
        material.setCreatedAt(now);
        material.setUpdatedBy(operatorId);
        material.setUpdatedAt(now);
        material.setDeleted(false);
        material.setDeleteMarker(0L);
        return material;
    }

    private void updateStatus(Long workOrderId, String status, Long operatorId, LocalDateTime now) {
        workOrderMapper.update(null, new LambdaUpdateWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getId, workOrderId)
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .set(ProductionWorkOrder::getStatus, status)
                .set(ProductionWorkOrder::getUpdatedAt, now)
                .set(ProductionWorkOrder::getUpdatedBy, operatorId));
    }

    private void ensureTerminalStatusNotMutated(ProductionWorkOrder workOrder) {
        if (ProductionWorkOrderStatus.CANCELLED.name().equals(workOrder.getStatus())) {
            throw new ProductionWorkOrderException("WORK_ORDER_CANCELLED");
        }
        if (ProductionWorkOrderStatus.COMPLETED.name().equals(workOrder.getStatus())) {
            throw new ProductionWorkOrderException("WORK_ORDER_COMPLETED");
        }
    }
}
