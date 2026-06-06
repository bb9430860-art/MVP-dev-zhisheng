import { describe, expect, it } from "vitest";

import { filterRouteTemplates } from "../utils/routeTemplateFilters";
import type { RouteTemplate } from "../types";

const templates: RouteTemplate[] = [
  {
    id: 1,
    tenantId: 1,
    routeCode: "RT-SPIRIT",
    routeName: "精神堡垒路线",
    productType: "SPIRIT_FORTRESS",
    description: "大型标识",
    enabled: true,
    deleted: false,
  },
  {
    id: 2,
    tenantId: 1,
    routeCode: "RT-GENERAL",
    routeName: "通用路线",
    productType: "GENERAL",
    description: "",
    enabled: false,
    deleted: false,
  },
];

describe("filterRouteTemplates", () => {
  it("filters by keyword across code and name", () => {
    const result = filterRouteTemplates(templates, {
      keyword: "SPIRIT",
      productType: "",
      enabled: null,
    });

    expect(result.map((item) => item.id)).toEqual([1]);
  });

  it("filters by product type and enabled status", () => {
    const result = filterRouteTemplates(templates, {
      keyword: "",
      productType: "GENERAL",
      enabled: false,
    });

    expect(result.map((item) => item.id)).toEqual([2]);
  });
});
