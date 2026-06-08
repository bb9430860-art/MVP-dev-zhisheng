# Inventory Material Core Tasks

## Task 1: Confirm scope and prior changes

### Steps

- Read `process-route-template`, `process-template-material-requirement`, `production-work-order`, and work-order-driven dispatch context.
- Confirm current material demand is represented by step material templates and `production_work_order_material`.
- Confirm this change only designs inventory core documents.
- Confirm no backend, frontend, migration, package, or pom file is allowed in this change.

### DoD

- Scope is limited to OpenSpec documents.
- Existing production and process implementations are not modified.
- Readiness and shortage are explicitly separated into a future change.

## Task 2: Design material item model

### Steps

- Define `material_item` field draft.
- Decide MVP material code rule.
- Define required fields: material code, material name, and unit.
- Define enabled/disabled behavior.
- Define soft delete and tenant ownership.

### DoD

- `material_item` supports material code, name, spec, unit, category, enabled state, and remark.
- Material code uniqueness is documented.
- Disabled material behavior is documented.

## Task 3: Design stock balance model

### Steps

- Define `inventory_stock` field draft.
- Define tenant and material uniqueness.
- Define `on_hand_qty`, `reserved_qty`, and `available_qty`.
- Define MVP reservation default as `0`.
- Define that stock cannot be changed without transaction.

### DoD

- Stock balance model is documented.
- Available quantity rule is documented.
- Direct balance mutation is forbidden.

## Task 4: Design inventory transaction model

### Steps

- Define `inventory_transaction` field draft.
- Define MVP transaction types.
- Define future transaction types as design-only vocabulary.
- Define before/after quantity fields.
- Define reference and idempotency fields.

### DoD

- Transaction model supports auditability.
- MVP transaction types are limited to manual in, manual out, adjust in, and adjust out.
- Future production consume, purchase, and reservation types are not implemented in this change.

## Task 5: Define stock operation rules

### Steps

- Define manual in behavior.
- Define manual out behavior.
- Define adjustment behavior.
- Define quantity validation.
- Define negative-stock rejection.
- Define transaction boundary for stock and transaction writes.

### DoD

- Every balance change requires `inventory_transaction`.
- `qty > 0` rule is documented.
- Negative stock is forbidden.
- Stock and transaction must be written atomically in future implementation.

## Task 6: Define admin-web inventory management flow

### Steps

- Design material management page or tab.
- Design stock balance page or tab.
- Design transaction history page or tab.
- Design manual in/out/adjustment entry.
- Define UI boundaries for readiness, shortage, purchase, supplier, and finance.

### DoD

- Admin-web inventory management draft is documented.
- The UI does not show readiness, shortage-by-step, purchase, supplier, or finance state in this change.

## Task 7: Define work order readiness boundary

### Steps

- State that inventory core does not read `production_work_order_material` for readiness.
- State that inventory core does not calculate shortage.
- State that inventory core does not block work order release, dispatch, or step start.
- Define future readiness ownership.

### DoD

- Readiness boundary is explicit.
- Future `work-order-material-readiness` owns required vs available, shortage quantity, and step display.

## Task 8: Define production dispatch boundary

### Steps

- State that dispatch does not deduct inventory.
- State that step completion does not deduct inventory.
- State that automatic production consume is future scope.
- Confirm existing dispatch is not modified by this change.

### DoD

- Dispatch boundary is documented.
- Production execution side effects are excluded.

## Task 9: Define process template material boundary

### Steps

- State that this change does not modify `process_step_material_requirement_template`.
- Define future relation between `material_item.id` and template `material_id`.
- State that automatic matching or repair of existing template rows is out of scope.

### DoD

- Process template material boundary is documented.
- Future linkage is described without implementing migration or repair.

## Task 10: Define future implementation API draft

### Steps

- Draft material item APIs.
- Draft stock query APIs.
- Draft transaction query and operation APIs.
- Confirm transaction APIs are the only quantity-changing entry points.

### DoD

- Future API paths are documented.
- Quantity-changing APIs are separated from stock query APIs.

## Task 11: Define future implementation tests

### Steps

- Define tests for material create/update/disable.
- Define tests for duplicate material code rejection.
- Define tests for manual in, manual out, and adjustment.
- Define tests for before/after transaction quantities.
- Define tests for negative-stock rejection.
- Define tests that direct stock update without transaction is not exposed by service/API.
- Define tests that no work order readiness, dispatch blocking, or production consume occurs.
- Define tests that CRM/order/contribution logic is not modified.

### DoD

- Future backend test expectations are clear.
- Future frontend validation expectations are clear.
- Boundary tests are included.

## Task 12: Define verification checklist

### Steps

- Verify `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- Run path-limited `git status --short --untracked-files=all -- openspec/changes/inventory-material-core`.
- Run path-limited `git diff --name-only -- openspec/changes/inventory-material-core`.
- Confirm no Java, SQL, Vue, TypeScript, pom, or package files are touched by this OpenSpec task.
- Confirm no commit or push is performed.

### DoD

- Verification evidence is available.
- The change is OpenSpec-only.
- The final report distinguishes this OpenSpec task from pre-existing dirty worktree files.

## Future Implementation Test Expectations

- `material_name` is required.
- `unit` is required.
- MVP `material_code` is required and unique per tenant for active records.
- Disabled materials reject new stock operations.
- First manual in creates stock row.
- Manual in increases `on_hand_qty`.
- Manual out decreases `on_hand_qty`.
- Adjustment creates `ADJUST_IN` or `ADJUST_OUT`.
- `available_qty = on_hand_qty - reserved_qty`.
- `reserved_qty` defaults to `0` in MVP.
- `qty <= 0` is rejected.
- Negative stock is rejected.
- Every balance change creates `inventory_transaction`.
- Before/after quantities are recorded.
- No work order readiness is calculated.
- No shortage by step is calculated.
- No dispatch or step-start blocking occurs.
- No automatic production consume occurs.
- No CRM, order core, or contribution logic is modified.

## Overall Acceptance Criteria

- Four OpenSpec files exist under `openspec/changes/inventory-material-core`.
- The change designs `material_item`, `inventory_stock`, and `inventory_transaction`.
- The change clearly states that all stock balance changes require transactions.
- The change clearly excludes readiness, shortage calculation, stock reservation, automatic deduction, purchase, supplier, finance, and customer-line logic.
- Verification commands are run before claiming the OpenSpec documents are prepared.
