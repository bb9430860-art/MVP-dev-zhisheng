import { describe, expect, it } from "vitest";

import {
  hasInvalidNonNegativeNumber,
  hasValidQuantityRule,
} from "../utils/stepMaterialRequirementRules";

describe("stepMaterialRequirementRules", () => {
  it("accepts base quantity, fixed quantity, or future expression as demand rule", () => {
    expect(hasValidQuantityRule({ baseQtyPerUnit: 1 })).toBe(true);
    expect(hasValidQuantityRule({ fixedQty: 2 })).toBe(true);
    expect(hasValidQuantityRule({ requiredQtyExpression: "qty * 2" })).toBe(
      true,
    );
  });

  it("rejects empty or zero-only demand rules", () => {
    expect(hasValidQuantityRule({})).toBe(false);
    expect(hasValidQuantityRule({ baseQtyPerUnit: 0, fixedQty: 0 })).toBe(
      false,
    );
  });

  it("flags negative or non-numeric number inputs", () => {
    expect(hasInvalidNonNegativeNumber(-1)).toBe(true);
    expect(hasInvalidNonNegativeNumber("bad")).toBe(true);
    expect(hasInvalidNonNegativeNumber(0)).toBe(false);
    expect(hasInvalidNonNegativeNumber("1.5")).toBe(false);
  });
});
