# Work Order Material Generation Design

## 1. Overview

This change designs how `process_step_material_requirement_template` records are converted into `production_work_order_material` rows for a DRAFT production work order.

The goal is to let standard process templates provide the default material demand for work orders while keeping users in control through preview and explicit apply actions. The generated demand remains a demand list only. It does not reserve stock, deduct stock, calculate shortage, or block production dispatch.

## 2. Current State

The current system already has:

- `process_route_template`
- `process_step_template`
- `process_step_material_requirement_template`
- `production_work_order`
- `production_work_order_material`
- Inventory material core with material items, stock balances, and transactions
- Work-order-driven dispatch from RELEASED work orders

The current system does not yet have:

- Automatic generation from process step material templates to work order materials
- Work order material demand preview from a selected route template
- Inventory matching between `production_work_order_material` and `inventory_stock`

## 3. Generation Timing

Two timing options are considered.

### Option A: Generate during DRAFT work order creation

If `processRouteTemplateId` is selected while creating a work order, the system could immediately generate material demand from the route template.

Tradeoff: this is efficient when the route template is known early, but current work order creation may not always select a route template. It also risks overwriting or surprising manual edits if route selection changes later.

### Option B: Generate manually from DRAFT work order detail

In the DRAFT work order detail or edit flow, the user selects a process route template, previews generated material demand, then explicitly applies it.

MVP recommendation: use Option B.

Reasons:

- Current work order creation does not always have a finalized process route template.
- Users can confirm the route template before generation.
- Preview before apply avoids silent overwrites.
- It keeps existing manual material editing behavior clear.
- It is easier to require explicit confirmation when replacing existing DRAFT material demand.

Future changes may add Option A after route-template selection becomes part of creation.

## 4. Generation Input

Generation input:

- `workOrderId`
- `routeTemplateId`
- `workOrder.quantitySnapshot`
- `replaceExisting`
- `previewOnly`

Validation rules:

