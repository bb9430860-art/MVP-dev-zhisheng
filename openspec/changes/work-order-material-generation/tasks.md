# Work Order Material Generation Tasks

## Task 1: Confirm scope and prerequisites

### Steps

- Read `process-route-template`, `process-template-material-requirement`, `production-work-order`, `inventory-material-core`, and work-order-driven dispatch context.
- Confirm step material templates can provide demand source data.
- Confirm `production_work_order_material` is the target demand table.
- Confirm this change is OpenSpec-only.

### DoD

- Scope is limited to OpenSpec documents.
- Prerequisite capabilities are identified.
- No implementation files are modified.

## Task 2: Design generation timing

### Steps

- Compare generation during work order creation with manual DRAFT generation.
- Document Option A for future creation-time generation.
- Document Option B for DRAFT detail preview and apply.
- Choose MVP recommendation.

### DoD

- MVP recommends manual DRAFT preview and apply.
- Rationale is documented.
- Future creation-time generation remains possible.

## Task 3: Design generation input and validation

### Steps

- Define input fields: `workOrderId`, `routeTemplateId`, `replaceExisting`, and `previewOnly`.
- Define tenant and existence checks.
- Define DRAFT-only apply rule.
- Define route template enabled check.
- Define enabled and non-deleted step material template filtering.

### DoD

- Input contract is clear.
- Validation rules are explicit.
- Non-DRAFT replacement is rejected.

## Task 4: Design quantity calculation

### Steps

- Define formula using `quantitySnapshot`.
- Define null handling for numeric fields.
- Define `lossRate` behavior.
- Define `requiredQty > 0`.
- Define `required_qty_expression` MVP handling.
- Define BigDecimal usage for future implementation.

### DoD

- Calculation formula is documented.
- Invalid quantity handling is documented.
- Expression engine is excluded from MVP.

## Task 5: Design mapping to production_work_order_material

### Steps

- Map material identity and snapshot fields.
- Map calculated `required_qty`.
- Map `usage_stage`.
- Map `step_template_id` to `related_step_template_id`.
- Leave `related_step_instance_id` null before dispatch.
- Document schema-inspection requirement if target fields are missing.

### DoD

- Mapping rules are complete.
- Step relation needed for future shortage display is preserved.
- No migration is created in this OpenSpec task.

## Task 6: Design replacement strategy

### Steps

- Define preview as read-only.
- Define MVP apply mode as `replaceExisting=true`.
- Require user confirmation before replacing existing DRAFT materials.
- Document future merge and source tracking options.

### DoD

- MVP replacement strategy is clear.
- Manual editing remains available.
- Merge mode is marked future scope.

## Task 7: Design API draft

### Steps

- Draft preview API path.
- Draft apply API path.
- Define request shape.
- Define response shape.
- Define that preview does not write.
- Define that apply only writes `production_work_order_material`.

### DoD

- Future API paths are documented.
- API side effects are explicit.
- Inventory and dispatch side effects are excluded.

## Task 8: Design admin-web flow

### Steps

- Design DRAFT work order action entry.
- Design route template selection.
- Design preview table fields.
- Design explicit replacement confirmation.
- Define demand-only warning copy.

### DoD

- Admin-web interaction is documented.
- User confirmation is required before replacement.
- UI does not show readiness, shortage, purchase, supplier, or finance state.

## Task 9: Define inventory boundary

### Steps

- State that generation does not query `inventory_stock`.
- State that generation does not reserve or deduct stock.
- State that generation does not write `inventory_transaction`.
- State that generation does not calculate shortage or readiness.
- Assign readiness responsibility to a future change.

### DoD

- Inventory boundary is explicit.
- Future readiness ownership is documented.
- Stock mutation is excluded.

## Task 10: Define dispatch boundary

### Steps

- State that generation does not create route instances.
- State that generation does not create step instances.
- State that generation does not change work order status.
- State that generation does not block dispatch.
- State that `related_step_instance_id` mapping can be future scope.

### DoD

- Dispatch boundary is explicit.
- Work-order-driven dispatch behavior remains unchanged.

## Task 11: Define process graph boundary

### Steps

- State that MVP generation uses linear `process_step_template`.
- State that future graph demand maps to `TASK` leaf nodes.
- State that `GROUP` nodes do not directly consume material.
- Exclude GROUP/TASK, parallel, and nested graph implementation.

### DoD

- Current linear step boundary is documented.
- Future graph migration concept is documented.
- Graph implementation is out of scope.

## Task 12: Define future implementation tests

### Steps

- Define preview tests.
- Define DRAFT apply tests.
- Define non-DRAFT rejection tests.
- Define quantity calculation tests.
- Define mapping tests for `related_step_template_id`.
- Define replacement tests.
- Define inventory boundary tests.
- Define dispatch boundary tests.
- Define order core boundary tests.

### DoD

- Future backend test expectations are clear.
- Future frontend validation expectations are clear.
- Boundary tests are included.

## Task 13: Define verification checklist

### Steps

- Verify `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- Run `git status --short --untracked-files=all -- openspec/changes/work-order-material-generation`.
- Run `git diff --name-only -- openspec/changes/work-order-material-generation`.
- Confirm no Java, SQL, Vue, TypeScript, pom, or package files are touched by this OpenSpec task.
- Confirm no commit or push is performed.

### DoD

- Verification evidence is available.
- The change is OpenSpec-only.
- The final report distinguishes this OpenSpec task from pre-existing dirty worktree files.

## Future Implementation Test Expectations

- Preview returns generated material lines.
- Preview does not write `production_work_order_material`.
- Preview does not query inventory.
- Missing route template is rejected.
- DRAFT work order can apply generated demand.
- Non-DRAFT work order cannot apply generated demand.
- `required_qty` calculation uses `quantitySnapshot`.
- `base_qty_per_unit`, `fixed_qty`, and `loss_rate` are handled correctly.
- `required_qty_expression` is not evaluated in MVP.
- `related_step_template_id` is preserved.
- `related_step_instance_id` remains null before dispatch.
- Existing DRAFT materials are replaced only with explicit request and confirmation.
- Manual material editing remains available.
- No `inventory_stock` query occurs.
- No inventory reservation or deduction occurs.
- No `inventory_transaction` is created.
- No shortage calculation or readiness status is produced.
- No route instance or step instance is created.
- No production status is changed.
- No order core fields are modified.

## Overall Acceptance Criteria

- Four OpenSpec files exist under `openspec/changes/work-order-material-generation`.
- The change clearly designs template-to-work-order material generation.
- Generation timing and MVP recommendation are documented.
- Quantity calculation and mapping rules are documented.
- DRAFT-only application and replacement confirmation are documented.
- Inventory, dispatch, process graph, and order core boundaries are documented.
- Verification commands are run before claiming the OpenSpec documents are prepared.
