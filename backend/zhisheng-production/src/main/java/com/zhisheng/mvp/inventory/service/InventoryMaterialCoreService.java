package com.zhisheng.mvp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhisheng.mvp.inventory.dto.InventoryAdjustmentRequest;
import com.zhisheng.mvp.inventory.dto.InventoryPageResponse;
import com.zhisheng.mvp.inventory.dto.InventoryStockResponse;
import com.zhisheng.mvp.inventory.dto.InventoryTransactionResponse;
import com.zhisheng.mvp.inventory.dto.MaterialItemRequest;
import com.zhisheng.mvp.inventory.dto.MaterialItemResponse;
import com.zhisheng.mvp.inventory.dto.StockOperationRequest;
import com.zhisheng.mvp.inventory.entity.InventoryStock;
import com.zhisheng.mvp.inventory.entity.InventoryTransaction;
import com.zhisheng.mvp.inventory.entity.MaterialItem;
import com.zhisheng.mvp.inventory.mapper.InventoryStockMapper;
import com.zhisheng.mvp.inventory.mapper.InventoryTransactionMapper;
import com.zhisheng.mvp.inventory.mapper.MaterialItemMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InventoryMaterialCoreService {

    private static final long DEFAULT_TENANT_ID = 1L;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final MaterialItemMapper materialMapper;
    private final InventoryStockMapper stockMapper;
    private final InventoryTransactionMapper transactionMapper;

    public InventoryMaterialCoreService(
            MaterialItemMapper materialMapper,
            InventoryStockMapper stockMapper,
            InventoryTransactionMapper transactionMapper) {
        this.materialMapper = materialMapper;
        this.stockMapper = stockMapper;
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse<MaterialItemResponse> listMaterials(
            String keyword,
            Boolean enabled,
            Integer page,
            Integer pageSize) {
        LambdaQueryWrapper<MaterialItem> query = new LambdaQueryWrapper<MaterialItem>()
                .eq(MaterialItem::getTenantId, DEFAULT_TENANT_ID)
                .eq(MaterialItem::getDeleted, false)
                .orderByDesc(MaterialItem::getId);
        if (StringUtils.hasText(keyword)) {
            String pattern = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(MaterialItem::getMaterialCode, pattern)
                    .or()
                    .like(MaterialItem::getMaterialName, pattern)
                    .or()
                    .like(MaterialItem::getSpec, pattern));
        }
        if (enabled != null) {
            query.eq(MaterialItem::getEnabled, enabled);
        }
        List<MaterialItemResponse> all = materialMapper.selectList(query).stream()
                .map(MaterialItemResponse::from)
                .toList();
        return page(all, page, pageSize);
    }

    @Transactional
    public MaterialItemResponse createMaterial(MaterialItemRequest request) {
        validateMaterialRequest(request);
        String code = request.materialCode().trim();
        ensureMaterialCodeUnique(code, null);

        LocalDateTime now = LocalDateTime.now();
        MaterialItem material = new MaterialItem();
        material.setTenantId(DEFAULT_TENANT_ID);
        applyMaterial(material, request);
        material.setEnabled(request.enabled() == null || request.enabled());
        material.setCreatedAt(now);
        material.setUpdatedAt(now);
        material.setDeleted(false);
        material.setDeleteMarker("");
        materialMapper.insert(material);
        return MaterialItemResponse.from(material);
    }

    @Transactional
    public MaterialItemResponse updateMaterial(Long materialId, MaterialItemRequest request) {
        validateMaterialRequest(request);
        MaterialItem material = requiredMaterial(materialId);
        String code = request.materialCode().trim();
        ensureMaterialCodeUnique(code, materialId);
        applyMaterial(material, request);
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);
        return MaterialItemResponse.from(requiredMaterial(materialId));
    }

    @Transactional
    public MaterialItemResponse setMaterialEnabled(Long materialId, boolean enabled) {
        MaterialItem material = requiredMaterial(materialId);
        material.setEnabled(enabled);
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);
        return MaterialItemResponse.from(requiredMaterial(materialId));
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse<InventoryStockResponse> listStocks(
            String keyword,
            Long materialId,
            Integer page,
            Integer pageSize) {
        LambdaQueryWrapper<InventoryStock> query = new LambdaQueryWrapper<InventoryStock>()
                .eq(InventoryStock::getTenantId, DEFAULT_TENANT_ID)
                .orderByDesc(InventoryStock::getUpdatedAt)
                .orderByDesc(InventoryStock::getId);
        if (materialId != null) {
            query.eq(InventoryStock::getMaterialId, materialId);
        }
        if (StringUtils.hasText(keyword)) {
            String pattern = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(InventoryStock::getMaterialCodeSnapshot, pattern)
                    .or()
                    .like(InventoryStock::getMaterialNameSnapshot, pattern)
                    .or()
                    .like(InventoryStock::getSpecSnapshot, pattern));
        }
        List<InventoryStockResponse> all = stockMapper.selectList(query).stream()
                .map(InventoryStockResponse::from)
                .toList();
        return page(all, page, pageSize);
    }

    @Transactional(readOnly = true)
    public InventoryStockResponse getStock(Long materialId) {
        InventoryStock stock = stockByMaterialId(materialId);
        if (stock == null) {
            MaterialItem material = requiredMaterial(materialId);
            return new InventoryStockResponse(
                    null,
                    material.getId(),
                    material.getMaterialCode(),
                    material.getMaterialName(),
                    material.getSpec(),
                    material.getUnit(),
                    ZERO,
                    ZERO,
                    ZERO,
                    null);
        }
        return InventoryStockResponse.from(stock);
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse<InventoryTransactionResponse> listTransactions(
            Long materialId,
            String transactionType,
            Integer page,
            Integer pageSize) {
        LambdaQueryWrapper<InventoryTransaction> query = new LambdaQueryWrapper<InventoryTransaction>()
                .eq(InventoryTransaction::getTenantId, DEFAULT_TENANT_ID)
                .orderByDesc(InventoryTransaction::getOccurredAt)
                .orderByDesc(InventoryTransaction::getId);
        if (materialId != null) {
            query.eq(InventoryTransaction::getMaterialId, materialId);
        }
        if (StringUtils.hasText(transactionType)) {
            query.eq(InventoryTransaction::getTransactionType, transactionType.trim());
        }
        List<InventoryTransactionResponse> all = transactionMapper.selectList(query).stream()
                .map(InventoryTransactionResponse::from)
                .toList();
        return page(all, page, pageSize);
    }

    @Transactional
    public InventoryTransactionResponse manualIn(StockOperationRequest request) {
        validateOperationRequest(request);
        MaterialItem material = requiredEnabledMaterial(request.materialId());
        ensureIdempotencyKeyAvailable(request.idempotencyKey());
        InventoryStock stock = stockByMaterialId(material.getId());
        if (stock == null) {
            stock = newStock(material);
            stockMapper.insert(stock);
        }
        return applyStockChange(material, stock, request.qty(), "MANUAL_IN", request);
    }

    @Transactional
    public InventoryTransactionResponse manualOut(StockOperationRequest request) {
        validateOperationRequest(request);
        MaterialItem material = requiredEnabledMaterial(request.materialId());
        ensureIdempotencyKeyAvailable(request.idempotencyKey());
        InventoryStock stock = requiredStock(material.getId());
        return applyStockChange(material, stock, request.qty().negate(), "MANUAL_OUT", request);
    }

    @Transactional
    public InventoryTransactionResponse adjust(InventoryAdjustmentRequest request) {
        if (request == null || request.materialId() == null) {
            throw new IllegalArgumentException("MATERIAL_NOT_FOUND");
        }
        validatePositiveQty(request.adjustmentQty());
        MaterialItem material = requiredEnabledMaterial(request.materialId());
        ensureIdempotencyKeyAvailable(request.idempotencyKey());
        InventoryStock stock = "IN".equalsIgnoreCase(request.direction())
                ? stockByMaterialId(material.getId())
                : requiredStock(material.getId());
        if (stock == null) {
            stock = newStock(material);
            stockMapper.insert(stock);
        }
        String transactionType;
        BigDecimal delta;
        if ("IN".equalsIgnoreCase(request.direction())) {
            transactionType = "ADJUST_IN";
            delta = request.adjustmentQty();
        } else if ("OUT".equalsIgnoreCase(request.direction())) {
            transactionType = "ADJUST_OUT";
            delta = request.adjustmentQty().negate();
        } else {
            throw new IllegalArgumentException("INVENTORY_QTY_INVALID");
        }
        StockOperationRequest operation = new StockOperationRequest(
                request.materialId(),
                request.adjustmentQty(),
                request.referenceType(),
                request.referenceId(),
                request.reason(),
                request.remark(),
                request.idempotencyKey());
        return applyStockChange(material, stock, delta, transactionType, operation);
    }

    private InventoryTransactionResponse applyStockChange(
            MaterialItem material,
            InventoryStock stock,
            BigDecimal delta,
            String transactionType,
            StockOperationRequest request) {
        BigDecimal beforeOnHand = safe(stock.getOnHandQty());
        BigDecimal beforeReserved = safe(stock.getReservedQty());
        BigDecimal afterOnHand = beforeOnHand.add(delta);
        if (afterOnHand.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("INVENTORY_INSUFFICIENT_STOCK");
        }
        if (afterOnHand.compareTo(beforeReserved) < 0) {
            throw new IllegalArgumentException("INVENTORY_NEGATIVE_STOCK_NOT_ALLOWED");
        }
        BigDecimal afterReserved = beforeReserved;
        BigDecimal afterAvailable = afterOnHand.subtract(afterReserved);

        LocalDateTime now = LocalDateTime.now();
        stock.setMaterialCodeSnapshot(material.getMaterialCode());
        stock.setMaterialNameSnapshot(material.getMaterialName());
        stock.setSpecSnapshot(material.getSpec());
        stock.setUnitSnapshot(material.getUnit());
        stock.setOnHandQty(afterOnHand);
        stock.setReservedQty(afterReserved);
        stock.setAvailableQty(afterAvailable);
        stock.setUpdatedAt(now);
        stockMapper.updateById(stock);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTenantId(DEFAULT_TENANT_ID);
        transaction.setMaterialId(material.getId());
        transaction.setTransactionType(transactionType);
        transaction.setQty(request.qty());
        transaction.setBeforeOnHandQty(beforeOnHand);
        transaction.setAfterOnHandQty(afterOnHand);
        transaction.setBeforeReservedQty(beforeReserved);
        transaction.setAfterReservedQty(afterReserved);
        transaction.setReferenceType(blankToNull(request.referenceType()));
        transaction.setReferenceId(request.referenceId());
        transaction.setReason(blankToNull(request.reason()));
        transaction.setRemark(blankToNull(request.remark()));
        transaction.setOperatorId(null);
        transaction.setOccurredAt(now);
        transaction.setCreatedAt(now);
        transaction.setIdempotencyKey(blankToNull(request.idempotencyKey()));
        transactionMapper.insert(transaction);
        return InventoryTransactionResponse.from(transaction);
    }

    private void validateMaterialRequest(MaterialItemRequest request) {
        if (request == null || !StringUtils.hasText(request.materialCode())) {
            throw new IllegalArgumentException("MATERIAL_CODE_REQUIRED");
        }
        if (!StringUtils.hasText(request.materialName())) {
            throw new IllegalArgumentException("MATERIAL_NAME_REQUIRED");
        }
        if (!StringUtils.hasText(request.unit())) {
            throw new IllegalArgumentException("MATERIAL_UNIT_REQUIRED");
        }
    }

    private void applyMaterial(MaterialItem material, MaterialItemRequest request) {
        material.setMaterialCode(request.materialCode().trim());
        material.setMaterialName(request.materialName().trim());
        material.setSpec(blankToNull(request.spec()));
        material.setUnit(request.unit().trim());
        material.setCategory(blankToNull(request.category()));
        material.setRemark(blankToNull(request.remark()));
        if (request.enabled() != null) {
            material.setEnabled(request.enabled());
        }
    }

    private void ensureMaterialCodeUnique(String materialCode, Long currentMaterialId) {
        Long count = materialMapper.selectCount(new LambdaQueryWrapper<MaterialItem>()
                .eq(MaterialItem::getTenantId, DEFAULT_TENANT_ID)
                .eq(MaterialItem::getDeleted, false)
                .eq(MaterialItem::getMaterialCode, materialCode)
                .ne(currentMaterialId != null, MaterialItem::getId, currentMaterialId));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("MATERIAL_CODE_DUPLICATED");
        }
    }

    private MaterialItem requiredMaterial(Long materialId) {
        MaterialItem material = materialMapper.selectById(materialId);
        if (material == null
                || Boolean.TRUE.equals(material.getDeleted())
                || !Long.valueOf(DEFAULT_TENANT_ID).equals(material.getTenantId())) {
            throw new IllegalArgumentException("MATERIAL_NOT_FOUND");
        }
        return material;
    }

    private MaterialItem requiredEnabledMaterial(Long materialId) {
        MaterialItem material = requiredMaterial(materialId);
        if (!Boolean.TRUE.equals(material.getEnabled())) {
            throw new IllegalArgumentException("MATERIAL_DISABLED");
        }
        return material;
    }

    private InventoryStock stockByMaterialId(Long materialId) {
        return stockMapper.selectOne(new LambdaQueryWrapper<InventoryStock>()
                .eq(InventoryStock::getTenantId, DEFAULT_TENANT_ID)
                .eq(InventoryStock::getMaterialId, materialId));
    }

    private InventoryStock requiredStock(Long materialId) {
        InventoryStock stock = stockByMaterialId(materialId);
        if (stock == null) {
            throw new IllegalArgumentException("INVENTORY_STOCK_NOT_FOUND");
        }
        return stock;
    }

    private InventoryStock newStock(MaterialItem material) {
        InventoryStock stock = new InventoryStock();
        stock.setTenantId(DEFAULT_TENANT_ID);
        stock.setMaterialId(material.getId());
        stock.setMaterialCodeSnapshot(material.getMaterialCode());
        stock.setMaterialNameSnapshot(material.getMaterialName());
        stock.setSpecSnapshot(material.getSpec());
        stock.setUnitSnapshot(material.getUnit());
        stock.setOnHandQty(ZERO);
        stock.setReservedQty(ZERO);
        stock.setAvailableQty(ZERO);
        stock.setUpdatedAt(LocalDateTime.now());
        return stock;
    }

    private void validateOperationRequest(StockOperationRequest request) {
        if (request == null || request.materialId() == null) {
            throw new IllegalArgumentException("MATERIAL_NOT_FOUND");
        }
        validatePositiveQty(request.qty());
    }

    private void validatePositiveQty(BigDecimal qty) {
        if (qty == null || qty.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("INVENTORY_QTY_INVALID");
        }
    }

    private void ensureIdempotencyKeyAvailable(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        Long count = transactionMapper.selectCount(new LambdaQueryWrapper<InventoryTransaction>()
                .eq(InventoryTransaction::getTenantId, DEFAULT_TENANT_ID)
                .eq(InventoryTransaction::getIdempotencyKey, idempotencyKey.trim()));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("INVENTORY_IDEMPOTENCY_KEY_DUPLICATED");
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private <T> InventoryPageResponse<T> page(List<T> all, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        int from = Math.min((safePage - 1) * safePageSize, all.size());
        int to = Math.min(from + safePageSize, all.size());
        return new InventoryPageResponse<>(all.subList(from, to), all.size(), safePage, safePageSize);
    }
}
