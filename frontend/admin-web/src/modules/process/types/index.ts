export interface RouteTemplate {
  id: number
  tenantId: number
  routeCode: string
  routeName: string
  productType: string | null
  description: string | null
  enabled: boolean
  deleted: boolean
  updatedAt?: string
}

export interface RouteTemplatePayload {
  routeCode: string
  routeName: string
  productType?: string
  description?: string
  enabled?: boolean
}

export interface RouteTemplateFilters {
  keyword: string
  productType: string
  enabled: boolean | null
}

export interface RouteTemplateOption {
  id: number
  routeName: string
  productType: string | null
  stepCount: number
}

export interface StepTemplate {
  id: number
  routeTemplateId: number
  stepCode: string
  stepName: string
  stepOrder: number
  assignedRole: string
  photoRequired: boolean
  remarkRequired: boolean
  mobileEnabled: boolean
  estimatedHours: number | string | null
  operationInstruction: string | null
  enabled: boolean
  deleted: boolean
}

export interface StepTemplatePayload {
  stepCode: string
  stepName: string
  assignedRole: string
  photoRequired: boolean
  remarkRequired: boolean
  mobileEnabled: boolean
  estimatedHours?: number | string | null
  operationInstruction?: string
  enabled?: boolean
}
