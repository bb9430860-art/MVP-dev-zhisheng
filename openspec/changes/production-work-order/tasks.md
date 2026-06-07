# Production Work Order Tasks

## Task 1: Confirm scope and boundary

Steps:

- Re-read `README.md`.
- Re-read `docs/codex-production-line.md`.
- Re-read `docs/cursor-customer-line.md`.
- Re-read prior production changes:
  - `production-dispatch-instance`
  - `production-step-execution`
  - `production-step-checkin-photo`
- Confirm this change is OpenSpec-only.
- Confirm no backend implementation, frontend implementation, database migration, Controller, page, worker app, upload flow, or inventory logic is created in this change.
- Confirm CRM, public pool, contribution, order creation, and order core logic remain Cursor/customer-line scope.
- Confirm production may read `order_item` but cannot modify order core fields.
- Confirm future new production main flow is `order_item -> production_work_order -> production_route_instance -> production_step_instance`.
- Confirm this OpenSpec does not refactor existing `production-dispatch-instance` direct dispatch behavior.
- Confirm legacy direct `order_item -> production_route_instance` may exist during transition.

DoD:

- The change contains only OpenSpec documents.
- No backend or frontend implementation file is modified.
- No migration file is created.
- The Codex/Cursor business boundary is documented.
- The implementation team can identify forbidden modules before coding.

## Task 2: Design production work order model

Steps:

- Define the purpose of `production_work_order`.
- Define it as a production-owned internal document.
- Draft fields for:
  - identity
  - tenant
  - work order number
  - order references
  - optional route instance reference
  - production snapshots
  - status
  - instruction fields
  - technical fields
  - responsible people
  - confirmation fields
  - audit and soft-delete fields
- Define MVP uniqueness for one active work order per `tenant_id + order_item_id`.
- Define active statuses as `DRAFT`, `RELEASED`, and `IN_PROGRESS`.
- Define non-active statuses as `COMPLETED` and `CANCELLED`.
- Define duplicate active creation as a business error, not automatic reuse.
- Define indexes for future implementation.
- State that no migration is created in this OpenSpec change.

DoD:

- `design.md` includes a `production_work_order` table draft.
- The draft supports `order_id` and `order_item_id`.
- The draft supports optional `production_route_instance_id`.
- The draft makes the work order production-owned.
- The draft does not include customer commercial ownership fields such as order amount or quotation.
- The draft allows only one active work order per `tenant_id + order_item_id` in MVP.
- The draft does not create a migration.

## Task 3: Design work order status machine

Steps:

- Define statuses:
  - `DRAFT`
  - `RELEASED`
  - `IN_PROGRESS`
  - `COMPLETED`
  - `CANCELLED`
- Define allowed MVP transitions.
- Define terminal statuses.
- Define rejected transitions.
- Define `RELEASED` as covering confirmed, issued, and available for production preparation in MVP.
- Explicitly reject `IN_PROGRESS -> CANCELLED`, `COMPLETED -> CANCELLED`, `COMPLETED -> DRAFT`, and `RELEASED -> DRAFT`.
- Define suggested future error codes.
- Explicitly exclude reopen, pause, reissue, and approval workflow from this change.

DoD:

- `design.md` documents the state machine.
- `spec.md` includes scenarios affected by status and duplicate active work order rules.
- Status machine does not introduce BPM or approval workflow.
- `COMPLETED` and `CANCELLED` are terminal for MVP.
- `RELEASED` covers confirmed, issued, and available for production preparation in MVP.
- Forbidden backward or late cancel transitions are documented.

## Task 4: Design production instruction fields

Steps:

- Draft production instruction fields.
- Include production requirement, quality requirement, packaging requirement, shipping requirement, and delivery requirement.
- Include planned start date, planned finish date, required delivery date, and deadline remark.
- Include responsible user, handler, production manager, and optional primary worker.
- Clarify that these are production-side planning fields.
- Clarify that they do not modify order-line delivery, quotation, customer, or product fields.

DoD:

- `design.md` includes production instruction field draft.
- `spec.md` requires recording production instruction fields.
- The fields support a production instruction sheet use case.
- The fields do not move order ownership into production.

