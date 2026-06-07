# Production Work Order Design

## Overview

This change defines the production work order layer between customer-line `order_item` and production execution.

It is OpenSpec-only. It must not create backend code, frontend code, database migrations, Controller APIs, pages, inventory deduction, stock in/out, file upload, photo upload, worker apps, CRM, contribution, or order core logic in this change.

The new production chain is:

```text
order_item
-> production_work_order
-> production_route_instance
-> production_step_instance
-> production_step_checkin
```

`production_work_order` is the production department's internal instruction sheet. It is not a customer order and must not own customer-line commercial data.

## Production Work Order Flow

1. Customer/order line creates an order and one or more `order_item` records.
2. Production manager opens the production work order creation flow for one `order_item`.
3. Production reads the order item through a production-owned read contract such as `OrderItemReadPort`.
4. Production creates a `production_work_order` as a production-owned document.
5. The work order captures production instruction fields, technical configuration fields, schedule requirements, responsible people, and confirmation fields.
6. Production manager records material requirements under the work order.
7. This change stops at the material requirement draft. It does not reserve stock, deduct stock, create stock transactions, purchase materials, or call suppliers.
8. Later production dispatch should create or link a `production_route_instance` from the work order.
9. Existing step execution and future check-in evidence continue to operate on `production_step_instance` records.
10. Future inventory/material-readiness will evaluate work order material requirements and mark shortages by step or material use stage.

Confirmed future main flow:

```text
order_item
-> production_work_order
-> production_route_instance
-> production_step_instance
```

New production implementation should move toward this flow. This OpenSpec does not refactor the already defined `production-dispatch-instance` change. During the transition, the existing `order_item -> production_route_instance` ability may remain available until the work order-driven dispatch flow is implemented and adopted.

## Data Flow

```text
order_item
  read-only production contract
  |
  v
production_work_order
  production instruction and internal planning document
  |
  v
production_work_order_material
  material requirement draft only
  |
  v
production_route_instance
  optional link, created by dispatch after or during work order release
  |
  v
production_step_instance
  executable production tasks
```

Material readiness is a later flow:

```text
production_work_order_material
-> inventory/material-readiness check
-> shortages by material and future step usage
-> optional reservation
-> later stock deduction or issue transaction
```

This change defines only the first two records in that flow.

## Work Order Status Machine

Suggested `production_work_order.status` values:

```text
DRAFT
RELEASED
IN_PROGRESS
COMPLETED
CANCELLED
```

Allowed MVP transitions:

```text
DRAFT -> RELEASED
RELEASED -> IN_PROGRESS
IN_PROGRESS -> COMPLETED
DRAFT -> CANCELLED
RELEASED -> CANCELLED
```

Rules:

- `DRAFT` means the work order is being prepared and is not yet released to production.
- `RELEASED` means the production instruction is confirmed, issued, and available for production preparation in MVP. If the business later needs separate `CONFIRMED` and `ISSUED` states, that requires a later design change.
- `IN_PROGRESS` means production execution has started, usually through linked route/step instances.
- `COMPLETED` means the production work order is finished.
- `CANCELLED` means the production work order is no longer active.
- `COMPLETED` and `CANCELLED` are terminal for MVP.
- Active statuses are `DRAFT`, `RELEASED`, and `IN_PROGRESS`.
- Non-active statuses are `COMPLETED` and `CANCELLED`.
- `IN_PROGRESS -> CANCELLED` is not allowed in MVP.
- `COMPLETED -> CANCELLED` is not allowed in MVP.
- `COMPLETED -> DRAFT` is not allowed in MVP.
- `RELEASED -> DRAFT` is not allowed in MVP.
- Reopen, pause, close-and-reissue, and approval workflow are out of scope.

Suggested errors for future implementation:

```text
WORK_ORDER_NOT_FOUND
WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM
WORK_ORDER_INVALID_STATUS_TRANSITION
WORK_ORDER_CANCELLED
WORK_ORDER_COMPLETED
```

## Relation To `order_item`

`order_item` is owned by the customer/order line.

Production may read:

```text
order_item.id
order_item.order_id
order_item.item_name
order_item.product_type
order_item.quantity
order_item.status
order_item.production_status
order_item.production_progress
order_item.production_route_instance_id
```

Production must not modify:

