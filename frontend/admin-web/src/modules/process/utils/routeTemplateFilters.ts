import type { RouteTemplate, RouteTemplateFilters } from "../types";

export function filterRouteTemplates(
  templates: RouteTemplate[],
  filters: RouteTemplateFilters,
): RouteTemplate[] {
  const keyword = filters.keyword.trim().toLowerCase();
  const productType = filters.productType.trim();

  return templates.filter((template) => {
    const matchesKeyword =
      !keyword ||
      template.routeCode.toLowerCase().includes(keyword) ||
      template.routeName.toLowerCase().includes(keyword);
    const matchesProductType =
      !productType || template.productType === productType;
    const matchesEnabled =
      filters.enabled === null || template.enabled === filters.enabled;

    return matchesKeyword && matchesProductType && matchesEnabled;
  });
}
