package com.zhisheng.mvp.inventory.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.inventory.dto.InventoryAdjustmentRequest;
import com.zhisheng.mvp.inventory.dto.InventoryPageResponse;
import com.zhisheng.mvp.inventory.dto.InventoryTransactionResponse;
import com.zhisheng.mvp.inventory.dto.StockOperationRequest;
import com.zhisheng.mvp.inventory.service.InventoryMaterialCoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/transactions")
public class InventoryTransactionController {

    private final InventoryMaterialCoreService inventoryService;

    public InventoryTransactionController(InventoryMaterialCoreService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<InventoryPageResponse<InventoryTransactionResponse>> list(
            @RequestParam(value = "materialId", required = false) Long materialId,
            @RequestParam(value = "transactionType", required = false) String transactionType,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success(inventoryService.listTransactions(materialId, transactionType, page, pageSize));
    }

    @PostMapping("/manual-in")
    public ApiResponse<InventoryTransactionResponse> manualIn(@RequestBody StockOperationRequest request) {
        return ApiResponse.success(inventoryService.manualIn(request));
    }

    @PostMapping("/manual-out")
    public ApiResponse<InventoryTransactionResponse> manualOut(@RequestBody StockOperationRequest request) {
        return ApiResponse.success(inventoryService.manualOut(request));
    }

    @PostMapping("/adjust")
    public ApiResponse<InventoryTransactionResponse> adjust(@RequestBody InventoryAdjustmentRequest request) {
        return ApiResponse.success(inventoryService.adjust(request));
    }
}
