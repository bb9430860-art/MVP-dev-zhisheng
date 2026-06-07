# Production Work Order API And Admin Design

## Overview

This change designs API and admin-web surfaces for the already implemented production work order backend core.

It is OpenSpec-only. It does not write Java code, Controller code, Mapper/Service/Entity code, migration files, Vue files, TypeScript files, routes, menus, package metadata, or backend core changes.

The designed user flow is:

```text
order_item candidate
-> create DRAFT production_work_order
-> edit DRAFT instruction and material requirements
-> release DRAFT to RELEASED
-> later dispatch/link production_route_instance
```

The future production chain remains:

```text
order_item
-> production_work_order
-> production_route_instance
-> production_step_instance
```

This change does not refactor existing `production-dispatch-instance`.

## Order Contract Integration

Customer line provides an order contract based on `project_order` and `order_item`.

`project_order`:

```text
id
tenant_id
customer_id
source_lead_id
deal_owner_id
deal_amount
deal_status
order_no
order_type
customer_type
deal_at
created_at
updated_at
```

`order_item`:

```text
id
tenant_id
order_id
item_name
spec
unit
quantity
unit_price
subtotal
remark
product_type
production_status
production_progress
production_route_instance_id
production_started_at
completed_at
created_at
updated_at
```

Available or planned customer-line APIs:

```http
GET /api/order/list
GET /api/order/{id}
GET /api/order-items?orderId=xxx
GET /api/order-items/{id}
PUT /api/order-items/{id}
```

Production work order design must treat this contract carefully:

- `project_order + N order_item` is an order summary view and candidate source.
- It is not the production work order model.
- `production_work_order` is not a wrapper for the whole order and all items.
- MVP creates one work order from one `order_item`.
- The production chain remains `order_item -> production_work_order -> production_route_instance -> production_step_instance`.
- Batch work order creation and batch dispatch are future scope.
- Production instruction print/PDF is future scope.

### What Production May Read

The production work order candidate API may read these order-side fields:

```text
order_id
order_no
order_type
customer_type
deal_owner_id or dealOwnerName
order_item.id
item_name
spec
unit
quantity
remark
product_type
production_status
production_progress
production_route_instance_id
```

These fields are read-only context for selecting an `order_item` and creating production-side snapshots.

### What Production Must Not Store Or Show By Default

Commercial and financial fields remain customer/order/finance scope:

```text
deal_amount
unit_price
subtotal
```

Rules:

- They do not enter `production_work_order` MVP.
- They are not work order edit fields.
- Production line must not modify them.
- Admin work order pages should not show them by default.
- If a later boss dashboard needs amount metrics, it should read order-line aggregation instead of copying amounts into `production_work_order`.

### Work Order Snapshot Guidance

`production_work_order` may store production-required snapshots:

```text
order_id
order_item_id
order_no_snapshot optional
item_name_snapshot
spec_snapshot future optional if backend core does not already support it
unit_snapshot future optional
quantity_snapshot
product_type_snapshot
remark_snapshot future optional
```

Existing backend core fields should be reused first. Future optional snapshot fields require a later implementation and migration change if they are not already present.

### What Production May Write Back And Which Change Owns It

Customer line's restricted endpoint:

```http
PUT /api/order-items/{id}
```

allows production write-back only for:

```text
productionStatus
productionProgress
productionRouteInstanceId
```

This write-back belongs to dispatch, execution, and progress synchronization changes. `production-work-order-api-admin` must not call it when creating, editing, releasing, or cancelling a work order.

## API Design Draft

All API shapes are drafts for later implementation and should use the project's shared authentication, tenant, current user, and response conventions.

### List Order Item Candidates

```http
GET /api/production/work-orders/order-items/candidates
```

Purpose:

- Read order items that production may turn into work orders.
- Optionally include order summary context from `project_order`.
- Mark whether an order item already has an active work order.
- Avoid order creation or order core mutation.

Query draft:

```text
keyword
productType
productionStatus
hasActiveWorkOrder
orderNo
orderType
customerType
page
pageSize
```

Response data draft:

```json
{
  "items": [
    {
      "orderItemId": 1001,
      "orderId": 501,
      "orderNo": "ORD-20260607-001",
      "orderType": "PROJECT",
      "customerType": "ENTERPRISE",
      "dealOwnerId": 21,
      "dealOwnerName": "业务员A",
      "itemName": "入口精神堡垒",
      "spec": "3000mm x 1200mm",
      "unit": "套",
      "productType": "SPIRIT_FORTRESS",
      "quantity": 1,
      "remark": "入口主标识",
      "productionStatus": "NOT_DISPATCHED",
      "productionProgress": 0,
      "productionRouteInstanceId": null,
      "hasActiveWorkOrder": false,
      "activeWorkOrderId": null,
      "activeWorkOrderNo": null
    }
  ],
  "total": 1
}
```

