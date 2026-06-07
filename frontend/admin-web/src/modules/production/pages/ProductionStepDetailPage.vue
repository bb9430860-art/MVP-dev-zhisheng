<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">生产工序详情</h1>
        <p class="page-description">
          冻结生产实例只能执行状态流转，不能修改工序结构。
        </p>
      </div>
      <div class="header-actions">
        <el-button @click="router.push('/production/tasks')">返回任务列表</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <el-alert
      title="photo_required / remark_required 当前仅为执行要求元数据。本页面不提供拍照、文件上传、check-in 或强制备注。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <el-row :gutter="16">
      <el-col :span="15">
        <el-card shadow="never" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>{{ detail?.stepName ?? "工序详情" }}</span>
              <el-tag v-if="detail" :type="statusTagType(detail.status)">
                {{ formatProductionStepStatus(detail.status) }}
              </el-tag>
            </div>
          </template>

          <el-descriptions v-if="detail" :column="2" border>
            <el-descriptions-item label="产品/点位">
              {{ detail.itemName || `订单产品 ${detail.orderItemId}` }}
            </el-descriptions-item>
            <el-descriptions-item label="工序顺序">
              {{ detail.stepOrder }}
            </el-descriptions-item>
            <el-descriptions-item label="执行角色">
              {{ formatProductionRole(detail.assignedRole) }}
            </el-descriptions-item>
            <el-descriptions-item label="负责人 ID">
              {{ detail.assignedUserId ?? "未指定" }}
            </el-descriptions-item>
            <el-descriptions-item label="拍照要求">
              <el-tag :type="detail.photoRequired ? 'warning' : 'info'">
                {{ detail.photoRequired ? "需要拍照" : "不需要拍照" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="备注要求">
              <el-tag :type="detail.remarkRequired ? 'warning' : 'info'">
                {{ detail.remarkRequired ? "需要备注" : "不需要备注" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="移动端执行">
              {{ detail.mobileEnabled ? "允许" : "不允许" }}
            </el-descriptions-item>
            <el-descriptions-item label="冻结状态">
              {{ detail.frozen ? "frozen=true" : "frozen=false" }}
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ detail.startedAt ?? "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="开始人">
              {{ detail.startedBy ?? "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="完成时间">
              {{ detail.completedAt ?? "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="完成人">
              {{ detail.completedBy ?? "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="操作说明" :span="2">
              {{ detail.operationInstruction || "-" }}
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无工序详情" />
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card shadow="never">
          <template #header>
            <span>执行操作</span>
          </template>

          <div v-if="detail" class="operation-panel">
            <el-progress :percentage="progress?.progress ?? 0" :stroke-width="12" />
            <div class="progress-text">
              已完成 {{ progress?.completedSteps ?? 0 }} /
              {{ progress?.totalSteps ?? 0 }} 道工序
            </div>
            <el-button
              type="primary"
              :disabled="!canStartStep(detail.status, detail.canStart)"
              @click="startStep"
            >
              开始工序
            </el-button>
            <el-button
              type="success"
              :disabled="!canCompleteStep(detail.status, detail.canComplete)"
              @click="completeStep"
            >
              完成工序
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup lang="ts">
import { Refresh } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import {
  completeProductionStep,
  getProductionRouteProgress,
  getProductionStepDetail,
  startProductionStep,
} from "../api/productionStepExecutionApi";
import type { ProductionProgress, ProductionStepDetail } from "../types";
import { canCompleteStep, canStartStep, statusTagType } from "../utils/executionRules";
import {
  formatProductionRole,
  formatProductionStepStatus,
} from "../utils/displayLabels";

const route = useRoute();
const router = useRouter();
const stepInstanceId = computed(() => Number(route.params.stepInstanceId));
const loading = ref(false);
const detail = ref<ProductionStepDetail | null>(null);
const progress = ref<ProductionProgress | null>(null);

onMounted(loadPage);

async function loadPage() {
  loading.value = true;
  try {
    detail.value = await getProductionStepDetail(stepInstanceId.value);
    progress.value = await getProductionRouteProgress(detail.value.routeInstanceId);
  } finally {
    loading.value = false;
  }
}

async function startStep() {
  await startProductionStep(stepInstanceId.value);
  ElMessage.success("工序已开始");
  await loadPage();
}

async function completeStep() {
  await completeProductionStep(stepInstanceId.value);
  ElMessage.success("工序已完成，进度已更新");
  await loadPage();
}
</script>

<style scoped>
.card-header,
.header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.operation-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-text {
  color: #52616f;
  font-size: 14px;
}
</style>
