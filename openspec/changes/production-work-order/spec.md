# Production Work Order Specification

## ADDED Requirements

### Requirement: Create production work order from order item

The system SHALL allow production to create a production work order from an existing customer-line `order_item`.

#### Scenario: Create draft work order from order item

- GIVEN an `order_item` exists through the customer-line order contract or a dev/test production-owned adapter
- WHEN production creates a work order from that `order_item`
- THEN the system records one `production_work_order`
- AND stores `order_id`
- AND stores `order_item_id`
- AND initializes the work order status as `DRAFT`
- AND records production-side snapshots needed for production display
- AND does not create an order
- AND does not modify order core data

#### Scenario: Prevent duplicate active work order for one order item

- GIVEN an active `production_work_order` already exists for the same `tenant_id + order_item_id`
- AND active statuses are `DRAFT`, `RELEASED`, and `IN_PROGRESS`
- WHEN production attempts to create another active work order for the same `order_item`
- THEN the system rejects the request with `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM`
- AND does not create another active work order
- AND does not automatically reuse the existing work order

#### Scenario: Completed or cancelled work order is not active

- GIVEN a `production_work_order` for an `order_item` is `COMPLETED` or `CANCELLED`
- WHEN production checks active work order uniqueness
- THEN that completed or cancelled work order is not treated as active
- AND a later new work order policy, if needed, must be explicitly designed before implementation

#### Scenario: Generate work order number

- GIVEN a create work order request does not include a work order number
- WHEN the work order is created in a future implementation
- THEN the backend generates a production-owned work order number
- AND the format is `WO-{yyyyMMdd}-{dailySequence}`
- AND an example is `WO-20260607-0001`
- AND the number is unique per tenant
- AND the daily sequence increments by `tenant_id + date`
- AND the number does not depend on `order_id` or `order_item_id`
- AND future implementation handles concurrent number conflicts and retries safely
- AND the work order number does not replace the customer order number

### Requirement: Preserve order-line ownership

The system SHALL preserve customer-line and order-line ownership when creating or updating production work orders.

#### Scenario: Production reads order item through production contract

- WHEN production needs order item data for a work order
- THEN it reads through a production-owned contract such as `OrderItemReadPort`
- AND treats `order_item` as an external shared contract
- AND does not implement CRM, public pool, contribution, or order core logic

#### Scenario: Production does not modify order core fields

- WHEN a production work order is created, edited, released, linked, or completed
- THEN production must not update customer data, order amount, quotation data, product specification, product quantity, customer source, order custom fields, or order core lifecycle status

#### Scenario: Production work order is internal production document

- WHEN a work order references `order_id` and `order_item_id`
- THEN the work order remains a production-owned internal document
- AND it does not become the source of truth for customer commercial data
- AND it does not replace the customer-line order or order item

### Requirement: Record production instruction fields

The system SHALL define production instruction fields needed by the production department.

#### Scenario: Record instruction and requirement fields

- WHEN a work order is drafted
- THEN it can record production-side instruction fields
- AND fields may include instruction title, instruction remark, production requirement, quality requirement, packaging requirement, shipping requirement, delivery requirement, and deadline remark
- AND these fields are production instructions, not quotation or customer contract fields

#### Scenario: Record schedule fields

- WHEN a work order is drafted
- THEN it can record planned start date
- AND planned finish date
- AND required delivery date
- AND these fields guide production planning
- AND they do not modify customer-line order fields

#### Scenario: Record responsible people

- WHEN a work order is drafted
- THEN it can record responsible user, handler, production manager, and optional primary worker
- AND these fields belong to production execution planning
- AND they do not implement employee management, payroll, contribution, or attendance logic

#### Scenario: Record signature and confirmation draft fields

- WHEN a work order moves through internal confirmation
- THEN it can record released by/at, confirmed by/at, production signed by/at, warehouse confirmed by/at, and quality confirmed by/at
- AND these fields are internal confirmation records only
- AND they do not implement legal electronic signature, approval workflow, or customer acceptance workflow in this change

### Requirement: Record technical configuration fields

The system SHALL define production-side technical configuration fields for a work order.

#### Scenario: Record common explicit technical configuration

- WHEN a work order is drafted
- THEN it can record only common high-frequency technical fields that need querying, filtering, or list display
- AND examples include equipment model, technical configuration summary, and technical configuration remark
- AND these fields are production-side technical configuration
- AND they do not modify order item product specification fields owned by the order line
- AND the first version must not solidify every production instruction sheet field into columns

