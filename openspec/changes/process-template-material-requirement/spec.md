# Process Template Material Requirement Specification

## ADDED Requirements

### Requirement: Define material requirements on process step templates

The system SHALL design material requirement templates that belong to process step templates.

#### Scenario: Add material requirement to step template

- GIVEN a `process_step_template` exists under a `process_route_template`
- WHEN a future admin user adds a material requirement template to that step
- THEN the material requirement records the route template id
- AND records the step template id
- AND records demand fields such as material name, unit, quantity rule, usage stage, and remark

#### Scenario: Edit material requirement

- GIVEN a step material requirement template exists
- WHEN a future admin user edits it
- THEN editable demand fields may be updated
- AND the record remains linked to the same tenant, route template, and step template

#### Scenario: Delete material requirement

- GIVEN a step material requirement template exists
- WHEN a future admin user deletes it
- THEN the future implementation may soft delete or disable the template line
- AND later work order material generation does not use deleted or disabled lines

#### Scenario: Material name is required

- GIVEN a material requirement template is saved
- WHEN `material_name` is blank
- THEN the system rejects it with `MATERIAL_NAME_REQUIRED` or `STEP_MATERIAL_REQUIREMENT_INVALID`

#### Scenario: Unit is required

- GIVEN a material requirement template is saved
- WHEN `unit` is blank
- THEN the system rejects it with `MATERIAL_UNIT_REQUIRED` or `STEP_MATERIAL_REQUIREMENT_INVALID`

#### Scenario: Quantity rule must be valid

- GIVEN a material requirement template is saved
- WHEN `base_qty_per_unit`, `fixed_qty`, `loss_rate`, or future expression values are invalid
- THEN the system rejects it with `MATERIAL_QUANTITY_RULE_INVALID` or `MATERIAL_LOSS_RATE_INVALID`

#### Scenario: Material id can be null before inventory material master exists

- GIVEN material master is not complete
- WHEN a material requirement template is created
- THEN `material_id` may be null
- AND material name and unit still provide a usable demand template

### Requirement: Calculate material demand from order quantity

The system SHALL design how future work order material demand can be calculated from process step material templates and order item quantity.

#### Scenario: Calculate by base quantity per unit

- GIVEN a step material template has `base_qty_per_unit`
- AND the source `order_item.quantity` is known
- WHEN future work order material demand is generated
- THEN `required_qty` is calculated as `base_qty_per_unit * order_item.quantity`

#### Scenario: Include fixed quantity

- GIVEN a step material template has `fixed_qty`
- WHEN future work order material demand is generated
- THEN fixed quantity is added to the quantity-based demand

#### Scenario: Include loss rate

- GIVEN a step material template has `loss_rate`
- WHEN future work order material demand is generated
- THEN the calculated demand includes the loss rate multiplier

#### Scenario: Avoid complex expression engine in MVP

- WHEN MVP material demand generation is implemented in a future change
- THEN it should support simple numeric rules first
- AND it should not require a complex expression engine
- AND `required_qty_expression` remains future optional scope

### Requirement: Map template material requirements to work order materials

The system SHALL design mapping from process step material requirement templates to `production_work_order_material`.

#### Scenario: Generate work order material demand from step template materials

- GIVEN a production work order is created from an order item and selected process template in a future change
- WHEN material demand is generated
- THEN enabled step material requirement templates are mapped into `production_work_order_material`

#### Scenario: Keep related step template id

- WHEN a template material requirement is mapped to work order material demand
- THEN `related_step_template_id` is set to the source `step_template_id`
- AND later shortage display can identify which template step needs the material

#### Scenario: Leave related step instance id empty before dispatch

- GIVEN production route and step instances do not exist before dispatch
- WHEN work order material demand is generated
- THEN `related_step_instance_id` remains null
- AND later readiness or dispatch integration may map it to a step instance

#### Scenario: Do not check inventory

- WHEN template material demand is mapped to work order material demand
- THEN the system does not check available stock
- AND does not calculate shortage

#### Scenario: Do not reserve or deduct stock

