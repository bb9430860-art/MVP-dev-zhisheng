# Production Work Order Dispatch Integration Tasks

## Task 1: Confirm scope and prior changes

Steps:

- Re-read `production-work-order`.
- Re-read `production-work-order-api-admin`.
- Re-read `production-dispatch-instance`.
- Re-read `production-step-execution`.
- Confirm this change is OpenSpec-only.
- Confirm no backend code, frontend code, migration, API implementation, inventory, upload, worker app, or customer-line code is created.

DoD:

- Scope is documented in proposal and design.
- Only four OpenSpec files are created.
- Forbidden implementation areas are explicit.

## Task 2: Define work-order-driven dispatch flow

Steps:

- Define new main flow as `order_item -> production_work_order -> production_route_instance -> production_step_instance`.
- Define dispatch entry by `workOrderId`.
- Define how work order context supplies `order_id`, `order_item_id`, and snapshots.
- Define future dispatch context/config/confirm API shapes as drafts only.

DoD:

- `design.md` contains target flow.
- `spec.md` requires dispatch from released work order.
- The flow does not start from direct `orderItemId` as the preferred admin path.

## Task 3: Define status and precondition rules

Steps:

- Define status/action matrix.
- Reject `DRAFT`.
- Allow `RELEASED`.
- Reject `IN_PROGRESS`, `COMPLETED`, and `CANCELLED`.
- Reject work orders that already have `production_route_instance_id`.
- Compare status update Option A and Option B.
- Recommend MVP Option A: dispatch success changes `RELEASED -> IN_PROGRESS`.

DoD:

- Status rules are documented in design and spec.
- Option A/Option B analysis is present.
- Future split states such as `DISPATCHED` or `ISSUED` are future scope.

## Task 4: Define route template reuse and freeze boundary

Steps:

- Define route template selection from work order dispatch.
- Define enabled/non-deleted template requirement.
- Define route template snapshot copy.
- Define step template snapshot copy.
- Define `production_route_instance.frozen = true`.
- Define frozen structure protections.

DoD:

- Existing `production-dispatch-instance` copy/freeze rules are reused.
- Work order cannot mutate frozen route or step structure.
- No nested/parallel process graph is introduced.

## Task 5: Define work order and route instance linking

Steps:

- Define setting `production_work_order.production_route_instance_id`.
- Define MVP single-direction linking through `production_work_order.production_route_instance_id`.
- Define that MVP does not add `production_route_instance.work_order_id`.
- Define optional future `production_route_instance.work_order_id` only through a separate OpenSpec and Flyway migration.
- Define same tenant, same order id, and same order item validation.
- Define route link conflict error.
- Clarify that no migration is created for reverse linking in this change.
- Clarify that manual database structure changes are forbidden.

DoD:

- Link direction is documented.
- MVP single-direction relation is documented.
- No `production_route_instance.work_order_id` migration is required by this change.
- Schema limitations are handled without creating migration in this change.
- `WORK_ORDER_ROUTE_LINK_CONFLICT` is documented.

## Task 6: Define restricted order_item production write-back

Steps:

- Document customer-line restricted write-back contract.
- Allow only `productionStatus`, `productionProgress`, and `productionRouteInstanceId`.
- Define that work-order dispatch must reuse existing direct dispatch initial `production_status` write-back value.
- Define that no new work-order-specific initial production status is invented.
- Exclude item name, spec, unit, quantity, unit price, subtotal, remark, customer fields, order amount, quotation, and order core status.
- Define write-back failure behavior.

DoD:

- `design.md` contains write-back boundary.
- `spec.md` includes restricted write-back scenarios.
- Existing dispatch status semantics are reused.
- Order core mutation remains forbidden.

## Task 7: Define admin-web dispatch entry

Steps:

- Define "dispatch production" action on work order list/detail.
- Show action only for `RELEASED` work orders without route link.
- Hide or disable action for other statuses.
- Define MVP dialog/drawer dispatch entry keyed by `workOrderId`.
- Define template selection, template step loading, simple step adjustment, and confirm dispatch inside the dialog/drawer.
- Mark a dedicated dispatch page as future optional scope.
- Define route instance link display after dispatch.
- Exclude batch dispatch and print/PDF.

DoD:

- Admin interaction draft exists.
- Dialog/drawer MVP approach is documented.
- UI status rules are documented.
- No frontend file is created by this OpenSpec.

## Task 8: Define compatibility with legacy dispatch

Steps:

- Document current direct `order_item -> production_route_instance` flow.
- Mark direct dispatch as legacy/transition.
- Define future admin preference for work-order-driven dispatch.
- Allow future implementation to reuse or extract copy/freeze logic.
- State this change does not remove or refactor legacy dispatch.

DoD:

- Compatibility section exists in design.
- Spec includes legacy dispatch compatibility requirement.
- Existing dispatch remains untouched.

## Task 9: Define inventory boundary

Steps:

- Exclude inventory availability checks.
- Exclude material readiness.
- Exclude stock reservation.
- Exclude stock deduction.
- Exclude stock in/out and `inventory_transaction`.
- Exclude shortage node display and shortage-based step blocking.

DoD:

- Inventory boundary appears in proposal, design, and spec.
- Work-order dispatch remains independent from material readiness.
- Future inventory/material-readiness ownership is clear.

## Task 10: Define process graph boundary

Steps:

- Confirm current serial `step_order` remains.
- Exclude GROUP/TASK node modeling.
- Exclude dependency graph.
- Exclude parallel branches and nested process.
- Exclude check-in graph changes.

DoD:

- Process graph boundary appears in design and spec.
- No dependency or nested process design is introduced.

## Task 11: Define future implementation tests

Steps:

- Define tests for dispatch allowed from `RELEASED`.
- Define tests for rejecting `DRAFT`, `CANCELLED`, `COMPLETED`, and repeated dispatch.
- Define tests for route and step creation using existing freeze rules.
- Define tests for work order route link.
- Define tests for `RELEASED -> IN_PROGRESS`.
- Define tests for restricted order item production write-back only.
- Define tests proving inventory, route graph, batch dispatch, and print/PDF are not implemented.

DoD:

- Test expectations are documented.
- Verification remains required before future implementation can be called complete.

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
- Confirm only the four OpenSpec md files appear.
- Confirm no `.java`, `.sql`, `.vue`, `.ts`, `pom.xml`, or `package.json` changes appear.
- Confirm no implementation completion is claimed.

DoD:

- Verification evidence is available.
- Final report says only that OpenSpec documents are prepared.
- No code, migration, API, page, or commit is produced.

## Overall Acceptance Criteria

- `openspec/changes/production-work-order-dispatch-integration/proposal.md` exists.
- `openspec/changes/production-work-order-dispatch-integration/design.md` exists.
- `openspec/changes/production-work-order-dispatch-integration/spec.md` exists.
- `openspec/changes/production-work-order-dispatch-integration/tasks.md` exists.
- This change is OpenSpec-only.
- No backend Java, migration, frontend Vue/TS, package, or build metadata is changed.
- Work-order-driven dispatch is designed.
- Existing direct dispatch is marked legacy/transition.
- Dispatch status rules are documented.
- MVP `RELEASED -> IN_PROGRESS` after dispatch is documented.
- Work order to route instance linking is documented.
- Restricted order item production write-back is documented.
- Inventory/material-readiness, batch dispatch, print/PDF, nested/parallel graph, upload, worker apps, screen-web, attendance, dashboard, CRM, public pool, contribution, and order core mutation are excluded.