```text
customer data
customer contact data
order amount
quotation data
product specification
product quantity
order custom fields
customer source
order core lifecycle status
```

The work order should store production-side snapshots needed for execution display, such as:

```text
order_item_name_snapshot
product_type_snapshot
quantity_snapshot
```

These snapshots are for production instruction context only. They must not become the order-line source of truth.

Suggested uniqueness:

```text
tenant_id + order_item_id + delete_marker
```

The MVP rule is one active work order for one `tenant_id + order_item_id`.

Active statuses:

```text
DRAFT
RELEASED
IN_PROGRESS
```

Non-active statuses:

```text
COMPLETED
CANCELLED
```

If the same `order_item` already has an active work order, duplicate creation must return `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM`. MVP must not automatically reuse the existing work order. Future split work orders or explicit reuse flows require a separate design.

## Relation To `production_route_instance`

`production_work_order` may link to one `production_route_instance`:

```text
production_work_order.production_route_instance_id nullable
production_route_instance.production_work_order_id optional future field
```

MVP relationship rule:

- A work order can exist before dispatch.
- New production main flow should create the work order first, then dispatch a route instance from the work order.
- A route instance can be created after the work order is released.
- The work order may reference the route instance after dispatch.
- Existing `production-dispatch-instance` behavior still owns route/step snapshot copying and freezing.
- The route instance remains the execution source of truth for step execution.
- This change does not refactor existing direct `order_item -> production_route_instance` dispatch behavior.
- During transition, legacy direct dispatch may continue to exist until work order-driven dispatch replaces it in later implementation.

This change does not redefine dispatch freeze rules.

Recommended future dispatch relation options:

1. Create work order first, then dispatch route for that work order. This is the confirmed future main flow.
2. If a route instance already exists for legacy/demo data, link it back to a work order through a migration or controlled command in a later change.

This OpenSpec does not implement either option.

## Relation To `production_step_instance` And Check-In

`production_step_instance` remains the executable task model for current serial execution.

Rules:

- A work order is an instruction and preparation document.
- A route instance defines the frozen executable process.
- A step instance is the executable unit.
- Check-in evidence attaches to executable step instances, not directly to the work order.
- Future nested/parallel graph work may introduce GROUP and TASK nodes.
- Only TASK leaf nodes should be executable and eligible for check-in in that future model.

This change does not modify `production_step_instance`, `production_step_checkin`, or check-in photo behavior.

## Production Instruction Field Draft

Suggested production instruction fields on `production_work_order`:

```text
id
tenant_id
work_order_no
order_id
order_item_id
production_route_instance_id nullable
order_item_name_snapshot
product_type_snapshot
quantity_snapshot
status
priority
instruction_title
instruction_remark
production_requirement
delivery_requirement
quality_requirement
packaging_requirement
shipping_requirement
planned_start_date
planned_finish_date
required_delivery_date
deadline_remark
responsible_user_id
handler_user_id
production_manager_id
primary_worker_id nullable
created_by
created_at
updated_by
updated_at
deleted
delete_marker
```

Examples from the real production instruction sheet can be represented as production-side instruction fields or extension fields:

```text
equipment_model
machine_color
delivery_address_or_requirement
assembly_person_id
```

Avoid storing customer commercial terms, quotation rules, or order amount in the work order.

## Technical Configuration Field Draft

The first version uses a small number of explicit nullable columns plus a reserved JSON extension for non-standard fields.

Explicit fields should be limited to common, high-frequency values that need filtering, querying, or list display. Do not solidify every field from the production instruction sheet into columns in the first version.

Suggested explicit fields:

```text
equipment_model
technical_config_summary
technical_config_remark
```

Suggested extension field:

```text
technical_config_json
```

Rules:

- Explicit fields cover stable production planning needs only.
- `technical_config_json` is for production-side extra configuration only.
- `technical_config_json` may carry non-standard and frequently changing fields such as:
  - CNC system
  - compensation method
  - cylinder brand
  - motor brand
  - valve group brand
  - oil pump brand
  - mold or blade
  - random accessories
  - machine color
  - shipping requirement
  - other non-standard configuration
- Do not store order amount, quotation, customer contact, CRM source, or contribution data in this JSON field.
- If a field becomes important for querying or reporting, promote it to a typed column in a later migration proposal.

