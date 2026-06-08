# Inventory Material Core Specification

## ADDED Requirements

### Requirement: Manage material items

The system SHALL design material master data for inventory core.

#### Scenario: Create material

- GIVEN an admin user creates a material item in a future implementation
- WHEN material code, material name, unit, specification, category, and remark are submitted
- THEN the system creates `material_item`
- AND records tenant, enabled state, audit fields, and soft delete fields

#### Scenario: Update material

- GIVEN a material item exists
- WHEN editable master data is updated
- THEN material name, specification, unit, category, remark, and enabled state may be changed
- AND historical transactions remain unchanged

#### Scenario: Material name required

- GIVEN a material item is saved
- WHEN `material_name` is blank
- THEN the system rejects it with `MATERIAL_NAME_REQUIRED`

#### Scenario: Unit required

- GIVEN a material item is saved
- WHEN `unit` is blank
- THEN the system rejects it with `MATERIAL_UNIT_REQUIRED`

#### Scenario: Duplicate material code rejected

- GIVEN MVP requires `material_code` to be unique within one tenant
- WHEN another active material uses the same `material_code`
- THEN the system rejects the save with `MATERIAL_CODE_DUPLICATED`

#### Scenario: Disable material

- GIVEN a material item exists
- WHEN it is disabled
- THEN existing stock and historical transactions remain queryable
- AND the material cannot receive new manual in, manual out, or adjustment operations

#### Scenario: Disabled material cannot receive new stock operations

- GIVEN `material_item.enabled = false`
- WHEN a user attempts a new stock operation
- THEN the system rejects it with `MATERIAL_DISABLED`

### Requirement: Track inventory stock balance

The system SHALL design current stock balance for each tenant and material.

#### Scenario: Create stock row when first inbound transaction occurs

- GIVEN a material has no stock row
- WHEN a manual inbound transaction is created
- THEN the system creates `inventory_stock`
- AND initializes quantities from the inbound operation

#### Scenario: On hand quantity increases on manual in

- GIVEN a stock row exists
- WHEN manual stock in with `qty > 0` is recorded
- THEN `on_hand_qty` increases by `qty`
- AND `reserved_qty` remains unchanged
- AND `available_qty` is recalculated

#### Scenario: On hand quantity decreases on manual out

- GIVEN a stock row exists
- WHEN manual stock out with `qty > 0` is recorded
- THEN `on_hand_qty` decreases by `qty`
- AND `available_qty` is recalculated

#### Scenario: Negative stock rejected

- GIVEN a stock operation would make `on_hand_qty` negative
- WHEN the operation is submitted
- THEN the system rejects it with `INVENTORY_NEGATIVE_STOCK_NOT_ALLOWED` or `INVENTORY_INSUFFICIENT_STOCK`

#### Scenario: Available qty equals on hand minus reserved

- GIVEN a stock row exists
- WHEN stock is queried
- THEN `available_qty = on_hand_qty - reserved_qty`
- AND MVP `reserved_qty` defaults to `0` because reservation is not implemented

### Requirement: Record inventory transactions

The system SHALL design stock transaction records for every stock balance change.

#### Scenario: Manual in creates transaction

- GIVEN manual stock in is accepted
- WHEN `inventory_stock` is updated
- THEN an `inventory_transaction` with `transaction_type = MANUAL_IN` is created
- AND before and after quantities are recorded

#### Scenario: Manual out creates transaction

- GIVEN manual stock out is accepted
- WHEN `inventory_stock` is updated
- THEN an `inventory_transaction` with `transaction_type = MANUAL_OUT` is created
- AND before and after quantities are recorded

#### Scenario: Adjustment creates transaction

- GIVEN inventory adjustment is accepted
- WHEN stock is adjusted upward
- THEN `ADJUST_IN` is recorded
- WHEN stock is adjusted downward
- THEN `ADJUST_OUT` is recorded

#### Scenario: Before and after quantities recorded

