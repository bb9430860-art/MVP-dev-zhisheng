# Production Work Order API And Admin Tasks

## Task 1: Confirm scope and boundaries

Steps:

- Re-read `README.md`.
- Re-read `docs/codex-production-line.md`.
- Re-read `docs/cursor-customer-line.md`.
- Re-read `openspec/changes/production-work-order`.
- Confirm backend core already exists and this change only designs API/admin surfaces.
- Confirm no Java, Controller, Mapper, Service, Entity, migration, Vue, TypeScript, route, package, backend core, dispatch, inventory, CRM, contribution, or order core changes are allowed.

DoD:

- Scope is documented in proposal and design.
- Only OpenSpec documents are created.
- Forbidden implementation areas are explicitly listed.

## Task 2: Design work order API endpoints

Steps:

- Draft candidate order item API.
- Draft create-from-order-item API.
- Draft work order list API.
- Draft work order detail API.
- Draft DRAFT base update API.
- Draft DRAFT material update API.
- Draft release API.
- Draft cancel API.
- Draft route instance link API as optional/future-facing.

DoD:

- `design.md` lists endpoint paths and purpose.
- `spec.md` includes API requirements and scenarios.
- Endpoints do not create route instances or mutate order core fields.

## Task 3: Design request and response DTO shape

Steps:

- Draft candidate row fields.
- Draft create request fields.
- Draft material line request fields.
- Draft list row fields.
- Draft detail response contents.
- Include work order status and route link state.

DoD:

- `design.md` includes request/response examples.
- DTO drafts include production instruction, technical configuration, schedule, people, and material requirement fields.
- DTO drafts do not include order amount, quotation, customer editing, inventory deduction, or finance fields.

## Task 4: Design admin work order list page

Steps:

- Draft `/production/work-orders` route intent.
- Draft list columns.
- Draft status filter.
- Draft work order number and keyword search.
- Draft route linked/not linked display.
- Draft create, view, edit, release, and cancel actions.

DoD:

- `design.md` documents list page behavior.
- `spec.md` includes list page scenarios.
- The page design does not create route/menu files in this change.

## Task 5: Design create-from-order-item flow

Steps:

- Define entry from work order list.
- Define order item candidate selector.
- Define snapshot display after selection.
- Define production instruction form.
- Define technical configuration form.
- Define schedule and owner fields.
- Define material requirement entry.
- Define duplicate active work order handling.

DoD:

- `design.md` documents the full interaction flow.
- Duplicate active work order error `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM` is documented.
- The flow reads order item only and does not modify order core fields.

## Task 6: Design work order detail and edit drawer

Steps:

- Draft detail sections.
- Draft edit behavior for DRAFT work orders.
- Draft read-only behavior for RELEASED, IN_PROGRESS, COMPLETED, and CANCELLED work orders.
- Draft how route instance link state appears.
- Draft status action visibility.

DoD:

- `design.md` includes detail and edit behavior.
- `spec.md` includes detail/edit scenarios.
- Released and later edit restrictions are explicit.

## Task 7: Design material requirement editor

Steps:

- Draft material fields.
- Define `material_name` required.
- Define `required_qty > 0`.
- Define replace/update behavior for DRAFT only.
- Define demand-only wording.
- Exclude readiness, reservation, deduction, in/out, purchase, supplier, and finance behavior.

DoD:

- `design.md` includes material editor rules.
- `spec.md` includes material editing scenarios.
- The editor cannot be mistaken for inventory readiness or stock deduction.

## Task 8: Design status actions and error handling

Steps:

- Create a status/action matrix.
- Define release allowed only from DRAFT.
- Define cancel allowed only from DRAFT and RELEASED.
- Define edit allowed only from DRAFT.
- Draft business error handling.
- Define stale-status refresh behavior after errors.

DoD:

- `design.md` includes status/action matrix.
- `spec.md` covers release, cancel, and rejected edit scenarios.
- Error codes are listed for future implementation.

## Task 9: Design dispatch boundary

Steps:

- State this change does not create `production_route_instance`.
- State this change does not freeze route or step structure.
- State this change does not modify serial `step_order`.
- State this change does not refactor existing `production-dispatch-instance`.
- Define route link display and optional link validation only.
- State work-order-driven dispatch is a future change.

DoD:

- Dispatch boundary appears in proposal, design, and spec.
- Existing dispatch behavior remains unchanged by this OpenSpec.
- Future implementation team can identify what not to implement in this change.

## Task 10: Design inventory and material-readiness boundary

Steps:

- Define material requirements as demand only.
- Exclude inventory reservation.
- Exclude stock deduction.
- Exclude stock in/out and inventory transactions.
- Exclude purchase, supplier, and finance.
- State future readiness owns available quantity, shortage, node shortage display, and affected-step start blocking.

DoD:

- Inventory boundary appears in proposal, design, and spec.
- No wording implies stock has been deducted or reserved.
- Future inventory/material-readiness responsibility is clear.

## Task 11: Design order-line and customer-line boundary

Steps:

- Define production candidate API as read-only.
- Define fields production may display from order item.
- Exclude order creation.
- Exclude order amount, quotation, customer, specification, quantity, and order core status mutation.
- Exclude CRM, public pool, and contribution.

DoD:

- Order-line boundary appears in proposal, design, and spec.
- Production work order remains a production-owned internal document.
- Customer-line ownership remains clear.

## Task 12: Define future implementation test expectations

Steps:

- Draft backend API tests.
- Draft service integration expectations for DRAFT edit and material edit.
- Draft frontend/admin interaction tests.
- Include negative tests for duplicate active work order, invalid material, forbidden edit, forbidden inventory effects, forbidden order mutation, and dispatch boundary.

DoD:

- `design.md` includes a test plan draft.
- `tasks.md` documents test expectations.
- The plan requires verification evidence before implementation can be called complete.

## Task 13: Define verification checklist

Steps:

- Verify the change directory exists.
- Verify these four files exist:
  - `proposal.md`
  - `design.md`
  - `spec.md`
  - `tasks.md`
- Run `git status --short --untracked-files=all`.
- Confirm only the four OpenSpec md files appear for this change.
- Confirm no `.java`, `.sql`, `.vue`, `.ts`, `pom.xml`, `package.json`, backend source, frontend source, or migration modifications appear.
- Confirm no API or page completion claim is made.

DoD:

- Verification evidence is recorded before final reporting.
- The final report says only that OpenSpec documents are prepared.
- No implementation completion is claimed.

## Overall Acceptance Criteria

- `openspec/changes/production-work-order-api-admin/proposal.md` exists.
- `openspec/changes/production-work-order-api-admin/design.md` exists.
- `openspec/changes/production-work-order-api-admin/spec.md` exists.
- `openspec/changes/production-work-order-api-admin/tasks.md` exists.
- The change is OpenSpec-only.
- No business code is written.
- No Controller/API is implemented.
- No Mapper, Service, or Entity is changed.
- No Flyway migration is created.
- No Vue or TypeScript files are created or modified.
- No admin-web route/menu is modified.
- No package or Maven metadata is modified.
- Existing production-work-order backend core is not modified.
- Existing production-dispatch-instance is not refactored.
- Inventory deduction, reservation, stock in/out, inventory transaction, purchase, supplier, and finance are excluded.
- CRM, public pool, contribution, order creation, and order core mutation are excluded.
- Nested/parallel process graph, photo upload, file upload, worker-uniapp, production-h5, and screen-web are excluded.
- API/admin design covers list, detail, create from order item, update DRAFT, update materials, release, cancel, candidate order items, and route link display.
- Verification evidence exists before this OpenSpec documentation is reported as prepared.
