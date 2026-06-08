# Work Order Material Readiness On Create Proposal

## Why

工艺模板已经能定义每个工序节点需要什么物料，库存核心也已经能维护物料档案和库存余额。但创建生产工单时，用户还看不到“这个工单按该工艺路线需要哪些物料，以及库存是否足够”。

工单创建阶段就应该做库存核对，让生产部提前知道缺料。缺料不能阻止整张工单创建，因为缺料可能属于后面的工序节点，生产仍然可以先进入准备或前序工序。缺料应该显示到对应工序节点，为后续节点开工前提醒或阻止做基础。

## Goals

- 创建 DRAFT 工单时支持选择工艺路线模板。
- 选择路线模板后，按工序节点生成物料需求预览。
- 按 quantitySnapshot 计算 requiredQty。
- 对已绑定 material_id 的物料需求查询 inventory_stock.available_qty。
- 计算 shortageQty。
- 给出 readinessStatus：
  - READY
  - SHORTAGE
  - UNLINKED_MATERIAL
  - NO_STOCK_RECORD
- 按 stepTemplateId / stepOrder / stepName / usageStage 展示物料需求。
- 缺料不阻止 DRAFT 工单创建。
- 创建 DRAFT 工单后写入 production_work_order_material。
- 保存或展示库存核对快照，为后续缺料节点显示做基础。
- 保留手动补充物料需求能力。

## Non-Goals

- 不做库存预留。
- 不做库存扣减。
- 不写 inventory_transaction。
- 不做采购。
- 不做供应商。
- 不做财务。
- 不做自动生产扣料。
- 不阻止工单创建。
- 不阻止工单发布。
- 不阻止生产下发。
- 不阻止工序开工。
- 不做 step-start shortage guard。
- 不做 GROUP / TASK / 并行 / 嵌套工艺图。
- 不做 CRM、公海、贡献值、订单核心逻辑。
- 不修改订单金额、报价、客户、规格、数量、单价、小计、订单核心状态。
- 不做 worker-uniapp / production-h5 / screen-web。

## Scope

本 change 只设计：

- 创建工单时的路线模板选择。
- 节点物料需求预览。
- 库存可用量核对。
- 缺料提示。
- DRAFT 工单物料需求写入。
- 与库存预留/扣减的边界。
- 与后续节点开工缺料阻止的边界。
