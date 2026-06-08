import type {
  InventoryAdjustmentDirection,
  InventoryTransactionType,
} from "../types";

const forbiddenCopyPatterns = [
  "节点缺料",
  "缺料阻止",
  "库存齐套",
  "生产自动扣料",
  "采购状态",
  "供应商状态",
  "财务",
  "成本",
];

export function isPositiveQuantity(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return false;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0;
}

export function canOperateStock(materialEnabled: boolean) {
  return materialEnabled;
}

export function isValidAdjustmentDirection(
  value: string,
): value is InventoryAdjustmentDirection {
  return value === "IN" || value === "OUT";
}

export function hasForbiddenInventoryCopy(text: string) {
  return forbiddenCopyPatterns.some((pattern) => text.includes(pattern));
}

export function inventoryTransactionTypeLabel(type: InventoryTransactionType) {
  const labels: Record<InventoryTransactionType, string> = {
    MANUAL_IN: "手工入库",
    MANUAL_OUT: "手工出库",
    ADJUST_IN: "库存调增",
    ADJUST_OUT: "库存调减",
  };
  return labels[type] ?? type;
}

export function inventoryTransactionTagType(type: InventoryTransactionType) {
  if (type === "MANUAL_IN" || type === "ADJUST_IN") {
    return "success";
  }
  return "warning";
}
