export interface OrderItemProductionContext {
  id: number;
  orderId: number;
  itemName: string;
  productType: string | null;
  quantity: number | string | null;
  productionStatus: string;
  productionProgress: number | string | null;
  productionRouteInstanceId: number | null;
}

export interface ProductionConfigContext {
  orderItem: OrderItemProductionContext;
  dispatched: boolean;
}

export interface DispatchStepConfig {
  clientStepId: string;
  sourceStepTemplateId: number | null;
  stepCode: string | null;
  stepName: string;
  stepOrder: number;
  assignedRole: string;
  assignedUserId: number | null;
  photoRequired: boolean;
  remarkRequired: boolean;
  mobileEnabled: boolean;
  estimatedHours: number | string | null;
  operationInstruction: string | null;
}

export interface DispatchConfig {
  routeTemplateId: number;
  routeCode: string;
  routeName: string;
  productType: string | null;
  description: string | null;
  steps: DispatchStepConfig[];
}

export interface DispatchPayload {
  routeTemplateId: number;
  idempotencyKey?: string | null;
  routeName: string;
  steps: DispatchStepConfig[];
}

export interface ProductionDispatchResult {
  routeInstanceId: number;
  orderItemId: number;
  status: string;
  frozen: boolean;
  stepCount: number;
}

export interface ProductionSummary {
  orderItemId: number;
  productionStatus: string;
  productionRouteInstanceId: number | null;
  progress: number | string | null;
  totalSteps: number;
  completedSteps: number;
  currentStepName: string | null;
  dispatched: boolean;
  frozen: boolean;
}

export interface RouteTemplateOption {
  id: number;
  routeName: string;
  productType: string | null;
  stepCount: number;
}
