export interface RouteTemplate {
  id: number;
  tenantId: number;
  routeCode: string;
  routeName: string;
  productType: string | null;
  description: string | null;
  enabled: boolean;
  deleted: boolean;
  updatedAt?: string;
}

export interface RouteTemplatePayload {
  routeCode: string;
  routeName: string;
  productType?: string;
  description?: string;
  enabled?: boolean;
}

export interface RouteTemplateFilters {
  keyword: string;
  productType: string;
  enabled: boolean | null;
}

export interface RouteTemplateOption {
  id: number;
  routeName: string;
  productType: string | null;
  stepCount: number;
}

export interface StepTemplate {
  id: number;
  routeTemplateId: number;
  stepCode: string;
  stepName: string;
  stepOrder: number;
  assignedRole: string;
  photoRequired: boolean;
  remarkRequired: boolean;
  mobileEnabled: boolean;
  estimatedHours: number | string | null;
  operationInstruction: string | null;
  enabled: boolean;
  deleted: boolean;
}

export interface StepTemplatePayload {
  stepCode: string;
  stepName: string;
  assignedRole: string;
  photoRequired: boolean;
  remarkRequired: boolean;
  mobileEnabled: boolean;
  estimatedHours?: number | string | null;
  operationInstruction?: string;
  enabled?: boolean;
}

export interface StepMaterialRequirementTemplate {
  id: number;
  routeTemplateId: number;
  stepTemplateId: number;
  materialId: number | null;
  materialCode: string | null;
  materialName: string;
  spec: string | null;
  unit: string;
  baseQtyPerUnit: number | string | null;
  fixedQty: number | string | null;
  lossRate: number | string | null;
  requiredQtyExpression: string | null;
  usageStage: string | null;
  remark: string | null;
  enabled: boolean;
}

export interface StepMaterialRequirementPayload {
  materialId?: number | null;
  materialCode?: string;
  materialName: string;
  spec?: string;
  unit: string;
  baseQtyPerUnit?: number | string | null;
  fixedQty?: number | string | null;
  lossRate?: number | string | null;
  requiredQtyExpression?: string;
  usageStage?: string;
  remark?: string;
  enabled?: boolean;
}
