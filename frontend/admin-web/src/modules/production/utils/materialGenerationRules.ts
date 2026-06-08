import type { ProductionWorkOrderStatus } from "../types";

export function canGenerateWorkOrderMaterials(status: ProductionWorkOrderStatus) {
  return status === "DRAFT";
}

export function materialGenerationNeedsReplacementConfirm(existingMaterialCount: number) {
  return existingMaterialCount > 0;
}
