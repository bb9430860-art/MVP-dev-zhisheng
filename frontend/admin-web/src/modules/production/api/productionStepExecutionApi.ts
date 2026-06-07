import { httpClient, requestData } from "@/api/httpClient";
import type {
  ProductionProgress,
  ProductionStepDetail,
  ProductionStepExecutionResult,
  ProductionTask,
} from "../types";

const productionBase = "/api/production";

export function listMyProductionTasks(): Promise<ProductionTask[]> {
  return requestData(httpClient.get(`${productionBase}/tasks/my`));
}

export function getProductionStepDetail(
  stepInstanceId: number,
): Promise<ProductionStepDetail> {
  return requestData(
    httpClient.get(`${productionBase}/step-instances/${stepInstanceId}`),
  );
}

export function startProductionStep(
  stepInstanceId: number,
): Promise<ProductionStepExecutionResult> {
  return requestData(
    httpClient.post(`${productionBase}/step-instances/${stepInstanceId}/start`),
  );
}

export function completeProductionStep(
  stepInstanceId: number,
): Promise<ProductionStepExecutionResult> {
  return requestData(
    httpClient.post(`${productionBase}/step-instances/${stepInstanceId}/complete`),
  );
}

export function getProductionRouteProgress(
  routeInstanceId: number,
): Promise<ProductionProgress> {
  return requestData(
    httpClient.get(`${productionBase}/route-instances/${routeInstanceId}/progress`),
  );
}
