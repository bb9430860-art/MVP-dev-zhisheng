# Production Work Order API And Admin Proposal

## Why

`production-work-order` backend core now provides the production-owned work order model and service rules, but production managers still have no API or admin-web workflow to use it.

Production work orders are the production preparation entry point before material readiness, production dispatch, route execution, and worker check-in. Admin users need a controlled place to create a work order from an existing `order_item`, edit the draft instruction, maintain demand-only material requirements, release or cancel the work order, and see whether it has been linked to a production route instance.

This change designs the next layer:

```text
order_item
-> production_work_order
-> production_route_instance
-> production_step_instance
```

It is OpenSpec-only. It does not implement API code, Controller code, admin-web pages, route changes, TypeScript clients, migrations, or backend core changes.

## Goals

- Design production work order API endpoints.
- Design admin-web production work order management pages.
- Provide a create-from-order-item interaction flow.
- Preserve the existing production work order status machine:
  - `DRAFT`
  - `RELEASED`
  - `IN_PROGRESS`
  - `COMPLETED`
  - `CANCELLED`
- Define draft edit restrictions:
  - `DRAFT` can edit base information and material requirements.
  - `RELEASED` freezes key production instruction and material requirement edits in MVP.
  - `IN_PROGRESS`, `COMPLETED`, and `CANCELLED` cannot be edited.
- Design order item candidate read API without taking ownership of order-line logic.
- Integrate the customer-line `project_order` and `order_item` read contract into the candidate API design.
- Clarify that `project_order + N order_item` is an order summary view and candidate source, not the production work order model.
- Keep MVP work order creation at one `order_item` to one `production_work_order`.
- Design material requirement editing as demand-only records.
- Design admin list, detail, form drawer, material editor, status actions, and dispatch link placeholders.
- Define permission expectations for production manager, production staff, and management viewers.
- Keep work-order-driven dispatch as a later change.
- Keep inventory/material-readiness as a later change.
- Keep customer-line, CRM, public pool, contribution, and order core logic out of this change.

## Non-Goals

This change does not implement:

- Java business code
- Controller APIs
- Mapper, Service, or Entity changes
- Flyway migration
- Vue pages
- TypeScript API clients
- admin-web route or menu changes
- package or Maven dependency changes
- production-work-order backend core changes
- production-dispatch-instance refactor
- production route instance creation
- route instance freezing
- batch work order creation
- batch dispatch
- production instruction print or PDF export
- current serial `step_order` execution changes
- inventory deduction
- inventory reservation
- stock in/out
- inventory transaction
- purchase
- supplier
- finance
- CRM
- customer public pool
- contribution value logic
- order creation
- order amount, quotation, customer, product specification, product quantity, or order core status mutation
- storing `deal_amount`, `unit_price`, or `subtotal` in production work order MVP
- showing commercial amount fields by default in the production work order admin page
- nested, parallel, or non-linear process graph
- photo upload
- file upload
- worker-uniapp
- production-h5
- screen-web

## Scope

OpenSpec scope:

```text
openspec/changes/production-work-order-api-admin/
```

Allowed files in this change:

```text
proposal.md
design.md
spec.md
tasks.md
```

Future implementation scope, after this OpenSpec is approved:

```text
backend/zhisheng-production
frontend/admin-web production module
```

This OpenSpec may describe future files such as `WorkOrderList.vue`, `WorkOrderDetail.vue`, `WorkOrderFormDrawer.vue`, `WorkOrderMaterialEditor.vue`, and `workOrderApi.ts`, but it must not create them.

## Collaboration Boundaries

Codex owns the production work order API and admin production module design because it belongs to the production line.

Cursor/customer line owns:

- CRM
- customer public pool
- contribution
- order creation
- order amount and quotation
- customer and contact data
- product specification and product quantity
- order core lifecycle status

Production may read `order_item` as a candidate source for work order creation. Production must not create orders or mutate order core fields.

The admin work order UI may show production-side snapshots and route-instance link state. It must not become an order editor, CRM editor, quotation editor, or inventory transaction screen.

### Order Contract Integration

Customer line provides `project_order` and `order_item` as the order-side contract.

`project_order` fields include:

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

`order_item` fields include:

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

Customer line has implemented or plans to provide:

```http
GET /api/order/list
GET /api/order/{id}
GET /api/order-items?orderId=xxx
GET /api/order-items/{id}
PUT /api/order-items/{id}
```

Boundary rules:

- `production_work_order` is not `project_order + N order_item`.
- `project_order + N order_item` can support order summary views and order item candidate selection.
- It is not the production work order main model.
- MVP production work order creation remains one `order_item` to one `production_work_order`.
- The new production main flow remains `order_item -> production_work_order -> production_route_instance -> production_step_instance`.
- Batch work order creation and batch dispatch are future scope.
- Production instruction print/PDF is future scope.
- `PUT /api/order-items/{id}` is a restricted production write-back contract for dispatch/execution/progress synchronization only.
- This API/admin change must not call that write-back endpoint when creating, editing, releasing, or cancelling work orders.
- Only later production dispatch or step execution changes may write `production_status`, `production_progress`, or `production_route_instance_id`.

Production work order candidate APIs may read only production-relevant order fields:

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

Production work order snapshots may store production-required fields:

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

Commercial fields stay with order/customer/finance scope:

```text
deal_amount
unit_price
subtotal
```

They must not enter production work order MVP, must not be editable from production work order pages, must not be modified by production, and should not be shown by default in admin work order pages. If a later boss dashboard needs amount metrics, it should read order-line aggregation, not copy amounts into `production_work_order`.

## Risks

- If the API exposes order editing fields, production may accidentally take over customer/order ownership.
- If material requirements are presented as stock readiness, users may assume inventory was reserved or deducted when it was not.
- If release is allowed to behave like dispatch, this change may blur the boundary with `production-dispatch-instance`.
- If `RELEASED` work orders remain broadly editable, the production instruction can drift after confirmation without an approval model.
- If order items with active work orders are hidden without explanation, production users may try to create duplicate work orders elsewhere.
- If work-order-driven dispatch is implemented inside this change, it will expand beyond API/admin design and conflict with the existing dispatch transition plan.
- If `project_order + N order_item` is treated as the work order model, production instruction, material demand, dispatch, and execution boundaries will be blurred.
- If `deal_amount`, `unit_price`, or `subtotal` are copied into the work order, production pages may become accidental finance/order pages.
- If this change calls restricted `PUT /api/order-items/{id}`, work order creation or release may incorrectly update dispatch/progress fields before dispatch or execution.
- If batch dispatch or print/PDF is added here, the API/admin change will exceed its MVP preparation scope.
- If verification is skipped, OpenSpec-only documentation may be mistaken for completed API or UI implementation.
