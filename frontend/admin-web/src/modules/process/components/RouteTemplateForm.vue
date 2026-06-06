<template>
  <el-form ref="formRef" :model="localForm" :rules="rules" label-width="120px" class="form-section">
    <el-form-item label="路线编码" prop="routeCode">
      <el-input v-model.trim="localForm.routeCode" maxlength="64" placeholder="例如 RT-SPIRIT" />
    </el-form-item>
    <el-form-item label="路线名称" prop="routeName">
      <el-input v-model.trim="localForm.routeName" maxlength="100" placeholder="例如 精神堡垒工艺路线" />
    </el-form-item>
    <el-form-item label="产品类型">
      <el-select
        v-model="localForm.productType"
        allow-create
        clearable
        filterable
        default-first-option
        placeholder="输入或选择字典值"
      >
        <el-option
          v-for="item in productTypeSuggestions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="启用状态">
      <el-switch v-model="localForm.enabled" active-text="启用" inactive-text="停用" />
    </el-form-item>
    <el-form-item label="描述">
      <el-input
        v-model="localForm.description"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="补充适用场景或工艺说明"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      <el-button @click="$emit('cancel')">返回</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref, watch } from 'vue'

import { productTypeSuggestions } from '../constants'
import type { RouteTemplatePayload } from '../types'

const props = defineProps<{
  modelValue: RouteTemplatePayload
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: RouteTemplatePayload]
  submit: []
  cancel: []
}>()

const formRef = ref<FormInstance>()
const localForm = reactive<RouteTemplatePayload>({
  routeCode: '',
  routeName: '',
  productType: '',
  description: '',
  enabled: false,
})

const rules: FormRules<RouteTemplatePayload> = {
  routeCode: [{ required: true, message: '请输入路线编码', trigger: 'blur' }],
  routeName: [{ required: true, message: '请输入路线名称', trigger: 'blur' }],
}

watch(
  () => props.modelValue,
  (value) => {
    Object.assign(localForm, value)
  },
  { immediate: true, deep: true },
)

watch(
  localForm,
  (value) => {
    emit('update:modelValue', { ...value })
  },
  { deep: true },
)

async function submit() {
  const valid = await formRef.value?.validate()
  if (valid) {
    emit('submit')
  }
}
</script>
