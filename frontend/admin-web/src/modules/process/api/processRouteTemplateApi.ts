import { httpClient, requestData } from '@/api/httpClient'
import type {
  RouteTemplate,
  RouteTemplateOption,
  RouteTemplatePayload,
  StepTemplate,
  StepTemplatePayload,
} from '../types'

const routeTemplateBase = '/api/process/route-templates'

export function listRouteTemplates(): Promise<RouteTemplate[]> {
  return requestData(httpClient.get(routeTemplateBase))
}

export function createRouteTemplate(payload: RouteTemplatePayload): Promise<RouteTemplate> {
  return requestData(httpClient.post(routeTemplateBase, payload))
}

export function getRouteTemplate(id: number): Promise<RouteTemplate> {
  return requestData(httpClient.get(`${routeTemplateBase}/${id}`))
}

export function updateRouteTemplate(
  id: number,
  payload: RouteTemplatePayload,
): Promise<RouteTemplate> {
  return requestData(httpClient.put(`${routeTemplateBase}/${id}`, payload))
}

export function setRouteTemplateEnabled(id: number, enabled: boolean): Promise<RouteTemplate> {
  return requestData(httpClient.patch(`${routeTemplateBase}/${id}/enabled`, { enabled }))
}

export function deleteRouteTemplate(id: number): Promise<void> {
  return requestData(httpClient.delete(`${routeTemplateBase}/${id}`))
}

export function getRouteTemplateOptions(productType?: string): Promise<RouteTemplateOption[]> {
  return requestData(
    httpClient.get(`${routeTemplateBase}/options`, {
      params: productType ? { productType } : undefined,
    }),
  )
}

export function listStepTemplates(routeTemplateId: number): Promise<StepTemplate[]> {
  return requestData(httpClient.get(`${routeTemplateBase}/${routeTemplateId}/steps`))
}

export function createStepTemplate(
  routeTemplateId: number,
  payload: StepTemplatePayload,
): Promise<StepTemplate> {
  return requestData(httpClient.post(`${routeTemplateBase}/${routeTemplateId}/steps`, payload))
}

export function updateStepTemplate(
  routeTemplateId: number,
  stepId: number,
  payload: StepTemplatePayload,
): Promise<StepTemplate> {
  return requestData(httpClient.put(`${routeTemplateBase}/${routeTemplateId}/steps/${stepId}`, payload))
}

export function setStepTemplateEnabled(
  routeTemplateId: number,
  stepId: number,
  enabled: boolean,
): Promise<StepTemplate> {
  return requestData(
    httpClient.patch(`${routeTemplateBase}/${routeTemplateId}/steps/${stepId}/enabled`, { enabled }),
  )
}

export function deleteStepTemplate(routeTemplateId: number, stepId: number): Promise<void> {
  return requestData(httpClient.delete(`${routeTemplateBase}/${routeTemplateId}/steps/${stepId}`))
}

export function moveStepUp(routeTemplateId: number, stepId: number): Promise<StepTemplate[]> {
  return requestData(httpClient.put(`${routeTemplateBase}/${routeTemplateId}/steps/${stepId}/move-up`))
}

export function moveStepDown(routeTemplateId: number, stepId: number): Promise<StepTemplate[]> {
  return requestData(httpClient.put(`${routeTemplateBase}/${routeTemplateId}/steps/${stepId}/move-down`))
}

export function reorderSteps(routeTemplateId: number, stepIds: number[]): Promise<StepTemplate[]> {
  return requestData(httpClient.put(`${routeTemplateBase}/${routeTemplateId}/steps/reorder`, { stepIds }))
}
