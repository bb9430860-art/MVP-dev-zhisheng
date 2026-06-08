package com.zhisheng.mvp.production.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.production.dto.DispatchConfigFromTemplateRequest;
import com.zhisheng.mvp.production.dto.DispatchConfigResponse;
import com.zhisheng.mvp.production.dto.DispatchProductionRequest;
import com.zhisheng.mvp.production.dto.PageResponse;
import com.zhisheng.mvp.production.dto.ProductionDispatchResponse;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCandidateQuery;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCandidateResponse;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderCreateRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderMaterialsUpdateRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderQuery;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderResponse;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderRouteLinkRequest;
import com.zhisheng.mvp.production.dto.ProductionWorkOrderUpdateRequest;
import com.zhisheng.mvp.production.dto.WorkOrderDispatchContextResponse;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialGenerationRequest;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialGenerationResponse;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessCreateRequest;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessPreviewRequest;
import com.zhisheng.mvp.production.dto.WorkOrderMaterialReadinessResponse;
import com.zhisheng.mvp.production.exception.ProductionWorkOrderException;
import com.zhisheng.mvp.production.port.CurrentProductionUserPort;
import com.zhisheng.mvp.production.service.ProductionDispatchService;
import com.zhisheng.mvp.production.service.ProductionWorkOrderService;
import java.time.LocalDate;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/api/production/work-orders")
public class ProductionWorkOrderController {

    private final ProductionWorkOrderService productionWorkOrderService;
    private final ProductionDispatchService productionDispatchService;
    private final CurrentProductionUserPort currentProductionUserPort;

    public ProductionWorkOrderController(
            ProductionWorkOrderService productionWorkOrderService,
            ProductionDispatchService productionDispatchService,
            CurrentProductionUserPort currentProductionUserPort) {
        this.productionWorkOrderService = productionWorkOrderService;
        this.productionDispatchService = productionDispatchService;
        this.currentProductionUserPort = currentProductionUserPort;
    }