## Signature And Confirmation Field Draft

Suggested fields:

```text
released_by
released_at
confirmed_by
confirmed_at
production_signed_by
production_signed_at
warehouse_confirmed_by
warehouse_confirmed_at
quality_confirmed_by
quality_confirmed_at
customer_acceptance_required
acceptance_remark
```

MVP rules:

- These are internal confirmation records only.
- They are not electronic contract signatures.
- They do not implement approval workflow.
- They do not implement customer acceptance or legal signature.
- They do not upload signature images in this change.

## Material Requirement Table Draft

Suggested child table:

```text
production_work_order_material
```

Purpose:

```text
Record material or component demand for one production work order.
```

Suggested fields:

```text
id
tenant_id
work_order_id
order_id
order_item_id
material_id nullable
material_code nullable
material_name
spec
unit
required_qty
usage_stage nullable
related_step_template_id nullable
related_step_instance_id nullable
requirement_status
remark
created_by
created_at
updated_by
updated_at
deleted
delete_marker
```

Suggested `requirement_status` values:

```text
DRAFT
CONFIRMED
```

Optional future statuses, not implemented by this change:

```text
READY
SHORTAGE
RESERVED
ISSUED
```

MVP rule:

```text
production_work_order_material is a requirement list only.
```

It must not:

- deduct stock
- reserve stock
- create inventory transactions
- create purchase requests
- create supplier records
- create financial records

## Boundary With Inventory / Material Readiness

This change prepares the input for inventory readiness but does not implement inventory behavior.

Future `inventory/material-readiness` should own:

- checking available quantity
- comparing required quantity with available and reserved quantity
- generating shortage state
- showing shortage on the step or stage that needs the material
- optional reservation
- preventing or warning before starting a shortage-related step
- later supervisor override, if approved
- stock issue and deduction

Important rule:

```text
Shortage must not block the whole work order by default.
```

Reason:

- Not all materials are used at the first step.
- Upstream steps can continue when their own materials are ready.
- Warehouse can prepare downstream materials while early production steps run.

The work order material draft should include `usage_stage`, `related_step_template_id`, or `related_step_instance_id` so future readiness can attach shortages to the right process node.

## Boundary With Nested / Parallel Process Graph

This change does not implement non-linear process execution.

Future `production-route-graph` should own:

- GROUP nodes
- TASK leaf nodes
- parent-child step structure
- step dependency graph
- parallel branches
- multi-predecessor dependencies
- group progress aggregation
- rule that only TASK leaf nodes can start, complete, or check in

This change may include only references that help future mapping:

```text
usage_stage
related_step_template_id
related_step_instance_id
```

It must not change current serial `step_order` execution rules.

## Boundary With Photo Check-In

This change does not implement photo upload, file upload, or check-in UI.

Existing and future check-in behavior remains:

```text
production_step_instance
-> production_step_checkin
-> shared file binding
```

Work order fields can provide production instructions that workers read later, but completion evidence must be attached to executable step instances or future TASK leaf nodes.

## API Shape Draft

All APIs use the shared response envelope:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Create work order from an order item:

```http
POST /api/production/work-orders/from-order-item
```

Request draft:

```json
{
  "orderItemId": 1001,
  "workOrderNo": null,
  "plannedStartDate": "2026-06-08",
  "plannedFinishDate": "2026-06-18",
  "requiredDeliveryDate": "2026-06-20",
  "responsibleUserId": 12,
  "handlerUserId": 13,
  "productionRequirement": "按图纸和确认稿生产",
  "technicalConfig": {
    "equipmentModel": "ZC-100",
    "cncSystem": "标准数控系统",
    "motorBrand": "示例电机品牌"
  },
  "materials": [
    {
      "materialId": 301,
      "materialName": "镀锌板",
      "spec": "1.2mm",
      "unit": "张",
      "requiredQty": 5,
      "usageStage": "下料"
    }
  ]
}
```

Response draft:

```json
{
  "id": 5001,
  "workOrderNo": "WO-20260607-0001",
  "orderId": 501,
  "orderItemId": 1001,
  "status": "DRAFT"
}
```

Get work order detail:

```http
GET /api/production/work-orders/{workOrderId}
```

Release work order:

```http
POST /api/production/work-orders/{workOrderId}/release
```