- WHEN work order material demand is generated from templates
- THEN the system does not reserve stock
- AND does not deduct stock
- AND does not create stock in/out or `inventory_transaction`

### Requirement: Provide admin editing design for step material templates

The system SHALL design admin editing behavior for material requirements on process step templates.

#### Scenario: Edit materials from step template management

- GIVEN a future admin user edits a route template
- WHEN they manage a step template
- THEN they can add, edit, delete, enable, or disable material requirement template lines for that step

#### Scenario: Show demand-only warning

- WHEN the user edits material requirement templates
- THEN the UI shows that these records are demand templates only
- AND they do not represent stock reservation, stock deduction, or stock readiness

#### Scenario: Do not show stock readiness

- WHEN editing step material templates
- THEN the UI does not show available stock, reserved stock, shortage, or readiness status

### Requirement: Preserve inventory boundary

The system SHALL keep inventory behavior out of this change.

#### Scenario: No inventory availability check

- WHEN material requirement templates are edited or saved
- THEN the system does not query inventory availability

#### Scenario: No reservation

- WHEN material requirement templates are edited or mapped in future design
- THEN the system does not reserve stock

#### Scenario: No deduction

- WHEN material requirement templates are edited or mapped in future design
- THEN the system does not deduct stock

#### Scenario: No inventory transaction

- WHEN material requirement templates are edited or mapped in future design
- THEN the system does not create `inventory_transaction`

#### Scenario: No shortage calculation

- WHEN material requirement templates are edited
- THEN the system does not calculate shortage
- AND shortage calculation belongs to future inventory/material-readiness

### Requirement: Preserve work order boundary

The system SHALL avoid modifying existing work order behavior in this OpenSpec.

#### Scenario: Existing manual work order material editing remains

- GIVEN admin-web can manually edit `production_work_order_material`
- WHEN this change is designed
- THEN that existing manual ability remains unchanged

#### Scenario: Automatic generation from template is future implementation

- WHEN this change is applied
- THEN it does not automatically generate work order material demand
- AND generation from templates requires a later implementation change

#### Scenario: No order core mutation

- WHEN future generation reads order item quantity
- THEN it must not mutate order amount, quotation, customer fields, product specification, product quantity, or order core status

### Requirement: Preserve process graph boundary

The system SHALL keep process graph behavior out of this change.

#### Scenario: Current template material attaches to linear step template

- WHEN this change is designed
- THEN material requirements attach to current linear `process_step_template`
- AND current `step_order` behavior remains unchanged

#### Scenario: Future graph maps material requirements to TASK leaf nodes

- WHEN future route graph is designed
- THEN material requirements should map to executable `TASK` leaf nodes
- AND shortage guard should apply to `TASK` nodes

#### Scenario: No GROUP TASK model in this change

- WHEN this OpenSpec is applied
- THEN it does not introduce GROUP nodes
- AND it does not introduce TASK leaf nodes
- AND it does not implement parallel or nested process execution

## DoD

- The OpenSpec change exists under `openspec/changes/process-template-material-requirement`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change only writes OpenSpec documents.
- No `.java`, `.sql`, `.vue`, `.ts`, `pom.xml`, or `package.json` files are modified by this change.
- Material requirement templates for process step templates are clearly designed.
- Mapping from template material requirements to `production_work_order_material` is documented.
- Inventory boundary is documented.
- Work order boundary is documented.
- Future TASK leaf node boundary is documented.
- Verification evidence is available before claiming the OpenSpec documents are prepared.

## Out Of Scope

The system SHALL NOT implement backend code, frontend code, migrations, Controller APIs, admin-web pages, inventory deduction, inventory reservation, stock in/out, `inventory_transaction`, material readiness, shortage checks, purchase, supplier, finance, production work order code changes, production dispatch code changes, route graph, photo upload, file upload, worker-uniapp, production-h5, screen-web, CRM, public pool, contribution, order creation, order amount or quotation modification, customer field modification, product spec or quantity modification, or order core status mutation in this change.
