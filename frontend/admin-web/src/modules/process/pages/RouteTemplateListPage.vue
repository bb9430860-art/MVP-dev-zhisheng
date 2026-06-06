<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">工艺路线模板</h1>
        <p class="page-description">维护可复用的生产工艺路线和适用产品类型。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="router.push('/process/route-templates/new')">
        新建
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索编码或名称"
          style="width: 240px"
          :prefix-icon="Search"
        />
        <el-select
          v-model="filters.productType"
          clearable
          filterable
          allow-create
          placeholder="产品类型"
          style="width: 220px"
        >
          <el-option
            v-for="item in productTypeFilterOptions"
            :key="item"
            :label="item || '空'"
            :value="item"
          />
        </el-select>
        <el-select v-model="filters.enabled" clearable placeholder="启用状态" style="width: 160px">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
        <el-button :icon="Refresh" @click="loadRoutes">刷新</el-button>
      </div>

      <el-table :data="filteredRoutes" v-loading="loading" border>
        <el-table-column prop="routeCode" label="路线编码" min-width="130" />
        <el-table-column prop="routeName" label="路线名称" min-width="180" />
        <el-table-column prop="productType" label="产品类型" min-width="150">
          <template #default="{ row }">{{ row.productType || '通用' }}</template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="更新时间" width="150">
          <template #default>-</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="router.push(`/process/route-templates/${row.id}/edit`)">
                编辑
              </el-button>
              <el-button size="small" @click="toggleEnabled(row)">
                {{ row.enabled ? '停用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="removeRoute(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  deleteRouteTemplate,
  listRouteTemplates,
  listStepTemplates,
  setRouteTemplateEnabled,
} from '../api/processRouteTemplateApi'
import type { RouteTemplate, RouteTemplateFilters } from '../types'
import { filterRouteTemplates } from '../utils/routeTemplateFilters'
import { hasActiveEnabledStep } from '../utils/routeTemplateRules'

const router = useRouter()
const loading = ref(false)
const routes = ref<RouteTemplate[]>([])
const filters = reactive<RouteTemplateFilters>({
  keyword: '',
  productType: '',
  enabled: null,
})

const filteredRoutes = computed(() => filterRouteTemplates(routes.value, filters))
const productTypeFilterOptions = computed(() =>
  Array.from(new Set(routes.value.map((item) => item.productType).filter(Boolean))) as string[],
)

onMounted(loadRoutes)

async function loadRoutes() {
  loading.value = true
  try {
    routes.value = await listRouteTemplates()
  } finally {
    loading.value = false
  }
}

async function toggleEnabled(route: RouteTemplate) {
  const nextEnabled = !route.enabled
  if (nextEnabled) {
    const steps = await listStepTemplates(route.id)
    if (!hasActiveEnabledStep(steps)) {
      ElMessage.warning('启用路线模板前至少需要一道启用工序')
      return
    }
  }

  await setRouteTemplateEnabled(route.id, nextEnabled)
  ElMessage.success(nextEnabled ? '已启用路线模板' : '已停用路线模板')
  await loadRoutes()
}

async function removeRoute(route: RouteTemplate) {
  await ElMessageBox.confirm(`确认删除路线模板「${route.routeName}」？`, '删除确认', {
    type: 'warning',
  })
  await deleteRouteTemplate(route.id)
  ElMessage.success('已删除路线模板')
  await loadRoutes()
}
</script>
