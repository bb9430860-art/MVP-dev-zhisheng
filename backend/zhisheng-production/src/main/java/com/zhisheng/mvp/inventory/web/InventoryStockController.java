package com.zhisheng.mvp.inventory.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.inventory.dto.InventoryPageResponse;
import com.zhisheng.mvp.inventory.dto.InventoryStockResponse;
import com.zhisheng.mvp.inventory.service.InventoryMaterialCoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/stocks")
public class InventoryStockController {

    private final InventoryMaterialCoreService inventoryService;

    public InventoryStockController(InventoryMaterialCoreService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<InventoryPageResponse<InventoryStockResponse>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "materialId", required = false) Long materialId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success(inventoryService.listStocks(keyword, materialId, page, pageSize));
    }

    @GetMapping("/{materialId}")
    public ApiResponse<InventoryStockResponse> get(@PathVariable("materialId") Long materialId) {
        return ApiResponse.success(inventoryService.getStock(materialId));
    }
}
