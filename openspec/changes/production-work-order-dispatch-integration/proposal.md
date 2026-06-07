# Production Work Order Dispatch Integration Proposal

## Why

The production work order layer now exists, and the API/admin workflow can create and release production work orders. However, the existing production dispatch capability may still start directly from `order_item`:

```text
order_item
-> production_route_instance
-> production_step_instance
```

The new production main chain must be connected end to end:

```text
order_item
-> production_work_order
-> production_route_instance
-> production_step_instance
```

`production_work_order` is the production department's instruction and preparation document. It is also the anchor for production requirements and future material readiness. Production dispatch should therefore start from a released work order, not from the customer/order-line item directly.

The old direct dispatch path remains useful during transition and for legacy/demo compatibility. This change designs the work-order-driven dispatch flow while preserving the existing `production-dispatch-instance` behavior as a legacy/transition entry.

This change is OpenSpec-only. It does not implement backend code, frontend code, migration, API, route, page, inventory, upload, worker app, or customer-line logic.

## Goals

- Design dispatch from a `RELEASED` production work order.
- Use `production_work_order.order_id`, `order_item_id`, and snapshots as dispatch context.
- Select a `process_route_template` from the work order dispatch flow.
- Reuse existing `production-dispatch-instance` template copy, step copy, and freeze rules.
- Create `production_route_instance` and `production_step_instance` after confirm dispatch in future implementation.
- Link the work order and route instance:
  - set `production_work_order.production_route_instance_id`
  - use `production_route_instance.work_order_id` if the schema later supports it
  - otherwise keep the work order as the source of the link until a later migration
- Prevent repeated dispatch when a work order already has a route instance.
- Reject dispatch for `DRAFT`, `CANCELLED`, `COMPLETED`, and already-dispatched `IN_PROGRESS` work orders.
- Define MVP status behavior after dispatch.
- Recommend MVP Option A: successful dispatch changes work order from `RELEASED` to `IN_PROGRESS`.
- Allow restricted production write-back to `order_item` only after successful dispatch.
- Keep the legacy direct `order_item -> production_route_instance` dispatch path as transition-only.
- Provide admin-web design for a "dispatch production" entry from work order list/detail.
- Prepare a clean handoff to future inventory/material-readiness.

## Non-Goals

This change does not implement:

- backend business code
- Controller APIs
- admin-web pages
- Vue or TypeScript files
- database migration
- production route graph tables
- inventory deduction
- inventory reservation
- inventory readiness
- stock in/out
- inventory transaction
- material shortage check
- shortage node display
- shortage-based step blocking
- purchase
- supplier
- finance
- CRM
- customer public pool
- contribution value logic
- order creation
- order amount or quotation changes
- customer field changes
- product spec changes
- product quantity changes
- batch work order creation
- batch dispatch
- production instruction print or PDF
- nested, parallel, or non-linear process graph
- photo upload
- file upload
- worker-uniapp
- production-h5
- screen-web
- attendance
- dashboard

## Scope

OpenSpec scope:

```text
openspec/changes/production-work-order-dispatch-integration/
```

Allowed files in this change:

```text
proposal.md
design.md
spec.md
tasks.md
```

Future implementation scope after approval:

```text
backend/zhisheng-production
frontend/admin-web production module
```

Future implementation may add work-order dispatch API/service methods and admin-web navigation from work order detail/list to dispatch. It must not implement inventory/material-readiness in this change.

## Collaboration Boundaries

Codex production line owns:

- work-order-driven dispatch
- production route instance creation
- production step instance creation
- route and step freeze behavior
- work order to route instance linking
- restricted production write-back to order item production fields

Cursor customer/order line owns:

- `project_order`
- `order_item` core fields
- customer fields
- quotation and amount fields
- product specification and quantity ownership
- CRM
- public pool
- contribution

Customer line provides restricted production write-back through:

```http
PUT /api/order-items/{id}
```

Work-order dispatch may write only:

```text
productionStatus
productionProgress
productionRouteInstanceId
```

It must not update:

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

## Risks

- If direct order-item dispatch remains the main UI path, work orders will not become the stable production preparation anchor.
- If dispatch can run from `DRAFT` work orders, unconfirmed production instructions may create frozen execution instances.
- If dispatch does not link back to the work order, inventory readiness and production tracking will lose the work order anchor.
- If dispatch updates order core fields beyond the restricted production fields, production will violate the customer/order-line boundary.
- If work order status stays `RELEASED` after dispatch without another visible state, admin users may not see that production has started. MVP therefore recommends `RELEASED -> IN_PROGRESS` on successful dispatch.
- If batch dispatch is mixed in, the change will expand beyond the single-work-order flow and obscure duplicate-dispatch rules.
- If inventory readiness is mixed in, dispatch may become blocked by material availability before the readiness design is approved.
