# Work Order Material Readiness On Create Design

## 1. Overview

本设计在创建工单阶段把工艺模板物料需求、库存余额和工单物料需求串起来，让用户在创建 DRAFT 工单前看到每个工序节点需要什么物料、需要多少、库存是否足够，以及缺多少。

主链路：

order_item
-> select process_route_template
-> process_step_material_requirement_template
-> inventory_stock availability check
-> production_work_order
-> production_work_order_material with readiness snapshot

## 2. Current Gap

当前已有能力：

- 工序模板能配置物料需求。
- 库存能维护物料档案和库存余额。
- 工单能保存物料需求。

当前缺口：

- 创建工单时没有自动展示“节点物料 + 库存足不足”。
- 物料需求生成和库存核对没有在创建 DRAFT 工单阶段形成一个可见的确认流程。
- 缺料信息还不能按工序节点沉淀到后续生产管控链路。

## 3. Create Work Order Flow

新的创建流程：

1. 用户选择 order_item。
2. 用户选择 process_route_template。
3. 系统加载 route template 下的 step templates。
4. 系统加载 step material requirement templates。
5. 系统计算 requiredQty。
6. 对有 material_id 的物料查询 inventory_stock。
7. 系统计算 availableQty / shortageQty / readinessStatus。
8. 系统按工序节点展示预览。
9. 用户确认创建 DRAFT 工单。
10. 系统创建 production_work_order。
11. 系统写入 production_work_order_material。
12. 缺料不会阻止创建。

## 4. Material Link Rule

- process_step_material_requirement_template.material_id 如果存在，则可核对库存。
- material_id 为空时，不用 material_name 猜库存，避免误匹配。
- material_id 为空时 readinessStatus = UNLINKED_MATERIAL。
- admin-web 工艺模板物料需求编辑应优先支持从 material_item 选择物料，并带出 material_code、material_name、spec、unit。
- 手填物料仍可保留，但无法库存核对。

## 5. Quantity Calculation

公式：

requiredQty = (baseQtyPerUnit * quantitySnapshot + fixedQty) * (1 + lossRate)

规则：

- null baseQtyPerUnit = 0。
- null fixedQty = 0。
- null lossRate = 0。
- requiredQty > 0。
- required_qty_expression MVP 不执行。
- 使用 BigDecimal，避免浮点误差。

## 6. Inventory Check Rules

对每条生成物料需求：

if material_id is null:

- readinessStatus = UNLINKED_MATERIAL
- availableQty = null
- shortageQty = null

else if inventory_stock row missing:

- readinessStatus = NO_STOCK_RECORD
- availableQty = 0
- shortageQty = requiredQty

else if availableQty >= requiredQty:

- readinessStatus = READY
- shortageQty = 0

else:

- readinessStatus = SHORTAGE
- shortageQty = requiredQty - availableQty

注意：

- 使用 inventory_stock.available_qty。
- 不改变 inventory_stock。
- 不写 inventory_transaction。
- 不做 reserved_qty 变化。
- 不修改 on_hand_qty。
- 不修改 available_qty。

## 7. Data Persistence Draft

Option A：在 production_work_order_material 上增加核对快照字段：

- available_qty_snapshot
- shortage_qty
- readiness_status
- readiness_checked_at
- readiness_message

Option B：新增 production_work_order_material_readiness 表：

- work_order_material_id
- material_id
- required_qty
- available_qty_snapshot
- shortage_qty
- readiness_status
- checked_at

MVP 建议优先 Option A。production_work_order_material 是需求主表，直接保存核对快照更简单，也便于工单详情和后续节点缺料显示复用。

implementation 阶段必须检查现有表结构。如字段缺失，必须新增 Flyway migration，禁止手动改库。

## 8. Admin-Web Create Draft Flow

创建 DRAFT 工单弹窗需要增加：

- 工艺路线模板选择。
- “生成并核对物料需求”按钮，或选择后自动预览。
- 按工序节点分组展示：
  - stepOrder
  - stepName
  - usageStage
  - materialName
  - materialCode
  - spec
  - unit
  - requiredQty
  - availableQty
  - shortageQty
  - readinessStatus
- 缺料行突出显示。
- UNLINKED_MATERIAL 行提示“未关联库存物料，无法核对”。
- NO_STOCK_RECORD 行提示“无库存记录”。
- 页面文案：

库存核对仅用于提示，不代表已预留、已扣减或已齐套。缺料不会阻止创建工单。

## 9. DRAFT Work Order Detail Flow

DRAFT 工单详情中应支持：

- 重新选择工艺模板并重新生成/核对。
- 手动补充物料。
- 刷新库存核对。
- 仅 DRAFT 可覆盖物料需求。

非 DRAFT：

- 可以查看核对结果。
- 不允许覆盖生成物料需求。
- 后续 readiness refresh 是否允许，需要单独设计。

## 10. Release / Dispatch Boundary

- 缺料不阻止 DRAFT 创建。
- MVP 不阻止 RELEASED。
- MVP 不阻止 production dispatch。
- 后续 step-start shortage guard 才决定到具体节点前是否阻止。

## 11. Inventory Boundary

- 只查询 inventory_stock。
- 不预留库存。
- 不扣库存。
- 不写 inventory_transaction。
- 不修改 reserved_qty。
- 不修改 on_hand_qty。
- 不修改 available_qty。

## 12. Process Graph Boundary

- 当前按 process_step_template 显示。
- 未来 graph 模型下应映射到 TASK 叶子节点。
- GROUP 不直接消耗物料。
- 本 change 不做 GROUP / TASK / 并行 / 嵌套。

## 13. API Draft

### Preview Create Material Readiness

POST /api/production/work-orders/material-readiness/preview-create

Request:

```json
{
  "orderItemId": 1001,
  "routeTemplateId": 5
}
```

Response:

```json
{
  "quantitySnapshot": 1,
  "itemsByStep": [
    {
      "stepTemplateId": 1,
      "stepOrder": 1,
      "stepName": "切割",
      "materials": [
        {
          "materialId": 10,
          "materialCode": "M-001",
          "materialName": "铝板",
          "requiredQty": 2,
          "availableQty": 1,
          "shortageQty": 1,
          "readinessStatus": "SHORTAGE"
        }
      ]
    }
  ],
  "summary": {
    "totalLines": 3,
    "readyLines": 1,
    "shortageLines": 1,
    "unlinkedLines": 1
  }
}
```

### Create With Material Readiness

POST /api/production/work-orders/create-with-material-readiness

Request:

```json
{
  "orderItemId": 1001,
  "routeTemplateId": 5,
  "workOrderFields": {},
  "applyGeneratedMaterials": true
}
```

MVP 建议采用“先预览，再 create-with-material-readiness”的创建流程，因为用户可以在创建前确认缺料提示。也可以保留“先 create DRAFT work order，再 generate readiness”的详情页流程，用于 DRAFT 工单后续重新生成、手动补充和刷新核对。

## 14. Error Handling Draft

- WORK_ORDER_NOT_FOUND
- ORDER_ITEM_NOT_FOUND
- PROCESS_ROUTE_TEMPLATE_NOT_FOUND
- PROCESS_ROUTE_TEMPLATE_DISABLED
- STEP_MATERIAL_TEMPLATE_NOT_FOUND
- WORK_ORDER_MATERIAL_GENERATION_EMPTY
- WORK_ORDER_MATERIAL_QUANTITY_INVALID
- MATERIAL_NOT_FOUND
- INVENTORY_STOCK_NOT_FOUND
- WORK_ORDER_NOT_DRAFT
- MATERIAL_READINESS_PREVIEW_FAILED