#### Scenario: Reserve technical extension field

- WHEN a non-standard technical value is needed
- THEN the work order may use a production-owned `technical_config_json` draft field
- AND the JSON may carry CNC system, compensation method, cylinder brand, motor brand, valve group brand, oil pump brand, mold or blade, random accessories, machine color, shipping requirement, and other non-standard configuration
- AND the JSON field must not store order amount, quotation, customer contact, CRM source, contribution, or financial data
- AND important query/reporting fields should be promoted to typed columns in a later migration proposal

### Requirement: Record material requirement draft

The system SHALL define material requirement records under a production work order.

#### Scenario: Add material requirement line to work order

- GIVEN a `production_work_order` exists
- WHEN a material or component demand is recorded
- THEN the system can draft a `production_work_order_material` row
- AND stores work order id, order id, order item id, material reference or material name, spec, unit, required quantity, usage stage, optional related step template id, optional related step instance id, status, and remark
- AND the row represents demand only

#### Scenario: Material requirement does not reserve or deduct stock

- WHEN a material requirement is created or edited in this change
- THEN the system does not reserve stock
- AND does not deduct stock
- AND does not create inventory transactions
- AND does not create purchase requests
- AND does not create supplier or finance records

#### Scenario: Invalid material requirement is rejected in future implementation

- GIVEN a material requirement line has missing material name
- OR has non-positive required quantity
- OR has inconsistent work order/order item references
- WHEN the future implementation validates the request
- THEN it returns `MATERIAL_REQUIREMENT_INVALID`
- AND does not save misleading requirement data

### Requirement: Link work order to production route instance

The system SHALL allow a production work order to be linked to a production route instance.

#### Scenario: New production main flow dispatches from work order

- GIVEN a production work order has been created for an `order_item`
- WHEN future production dispatch is implemented after this OpenSpec
- THEN the new production main flow is `order_item -> production_work_order -> production_route_instance -> production_step_instance`
- AND route instance dispatch should be driven from the work order
- AND the work order may store the resulting `production_route_instance_id`

#### Scenario: Work order exists before dispatch

- GIVEN a work order exists in `DRAFT` or `RELEASED`
- WHEN production dispatch is later performed
- THEN the resulting `production_route_instance` may be linked back to the work order
- AND the work order may store `production_route_instance_id`
- AND the route instance remains the execution source of truth

#### Scenario: Route link must match order item

- GIVEN a work order belongs to one `order_item`
- WHEN a route instance is linked to that work order
- THEN the route instance must belong to the same tenant and `order_item`
- AND mismatched links return `WORK_ORDER_ROUTE_LINK_CONFLICT`

#### Scenario: Existing direct dispatch remains transition-only

- GIVEN the existing `production-dispatch-instance` change already defines direct `order_item -> production_route_instance` behavior
- WHEN this OpenSpec is applied
- THEN this change does not refactor or remove that existing behavior
- AND the legacy direct path may remain during transition
- AND later implementation should gradually move the production main flow to work order-driven dispatch

#### Scenario: Work order does not redefine freeze rules

- WHEN a route instance is linked to a work order
- THEN existing `production-dispatch-instance` freeze rules still apply
- AND the work order does not allow editing frozen route or step structure

### Requirement: Keep inventory deduction out of this change

The system SHALL keep inventory deduction, reservation, stock in/out, purchase, supplier, and finance behavior out of this change.

#### Scenario: Work order material list is demand only

- WHEN this change is applied
- THEN `production_work_order_material` is only a material requirement draft
- AND it does not update `inventory_stock`
- AND it does not create `inventory_transaction`
- AND it does not perform stock reservation or stock deduction

#### Scenario: Purchase and supplier workflows are not introduced

- WHEN material shortages are anticipated
- THEN this change does not create purchase requests
- AND does not create supplier records
- AND does not create accounts payable, costing, or finance records

### Requirement: Prepare for material readiness by step

The system SHALL prepare the work order material model for future material readiness and shortage display by step or usage stage.

#### Scenario: Material requirement can reference usage stage

- WHEN a material requirement is drafted
- THEN it can record `usage_stage`
- AND may later map to a step template or step instance
- AND this enables future shortages to appear near the process node that needs the material

