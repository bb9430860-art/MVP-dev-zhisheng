# Work Order Material Readiness On Create Tasks

## Task 1: Confirm scope and current gap

### Steps

- Review existing process route template, step material requirement, production work order, work order material generation, and inventory material core changes.
- Confirm this change only designs create-time material readiness.
- Confirm current gap is the absence of node-level material readiness preview during work order creation.

### DoD

- Scope is limited to OpenSpec design.
- The current gap is documented.
- No implementation files are changed.

## Task 2: Design create work order readiness flow

### Steps

- Define the create flow from order_item and route template selection to readiness preview.
- Define when generated material demand is written to production_work_order_material.
- Confirm shortage does not block DRAFT creation.

### DoD

- Flow covers route selection, material generation, inventory lookup, preview, and DRAFT creation.
- DRAFT creation remains allowed with shortage.

## Task 3: Design material link rule

### Steps

- Define material_id as the only reliable inventory link.
- Define behavior for material_id null.
- Define admin-web preference for selecting material_item in template material editing.

### DoD

- UNLINKED_MATERIAL behavior is clear.
- The design explicitly avoids material_name fuzzy matching.
- Hand-filled template materials remain allowed but cannot be checked against stock.

## Task 4: Design quantity calculation

### Steps

- Define requiredQty formula from baseQtyPerUnit, fixedQty, lossRate, and quantitySnapshot.
- Define null handling.
- Define required_qty_expression as out of MVP execution.

### DoD

- Calculation rule is deterministic.
- BigDecimal usage is expected for future implementation.
- Invalid or non-positive requiredQty handling is identified.

## Task 5: Design inventory check rules

### Steps

- Define READY, SHORTAGE, UNLINKED_MATERIAL, and NO_STOCK_RECORD statuses.
- Define availableQty and shortageQty calculation.
- Define behavior when inventory_stock row is missing.

### DoD

- Every generated line maps to a readiness status.
- inventory_stock.available_qty is the only stock quantity used for comparison.
- Inventory mutation is excluded.

## Task 6: Design readiness persistence

### Steps

- Compare adding snapshot fields to production_work_order_material with adding a separate readiness table.
- Recommend the MVP persistence option.
- Define required migration review for future implementation.

### DoD

- Option A and Option B are documented.
- MVP recommendation is explicit.
- Manual database changes are prohibited.

## Task 7: Design admin-web create flow

### Steps

- Define route template selection in the create DRAFT work order flow.
- Define preview trigger.
- Define grouped readiness display by process step.
- Define required warning copy.

### DoD

- UI displays step, material, requiredQty, availableQty, shortageQty, and readinessStatus.
- Shortage, unlinked material, and missing stock states have clear prompts.
- The UI states that readiness is only a prompt and does not reserve or deduct stock.

## Task 8: Define DRAFT detail refresh behavior

### Steps

- Define DRAFT work order detail behavior for regenerate, manual supplement, and refresh stock check.
- Define non-DRAFT view-only behavior.
- Leave non-DRAFT readiness refresh as future design if needed.

### DoD

- Only DRAFT can overwrite generated material demand.
- Non-DRAFT behavior is bounded.
- Manual material supplement remains available.

## Task 9: Define release and dispatch boundary

### Steps

- Define shortage behavior for DRAFT creation, release, and dispatch.
- Define step-start guard as future scope.
- Confirm no production status transitions are changed by this design.

### DoD

- Shortage does not block DRAFT creation.
- Shortage does not block release.
- Shortage does not block dispatch.
- Step-start blocking is explicitly out of scope.

## Task 10: Define inventory mutation boundary

### Steps

- Define inventory_stock as read-only during preview and creation.
- Prohibit reservation, deduction, inventory_transaction writes, and stock quantity updates.
- Confirm reserved_qty, on_hand_qty, and available_qty are not changed.

### DoD

- Inventory boundary is explicit.
- No inventory mutation behavior is included.
- Future implementation tests can assert no inventory_transaction and unchanged stock balances.

## Task 11: Define process graph boundary

### Steps

- Define current mapping to process_step_template.
- Define future mapping to TASK leaf nodes.
- Define GROUP as non-consuming.

### DoD

- Current linear step model is supported.
- Future graph model is acknowledged.
- GROUP / TASK / parallel / nested graph implementation is out of scope.

## Task 12: Define API draft

### Steps

- Draft create-time readiness preview API.
- Draft create-with-material-readiness API.
- Compare with alternative create-first then generate-readiness flow.

### DoD

- Request and response shapes are documented.
- MVP recommended flow is clear.
- API draft does not include inventory mutation.

## Task 13: Define future implementation tests

### Steps

- Define tests for route selection and grouped preview.
- Define tests for requiredQty calculation.
- Define tests for inventory linked, unlinked, missing stock, ready, and shortage states.
- Define tests for DRAFT creation with generated production_work_order_material and readiness snapshot.
- Define boundary tests for no reservation, no deduction, no inventory_transaction, no dispatch blocking, and no order core mutation.

### DoD

- Future tests cover successful preview and create.
- Future tests cover all readiness statuses.
- Future tests cover inventory and production boundaries.

## Task 14: Define verification checklist

### Steps

- Verify proposal.md exists.
- Verify design.md exists.
- Verify spec.md exists.
- Verify tasks.md exists.
- Run path-limited git status and git diff checks.
- Confirm no Java, SQL, Vue, TypeScript, pom.xml, or package.json files were changed by this OpenSpec task.

### DoD

- Four markdown files exist under openspec/changes/work-order-material-readiness-on-create.
- Verification evidence is recorded.
- No code, migration, package, or build configuration files are modified by this task.
