# Production Work Order Dispatch Integration Design

## 1. Overview

This change designs production dispatch from `production_work_order`.

It is OpenSpec-only. It does not write code, migration, Controller, API implementation, Vue, TypeScript, inventory, upload, worker app, dashboard, or customer-line logic.

Target production chain:

```text
order_item
-> production_work_order
-> production_route_instance
-> production_step_instance
```

Dispatch starts from a `RELEASED` work order. The route and step instance creation rules should reuse the already designed `production-dispatch-instance` behavior: template snapshot copy, step snapshot copy, route frozen after dispatch, and frozen structure protection.

## 2. Current Legacy Dispatch Flow

Current legacy flow:

```text
order_item
-> select process_route_template
-> create production_route_instance
-> create production_step_instance
-> freeze route instance
-> write order_item production fields
```

This direct path is transition/legacy behavior. It must not be removed by this OpenSpec. Later implementation can keep the old endpoint while making admin-web prefer the work-order-driven entry.

## 3. Target Work-Order-Driven Dispatch Flow

Target flow:

```text
production_work_order(RELEASED)
-> select process_route_template
-> copy template into production_route_instance
-> copy steps into production_step_instance
-> freeze route instance
-> link work order and route instance
-> update work order status
-> restricted order_item production write-back
```

Admin entry should use `workOrderId` first. The page may still display order item context, but it should not require users to start dispatch from `orderItemId`.

Suggested future endpoints:

```http
GET /api/production/work-orders/{workOrderId}/dispatch-context
POST /api/production/work-orders/{workOrderId}/dispatch-config/from-template
POST /api/production/work-orders/{workOrderId}/dispatch
```

These are drafts only. This change does not implement them.

## 4. Status / Action Matrix

```text
Status       Dispatch Action
DRAFT        reject
RELEASED     allow
IN_PROGRESS  reject duplicate dispatch
COMPLETED    reject
CANCELLED    reject
```

Rules:

- `DRAFT` is not confirmed and cannot dispatch.
- `RELEASED` means production instruction has been confirmed and can dispatch.
- `IN_PROGRESS` means dispatch has already created a production instance or execution has started.
- `COMPLETED` and `CANCELLED` are terminal for MVP and cannot dispatch.
- Any work order with non-null `production_route_instance_id` must reject repeated dispatch.

## 5. Dispatch Preconditions

Future implementation must validate:

- current tenant matches work order tenant
- work order exists and is not deleted
- work order status is `RELEASED`
- `production_work_order.production_route_instance_id` is null
- related `order_item` can be read through production read contract
- selected process route template exists
- selected route template is enabled and not deleted
- selected route template has copyable enabled step templates
- configured steps are valid and non-empty
- each step keeps required dispatch fields such as step name, order, and assigned role

Suggested errors:

```text
WORK_ORDER_NOT_FOUND
WORK_ORDER_NOT_RELEASED
WORK_ORDER_ALREADY_DISPATCHED
WORK_ORDER_CANCELLED
WORK_ORDER_COMPLETED
PROCESS_ROUTE_TEMPLATE_NOT_FOUND
PROCESS_ROUTE_TEMPLATE_DISABLED
WORK_ORDER_ROUTE_LINK_CONFLICT
ORDER_ITEM_NOT_FOUND
ORDER_ITEM_PRODUCTION_WRITEBACK_FAILED
DISPATCH_INSTANCE_CREATE_FAILED
```

## 6. Dispatch Transaction Draft

Future implementation should treat confirm dispatch as one transaction.

Transaction draft:

1. Load work order by `tenant_id + workOrderId`.
2. Require work order status `RELEASED`.
3. Require `production_route_instance_id` is null.
4. Load order item read-only context by `work_order.order_item_id`.
5. Load selected `process_route_template`.
6. Validate template is enabled and not deleted.
7. Load enabled step templates or submitted dispatch configuration.
8. Copy route template/config into `production_route_instance`.
9. Copy step templates/config into `production_step_instance`.
10. Set `production_route_instance.frozen = true`.
11. Link work order to route instance by setting `production_work_order.production_route_instance_id`.
12. Set work order status according to the MVP decision.
13. Restricted update to `order_item` production fields.
14. Commit transaction.

The MVP transaction must not depend on `production_route_instance.work_order_id`. If that column does not exist, implementation should not add it in this change and must not manually alter the database without a Flyway migration.

If route/step creation, work order link, status update, or order item production write-back fails, the implementation should fail the dispatch transaction. MVP should prefer rollback over partial success. If the real customer-line write-back is outside the database transaction, the implementation must define a retry or compensation strategy before production use.

## 7. Work Order Status After Dispatch

Two options:

### Option A: Dispatch Success Sets `RELEASED -> IN_PROGRESS`

Pros:

- Simple MVP state model.
- Admin-web immediately shows that the work order has moved beyond preparation.
- Existing dispatch semantics already mean production instances exist.
- Avoids adding another state before workflow maturity.

Cons:

- It conflates "dispatched but no worker has started" with "production execution has started".
- Future reporting may need a separate issued/dispatched state.

### Option B: First Step Start Sets `RELEASED -> IN_PROGRESS`

Pros:

- More precise execution semantics.
- Distinguishes generated route instance from actual shop-floor start.

Cons:

- Admin-web needs another visible state or route link indicator to avoid confusion.
- Existing step execution already owns first-start route/order item status changes, so work order status synchronization would span changes.

### MVP Recommendation

Use Option A:

```text
RELEASED -> IN_PROGRESS on successful work-order dispatch
```

Reason:

- The MVP currently uses dispatch as the moment when frozen production tasks are generated.
- It gives production managers a clear visual transition after clicking dispatch.
- It avoids adding `ISSUED` or `DISPATCHED` to the work order status machine in this phase.

