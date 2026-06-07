import {
  assignedRoleLabels,
  productionStepStatusLabels,
  productTypeLabels,
} from "../constants";

export function formatProductionProductType(value?: string | null) {
  if (!value) {
    return "通用";
  }
  return productTypeLabels[value] ?? value;
}

export function formatProductionRole(value?: string | null) {
  if (!value) {
    return "-";
  }
  return assignedRoleLabels[value] ?? value;
}

export function formatProductionStepStatus(value?: string | null) {
  if (!value) {
    return "-";
  }
  return productionStepStatusLabels[value] ?? value;
}