## Task 5: Design technical configuration fields

Steps:

- Draft a small set of explicit technical fields for common high-frequency values that need querying or filtering.
- Include examples:
  - equipment model
  - technical configuration summary
  - technical configuration remark
- Define optional `technical_config_json`.
- Define that `technical_config_json` may carry CNC system, compensation method, cylinder brand, motor brand, valve group brand, oil pump brand, mold or blade, random accessories, machine color, shipping requirement, and other non-standard configuration.
- Define what must not be stored in the technical JSON field.
- Clarify when a JSON value should later become a typed column.

DoD:

- `design.md` includes technical configuration field draft.
- `spec.md` requires technical configuration fields.
- The design covers the demonstrated production instruction sheet without solidifying every field into columns.
- The JSON extension is production-only and does not store CRM, quotation, contribution, customer contact, or finance data.

## Task 6: Design signature and confirmation fields

Steps:

- Draft release confirmation fields.
- Draft internal production signature fields.
- Draft warehouse confirmation fields.
- Draft quality confirmation fields.
- Define customer acceptance placeholder fields only if needed for later planning.
- Clarify these fields are internal records only.
- Exclude legal electronic signature, upload, approval workflow, and customer acceptance workflow.

DoD:

- `design.md` includes signature and confirmation field draft.
- `spec.md` includes a scenario for signature and confirmation draft fields.
- The design does not implement legal signature or approval workflow.
- The design does not require file upload or signature image upload.

## Task 7: Design material requirement model

Steps:

- Define `production_work_order_material` purpose.
- Draft fields for:
  - work order reference
  - order reference
  - material reference
  - material name
  - spec
  - unit
  - required quantity
  - usage stage
  - optional related step template id
  - optional related step instance id
  - requirement status
  - remark
  - audit fields
- Define requirement status for this change as demand draft or confirmed demand only.
- Define validation expectations for positive quantity and required material name.
- Explicitly state that this model does not reserve or deduct stock.

DoD:

- `design.md` includes `production_work_order_material` table draft.
- `spec.md` requires material requirement draft records.
- Material requirement is clearly demand only.
- The model does not create inventory transaction, reservation, purchase, supplier, or finance scope.

## Task 8: Design order_item read-only contract

Steps:

- Define the minimum `order_item` fields production may read.
- Define the production-owned port concept such as `OrderItemReadPort`.
- Define that production work order creation must not create an order.
- Define that production work order creation must not modify order amount, quotation, product specification, product quantity, customer fields, customer source, or order custom fields.
- Define production-side snapshots copied into the work order for display.
- Clarify that snapshots are not order-line source of truth.

DoD:

- `design.md` documents the `order_item` read boundary.
- `spec.md` includes preserve order-line ownership scenarios.
- Production can create work orders without owning order core.
- The design does not add order creation, CRM, public pool, or contribution behavior.

## Task 9: Design relation with production dispatch and route instances

Steps:

- Define that work order can exist before dispatch.
- Define optional `production_route_instance_id`.
- Define that future new dispatch should be driven from the work order.
- Define how a later dispatch may link a route instance to the work order.
- Define route link validation by tenant and `order_item`.
- State that `production-dispatch-instance` still owns snapshot copy and freeze rules.
- State that the work order does not unlock editing frozen route or step structure.
- State that step execution remains on `production_step_instance`.
- State that this OpenSpec does not refactor existing direct `order_item -> production_route_instance` behavior.
- State that legacy direct dispatch may exist during transition.

DoD:

- `design.md` documents the work order to route instance relation.
- `spec.md` includes route instance link scenarios.
- Existing frozen route behavior remains unchanged.
- Work order does not become the execution source of truth for steps.
- New production main flow is documented as work order-driven dispatch.
- Legacy direct dispatch transition boundary is documented.

## Task 10: Design future inventory/material readiness boundary

Steps:

- Define that this change only records material requirements.
- Define that future inventory/material-readiness owns:
  - available quantity check
  - shortage state
  - optional reservation
  - stock issue
  - stock deduction
  - shortage display by usage stage or step
  - warning or blocking before affected step start
