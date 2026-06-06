<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">生产下发配置</h1>
        <p class="page-description">
          为产品/点位选择工艺路线，确认下发后生成冻结生产实例。
        </p>
      </div>
      <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>产品/点位上下文</span>
              <el-tag :type="summary?.dispatched ? 'success' : 'info'">
                {{ summary?.dispatched ? "已下发" : "未下发" }}
              </el-tag>
            </div>
          </template>

          <el-descriptions v-if="context" :column="2" border>
            <el-descriptions-item label="产品/点位">
              {{ context.orderItem.itemName }}
            </el-descriptions-item>
            <el-descriptions-item label="订单 ID">
              {{ context.orderItem.orderId }}
            </el-descriptions-item>
            <el-descriptions-item label="产品类型">
              {{ formatProductionProductType(context.orderItem.productType) }}
            </el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ context.orderItem.quantity ?? "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="生产状态">
              {{ context.orderItem.productionStatus }}
            </el-descriptions-item>
            <el-descriptions-item label="生产实例">
              {{ summary?.productionRouteInstanceId ?? "-" }}
            </el-descriptions-item>
          </el-descriptions>

          <el-empty v-else description="暂无产品/点位上下文" />
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>生产摘要</span>
              <el-tag v-if="summary?.frozen" type="warning">frozen=true</el-tag>
            </div>
          </template>

          <el-descriptions v-if="summary" :column="1" border>
            <el-descriptions-item label="进度">
              {{ summary.progress ?? 0 }}%
            </el-descriptions-item>
            <el-descriptions-item label="工序">
              {{ summary.completedSteps }} / {{ summary.totalSteps }}
            </el-descriptions-item>
            <el-descriptions-item label="当前工序">
              {{ summary.currentStepName ?? "-" }}
            </el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="summary?.frozen"
            title="生产实例已冻结，不能继续增删工序或调整流程结构。"
            type="warning"
            show-icon
            :closable="false"
            style="margin-top: 12px"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>下发前配置</span>
          <el-button
            type="primary"
            :disabled="!canEdit"
            :loading="dispatching"
            @click="confirmDispatch"
          >
            确认下发生产
          </el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-select
          v-model="selectedRouteTemplateId"
          filterable
          clearable
          placeholder="选择工艺路线模板"
          style="width: 320px"
          :disabled="!canEdit"
        >
          <el-option
            v-for="item in routeOptions"
            :key="item.id"
            :label="`${item.routeName}（${formatProductionProductType(item.productType)}，${item.stepCount}道工序）`"
            :value="item.id"
          />
        </el-select>
        <el-button
          type="primary"
          plain
          :disabled="!canEdit || !selectedRouteTemplateId"
          @click="loadTemplateConfig"
        >
          从模板生成工序
        </el-button>
        <el-input
          v-model="routeName"
          placeholder="下发路线名称"
          style="width: 260px"
          :disabled="!canEdit"
        />
      </div>

      <el-table :data="steps" border>
        <el-table-column prop="stepOrder" label="顺序" width="90">
          <template #default="{ row }">
            <el-input-number
              v-model="row.stepOrder"
              :min="1"
              :controls="false"
              style="width: 64px"
              :disabled="!canEdit"
            />
          </template>
        </el-table-column>
        <el-table-column label="工序名称" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.stepName" :disabled="!canEdit" />
          </template>
        </el-table-column>
        <el-table-column label="执行角色" min-width="150">
          <template #default="{ row }">
            <el-select v-model="row.assignedRole" :disabled="!canEdit">
              <el-option
                v-for="item in productionRoleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="负责人 ID" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.assignedUserId"
              :min="1"
              :controls="false"
              placeholder="可空"
              style="width: 96px"
              :disabled="!canEdit"
            />
          </template>
        </el-table-column>
        <el-table-column label="拍照" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.photoRequired" :disabled="!canEdit" />
          </template>
        </el-table-column>
        <el-table-column label="备注" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.remarkRequired" :disabled="!canEdit" />
          </template>
        </el-table-column>
        <el-table-column label="移动端" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.mobileEnabled" :disabled="!canEdit" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row, $index }">
            <div class="table-actions">
              <el-button
                size="small"
                :disabled="!canEdit || $index === 0"
                @click="moveStep($index, -1)"
              >
                上移
              </el-button>
              <el-button
                size="small"
                :disabled="!canEdit || $index === steps.length - 1"
                @click="moveStep($index, 1)"
              >
                下移
              </el-button>
              <el-button
                size="small"
                type="danger"
                :disabled="!canEdit"
                @click="removeStep(row.clientStepId)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">请选择模板生成工序，或手动新增工序。</div>
        </template>
      </el-table>

      <div class="footer-actions">
        <el-button :disabled="!canEdit" @click="addStep">新增工序</el-button>
        <el-button :disabled="!canEdit || steps.length === 0" @click="saveManualOrder">
          保存手动顺序
        </el-button>
      </div>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { Refresh } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";