- The work order must exist in the same tenant.
- The work order must be `DRAFT` to apply generated demand.
- `RELEASED`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED` work orders cannot replace material demand.
- Preview may be available for DRAFT preparation, but it still must not write data.
- The route template must exist in the same tenant.
- The route template must be enabled.
- Only enabled and non-deleted step material templates are used.
- Empty generation result is reported with `WORK_ORDER_MATERIAL_GENERATION_EMPTY`.

## 5. Quantity Calculation

The generated quantity uses the work order snapshot quantity:

```text
base = baseQtyPerUnit * workOrder.quantitySnapshot + fixedQty
requiredQty = base * (1 + lossRate)
```

Rules:

- Null `baseQtyPerUnit` is treated as `0`.
- Null `fixedQty` is treated as `0`.
- Null `lossRate` is treated as `0`.
- `requiredQty` must be greater than `0`.
- Use `BigDecimal` in future implementation to avoid floating-point error.
- MVP does not execute `required_qty_expression`.
- If `required_qty_expression` exists, MVP should not evaluate it. The future implementation may include it in warnings or remarks as an unsupported expression, but it must not silently calculate from it.
- Complex expression evaluation is out of scope for MVP.

## 6. Mapping Rules

Mapping from `process_step_material_requirement_template` to `production_work_order_material`:

- `material_id` -> `material_id`
- `material_code` -> `material_code`
- `material_name` -> `material_name`
- `spec` -> `spec`
- `unit` -> `unit`
- Calculated `requiredQty` -> `required_qty`
- `usage_stage` -> `usage_stage`
- `step_template_id` -> `related_step_template_id`
- `related_step_instance_id` = null
- `remark` -> `remark`

The generated work order material line remains a demand record. It does not imply inventory availability, reservation, deduction, or readiness.

If `production_work_order_material` lacks required relation fields such as `related_step_template_id` or `related_step_instance_id`, the implementation phase must inspect the actual schema and may propose a migration. This OpenSpec does not create migrations, and manual database changes are forbidden.

## 7. Existing Work Order Materials Handling

MVP strategy:

- Preview generated demand without writing.
- Apply generated demand only when `replaceExisting=true`.
- Applying with `replaceExisting=true` replaces current material demand for the DRAFT work order.
- The admin UI must ask for explicit confirmation before replacement.
- Non-DRAFT work orders cannot replace material demand.

Future options:

- Merge mode.
- Merge by `material_id`.
- Merge by `material_name` and `spec`.
- Preserve manual lines while adding generated lines.
- Track `source_type = TEMPLATE / MANUAL`.

These future options are out of MVP scope.

## 8. API Draft

Future API draft only. This change does not implement APIs.

```http
GET /api/production/work-orders/{workOrderId}/material-generation/preview?routeTemplateId=1
```

Behavior:

- Loads DRAFT work order context.
- Loads enabled step material templates from the selected route template.
- Calculates generated demand.
- Returns generated material list, count, and warnings.
- Does not write `production_work_order_material`.
- Does not query inventory.

```http
POST /api/production/work-orders/{workOrderId}/materials/generate-from-template
```

Request:

```json
{
  "routeTemplateId": 1,
  "replaceExisting": true
}
```

Response draft:

- Generated material list.
- Generated count.
- Replacement count.
- Warnings.

Behavior:

- Writes only `production_work_order_material`.
- Only allowed for DRAFT work orders.
- Does not query `inventory_stock`.
- Does not write `inventory_transaction`.

## 9. Admin-Web Draft

Add an action in production work order detail or DRAFT editing:

Button: `从工艺模板生成物料需求`

Flow:

1. Select process route template.
2. Click preview.
3. Show generated material demand:
   - Step name or step order
   - `usageStage`
   - `materialName`
   - `spec`
   - `unit`
   - `requiredQty`
   - Quantity rule summary
4. User confirms apply.
5. If existing work order materials exist, show a confirmation that applying will replace current DRAFT material demand.

Required UI copy:

`生成物料需求只代表需求清单，不代表库存已预留、已扣减或已齐套。`

The UI must not show stock readiness, shortage, purchase, supplier, or finance state in this change.

## 10. Inventory Boundary

This change does not:

- Query `inventory_stock`.
- Calculate `availableQty`.
- Calculate `shortageQty`.
- Write `inventory_transaction`.
- Reserve stock.
- Deduct stock.
- Change `inventory_stock`.

Future `work-order-material-readiness` owns:

- `required_qty` vs `available_qty`.
- `shortage_qty`.
- `readiness_status`.
- Displaying shortage by `related_step_template_id` or `usage_stage`.

## 11. Dispatch Boundary

This change does not modify production dispatch.

- It does not create `production_route_instance`.
- It does not create `production_step_instance`.
- It does not change work order status.
- It does not block dispatch because of material shortage.
- It does not change order item production write-back.

Mapping `related_step_instance_id` after dispatch may be designed in a future readiness or dispatch integration change.

## 12. Process Graph Boundary

Current generation is based on linear `process_step_template`.

Future graph model:

- Material demand should map to executable `TASK` leaf nodes.
- `GROUP` nodes should not directly consume material.
- Step-start shortage guards should apply to executable `TASK` nodes.

This change does not implement `GROUP`, `TASK`, parallel execution, or nested process graphs.

## 13. Error Handling Draft

Future error codes:

- `WORK_ORDER_NOT_FOUND`
- `WORK_ORDER_NOT_DRAFT`
- `PROCESS_ROUTE_TEMPLATE_NOT_FOUND`
- `PROCESS_ROUTE_TEMPLATE_DISABLED`
- `STEP_MATERIAL_TEMPLATE_NOT_FOUND`
- `WORK_ORDER_MATERIAL_GENERATION_EMPTY`
- `WORK_ORDER_MATERIAL_QUANTITY_INVALID`
- `WORK_ORDER_MATERIAL_REPLACE_REJECTED`

## 14. Out Of Scope

This change does not implement backend code, frontend code, migrations, Controllers, Services, Mappers, Entities, inventory checks, inventory reservation, inventory deduction, inventory transactions, shortage calculation, readiness status, step shortage display, step-start blocking, purchase, supplier, finance, automatic production consume, production dispatch changes, order core mutation, CRM, public pool, contribution, process graph `GROUP` / `TASK`, parallel or nested execution, worker-uniapp, production-h5, or screen-web.
