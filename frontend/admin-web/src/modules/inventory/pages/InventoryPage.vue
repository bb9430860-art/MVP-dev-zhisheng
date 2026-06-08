<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">库存管理</h1>
        <p class="page-description">
          维护物料档案、库存余额和库存流水，所有余额变化都通过流水记录。
        </p>
      </div>
      <el-button :icon="Refresh" @click="refreshActiveTab">刷新</el-button>
    </div>

    <el-alert
      title="库存核心只记录物料余额和流水。本阶段不做工单齐套、不做缺料检查、不做生产自动扣料。"
      type="info"
      show-icon
      :closable="false"
      class="boundary-alert"
    />

    <el-tabs v-model="activeTab" @tab-change="refreshActiveTab">
      <el-tab-pane label="物料档案" name="materials">
        <el-card shadow="never">
          <div class="toolbar">
            <el-input
              v-model="materialFilters.keyword"
              clearable
              placeholder="搜索编码、名称或规格"
              style="width: 260px"
              :prefix-icon="Search"
              @keyup.enter="loadMaterials"
            />
            <el-select
              v-model="materialFilters.enabled"
              clearable
              placeholder="启用状态"
              style="width: 150px"
            >
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
            <el-button :icon="Search" @click="loadMaterials">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="openMaterialDialog()">
              新增物料
            </el-button>
          </div>

          <el-table :data="materials" v-loading="materialLoading" border>
            <el-table-column prop="materialCode" label="物料编码" min-width="130" />
            <el-table-column prop="materialName" label="物料名称" min-width="180" />
            <el-table-column prop="spec" label="规格" min-width="160" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" width="90" />
            <el-table-column prop="category" label="分类" min-width="120" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">
                  {{ row.enabled ? "启用" : "停用" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button size="small" @click="openMaterialDialog(row)">编辑</el-button>
                  <el-button size="small" @click="toggleMaterial(row)">
                    {{ row.enabled ? "停用" : "启用" }}
                  </el-button>
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="!canOperateStock(row.enabled)"
                    @click="openOperationDialog('MANUAL_IN', row.id)"
                  >
                    入库
                  </el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无物料档案" />
            </template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="库存余额" name="stocks">
        <el-card shadow="never">
          <div class="toolbar">
            <el-input
              v-model="stockFilters.keyword"
              clearable
              placeholder="搜索物料编码、名称或规格"
              style="width: 280px"
              :prefix-icon="Search"
              @keyup.enter="loadStocks"
            />
            <el-button :icon="Search" @click="loadStocks">查询</el-button>
          </div>

          <el-table :data="stocks" v-loading="stockLoading" border>
            <el-table-column prop="materialCode" label="物料编码" min-width="130" />
            <el-table-column prop="materialName" label="物料名称" min-width="180" />
            <el-table-column prop="spec" label="规格" min-width="150" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" width="90" />
            <el-table-column label="现有库存" width="130">
              <template #default="{ row }">{{ formatQty(row.onHandQty) }}</template>
            </el-table-column>
            <el-table-column label="已预留" width="120">
              <template #default="{ row }">{{ formatQty(row.reservedQty) }}</template>
            </el-table-column>
            <el-table-column label="可用库存" width="130">
              <template #default="{ row }">{{ formatQty(row.availableQty) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="!isMaterialEnabled(row.materialId)"
                    @click="openOperationDialog('MANUAL_IN', row.materialId)"
                  >
                    入库
                  </el-button>
                  <el-button
                    size="small"
                    :disabled="!isMaterialEnabled(row.materialId)"
                    @click="openOperationDialog('MANUAL_OUT', row.materialId)"
                  >
                    出库
                  </el-button>
                  <el-button
                    size="small"
                    :disabled="!isMaterialEnabled(row.materialId)"
                    @click="openOperationDialog('ADJUST', row.materialId)"
                  >
                    调整
                  </el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无库存余额，首次入库后会生成余额记录" />
            </template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="库存流水" name="transactions">
        <el-card shadow="never">
          <div class="toolbar">
            <el-select
              v-model="transactionFilters.materialId"
              clearable
              filterable
              placeholder="选择物料"
              style="width: 240px"
            >
              <el-option
                v-for="item in materials"
                :key="item.id"
                :label="`${item.materialCode} ${item.materialName}`"
                :value="item.id"
              />
            </el-select>
            <el-select
              v-model="transactionFilters.transactionType"
              clearable
              placeholder="流水类型"
              style="width: 160px"
            >
              <el-option label="手工入库" value="MANUAL_IN" />
              <el-option label="手工出库" value="MANUAL_OUT" />
              <el-option label="库存调增" value="ADJUST_IN" />
              <el-option label="库存调减" value="ADJUST_OUT" />
            </el-select>
            <el-button :icon="Search" @click="loadTransactions">查询</el-button>
          </div>

          <el-table :data="transactions" v-loading="transactionLoading" border>
            <el-table-column label="物料" min-width="190">
              <template #default="{ row }">
                {{ materialLabel(row.materialId) }}
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="inventoryTransactionTagType(row.transactionType)">
                  {{ inventoryTransactionTypeLabel(row.transactionType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="110">
              <template #default="{ row }">{{ formatQty(row.qty) }}</template>
            </el-table-column>
            <el-table-column label="现有库存变化" min-width="170">
              <template #default="{ row }">
                {{ formatQty(row.beforeOnHandQty) }} -> {{ formatQty(row.afterOnHandQty) }}
              </template>
            </el-table-column>
            <el-table-column label="预留变化" min-width="150">
              <template #default="{ row }">
                {{ formatQty(row.beforeReservedQty) }} -> {{ formatQty(row.afterReservedQty) }}
              </template>
            </el-table-column>
            <el-table-column prop="operatorId" label="操作人" width="100" />
            <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column prop="occurredAt" label="发生时间" min-width="170" />
            <template #empty>
              <el-empty description="暂无库存流水" />
            </template>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="materialDialogVisible"
      :title="editingMaterial ? '编辑物料' : '新增物料'"
      width="560px"
    >
      <el-form :model="materialForm" label-width="90px">
        <el-form-item label="物料编码" required>
          <el-input v-model="materialForm.materialCode" maxlength="100" />
        </el-form-item>
        <el-form-item label="物料名称" required>
          <el-input v-model="materialForm.materialName" maxlength="200" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="materialForm.spec" maxlength="500" />
        </el-form-item>
        <el-form-item label="单位" required>
          <el-input v-model="materialForm.unit" maxlength="50" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="materialForm.category" maxlength="100" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="materialForm.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="materialForm.remark" type="textarea" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMaterial">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="operationDialogVisible"
      :title="operationTitle"
      width="520px"
    >
      <el-form :model="operationForm" label-width="90px">
        <el-form-item label="物料" required>
          <el-select
            v-model="operationForm.materialId"
            filterable
            placeholder="选择启用物料"
            style="width: 100%"
          >
            <el-option
              v-for="item in enabledMaterials"
              :key="item.id"
              :label="`${item.materialCode} ${item.materialName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="operationType === 'ADJUST'" label="方向" required>
          <el-segmented
            v-model="operationForm.direction"
            :options="[
              { label: '调增', value: 'IN' },
              { label: '调减', value: 'OUT' },
            ]"
          />
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number
            v-model="operationForm.qty"
            :min="0"
            :precision="4"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="operationForm.reason" maxlength="200" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="operationForm.remark" type="textarea" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="operationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitOperation">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Refresh, Search } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";

import {
  adjustStock,
  createMaterial,
  disableMaterial,
  enableMaterial,
  listMaterials,
  listStocks,
  listTransactions,
  manualIn,
  manualOut,
  updateMaterial,
} from "../api/inventoryApi";
import type {
  InventoryAdjustmentDirection,
  InventoryStock,
  InventoryTransaction,
  InventoryTransactionType,
  MaterialItem,
  MaterialItemPayload,
} from "../types";
import {
  canOperateStock,
  inventoryTransactionTagType,
  inventoryTransactionTypeLabel,
  isPositiveQuantity,
} from "../utils/inventoryRules";

type InventoryTab = "materials" | "stocks" | "transactions";
type OperationType = "MANUAL_IN" | "MANUAL_OUT" | "ADJUST";

const activeTab = ref<InventoryTab>("materials");
const materialLoading = ref(false);
const stockLoading = ref(false);
const transactionLoading = ref(false);
const materials = ref<MaterialItem[]>([]);
const stocks = ref<InventoryStock[]>([]);
const transactions = ref<InventoryTransaction[]>([]);

const materialFilters = reactive<{
  keyword: string;
  enabled?: boolean;
}>({
  keyword: "",
  enabled: undefined,
});
const stockFilters = reactive({
  keyword: "",
});
const transactionFilters = reactive<{
  materialId?: number;
  transactionType?: InventoryTransactionType;
}>({
  materialId: undefined,
  transactionType: undefined,
});

const materialDialogVisible = ref(false);
const editingMaterial = ref<MaterialItem | null>(null);
const materialForm = reactive<MaterialItemPayload>({
  materialCode: "",
  materialName: "",
  spec: "",
  unit: "",
  category: "",
  enabled: true,
  remark: "",
});

const operationDialogVisible = ref(false);
const operationType = ref<OperationType>("MANUAL_IN");
const operationForm = reactive<{
  materialId?: number;
  direction: InventoryAdjustmentDirection;
  qty: number;
  reason: string;
  remark: string;
}>({
  materialId: undefined,
  direction: "IN",
  qty: 1,
  reason: "",
  remark: "",
});

const enabledMaterials = computed(() =>
  materials.value.filter((item) => item.enabled),
);
const materialById = computed(() =>
  Object.fromEntries(materials.value.map((item) => [item.id, item])),
);
const operationTitle = computed(() => {
  if (operationType.value === "MANUAL_OUT") {
    return "手工出库";
  }
  if (operationType.value === "ADJUST") {
    return "库存调整";
  }
  return "手工入库";
});

onMounted(async () => {
  await loadMaterials();
  await loadStocks();
  await loadTransactions();
});

async function refreshActiveTab() {
  if (activeTab.value === "materials") {
    await loadMaterials();
  } else if (activeTab.value === "stocks") {
    await loadStocks();
  } else {
    await loadTransactions();
  }
}

async function loadMaterials() {
  materialLoading.value = true;
  try {
    const page = await listMaterials({
      keyword: materialFilters.keyword || undefined,
      enabled: materialFilters.enabled,
      page: 1,
      pageSize: 200,
    });
    materials.value = page.items;
  } finally {
    materialLoading.value = false;
  }
}

async function loadStocks() {
  stockLoading.value = true;
  try {
    const page = await listStocks({
      keyword: stockFilters.keyword || undefined,
      page: 1,
      pageSize: 200,
    });
    stocks.value = page.items;
  } finally {
    stockLoading.value = false;
  }
}

async function loadTransactions() {
  transactionLoading.value = true;
  try {
    const page = await listTransactions({
      materialId: transactionFilters.materialId,
      transactionType: transactionFilters.transactionType,
      page: 1,
      pageSize: 200,
    });
    transactions.value = page.items;
  } finally {
    transactionLoading.value = false;
  }
}

function openMaterialDialog(material?: MaterialItem) {
  editingMaterial.value = material ?? null;
  materialForm.materialCode = material?.materialCode ?? "";
  materialForm.materialName = material?.materialName ?? "";
  materialForm.spec = material?.spec ?? "";
  materialForm.unit = material?.unit ?? "";
  materialForm.category = material?.category ?? "";
  materialForm.enabled = material?.enabled ?? true;
  materialForm.remark = material?.remark ?? "";
  materialDialogVisible.value = true;
}

async function submitMaterial() {
  if (!materialForm.materialCode.trim()) {
    ElMessage.warning("请填写物料编码");
    return;
  }
  if (!materialForm.materialName.trim()) {
    ElMessage.warning("请填写物料名称");
    return;
  }
  if (!materialForm.unit.trim()) {
    ElMessage.warning("请填写单位");
    return;
  }

  const payload = normalizeMaterialPayload();
  if (editingMaterial.value) {
    await updateMaterial(editingMaterial.value.id, payload);
    ElMessage.success("物料已更新");
  } else {
    await createMaterial(payload);
    ElMessage.success("物料已创建");
  }
  materialDialogVisible.value = false;
  await loadMaterials();
  await loadStocks();
}

function normalizeMaterialPayload(): MaterialItemPayload {
  return {
    materialCode: materialForm.materialCode.trim(),
    materialName: materialForm.materialName.trim(),
    spec: materialForm.spec?.trim() || null,
    unit: materialForm.unit.trim(),
    category: materialForm.category?.trim() || null,
    enabled: materialForm.enabled,
    remark: materialForm.remark?.trim() || null,
  };
}

async function toggleMaterial(material: MaterialItem) {
  if (material.enabled) {
    await ElMessageBox.confirm(
      "停用后该物料不能继续发生新的入库、出库或调整操作，历史余额和流水仍可查询。",
      "停用物料",
      {
        confirmButtonText: "确认停用",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
    await disableMaterial(material.id);
    ElMessage.success("物料已停用");
  } else {
    await enableMaterial(material.id);
    ElMessage.success("物料已启用");
  }
  await loadMaterials();
}

function openOperationDialog(type: OperationType, materialId?: number) {
  operationType.value = type;
  operationForm.materialId = materialId;
  operationForm.direction = "IN";
  operationForm.qty = 1;
  operationForm.reason = "";
  operationForm.remark = "";
  operationDialogVisible.value = true;
}

async function submitOperation() {
  if (!operationForm.materialId) {
    ElMessage.warning("请选择物料");
    return;
  }
  if (!isMaterialEnabled(operationForm.materialId)) {
    ElMessage.warning("停用物料不能发生新的库存操作");
    return;
  }
  if (!isPositiveQuantity(operationForm.qty)) {
    ElMessage.warning("数量必须大于 0");
    return;
  }

  const payload = {
    materialId: operationForm.materialId,
    qty: operationForm.qty,
    reason: operationForm.reason.trim() || null,
    remark: operationForm.remark.trim() || null,
  };

  if (operationType.value === "MANUAL_IN") {
    await manualIn(payload);
    ElMessage.success("入库已记录");
  } else if (operationType.value === "MANUAL_OUT") {
    await manualOut(payload);
    ElMessage.success("出库已记录");
  } else {
    await adjustStock({
      materialId: operationForm.materialId,
      adjustmentQty: operationForm.qty,
      direction: operationForm.direction,
      reason: operationForm.reason.trim() || null,
      remark: operationForm.remark.trim() || null,
    });
    ElMessage.success("库存调整已记录");
  }

  operationDialogVisible.value = false;
  await loadStocks();
  await loadTransactions();
}

function isMaterialEnabled(materialId: number) {
  return canOperateStock(materialById.value[materialId]?.enabled ?? true);
}

function materialLabel(materialId: number) {
  const material = materialById.value[materialId];
  if (!material) {
    return `物料 ${materialId}`;
  }
  return `${material.materialCode} ${material.materialName}`;
}

function formatQty(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return "0";
  }
  return Number(value).toLocaleString("zh-CN", {
    maximumFractionDigits: 4,
  });
}
</script>

<style scoped>
.boundary-alert {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