- Define that material shortage should not block the whole work order by default.
- Define how `usage_stage`, `related_step_template_id`, or `related_step_instance_id` prepares the future mapping.

DoD:

- `design.md` documents the inventory/material-readiness boundary.
- `spec.md` requires keeping inventory deduction out of this change.
- `spec.md` requires preparation for material readiness by step.
- The design explicitly excludes stock in/out, reservation, purchase, supplier, and finance.

## Task 11: Design future nested/parallel process graph boundary

Steps:

- Define that this change does not implement non-linear process execution.
- Define future process graph concepts:
  - GROUP nodes
  - TASK leaf nodes
  - parent-child structure
  - dependencies
  - parallel branches
  - group progress aggregation
- Define that only TASK leaf nodes can start, complete, or check in in the future model.
- Confirm current serial `step_order` execution remains unchanged.
- Clarify that material usage references are preparation only.

DoD:

- `design.md` documents the nested/parallel process graph boundary.
- `spec.md` requires keeping nested and parallel graph implementation out of this change.
- No dependency table or graph status machine is introduced.
- Existing serial execution is not modified.

## Task 12: Define verification checklist

Steps:

- Verify the OpenSpec change directory exists.
- Verify all four files exist:
  - `proposal.md`
  - `design.md`
  - `spec.md`
  - `tasks.md`
- Verify this change states no-code and no-migration scope.
- Verify no backend/frontend implementation files changed.
- Verify no migration file was created.
- Verify forbidden scope is explicitly listed.
- Verify hidden or bidirectional Unicode controls are absent.
- Verify no completion claim is made without verification evidence.

DoD:

- File existence checks pass.
- Diff checks show only `openspec/changes/production-work-order/` files for this task.
- Scope guardrails are present in proposal, design, spec, and tasks.
- Verification evidence is available before any completion wording is used.

## Task 13: Review out-of-scope protection

Steps:

- Review proposal, design, spec, and tasks for accidental CRM scope.
- Review for accidental public pool scope.
- Review for accidental contribution scope.
- Review for accidental order creation or order core mutation.
- Review for accidental inventory deduction, stock transaction, purchase, supplier, or finance scope.
- Review for accidental nested/parallel graph implementation.
- Review for accidental photo upload, file upload, worker-uniapp, production-h5, or screen-web scope.
- Review for accidental attendance or dashboard scope.

DoD:

- Out-of-scope list is present in all relevant documents.
- Production work order remains a production-owned internal document.
- Material requirement remains demand only.
- Future inventory/material-readiness owns shortage and readiness behavior.
- Future process graph owns GROUP/TASK/dependency behavior.
- Order-line ownership is preserved.

## Overall Acceptance Criteria

- An OpenSpec change exists at `openspec/changes/production-work-order`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` all exist.
- This round explicitly does not write code.
- This round explicitly does not create migrations.
- This round explicitly does not implement Controller APIs or pages.
- Inventory deduction, stock in/out, purchase, supplier, and finance are explicitly forbidden.
- Non-linear, nested, and parallel process graph implementation is explicitly forbidden.
- Photo upload, file upload, `worker-uniapp`, `production-h5`, and `screen-web` are explicitly forbidden.
- CRM, public pool, contribution, order creation, and order core logic are explicitly forbidden.
- Production only reads `order_item` and does not modify order core fields.
- Future new production main flow is `order_item -> production_work_order -> production_route_instance -> production_step_instance`.
- Existing direct `order_item -> production_route_instance` dispatch is not refactored by this OpenSpec and may exist during transition.
- One `tenant_id + order_item_id` can have at most one active work order in MVP.
- Active statuses are `DRAFT`, `RELEASED`, and `IN_PROGRESS`.
- Non-active statuses are `COMPLETED` and `CANCELLED`.
- Duplicate active work order creation returns a business error and does not automatically reuse the existing work order.
- `production_work_order` is defined as production's internal document.
- `production_work_order_material` is defined as material requirement demand only.
- Material requirement does not reserve or deduct stock.
- Future inventory/material-readiness handles shortage and node readiness.
- Future nested/parallel process graph handles GROUP/TASK/dependency modeling.
- No completion claim is allowed without verification evidence.
