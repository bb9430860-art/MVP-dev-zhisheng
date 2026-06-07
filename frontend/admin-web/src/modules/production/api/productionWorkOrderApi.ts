import { httpClient, requestData } from "@/api/httpClient";
import type {
  PageResponse,
  WorkOrder,
  WorkOrderBasePayload,
  WorkOrderCandidate,
  WorkOrderCreatePayload,
  WorkOrderMaterialPayload,
} from "../types";

const workOrderBase = "/api/production/work-orders";

export interface WorkOrderCandidateQuery {
  keyword?: string;
  productType?: string;
  productionStatus?: string;
  orderNo?: string;
  orderType?: string;
  customerType?: string;
  hasActiveWorkOrder?: boolean;
  page?: number;
  pageSize?: number;
}

export interface WorkOrderQuery {
  status?: string;
  workOrderNo?: string;
  orderItemId?: number;
  keyword?: string;
  plannedStartFrom?: string;
  plannedStartTo?: string;
  requiredDeliveryFrom?: string;
  requiredDeliveryTo?: string;
  routeLinked?: boolean;
  page?: number;
  pageSize?: number;
}

export function listWorkOrderCandidates(
  params: WorkOrderCandidateQuery,
): Promise<PageResponse<WorkOrderCandidate>> {
  return requestData(
    httpClient.get(`${workOrderBase}/order-items/candidates`, { params }),
  );
}

export function createWorkOrderFromOrderItem(
  payload: WorkOrderCreatePayload,
): Promise<WorkOrder> {
  return requestData(httpClient.post(`${workOrderBase}/from-order-item`, payload));
}

export function listWorkOrders(
  params: WorkOrderQuery,
): Promise<PageResponse<WorkOrder>> {
  return requestData(httpClient.get(workOrderBase, { params }));
}

export function getWorkOrder(workOrderId: number): Promise<WorkOrder> {
  return requestData(httpClient.get(`${workOrderBase}/${workOrderId}`));
}

export function updateWorkOrder(
  workOrderId: number,
  payload: WorkOrderBasePayload,
): Promise<WorkOrder> {
  return requestData(httpClient.put(`${workOrderBase}/${workOrderId}`, payload));
}

export function updateWorkOrderMaterials(
  workOrderId: number,
  materials: WorkOrderMaterialPayload[],
): Promise<WorkOrder> {
  return requestData(
    httpClient.put(`${workOrderBase}/${workOrderId}/materials`, { materials }),
  );
}

export function releaseWorkOrder(workOrderId: number): Promise<WorkOrder> {
  return requestData(httpClient.post(`${workOrderBase}/${workOrderId}/release`));
}

export function cancelWorkOrder(workOrderId: number): Promise<WorkOrder> {
  return requestData(httpClient.post(`${workOrderBase}/${workOrderId}/cancel`));
}
