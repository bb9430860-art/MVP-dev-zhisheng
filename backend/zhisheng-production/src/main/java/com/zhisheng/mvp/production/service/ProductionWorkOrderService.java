package com.zhisheng.mvp.production.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhisheng.mvp.inventory.entity.InventoryStock;
import com.zhisheng.mvp.inventory.mapper.InventoryStockMapper;
import com.zhisheng.mvp.process.entity.ProcessRouteTemplate;
import com.zhisheng.mvp.process.entity.ProcessStepMaterialRequirementTemplate;
import com.zhisheng.mvp.process.entity.ProcessStepTemplate;
import com.zhisheng.mvp.process.mapper.ProcessRouteTemplateMapper;
import com.zhisheng.mvp.process.mapper.ProcessStepMaterialRequirementTemplateMapper;
import com.zhisheng.mvp.process.mapper.ProcessStepTemplateMapper;
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
import com.zhisheng.mvp.production.dto.WorkOrderMaterialGenerationItemResponse;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialGenerationRequest;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialGenerationResponse;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessCreateRequest;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessItemResponse;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessPreviewRequest;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessResponse;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessStepResponse;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessSummaryResponse;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final String READINESS_READY = "READY";
    private static final String READINESS_SHORTAGE = "SHORTAGE";
    private static final String READINESS_UNLINKED_MATERIAL = "UNLINKED_MATERIAL";
    private static final String READINESS_NO_STOCK_RECORD = "NO_STOCK_RECORD";

    private final ProductionWorkOrderMapper workOrderMapper;
    private final ProductionWorkOrderMaterialMapper materialMapper;
    private final ProductionRouteInstanceMapper routeInstanceMapper;
    private final InventoryStockMapper inventoryStockMapper;
    private final ProcessRouteTemplateMapper routeTemplateMapper;
    private final ProcessStepTemplateMapper stepTemplateMapper;
    private final ProcessStepMaterialRequirementTemplateMapper stepMaterialTemplateMapper;
    private final OrderItemReadPort orderItemReadPort;
    private final OrderItemCandidateReadPort orderItemCandidateReadPort;
    private final WorkOrderNoGenerator workOrderNoGenerator;

    public ProductionWorkOrderService(
            ProductionWorkOrderMapper workOrderMapper,
            ProductionWorkOrderMaterialMapper materialMapper,
            ProductionRouteInstanceMapper routeInstanceMapper,
            InventoryStockMapper inventoryStockMapper,
            ProcessRouteTemplateMapper routeTemplateMapper,
            ProcessStepTemplateMapper stepTemplateMapper,
            ProcessStepMaterialRequirementTemplateMapper stepMaterialTemplateMapper,
            OrderItemReadPort orderItemReadPort,
            OrderItemCandidateReadPort orderItemCandidateReadPort,
            WorkOrderNoGenerator workOrderNoGenerator) {
        this.workOrderMapper = workOrderMapper;
        this.materialMapper = materialMapper;
        this.routeInstanceMapper = routeInstanceMapper;
        this.inventoryStockMapper = inventoryStockMapper;
        this.routeTemplateMapper = routeTemplateMapper;
        this.stepTemplateMapper = stepTemplateMapper;
        this.stepMaterialTemplateMapper = stepMaterialTemplateMapper;
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

    public WorkOrderMaterialGenerationResponse previewMaterialGeneration(Long workOrderId, Long routeTemplateId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        return generateMaterialResponse(workOrder, routeTemplateId, 0);
    }

    public WorkOrderMaterialReadinessResponse previewCreateMaterialReadiness(
            WorkOrderMaterialReadinessPreviewRequest request) {
        if (request == null || request.orderItemId() == null) {
            throw new ProductionWorkOrderException("ORDER_ITEM_NOT_FOUND");
        }
        OrderItemProductionContext orderItem = orderItemReadPort.findById(request.orderItemId())
                .orElseThrow(() -> new ProductionWorkOrderException("ORDER_ITEM_NOT_FOUND"));
        return generateReadinessResponse(orderItem, request.routeTemplateId());
    }

    @Transactional
    public ProductionWorkOrder createWithMaterialReadiness(
            WorkOrderMaterialReadinessCreateRequest request,
            Long operatorId) {
        if (request == null || request.orderItemId() == null) {
            throw new ProductionWorkOrderException("ORDER_ITEM_NOT_FOUND");
        }
        if (!Boolean.TRUE.equals(request.applyGeneratedMaterials())) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_GENERATION_EMPTY");
        }
        OrderItemProductionContext orderItem = orderItemReadPort.findById(request.orderItemId())
                .orElseThrow(() -> new ProductionWorkOrderException("ORDER_ITEM_NOT_FOUND"));
        ensureNoActiveWorkOrder(orderItem.id());
        WorkOrderMaterialReadinessResponse readiness = generateReadinessResponse(orderItem, request.routeTemplateId());
        ProductionWorkOrderCreateRequest createRequest = mergeCreateFields(request.orderItemId(), request.workOrderFields());

        ProductionWorkOrder workOrder = null;
        for (int attempt = 0; attempt < MAX_NO_RETRY; attempt++) {
            workOrder = createWorkOrder(orderItem, createRequest, operatorId);
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
        insertReadinessMaterials(workOrder, flattenReadinessItems(readiness), operatorId);
        return workOrderMapper.selectById(workOrder.getId());
    }

    @Transactional
    public WorkOrderMaterialGenerationResponse generateMaterialsFromTemplate(
            Long workOrderId,
            WorkOrderMaterialGenerationRequest request,
            Long operatorId) {
        ProductionWorkOrder workOrder = requiredWorkOrder(workOrderId);
        if (!ProductionWorkOrderStatus.DRAFT.name().equals(workOrder.getStatus())) {
            throw new ProductionWorkOrderException("WORK_ORDER_NOT_DRAFT");
        }
        if (request == null || !Boolean.TRUE.equals(request.replaceExisting())) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_REPLACE_REJECTED");
        }
        WorkOrderMaterialGenerationResponse preview = generateMaterialResponse(workOrder, request.routeTemplateId(), 0);
        int replacedCount = Math.toIntExact(materialMapper.selectCount(new LambdaQueryWrapper<ProductionWorkOrderMaterial>()
                .eq(ProductionWorkOrderMaterial::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionWorkOrderMaterial::getWorkOrderId, workOrder.getId())
                .eq(ProductionWorkOrderMaterial::getDeleted, false)));
        materialMapper.delete(new LambdaQueryWrapper<ProductionWorkOrderMaterial>()
                .eq(ProductionWorkOrderMaterial::getTenantId, DEFAULT_TENANT_ID)
                .eq(ProductionWorkOrderMaterial::getWorkOrderId, workOrder.getId()));
        insertGeneratedMaterials(workOrder, preview.generatedMaterials(), operatorId);
        workOrderMapper.update(null, new LambdaUpdateWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getId, workOrder.getId())
                .eq(ProductionWorkOrder::getTenantId, DEFAULT_TENANT_ID)
                .set(ProductionWorkOrder::getUpdatedAt, LocalDateTime.now())
                .set(ProductionWorkOrder::getUpdatedBy, operatorId));
        return new WorkOrderMaterialGenerationResponse(
                preview.generatedMaterials(),
                preview.generatedCount(),
                replacedCount,
                preview.warnings());
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

    private ProductionWorkOrderCreateRequest mergeCreateFields(
            Long orderItemId,
            ProductionWorkOrderCreateRequest fields) {
        if (fields == null) {
            return new ProductionWorkOrderCreateRequest(
                    orderItemId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        return new ProductionWorkOrderCreateRequest(
                orderItemId,
                fields.priority(),
                fields.instructionTitle(),
                fields.instructionRemark(),
                fields.productionRequirement(),
                fields.qualityRequirement(),
                fields.packagingRequirement(),
                fields.shippingRequirement(),
                fields.deliveryRequirement(),
                fields.plannedStartDate(),
                fields.plannedFinishDate(),
                fields.requiredDeliveryDate(),
                fields.deadlineRemark(),
                fields.equipmentModel(),
                fields.technicalConfigSummary(),
                fields.technicalConfigRemark(),
                fields.technicalConfigJson(),
                fields.responsibleUserId(),
                fields.handlerUserId(),
                fields.productionManagerId(),
                fields.primaryWorkerId(),
                fields.customerAcceptanceRequired(),
                fields.acceptanceRemark(),
                null);
    }

    private WorkOrderMaterialGenerationResponse generateMaterialResponse(
            ProductionWorkOrder workOrder,
            Long routeTemplateId,
            int replacedCount) {
        ProcessRouteTemplate routeTemplate = requiredEnabledRouteTemplate(routeTemplateId);
        List<ProcessStepMaterialRequirementTemplate> templates = loadStepMaterialTemplates(routeTemplate.getId());
        if (templates.isEmpty()) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_GENERATION_EMPTY");
        }
        Map<Long, ProcessStepTemplate> stepsById = loadStepsById(routeTemplate.getId());
        List<String> warnings = new ArrayList<>();
        List<WorkOrderMaterialGenerationItemResponse> generated = templates.stream()
                .map(template -> toGeneratedMaterial(workOrder, template, stepsById.get(template.getStepTemplateId()), warnings))
                .toList();
        if (generated.isEmpty()) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_GENERATION_EMPTY");
        }
        return new WorkOrderMaterialGenerationResponse(generated, generated.size(), replacedCount, warnings);
    }

    private WorkOrderMaterialReadinessResponse generateReadinessResponse(
            OrderItemProductionContext orderItem,
            Long routeTemplateId) {
        ProcessRouteTemplate routeTemplate = requiredEnabledRouteTemplate(routeTemplateId);
        List<ProcessStepMaterialRequirementTemplate> templates = loadStepMaterialTemplates(routeTemplate.getId());
        if (templates.isEmpty()) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_GENERATION_EMPTY");
        }
        Map<Long, ProcessStepTemplate> stepsById = loadStepsById(routeTemplate.getId());
        Map<Long, InventoryStock> stockByMaterialId = loadStockByMaterialId(templates);
        Map<Long, List<WorkOrderMaterialReadinessItemResponse>> materialsByStep = new LinkedHashMap<>();
        int readyLines = 0;
        int shortageLines = 0;
        int unlinkedLines = 0;
        int noStockRecordLines = 0;
        for (ProcessStepMaterialRequirementTemplate template : templates) {
            WorkOrderMaterialReadinessItemResponse item = toReadinessMaterial(
                    orderItem.quantity(),
                    template,
                    stockByMaterialId.get(template.getMaterialId()));
            materialsByStep.computeIfAbsent(template.getStepTemplateId(), ignored -> new ArrayList<>()).add(item);
            switch (item.readinessStatus()) {
                case READINESS_READY -> readyLines++;
                case READINESS_SHORTAGE -> shortageLines++;
                case READINESS_UNLINKED_MATERIAL -> unlinkedLines++;
                case READINESS_NO_STOCK_RECORD -> noStockRecordLines++;
                default -> throw new ProductionWorkOrderException("MATERIAL_READINESS_PREVIEW_FAILED");
            }
        }
        if (materialsByStep.isEmpty()) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_GENERATION_EMPTY");
        }
        List<WorkOrderMaterialReadinessStepResponse> itemsByStep = materialsByStep.entrySet().stream()
                .map(entry -> {
                    ProcessStepTemplate step = stepsById.get(entry.getKey());
                    return new WorkOrderMaterialReadinessStepResponse(
                            entry.getKey(),
                            step == null ? null : step.getStepOrder(),
                            step == null ? null : step.getStepName(),
                            entry.getValue());
                })
                .toList();
        return new WorkOrderMaterialReadinessResponse(
                orderItem.quantity(),
                itemsByStep,
                new WorkOrderMaterialReadinessSummaryResponse(
                        readyLines + shortageLines + unlinkedLines + noStockRecordLines,
                        readyLines,
                        shortageLines,
                        unlinkedLines,
                        noStockRecordLines));
    }

    private List<ProcessStepMaterialRequirementTemplate> loadStepMaterialTemplates(Long routeTemplateId) {
        return stepMaterialTemplateMapper.selectList(
                new LambdaQueryWrapper<ProcessStepMaterialRequirementTemplate>()
                        .eq(ProcessStepMaterialRequirementTemplate::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ProcessStepMaterialRequirementTemplate::getRouteTemplateId, routeTemplateId)
                        .eq(ProcessStepMaterialRequirementTemplate::getEnabled, true)
                        .eq(ProcessStepMaterialRequirementTemplate::getDeleted, false)
                        .orderByAsc(ProcessStepMaterialRequirementTemplate::getStepTemplateId)
                        .orderByAsc(ProcessStepMaterialRequirementTemplate::getId));
    }

    private Map<Long, InventoryStock> loadStockByMaterialId(List<ProcessStepMaterialRequirementTemplate> templates) {
        List<Long> materialIds = templates.stream()
                .map(ProcessStepMaterialRequirementTemplate::getMaterialId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return inventoryStockMapper.selectList(new LambdaQueryWrapper<InventoryStock>()
                        .eq(InventoryStock::getTenantId, DEFAULT_TENANT_ID)
                        .in(InventoryStock::getMaterialId, materialIds))
                .stream()
                .collect(Collectors.toMap(InventoryStock::getMaterialId, Function.identity(), (left, right) -> left));
    }

    private ProcessRouteTemplate requiredEnabledRouteTemplate(Long routeTemplateId) {
        if (routeTemplateId == null) {
            throw new ProductionWorkOrderException("PROCESS_ROUTE_TEMPLATE_NOT_FOUND");
        }
        ProcessRouteTemplate routeTemplate = routeTemplateMapper.selectById(routeTemplateId);
        if (routeTemplate == null
                || !Long.valueOf(DEFAULT_TENANT_ID).equals(routeTemplate.getTenantId())
                || Boolean.TRUE.equals(routeTemplate.getDeleted())) {
            throw new ProductionWorkOrderException("PROCESS_ROUTE_TEMPLATE_NOT_FOUND");
        }
        if (!Boolean.TRUE.equals(routeTemplate.getEnabled())) {
            throw new ProductionWorkOrderException("PROCESS_ROUTE_TEMPLATE_DISABLED");
        }
        return routeTemplate;
    }

    private Map<Long, ProcessStepTemplate> loadStepsById(Long routeTemplateId) {
        return stepTemplateMapper.selectList(new LambdaQueryWrapper<ProcessStepTemplate>()
                        .eq(ProcessStepTemplate::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ProcessStepTemplate::getRouteTemplateId, routeTemplateId)
                        .eq(ProcessStepTemplate::getDeleted, false))
                .stream()
                .collect(Collectors.toMap(ProcessStepTemplate::getId, Function.identity(), (left, right) -> left));
    }

    private WorkOrderMaterialGenerationItemResponse toGeneratedMaterial(
            ProductionWorkOrder workOrder,
            ProcessStepMaterialRequirementTemplate template,
            ProcessStepTemplate step,
            List<String> warnings) {
        BigDecimal requiredQty = calculateRequiredQty(workOrder.getQuantitySnapshot(), template);
        if (requiredQty.signum() <= 0) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_QUANTITY_INVALID");
        }
        String warning = null;
        if (StringUtils.hasText(template.getRequiredQtyExpression())) {
            warning = "required_qty_expression ignored in MVP: " + template.getRequiredQtyExpression().trim();
            warnings.add(warning);
        }
        return new WorkOrderMaterialGenerationItemResponse(
                template.getMaterialId(),
                template.getMaterialCode(),
                template.getMaterialName(),
                template.getSpec(),
                template.getUnit(),
                requiredQty,
                template.getUsageStage(),
                template.getStepTemplateId(),
                step == null ? null : step.getStepName(),
                step == null ? null : step.getStepOrder(),
                template.getStepTemplateId(),
                null,
                quantityRuleSummary(template),
                warning,
                template.getRemark());
    }

    private WorkOrderMaterialReadinessItemResponse toReadinessMaterial(
            BigDecimal quantitySnapshot,
            ProcessStepMaterialRequirementTemplate template,
            InventoryStock stock) {
        BigDecimal requiredQty = calculateRequiredQty(quantitySnapshot, template);
        if (requiredQty.signum() <= 0) {
            throw new ProductionWorkOrderException("WORK_ORDER_MATERIAL_QUANTITY_INVALID");
        }
        String warning = null;
        if (StringUtils.hasText(template.getRequiredQtyExpression())) {
            warning = "required_qty_expression ignored in MVP: " + template.getRequiredQtyExpression().trim();
        }
        BigDecimal availableQty = null;
        BigDecimal shortageQty = null;
        String readinessStatus;
        String readinessMessage;
        if (template.getMaterialId() == null) {
            readinessStatus = READINESS_UNLINKED_MATERIAL;
            readinessMessage = "未关联库存物料，无法核对";
        } else if (stock == null) {
            availableQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            shortageQty = requiredQty;
            readinessStatus = READINESS_NO_STOCK_RECORD;
            readinessMessage = "无库存记录";
        } else {
            availableQty = defaultZero(stock.getAvailableQty()).setScale(4, RoundingMode.HALF_UP);
            if (availableQty.compareTo(requiredQty) >= 0) {
                shortageQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                readinessStatus = READINESS_READY;
                readinessMessage = "库存满足";
            } else {
                shortageQty = requiredQty.subtract(availableQty).setScale(4, RoundingMode.HALF_UP);
                readinessStatus = READINESS_SHORTAGE;
                readinessMessage = "库存不足";
            }
        }
        return new WorkOrderMaterialReadinessItemResponse(
                template.getMaterialId(),
                template.getMaterialCode(),
                template.getMaterialName(),
                template.getSpec(),
                template.getUnit(),
                requiredQty,
                availableQty,
                shortageQty,
                readinessStatus,
                readinessMessage,
                template.getUsageStage(),
                template.getStepTemplateId(),
                null,
                quantityRuleSummary(template),
                warning,
                template.getRemark());
    }

    private BigDecimal calculateRequiredQty(
            BigDecimal quantitySnapshot,
            ProcessStepMaterialRequirementTemplate template) {
        BigDecimal quantity = defaultZero(quantitySnapshot);
        BigDecimal baseQty = defaultZero(template.getBaseQtyPerUnit()).multiply(quantity);
        BigDecimal fixedQty = defaultZero(template.getFixedQty());
        BigDecimal lossMultiplier = BigDecimal.ONE.add(defaultZero(template.getLossRate()));
        return baseQty.add(fixedQty).multiply(lossMultiplier).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String quantityRuleSummary(ProcessStepMaterialRequirementTemplate template) {
        return "baseQtyPerUnit="
                + defaultZero(template.getBaseQtyPerUnit()).toPlainString()
                + ", fixedQty="
                + defaultZero(template.getFixedQty()).toPlainString()
                + ", lossRate="
                + defaultZero(template.getLossRate()).toPlainString();
    }

    private void insertGeneratedMaterials(
            ProductionWorkOrder workOrder,
            List<WorkOrderMaterialGenerationItemResponse> materials,
            Long operatorId) {
        for (WorkOrderMaterialGenerationItemResponse item : materials) {
            ProductionWorkOrderMaterial material = createGeneratedMaterial(workOrder, item, operatorId);
            materialMapper.insert(material);
        }
    }

    private List<WorkOrderMaterialReadinessItemResponse> flattenReadinessItems(
            WorkOrderMaterialReadinessResponse readiness) {
        return readiness.itemsByStep().stream()
                .flatMap(step -> step.materials().stream())
                .toList();
    }

    private void insertReadinessMaterials(
            ProductionWorkOrder workOrder,
            List<WorkOrderMaterialReadinessItemResponse> materials,
            Long operatorId) {
        LocalDateTime checkedAt = LocalDateTime.now();
        for (WorkOrderMaterialReadinessItemResponse item : materials) {
            materialMapper.insert(createReadinessMaterial(workOrder, item, operatorId, checkedAt));
        }
    }

    private ProductionWorkOrderMaterial createGeneratedMaterial(
            ProductionWorkOrder workOrder,
            WorkOrderMaterialGenerationItemResponse item,
            Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        ProductionWorkOrderMaterial material = new ProductionWorkOrderMaterial();
        material.setTenantId(workOrder.getTenantId());
        material.setWorkOrderId(workOrder.getId());
        material.setOrderId(workOrder.getOrderId());
        material.setOrderItemId(workOrder.getOrderItemId());
        material.setMaterialId(item.materialId());
        material.setMaterialCode(item.materialCode());
        material.setMaterialName(item.materialName());
        material.setSpec(item.spec());
        material.setUnit(item.unit());
        material.setRequiredQty(item.requiredQty());
        material.setUsageStage(item.usageStage());
        material.setRelatedStepTemplateId(item.relatedStepTemplateId());
        material.setRelatedStepInstanceId(null);
        material.setRequirementStatus(ProductionWorkOrderStatus.DRAFT.name());
        material.setRemark(item.remark());
        material.setCreatedBy(operatorId);
        material.setCreatedAt(now);
        material.setUpdatedBy(operatorId);
        material.setUpdatedAt(now);
        material.setDeleted(false);
        material.setDeleteMarker(0L);
        return material;
    }

    private ProductionWorkOrderMaterial createReadinessMaterial(
            ProductionWorkOrder workOrder,
            WorkOrderMaterialReadinessItemResponse item,
            Long operatorId,
            LocalDateTime checkedAt) {
        LocalDateTime now = LocalDateTime.now();
        ProductionWorkOrderMaterial material = new ProductionWorkOrderMaterial();
        material.setTenantId(workOrder.getTenantId());
        material.setWorkOrderId(workOrder.getId());
        material.setOrderId(workOrder.getOrderId());
        material.setOrderItemId(workOrder.getOrderItemId());
        material.setMaterialId(item.materialId());
        material.setMaterialCode(item.materialCode());
        material.setMaterialName(item.materialName());
        material.setSpec(item.spec());
        material.setUnit(item.unit());
        material.setRequiredQty(item.requiredQty());
        material.setAvailableQtySnapshot(item.availableQty());
        material.setShortageQty(item.shortageQty());
        material.setReadinessStatus(item.readinessStatus());
        material.setReadinessCheckedAt(checkedAt);
        material.setReadinessMessage(item.readinessMessage());
        material.setUsageStage(item.usageStage());
        material.setRelatedStepTemplateId(item.relatedStepTemplateId());
        material.setRelatedStepInstanceId(null);
        material.setRequirementStatus(ProductionWorkOrderStatus.DRAFT.name());
        material.setRemark(item.remark());
        material.setCreatedBy(operatorId);
        material.setCreatedAt(now);
        material.setUpdatedBy(operatorId);
        material.setUpdatedAt(now);
        material.setDeleted(false);
        material.setDeleteMarker(0L);
        return material;
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
              material.getAvailableQtySnapshot(),
              material.getShortageQty(),
              material.getReadinessStatus(),
              material.getReadinessCheckedAt(),
              material.getReadinessMessage(),
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