Rules:

- Query is production-side read-only.
- The API may aggregate `project_order` summary and `order_item` rows for candidate display.
- The API must not treat one order with many order items as one production work order.
- Existing active work order may be shown as disabled or marked with `hasActiveWorkOrder = true`.
- Active statuses are `DRAFT`, `RELEASED`, and `IN_PROGRESS`.
- Duplicate create must return `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM`.
- `deal_amount`, `unit_price`, and `subtotal` should not be returned by default.

### Create Work Order From Order Item

```http
POST /api/production/work-orders/from-order-item
```

Request draft:

```json
{
  "orderItemId": 1001,
  "priority": "NORMAL",
  "instructionTitle": "入口精神堡垒生产指令",
  "productionRequirement": "按确认图纸生产",
  "qualityRequirement": "出厂前质检",
  "packagingRequirement": "木箱包装",
  "shippingRequirement": "按安装计划发货",
  "deliveryRequirement": "安装前到场",
  "plannedStartDate": "2026-06-08",
  "plannedFinishDate": "2026-06-16",
  "requiredDeliveryDate": "2026-06-18",
  "deadlineRemark": "优先保障安装节点",
  "equipmentModel": "ZC-100",
  "technicalConfigSummary": "标准配置",
  "technicalConfigRemark": "非标配置见 JSON",
  "technicalConfigJson": "{\"cncSystem\":\"standard\"}",
  "responsibleUserId": 11,
  "handlerUserId": 12,
  "productionManagerId": 13,
  "primaryWorkerId": null,
  "customerAcceptanceRequired": false,
  "acceptanceRemark": "内部验收",
  "materials": [
    {
      "materialId": 301,
      "materialCode": "MAT-001",
      "materialName": "镀锌板",
      "spec": "1.2mm",
      "unit": "张",
      "requiredQty": 5.5,
      "usageStage": "下料",
      "relatedStepTemplateId": null,
      "relatedStepInstanceId": null,
      "remark": "下料前备料"
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
  "status": "DRAFT",
  "productionRouteInstanceId": null
}
```

Rules:

- Creates a `DRAFT` work order.
- Reads order item snapshot through production read contract.
- May copy production-required snapshots such as order no, item name, spec, unit, quantity, product type, and remark where supported.
- Does not create a route instance.
- Does not modify order amount, quotation, customer, spec, quantity, or order core status.
- Does not call `PUT /api/order-items/{id}`.
- Does not reserve or deduct inventory.

### List Work Orders

```http
GET /api/production/work-orders
```

Query draft:

```text
status
workOrderNo
orderItemId
keyword
plannedStartFrom
plannedStartTo
requiredDeliveryFrom
requiredDeliveryTo
routeLinked
page
pageSize
```

Response row draft:

```json
{
  "id": 5001,
  "workOrderNo": "WO-20260607-0001",
  "orderId": 501,
  "orderNoSnapshot": "ORD-20260607-001",
  "orderItemId": 1001,
  "orderItemNameSnapshot": "入口精神堡垒",
  "specSnapshot": "future optional",
  "unitSnapshot": "future optional",
  "productTypeSnapshot": "SPIRIT_FORTRESS",
  "quantitySnapshot": 1,
  "remarkSnapshot": "future optional",
  "status": "DRAFT",
  "priority": "NORMAL",
  "plannedStartDate": "2026-06-08",
  "plannedFinishDate": "2026-06-16",
  "requiredDeliveryDate": "2026-06-18",
  "responsibleUserId": 11,
  "productionRouteInstanceId": null,
  "routeLinked": false,
  "updatedAt": "2026-06-07T16:00:00"
}
```

### Get Work Order Detail

```http
GET /api/production/work-orders/{workOrderId}
```

Response includes:

- work order identity
- order references
- order item snapshots
- production instruction fields
- technical configuration fields
- schedule fields
- people fields
- confirmation fields
- material requirement lines
- `production_route_instance_id`
- route link state
- optional order summary display fields if stored as snapshots or loaded read-only from order APIs

### Update Draft Work Order

```http
PUT /api/production/work-orders/{workOrderId}
```

Purpose:

