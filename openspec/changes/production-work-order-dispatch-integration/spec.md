# Production Work Order Dispatch Integration Specification

## ADDED Requirements

### Requirement: Dispatch production from released work order

The system SHALL design production dispatch from a released production work order.

#### Scenario: RELEASED work order can dispatch

- GIVEN a `production_work_order` exists
- AND its status is `RELEASED`
- AND `production_route_instance_id` is null
- WHEN production dispatch is confirmed from that work order
- THEN the future implementation may create a `production_route_instance`
- AND may create `production_step_instance` records
- AND the dispatch is anchored by `workOrderId`

#### Scenario: DRAFT work order is rejected

- GIVEN a work order status is `DRAFT`
- WHEN dispatch is requested
- THEN the system rejects the request with `WORK_ORDER_NOT_RELEASED`
- AND does not create a route instance

#### Scenario: CANCELLED or COMPLETED work order is rejected

- GIVEN a work order status is `CANCELLED` or `COMPLETED`
- WHEN dispatch is requested
- THEN the system rejects the request
- AND does not create route or step instances

#### Scenario: Already linked work order rejects duplicate dispatch

- GIVEN a work order already has `production_route_instance_id`
- WHEN dispatch is requested again
- THEN the system rejects the request with `WORK_ORDER_ALREADY_DISPATCHED`
- AND does not create another route instance

### Requirement: Reuse route template freeze rules

The system SHALL reuse existing production dispatch template-copy and freeze rules for work-order-driven dispatch.

#### Scenario: Select process route template

- GIVEN a released work order exists
- WHEN a production manager opens work-order dispatch
- THEN the user can select an enabled non-deleted `process_route_template`
- AND unavailable templates are rejected with `PROCESS_ROUTE_TEMPLATE_NOT_FOUND` or `PROCESS_ROUTE_TEMPLATE_DISABLED`

#### Scenario: Copy route template

- WHEN dispatch is confirmed
- THEN route template/config values are copied into `production_route_instance` snapshot fields
- AND later template edits do not mutate the route instance

#### Scenario: Copy step templates

- WHEN dispatch is confirmed
- THEN enabled step template/config values are copied into ordered `production_step_instance` rows
- AND each step initializes as `PENDING`
- AND step order remains contiguous

#### Scenario: Route instance is frozen

- WHEN dispatch succeeds
- THEN the route instance is marked `frozen = true`
- AND frozen structure protections apply

#### Scenario: Work order cannot modify frozen structure

- GIVEN a route instance is frozen
- WHEN a user acts from the work order page
- THEN the system must not allow adding, deleting, reordering, renaming, skipping, reworking, or appending route steps through the work order

### Requirement: Link work order and route instance

The system SHALL link the work order and production route instance after work-order-driven dispatch.

#### Scenario: Work order stores production route instance id

- WHEN dispatch succeeds
- THEN `production_work_order.production_route_instance_id` is set to the new route instance id
- AND later work order pages can show the linked production instance

#### Scenario: Route instance can trace back to work order

- WHEN schema supports `production_route_instance.work_order_id`
- THEN dispatch should store the work order id on the route instance
- AND if schema does not support it yet, a later migration design may add it
- AND this OpenSpec does not create that migration

#### Scenario: Same tenant and same order item are required

- GIVEN a route instance is linked to a work order
- THEN tenant id must match
- AND order id must match
- AND order item id must match
- AND mismatches return `WORK_ORDER_ROUTE_LINK_CONFLICT`

### Requirement: Update work order status after dispatch

The system SHALL define how work order status changes after successful dispatch.

#### Scenario: MVP changes RELEASED to IN_PROGRESS on dispatch success

- GIVEN a work order is `RELEASED`
- WHEN work-order-driven dispatch succeeds
- THEN MVP changes the work order status to `IN_PROGRESS`
- AND records updated user/time according to existing audit patterns

#### Scenario: Repeated dispatch is rejected

- GIVEN a work order is `IN_PROGRESS`
- OR already has a linked route instance
- WHEN dispatch is requested
- THEN the system rejects repeated dispatch
- AND does not create another route instance

#### Scenario: Future issued or dispatched status requires separate change

- WHEN the business needs separate `ISSUED`, `DISPATCHED`, or "released but not started" states
- THEN a later OpenSpec change must extend the work order status machine
- AND this MVP keeps the existing status set unchanged

### Requirement: Restricted order item production write-back

The system SHALL restrict order item write-back during work-order-driven dispatch.

#### Scenario: Write only production fields

- WHEN dispatch succeeds
- THEN production may update only:
  - `production_status`
  - `production_progress`
  - `production_route_instance_id`
