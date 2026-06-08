<template>
  <el-dialog
    :model-value="visible"
    :title="step ? `工序物料需求 - ${step.stepName}` : '工序物料需求'"
    width="1080px"
    @close="close"
  >
    <el-alert
      title="这是模板物料需求，只代表标准需求来源，不代表库存已预留、已扣减或已齐套。"
      type="info"
      :closable="false"
      show-icon
      class="material-warning"
    />

    <el-alert
      title="未关联库存物料时，创建工单只能生成需求，无法核对库存。"
      type="warning"
      :closable="false"
      show-icon
      class="material-warning"
    />

    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="addMaterial">
        添加物料
      </el-button>
    </div>

    <el-table :data="materialRows" v-loading="loading" border>
      <el-table-column label="选择库存物料" min-width="180">
        <template #default="{ row }">
          <el-select
            v-model="row.materialId"
            clearable
            filterable
            placeholder="可手填"
            :loading="inventoryMaterialLoading"
            @change="selectInventoryMaterial(row)"
          >
            <el-option
              v-for="item in inventoryMaterials"
              :key="item.id"
              :label="`${item.materialCode} / ${item.materialName}`"
              :value="item.id"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="物料名称" min-width="150">
        <template #default="{ row }">
          <el-input v-model.trim="row.materialName" maxlength="128" />
        </template>
      </el-table-column>
      <el-table-column label="编码" min-width="120">
        <template #default="{ row }">
          <el-input v-model.trim="row.materialCode" maxlength="64" />
        </template>
      </el-table-column>
      <el-table-column label="规格" min-width="130">
        <template #default="{ row }">
          <el-input v-model.trim="row.spec" maxlength="255" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="110">
        <template #default="{ row }">
          <el-input v-model.trim="row.unit" maxlength="32" />
        </template>
      </el-table-column>
      <el-table-column label="单位用量" width="140">
        <template #default="{ row }">
          <el-input-number
            v-model="row.baseQtyPerUnit"
            :min="0"
            :precision="4"
            :step="1"
            controls-position="right"
          />
        </template>
      </el-table-column>
      <el-table-column label="固定用量" width="140">
        <template #default="{ row }">
          <el-input-number
            v-model="row.fixedQty"
            :min="0"
            :precision="4"
            :step="1"
            controls-position="right"
          />
        </template>
      </el-table-column>
      <el-table-column label="损耗率" width="130">
        <template #default="{ row }">
          <el-input-number
            v-model="row.lossRate"
            :min="0"
            :precision="4"
            :step="0.01"
            controls-position="right"
          />
        </template>
      </el-table-column>
      <el-table-column label="使用阶段" min-width="130">
        <template #default="{ row }">
          <el-input v-model.trim="row.usageStage" maxlength="128" />
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="150">
        <template #default="{ row }">
          <el-input v-model.trim="row.remark" maxlength="500" />
        </template>
      </el-table-column>
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ $index }">
          <el-button size="small" type="danger" @click="removeMaterial($index)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Plus } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { ref, watch } from "vue";

import { listMaterials } from "../../inventory/api/inventoryApi";
import type { MaterialItem } from "../../inventory/types";
import type {
  StepMaterialRequirementPayload,
  StepMaterialRequirementTemplate,
  StepTemplate,
} from "../types";
import {
  hasInvalidNonNegativeNumber,
  hasValidQuantityRule,
} from "../utils/stepMaterialRequirementRules";

interface EditableMaterial extends StepMaterialRequirementPayload {
  rowKey: number;
  materialName: string;
  unit: string;
  materialCode: string;
  spec: string;
  usageStage: string;
  remark: string;
  baseQtyPerUnit: number | null;
  fixedQty: number | null;
  lossRate: number | null;
  enabled: boolean;
}

const props = defineProps<{
  visible: boolean;
  step: StepTemplate | null;
  materials: StepMaterialRequirementTemplate[];
  loading?: boolean;
  saving?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  save: [materials: StepMaterialRequirementPayload[]];
}>();

