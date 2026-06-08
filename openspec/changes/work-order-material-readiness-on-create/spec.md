## ADDED Requirements

### Requirement: Preview node material readiness during work order creation

The system SHALL allow users to preview process-step material demand and inventory readiness while creating a production work order.

#### Scenario: select route template during work order creation

- **WHEN** a user creates a production work order
- **AND** selects a process route template
- **THEN** the system SHALL use that route template as the source for step material demand preview

#### Scenario: preview material demand grouped by step

- **WHEN** the system builds the material readiness preview
- **THEN** it SHALL group material demand by stepTemplateId, stepOrder, stepName, and usageStage

#### Scenario: calculate required quantity

- **WHEN** a step material template has baseQtyPerUnit, fixedQty, or lossRate
- **THEN** the system SHALL calculate requiredQty from quantitySnapshot using the MVP quantity calculation rule

#### Scenario: query inventory for linked materials

- **WHEN** a generated material demand line has material_id
- **THEN** the system SHALL query inventory_stock.available_qty for that material

#### Scenario: mark unlinked material when material_id is missing

- **WHEN** a generated material demand line has no material_id
- **THEN** the system SHALL mark readinessStatus as UNLINKED_MATERIAL
- **AND** it SHALL NOT match inventory by material_name

#### Scenario: shortage does not block create

- **WHEN** one or more preview lines have readinessStatus SHORTAGE, UNLINKED_MATERIAL, or NO_STOCK_RECORD
- **THEN** the system SHALL still allow the user to create a DRAFT production work order

### Requirement: Create DRAFT work order with generated material demand and readiness snapshot

The system SHALL create a DRAFT work order with generated material demand and readiness snapshot after the user confirms the create flow.

#### Scenario: create DRAFT with generated materials

- **WHEN** the user confirms creation with applyGeneratedMaterials enabled
- **THEN** the system SHALL create a DRAFT production work order
- **AND** it SHALL write generated material demand to production_work_order_material

#### Scenario: preserve related_step_template_id

- **WHEN** generated material demand is written to production_work_order_material
- **THEN** each line SHALL preserve the source step_template_id as related_step_template_id

#### Scenario: persist readiness snapshot

- **WHEN** generated material demand is written
- **THEN** the system SHALL persist or expose the readiness snapshot, including availableQty, shortageQty, readinessStatus, and checked time according to the selected persistence design

#### Scenario: do not reserve or deduct stock

- **WHEN** a DRAFT work order is created with generated material demand
- **THEN** the system SHALL NOT reserve or deduct stock

#### Scenario: do not create inventory transaction

- **WHEN** a DRAFT work order is created with generated material demand
- **THEN** the system SHALL NOT create inventory_transaction records

### Requirement: Display material readiness by process step

The admin UI SHALL display material readiness by process step during work order creation and DRAFT work order review.

#### Scenario: group by step order and step name

- **WHEN** readiness results are displayed
- **THEN** the UI SHALL group results by step order and step name

#### Scenario: show requiredQty / availableQty / shortageQty

- **WHEN** a material line is displayed
- **THEN** the UI SHALL show requiredQty, availableQty, and shortageQty where applicable

#### Scenario: show READY / SHORTAGE / UNLINKED_MATERIAL / NO_STOCK_RECORD

- **WHEN** a material line is displayed
- **THEN** the UI SHALL show one of READY, SHORTAGE, UNLINKED_MATERIAL, or NO_STOCK_RECORD

#### Scenario: show warning that readiness is only a prompt

- **WHEN** readiness results are displayed
- **THEN** the UI SHALL show that inventory readiness is only a prompt
- **AND** it SHALL state that stock has not been reserved, deducted, or fully kitted

### Requirement: Preserve production flow boundary

The system SHALL keep material shortage prompts separate from production status transitions and dispatch.

#### Scenario: shortage does not block DRAFT creation

- **WHEN** material shortage exists
- **THEN** the system SHALL NOT block DRAFT work order creation

#### Scenario: shortage does not block release

- **WHEN** material shortage exists
- **THEN** the system SHALL NOT block work order release in this change

#### Scenario: shortage does not block dispatch

- **WHEN** material shortage exists
- **THEN** the system SHALL NOT block production dispatch in this change

#### Scenario: step-start guard is future scope

- **WHEN** future node-level start checks are needed
- **THEN** they SHALL be handled by a later step-start shortage guard change

### Requirement: Preserve inventory mutation boundary

The system SHALL treat inventory as read-only during create-time readiness preview and DRAFT material generation.

#### Scenario: inventory_stock is read only

- **WHEN** readiness is previewed or saved
- **THEN** inventory_stock SHALL only be queried

#### Scenario: no reservation

- **WHEN** readiness is previewed or saved
- **THEN** the system SHALL NOT reserve stock

#### Scenario: no deduction

- **WHEN** readiness is previewed or saved
- **THEN** the system SHALL NOT deduct stock

#### Scenario: no inventory_transaction

- **WHEN** readiness is previewed or saved
- **THEN** the system SHALL NOT write inventory_transaction

#### Scenario: no reserved_qty change

- **WHEN** readiness is previewed or saved
- **THEN** the system SHALL NOT change reserved_qty

### Requirement: Preserve order/customer boundary

The system SHALL avoid customer-line and order-core changes in this change.

#### Scenario: no order core mutation

- **WHEN** readiness is previewed or saved
- **THEN** the system SHALL NOT mutate order amount, quotation, customer, spec, quantity, unit price, subtotal, or order core status

#### Scenario: no CRM

- **WHEN** readiness is previewed or saved
- **THEN** the system SHALL NOT modify CRM behavior or data

#### Scenario: no contribution

- **WHEN** readiness is previewed or saved
- **THEN** the system SHALL NOT modify contribution logic
