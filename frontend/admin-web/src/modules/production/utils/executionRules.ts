import type { ProductionStepStatus } from "../types";

export function canStartStep(status: ProductionStepStatus, canStart: boolean) {
  return status === "PENDING" && canStart;
}

export function canCompleteStep(status: ProductionStepStatus, canComplete: boolean) {
  return status === "IN_PROGRESS" && canComplete;
}

export function statusTagType(status: ProductionStepStatus) {
  if (status === "COMPLETED") {
    return "success";
  }
  if (status === "IN_PROGRESS") {
    return "warning";
  }
  return "info";
}
