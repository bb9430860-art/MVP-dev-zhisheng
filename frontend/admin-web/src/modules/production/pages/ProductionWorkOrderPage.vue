<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">生产工单</h1>
        <p class="page-description">
          从订单项创建生产指令工单，发布后进入生产准备。物料需求不代表库存已预留或已扣减。
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadWorkOrders">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">
          从订单项创建工单
        </el-button>
      </div>
    </div>

    <el-alert
      title="物料需求只是需求清单，不代表库存齐套、已预留或已扣减。库存齐套与缺料节点由后续库存齐套模块处理。"
      type="info"
      show-icon
      :closable="false"
      class="boundary-alert"
    />

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="filters.status" clearable placeholder="状态" style="width: 150px">
          <el-option
            v-for="item in workOrderStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="filters.workOrderNo"
          clearable
          placeholder="工单编号"
          style="width: 180px"
        />
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="产品/点位关键字"
          style="width: 220px"
        />
        <el-date-picker
          v-model="plannedRange"
          type="daterange"
          range-separator="至"
          start-placeholder="计划开始"
          end-placeholder="计划结束"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
        <el-select
          v-model="filters.routeLinked"
          clearable
          placeholder="生产实例"
          style="width: 150px"
        >
          <el-option label="已关联" :value="true" />
          <el-option label="未关联" :value="false" />
        </el-select>
        <el-button type="primary" @click="loadWorkOrders">查询</el-button>
      </div>

      <el-table :data="workOrders" border v-loading="loading">
        <el-table-column prop="workOrderNo" label="工单编号" width="170" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="workOrderStatusTagType(row.status)">
              {{ formatWorkOrderStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单" min-width="150">
          <template #default="{ row }">
            <div>{{ row.orderNo ?? `订单 ${row.orderId}` }}</div>
            <small class="muted">{{ row.orderType ?? "-" }} / {{ row.customerType ?? "-" }}</small>
          </template>
        </el-table-column>
        <el-table-column label="产品/点位" min-width="220">
          <template #default="{ row }">
            <div>{{ row.orderItemNameSnapshot }}</div>
            <small class="muted">
              {{ formatProductionProductType(row.productTypeSnapshot) }} · 数量 {{ row.quantitySnapshot ?? "-" }}
            </small>
          </template>
        </el-table-column>
        <el-table-column prop="plannedStartDate" label="计划开始" width="120" />
        <el-table-column prop="requiredDeliveryDate" label="要求交付" width="120" />
        <el-table-column label="生产实例" width="130">
          <template #default="{ row }">
            <el-tag :type="row.routeLinked ? 'success' : 'info'">
              {{ row.routeLinked ? `已关联 ${row.productionRouteInstanceId}` : "未关联" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="openDetail(row.id)">详情</el-button>
              <el-button
                size="small"
                :disabled="!canEditWorkOrder(row.status)"
                @click="openEdit(row.id)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                type="primary"
                :disabled="!canReleaseWorkOrder(row.status)"
                @click="releaseRow(row)"
              >
                发布
              </el-button>
              <el-button
                size="small"
                type="danger"
                :disabled="!canCancelWorkOrder(row.status)"
                @click="cancelRow(row)"
              >
                取消
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="candidateDialogVisible" title="选择订单项" width="960px">
      <div class="toolbar">
        <el-input
          v-model="candidateKeyword"
          clearable
          placeholder="订单号 / 产品 / 规格 / 备注"
          style="width: 280px"
        />
        <el-button type="primary" @click="loadCandidates">查询</el-button>
      </div>
      <el-table :data="candidates" border v-loading="candidateLoading" height="360">
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column prop="orderType" label="订单类型" width="110" />
        <el-table-column prop="customerType" label="客户类型" width="110" />
        <el-table-column prop="dealOwnerName" label="成交人" width="120" />
        <el-table-column label="产品/点位" min-width="220">
          <template #default="{ row }">
            <div>{{ row.itemName }}</div>
            <small class="muted">{{ row.spec ?? "-" }} · {{ row.unit ?? "-" }} · {{ row.quantity ?? "-" }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column prop="productionStatus" label="生产状态" width="130" />
        <el-table-column label="工单" width="150">
          <template #default="{ row }">
            <el-tag :type="row.hasActiveWorkOrder ? 'warning' : 'success'">
              {{ row.hasActiveWorkOrder ? row.activeWorkOrderNo : "可创建" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :disabled="row.hasActiveWorkOrder"
              @click="selectCandidate(row)"
            >
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-drawer v-model="formDrawerVisible" :title="formTitle" size="720px">
      <el-form label-width="110px" :model="form">
        <el-alert
          v-if="selectedCandidate"
          :title="`${selectedCandidate.orderNo ?? `订单 ${selectedCandidate.orderId}`} / ${selectedCandidate.itemName}`"
          type="info"
          :closable="false"
          class="drawer-alert"
        />

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" clearable>
                <el-option label="普通" value="NORMAL" />
                <el-option label="高" value="HIGH" />
                <el-option label="紧急" value="URGENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号">
              <el-input v-model="form.equipmentModel" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="指令标题">
          <el-input v-model="form.instructionTitle" />
        </el-form-item>
        <el-form-item label="生产要求">
          <el-input v-model="form.productionRequirement" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="质量要求">
          <el-input v-model="form.qualityRequirement" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="包装要求">
          <el-input v-model="form.packagingRequirement" />
        </el-form-item>
        <el-form-item label="发货要求">
          <el-input v-model="form.shippingRequirement" />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="计划开始">
              <el-date-picker v-model="form.plannedStartDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划完成">
              <el-date-picker v-model="form.plannedFinishDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="要求交付">
              <el-date-picker v-model="form.requiredDeliveryDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="负责人 ID">
              <el-input-number v-model="form.responsibleUserId" :min="1" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="经办人 ID">
              <el-input-number v-model="form.handlerUserId" :min="1" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产经理 ID">
              <el-input-number v-model="form.productionManagerId" :min="1" :controls="false" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="技术摘要">
          <el-input v-model="form.technicalConfigSummary" />
        </el-form-item>
        <el-form-item label="技术 JSON">
          <el-input v-model="form.technicalConfigJson" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <el-divider>物料需求</el-divider>
      <el-alert
        title="这里只维护需求清单，不显示已预留、已扣库存或库存齐套。"
        type="warning"
        show-icon
        :closable="false"
        class="drawer-alert"
      />
      <el-table :data="materialForm" border>
        <el-table-column label="物料名称" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.materialName" />
          </template>
        </el-table-column>
        <el-table-column label="规格" width="130">
          <template #default="{ row }">
            <el-input v-model="row.spec" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="90">
          <template #default="{ row }">
            <el-input v-model="row.unit" />
          </template>
        </el-table-column>
        <el-table-column label="需求数量" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.requiredQty" :min="0" :controls="false" />
          </template>
        </el-table-column>
        <el-table-column label="使用阶段" width="130">
          <template #default="{ row }">
            <el-input v-model="row.usageStage" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ $index }">
            <el-button size="small" type="danger" @click="removeMaterial($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="footer-actions">
        <el-button @click="addMaterial">新增物料</el-button>
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </div>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="工单详情" size="720px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ detail.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="workOrderStatusTagType(detail.status)">
              {{ formatWorkOrderStatus(detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detail.orderNo ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ detail.orderType ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="产品/点位">{{ detail.orderItemNameSnapshot }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ detail.quantitySnapshot ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="生产实例">
            {{ detail.productionRouteInstanceId ?? "未关联" }}
          </el-descriptions-item>
          <el-descriptions-item label="要求交付">{{ detail.requiredDeliveryDate ?? "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>生产指令</el-divider>
        <p>{{ detail.productionRequirement || "-" }}</p>

        <el-divider>物料需求</el-divider>
        <el-table :data="detail.materials" border>
          <el-table-column prop="materialName" label="物料" />
          <el-table-column prop="spec" label="规格" width="130" />
          <el-table-column prop="unit" label="单位" width="90" />
          <el-table-column prop="requiredQty" label="需求数量" width="120" />
          <el-table-column prop="usageStage" label="使用阶段" width="130" />
        </el-table>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { Plus, Refresh } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";

import {
  cancelWorkOrder,
  createWorkOrderFromOrderItem,
  getWorkOrder,
  listWorkOrderCandidates,
  listWorkOrders,
  releaseWorkOrder,
  updateWorkOrder,
  updateWorkOrderMaterials,
} from "../api/productionWorkOrderApi";
import type {
  ProductionWorkOrderStatus,
  WorkOrder,
  WorkOrderBasePayload,
  WorkOrderCandidate,
  WorkOrderMaterialPayload,
} from "../types";
import { formatProductionProductType } from "../utils/displayLabels";
import {
  canCancelWorkOrder,
  canEditWorkOrder,
  canReleaseWorkOrder,
  workOrderStatusTagType,
} from "../utils/workOrderRules";

const workOrderStatusOptions = [
  { label: "草稿", value: "DRAFT" },
  { label: "已发布", value: "RELEASED" },
  { label: "生产中", value: "IN_PROGRESS" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" },
] as const;

const loading = ref(false);
const saving = ref(false);
const workOrders = ref<WorkOrder[]>([]);
const candidates = ref<WorkOrderCandidate[]>([]);
const detail = ref<WorkOrder | null>(null);
const selectedCandidate = ref<WorkOrderCandidate | null>(null);
const editingWorkOrderId = ref<number | null>(null);
const candidateDialogVisible = ref(false);
const formDrawerVisible = ref(false);
const detailDrawerVisible = ref(false);
const candidateLoading = ref(false);
const candidateKeyword = ref("");
const plannedRange = ref<[string, string] | null>(null);

const filters = reactive<{
  status?: ProductionWorkOrderStatus;
  workOrderNo: string;
  keyword: string;
  routeLinked?: boolean;
}>({
  status: undefined,
  workOrderNo: "",
  keyword: "",
  routeLinked: undefined,
});

const form = reactive<WorkOrderBasePayload>({
  priority: "NORMAL",
  instructionTitle: "",
  productionRequirement: "",
  qualityRequirement: "",
  packagingRequirement: "",
  shippingRequirement: "",
  deliveryRequirement: "",
  plannedStartDate: null,
  plannedFinishDate: null,
  requiredDeliveryDate: null,
  equipmentModel: "",
  technicalConfigSummary: "",
  technicalConfigJson: "",
  responsibleUserId: null,
  handlerUserId: null,
  productionManagerId: null,
  customerAcceptanceRequired: false,
});

const materialForm = ref<WorkOrderMaterialPayload[]>([]);
const formTitle = computed(() =>
  editingWorkOrderId.value ? "编辑 DRAFT 工单" : "创建 DRAFT 工单",
);

onMounted(loadWorkOrders);

async function loadWorkOrders() {
  loading.value = true;
  try {
    const result = await listWorkOrders({
      status: filters.status,
      workOrderNo: filters.workOrderNo || undefined,
      keyword: filters.keyword || undefined,
      routeLinked: filters.routeLinked,
      plannedStartFrom: plannedRange.value?.[0],
      plannedStartTo: plannedRange.value?.[1],
      pageSize: 50,
    });
    workOrders.value = result.items;
  } finally {
    loading.value = false;
  }
}

async function openCreateDialog() {
  candidateDialogVisible.value = true;
  await loadCandidates();
}

async function loadCandidates() {
  candidateLoading.value = true;
  try {
    const result = await listWorkOrderCandidates({
      keyword: candidateKeyword.value || undefined,
      pageSize: 50,
    });
    candidates.value = result.items;
  } finally {
    candidateLoading.value = false;
  }
}

function selectCandidate(row: WorkOrderCandidate) {
  if (row.hasActiveWorkOrder) {
    ElMessage.warning(`该订单项已有 active 工单：${row.activeWorkOrderNo ?? row.activeWorkOrderId}`);
    return;
  }
  selectedCandidate.value = row;
  editingWorkOrderId.value = null;
  resetForm();
  materialForm.value = [];
  addMaterial();
  candidateDialogVisible.value = false;
  formDrawerVisible.value = true;
}

async function openEdit(workOrderId: number) {
  const item = await getWorkOrder(workOrderId);
  if (!canEditWorkOrder(item.status)) {
    ElMessage.warning("只有 DRAFT 工单可以编辑");
    return;
  }
  editingWorkOrderId.value = item.id;
  selectedCandidate.value = null;
  Object.assign(form, pickBasePayload(item));
  materialForm.value = item.materials.map((material) => ({ ...material }));
  formDrawerVisible.value = true;
}

async function openDetail(workOrderId: number) {
  detail.value = await getWorkOrder(workOrderId);
  detailDrawerVisible.value = true;
}

async function saveForm() {
  const invalid = materialForm.value.find(
    (item) => !item.materialName.trim() || Number(item.requiredQty) <= 0,
  );
  if (invalid) {
    ElMessage.warning("物料名称必填，需求数量必须大于 0");
    return;
  }
  saving.value = true;
  try {
    if (editingWorkOrderId.value) {
      await updateWorkOrder(editingWorkOrderId.value, { ...form });
      await updateWorkOrderMaterials(editingWorkOrderId.value, materialForm.value);
      ElMessage.success("工单已更新");
    } else if (selectedCandidate.value) {
      await createWorkOrderFromOrderItem({
        orderItemId: selectedCandidate.value.orderItemId,
        ...form,
        materials: materialForm.value,
      });
      ElMessage.success("DRAFT 工单已创建");
    }
    formDrawerVisible.value = false;
    await loadWorkOrders();
  } catch (error) {
    if (error instanceof Error && error.message === "WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM") {
      ElMessage.error("该订单项已有 active 工单，不能重复创建");
    }
    throw error;
  } finally {
    saving.value = false;
  }
}

async function releaseRow(row: WorkOrder) {
  await ElMessageBox.confirm("发布后生产指令和物料需求默认冻结，确认发布？", "发布工单", {
    confirmButtonText: "发布",
    cancelButtonText: "取消",
    type: "warning",
  });
  await releaseWorkOrder(row.id);
  ElMessage.success("工单已发布");
  await loadWorkOrders();
}

async function cancelRow(row: WorkOrder) {
  await ElMessageBox.confirm("确认取消该工单？", "取消工单", {
    confirmButtonText: "取消工单",
    cancelButtonText: "返回",
    type: "warning",
  });
  await cancelWorkOrder(row.id);
  ElMessage.success("工单已取消");
  await loadWorkOrders();
}

function addMaterial() {
  materialForm.value.push({
    materialName: "",
    spec: "",
    unit: "",
    requiredQty: 1,
    usageStage: "",
  });
}

function removeMaterial(index: number) {
  materialForm.value.splice(index, 1);
}

function resetForm() {
  Object.assign(form, {
    priority: "NORMAL",
    instructionTitle: "",
    instructionRemark: "",
    productionRequirement: "",
    qualityRequirement: "",
    packagingRequirement: "",
    shippingRequirement: "",
    deliveryRequirement: "",
    plannedStartDate: null,
    plannedFinishDate: null,
    requiredDeliveryDate: null,
    deadlineRemark: "",
    equipmentModel: "",
    technicalConfigSummary: "",
    technicalConfigRemark: "",
    technicalConfigJson: "",
    responsibleUserId: null,
    handlerUserId: null,
    productionManagerId: null,
    primaryWorkerId: null,
    customerAcceptanceRequired: false,
    acceptanceRemark: "",
  });
}

function pickBasePayload(item: WorkOrder): WorkOrderBasePayload {
  return {
    priority: item.priority,
    instructionTitle: item.instructionTitle,
    instructionRemark: item.instructionRemark,
    productionRequirement: item.productionRequirement,
    qualityRequirement: item.qualityRequirement,
    packagingRequirement: item.packagingRequirement,
    shippingRequirement: item.shippingRequirement,
    deliveryRequirement: item.deliveryRequirement,
    plannedStartDate: item.plannedStartDate,
    plannedFinishDate: item.plannedFinishDate,
    requiredDeliveryDate: item.requiredDeliveryDate,
    deadlineRemark: item.deadlineRemark,
    equipmentModel: item.equipmentModel,
    technicalConfigSummary: item.technicalConfigSummary,
    technicalConfigRemark: item.technicalConfigRemark,
    technicalConfigJson: item.technicalConfigJson,
    responsibleUserId: item.responsibleUserId,
    handlerUserId: item.handlerUserId,
    productionManagerId: item.productionManagerId,
    primaryWorkerId: item.primaryWorkerId,
    customerAcceptanceRequired: item.customerAcceptanceRequired,
    acceptanceRemark: item.acceptanceRemark,
  };
}

function formatWorkOrderStatus(status: ProductionWorkOrderStatus) {
  return workOrderStatusOptions.find((item) => item.value === status)?.label ?? status;
}
</script>

<style scoped>
.header-actions,
.toolbar,
.table-actions,
.footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.boundary-alert {
  margin-bottom: 16px;
}

.drawer-alert {
  margin-bottom: 16px;
}

.muted {
  color: #718096;
}

.footer-actions {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