- Update DRAFT work order base information.
- Exclude material list, which uses a dedicated endpoint.

Rules:

- `DRAFT` can edit base instruction, technical, schedule, and person fields.
- `RELEASED`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED` reject base edit in MVP.
- Future correction or approval flow requires a separate change.

### Update Draft Materials

```http
PUT /api/production/work-orders/{workOrderId}/materials
```

Request draft:

```json
{
  "materials": [
    {
      "materialId": 301,
      "materialCode": "MAT-001",
      "materialName": "镀锌板",
      "spec": "1.2mm",
      "unit": "张",
      "requiredQty": 5.5,
      "usageStage": "下料",
      "relatedStepTemplateId": null,
      "relatedStepInstanceId": null,
      "remark": "下料前备料"
    }
  ]
}
```

Rules:

- MVP should replace the draft material list as one explicit save operation, unless later implementation chooses row-level update with the same validation.
- Only `DRAFT` can edit materials.
- `materialName` is required.
- `requiredQty` must be greater than zero.
- No stock reservation, deduction, in/out, transaction, purchase, supplier, or finance behavior is allowed.

### Release Work Order

```http
POST /api/production/work-orders/{workOrderId}/release
```

Rules:

- Allows `DRAFT -> RELEASED`.
- Records release operator and time.
- Does not dispatch.
- Does not create `production_route_instance`.
- Does not reserve or deduct inventory.

### Cancel Work Order

```http
POST /api/production/work-orders/{workOrderId}/cancel
```

Rules:

- Allows `DRAFT -> CANCELLED`.
- Allows `RELEASED -> CANCELLED`.
- Rejects `IN_PROGRESS -> CANCELLED`.
- Rejects `COMPLETED -> CANCELLED`.
- Does not change route or step structure.

### Link Route Instance

```http
POST /api/production/work-orders/{workOrderId}/link-route-instance
```

This endpoint is a draft and may be deferred to work-order-driven dispatch implementation.

Request draft:

```json
{
  "productionRouteInstanceId": 3001
}
```

Rules:

- Validate same tenant.
- Validate same `order_item_id`.
- Do not mutate frozen route structure.
- Do not create route instance.
- Do not refactor existing dispatch.

## Admin Page Design Draft

Planned route:

```text
/production/work-orders
```

Planned files, not created by this change:

```text
WorkOrderList.vue
WorkOrderDetail.vue
WorkOrderFormDrawer.vue
WorkOrderMaterialEditor.vue
workOrderApi.ts
route/menu entries
```

### Work Order List

Capabilities:

- Show work orders.
- Filter by status.
- Search by work order number.
- Search by product/location keyword.
- Filter by date range.
- Show route linked or not.
- Show order no if available as a snapshot or read-only order summary.
- Create work order entry.
- View detail.
- Edit DRAFT.
- Release DRAFT.
- Cancel DRAFT/RELEASED.
- Show material requirement summary.
- Show clear hint: material requirements are not inventory readiness.

### Work Order Detail

Sections:

- basic work order identity
- order summary read-only context such as order no, order type, and customer type when available
- order item snapshot
- production instruction
- technical configuration
- schedule and deadline
- responsible people
- signature/confirmation draft fields
- material requirement list
- route instance relation
- operation history placeholder if later supported

### Create From Order Item Flow

1. User opens `/production/work-orders`.
2. User clicks "create from order item".
3. UI opens an order item candidate selector.
4. User selects one readable order item.
5. UI displays order summary and order item snapshot fields.
6. User fills production instruction, technical configuration, dates, and responsible people.
7. User adds demand-only material requirement rows.
8. User saves.
9. Backend creates `DRAFT` work order.
10. If an active work order exists, UI shows `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM`.
11. UI does not call order-item production write-back.

### Material Requirement Editor

Fields:

```text
material_code
material_name
spec
unit
required_qty
usage_stage
related_step_template_id
related_step_instance_id
remark
```

UI rules:

- `material_name` required.
- `required_qty > 0`.
- Display demand-only wording.
- Do not display "stock deducted".
- Do not display "reserved".
- Do not display readiness state in this change.

## Status And Action Matrix

```text
Status       Edit Base  Edit Materials  Release  Cancel  Dispatch Link Display
DRAFT        yes        yes             yes      yes     display only
RELEASED     no         no              no       yes     display only
IN_PROGRESS  no         no              no       no      display only
COMPLETED    no         no              no       no      display only
CANCELLED    no         no              no       no      display only
```

MVP does not implement released-work-order revision approval.

## Error Handling

Suggested business errors:

```text
WORK_ORDER_NOT_FOUND
WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM
WORK_ORDER_INVALID_STATUS_TRANSITION
WORK_ORDER_CANCELLED
WORK_ORDER_COMPLETED
WORK_ORDER_ROUTE_LINK_CONFLICT
MATERIAL_REQUIREMENT_INVALID
ORDER_ITEM_NOT_FOUND
WORK_ORDER_EDIT_NOT_ALLOWED
```

UI handling:

- Duplicate active work order: show existing work order number if backend provides it, otherwise show a direct message.
- Invalid status transition: refresh detail after error to avoid stale action buttons.
- Invalid material requirement: focus the invalid material row.
- Order item not found: close selector item or mark unavailable.

## Permission Draft

Roles are design-only and should reuse the existing JWT/currentUser/tenant mechanism.

- Production manager: create, edit DRAFT, edit DRAFT materials, release, cancel DRAFT/RELEASED, view all.
- Production staff: read work orders relevant to production; execution still uses step execution flows.
- Boss/management: read-only view for management context.

This change does not implement a new permission system.

## Dispatch Boundary

This change does not:

- create `production_route_instance`
- freeze route instance
- create `production_step_instance`
- modify `step_order`
- refactor existing `production-dispatch-instance`
- remove legacy direct `order_item -> production_route_instance`

The admin page may show:

```text
not dispatched
linked production route instance
go to dispatch placeholder
view production instance placeholder
```

Work-order-driven dispatch requires a later OpenSpec change.

Batch work order creation and batch dispatch are also future scope. This API/admin design creates one work order from one order item.

## Inventory Boundary

Material requirements in admin-web are demand-only.

This change does not:

- show stock as deducted
- reserve stock
- deduct stock
- create stock in/out
- create inventory transactions
- create purchase records
- create supplier records
- create finance records
- calculate readiness
- show step shortage state
- block step start

Future `inventory/material-readiness` owns available quantity, shortage, optional reservation, readiness by step or usage stage, and affected-step start warning or blocking.

## Order-Line Boundary

The API may read `order_item` candidates through production-side contracts.

The API may also read `project_order` summary fields for candidate display:

```text
order_no
order_type
customer_type
deal_owner_id or dealOwnerName
```

This is read-only context. It does not make `project_order + N order_item` the production work order model.

It must not:

- create orders
- modify order amount
- modify quotation
- modify customer information
- modify product specification
- modify product quantity
- modify order core status
- store or show `deal_amount`, `unit_price`, or `subtotal` by default
- call restricted `PUT /api/order-items/{id}` from work order create/edit/release/cancel flows
- implement CRM
- implement customer public pool
- implement contribution

Order item snapshot fields stored in the work order are production display context only.

The restricted `PUT /api/order-items/{id}` production write-back is owned by dispatch/execution/progress changes. Only those later or existing changes may update `productionStatus`, `productionProgress`, or `productionRouteInstanceId`.

## Batch And Print Boundary

This change does not design implementation for:

- batch work order creation
- batch dispatch
- production instruction print
- production instruction PDF export

These may be future OpenSpec changes after single-item work order API/admin is stable.

## Test Plan Draft

Future implementation tests should cover:

- list work orders with filters
- get work order detail with material requirements
- create from order item
- duplicate active work order error
- candidate API marks or hides order items with active work order
- update DRAFT base fields
- reject update after release
- replace DRAFT material requirements
- reject blank material name
- reject non-positive quantity
- release DRAFT
- cancel DRAFT and RELEASED
- reject cancel IN_PROGRESS and COMPLETED
- no inventory transaction creation
- no order core mutation
- no commercial amount field storage or default display
- no work-order create/edit/release/cancel call to `PUT /api/order-items/{id}`
- link route instance conflict by tenant/order item
- no route frozen structure mutation
- one order item creates one work order; no batch dispatch behavior
- admin UI action visibility by status

## Out Of Scope

This OpenSpec does not implement backend code, Controller APIs, Mapper/Service/Entity changes, migration, admin-web files, TypeScript APIs, route/menu changes, frontend apps, inventory, purchase, supplier, finance, CRM, public pool, contribution, order creation, order core mutation, commercial amount storage, restricted order-item production write-back, batch work order creation, batch dispatch, production instruction print/PDF, dispatch refactor, route instance creation, route freezing, nested/parallel graph, photo upload, file upload, worker-uniapp, production-h5, screen-web, attendance, or dashboard.
