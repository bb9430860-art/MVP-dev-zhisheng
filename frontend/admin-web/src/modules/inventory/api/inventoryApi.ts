import { httpClient, requestData } from "@/api/httpClient";
import type {
  InventoryAdjustmentPayload,
  InventoryPageResponse,
  InventoryStock,
  InventoryTransaction,
  MaterialItem,
  MaterialItemPayload,
  MaterialQuery,
  StockOperationPayload,
  StockQuery,
  TransactionQuery,
} from "../types";

export function listMaterials(
  params: MaterialQuery,
): Promise<InventoryPageResponse<MaterialItem>> {
  return requestData(httpClient.get("/api/inventory/materials", { params }));
}

export function createMaterial(
  payload: MaterialItemPayload,
): Promise<MaterialItem> {
  return requestData(httpClient.post("/api/inventory/materials", payload));
}

export function updateMaterial(
  materialId: number,
  payload: MaterialItemPayload,
): Promise<MaterialItem> {
  return requestData(
    httpClient.put(`/api/inventory/materials/${materialId}`, payload),
  );
}

export function enableMaterial(materialId: number): Promise<MaterialItem> {
  return requestData(
    httpClient.post(`/api/inventory/materials/${materialId}/enable`),
  );
}

export function disableMaterial(materialId: number): Promise<MaterialItem> {
  return requestData(
    httpClient.post(`/api/inventory/materials/${materialId}/disable`),
  );
}

export function listStocks(
  params: StockQuery,
): Promise<InventoryPageResponse<InventoryStock>> {
  return requestData(httpClient.get("/api/inventory/stocks", { params }));
}

export function getStock(materialId: number): Promise<InventoryStock> {
  return requestData(httpClient.get(`/api/inventory/stocks/${materialId}`));
}

export function listTransactions(
  params: TransactionQuery,
): Promise<InventoryPageResponse<InventoryTransaction>> {
  return requestData(httpClient.get("/api/inventory/transactions", { params }));
}

export function manualIn(
  payload: StockOperationPayload,
): Promise<InventoryTransaction> {
  return requestData(
    httpClient.post("/api/inventory/transactions/manual-in", payload),
  );
}

export function manualOut(
  payload: StockOperationPayload,
): Promise<InventoryTransaction> {
  return requestData(
    httpClient.post("/api/inventory/transactions/manual-out", payload),
  );
}

export function adjustStock(
  payload: InventoryAdjustmentPayload,
): Promise<InventoryTransaction> {
  return requestData(
    httpClient.post("/api/inventory/transactions/adjust", payload),
  );
}