Link route instance after dispatch:

```http
POST /api/production/work-orders/{workOrderId}/link-route-instance
```

Request draft:

```json
{
  "productionRouteInstanceId": 3001
}
```

Query by order item:

```http
GET /api/production/order-items/{orderItemId}/work-order
```

These APIs are drafts only. This OpenSpec change does not implement them.

## Work Order Number Draft

Confirmed work order number format:

```text
WO-{yyyyMMdd}-{dailySequence}
```

Example:

```text
WO-20260607-0001
```

Rules:

- Work order number is production-owned.
- It must be unique inside one tenant.
- It does not depend on `order_id` or `order_item_id` as the unique number.
- The daily sequence increments by `tenant_id + date`.
- It should be generated by the backend in future implementation when omitted by the request.
- Future implementation must handle concurrent creation conflicts and retry number generation safely.
- Manual override, if allowed later, must still enforce tenant uniqueness.
- The number is not the customer order number and must not replace `order_no`.

Suggested uniqueness:

```text
tenant_id + work_order_no
```

## Data Table Drafts

No migration is created in this OpenSpec change. These are schema drafts only.

### `production_work_order`

Planned fields:

```text
id
tenant_id
work_order_no
order_id
order_item_id
production_route_instance_id
order_item_name_snapshot
product_type_snapshot
quantity_snapshot
status
priority
instruction_title
instruction_remark
production_requirement
quality_requirement
packaging_requirement
shipping_requirement
delivery_requirement
planned_start_date
planned_finish_date
required_delivery_date
deadline_remark
equipment_model
technical_config_summary
technical_config_remark
technical_config_json
responsible_user_id
handler_user_id
production_manager_id
primary_worker_id
released_by
released_at
confirmed_by
confirmed_at
production_signed_by
production_signed_at
warehouse_confirmed_by
warehouse_confirmed_at
quality_confirmed_by
quality_confirmed_at
customer_acceptance_required
acceptance_remark
created_by
created_at
updated_by
updated_at
deleted
delete_marker
```

Suggested indexes:

```text
tenant_id + work_order_no
tenant_id + order_item_id + delete_marker
tenant_id + status + deleted
tenant_id + production_route_instance_id
```

### `production_work_order_material`

Planned fields:

```text
id
tenant_id
work_order_id
order_id
order_item_id
material_id
material_code
material_name
spec
unit
required_qty
usage_stage
related_step_template_id
related_step_instance_id
requirement_status
remark
created_by
created_at
updated_by
updated_at
deleted
delete_marker
```

Suggested indexes:

```text
tenant_id + work_order_id + deleted
tenant_id + material_id + deleted
tenant_id + order_item_id + deleted
tenant_id + related_step_instance_id + deleted
```

## Exception Scenarios

### `ORDER_ITEM_NOT_FOUND`

Return when the order item cannot be read through the production-owned order item read contract.

### `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM`

Return when an active work order already exists for the `order_item`.

### `WORK_ORDER_NOT_FOUND`

Return when the work order does not exist or is deleted.

### `WORK_ORDER_INVALID_STATUS_TRANSITION`

Return when a status transition is not allowed by the MVP state machine.

### `WORK_ORDER_CANCELLED`

Return when an operation attempts to release, start, link, or complete a cancelled work order.

### `WORK_ORDER_COMPLETED`

Return when an operation attempts to mutate a completed work order outside approved future correction flows.

### `WORK_ORDER_ROUTE_LINK_CONFLICT`

Return when a work order is linked to a route instance that belongs to another order item or tenant.

### `MATERIAL_REQUIREMENT_INVALID`

Return when a material requirement has invalid quantity, missing name, unsupported unit, or inconsistent work order reference.

## Out Of Scope

This change does not implement:

- backend business code
- database migration
- Controller APIs
- admin-web pages
- production-h5
- worker-uniapp
- screen-web
- inventory deduction
- inventory reservation
- stock in/out
- stock transaction
- purchase
- supplier
- finance
- costing
- CRM
- customer public pool
- contribution
- order creation
- order amount or quotation modification
- customer field modification
- order item specification or quantity modification
- file upload
- photo upload
- shared file infrastructure
- production step check-in UI
- nested process graph
- parallel process graph
- non-linear execution implementation
- attendance
- dashboard