- WHEN a stock transaction is created
- THEN it records `before_on_hand_qty`
- AND `after_on_hand_qty`
- AND `before_reserved_qty`
- AND `after_reserved_qty`

#### Scenario: Direct stock update without transaction forbidden

- WHEN stock balance changes
- THEN the change must be paired with `inventory_transaction`
- AND direct balance mutation without transaction is forbidden

### Requirement: Provide admin inventory management design

The system SHALL design admin-web inventory management behavior.

#### Scenario: Material list

- GIVEN an admin user opens inventory management
- THEN they can view material items
- AND create, edit, enable, or disable material items in future implementation

#### Scenario: Stock list

- GIVEN inventory stock exists
- WHEN the admin opens stock balance
- THEN the UI shows material context and current `on_hand_qty`, `reserved_qty`, and `available_qty`

#### Scenario: Transaction list

- GIVEN stock transactions exist
- WHEN the admin opens transaction history
- THEN the UI shows transaction type, quantity, before/after quantities, operator, reason, reference, and occurrence time

#### Scenario: Manual in out adjust entry

- GIVEN an enabled material exists
- WHEN the admin records manual in, manual out, or adjustment
- THEN the future implementation updates stock through transaction APIs only

### Requirement: Preserve work order readiness boundary

The system SHALL keep work order readiness out of inventory core.

#### Scenario: No work order material readiness calculation

- WHEN inventory core is designed
- THEN it does not compare `production_work_order_material.required_qty` with stock

#### Scenario: No shortage by step

- WHEN inventory core is designed
- THEN it does not calculate shortage by `related_step_template_id`, `related_step_instance_id`, or `usage_stage`

#### Scenario: No dispatch blocking

- WHEN production dispatch occurs
- THEN inventory core does not block dispatch because of shortage

#### Scenario: No step-start blocking

- WHEN a production step starts
- THEN inventory core does not block step start
- AND step-start shortage guard belongs to a future readiness change

### Requirement: Preserve production boundary

The system SHALL keep production execution side effects out of inventory core.

#### Scenario: No automatic production consume

- WHEN production work order, route instance, or step instance changes
- THEN inventory core does not automatically consume material

#### Scenario: No inventory deduction on dispatch

- WHEN a work order is dispatched
- THEN inventory core does not deduct stock

#### Scenario: No inventory deduction on step complete

- WHEN a production step is completed
- THEN inventory core does not deduct stock

### Requirement: Preserve customer-line boundary

The system SHALL not modify customer-line logic.

#### Scenario: No CRM

- WHEN inventory core is designed
- THEN it does not implement CRM, customer public pool, or customer archive behavior

#### Scenario: No order core mutation

- WHEN inventory core is designed
- THEN it does not create orders
- AND does not change order amount, quotation, customer fields, product specification, product quantity, or order core status

#### Scenario: No contribution logic

- WHEN inventory core is designed
- THEN it does not read or write contribution account or contribution transaction logic

## DoD

- The OpenSpec change exists under `openspec/changes/inventory-material-core`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change only writes OpenSpec documents.
- No `.java`, `.sql`, `.vue`, `.ts`, `pom.xml`, or `package.json` files are modified by this change.
- `material_item`, `inventory_stock`, and `inventory_transaction` are clearly designed.
- All balance changes require `inventory_transaction`.
- Work order readiness, shortage calculation, and node blocking are explicitly out of scope.
- Production dispatch and production execution do not deduct inventory in this change.
- Verification evidence is available before claiming the OpenSpec documents are prepared.

## Out Of Scope

The system SHALL NOT implement backend code, frontend code, migrations, Controller APIs, admin-web pages, work order material readiness, shortage calculation, shortage display by step, dispatch blocking, step-start blocking, automatic production consume, stock reservation, purchase, supplier, finance, cost accounting, multi-warehouse/bin complexity, barcode/RFID, full stocktaking workflow, CRM, public pool, contribution, order creation, order amount/quotation/customer/spec/quantity/core status mutation, worker-uniapp, production-h5, or screen-web in this change.
