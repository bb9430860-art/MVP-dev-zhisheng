# Work Order Material Generation Specification

## ADDED Requirements

### Requirement: Preview work order material generation from process template

The system SHALL design a preview flow that calculates work order material demand from a selected process route template without writing data.

#### Scenario: Preview generated materials

- GIVEN a DRAFT production work order exists
- AND an enabled process route template has enabled step material requirement templates
- WHEN a future admin user previews material generation for the work order and route template
- THEN the system returns generated material demand lines
- AND each line includes material context, usage stage, related step template id, and calculated required quantity

#### Scenario: Preview does not write production work order material

- GIVEN a work order has existing `production_work_order_material` rows
- WHEN material generation preview is requested
- THEN no `production_work_order_material` rows are inserted, updated, deleted, or replaced

#### Scenario: Preview does not query inventory

- WHEN material generation preview is requested
- THEN the system does not query `inventory_stock`
- AND does not calculate availability or shortage

#### Scenario: Preview rejects missing route template

- GIVEN a work order exists
- WHEN preview is requested with a missing route template
- THEN the system rejects the request with `PROCESS_ROUTE_TEMPLATE_NOT_FOUND`

### Requirement: Generate work order materials from process step material templates

The system SHALL design applying generated material demand from process step material templates into DRAFT work order material demand.

#### Scenario: Generate materials for DRAFT work order

- GIVEN a production work order is in `DRAFT`
- AND an enabled route template has enabled step material requirement templates
- WHEN the user applies generated material demand
- THEN the system writes generated rows to `production_work_order_material`
- AND the generated rows belong to the work order

#### Scenario: Reject non-DRAFT work order

- GIVEN a production work order is `RELEASED`, `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`
- WHEN generated material demand is applied
- THEN the system rejects the request with `WORK_ORDER_NOT_DRAFT`
- AND existing material demand is not changed

#### Scenario: Calculate required qty from quantity snapshot

- GIVEN a work order has `quantitySnapshot`
- AND a step material template has `base_qty_per_unit`, `fixed_qty`, and `loss_rate`
- WHEN generated material demand is calculated
- THEN `required_qty = (base_qty_per_unit * quantitySnapshot + fixed_qty) * (1 + loss_rate)`
- AND null numeric rule fields are treated as `0`
- AND the result must be greater than `0`

#### Scenario: Preserve related step template id

- GIVEN a generated material demand line comes from a step material template
- WHEN it is written to `production_work_order_material`
- THEN `related_step_template_id` is set to the source `step_template_id`

#### Scenario: Leave related step instance id empty

- GIVEN production route and step instances are not created by this change
- WHEN generated material demand is written
- THEN `related_step_instance_id` remains null

#### Scenario: Replace existing DRAFT materials when requested

- GIVEN a DRAFT work order already has material demand
- WHEN the user applies generated demand with `replaceExisting=true`
- THEN current DRAFT work order material demand is replaced by generated demand
- AND replacement requires explicit confirmation in admin-web

### Requirement: Preserve manual material editing

The system SHALL preserve existing manual work order material editing behavior.

#### Scenario: Existing manual editing remains

- GIVEN admin-web already supports editing DRAFT work order materials manually
- WHEN this generation design is applied in future implementation
- THEN manual editing remains available for DRAFT work orders

#### Scenario: Replacement requires explicit confirmation

- GIVEN a DRAFT work order has existing material demand
- WHEN the user chooses to apply generated demand
- THEN the UI must warn that current DRAFT material demand will be replaced
- AND the user must explicitly confirm before applying

#### Scenario: Future merge mode is out of scope

- WHEN this MVP generation design is implemented
- THEN it does not need to merge generated and manual lines
- AND merge mode or source tracking requires a future change

### Requirement: Preserve inventory boundary

The system SHALL keep inventory checks and stock mutation out of work order material generation.

#### Scenario: No inventory stock query

- WHEN previewing or applying generated material demand
- THEN the system does not query `inventory_stock`

#### Scenario: No reservation

- WHEN generated material demand is applied
- THEN the system does not reserve stock

#### Scenario: No deduction

- WHEN generated material demand is applied
- THEN the system does not deduct stock

#### Scenario: No inventory transaction

- WHEN generated material demand is previewed or applied
- THEN the system does not create `inventory_transaction`

#### Scenario: No shortage calculation

- WHEN generated material demand is previewed or applied
- THEN the system does not calculate `shortage_qty`
- AND does not assign readiness status

### Requirement: Preserve dispatch boundary

The system SHALL keep production dispatch behavior unchanged.

#### Scenario: No dispatch blocking

- WHEN material demand is generated for a DRAFT work order
- THEN it does not create a shortage decision
- AND it does not block future release or dispatch

#### Scenario: No route instance creation

- WHEN material demand is previewed or applied
- THEN the system does not create `production_route_instance`

#### Scenario: No step instance creation

- WHEN material demand is previewed or applied
- THEN the system does not create `production_step_instance`

#### Scenario: No production status change

- WHEN material demand is previewed or applied
- THEN the system does not change work order status
- AND does not change order item production status or progress

### Requirement: Preserve process graph boundary

The system SHALL keep generation attached to current linear process step templates and leave graph behavior to future changes.

#### Scenario: Attach generated demand to current step template id

- GIVEN current route templates use linear `process_step_template`
- WHEN material demand is generated
- THEN generated demand references the current `step_template_id`

#### Scenario: Future graph maps to TASK leaf nodes

- WHEN a future process graph model exists
- THEN material demand should map to executable `TASK` leaf nodes
- AND `GROUP` nodes should not directly consume material

#### Scenario: No GROUP TASK model in this change

- WHEN this change is applied
- THEN it does not introduce `GROUP` nodes
- AND it does not introduce `TASK` nodes
- AND it does not implement parallel or nested process execution

## DoD

- The OpenSpec change exists under `openspec/changes/work-order-material-generation`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change only writes OpenSpec documents.
- No `.java`, `.sql`, `.vue`, `.ts`, `pom.xml`, or `package.json` files are modified by this change.
- Generation timing is documented.
- Quantity calculation rules are documented.
- Mapping rules are documented.
- Inventory boundary is documented.
- Dispatch boundary is documented.
- DRAFT-only application restriction is documented.
- Verification evidence is available before claiming the OpenSpec documents are prepared.

## Out Of Scope

The system SHALL NOT implement backend code, frontend code, migrations, Controller APIs, Service logic, Mapper logic, Entity changes, admin-web pages, inventory stock checks, inventory reservation, inventory deduction, inventory transactions, shortage calculation, readiness status, step shortage display, dispatch blocking, route instance creation, step instance creation, production status mutation, order core mutation, CRM, public pool, contribution, purchase, supplier, finance, process graph `GROUP` / `TASK`, parallel or nested process execution, worker-uniapp, production-h5, or screen-web in this change.
