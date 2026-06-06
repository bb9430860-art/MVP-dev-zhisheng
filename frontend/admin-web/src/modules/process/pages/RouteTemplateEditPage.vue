<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ isCreate ? '新建工艺路线模板' : '编辑工艺路线模板' }}</h1>
        <p class="page-description">模板阶段只配置系统角色和执行要求，不绑定具体员工。</p>
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
          <h2 class="section-title">工序模板</h2>
          <p class="page-description">停用工序仍显示在管理列表中，但不参与 active 工序排序。</p>
        </div>
      </div>
      <StepTemplateTable
        :steps="steps"
        :loading="stepLoading"
        @create="openStepDialog(null)"
        @edit="openStepDialog"
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
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import RouteTemplateForm from '../components/RouteTemplateForm.vue'
import StepTemplateFormDialog from '../components/StepTemplateFormDialog.vue'
import StepTemplateTable from '../components/StepTemplateTable.vue'
import {
  createRouteTemplate,
  createStepTemplate,
  deleteStepTemplate,
  getRouteTemplate,
  listStepTemplates,
  moveStepDown,
  moveStepUp,
  reorderSteps,
  setRouteTemplateEnabled,
  setStepTemplateEnabled,
  updateRouteTemplate,
  updateStepTemplate,
} from '../api/processRouteTemplateApi'
import type { RouteTemplatePayload, StepTemplate, StepTemplatePayload } from '../types'
import { hasActiveEnabledStep } from '../utils/routeTemplateRules'

const router = useRouter()
const currentRoute = useRoute()
const routeId = computed(() => {
  const value = currentRoute.params.id
  return value ? Number(value) : null
})
const isCreate = computed(() => routeId.value === null)

const form = reactive<RouteTemplatePayload>({
  routeCode: '',
  routeName: '',
  productType: '',
  description: '',
  enabled: false,
})
const originalEnabled = ref(false)
const routeSaving = ref(false)
const stepLoading = ref(false)
const stepSaving = ref(false)
const steps = ref<StepTemplate[]>([])
const stepDialogVisible = ref(false)
const editingStep = ref<StepTemplate | null>(null)

onMounted(async () => {
  if (routeId.value) {
    await Promise.all([loadRoute(), loadSteps()])
  }
})

async function loadRoute() {
  if (!routeId.value) {
    return
  }
  const route = await getRouteTemplate(routeId.value)
  Object.assign(form, {
    routeCode: route.routeCode,
    routeName: route.routeName,
    productType: route.productType ?? '',
    description: route.description ?? '',
    enabled: route.enabled,
  })
  originalEnabled.value = route.enabled
}

async function loadSteps() {
  if (!routeId.value) {
    return
  }
  stepLoading.value = true
  try {
    steps.value = await listStepTemplates(routeId.value)
  } finally {
    stepLoading.value = false
  }
}

async function saveRoute() {
  routeSaving.value = true
  try {
    if (!routeId.value) {
      const created = await createRouteTemplate({ ...form, enabled: false })
      ElMessage.success('已创建路线模板')
      await router.replace(`/process/route-templates/${created.id}/edit`)
      return
    }

    await updateRouteTemplate(routeId.value, { ...form, enabled: originalEnabled.value })
    if (form.enabled !== originalEnabled.value) {
      await applyRouteEnabled(form.enabled === true)
    }
    await loadRoute()
    ElMessage.success('已保存路线模板')
  } finally {
    routeSaving.value = false
  }
}

async function applyRouteEnabled(enabled: boolean) {
  if (!routeId.value) {
    return
  }
  if (enabled && !hasActiveEnabledStep(steps.value)) {
    ElMessage.warning('启用路线模板前至少需要一道启用工序')
    return
  }
  const updated = await setRouteTemplateEnabled(routeId.value, enabled)
  originalEnabled.value = updated.enabled
  form.enabled = updated.enabled
}

function openStepDialog(step: StepTemplate | null) {
  editingStep.value = step
  stepDialogVisible.value = true
}

function closeStepDialog() {
  stepDialogVisible.value = false
  editingStep.value = null
}

async function saveStep(payload: StepTemplatePayload) {
  if (!routeId.value) {
    return
  }
  stepSaving.value = true
  try {
    if (editingStep.value) {
      await updateStepTemplate(routeId.value, editingStep.value.id, payload)
      ElMessage.success('已更新工序')
    } else {
      await createStepTemplate(routeId.value, payload)
      ElMessage.success('已新增工序')
    }
    closeStepDialog()
    await loadSteps()
  } finally {
    stepSaving.value = false
  }
}

async function toggleStepEnabled(step: StepTemplate) {
  if (!routeId.value) {
    return
  }
  await setStepTemplateEnabled(routeId.value, step.id, !step.enabled)
  ElMessage.success(step.enabled ? '已停用工序' : '已启用工序')
  await loadSteps()
}

async function removeStep(step: StepTemplate) {
  if (!routeId.value) {
    return
  }
  await ElMessageBox.confirm(`确认删除工序「${step.stepName}」？`, '删除确认', {
    type: 'warning',
  })
  await deleteStepTemplate(routeId.value, step.id)
  ElMessage.success('已删除工序')
  await loadSteps()
}

async function moveUp(step: StepTemplate) {
  if (!routeId.value) {
    return
  }
  steps.value = await moveStepUp(routeId.value, step.id)
}

async function moveDown(step: StepTemplate) {
  if (!routeId.value) {
    return
  }
  steps.value = await moveStepDown(routeId.value, step.id)
}

async function saveOrder(stepIds: number[]) {
  if (!routeId.value) {
    return
  }
  steps.value = await reorderSteps(routeId.value, stepIds)
  ElMessage.success('已保存工序顺序')
}
</script>
