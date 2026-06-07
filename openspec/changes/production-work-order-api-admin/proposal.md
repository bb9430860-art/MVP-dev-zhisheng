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

## Risks

- If the API exposes order editing fields, production may accidentally take over customer/order ownership.
- If material requirements are presented as stock readiness, users may assume inventory was reserved or deducted when it was not.
- If release is allowed to behave like dispatch, this change may blur the boundary with `production-dispatch-instance`.
- If `RELEASED` work orders remain broadly editable, the production instruction can drift after confirmation without an approval model.
- If order items with active work orders are hidden without explanation, production users may try to create duplicate work orders elsewhere.
- If work-order-driven dispatch is implemented inside this change, it will expand beyond API/admin design and conflict with the existing dispatch transition plan.
- If verification is skipped, OpenSpec-only documentation may be mistaken for completed API or UI implementation.