- AND production progress starts at `0`
- AND route instance id is the newly created route instance id

#### Scenario: Do not modify order core fields

- WHEN dispatch succeeds
- THEN production must not update item name, spec, unit, quantity, unit price, subtotal, remark, customer fields, order amount, quotation, or order core status

#### Scenario: Write-back failure keeps dispatch consistent

- GIVEN route and step instances are being created
- WHEN restricted order item production write-back fails
- THEN MVP design requires the dispatch transaction to fail and roll back when possible
- AND if the real write-back is outside the same database transaction, future implementation must define retry or compensation before production use

### Requirement: Provide admin work order dispatch entry

The system SHALL design an admin-web dispatch entry from production work orders.

#### Scenario: RELEASED work order shows dispatch production action

- GIVEN a work order status is `RELEASED`
- AND it has no linked route instance
- WHEN the user views list or detail
- THEN the UI may show "dispatch production"

#### Scenario: Other statuses hide or disable dispatch action

- GIVEN a work order is `DRAFT`, `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`
- WHEN the user views list or detail
- THEN dispatch action is hidden or disabled with a clear reason

#### Scenario: Dispatch result shows route instance link

- WHEN work-order-driven dispatch succeeds
- THEN admin-web shows the linked route instance id or navigation
- AND no batch dispatch is performed

### Requirement: Preserve legacy dispatch compatibility

The system SHALL preserve the existing direct dispatch path during transition.

#### Scenario: Legacy direct dispatch remains transition path

- GIVEN the existing `order_item -> production_route_instance` dispatch exists
- WHEN this OpenSpec is applied
- THEN it is not removed or refactored by this change
- AND new admin entry should prefer work-order-driven dispatch

#### Scenario: Shared copy and freeze rules remain compatible

- WHEN future implementation reuses or extracts dispatch logic
- THEN route template copying, step copying, and frozen structure rules remain compatible with existing dispatch behavior

### Requirement: Preserve inventory boundary

The system SHALL keep inventory and material readiness out of work-order dispatch integration.

#### Scenario: No inventory check

- WHEN work-order-driven dispatch is requested
- THEN the system does not check inventory availability
- AND does not calculate material readiness

#### Scenario: No reservation or deduction

- WHEN dispatch succeeds
- THEN the system does not reserve inventory
- AND does not deduct stock
- AND does not create stock in/out or `inventory_transaction`

#### Scenario: No shortage-based blocking

- WHEN the work order contains material requirements
- THEN dispatch does not block on shortages
- AND shortage node display or step blocking belongs to a later inventory/material-readiness change

### Requirement: Preserve process graph boundary

The system SHALL keep nested and parallel process graph behavior out of this change.

#### Scenario: Keep serial step order

- WHEN dispatch creates step instances
- THEN it uses current serial `step_order` behavior
- AND does not create a dependency graph

#### Scenario: No GROUP TASK or parallel nested model

- WHEN this change is designed
- THEN it does not introduce GROUP nodes
- AND does not introduce TASK leaf node rules
- AND does not implement parallel or nested execution

## DoD

- The OpenSpec change exists under `openspec/changes/production-work-order-dispatch-integration`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change only writes OpenSpec documents.
- No `.java`, `.sql`, `.vue`, `.ts`, `pom.xml`, or `package.json` files are modified.
- The design clearly dispatches from `production_work_order`.
- Existing direct dispatch is documented as legacy/transition.
- Status rules for `DRAFT`, `RELEASED`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED` are documented.
- MVP status update rule `RELEASED -> IN_PROGRESS` on successful dispatch is documented.
- Work order to route instance linking is documented.
- Restricted order item production write-back is documented.
- Inventory, material readiness, shortage checks, reservation, deduction, stock in/out, purchase, supplier, and finance are excluded.
- CRM, public pool, contribution, order creation, and order core mutation are excluded.
- Batch dispatch and print/PDF are excluded.
- Nested/parallel process graph, upload, worker apps, screen-web, attendance, and dashboard are excluded.
- No completion claim is allowed without verification evidence.

## Out Of Scope

The system SHALL NOT implement backend code, frontend code, migrations, Controller APIs, admin-web pages, inventory deduction, inventory reservation, stock in/out, `inventory_transaction`, material readiness, shortage checks, purchase, supplier, finance, CRM, public pool, contribution, order creation, order amount or quotation modification, customer field modification, product spec or quantity modification, batch dispatch, print/PDF, nested/parallel graph, photo upload, file upload, worker-uniapp, production-h5, screen-web, attendance, or dashboard in this change.
