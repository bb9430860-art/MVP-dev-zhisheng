import { httpClient, requestData } from "@/api/httpClient";
import type {
  DispatchConfig,
  DispatchPayload,
  ProductionConfigContext,
  ProductionDispatchResult,
  ProductionSummary,
  RouteTemplateOption,
} from "../types";

const productionOrderItemsBase = "/api/production/order-items";
const processRouteTemplateBase = "/api/process/route-templates";

export function getProductionConfigContext(
  orderItemId: number,
): Promise<ProductionConfigContext> {
  return requestData(
    httpClient.get(`${productionOrderItemsBase}/${orderItemId}/config-context`),
  );
}

export function listRouteTemplateOptions(
  productType?: string | null,
): Promise<RouteTemplateOption[]> {
  return requestData(
    httpClient.get(`${processRouteTemplateBase}/options`, {
      params: productType ? { productType } : undefined,
    }),
  );
}

export function createDispatchConfigFromTemplate(
  orderItemId: number,
  routeTemplateId: number,
): Promise<DispatchConfig> {
  return requestData(
    httpClient.post(
      `${productionOrderItemsBase}/${orderItemId}/dispatch-config/from-template`,
      { routeTemplateId },
    ),
  );
}

export function dispatchProduction(
  orderItemId: number,
  payload: DispatchPayload,
): Promise<ProductionDispatchResult> {
  return requestData(
    httpClient.post(`${productionOrderItemsBase}/${orderItemId}/dispatch`, payload),
  );
}

export function getProductionSummary(
  orderItemId: number,
): Promise<ProductionSummary> {
  return requestData(
    httpClient.get(`${productionOrderItemsBase}/${orderItemId}/summary`),
  );
}