import {
  createDispatchConfigFromTemplate,
  dispatchProduction,
  getProductionConfigContext,
  getProductionSummary,
  listRouteTemplateOptions,
} from "../api/productionDispatchApi";
import { productionRoleOptions } from "../constants";
import type {
  DispatchStepConfig,
  ProductionConfigContext,
  ProductionSummary,
  RouteTemplateOption,
} from "../types";
import {
  canEditDispatchStructure,
  normalizeDispatchStepOrders,
} from "../utils/dispatchConfigRules";
import { formatProductionProductType } from "../utils/displayLabels";

const route = useRoute();
const orderItemId = computed(() => Number(route.params.orderItemId || 1001));
const loading = ref(false);
const dispatching = ref(false);
const context = ref<ProductionConfigContext | null>(null);
const summary = ref<ProductionSummary | null>(null);
const routeOptions = ref<RouteTemplateOption[]>([]);
const selectedRouteTemplateId = ref<number | null>(null);
const routeName = ref("");
const steps = ref<DispatchStepConfig[]>([]);

const canEdit = computed(() =>
  canEditDispatchStructure(Boolean(summary.value?.frozen)),
);

onMounted(loadPage);

async function loadPage() {
  loading.value = true;
  try {
    context.value = await getProductionConfigContext(orderItemId.value);
    summary.value = await getProductionSummary(orderItemId.value);
    routeOptions.value = await listRouteTemplateOptions(
      context.value.orderItem.productType,
    );
  } finally {
    loading.value = false;
  }
}

async function loadTemplateConfig() {
  if (!selectedRouteTemplateId.value) {
    return;
  }
  const config = await createDispatchConfigFromTemplate(
    orderItemId.value,
    selectedRouteTemplateId.value,
  );
  routeName.value = config.routeName;
  steps.value = normalizeDispatchStepOrders(config.steps);
  ElMessage.success("已从模板生成下发前工序");
}

function addStep() {
  const nextOrder = steps.value.length + 1;
  steps.value.push({
    clientStepId: `custom-step-${Date.now()}`,
    sourceStepTemplateId: null,
    stepCode: `CUSTOM-${nextOrder}`,
    stepName: "",
    stepOrder: nextOrder,
    assignedRole: "WORKER",
    assignedUserId: null,
    photoRequired: false,
    remarkRequired: false,
    mobileEnabled: true,
    estimatedHours: null,
    operationInstruction: "",
  });
}

function removeStep(clientStepId: string) {
  steps.value = normalizeDispatchStepOrders(
    steps.value.filter((item) => item.clientStepId !== clientStepId),
  );
}

function moveStep(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= steps.value.length) {
    return;
  }
  const next = steps.value.slice();
  [next[index], next[target]] = [next[target], next[index]];
  steps.value = next.map((step, stepIndex) => ({
    ...step,
    stepOrder: stepIndex + 1,
  }));
}

function saveManualOrder() {
  steps.value = normalizeDispatchStepOrders(steps.value);
  ElMessage.success("已保存手动顺序");
}

async function confirmDispatch() {
  if (!selectedRouteTemplateId.value) {
    ElMessage.warning("请选择工艺路线模板");
    return;
  }
  const normalized = normalizeDispatchStepOrders(steps.value);
  if (normalized.length === 0) {
    ElMessage.warning("请至少配置一道工序");
    return;
  }
  const invalid = normalized.find(
    (step) => !step.stepName.trim() || !step.assignedRole,
  );
  if (invalid) {
    ElMessage.warning("每道工序都必须填写工序名称和执行角色");
    return;
  }

  await ElMessageBox.confirm(
    "确认后将生成 frozen=true 的生产实例，不能再增删工序或调整流程结构。",
    "确认下发生产",
    {
      confirmButtonText: "确认下发",
      cancelButtonText: "取消",
      type: "warning",
    },
  );

  dispatching.value = true;
  try {
    await dispatchProduction(orderItemId.value, {
      routeTemplateId: selectedRouteTemplateId.value,
      routeName: routeName.value || "生产路线",
      steps: normalized,
    });
    ElMessage.success("已确认下发生产");
    steps.value = normalized;
    await loadPage();
  } finally {
    dispatching.value = false;
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>
