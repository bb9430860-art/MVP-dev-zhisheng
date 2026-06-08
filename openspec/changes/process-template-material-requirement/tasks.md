# Process Template Material Requirement Tasks

## Task 1: Confirm scope and prior changes

Steps:

- Re-read process route template context.
- Re-read production work order material context.
- Re-read work-order-driven dispatch boundary.
- Confirm this change is OpenSpec-only.
- Confirm no backend code, frontend code, migration, inventory, production work order implementation, dispatch implementation, or customer-line logic is created.

DoD:

- Scope is documented in proposal and design.
- Only four OpenSpec files are created for this change.
- Forbidden implementation areas are explicit.

## Task 2: Design step material requirement template model

Steps:

- Define the future `process_step_material_requirement_template` table draft.
- Define tenant, route template, and step template ownership.
- Define nullable `material_id`.
- Define required `material_name` and `unit`.
- Define quantity rule fields.
- Define enabled and soft-delete fields.

DoD:

- `design.md` contains field draft and rules.
- `spec.md` requires material requirements on process step templates.
- Material master dependency remains optional.

## Task 3: Design material quantity calculation rules

Steps:

- Define `base_qty_per_unit`.
- Define `fixed_qty`.
- Define `loss_rate`.
- Define simple numeric calculation.
- Mark complex expression engine as future optional scope.

DoD:

- Quantity calculation formula is documented.
- Examples are included.
- MVP avoids complex expression engine.

## Task 4: Design mapping to production_work_order_material

Steps:

- Define source template fields.
- Define target `production_work_order_material` fields.
- Define calculated `required_qty`.
- Preserve `related_step_template_id`.
- Leave `related_step_instance_id` empty before dispatch.
- Define demand-only status as future inventory/material-readiness decision.

DoD:

- Mapping appears in `design.md`.
- `spec.md` includes mapping scenarios.
- Mapping does not check, reserve, or deduct inventory.

## Task 5: Design admin-web template editing flow

Steps:

- Define step-level material editing in process route template management.
- Compare row expansion and step edit tab interaction options.
- Define add, edit, delete, enable, and disable interactions.
- Define fields visible in the editor.
- Define demand-only warning.
- Exclude stock readiness display.

DoD:

- Admin editing draft exists.
- Demand-only warning is documented.
- No admin-web files are created by this OpenSpec.

## Task 6: Define inventory boundary

Steps:

- Exclude inventory availability checks.
- Exclude reservation.
- Exclude deduction.
- Exclude stock in/out.
- Exclude `inventory_transaction`.
- Exclude shortage calculation and shortage node display.

DoD:

- Inventory boundary appears in proposal, design, and spec.
- Future inventory/material-readiness ownership is clear.
- Template editing remains independent from stock state.

## Task 7: Define work order boundary

Steps:

- Preserve existing manual work order material editing.
- Define automatic generation from templates as future implementation.
- Clarify that this change does not modify `production_work_order` APIs, pages, services, or schema.
- Clarify no order core mutation.

DoD:

- Work order boundary appears in design and spec.
- Existing `production_work_order_material` manual editing remains unchanged.
- Future generation is separated into a later change.

## Task 8: Define dispatch boundary

Steps:

- Confirm work-order-driven dispatch remains unchanged.
- Confirm template material requirements do not block dispatch in this change.
- Confirm no route instance or step instance logic changes.
- Confirm no shortage guard is implemented.

DoD:

- Dispatch boundary appears in design.
- No production dispatch implementation is included.
- Dispatch and material readiness remain separate concerns.

## Task 9: Define future process graph boundary

Steps:

- Confirm current model attaches material requirements to linear `process_step_template`.
- Define future mapping to `TASK` leaf nodes.
- Clarify `GROUP` nodes do not directly consume material.
- Exclude GROUP/TASK, dependency graph, parallel, and nested process implementation.

DoD:

- Process graph boundary appears in design and spec.
- Future TASK node relationship is clear.
- No process graph implementation is included.

## Task 10: Define future implementation API draft

Steps:

- Draft API for listing route template step materials.
- Draft API for replacing one step template's material requirement list.
- Draft API for material demand preview.
- Mark all APIs as future design only.

DoD:

- API draft appears in design.
- No Controller/API is implemented.
- Preview API remains optional future scope.

## Task 11: Define future implementation tests

Steps:

- Define tests for required `material_name`.
- Define tests for required `unit`.
- Define tests for `base_qty_per_unit`, `fixed_qty`, and `loss_rate` validation.
- Define tests proving `material_id` can be null.
- Define tests for correct `required_qty` calculation when generating work order material demand.
- Define tests preserving `related_step_template_id`.
- Define tests proving inventory is not queried, deducted, or transacted.
- Define tests proving dispatch is not affected.
- Define tests proving order core fields are not mutated.

DoD:

- Future test expectations are documented.
- Tests cover validation, generation, and boundaries.
- No implementation tests are created in this OpenSpec-only change.

## Task 12: Define verification checklist

Steps:

- Verify the change directory exists.
- Verify these files exist:
  - `proposal.md`
  - `design.md`
  - `spec.md`
  - `tasks.md`
- Run `git status --short --untracked-files=all`.
- Run `git diff --name-only`.
- Confirm this OpenSpec only adds four md files under `openspec/changes/process-template-material-requirement/`.
- Confirm no additional `.java`, `.sql`, `.vue`, `.ts`, `pom.xml`, or `package.json` files are modified by this change.
- Confirm no implementation completion is claimed.

DoD:

- Verification evidence is available.
- Final report says only that OpenSpec documents are prepared.
- No code, migration, API, page, commit, or push is produced.

## Overall Acceptance Criteria

- `openspec/changes/process-template-material-requirement/proposal.md` exists.
- `openspec/changes/process-template-material-requirement/design.md` exists.
- `openspec/changes/process-template-material-requirement/spec.md` exists.
- `openspec/changes/process-template-material-requirement/tasks.md` exists.
- This change is OpenSpec-only.
- Template material requirement design is documented.
- Mapping to `production_work_order_material` is documented.
- Inventory boundary is documented.
- Work order boundary is documented.
- Dispatch boundary is documented.
- Future TASK leaf node boundary is documented.
- No backend Java, SQL, migration, frontend Vue/TS, package, or build metadata is changed by this OpenSpec change.
