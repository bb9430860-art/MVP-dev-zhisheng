<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">生产工单</h1>
        <p class="page-description">
          从订单项创建生产指令工单，发布后进入生产准备。物料需求不代表库存已预留或已扣减。
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadWorkOrders">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">
          从订单项创建工单
        </el-button>
      </div>
    </div>

    <el-alert
      title="物料需求只是需求清单，不代表库存齐套、已预留或已扣减。库存齐套与缺料节点由后续库存齐套模块处理。"
      type="info"
      show-icon
      :closable="false"
      class="boundary-alert"
    />

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="filters.status" clearable placeholder="状态" style="width: 150px">
          <el-option
            v-for="item in workOrderStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="filters.workOrderNo"
          clearable
          placeholder="工单编号"
          style="width: 180px"
        />
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="产品/点位关键字"
          style="width: 220px"
        />
        <el-date-picker
          v-model="plannedRange"
          type="daterange"
          range-separator="至"
          start-placeholder="计划开始"
          end-placeholder="计划结束"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
        <el-select
          v-model="filters.routeLinked"
          clearable
          placeholder="生产实例"
          style="width: 150px"
        >
          <el-option label="已关联" :value="true" />
          <el-option label="未关联" :value="false" />
        </el-select>
        <el-button type="primary" @click="loadWorkOrders">查询</el-button>
      </div>

      <el-table :data="workOrders" border v-loading="loading">
        <el-table-column prop="workOrderNo" label="工单编号" width="170" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="workOrderStatusTagType(row.status)">
              {{ formatWorkOrderStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单" min-width="150">
          <template #default="{ row }">
            <div>{{ row.orderNo ?? `订单 ${row.orderId}` }}</div>
            <small class="muted">{{ row.orderType ?? "-" }} / {{ row.customerType ?? "-" }}</small>
          </template>
        </el-table-column>
        <el-table-column label="产品/点位" min-width="220">
          <template #default="{ row }">
            <div>{{ row.orderItemNameSnapshot }}</div>
            <small class="muted">
              {{ formatProductionProductType(row.productTypeSnapshot) }} · 数量 {{ row.quantitySnapshot ?? "-" }}
            </small>
          </template>
        </el-table-column>
        <el-table-column prop="plannedStartDate" label="计划开始" width="120" />
        <el-table-column prop="requiredDeliveryDate" label="要求交付" width="120" />
        <el-table-column label="生产实例" width="130">
          <template #default="{ row }">
            <el-tag :type="row.routeLinked ? 'success' : 'info'">
              {{ row.routeLinked ? `已关联 ${row.productionRouteInstanceId}` : "未关联" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="openDetail(row.id)">详情</el-button>
              <el-button
                size="small"
                :disabled="!canEditWorkOrder(row.status)"
                @click="openEdit(row.id)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                type="primary"
                :disabled="!canReleaseWorkOrder(row.status)"
                @click="releaseRow(row)"
              >
                发布
              </el-button>
              <el-button
                size="small"
                type="success"
                :disabled="!canDispatchWorkOrder(row.status, row.routeLinked)"
                @click="openDispatchDrawer(row)"
              >
                下发生产
              </el-button>
              <el-button
                size="small"
                type="danger"
                :disabled="!canCancelWorkOrder(row.status)"
                @click="cancelRow(row)"
              >
                取消
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="candidateDialogVisible" title="选择订单项" width="960px">
      <div class="toolbar">
        <el-input
          v-model="candidateKeyword"
          clearable
          placeholder="订单号 / 产品 / 规格 / 备注"
          style="width: 280px"
        />
        <el-button type="primary" @click="loadCandidates">查询</el-button>
      </div>
      <el-table :data="candidates" border v-loading="candidateLoading" height="360">
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column prop="orderType" label="订单类型" width="110" />
        <el-table-column prop="customerType" label="客户类型" width="110" />
        <el-table-column prop="dealOwnerName" label="成交人" width="120" />
        <el-table-column label="产品/点位" min-width="220">
          <template #default="{ row }">
            <div>{{ row.itemName }}</div>
            <small class="muted">{{ row.spec ?? "-" }} · {{ row.unit ?? "-" }} · {{ row.quantity ?? "-" }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column prop="productionStatus" label="生产状态" width="130" />
        <el-table-column label="工单" width="150">
          <template #default="{ row }">
            <el-tag :type="row.hasActiveWorkOrder ? 'warning' : 'success'">
              {{ row.hasActiveWorkOrder ? row.activeWorkOrderNo : "可创建" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :disabled="row.hasActiveWorkOrder"
              @click="selectCandidate(row)"
            >
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-drawer v-model="formDrawerVisible" :title="formTitle" size="720px">
      <el-form label-width="110px" :model="form">
        <el-alert
          v-if="selectedCandidate"
          :title="`${selectedCandidate.orderNo ?? `订单 ${selectedCandidate.orderId}`} / ${selectedCandidate.itemName}`"
          type="info"
          :closable="false"
          class="drawer-alert"
        />

        <template v-if="selectedCandidate && !editingWorkOrderId">
          <el-divider>物料需求与库存核对</el-divider>
          <el-alert
            title="库存核对仅用于提示，不代表已预留、已扣减或已齐套。缺料不会阻止创建工单。"
            type="warning"
            show-icon
            :closable="false"
            class="drawer-alert"
          />
          <div class="toolbar">
            <el-select
              v-model="createRouteTemplateId"
              filterable
              clearable
              placeholder="选择工艺路线模板"
              style="width: 340px"
              @change="createReadinessPreview = null"
            >
              <el-option
                v-for="item in createRouteOptions"
                :key="item.id"
                :label="`${item.routeName}（${formatProductionProductType(item.productType)}，${item.stepCount}道工序）`"
                :value="item.id"
              />
            </el-select>
            <el-button
              type="primary"
              plain
              :loading="createReadinessLoading"
              :disabled="!canPreviewCreateReadiness(selectedCandidate.orderItemId, createRouteTemplateId)"
              @click="previewCreateReadiness"
            >
              生成并核对物料需求
            </el-button>
          </div>
          <template v-if="createReadinessPreview">
            <el-table
              v-for="step in createReadinessPreview.itemsByStep"
              :key="step.stepTemplateId ?? step.stepName ?? step.stepOrder"
              :data="step.materials"
              border
              class="readiness-step-table"
              :row-class-name="readinessRowClassName"
            >
              <el-table-column
                :label="`${step.stepOrder ?? '-'} / ${step.stepName ?? '-'}`"
                min-width="170"
              >
                <template #default="{ row }">
                  <div>{{ row.materialName }}</div>
                  <small class="muted">{{ row.materialCode ?? "-" }} / {{ row.usageStage ?? "-" }}</small>
                </template>
              </el-table-column>
              <el-table-column prop="spec" label="规格" width="110" />
              <el-table-column prop="unit" label="单位" width="80" />
              <el-table-column prop="requiredQty" label="需求数量" width="110" />
              <el-table-column prop="availableQty" label="可用库存" width="110" />
              <el-table-column prop="shortageQty" label="缺料数量" width="110" />
              <el-table-column label="状态" width="150">
                <template #default="{ row }">
                  <el-tag :type="readinessTagType(row.readinessStatus)">
                    {{ row.readinessStatus }}
                  </el-tag>
                  <div class="muted">{{ row.readinessMessage }}</div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </template>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" clearable>
                <el-option label="普通" value="NORMAL" />
                <el-option label="高" value="HIGH" />
                <el-option label="紧急" value="URGENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号">
              <el-input v-model="form.equipmentModel" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="指令标题">
          <el-input v-model="form.instructionTitle" />
        </el-form-item>
        <el-form-item label="生产要求">
          <el-input v-model="form.productionRequirement" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="质量要求">
          <el-input v-model="form.qualityRequirement" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="包装要求">
          <el-input v-model="form.packagingRequirement" />
        </el-form-item>
        <el-form-item label="发货要求">
          <el-input v-model="form.shippingRequirement" />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="计划开始">
              <el-date-picker v-model="form.plannedStartDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划完成">
              <el-date-picker v-model="form.plannedFinishDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="要求交付">
              <el-date-picker v-model="form.requiredDeliveryDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="负责人 ID">
              <el-input-number v-model="form.responsibleUserId" :min="1" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="经办人 ID">
              <el-input-number v-model="form.handlerUserId" :min="1" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产经理 ID">
              <el-input-number v-model="form.productionManagerId" :min="1" :controls="false" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="技术摘要">
          <el-input v-model="form.technicalConfigSummary" />
        </el-form-item>
        <el-form-item label="技术 JSON">
          <el-input v-model="form.technicalConfigJson" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <el-collapse v-model="manualMaterialCollapse" class="manual-material-section">
        <el-collapse-item name="manual">
          <template #title>
            <span class="manual-material-title">手动补充物料（可选）</span>
          </template>
          <el-alert
            title="手动补充物料只用于添加工艺模板未覆盖的临时需求。库存核对结果以上方‘物料需求与库存核对’为准。"
            type="info"
            show-icon
            :closable="false"
            class="drawer-alert"
          />
          <el-table :data="materialForm" border>
            <el-table-column label="物料名称" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.materialName" />
              </template>
            </el-table-column>
            <el-table-column label="规格" width="130">
              <template #default="{ row }">
                <el-input v-model="row.spec" />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="90">
              <template #default="{ row }">
                <el-input v-model="row.unit" />
              </template>
            </el-table-column>
            <el-table-column label="需求数量" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.requiredQty" :min="0" :controls="false" />
              </template>
            </el-table-column>
            <el-table-column label="使用阶段" width="130">
              <template #default="{ row }">
                <el-input v-model="row.usageStage" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }">
                <el-button size="small" type="danger" @click="removeMaterial($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="manual-material-actions">
            <el-button @click="addMaterial">新增物料</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
      <div class="footer-actions">
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </div>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="工单详情" size="720px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ detail.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="workOrderStatusTagType(detail.status)">
              {{ formatWorkOrderStatus(detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detail.orderNo ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ detail.orderType ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="产品/点位">{{ detail.orderItemNameSnapshot }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ detail.quantitySnapshot ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="生产实例">
            {{ detail.productionRouteInstanceId ?? "未关联" }}
          </el-descriptions-item>
          <el-descriptions-item label="要求交付">{{ detail.requiredDeliveryDate ?? "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>生产指令</el-divider>
        <p>{{ detail.productionRequirement || "-" }}</p>

        <el-divider>物料需求</el-divider>
        <div class="section-actions">
          <el-button
            v-if="canGenerateWorkOrderMaterials(detail.status)"
            size="small"
            type="primary"
            @click="openMaterialGenerationDrawer(detail)"
          >
            从工艺模板生成物料需求
          </el-button>
        </div>
        <el-table :data="detail.materials" border>
          <el-table-column prop="relatedStepTemplateId" label="来源工序" width="110" />
          <el-table-column prop="materialName" label="物料" />
          <el-table-column prop="spec" label="规格" width="130" />
          <el-table-column prop="unit" label="单位" width="90" />
          <el-table-column prop="requiredQty" label="需求数量" width="120" />
          <el-table-column prop="availableQtySnapshot" label="可用库存" width="110" />
          <el-table-column prop="shortageQty" label="缺料数量" width="110" />
          <el-table-column label="核对状态" width="150">
            <template #default="{ row }">
              <el-tag v-if="row.readinessStatus" :type="readinessTagType(row.readinessStatus)">
                {{ row.readinessStatus }}
              </el-tag>
              <span v-else>-</span>
              <div class="muted">{{ row.readinessMessage ?? "" }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="usageStage" label="使用阶段" width="130" />
        </el-table>
      </template>
    </el-drawer>

    <el-drawer v-model="dispatchDrawerVisible" title="下发生产" size="860px">
      <template v-if="dispatchTarget && dispatchContext">
        <el-alert
          title="确认下发后会生成 frozen=true 的生产实例，工单进入生产中；本流程不做库存齐套、预留或扣减。"
          type="warning"
          show-icon
          :closable="false"
          class="drawer-alert"
        />

        <el-descriptions :column="2" border class="drawer-alert">
          <el-descriptions-item label="工单编号">
            {{ dispatchContext.workOrder.workOrderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ formatWorkOrderStatus(dispatchContext.workOrder.status) }}
          </el-descriptions-item>
          <el-descriptions-item label="订单号">
            {{ dispatchContext.workOrder.orderNo ?? "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="产品/点位">
            {{ dispatchContext.workOrder.orderItemNameSnapshot }}
          </el-descriptions-item>
          <el-descriptions-item label="产品类型">
            {{ formatProductionProductType(dispatchContext.workOrder.productTypeSnapshot) }}
          </el-descriptions-item>
          <el-descriptions-item label="数量">
            {{ dispatchContext.workOrder.quantitySnapshot ?? "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="订单项生产状态">
            {{ dispatchContext.orderItem.productionStatus }}
          </el-descriptions-item>
          <el-descriptions-item label="生产实例">
            {{ dispatchContext.workOrder.productionRouteInstanceId ?? "未关联" }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="toolbar">
          <el-select
            v-model="dispatchRouteTemplateId"
            filterable
            clearable
            placeholder="选择工艺路线模板"
            style="width: 340px"
            :disabled="dispatching"
          >
            <el-option
              v-for="item in dispatchRouteOptions"
              :key="item.id"
              :label="`${item.routeName}（${formatProductionProductType(item.productType)}，${item.stepCount}道工序）`"
              :value="item.id"
            />
          </el-select>
          <el-button
            type="primary"
            plain
            :disabled="!dispatchRouteTemplateId || dispatching"
            @click="loadDispatchTemplateConfig"
          >
            从模板生成工序
          </el-button>
          <el-input
            v-model="dispatchRouteName"
            placeholder="下发路线名称"
            style="width: 260px"
            :disabled="dispatching"
          />
        </div>

        <el-table :data="dispatchSteps" border v-loading="dispatchLoading">
          <el-table-column prop="stepOrder" label="顺序" width="80">
            <template #default="{ row }">
              <el-input-number
                v-model="row.stepOrder"
                :min="1"
                :controls="false"
                style="width: 64px"
                :disabled="dispatching"
              />
            </template>
          </el-table-column>
          <el-table-column label="工序名称" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.stepName" :disabled="dispatching" />
            </template>
          </el-table-column>
          <el-table-column label="执行角色" width="150">
            <template #default="{ row }">
              <el-select v-model="row.assignedRole" :disabled="dispatching">
                <el-option
                  v-for="item in productionRoleOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="负责人 ID" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.assignedUserId"
                :min="1"
                :controls="false"
                style="width: 90px"
                :disabled="dispatching"
              />
            </template>
          </el-table-column>
          <el-table-column label="拍照" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.photoRequired" :disabled="dispatching" />
            </template>
          </el-table-column>
          <el-table-column label="备注" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.remarkRequired" :disabled="dispatching" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row, $index }">
              <div class="table-actions">
                <el-button
                  size="small"
                  :disabled="dispatching || $index === 0"
                  @click="moveDispatchStep($index, -1)"
                >
                  上移
                </el-button>
                <el-button
                  size="small"
                  :disabled="dispatching || $index === dispatchSteps.length - 1"
                  @click="moveDispatchStep($index, 1)"
                >
                  下移
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  :disabled="dispatching"
                  @click="removeDispatchStep(row.clientStepId)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <div class="empty-state">请选择模板生成工序。</div>
          </template>
        </el-table>

        <div class="footer-actions">
          <el-button :disabled="dispatching" @click="addDispatchStep">新增工序</el-button>
          <el-button @click="dispatchDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="dispatching" @click="confirmWorkOrderDispatch">
            确认下发生产
          </el-button>
        </div>
      </template>
    </el-drawer>
    <el-drawer v-model="materialGenerationDrawerVisible" title="从工艺模板生成物料需求" size="820px">
      <template v-if="materialGenerationTarget">
        <el-alert
          title="生成物料需求只代表需求清单，不代表库存已预留、已扣减或已齐套。"
          type="warning"
          show-icon
          :closable="false"
          class="drawer-alert"
        />

        <el-descriptions :column="2" border class="drawer-alert">
          <el-descriptions-item label="工单编号">
            {{ materialGenerationTarget.workOrderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ formatWorkOrderStatus(materialGenerationTarget.status) }}
          </el-descriptions-item>
          <el-descriptions-item label="产品/点位">
            {{ materialGenerationTarget.orderItemNameSnapshot }}
          </el-descriptions-item>
          <el-descriptions-item label="数量">
            {{ materialGenerationTarget.quantitySnapshot ?? "-" }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="toolbar">
          <el-select
            v-model="materialGenerationRouteTemplateId"
            filterable
            clearable
            placeholder="选择工艺路线模板"
            style="width: 340px"
            :disabled="materialGenerationLoading || materialGenerationApplying"
          >
            <el-option
              v-for="item in materialGenerationRouteOptions"
              :key="item.id"
              :label="`${item.routeName}（${formatProductionProductType(item.productType)}，${item.stepCount}道工序）`"
              :value="item.id"
            />
          </el-select>
          <el-button
            type="primary"
            plain
            :loading="materialGenerationLoading"
            :disabled="!materialGenerationRouteTemplateId || materialGenerationApplying"
            @click="previewMaterialGeneration"
          >
            预览生成
          </el-button>
        </div>

        <el-table
          :data="materialGenerationPreview?.generatedMaterials ?? []"
          border
          v-loading="materialGenerationLoading"
        >
          <el-table-column prop="stepOrder" label="工序顺序" width="90" />
          <el-table-column prop="stepName" label="工序" min-width="140" />
          <el-table-column prop="usageStage" label="使用阶段" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="160" />
          <el-table-column prop="spec" label="规格" width="120" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="requiredQty" label="需求数量" width="120" />
          <el-table-column prop="quantityRuleSummary" label="计算规则" min-width="220" show-overflow-tooltip />
          <el-table-column prop="warning" label="提示" min-width="180" show-overflow-tooltip />
          <template #empty>
            <el-empty description="请选择模板并预览生成结果" />
          </template>
        </el-table>

        <el-alert
          v-if="materialGenerationPreview?.warnings.length"
          :title="materialGenerationPreview.warnings.join('；')"
          type="info"
          show-icon
          :closable="false"
          class="drawer-alert"
        />

        <div class="footer-actions">
          <el-button @click="materialGenerationDrawerVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="materialGenerationApplying"
            :disabled="!materialGenerationPreview?.generatedCount"
            @click="applyMaterialGeneration"
          >
            应用到工单
          </el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { Plus, Refresh } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";

import {
  cancelWorkOrder,
  createWorkOrderWithMaterialReadiness,
  createWorkOrderDispatchConfigFromTemplate,
  createWorkOrderFromOrderItem,
  dispatchWorkOrder,
  generateWorkOrderMaterialsFromTemplate,
  getWorkOrderDispatchContext,
  getWorkOrder,
  listWorkOrderCandidates,
  listWorkOrders,
  previewCreateWorkOrderMaterialReadiness,
  previewWorkOrderMaterialGeneration,
  releaseWorkOrder,
  updateWorkOrder,
  updateWorkOrderMaterials,
} from "../api/productionWorkOrderApi";
import { listRouteTemplateOptions } from "../api/productionDispatchApi";
import { productionRoleOptions } from "../constants";
import type {
  DispatchStepConfig,
  ProductionWorkOrderStatus,
  RouteTemplateOption,
  WorkOrder,
  WorkOrderBasePayload,
  WorkOrderCandidate,
  WorkOrderDispatchContext,
  WorkOrderMaterialGenerationResult,
  WorkOrderMaterialReadinessItem,
  WorkOrderMaterialPayload,
  WorkOrderMaterialReadinessResult,
} from "../types";
import { formatProductionProductType } from "../utils/displayLabels";
import { normalizeDispatchStepOrders } from "../utils/dispatchConfigRules";
import {
  canGenerateWorkOrderMaterials,
  materialGenerationNeedsReplacementConfirm,
} from "../utils/materialGenerationRules";
import {
  canPreviewCreateReadiness,
  isReadinessWarning,
  readinessTagType,
} from "../utils/materialReadinessRules";
import {
  canCancelWorkOrder,
  canDispatchWorkOrder,
  canEditWorkOrder,
  canReleaseWorkOrder,
  workOrderStatusTagType,
} from "../utils/workOrderRules";

const workOrderStatusOptions = [
  { label: "草稿", value: "DRAFT" },
  { label: "已发布", value: "RELEASED" },
  { label: "生产中", value: "IN_PROGRESS" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" },
] as const;

const loading = ref(false);
const saving = ref(false);
const workOrders = ref<WorkOrder[]>([]);
const candidates = ref<WorkOrderCandidate[]>([]);
const detail = ref<WorkOrder | null>(null);
const selectedCandidate = ref<WorkOrderCandidate | null>(null);
const editingWorkOrderId = ref<number | null>(null);
const dispatchTarget = ref<WorkOrder | null>(null);
const dispatchContext = ref<WorkOrderDispatchContext | null>(null);
const dispatchRouteOptions = ref<RouteTemplateOption[]>([]);
const dispatchRouteTemplateId = ref<number | null>(null);
const dispatchRouteName = ref("");
const dispatchSteps = ref<DispatchStepConfig[]>([]);
const materialGenerationTarget = ref<WorkOrder | null>(null);
const materialGenerationRouteOptions = ref<RouteTemplateOption[]>([]);
const materialGenerationRouteTemplateId = ref<number | null>(null);
const materialGenerationPreview = ref<WorkOrderMaterialGenerationResult | null>(null);
const createRouteOptions = ref<RouteTemplateOption[]>([]);
const createRouteTemplateId = ref<number | null>(null);
const createReadinessPreview = ref<WorkOrderMaterialReadinessResult | null>(null);
const candidateDialogVisible = ref(false);
const formDrawerVisible = ref(false);
const detailDrawerVisible = ref(false);
const dispatchDrawerVisible = ref(false);
const materialGenerationDrawerVisible = ref(false);
const candidateLoading = ref(false);
const dispatchLoading = ref(false);
const dispatching = ref(false);
const materialGenerationLoading = ref(false);
const materialGenerationApplying = ref(false);
const createReadinessLoading = ref(false);
const manualMaterialCollapse = ref<string[]>([]);
const candidateKeyword = ref("");
const plannedRange = ref<[string, string] | null>(null);

const filters = reactive<{
  status?: ProductionWorkOrderStatus;
  workOrderNo: string;
  keyword: string;
  routeLinked?: boolean;
}>({
  status: undefined,
  workOrderNo: "",
  keyword: "",
  routeLinked: undefined,
});

const form = reactive<WorkOrderBasePayload>({
  priority: "NORMAL",
  instructionTitle: "",
  productionRequirement: "",
  qualityRequirement: "",
  packagingRequirement: "",
  shippingRequirement: "",
  deliveryRequirement: "",
  plannedStartDate: null,
  plannedFinishDate: null,
  requiredDeliveryDate: null,
  equipmentModel: "",
  technicalConfigSummary: "",
  technicalConfigJson: "",
  responsibleUserId: null,
  handlerUserId: null,
  productionManagerId: null,
  customerAcceptanceRequired: false,
});

const materialForm = ref<WorkOrderMaterialPayload[]>([]);
const formTitle = computed(() =>
  editingWorkOrderId.value ? "编辑 DRAFT 工单" : "创建 DRAFT 工单",
);

onMounted(loadWorkOrders);

async function loadWorkOrders() {
  loading.value = true;
  try {
    const result = await listWorkOrders({
      status: filters.status,
      workOrderNo: filters.workOrderNo || undefined,
      keyword: filters.keyword || undefined,
      routeLinked: filters.routeLinked,
      plannedStartFrom: plannedRange.value?.[0],
      plannedStartTo: plannedRange.value?.[1],
      pageSize: 50,
    });
    workOrders.value = result.items;
  } finally {
    loading.value = false;
  }
}

async function openCreateDialog() {
  candidateDialogVisible.value = true;
  await loadCandidates();
}

async function loadCandidates() {
  candidateLoading.value = true;
  try {
    const result = await listWorkOrderCandidates({
      keyword: candidateKeyword.value || undefined,
      pageSize: 50,
    });
    candidates.value = result.items;
  } finally {
    candidateLoading.value = false;
  }
}

async function selectCandidate(row: WorkOrderCandidate) {
  if (row.hasActiveWorkOrder) {
    ElMessage.warning(`该订单项已有 active 工单：${row.activeWorkOrderNo ?? row.activeWorkOrderId}`);
    return;
  }
  selectedCandidate.value = row;
  editingWorkOrderId.value = null;
  createRouteTemplateId.value = null;
  createReadinessPreview.value = null;
  createRouteOptions.value = [];
  resetForm();
  materialForm.value = [];
  manualMaterialCollapse.value = [];
  candidateDialogVisible.value = false;
  formDrawerVisible.value = true;
  createReadinessLoading.value = true;
  try {
    createRouteOptions.value = await listRouteTemplateOptions(row.productType);
  } finally {
    createReadinessLoading.value = false;
  }
}

async function openEdit(workOrderId: number) {
  const item = await getWorkOrder(workOrderId);
  if (!canEditWorkOrder(item.status)) {
    ElMessage.warning("只有 DRAFT 工单可以编辑");
    return;
  }
  editingWorkOrderId.value = item.id;
  selectedCandidate.value = null;
  Object.assign(form, pickBasePayload(item));
  materialForm.value = item.materials.map((material) => ({ ...material }));
  manualMaterialCollapse.value = ["manual"];
  formDrawerVisible.value = true;
}

async function openDetail(workOrderId: number) {
  detail.value = await getWorkOrder(workOrderId);
  detailDrawerVisible.value = true;
}

async function openMaterialGenerationDrawer(workOrder: WorkOrder) {
  if (!canGenerateWorkOrderMaterials(workOrder.status)) {
    ElMessage.warning("只有 DRAFT 工单可以从工艺模板生成物料需求");
    return;
  }
  materialGenerationTarget.value = workOrder;
  materialGenerationRouteTemplateId.value = null;
  materialGenerationPreview.value = null;
  materialGenerationDrawerVisible.value = true;
  materialGenerationLoading.value = true;
  try {
    materialGenerationRouteOptions.value = await listRouteTemplateOptions(
      workOrder.productTypeSnapshot,
    );
  } finally {
    materialGenerationLoading.value = false;
  }
}

async function previewMaterialGeneration() {
  if (!materialGenerationTarget.value || !materialGenerationRouteTemplateId.value) {
    ElMessage.warning("请选择工艺路线模板");
    return;
  }
  materialGenerationLoading.value = true;
  try {
    materialGenerationPreview.value = await previewWorkOrderMaterialGeneration(
      materialGenerationTarget.value.id,
      materialGenerationRouteTemplateId.value,
    );
  } finally {
    materialGenerationLoading.value = false;
  }
}

async function previewCreateReadiness() {
  if (!selectedCandidate.value || !createRouteTemplateId.value) {
    ElMessage.warning("请选择工艺路线模板");
    return;
  }
  createReadinessLoading.value = true;
  try {
    createReadinessPreview.value = await previewCreateWorkOrderMaterialReadiness(
      selectedCandidate.value.orderItemId,
      createRouteTemplateId.value,
    );
  } finally {
    createReadinessLoading.value = false;
  }
}

async function applyMaterialGeneration() {
  if (
    !materialGenerationTarget.value ||
    !materialGenerationRouteTemplateId.value ||
    !materialGenerationPreview.value?.generatedCount
  ) {
    ElMessage.warning("请先预览生成结果");
    return;
  }
  if (materialGenerationNeedsReplacementConfirm(materialGenerationTarget.value.materials.length)) {
    await ElMessageBox.confirm(
      "将替换当前 DRAFT 工单物料需求，是否继续？",
      "应用生成物料需求",
      {
        confirmButtonText: "确认替换",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  }
  materialGenerationApplying.value = true;
  try {
    await generateWorkOrderMaterialsFromTemplate(
      materialGenerationTarget.value.id,
      materialGenerationRouteTemplateId.value,
    );
    ElMessage.success("已从工艺模板生成物料需求");
    const updated = await getWorkOrder(materialGenerationTarget.value.id);
    detail.value = updated;
    materialGenerationTarget.value = updated;
    materialGenerationDrawerVisible.value = false;
    await loadWorkOrders();
  } finally {
    materialGenerationApplying.value = false;
  }
}

async function saveForm() {
  const useReadinessCreate = Boolean(
    !editingWorkOrderId.value &&
      selectedCandidate.value &&
      createRouteTemplateId.value &&
      createReadinessPreview.value,
  );
  if (!useReadinessCreate) {
    const invalid = materialForm.value.find(
      (item) => !item.materialName.trim() || Number(item.requiredQty) <= 0,
    );
    if (invalid) {
      ElMessage.warning("物料名称必填，需求数量必须大于 0");
      return;
    }
  }
  saving.value = true;
  try {
    if (editingWorkOrderId.value) {
      await updateWorkOrder(editingWorkOrderId.value, { ...form });
      await updateWorkOrderMaterials(editingWorkOrderId.value, materialForm.value);
      ElMessage.success("工单已更新");
    } else if (selectedCandidate.value && createRouteTemplateId.value && createReadinessPreview.value) {
      await createWorkOrderWithMaterialReadiness({
        orderItemId: selectedCandidate.value.orderItemId,
        routeTemplateId: createRouteTemplateId.value,
        workOrderFields: { ...form },
        applyGeneratedMaterials: true,
      });
      ElMessage.success("DRAFT 工单已创建，物料需求已按工序生成并完成库存核对");
    } else if (selectedCandidate.value) {
      await createWorkOrderFromOrderItem({
        orderItemId: selectedCandidate.value.orderItemId,
        ...form,
        materials: materialForm.value,
      });
      ElMessage.success("DRAFT 工单已创建");
    }
    formDrawerVisible.value = false;
    await loadWorkOrders();
  } catch (error) {
    if (error instanceof Error && error.message === "WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM") {
      ElMessage.error("该订单项已有 active 工单，不能重复创建");
    }
    throw error;
  } finally {
    saving.value = false;
  }
}

async function releaseRow(row: WorkOrder) {
  await ElMessageBox.confirm("发布后生产指令和物料需求默认冻结，确认发布？", "发布工单", {
    confirmButtonText: "发布",
    cancelButtonText: "取消",
    type: "warning",
  });
  await releaseWorkOrder(row.id);
  ElMessage.success("工单已发布");
  await loadWorkOrders();
}

async function cancelRow(row: WorkOrder) {
  await ElMessageBox.confirm("确认取消该工单？", "取消工单", {
    confirmButtonText: "取消工单",
    cancelButtonText: "返回",
    type: "warning",
  });
  await cancelWorkOrder(row.id);
  ElMessage.success("工单已取消");
  await loadWorkOrders();
}

async function openDispatchDrawer(row: WorkOrder) {
  if (!canDispatchWorkOrder(row.status, row.routeLinked)) {
    ElMessage.warning("只有 RELEASED 且未关联生产实例的工单可以下发");
    return;
  }
  dispatchTarget.value = row;
  dispatchContext.value = null;
  dispatchRouteTemplateId.value = null;
  dispatchRouteName.value = "";
  dispatchSteps.value = [];
  dispatchDrawerVisible.value = true;
  dispatchLoading.value = true;
  try {
    const context = await getWorkOrderDispatchContext(row.id);
    dispatchContext.value = context;
    dispatchRouteOptions.value = await listRouteTemplateOptions(
      context.workOrder.productTypeSnapshot,
    );
  } finally {
    dispatchLoading.value = false;
  }
}

async function loadDispatchTemplateConfig() {
  if (!dispatchTarget.value || !dispatchRouteTemplateId.value) {
    return;
  }
  dispatchLoading.value = true;
  try {
    const config = await createWorkOrderDispatchConfigFromTemplate(
      dispatchTarget.value.id,
      dispatchRouteTemplateId.value,
    );
    dispatchRouteName.value = config.routeName;
    dispatchSteps.value = normalizeDispatchStepOrders(config.steps);
    ElMessage.success("已从模板生成下发工序");
  } finally {
    dispatchLoading.value = false;
  }
}

function addDispatchStep() {
  const nextOrder = dispatchSteps.value.length + 1;
  dispatchSteps.value.push({
    clientStepId: `work-order-step-${Date.now()}`,
    sourceStepTemplateId: null,
    stepCode: `CUSTOM-${nextOrder}`,
    stepName: "",
    stepOrder: nextOrder,
    assignedRole: "WORKER",
    assignedUserId: null,
    photoRequired: false,
    remarkRequired: false,
    mobileEnabled: true,
    estimatedHours: null,
    operationInstruction: "",
  });
}

function removeDispatchStep(clientStepId: string) {
  dispatchSteps.value = normalizeDispatchStepOrders(
    dispatchSteps.value.filter((item) => item.clientStepId !== clientStepId),
  );
}

function moveDispatchStep(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= dispatchSteps.value.length) {
    return;
  }
  const next = dispatchSteps.value.slice();
  [next[index], next[target]] = [next[target], next[index]];
  dispatchSteps.value = next.map((step, stepIndex) => ({
    ...step,
    stepOrder: stepIndex + 1,
  }));
}

async function confirmWorkOrderDispatch() {
  if (!dispatchTarget.value || !dispatchRouteTemplateId.value) {
    ElMessage.warning("请选择工艺路线模板");
    return;
  }
  const normalized = normalizeDispatchStepOrders(dispatchSteps.value);
  if (normalized.length === 0) {
    ElMessage.warning("请至少配置一道工序");
    return;
  }
  const invalid = normalized.find(
    (step) => !step.stepName.trim() || !step.assignedRole,
  );
  if (invalid) {
    ElMessage.warning("每道工序都必须填写工序名称和执行角色");
    return;
  }

  await ElMessageBox.confirm(
    "确认后将生成 frozen=true 的生产实例，工单进入生产中，且不能重复下发。",
    "确认下发生产",
    {
      confirmButtonText: "确认下发",
      cancelButtonText: "取消",
      type: "warning",
    },
  );

  dispatching.value = true;
  try {
    await dispatchWorkOrder(dispatchTarget.value.id, {
      routeTemplateId: dispatchRouteTemplateId.value,
      routeName: dispatchRouteName.value || "工单生产路线",
      steps: normalized,
    });
    ElMessage.success("已下发生产");
    dispatchDrawerVisible.value = false;
    await loadWorkOrders();
    if (detail.value?.id === dispatchTarget.value.id) {
      detail.value = await getWorkOrder(dispatchTarget.value.id);
    }
  } catch (error) {
    if (error instanceof Error && error.message === "WORK_ORDER_ALREADY_DISPATCHED") {
      ElMessage.error("该工单已关联生产实例，不能重复下发");
    }
    throw error;
  } finally {
    dispatching.value = false;
  }
}

function addMaterial() {
  materialForm.value.push({
    materialName: "",
    spec: "",
    unit: "",
    requiredQty: 1,
    usageStage: "",
  });
}

function removeMaterial(index: number) {
  materialForm.value.splice(index, 1);
}

function readinessRowClassName({ row }: { row: WorkOrderMaterialReadinessItem }) {
  return isReadinessWarning(row.readinessStatus) ? "readiness-warning-row" : "";
}

function resetForm() {
  Object.assign(form, {
    priority: "NORMAL",
    instructionTitle: "",
    instructionRemark: "",
    productionRequirement: "",
    qualityRequirement: "",
    packagingRequirement: "",
    shippingRequirement: "",
    deliveryRequirement: "",
    plannedStartDate: null,
    plannedFinishDate: null,
    requiredDeliveryDate: null,
    deadlineRemark: "",
    equipmentModel: "",
    technicalConfigSummary: "",
    technicalConfigRemark: "",
    technicalConfigJson: "",
    responsibleUserId: null,
    handlerUserId: null,
    productionManagerId: null,
    primaryWorkerId: null,
    customerAcceptanceRequired: false,
    acceptanceRemark: "",
  });
}

function pickBasePayload(item: WorkOrder): WorkOrderBasePayload {
  return {
    priority: item.priority,
    instructionTitle: item.instructionTitle,
    instructionRemark: item.instructionRemark,
    productionRequirement: item.productionRequirement,
    qualityRequirement: item.qualityRequirement,
    packagingRequirement: item.packagingRequirement,
    shippingRequirement: item.shippingRequirement,
    deliveryRequirement: item.deliveryRequirement,
    plannedStartDate: item.plannedStartDate,
    plannedFinishDate: item.plannedFinishDate,
    requiredDeliveryDate: item.requiredDeliveryDate,
    deadlineRemark: item.deadlineRemark,
    equipmentModel: item.equipmentModel,
    technicalConfigSummary: item.technicalConfigSummary,
    technicalConfigRemark: item.technicalConfigRemark,
    technicalConfigJson: item.technicalConfigJson,
    responsibleUserId: item.responsibleUserId,
    handlerUserId: item.handlerUserId,
    productionManagerId: item.productionManagerId,
    primaryWorkerId: item.primaryWorkerId,
    customerAcceptanceRequired: item.customerAcceptanceRequired,
    acceptanceRemark: item.acceptanceRemark,
  };
}

function formatWorkOrderStatus(status: ProductionWorkOrderStatus) {
  return workOrderStatusOptions.find((item) => item.value === status)?.label ?? status;
}
</script>

<style scoped>
.header-actions,
.toolbar,
.table-actions,
.footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.boundary-alert {
  margin-bottom: 16px;
}

.drawer-alert {
  margin-bottom: 16px;
}

.muted {
  color: #718096;
}

.footer-actions {
  justify-content: flex-end;
  margin-top: 16px;
}

.section-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.manual-material-section {
  margin-top: 16px;
}

.manual-material-title {
  font-weight: 600;
}

.manual-material-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.readiness-step-table {
  margin-bottom: 12px;
}

:deep(.readiness-warning-row) {
  --el-table-tr-bg-color: #fff7ed;
}
</style>
