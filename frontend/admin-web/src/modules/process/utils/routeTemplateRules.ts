import type { StepTemplate } from "../types";

export function hasActiveEnabledStep(steps: StepTemplate[]): boolean {
  return steps.some((step) => step.enabled && !step.deleted);
}
