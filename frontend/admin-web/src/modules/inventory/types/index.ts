export interface InventoryPageResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface MaterialItem {
  id: number;
  materialCode: string;
  materialName: string;
  spec?: string | null;
  unit: string;
  category?: string | null;
  enabled: boolean;
  remark?: string | null;
  updatedAt?: string | null;
}

export interface MaterialItemPayload {
  materialCode: string;
  materialName: string;
  spec?: string | null;
  unit: string;
  category?: string | null;
  enabled?: boolean;
  remark?: string | null;
}

export interface InventoryStock {
  id?: number | null;
  materialId: number;
  materialCode: string;
  materialName: string;
  spec?: string | null;
  unit: string;
  onHandQty: number;
  reservedQty: number;
  availableQty: number;
  updatedAt?: string | null;
}

export type InventoryTransactionType =
  | "MANUAL_IN"
  | "MANUAL_OUT"
  | "ADJUST_IN"
  | "ADJUST_OUT";

export interface InventoryTransaction {
  id: number;
  materialId: number;
  transactionType: InventoryTransactionType;
  qty: number;
  beforeOnHandQty: number;
  afterOnHandQty: number;
  beforeReservedQty: number;
  afterReservedQty: number;
  referenceType?: string | null;
  referenceId?: number | null;
  reason?: string | null;
  remark?: string | null;
  operatorId?: number | null;
  occurredAt?: string | null;
  idempotencyKey?: string | null;
}

export interface MaterialQuery {
  keyword?: string;
  enabled?: boolean;
  page?: number;
  pageSize?: number;
}

export interface StockQuery {
  keyword?: string;
  materialId?: number;
  page?: number;
  pageSize?: number;
}

export interface TransactionQuery {
  materialId?: number;
  transactionType?: InventoryTransactionType;
  page?: number;
  pageSize?: number;
}

export interface StockOperationPayload {
  materialId: number;
  qty: number;
  reason?: string | null;
  remark?: string | null;
  idempotencyKey?: string | null;
}

export type InventoryAdjustmentDirection = "IN" | "OUT";

export interface InventoryAdjustmentPayload {
  materialId: number;
  adjustmentQty: number;
  direction: InventoryAdjustmentDirection;
  reason?: string | null;
  remark?: string | null;
  idempotencyKey?: string | null;
}
