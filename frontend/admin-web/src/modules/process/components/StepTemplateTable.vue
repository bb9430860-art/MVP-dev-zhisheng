<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="$emit('create')"
        >新增工序</el-button
      >
      <el-button :icon="Sort" @click="saveManualOrder">保存手动顺序</el-button>
    </div>
    <el-table :data="steps" v-loading="loading" border>
      <el-table-column label="顺序" width="90">
        <template #default="{ row }">
          <el-input-number
            v-model="manualOrderById[row.id]"
            :min="1"
            :disabled="!row.enabled"
            controls-position="right"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column prop="stepCode" label="工序编码" min-width="120" />
      <el-table-column prop="stepName" label="工序名称" min-width="140" />
      <el-table-column prop="assignedRole" label="执行角色" min-width="140" />
      <el-table-column prop="photoRequired" label="拍照" width="80">
        <template #default="{ row }">
          <el-tag :type="row.photoRequired ? 'warning' : 'info'">{{
            row.photoRequired ? "需要" : "否"
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remarkRequired" label="备注" width="80">
        <template #default="{ row }">
          <el-tag :type="row.remarkRequired ? 'warning' : 'info'">{{
            row.remarkRequired ? "需要" : "否"
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="mobileEnabled" label="移动端" width="90">
        <template #default="{ row }">
          <el-tag :type="row.mobileEnabled ? 'success' : 'info'">{{
            row.mobileEnabled ? "允许" : "否"
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{
            row.enabled ? "启用" : "停用"
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="estimatedHours" label="工时" width="90" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button
              size="small"
              :disabled="!row.enabled"
              @click="$emit('move-up', row)"
              >上移</el-button
            >
            <el-button
              size="small"
              :disabled="!row.enabled"
              @click="$emit('move-down', row)"
              >下移</el-button
            >
            <el-button size="small" @click="$emit('edit', row)">编辑</el-button>
            <el-button size="small" @click="$emit('toggle-enabled', row)">
              {{ row.enabled ? "停用" : "启用" }}
            </el-button>
            <el-button size="small" type="danger" @click="$emit('delete', row)"
              >删除</el-button
            >
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { Plus, Sort } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { reactive, watch } from "vue";

import type { StepTemplate } from "../types";

const props = defineProps<{
  steps: StepTemplate[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  create: [];
  edit: [step: StepTemplate];
  "toggle-enabled": [step: StepTemplate];
  delete: [step: StepTemplate];
  "move-up": [step: StepTemplate];
  "move-down": [step: StepTemplate];
  reorder: [stepIds: number[]];
}>();

const manualOrderById = reactive<Record<number, number>>({});

watch(
  () => props.steps,
  (steps) => {
    for (const step of steps) {
      manualOrderById[step.id] = step.stepOrder;
    }
  },
  { immediate: true, deep: true },
);

function saveManualOrder() {
  const activeSteps = props.steps.filter(
    (step) => step.enabled && !step.deleted,
  );
  if (activeSteps.length === 0) {
    ElMessage.warning("没有可排序的启用工序");
    return;
  }

  const ordered = [...activeSteps].sort((left, right) => {
    const leftOrder = manualOrderById[left.id] ?? left.stepOrder;
    const rightOrder = manualOrderById[right.id] ?? right.stepOrder;
    return leftOrder - rightOrder || left.id - right.id;
  });

  emit(
    "reorder",
    ordered.map((step) => step.id),
  );
}
</script>