    @GetMapping("/order-items/candidates")
    public ApiResponse<PageResponse<ProductionWorkOrderCandidateResponse>> candidates(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "productType", required = false) String productType,
            @RequestParam(name = "productionStatus", required = false) String productionStatus,
            @RequestParam(name = "orderNo", required = false) String orderNo,
            @RequestParam(name = "orderType", required = false) String orderType,
            @RequestParam(name = "customerType", required = false) String customerType,
            @RequestParam(name = "hasActiveWorkOrder", required = false) Boolean hasActiveWorkOrder,
            @RequestParam(name = "page", required = false) Long page,
            @RequestParam(name = "pageSize", required = false) Long pageSize) {
        return ApiResponse.success(productionWorkOrderService.listOrderItemCandidates(
                new ProductionWorkOrderCandidateQuery(
                        keyword,
                        productType,
                        productionStatus,
                        orderNo,
                        orderType,
                        customerType,
                        hasActiveWorkOrder,
                        page,
                        pageSize)));
    }

    @PostMapping("/from-order-item")
    public ApiResponse<ProductionWorkOrderResponse> createFromOrderItem(
            @RequestBody ProductionWorkOrderCreateRequest request) {
        return ApiResponse.success(productionWorkOrderService.detail(
                productionWorkOrderService.createFromOrderItem(request, currentUserId()).getId()));
    }

    @PostMapping("/material-readiness/preview-create")
    public ApiResponse<WorkOrderMaterialReadinessResponse> previewCreateMaterialReadiness(
            @RequestBody WorkOrderMaterialReadinessPreviewRequest request) {
        return ApiResponse.success(productionWorkOrderService.previewCreateMaterialReadiness(request));
    }

    @PostMapping("/create-with-material-readiness")
    public ApiResponse<ProductionWorkOrderResponse> createWithMaterialReadiness(
            @RequestBody WorkOrderMaterialReadinessCreateRequest request) {
        return ApiResponse.success(productionWorkOrderService.detail(
                productionWorkOrderService.createWithMaterialReadiness(request, currentUserId()).getId()));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductionWorkOrderResponse>> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "workOrderNo", required = false) String workOrderNo,
            @RequestParam(name = "orderItemId", required = false) Long orderItemId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "plannedStartFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedStartFrom,
            @RequestParam(name = "plannedStartTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedStartTo,
            @RequestParam(name = "requiredDeliveryFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requiredDeliveryFrom,
            @RequestParam(name = "requiredDeliveryTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requiredDeliveryTo,
            @RequestParam(name = "routeLinked", required = false) Boolean routeLinked,
            @RequestParam(name = "page", required = false) Long page,
            @RequestParam(name = "pageSize", required = false) Long pageSize) {
        return ApiResponse.success(productionWorkOrderService.listWorkOrders(new ProductionWorkOrderQuery(
                status,
                workOrderNo,
                orderItemId,
                keyword,
                plannedStartFrom,
                plannedStartTo,
                requiredDeliveryFrom,
                requiredDeliveryTo,
                routeLinked,
                page,
                pageSize)));
    }

    @GetMapping("/{workOrderId}")
    public ApiResponse<ProductionWorkOrderResponse> detail(@PathVariable("workOrderId") Long workOrderId) {
        return ApiResponse.success(productionWorkOrderService.detail(workOrderId));
    }

    @PutMapping("/{workOrderId}")
    public ApiResponse<ProductionWorkOrderResponse> update(
            @PathVariable("workOrderId") Long workOrderId,
            @RequestBody ProductionWorkOrderUpdateRequest request) {
        return ApiResponse.success(productionWorkOrderService.detail(
                productionWorkOrderService.updateDraft(workOrderId, request, currentUserId()).getId()));
    }

    @PutMapping("/{workOrderId}/materials")
    public ApiResponse<ProductionWorkOrderResponse> updateMaterials(
            @PathVariable("workOrderId") Long workOrderId,
            @RequestBody ProductionWorkOrderMaterialsUpdateRequest request) {
        return ApiResponse.success(productionWorkOrderService.detail(
                productionWorkOrderService.replaceDraftMaterials(workOrderId, request, currentUserId()).getId()));
    }

    @GetMapping("/{workOrderId}/material-generation/preview")
    public ApiResponse<WorkOrderMaterialGenerationResponse> previewMaterialGeneration(
            @PathVariable("workOrderId") Long workOrderId,
            @RequestParam("routeTemplateId") Long routeTemplateId) {
        return ApiResponse.success(productionWorkOrderService.previewMaterialGeneration(workOrderId, routeTemplateId));
    }

    @PostMapping("/{workOrderId}/materials/generate-from-template")
    public ApiResponse<WorkOrderMaterialGenerationResponse> generateMaterialsFromTemplate(
            @PathVariable("workOrderId") Long workOrderId,
            @RequestBody WorkOrderMaterialGenerationRequest request) {
        return ApiResponse.success(productionWorkOrderService.generateMaterialsFromTemplate(
                workOrderId,
                request,
                currentUserId()));
    }

    @PostMapping("/{workOrderId}/release")
    public ApiResponse<ProductionWorkOrderResponse> release(@PathVariable("workOrderId") Long workOrderId) {
        return ApiResponse.success(productionWorkOrderService.detail(
                productionWorkOrderService.release(workOrderId, currentUserId()).getId()));
    }

    @PostMapping("/{workOrderId}/cancel")
    public ApiResponse<ProductionWorkOrderResponse> cancel(@PathVariable("workOrderId") Long workOrderId) {
        return ApiResponse.success(productionWorkOrderService.detail(
                productionWorkOrderService.cancel(workOrderId, currentUserId()).getId()));
    }

    @PostMapping("/{workOrderId}/link-route-instance")
    public ApiResponse<ProductionWorkOrderResponse> linkRouteInstance(
            @PathVariable("workOrderId") Long workOrderId,
            @RequestBody ProductionWorkOrderRouteLinkRequest request) {
        if (request == null || request.productionRouteInstanceId() == null) {
            throw new ProductionWorkOrderException("WORK_ORDER_ROUTE_LINK_CONFLICT");
        }
        return ApiResponse.success(productionWorkOrderService.detail(
                productionWorkOrderService.linkRouteInstance(
                        workOrderId,
                        request.productionRouteInstanceId(),
                        currentUserId()).getId()));
    }

    @GetMapping("/{workOrderId}/dispatch-context")
    public ApiResponse<WorkOrderDispatchContextResponse> dispatchContext(
            @PathVariable("workOrderId") Long workOrderId) {
        return ApiResponse.success(productionDispatchService.workOrderDispatchContext(workOrderId));
    }

    @PostMapping("/{workOrderId}/dispatch-config/from-template")
    public ApiResponse<DispatchConfigResponse> dispatchConfigFromTemplate(
            @PathVariable("workOrderId") Long workOrderId,
            @RequestBody DispatchConfigFromTemplateRequest request) {
        return ApiResponse.success(productionDispatchService.createWorkOrderConfigFromTemplate(workOrderId, request));
    }

    @PostMapping("/{workOrderId}/dispatch")
    public ApiResponse<ProductionDispatchResponse> dispatch(
            @PathVariable("workOrderId") Long workOrderId,
            @RequestBody DispatchProductionRequest request) {
        return ApiResponse.success(productionDispatchService.dispatchWorkOrder(workOrderId, request, currentUserId()));
    }

    private Long currentUserId() {
        return currentProductionUserPort.currentUser().currentUserId();
    }
}
