import { describe, expect, it } from "vitest";

import {
  canOperateStock,
  hasForbiddenInventoryCopy,
  isPositiveQuantity,
  isValidAdjustmentDirection,
} from "../utils/inventoryRules";

describe("inventoryRules", () => {
  it("accepts only positive stock operation quantities", () => {
    expect(isPositiveQuantity(1)).toBe(true);
    expect(isPositiveQuantity("2.5")).toBe(true);
    expect(isPositiveQuantity(0)).toBe(false);
    expect(isPositiveQuantity("-1")).toBe(false);
    expect(isPositiveQuantity("bad")).toBe(false);
  });

  it("allows stock operations only for enabled materials", () => {
    expect(canOperateStock(true)).toBe(true);
    expect(canOperateStock(false)).toBe(false);
  });

  it("accepts only MVP adjustment directions", () => {
    expect(isValidAdjustmentDirection("IN")).toBe(true);
    expect(isValidAdjustmentDirection("OUT")).toBe(true);
    expect(isValidAdjustmentDirection("RESERVE")).toBe(false);
  });

  it("detects forbidden inventory readiness copy", () => {
    expect(hasForbiddenInventoryCopy("库存核心只记录物料余额和流水")).toBe(false);
    expect(hasForbiddenInventoryCopy("显示节点缺料并阻止开工")).toBe(true);
    expect(hasForbiddenInventoryCopy("查看采购状态和供应商状态")).toBe(true);
  });
});