#### Scenario: Future shortage should not block entire work order by default

- WHEN future inventory/material-readiness detects a shortage for a downstream material
- THEN the design expects upstream non-shortage steps may continue
- AND shortage should be displayed on the corresponding future step or usage stage
- AND a shortage should warn or block before starting the affected step, not automatically block the whole work order

#### Scenario: Future readiness owns reservation and shortage status

- WHEN material readiness is implemented later
- THEN that later change owns available quantity checks, shortage state, optional reservation, stock issue, and step-level start blocking or warning
- AND this work order change does not implement those behaviors

### Requirement: Keep nested and parallel process graph out of this change

The system SHALL keep nested, parallel, and non-linear process graph implementation out of this change.

#### Scenario: Current serial route execution remains unchanged

- WHEN this work order change is designed
- THEN it does not change current `step_order` serial execution rules
- AND does not add dependency graph tables
- AND does not add GROUP or TASK node tables
- AND does not implement parallel branches

#### Scenario: Future process graph owns executable leaf node rules

- WHEN a future nested/parallel process graph is designed
- THEN that future change owns GROUP nodes, TASK leaf nodes, dependencies, group progress aggregation, and execution eligibility
- AND only TASK leaf nodes should be allowed to start, complete, or check in
- AND this change only prepares references such as usage stage or optional related step ids

### Requirement: Preserve check-in boundary

The system SHALL keep photo upload, file upload, worker app, and check-in UI out of this change.

#### Scenario: Work order does not own photo evidence

- WHEN workers later submit evidence
- THEN evidence attaches to executable `production_step_instance` records or future TASK leaf nodes
- AND not to the work order as a substitute for step evidence

#### Scenario: File upload remains external to this change

- WHEN this change is applied
- THEN it does not implement `/api/files/upload`
- AND does not implement `/api/files/bind`
- AND does not create or modify `file_asset`
- AND does not implement worker-uniapp or production-h5 upload pages

## DoD

- The OpenSpec change exists under `openspec/changes/production-work-order`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change explicitly states that it does not write business code.
- This change explicitly states that it does not create database migrations.
- This change explicitly states that it does not implement Controller APIs or pages.
- This change explicitly forbids inventory deduction, stock in/out, purchase, supplier, and finance logic.
- This change explicitly forbids non-linear, nested, and parallel process graph implementation.
- This change explicitly forbids photo upload, file upload, `worker-uniapp`, `production-h5`, and `screen-web`.
- This change explicitly forbids CRM, public pool, contribution, order creation, and order core logic.
- Production reads `order_item` and does not modify customer/order core fields.
- `production_work_order` is defined as a production-owned internal document.
- A production work order can reference `order_id` and `order_item_id`.
- A production work order can optionally link to `production_route_instance_id`.
- Work order statuses are defined as `DRAFT`, `RELEASED`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED`.
- Active work order statuses are `DRAFT`, `RELEASED`, and `IN_PROGRESS`.
- Non-active work order statuses are `COMPLETED` and `CANCELLED`.
- One `tenant_id + order_item_id` can have at most one active production work order in MVP.
- Duplicate active work order creation returns `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM` and does not automatically reuse the existing work order.
- Work order number draft rules are defined.
- Production instruction fields are drafted.
- Technical configuration fields are drafted.
- Signature and confirmation fields are drafted.
- `production_work_order_material` is drafted as a material requirement list only.
- Material requirement does not reserve or deduct stock.
- Future inventory/material-readiness owns shortage detection, reservation, stock issue, and step-level readiness behavior.
- Future nested/parallel process graph owns GROUP/TASK nodes and dependency behavior.
- Check-in photo evidence remains attached to executable step instances or future TASK leaf nodes.
- No completion claim may be made without verification evidence.

## Out Of Scope

The system SHALL NOT implement backend business code, migrations, Controller APIs, admin-web pages, `production-h5`, `worker-uniapp`, `screen-web`, inventory deduction, inventory reservation, stock in/out, stock transaction, purchase, supplier, finance, costing, CRM, customer public pool, contribution value, order creation, order amount or quotation modification, customer field modification, order item specification or quantity modification, file upload, photo upload, shared file infrastructure, production step check-in UI, nested process graph, parallel process graph, non-linear execution implementation, attendance, or dashboard in this change.
