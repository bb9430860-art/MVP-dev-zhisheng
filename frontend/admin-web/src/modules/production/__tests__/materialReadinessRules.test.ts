import { describe, expect, it } from "vitest";

import {
  canPreviewCreateReadiness,
  isReadinessWarning,
  readinessTagType,
} from "../utils/materialReadinessRules";

describe("materialReadinessRules", () => {
  it("maps readiness statuses to tag types", () => {
    expect(readinessTagType("READY")).toBe("success");
    expect(readinessTagType("SHORTAGE")).toBe("danger");
    expect(readinessTagType("UNLINKED_MATERIAL")).toBe("warning");
    expect(readinessTagType("NO_STOCK_RECORD")).toBe("info");
  });

  it("marks shortage, unlinked materials, and missing stock as warnings", () => {
    expect(isReadinessWarning("READY")).toBe(false);
    expect(isReadinessWarning("SHORTAGE")).toBe(true);
    expect(isReadinessWarning("UNLINKED_MATERIAL")).toBe(true);
    expect(isReadinessWarning("NO_STOCK_RECORD")).toBe(true);
  });

  it("requires both order item and route template before preview", () => {
    expect(canPreviewCreateReadiness(1, 2)).toBe(true);
    expect(canPreviewCreateReadiness(1, null)).toBe(false);
    expect(canPreviewCreateReadiness(null, 2)).toBe(false);
  });
});