const materialRows = ref<EditableMaterial[]>([]);
const inventoryMaterials = ref<MaterialItem[]>([]);
const inventoryMaterialLoading = ref(false);
let nextRowKey = 1;

watch(
  () => [props.visible, props.materials] as const,
  () => {
    if (!props.visible) {
      return;
    }
    materialRows.value = props.materials.map(toEditableMaterial);
    void loadInventoryMaterials();
  },
  { immediate: true },
);

async function loadInventoryMaterials() {
  inventoryMaterialLoading.value = true;
  try {
    const result = await listMaterials({ enabled: true, pageSize: 100 });
    inventoryMaterials.value = result.items;
  } finally {
    inventoryMaterialLoading.value = false;
  }
}

function toEditableMaterial(
  material: StepMaterialRequirementTemplate,
): EditableMaterial {
  return {
    rowKey: nextRowKey++,
    materialId: material.materialId,
    materialCode: material.materialCode ?? "",
    materialName: material.materialName,
    spec: material.spec ?? "",
    unit: material.unit,
    baseQtyPerUnit:
      material.baseQtyPerUnit == null ? null : Number(material.baseQtyPerUnit),
    fixedQty: material.fixedQty == null ? null : Number(material.fixedQty),
    lossRate: material.lossRate == null ? null : Number(material.lossRate),
    requiredQtyExpression: material.requiredQtyExpression ?? "",
    usageStage: material.usageStage ?? "",
    remark: material.remark ?? "",
    enabled: material.enabled,
  };
}

function addMaterial() {
  materialRows.value.push({
    rowKey: nextRowKey++,
    materialId: null,
    materialCode: "",
    materialName: "",
    spec: "",
    unit: "",
    baseQtyPerUnit: null,
    fixedQty: null,
    lossRate: null,
    requiredQtyExpression: "",
    usageStage: "",
    remark: "",
    enabled: true,
  });
}

function removeMaterial(index: number) {
  materialRows.value.splice(index, 1);
}

function selectInventoryMaterial(row: EditableMaterial) {
  if (!row.materialId) {
    return;
  }
  const material = inventoryMaterials.value.find((item) => item.id === row.materialId);
  if (!material) {
    return;
  }
  row.materialCode = material.materialCode;
  row.materialName = material.materialName;
  row.spec = material.spec ?? "";
  row.unit = material.unit;
}

function close() {
  emit("close");
}

function submit() {
  for (const row of materialRows.value) {
    if (!row.materialName.trim()) {
      ElMessage.warning("请填写物料名称");
      return;
    }
    if (!row.unit.trim()) {
      ElMessage.warning("请填写物料单位");
      return;
    }
    if (
      hasInvalidNonNegativeNumber(row.baseQtyPerUnit) ||
      hasInvalidNonNegativeNumber(row.fixedQty) ||
      hasInvalidNonNegativeNumber(row.lossRate)
    ) {
      ElMessage.warning("用量和损耗率不能为负数");
      return;
    }
    if (!hasValidQuantityRule(row)) {
      ElMessage.warning("请至少填写单位用量或固定用量");
      return;
    }
  }

  emit(
    "save",
    materialRows.value.map((row) => ({
      materialId: row.materialId ?? null,
      materialCode: row.materialCode.trim(),
      materialName: row.materialName.trim(),
      spec: row.spec.trim(),
      unit: row.unit.trim(),
      baseQtyPerUnit: row.baseQtyPerUnit,
      fixedQty: row.fixedQty,
      lossRate: row.lossRate,
      requiredQtyExpression: row.requiredQtyExpression?.trim(),
      usageStage: row.usageStage.trim(),
      remark: row.remark.trim(),
      enabled: row.enabled,
    })),
  );
}
</script>

<style scoped>
.material-warning {
  margin-bottom: 12px;
}
</style>
