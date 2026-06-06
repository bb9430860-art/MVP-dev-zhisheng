import { describe, expect, it } from "vitest";

import { unwrapApiEnvelope } from "../apiEnvelope";

describe("unwrapApiEnvelope", () => {
  it("returns data when backend code is success", () => {
    const data = unwrapApiEnvelope({
      code: 0,
      message: "success",
      data: { id: 12, routeName: "精神堡垒工艺路线" },
    });

    expect(data).toEqual({ id: 12, routeName: "精神堡垒工艺路线" });
  });

  it("throws backend message when backend code is not success", () => {
    expect(() =>
      unwrapApiEnvelope({
        code: 400,
        message: "Route template requires at least one active enabled step",
        data: null,
      }),
    ).toThrow("Route template requires at least one active enabled step");
  });
});
