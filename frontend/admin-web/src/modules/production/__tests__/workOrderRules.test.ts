import { describe, expect, it } from "vitest";

import {
  canCancelWorkOrder,
  canDispatchWorkOrder,
  canEditWorkOrder,
  canEditWorkOrderMaterials,
  canReleaseWorkOrder,
  workOrderStatusTagType,
} from "../utils/workOrderRules";

describe("workOrderRules", () => {
  it("allows edit and material edit only in DRAFT", () => {
    expect(canEditWorkOrder("DRAFT")).toBe(true);
    expect(canEditWorkOrderMaterials("DRAFT")).toBe(true);
    expect(canEditWorkOrder("RELEASED")).toBe(false);
    expect(canEditWorkOrderMaterials("IN_PROGRESS")).toBe(false);
    expect(canEditWorkOrder("COMPLETED")).toBe(false);
    expect(canEditWorkOrder("CANCELLED")).toBe(false);
  });

  it("allows release only in DRAFT and cancel only in DRAFT or RELEASED", () => {
    expect(canReleaseWorkOrder("DRAFT")).toBe(true);
    expect(canReleaseWorkOrder("RELEASED")).toBe(false);
    expect(canCancelWorkOrder("DRAFT")).toBe(true);
    expect(canCancelWorkOrder("RELEASED")).toBe(true);
    expect(canCancelWorkOrder("IN_PROGRESS")).toBe(false);
    expect(canCancelWorkOrder("COMPLETED")).toBe(false);
    expect(canCancelWorkOrder("CANCELLED")).toBe(false);
  });

  it("allows dispatch only for RELEASED work orders without linked route instance", () => {
    expect(canDispatchWorkOrder("RELEASED", false)).toBe(true);
    expect(canDispatchWorkOrder("RELEASED", true)).toBe(false);
    expect(canDispatchWorkOrder("DRAFT", false)).toBe(false);
    expect(canDispatchWorkOrder("IN_PROGRESS", false)).toBe(false);
    expect(canDispatchWorkOrder("COMPLETED", false)).toBe(false);
    expect(canDispatchWorkOrder("CANCELLED", false)).toBe(false);
  });

  it("maps work order status to Element Plus tag type", () => {
    expect(workOrderStatusTagType("DRAFT")).toBe("info");
    expect(workOrderStatusTagType("RELEASED")).toBe("warning");
    expect(workOrderStatusTagType("IN_PROGRESS")).toBe("primary");
    expect(workOrderStatusTagType("COMPLETED")).toBe("success");
    expect(workOrderStatusTagType("CANCELLED")).toBe("danger");
  });
});
