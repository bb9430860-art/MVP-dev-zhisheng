import { describe, expect, it } from "vitest";

import {
  canGenerateWorkOrderMaterials,
  materialGenerationNeedsReplacementConfirm,
} from "../utils/materialGenerationRules";

describe("materialGenerationRules", () => {
  it("allows material generation only for DRAFT work orders", () => {
    expect(canGenerateWorkOrderMaterials("DRAFT")).toBe(true);
    expect(canGenerateWorkOrderMaterials("RELEASED")).toBe(false);
    expect(canGenerateWorkOrderMaterials("IN_PROGRESS")).toBe(false);
    expect(canGenerateWorkOrderMaterials("COMPLETED")).toBe(false);
    expect(canGenerateWorkOrderMaterials("CANCELLED")).toBe(false);
  });

  it("requires replacement confirmation when existing materials are present", () => {
    expect(materialGenerationNeedsReplacementConfirm(1)).toBe(true);
    expect(materialGenerationNeedsReplacementConfirm(0)).toBe(false);
  });
});
