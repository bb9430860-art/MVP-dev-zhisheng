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

export type ProductionStepStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED";

export interface ProductionTask {
  stepInstanceId: number;
  routeInstanceId: number;
  orderId: number;
  orderItemId: number;
  itemName: string | null;
  stepName: string;
  stepOrder: number;
  assignedRole: string;
  assignedUserId: number | null;
  status: ProductionStepStatus;
  photoRequired: boolean;
  remarkRequired: boolean;
  mobileEnabled: boolean;
  canStart: boolean;
  canComplete: boolean;
}

export interface ProductionStepDetail extends ProductionTask {
  sourceStepTemplateId: number | null;
  stepCodeSnapshot: string | null;
  estimatedHours: number | string | null;
  operationInstruction: string | null;
  frozen: boolean;
  startedAt: string | null;
  startedBy: number | null;
  completedAt: string | null;
  completedBy: number | null;
}

export interface ProductionStepExecutionResult {
  stepInstanceId: number;
  routeInstanceId: number;
  status: ProductionStepStatus;
  productionProgress: number;
}

export interface ProductionProgress {
  routeInstanceId: number;
  totalSteps: number;
  completedSteps: number;
  progress: number;
  routeStatus: string;
}

export interface RouteTemplateOption {
  id: number;
  routeName: string;
  productType: string | null;
  stepCount: number;
}

export type ProductionWorkOrderStatus =
  | "DRAFT"
  | "RELEASED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface WorkOrderCandidate {
  orderItemId: number;
  orderId: number;
  orderNo: string | null;
  orderType: string | null;
  customerType: string | null;
  dealOwnerId: number | null;
  dealOwnerName: string | null;
  itemName: string;
  spec: string | null;
  unit: string | null;
  quantity: number | string | null;
  remark: string | null;
  productType: string | null;
  productionStatus: string;
  productionProgress: number | string | null;
  productionRouteInstanceId: number | null;
  hasActiveWorkOrder: boolean;
  activeWorkOrderId: number | null;
  activeWorkOrderNo: string | null;
}

export interface WorkOrderMaterialPayload {
  materialId?: number | null;
  materialCode?: string | null;
  materialName: string;
  spec?: string | null;
  unit?: string | null;
  requiredQty: number | string;
  usageStage?: string | null;
  relatedStepTemplateId?: number | null;
  relatedStepInstanceId?: number | null;
  remark?: string | null;
}

export type MaterialReadinessStatus =
  | "READY"
  | "SHORTAGE"
  | "UNLINKED_MATERIAL"
  | "NO_STOCK_RECORD";

export interface WorkOrderMaterial extends WorkOrderMaterialPayload {
  id: number;
  availableQtySnapshot?: number | string | null;
  shortageQty?: number | string | null;
  readinessStatus?: MaterialReadinessStatus | null;
  readinessCheckedAt?: string | null;
  readinessMessage?: string | null;
  requirementStatus: string;
  updatedAt: string | null;
}

export interface WorkOrderBasePayload {
  priority?: string | null;
  instructionTitle?: string | null;
  instructionRemark?: string | null;
  productionRequirement?: string | null;
  qualityRequirement?: string | null;
  packagingRequirement?: string | null;
  shippingRequirement?: string | null;
  deliveryRequirement?: string | null;
  plannedStartDate?: string | null;
  plannedFinishDate?: string | null;
  requiredDeliveryDate?: string | null;
  deadlineRemark?: string | null;
  equipmentModel?: string | null;
  technicalConfigSummary?: string | null;
  technicalConfigRemark?: string | null;
  technicalConfigJson?: string | null;
  responsibleUserId?: number | null;
  handlerUserId?: number | null;
  productionManagerId?: number | null;
  primaryWorkerId?: number | null;
  customerAcceptanceRequired?: boolean;
  acceptanceRemark?: string | null;
}

export interface WorkOrderCreatePayload extends WorkOrderBasePayload {
  orderItemId: number;
  materials: WorkOrderMaterialPayload[];
}

export interface WorkOrder extends WorkOrderBasePayload {
  id: number;
  workOrderNo: string;
  orderId: number;
  orderNo: string | null;
  orderType: string | null;
  customerType: string | null;
  dealOwnerId: number | null;
  dealOwnerName: string | null;
  orderItemId: number;
  orderItemNameSnapshot: string;
  spec: string | null;
  unit: string | null;
  quantitySnapshot: number | string | null;
  remark: string | null;
  productTypeSnapshot: string | null;
  status: ProductionWorkOrderStatus;
  releasedBy: number | null;
  releasedAt: string | null;
  confirmedBy: number | null;
  confirmedAt: string | null;
  productionSignedBy: number | null;
  productionSignedAt: string | null;
  warehouseConfirmedBy: number | null;
  warehouseConfirmedAt: string | null;
  qualityConfirmedBy: number | null;
  qualityConfirmedAt: string | null;
  productionRouteInstanceId: number | null;
  routeLinked: boolean;
  createdAt: string | null;
  updatedAt: string | null;
  materials: WorkOrderMaterial[];
}

export interface WorkOrderDispatchContext {
  workOrder: WorkOrder;
  orderItem: OrderItemProductionContext;
  dispatched: boolean;
}

export interface WorkOrderMaterialGenerationItem {
  materialId: number | null;
  materialCode: string | null;
  materialName: string;
  spec: string | null;
  unit: string | null;
  requiredQty: number | string;
  usageStage: string | null;
  stepTemplateId: number | null;
  stepName: string | null;
  stepOrder: number | null;
  relatedStepTemplateId: number | null;
  relatedStepInstanceId: number | null;
  quantityRuleSummary: string | null;
  warning: string | null;
  remark: string | null;
}

export interface WorkOrderMaterialGenerationResult {
  generatedMaterials: WorkOrderMaterialGenerationItem[];
  generatedCount: number;
  replacedCount: number;
  warnings: string[];
}

export interface WorkOrderMaterialReadinessItem {
  materialId: number | null;
  materialCode: string | null;
  materialName: string;
  spec: string | null;
  unit: string | null;
  requiredQty: number | string;
  availableQty: number | string | null;
  shortageQty: number | string | null;
  readinessStatus: MaterialReadinessStatus;
  readinessMessage: string | null;
  usageStage: string | null;
  relatedStepTemplateId: number | null;
  relatedStepInstanceId: number | null;
  quantityRuleSummary: string | null;
  warning: string | null;
  remark: string | null;
}

export interface WorkOrderMaterialReadinessStep {
  stepTemplateId: number | null;
  stepOrder: number | null;
  stepName: string | null;
  materials: WorkOrderMaterialReadinessItem[];
}

export interface WorkOrderMaterialReadinessSummary {
  totalLines: number;
  readyLines: number;
  shortageLines: number;
  unlinkedLines: number;
  noStockRecordLines: number;
}

export interface WorkOrderMaterialReadinessResult {
  quantitySnapshot: number | string | null;
  itemsByStep: WorkOrderMaterialReadinessStep[];
  summary: WorkOrderMaterialReadinessSummary;
}

export interface WorkOrderCreateWithReadinessPayload {
  orderItemId: number;
  routeTemplateId: number;
  workOrderFields: WorkOrderBasePayload;
  applyGeneratedMaterials: boolean;
}
