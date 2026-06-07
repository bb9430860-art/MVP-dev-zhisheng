import { describe, expect, it } from "vitest";

import {
  canCompleteStep,
  canStartStep,
  statusTagType,
} from "../utils/executionRules";

describe("executionRules", () => {
  it("allows starting only pending executable steps", () => {
    expect(canStartStep("PENDING", true)).toBe(true);
    expect(canStartStep("PENDING", false)).toBe(false);
    expect(canStartStep("IN_PROGRESS", true)).toBe(false);
    expect(canStartStep("COMPLETED", true)).toBe(false);
  });

  it("allows completing only in-progress completable steps", () => {
    expect(canCompleteStep("IN_PROGRESS", true)).toBe(true);
    expect(canCompleteStep("IN_PROGRESS", false)).toBe(false);
    expect(canCompleteStep("PENDING", true)).toBe(false);
    expect(canCompleteStep("COMPLETED", true)).toBe(false);
  });

  it("maps status to tag types", () => {
    expect(statusTagType("PENDING")).toBe("info");
    expect(statusTagType("IN_PROGRESS")).toBe("warning");
    expect(statusTagType("COMPLETED")).toBe("success");
  });
});
