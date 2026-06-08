package com.zhisheng.mvp.inventory.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.inventory.dto.InventoryPageResponse;
import com.zhisheng.mvp.inventory.dto.MaterialItemRequest;
import com.zhisheng.mvp.inventory.dto.MaterialItemResponse;
import com.zhisheng.mvp.inventory.service.InventoryMaterialCoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/materials")
public class InventoryMaterialController {

    private final InventoryMaterialCoreService inventoryService;

    public InventoryMaterialController(InventoryMaterialCoreService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<InventoryPageResponse<MaterialItemResponse>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success(inventoryService.listMaterials(keyword, enabled, page, pageSize));
    }

    @PostMapping
    public ApiResponse<MaterialItemResponse> create(@RequestBody MaterialItemRequest request) {
        return ApiResponse.success(inventoryService.createMaterial(request));
    }

    @PutMapping("/{materialId}")
    public ApiResponse<MaterialItemResponse> update(
            @PathVariable("materialId") Long materialId,
            @RequestBody MaterialItemRequest request) {
        return ApiResponse.success(inventoryService.updateMaterial(materialId, request));
    }

    @PostMapping("/{materialId}/enable")
    public ApiResponse<MaterialItemResponse> enable(@PathVariable("materialId") Long materialId) {
        return ApiResponse.success(inventoryService.setMaterialEnabled(materialId, true));
    }

    @PostMapping("/{materialId}/disable")
    public ApiResponse<MaterialItemResponse> disable(@PathVariable("materialId") Long materialId) {
        return ApiResponse.success(inventoryService.setMaterialEnabled(materialId, false));
    }
}
