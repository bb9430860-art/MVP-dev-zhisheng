<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          {{ isCreate ? "新建工艺路线模板" : "编辑工艺路线模板" }}
        </h1>
        <p class="page-description">
          配置路线基础信息和工序模板；模板阶段只绑定系统角色，不绑定具体员工。
        </p>
      </div>
    </div>

    <el-card shadow="never">
      <RouteTemplateForm
        v-model="form"
        :saving="routeSaving"
        @submit="saveRoute"
        @cancel="router.push('/process/route-templates')"
      />
    </el-card>

    <el-card v-if="routeId" shadow="never" style="margin-top: 16px">
      <div class="page-header">
        <div>
          <h2 class="section-title">工序模板管理</h2>
          <p class="page-description">
            工序按顺序执行；停用工序仍可查看，但不参与 active 排序和后续实例复制。
          </p>
        </div>
      </div>
      <StepTemplateTable
        :steps="steps"
        :loading="stepLoading"
        @create="openStepDialog(null)"
        @edit="openStepDialog"
        @edit-materials="openMaterialDialog"
        @toggle-enabled="toggleStepEnabled"
        @delete="removeStep"
        @move-up="moveUp"
        @move-down="moveDown"
        @reorder="saveOrder"
      />
    </el-card>

    <StepTemplateFormDialog
      :visible="stepDialogVisible"
      :step="editingStep"
      :saving="stepSaving"
      @close="closeStepDialog"
      @save="saveStep"
    />

    <StepMaterialRequirementDialog
      :visible="materialDialogVisible"
      :step="editingMaterialStep"
      :materials="editingStepMaterials"
      :loading="materialLoading"
      :saving="materialSaving"
      @close="closeMaterialDialog"
      @save="saveStepMaterials"
    />
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import RouteTemplateForm from "../components/RouteTemplateForm.vue";
import StepMaterialRequirementDialog from "../components/StepMaterialRequirementDialog.vue";
import StepTemplateFormDialog from "../components/StepTemplateFormDialog.vue";
import StepTemplateTable from "../components/StepTemplateTable.vue";
import {
  createRouteTemplate,
  createStepTemplate,
  deleteStepTemplate,
  getRouteTemplate,
  listStepMaterialRequirementsByStep,
  listStepTemplates,
  moveStepDown,
  moveStepUp,
  reorderSteps,
  replaceStepMaterialRequirements,
  setRouteTemplateEnabled,
  setStepTemplateEnabled,
  updateRouteTemplate,
  updateStepTemplate,
} from "../api/processRouteTemplateApi";
import type {
  RouteTemplatePayload,
  StepMaterialRequirementPayload,
  StepMaterialRequirementTemplate,
  StepTemplate,
  StepTemplatePayload,
} from "../types";
import { hasActiveEnabledStep } from "../utils/routeTemplateRules";

const router = useRouter();
const currentRoute = useRoute();
const routeId = computed(() => {
  const value = currentRoute.params.id;
  return value ? Number(value) : null;
});
const isCreate = computed(() => routeId.value === null);

const form = reactive<RouteTemplatePayload>({
  routeCode: "",
  routeName: "",
  productType: "",
  description: "",
  enabled: false,
});
const originalEnabled = ref(false);
const routeSaving = ref(false);
const stepLoading = ref(false);
const stepSaving = ref(false);
const materialLoading = ref(false);
const materialSaving = ref(false);
const steps = ref<StepTemplate[]>([]);
const stepDialogVisible = ref(false);
const editingStep = ref<StepTemplate | null>(null);
const materialDialogVisible = ref(false);
const editingMaterialStep = ref<StepTemplate | null>(null);
const editingStepMaterials = ref<StepMaterialRequirementTemplate[]>([]);

onMounted(async () => {
  if (routeId.value) {
    await Promise.all([loadRoute(), loadSteps()]);
  }
});

async function loadRoute() {
  if (!routeId.value) {
    return;
  }
  const route = await getRouteTemplate(routeId.value);
  Object.assign(form, {
    routeCode: route.routeCode,
    routeName: route.routeName,
    productType: route.productType ?? "",
    description: route.description ?? "",
    enabled: route.enabled,
  });
  originalEnabled.value = route.enabled;
}

async function loadSteps() {
  if (!routeId.value) {
    return;
  }
  stepLoading.value = true;
  try {
    steps.value = await listStepTemplates(routeId.value);
  } finally {
    stepLoading.value = false;
  }
}

