import type { ProductionWorkOrderStatus } from "../types";

export function canEditWorkOrder(status: ProductionWorkOrderStatus) {
  return status === "DRAFT";
}

export function canEditWorkOrderMaterials(status: ProductionWorkOrderStatus) {
  return status === "DRAFT";
}

export function canReleaseWorkOrder(status: ProductionWorkOrderStatus) {
  return status === "DRAFT";
}

export function canCancelWorkOrder(status: ProductionWorkOrderStatus) {
  return status === "DRAFT" || status === "RELEASED";
}

export function canDispatchWorkOrder(
  status: ProductionWorkOrderStatus,
  routeLinked: boolean,
) {
  return status === "RELEASED" && !routeLinked;
}

export function workOrderStatusTagType(status: ProductionWorkOrderStatus) {
  if (status === "COMPLETED") {
    return "success";
  }
  if (status === "RELEASED") {
    return "warning";
  }
  if (status === "IN_PROGRESS") {
    return "primary";
  }
  if (status === "CANCELLED") {
    return "danger";
  }
  return "info";
}
