import type { MaterialReadinessStatus } from "../types";

export function readinessTagType(status: MaterialReadinessStatus | null | undefined) {
  if (status === "READY") {
    return "success";
  }
  if (status === "SHORTAGE") {
    return "danger";
  }
  if (status === "UNLINKED_MATERIAL") {
    return "warning";
  }
  if (status === "NO_STOCK_RECORD") {
    return "info";
  }
  return "info";
}

export function isReadinessWarning(status: MaterialReadinessStatus | null | undefined) {
  return status === "SHORTAGE" || status === "UNLINKED_MATERIAL" || status === "NO_STOCK_RECORD";
}

export function canPreviewCreateReadiness(orderItemId?: number | null, routeTemplateId?: number | null) {
  return Boolean(orderItemId && routeTemplateId);
}
