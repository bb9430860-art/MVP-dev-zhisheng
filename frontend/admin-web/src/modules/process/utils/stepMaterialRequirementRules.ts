import type { StepMaterialRequirementPayload } from "../types";

function toNumber(value: number | string | null | undefined): number | null {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : Number.NaN;
}

export function hasValidQuantityRule(
  material: Pick<
    StepMaterialRequirementPayload,
    "baseQtyPerUnit" | "fixedQty" | "requiredQtyExpression"
  >,
): boolean {
  const baseQty = toNumber(material.baseQtyPerUnit);
  const fixedQty = toNumber(material.fixedQty);
  return (
    (baseQty !== null && baseQty > 0) ||
    (fixedQty !== null && fixedQty > 0) ||
    Boolean(material.requiredQtyExpression?.trim())
  );
}

export function hasInvalidNonNegativeNumber(
  value: number | string | null | undefined,
): boolean {
  const parsed = toNumber(value);
  return parsed !== null && (!Number.isFinite(parsed) || parsed < 0);
}
