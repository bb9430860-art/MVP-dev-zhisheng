import { describe, expect, it } from "vitest";

import {
  canEditDispatchStructure,
  normalizeDispatchStepOrders,
} from "../utils/dispatchConfigRules";
import type { DispatchStepConfig } from "../types";

describe("dispatchConfigRules", () => {
  it("normalizes step orders from one", () => {
    const steps = [
      step("b", 9),
      step("a", 3),
      step("c", 7),
    ];

    expect(normalizeDispatchStepOrders(steps).map((item) => item.stepOrder)).toEqual([
      1,
      2,
      3,
    ]);
  });

  it("allows structure editing only before frozen dispatch", () => {
    expect(canEditDispatchStructure(false)).toBe(true);
    expect(canEditDispatchStructure(true)).toBe(false);
  });
});

function step(clientStepId: string, stepOrder: number): DispatchStepConfig {
  return {
    clientStepId,
    sourceStepTemplateId: null,
    stepCode: clientStepId,
    stepName: clientStepId,
    stepOrder,
    assignedRole: "WORKER",
    assignedUserId: null,
    photoRequired: false,
    remarkRequired: false,
    mobileEnabled: true,
    estimatedHours: null,
    operationInstruction: "",
  };
}