Future change may split states:

```text
RELEASED -> DISPATCHED -> IN_PROGRESS
```

or introduce `ISSUED` if the business needs separate preparation, issued, and started states.

## 8. Route Instance Relation Draft

Required relation after dispatch:

```text
production_work_order.production_route_instance_id = production_route_instance.id
```

MVP relation decision:

- Use `production_work_order.production_route_instance_id` as the source of truth for the route link.
- Do not add `production_route_instance.work_order_id` in this implementation.
- Work order list/detail only needs to display the linked `productionRouteInstanceId`.
- Do not manually change database structure for reverse linking.
- Do not change table structure without a reviewed Flyway migration.

If future product requirements need reverse lookup from route instance to work order, open a separate OpenSpec and migration for:

```text
production_route_instance.work_order_id
```

That future change may decide whether dispatch should populate the reverse column.

Validation:

- work order tenant must match route instance tenant
- work order `order_item_id` must match route instance `order_item_id`
- work order `order_id` must match route instance `order_id`
- linking must not mutate frozen route structure

## 9. Order Item Production Write-Back Boundary

Customer line provides restricted production write-back:

```http
PUT /api/order-items/{id}
```

Work-order dispatch may use it after successful dispatch to write only:

```text
productionStatus
productionProgress
productionRouteInstanceId
```

Write-back values after confirm dispatch:

```text
productionStatus = existing direct dispatch initial production status
productionProgress = 0
productionRouteInstanceId = new route instance id
```

Work-order-driven dispatch must reuse the existing legacy direct dispatch production-status write-back rule. Do not invent a new initial `production_status` value for work-order dispatch. If direct dispatch writes `DISPATCHED`, work-order dispatch should also write `DISPATCHED`. If direct dispatch uses another initial status, keep that value. This keeps legacy dispatch and work-order dispatch aligned in `order_item` status semantics.

Work-order dispatch must not update:

```text
itemName
spec
unit
quantity
unitPrice
subtotal
remark
customer fields
order amount
quotation
order core status
```

## 10. Admin-Web Interaction Draft

Admin work order list/detail may provide:

- "dispatch production" action only for `RELEASED` work orders with no linked route instance
- disabled or hidden action for `DRAFT`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED`
- visible route instance link after dispatch
- dispatch entry keyed by `workOrderId`

MVP UI can use a dialog or drawer instead of a dedicated page. The dialog/drawer may:

```text
open from /production/work-orders list or detail
load dispatch context by workOrderId
select process route template
load template steps
allow simple step adjustment
confirm dispatch
```

The MVP does not require a complex route such as:

```text
/production/work-orders/:workOrderId/dispatch
```

That dedicated page is future optional scope if dispatch configuration becomes too large for a dialog/drawer.

The dialog/drawer may reuse existing dispatch UI patterns:

- work order context panel
- order item read-only context
- route template selection
- editable step configuration before confirm
- confirm dispatch warning about frozen route structure

It must not implement batch dispatch or print/PDF.

## 11. Error Handling

Suggested errors:

```text
WORK_ORDER_NOT_FOUND
WORK_ORDER_NOT_RELEASED
WORK_ORDER_ALREADY_DISPATCHED
WORK_ORDER_CANCELLED
WORK_ORDER_COMPLETED
PROCESS_ROUTE_TEMPLATE_NOT_FOUND
PROCESS_ROUTE_TEMPLATE_DISABLED
WORK_ORDER_ROUTE_LINK_CONFLICT
ORDER_ITEM_NOT_FOUND
ORDER_ITEM_PRODUCTION_WRITEBACK_FAILED
DISPATCH_INSTANCE_CREATE_FAILED
```

UI behavior:

- `WORK_ORDER_NOT_RELEASED`: show that work order must be released before dispatch.
- `WORK_ORDER_ALREADY_DISPATCHED`: refresh work order detail and show linked route instance.
- `PROCESS_ROUTE_TEMPLATE_DISABLED`: refresh route template options.
- `ORDER_ITEM_PRODUCTION_WRITEBACK_FAILED`: warn that dispatch failed and should not be partially visible.

## 12. Compatibility With Existing Production Dispatch Instance

This change reuses concepts from `production-dispatch-instance`:

- read order item context
- select route template
- create editable pre-dispatch configuration
- copy route template snapshot
- copy step template snapshots
- set route instance `frozen = true`
- initialize steps as `PENDING`
- write back limited order item production fields

Compatibility rules:

- Existing direct `order_item -> production_route_instance` endpoint remains transition-only.
- New admin work should prefer `workOrderId`.
- Shared internal copy/freeze logic may be extracted in future implementation to avoid divergence.
- This OpenSpec does not remove or rewrite legacy direct dispatch.

## 13. Inventory / Material-Readiness Boundary

This change does not:

- check available stock
- reserve stock
- deduct stock
- create stock in/out
- create `inventory_transaction`
- calculate material readiness
- show shortage by node
- block step start for shortage

Future `inventory/material-readiness` owns these behaviors and should use `production_work_order_material` as the demand input.

## 14. Process Graph Boundary

This change keeps existing serial `step_order` behavior.

It does not:

- create GROUP/TASK nodes
- create dependency graph
- create parallel branches
- create nested steps
- alter current serial execution rules
- change check-in binding rules

Future process graph design owns nested and parallel execution.

## 15. Out Of Scope

This OpenSpec does not implement backend code, frontend code, API, Controller, Service, Mapper, Entity, migration, inventory, purchase, supplier, finance, CRM, public pool, contribution, order creation, order core mutation, batch dispatch, print/PDF, nested/parallel graph, photo upload, file upload, worker-uniapp, production-h5, screen-web, attendance, or dashboard.
