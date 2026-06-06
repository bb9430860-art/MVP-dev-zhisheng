<template>
  <el-dialog
    :model-value="visible"
    :title="step ? '编辑工序模板' : '新增工序模板'"
    width="640px"
    @close="close"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="工序编码" prop="stepCode">
        <el-input v-model.trim="form.stepCode" maxlength="64" placeholder="例如 CUT" />
      </el-form-item>
      <el-form-item label="工序名称" prop="stepName">
        <el-input v-model.trim="form.stepName" maxlength="100" placeholder="例如 下料" />
      </el-form-item>
      <el-form-item label="执行角色" prop="assignedRole">
        <el-select v-model="form.assignedRole" filterable allow-create placeholder="选择系统角色">
          <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="预计工时">
        <el-input-number v-model="estimatedHoursValue" :min="0" :precision="2" :step="0.5" />
      </el-form-item>
      <el-form-item label="执行要求">
        <el-checkbox v-model="form.photoRequired">需要拍照</el-checkbox>
        <el-checkbox v-model="form.remarkRequired">需要备注</el-checkbox>
        <el-checkbox v-model="form.mobileEnabled">移动端执行</el-checkbox>
        <el-checkbox v-model="form.enabled">启用</el-checkbox>
      </el-form-item>
      <el-form-item label="操作说明">
        <el-input
          v-model="form.operationInstruction"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'

import { roleOptions } from '../constants'
import type { StepTemplate, StepTemplatePayload } from '../types'

const props = defineProps<{
  visible: boolean
  step: StepTemplate | null
  saving?: boolean
}>()

const emit = defineEmits<{
  close: []
  save: [payload: StepTemplatePayload]
}>()

const formRef = ref<FormInstance>()
const form = reactive<StepTemplatePayload>({
  stepCode: '',
  stepName: '',
  assignedRole: 'WORKER',
  photoRequired: false,
  remarkRequired: false,
  mobileEnabled: true,
  estimatedHours: null,
  operationInstruction: '',
  enabled: true,
})

const estimatedHoursValue = computed({
  get: () => (form.estimatedHours == null ? undefined : Number(form.estimatedHours)),
  set: (value?: number) => {
    form.estimatedHours = value ?? null
  },
})

const rules: FormRules<StepTemplatePayload> = {
  stepCode: [{ required: true, message: '请输入工序编码', trigger: 'blur' }],
  stepName: [{ required: true, message: '请输入工序名称', trigger: 'blur' }],
  assignedRole: [{ required: true, message: '请输入执行角色', trigger: 'change' }],
}

watch(
  () => [props.visible, props.step] as const,
  () => {
    if (!props.visible) {
      return
    }
    Object.assign(form, {
      stepCode: props.step?.stepCode ?? '',
      stepName: props.step?.stepName ?? '',
      assignedRole: props.step?.assignedRole ?? 'WORKER',
      photoRequired: props.step?.photoRequired ?? false,
      remarkRequired: props.step?.remarkRequired ?? false,
      mobileEnabled: props.step?.mobileEnabled ?? true,
      estimatedHours: props.step?.estimatedHours ?? null,
      operationInstruction: props.step?.operationInstruction ?? '',
      enabled: props.step?.enabled ?? true,
    })
  },
  { immediate: true },
)

function close() {
  emit('close')
}

async function submit() {
  const valid = await formRef.value?.validate()
  if (valid) {
    emit('save', { ...form })
  }
}
</script>
