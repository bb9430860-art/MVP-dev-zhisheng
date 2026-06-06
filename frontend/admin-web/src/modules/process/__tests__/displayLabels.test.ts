import { describe, expect, it } from "vitest";

import { formatAssignedRole, formatProductType } from "../utils/displayLabels";

describe("process display labels", () => {
  it("formats known product type values without changing stored values", () => {
    expect(formatProductType("SPIRIT_FORTRESS")).toBe("精神堡垒");
    expect(formatProductType("ILLUMINATED_LETTER")).toBe("发光字");
    expect(formatProductType("UNKNOWN_TYPE")).toBe("UNKNOWN_TYPE");
  });

  it("formats assigned role values without binding concrete employees", () => {
    expect(formatAssignedRole("PRODUCTION_MANAGER")).toBe("生产主管");
    expect(formatAssignedRole("DESIGNER")).toBe("设计");
    expect(formatAssignedRole("QC")).toBe("质检");
    expect(formatAssignedRole("INSTALLER")).toBe("安装");
    expect(formatAssignedRole("TEMP_ROLE")).toBe("TEMP_ROLE");
  });
});