async function saveRoute() {
  routeSaving.value = true;
  try {
    if (!routeId.value) {
      const created = await createRouteTemplate({ ...form, enabled: false });
      ElMessage.success("已创建路线模板");
      await router.replace(`/process/route-templates/${created.id}/edit`);
      return;
    }

    await updateRouteTemplate(routeId.value, {
      ...form,
      enabled: originalEnabled.value,
    });
    if (form.enabled !== originalEnabled.value) {
      const changed = await applyRouteEnabled(form.enabled === true);
      if (!changed) {
        return;
      }
    }
    await loadRoute();
    ElMessage.success("已保存路线模板");
  } finally {
    routeSaving.value = false;
  }
}

async function applyRouteEnabled(enabled: boolean) {
  if (!routeId.value) {
    return false;
  }
  if (enabled && !hasActiveEnabledStep(steps.value)) {
    ElMessage.warning(
      "启用前请至少添加并启用一道工序，否则无法被生产配置选择",
    );
    form.enabled = originalEnabled.value;
    return false;
  }
  if (!enabled) {
    await ElMessageBox.confirm(
      "停用后该路线不会出现在模板选择接口中，确认停用？",
      "停用路线模板",
      {
        confirmButtonText: "确认停用",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  }
  const updated = await setRouteTemplateEnabled(routeId.value, enabled);
  originalEnabled.value = updated.enabled;
  form.enabled = updated.enabled;
  return true;
}

function openStepDialog(step: StepTemplate | null) {
  editingStep.value = step;
  stepDialogVisible.value = true;
}

function closeStepDialog() {
  stepDialogVisible.value = false;
  editingStep.value = null;
}

async function saveStep(payload: StepTemplatePayload) {
  if (!routeId.value) {
    return;
  }
  stepSaving.value = true;
  try {
    if (editingStep.value) {
      await updateStepTemplate(routeId.value, editingStep.value.id, payload);
      ElMessage.success("已更新工序");
    } else {
      await createStepTemplate(routeId.value, payload);
      ElMessage.success("已新增工序");
    }
    closeStepDialog();
    await loadSteps();
  } finally {
    stepSaving.value = false;
  }
}

async function openMaterialDialog(step: StepTemplate) {
  if (!routeId.value) {
    return;
  }
  editingMaterialStep.value = step;
  materialDialogVisible.value = true;
  materialLoading.value = true;
  try {
    editingStepMaterials.value = await listStepMaterialRequirementsByStep(
      routeId.value,
      step.id,
    );
  } finally {
    materialLoading.value = false;
  }
}

function closeMaterialDialog() {
  materialDialogVisible.value = false;
  editingMaterialStep.value = null;
  editingStepMaterials.value = [];
}

async function saveStepMaterials(
  materials: StepMaterialRequirementPayload[],
) {
  if (!routeId.value || !editingMaterialStep.value) {
    return;
  }
  materialSaving.value = true;
  try {
    editingStepMaterials.value = await replaceStepMaterialRequirements(
      routeId.value,
      editingMaterialStep.value.id,
      materials,
    );
    ElMessage.success("已保存工序物料需求");
    closeMaterialDialog();
  } finally {
    materialSaving.value = false;
  }
}

async function toggleStepEnabled(step: StepTemplate) {
  if (!routeId.value) {
    return;
  }
  const nextEnabled = !step.enabled;
  if (!nextEnabled) {
    await ElMessageBox.confirm(
      `停用后「${step.stepName}」不参与 active 排序和后续实例复制，确认停用？`,
      "停用工序模板",
      {
        confirmButtonText: "确认停用",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  }
  await setStepTemplateEnabled(routeId.value, step.id, nextEnabled);
  ElMessage.success(step.enabled ? "已停用工序" : "已启用工序");
  await loadSteps();
}

async function removeStep(step: StepTemplate) {
  if (!routeId.value) {
    return;
  }
  await ElMessageBox.confirm(
    `删除后「${step.stepName}」将从当前工艺路线中移除，确认删除？`,
    "删除工序模板",
    {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    },
  );
  await deleteStepTemplate(routeId.value, step.id);
  ElMessage.success("已删除工序");
  await loadSteps();
}

async function moveUp(step: StepTemplate) {
  if (!routeId.value) {
    return;
  }
  steps.value = await moveStepUp(routeId.value, step.id);
}

async function moveDown(step: StepTemplate) {
  if (!routeId.value) {
    return;
  }
  steps.value = await moveStepDown(routeId.value, step.id);
}

async function saveOrder(stepIds: number[]) {
  if (!routeId.value) {
    return;
  }
  steps.value = await reorderSteps(routeId.value, stepIds);
  ElMessage.success("已保存工序顺序");
}
</script>
