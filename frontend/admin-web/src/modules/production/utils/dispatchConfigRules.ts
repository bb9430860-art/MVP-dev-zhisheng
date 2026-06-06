import type { DispatchStepConfig } from "../types";

export function normalizeDispatchStepOrders(
  steps: DispatchStepConfig[],
): DispatchStepConfig[] {
  return steps
    .slice()
    .sort((left, right) => left.stepOrder - right.stepOrder)
    .map((step, index) => ({
      ...step,
      stepOrder: index + 1,
    }));
}

export function canEditDispatchStructure(frozen: boolean): boolean {
  return !frozen;
}
