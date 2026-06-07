package com.zhisheng.mvp.production.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhisheng.mvp.production.dto.PageResponse;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCandidateQuery;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCandidateResponse;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCreateRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderMaterialResponse;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderMaterialRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderMaterialsUpdateRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderQuery;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderResponse;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderUpdateRequest;
import com.zhisheng.mvp.production.entity.ProductionRouteInstance;
import com.zhisheng.mvp.production.entity.ProductionWorkOrder;
import com.zhisheng.mvp.production.entity.ProductionWorkOrderMaterial;
import com.zhisheng.mvp.production.enums.ProductionWorkOrderStatus;
import com.zhisheng.mvp.production.exception.ProductionWorkOrderException;
import com.zhisheng.mvp.production.mapper.ProductionRouteInstanceMapper;
import com.zhisheng.mvp.production.mapper.ProductionWorkOrderMapper;
import com.zhisheng.mvp.production.mapper.ProductionWorkOrderMaterialMapper;
import com.zhisheng.mvp.production.port.OrderItemCandidateContext;
import com.zhisheng.mvp.production.port.OrderItemCandidateQuery;
import com.zhisheng.mvp.production.port.OrderItemCandidateReadPort;
import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import com.zhisheng.mvp.production.port.OrderItemReadPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final OrderItemCandidateReadPort orderItemCandidateReadPort;
    private final WorkOrderNoGenerator workOrderNoGenerator;

    public ProductionWorkOrderService(
            ProductionWorkOrderMapper workOrderMapper,
            ProductionWorkOrderMaterialMapper materialMapper,
            ProductionRouteInstanceMapper routeInstanceMapper,
            OrderItemReadPort orderItemReadPort,
            OrderItemCandidateReadPort orderItemCandidateReadPort,
            WorkOrderNoGenerator workOrderNoGenerator) {
        this.workOrderMapper = workOrderMapper;
        this.materialMapper = materialMapper;
        this.routeInstanceMapper = routeInstanceMapper;
        this.orderItemReadPort = orderItemReadPort;
        this.orderItemCandidateReadPort = orderItemCandidateReadPort;
        this.workOrderNoGenerator = workOrderNoGenerator;
    }

    public PageResponse<ProductionWorkOrderCandidateResponse> listOrderItemCandidates(
            ProductionWorkOrderCandidateQuery query) {
        List<OrderItemCandidateContext> candidates = orderItemCandidateReadPort.listCandidates(new OrderItemCandidateQuery(
                query == null ? null : query.keyword(),
                query == null ? null : query.productType(),
                query == null ? null : query.productionStatus(),
                query == null ? null : query.orderNo(),
                query == null ? null : query.orderType(),
                query == null ? null : query.customerType()));
        Map<Long, ProductionWorkOrder> activeByOrderItem = activeWorkOrdersByOrderItem(
                candidates.stream().map(OrderItemCandidateContext::id).toList());
        List<ProductionWorkOrderCandidateResponse> rows = candidates.stream()
                .map(candidate -> toCandidateResponse(candidate, activeByOrderItem.get(candidate.id())))
                .filter(row -> query == null
                        || query.hasActiveWorkOrder() == null
                        || query.hasActiveWorkOrder().equals(row.hasActiveWorkOrder()))
                .toList();
        long page = normalizePage(query == null ? null : query.page());
        long pageSize = normalizePageSize(query == null ? null : query.pageSize());
        return page(rows, page, pageSize);
    }

    public PageResponse<ProductionWorkOrderResponse> listWorkOrders(ProductionWorkOrderQuery query) {
        long page = normalizePage(query == null ? null : query.page());
        long pageSize = normalizePageSize(query == null ? null : query.pageSize());
        LambdaQueryWrapper<ProductionWorkOrder> wrapper = baseWorkOrderQuery();
        if (query != null) {
            if (StringUtils.hasText(query.status())) {
                wrapper.eq(ProductionWorkOrder::getStatus, query.status());
            }
            if (StringUtils.hasText(query.workOrderNo())) {
                wrapper.like(ProductionWorkOrder::getWorkOrderNo, query.workOrderNo());
            }
            if (query.orderItemId() != null) {
                wrapper.eq(ProductionWorkOrder::getOrderItemId, query.orderItemId());
            }
            if (StringUtils.hasText(query.keyword())) {
                wrapper.like(ProductionWorkOrder::getOrderItemNameSnapshot, query.keyword());
            }
            if (query.plannedStartFrom() != null) {
                wrapper.ge(ProductionWorkOrder::getPlannedStartDate, query.plannedStartFrom());
            }
            if (query.plannedStartTo() != null) {
                wrapper.le(ProductionWorkOrder::getPlannedStartDate, query.plannedStartTo());
            }
            if (query.requiredDeliveryFrom() != null) {
                wrapper.ge(ProductionWorkOrder::getRequiredDeliveryDate, query.requiredDeliveryFrom());
            }
            if (query.requiredDeliveryTo() != null) {
                wrapper.le(ProductionWorkOrder::getRequiredDeliveryDate, query.requiredDeliveryTo());
            }
            if (Boolean.TRUE.equals(query.routeLinked())) {
                wrapper.isNotNull(ProductionWorkOrder::getProductionRouteInstanceId);
            } else if (Boolean.FALSE.equals(query.routeLinked())) {
                wrapper.isNull(ProductionWorkOrder::getProductionRouteInstanceId);
            }
        }
        wrapper.orderByDesc(ProductionWorkOrder::getUpdatedAt).orderByDesc(ProductionWorkOrder::getId);
        Page<ProductionWorkOrder> result = workOrderMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResponse<>(
                result.getRecords().stream().map(workOrder -> toResponse(workOrder, false)).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    public ProductionWorkOrderResponse detail(Long workOrderId) {
        return toResponse(requiredWorkOrder(workOrderId), true);
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
    public ProductionWorkOrder updateDraft(Long workOrderId, ProductionWorkOrderUpdateRequest request, Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        ensureDraftEditable(workOrder);
        LocalDateTime now = LocalDateTime.now();
        workOrderMapper.update(null, new LambdaUpdateWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getId, workOrder.getId())
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .set(ProductionWorkOrder::getPriority, request.priority())
                .set(ProductionWorkOrder::getInstructionTitle, request.instructionTitle())
                .set(ProductionWorkOrder::getInstructionRemark, request.instructionRemark())
                .set(ProductionWorkOrder::getProductionRequirement, request.productionRequirement())
                .set(ProductionWorkOrder::getQualityRequirement, request.qualityRequirement())
                .set(ProductionWorkOrder::getPackagingRequirement, request.packagingRequirement())
                .set(ProductionWorkOrder::getShippingRequirement, request.shippingRequirement())
                .set(ProductionWorkOrder::getDeliveryRequirement, request.deliveryRequirement())
                .set(ProductionWorkOrder::getPlannedStartDate, request.plannedStartDate())
                .set(ProductionWorkOrder::getPlannedFinishDate, request.plannedFinishDate())
                .set(ProductionWorkOrder::getRequiredDeliveryDate, request.requiredDeliveryDate())
                .set(ProductionWorkOrder::getDeadlineRemark, request.deadlineRemark())
                .set(ProductionWorkOrder::getEquipmentModel, request.equipmentModel())
                .set(ProductionWorkOrder::getTechnicalConfigSummary, request.technicalConfigSummary())
                .set(ProductionWorkOrder::getTechnicalConfigRemark, request.technicalConfigRemark())
                .set(ProductionWorkOrder::getTechnicalConfigJson, request.technicalConfigJson())
                .set(ProductionWorkOrder::getResponsibleUserId, request.responsibleUserId())
                .set(ProductionWorkOrder::getHandlerUserId, request.handlerUserId())
                .set(ProductionWorkOrder::getProductionManagerId, request.productionManagerId())
                .set(ProductionWorkOrder::getPrimaryWorkerId, request.primaryWorkerId())
                .set(ProductionWorkOrder::getCustomerAcceptanceRequired,
                        Boolean.TRUE.equals(request.customerAcceptanceRequired()))
                .set(ProductionWorkOrder::getAcceptanceRemark, request.acceptanceRemark())
                .set(ProductionWorkOrder::getUpdatedAt, now)
                .set(ProductionWorkOrder::getUpdatedBy, operatorId));
        return requiredWorkOrder(workOrderId);
    }

    @Transactional
    public ProductionWorkOrder replaceDraftMaterials(
            Long workOrderId,
            ProductionWorkOrderMaterialsUpdateRequest request,
            Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        ensureDraftEditable(workOrder);
        List<ProductionWorkOrderMaterialRequest> materials = request == null ? null : request.materials();
        validateMaterials(materials);
        materialMapper.delete(new LambdaQueryWrapper<ProductionWorkOrderMaterial>()
                .eq(ProductionWorkOrderMaterial::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionWorkOrderMaterial::getWorkOrderId, workOrder.getId()));
        insertMaterials(workOrder, materials, operatorId);
        workOrderMapper.update(null, new LambdaUpdateWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getId, workOrder.getId())
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .set(ProductionWorkOrder::getUpdatedAt, LocalDateTime.now())
                .set(ProductionWorkOrder::getUpdatedBy, operatorId));
        return requiredWorkOrder(workOrderId);
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

    private Map<Long, ProductionWorkOrder> activeWorkOrdersByOrderItem(List<Long> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return Map.of();
        }
        return workOrderMapper.selectList(new LambdaQueryWrapper<ProductionWorkOrder>()
                        .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ProductionWorkOrder::getDeleted, false)
                        .in(ProductionWorkOrder::getOrderItemId, orderItemIds)
                        .in(ProductionWorkOrder::getStatus, activeStatuses())
                        .orderByDesc(ProductionWorkOrder::getUpdatedAt)
                        .orderByDesc(ProductionWorkOrder::getId))
                .stream()
                .collect(Collectors.toMap(
                        ProductionWorkOrder::getOrderItemId,
                        Function.identity(),
                        (left, right) -> left));
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

    private LambdaQueryWrapper<ProductionWorkOrder> baseWorkOrderQuery() {
        return new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionWorkOrder::getDeleted, false);
    }

    private List<String> activeStatuses() {
        return List.of(
                ProductionWorkOrderStatus.DRAFT.name(),
                ProductionWorkOrderStatus.RELEASED.name(),
                ProductionWorkOrderStatus.IN_PROGRESS.name());
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

    private void ensureDraftEditable(ProductionWorkOrder workOrder) {
        ensureTerminalStatusNotMutated(workOrder);
        if (!ProductionWorkOrderStatus.DRAFT.name().equals(workOrder.getStatus())) {
            throw new ProductionWorkOrderException("WORK_ORDER_EDIT_NOT_ALLOWED");
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

    private ProductionWorkOrderCandidateResponse toCandidateResponse(
            OrderItemCandidateContext candidate,
            ProductionWorkOrder activeWorkOrder) {
        return new ProductionWorkOrderCandidateResponse(
                candidate.id(),
                candidate.orderId(),
                candidate.orderNo(),
                candidate.orderType(),
                candidate.customerType(),
                candidate.dealOwnerId(),
                candidate.dealOwnerName(),
                candidate.itemName(),
                candidate.spec(),
                candidate.unit(),
                candidate.quantity(),
                candidate.remark(),
                candidate.productType(),
                candidate.productionStatus(),
                candidate.productionProgress(),
                candidate.productionRouteInstanceId(),
                activeWorkOrder != null,
                activeWorkOrder == null ? null : activeWorkOrder.getId(),
                activeWorkOrder == null ? null : activeWorkOrder.getWorkOrderNo());
    }

    private ProductionWorkOrderResponse toResponse(ProductionWorkOrder workOrder, boolean includeMaterials) {
        Optional<OrderItemCandidateContext> candidate = orderItemCandidateReadPort.findCandidateById(workOrder.getOrderItemId());
        List<ProductionWorkOrderMaterialResponse> materials = includeMaterials
                ? materialMapper.selectList(new LambdaQueryWrapper<ProductionWorkOrderMaterial>()
                                .eq(ProductionWorkOrderMaterial::getTenantId, DEFAULT_TENANT_ID)
                                .eq(ProductionWorkOrderMaterial::getWorkOrderId, workOrder.getId())
                                .eq(ProductionWorkOrderMaterial::getDeleted, false)
                                .orderByAsc(ProductionWorkOrderMaterial::getId))
                        .stream()
                        .map(this::toMaterialResponse)
                        .toList()
                : List.of();
        return new ProductionWorkOrderResponse(
                workOrder.getId(),
                workOrder.getWorkOrderNo(),
                workOrder.getOrderId(),
                candidate.map(OrderItemCandidateContext::orderNo).orElse(null),
                candidate.map(OrderItemCandidateContext::orderType).orElse(null),
                candidate.map(OrderItemCandidateContext::customerType).orElse(null),
                candidate.map(OrderItemCandidateContext::dealOwnerId).orElse(null),
                candidate.map(OrderItemCandidateContext::dealOwnerName).orElse(null),
                workOrder.getOrderItemId(),
                workOrder.getOrderItemNameSnapshot(),
                candidate.map(OrderItemCandidateContext::spec).orElse(null),
                candidate.map(OrderItemCandidateContext::unit).orElse(null),
                workOrder.getQuantitySnapshot(),
                candidate.map(OrderItemCandidateContext::remark).orElse(null),
                workOrder.getProductTypeSnapshot(),
                workOrder.getStatus(),
                workOrder.getPriority(),
                workOrder.getInstructionTitle(),
                workOrder.getInstructionRemark(),
                workOrder.getProductionRequirement(),
                workOrder.getQualityRequirement(),
                workOrder.getPackagingRequirement(),
                workOrder.getShippingRequirement(),
                workOrder.getDeliveryRequirement(),
                workOrder.getPlannedStartDate(),
                workOrder.getPlannedFinishDate(),
                workOrder.getRequiredDeliveryDate(),
                workOrder.getDeadlineRemark(),
                workOrder.getEquipmentModel(),
                workOrder.getTechnicalConfigSummary(),
                workOrder.getTechnicalConfigRemark(),
                workOrder.getTechnicalConfigJson(),
                workOrder.getResponsibleUserId(),
                workOrder.getHandlerUserId(),
                workOrder.getProductionManagerId(),
                workOrder.getPrimaryWorkerId(),
                workOrder.getReleasedBy(),
                workOrder.getReleasedAt(),
                workOrder.getConfirmedBy(),
                workOrder.getConfirmedAt(),
                workOrder.getProductionSignedBy(),
                workOrder.getProductionSignedAt(),
                workOrder.getWarehouseConfirmedBy(),
                workOrder.getWarehouseConfirmedAt(),
                workOrder.getQualityConfirmedBy(),
                workOrder.getQualityConfirmedAt(),
                workOrder.getCustomerAcceptanceRequired(),
                workOrder.getAcceptanceRemark(),
                workOrder.getProductionRouteInstanceId(),
                workOrder.getProductionRouteInstanceId() != null,
                workOrder.getCreatedAt(),
                workOrder.getUpdatedAt(),
                materials);
    }

    private ProductionWorkOrderMaterialResponse toMaterialResponse(ProductionWorkOrderMaterial material) {
        return new ProductionWorkOrderMaterialResponse(
                material.getId(),
                material.getMaterialId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                material.getSpec(),
                material.getUnit(),
                material.getRequiredQty(),
                material.getUsageStage(),
                material.getRelatedStepTemplateId(),
                material.getRelatedStepInstanceId(),
                material.getRequirementStatus(),
                material.getRemark(),
                material.getUpdatedAt());
    }

    private <T> PageResponse<T> page(List<T> rows, long page, long pageSize) {
        int from = (int) Math.min(rows.size(), Math.max(0, (page - 1) * pageSize));
        int to = (int) Math.min(rows.size(), from + pageSize);
        return new PageResponse<>(new ArrayList<>(rows.subList(from, to)), rows.size(), page, pageSize);
    }

    private long normalizePage(Long page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
