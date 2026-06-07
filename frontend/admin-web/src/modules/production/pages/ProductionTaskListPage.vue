<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">生产任务执行</h1>
        <p class="page-description">
          管理端临时执行入口，用于验证冻结生产实例的工序开始、完成和进度更新。
        </p>
      </div>
      <el-button :icon="Refresh" @click="loadTasks">刷新</el-button>
    </div>

    <el-alert
      title="拍照要求和备注要求当前仅作为元数据展示；本阶段不要求上传照片，也不要求填写备注。证据上传会在后续 production-step-checkin-photo 阶段实现。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>我的生产任务</span>
          <el-tag type="info">{{ tasks.length }} 项</el-tag>
        </div>
      </template>

      <el-table :data="tasks" border>
        <el-table-column prop="stepOrder" label="顺序" width="80" />
        <el-table-column label="产品/点位" min-width="180">
          <template #default="{ row }">
            {{ row.itemName || `订单产品 ${row.orderItemId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="stepName" label="工序" min-width="150" />
        <el-table-column label="执行角色" width="130">
          <template #default="{ row }">
            {{ formatProductionRole(row.assignedRole) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ formatProductionStepStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="要求" min-width="210">
          <template #default="{ row }">
            <div class="meta-tags">
              <el-tag :type="row.photoRequired ? 'warning' : 'info'">
                {{ row.photoRequired ? "需拍照" : "不需拍照" }}
              </el-tag>
              <el-tag :type="row.remarkRequired ? 'warning' : 'info'">
                {{ row.remarkRequired ? "需备注" : "不需备注" }}
              </el-tag>
              <el-tag :type="row.mobileEnabled ? 'success' : 'info'">
                {{ row.mobileEnabled ? "移动端允许" : "仅管理端" }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="progressByRoute[row.routeInstanceId]?.progress ?? 0"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                size="small"
                type="primary"
                :disabled="!canStartStep(row.status, row.canStart)"
                @click="startStep(row.stepInstanceId)"
              >
                开始
              </el-button>
              <el-button
                size="small"
                type="success"
                :disabled="!canCompleteStep(row.status, row.canComplete)"
                @click="completeStep(row.stepInstanceId)"
              >
                完成
              </el-button>
              <el-button size="small" @click="openDetail(row.stepInstanceId)">
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无可执行生产任务，请先完成生产下发。" />
        </template>
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { Refresh } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import {
  completeProductionStep,
  getProductionRouteProgress,
  listMyProductionTasks,
  startProductionStep,
} from "../api/productionStepExecutionApi";
import type { ProductionProgress, ProductionTask } from "../types";
import { canCompleteStep, canStartStep, statusTagType } from "../utils/executionRules";
import {
  formatProductionRole,
  formatProductionStepStatus,
} from "../utils/displayLabels";

const router = useRouter();
const loading = ref(false);
const tasks = ref<ProductionTask[]>([]);
const progressByRoute = ref<Record<number, ProductionProgress>>({});

onMounted(loadTasks);

async function loadTasks() {
  loading.value = true;
  try {
    tasks.value = await listMyProductionTasks();
    await loadProgress();
  } finally {
    loading.value = false;
  }
}

async function loadProgress() {
  const routeIds = Array.from(new Set(tasks.value.map((task) => task.routeInstanceId)));
  const entries = await Promise.all(
    routeIds.map(async (routeId) => [routeId, await getProductionRouteProgress(routeId)] as const),
  );
  progressByRoute.value = Object.fromEntries(entries);
}

async function startStep(stepInstanceId: number) {
  await startProductionStep(stepInstanceId);
  ElMessage.success("工序已开始");
  await loadTasks();
}

async function completeStep(stepInstanceId: number) {
  await completeProductionStep(stepInstanceId);
  ElMessage.success("工序已完成，进度已更新");
  await loadTasks();
}

function openDetail(stepInstanceId: number) {
  router.push(`/production/step-instances/${stepInstanceId}`);
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta-tags,
.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
