# Production Step Execution Tasks

## Task 1: Confirm scope and boundary

Steps:

- Re-read `README.md`, `docs/codex-production-line.md`, `docs/cursor-customer-line.md`, and prior production OpenSpec changes.
- Confirm this change is OpenSpec-only.
- Confirm no backend implementation, frontend implementation, database migration, Controller, page, worker app, or upload implementation is created in this change.
- Confirm this change starts from frozen `production_route_instance` and existing `production_step_instance` records.
- Confirm CRM, public pool, contribution, order core, inventory, attendance, dashboard, screen-web, worker-uniapp, production-h5, file upload, and photo check-in remain out of scope.

DoD:

- The change contains only OpenSpec files.
- No backend or frontend implementation file is modified.
- No migration file is created.
- No `production_step_checkin` table is created or designed as part of this change.
- The implementation team can identify forbidden modules before coding.

## Task 2: Design step execution state machine

Steps:

- Define `production_step_instance.status` values for MVP:
  - `PENDING`
  - `IN_PROGRESS`
  - `COMPLETED`
- Define allowed step transitions:
  - `PENDING → IN_PROGRESS`
  - `IN_PROGRESS → COMPLETED`
- Define rejected transitions and error codes.
- Define `production_route_instance.status` behavior:
  - `DISPATCHED → IN_PROGRESS`
  - `IN_PROGRESS → COMPLETED`
- Define `order_item.production_status` synchronization:
  - `IN_PROGRESS`
  - `COMPLETED`

DoD:

- State machines are documented in `design.md`.
- Invalid transitions map to explicit business errors.
- The design does not introduce pause, `BLOCKED`, rework, skip, or parallel execution behavior.
- The design keeps production execution independent from process templates as live execution source.

## Task 3: Design task query and current worker strategy

Steps:

- Define required current user context:
  - `currentUserId`
  - `tenantId`
  - roles
- Define future JWT/platform-auth current user as the preferred source.
- Define dev/test mock current user strategy if auth is not ready.
- Define task visibility for `assigned_user_id = currentUserId`.
- Define task visibility for unassigned role tasks where `assigned_user_id is null` and `assigned_role` matches current user roles.
- Define that unassigned role tasks are visible to all matching-role users while status is `PENDING`.
- Define that starting an unassigned role task claims execution through `started_by`, not by writing `assigned_user_id`.
- Define that an `IN_PROGRESS` role-claimed task is no longer shown as an unassigned `PENDING` task to other matching-role users.
- Define that completion defaults to the `started_by` user.
- Define rejection behavior for tasks not assigned to the current user or role.

DoD:

- Task query design supports both concrete assignee and role-based unassigned tasks.
- `assigned_user_id` remains optional for MVP.
- Role-based unassigned task claiming keeps `assigned_user_id` unchanged.
- `started_by` is the execution claimant for unassigned role tasks.
- Other matching-role users stop seeing the task once it is `IN_PROGRESS`.
- Mock current-user design is dev/test only.
- Mock current-user design does not implement employee, payroll, contribution, attendance, or complex permission logic.
- Unauthorized execution attempts return `STEP_NOT_ASSIGNED_TO_CURRENT_USER`.

## Task 4: Design start step behavior

Steps:

- Define start endpoint behavior for `POST /api/production/step-instances/{stepInstanceId}/start`.
- Validate the step exists and is not deleted.
- Validate the route instance exists and is frozen.
- Validate current user can execute the step.
- Validate step status is `PENDING`.
- Validate previous active steps are completed.
- Define updates:
  - step status to `IN_PROGRESS`
  - `started_at`
  - `started_by`
  - route status to `IN_PROGRESS` when first step starts
  - order item production status to `IN_PROGRESS`
- Define transaction boundary expectations.
- Define atomic status update behavior for role-based unassigned task start.
- Define concurrent start behavior where exactly one matching-role user can win the `PENDING → IN_PROGRESS` transition.

DoD:

- Start behavior changes only execution fields.
- Start behavior does not change frozen structure fields.
- Starting a later step before previous steps complete returns `PREVIOUS_STEP_NOT_COMPLETED`.
- Starting an already started or completed step returns the defined business error.
- Starting an unassigned role task does not write `assigned_user_id`.
- Concurrent role-based start tests prove only one request succeeds and losing requests return `STEP_NOT_PENDING` or `STEP_ALREADY_STARTED`.
- Order item write-back remains limited to production fields.

## Task 5: Design complete step behavior

Steps:

- Define complete endpoint behavior for `POST /api/production/step-instances/{stepInstanceId}/complete`.
- Validate the step exists and is not deleted.
- Validate the route instance exists and is frozen.
- Validate current user can execute the step.
- Validate step status is `IN_PROGRESS`.
- Validate that role-claimed unassigned tasks are completed by `started_by` by default.
- Define updates:
  - step status to `COMPLETED`
  - `completed_at`
  - `completed_by`
- Define that photo and remark requirement fields are not enforced in this change.
- Define that no upload, file binding, or check-in payload is accepted in this change.

DoD:

- Complete behavior changes only execution fields.
- Completing a `PENDING` step returns `STEP_NOT_IN_PROGRESS`.
- Completing a `COMPLETED` step returns `STEP_ALREADY_COMPLETED`.
- Completing a role-claimed task by a user other than `started_by` returns `STEP_NOT_ASSIGNED_TO_CURRENT_USER`.
- Completion does not require photo upload or mandatory remark in this change.
- Completion does not create `production_step_checkin` records.

## Task 6: Design progress calculation and order_item writeback

Steps:

- Define total steps as active non-deleted step instances under one route instance.
- Define completed steps as active non-deleted step instances with status `COMPLETED`.
- Define progress:

```text
completed_steps / total_steps
```

- Define MVP integer percentage representation:

```text
progress = floor(completed_steps * 100 / total_steps)
```

- Define all-completed progress as forced `100`.
- Define `total_steps = 0` as a business error or data anomaly, not silent division by zero.
- Define route progress update after completion.
- Define order item production progress write-back.
- Define route and order item completion when all steps are completed.
- Define rollback expectations when order item write-back fails.

DoD:

- Progress calculation is deterministic.
- Route progress and order item progress use the same integer percentage value.
- All steps completed sets route status to `COMPLETED`.
- All steps completed writes `order_item.production_status = COMPLETED`.
- All steps completed writes route and order item progress as `100`.
- Zero total steps is handled as an error or data anomaly.
- Write-back failure maps to `ORDER_ITEM_WRITEBACK_FAILED` and must not leave inconsistent state.

## Task 7: Design frozen structure protection tests

Steps:

- Define tests proving start does not change step order, step name, assigned role, photo requirement, remark requirement, mobile flag, or snapshot fields.
- Define tests proving complete does not change frozen structure fields.
- Define tests proving add/delete/reorder/rename/skip/rework/append operations are not available or return `PRODUCTION_ROUTE_STRUCTURE_FROZEN`.
- Define tests proving execution requires `production_route_instance.frozen = true`.
- Define tests for role-based unassigned task claiming:
  - same-role users can both see an unassigned `PENDING` task
  - exactly one concurrent start succeeds
  - losing concurrent start returns `STEP_NOT_PENDING` or `STEP_ALREADY_STARTED`
  - `assigned_user_id` is not written by start
  - only `started_by` can complete the role-claimed task by default

DoD:

- Test plan proves execution updates only execution fields.
- Test plan proves frozen structure cannot be modified by execution behavior.
- Test plan includes `PRODUCTION_ROUTE_NOT_FROZEN`.
- Test plan includes role-based concurrent claim behavior.
- Test plan excludes photo upload, file upload, check-in, inventory, attendance, dashboard, CRM, contribution, and order core logic.

## Task 8: Define schema and migration constraints for implementation

Steps:

- Confirm this OpenSpec change itself creates no migration.
- Define that the next implementation PR may add a focused execution migration if V031 lacks required fields.
- Suggested migration name:

```text
V032__production_step_execution.sql
```

- Limit allowed migration content to:
  - `production_step_instance.started_by`
  - `production_step_instance.completed_by`
  - indexes for `assigned_user_id + status`
  - indexes for `assigned_role + status`
  - indexes for `route_instance_id + step_order`
  - other minimal production execution fields required by this OpenSpec
- Explicitly forbid migration content for:
  - `production_step_checkin`
  - `file_asset`
  - inventory
  - attendance
  - dashboard
  - order core
  - contribution

DoD:

- Migration allowance is limited to production step execution support.
- No future implementation task can use this change to create check-in, file, inventory, attendance, dashboard, order core, or contribution tables.
- Required query indexes are identified before implementation.

## Task 9: Design API and future UI boundaries

Steps:

- Draft API shapes for:
  - `GET /api/production/tasks/my`
  - `GET /api/production/step-instances/{stepInstanceId}`
  - `POST /api/production/step-instances/{stepInstanceId}/start`
  - `POST /api/production/step-instances/{stepInstanceId}/complete`
  - `GET /api/production/route-instances/{routeInstanceId}/progress`
- Define shared response envelope.
- Define future UI ownership under production-owned modules only.
- Explicitly exclude worker-uniapp, production-h5, and screen-web implementation from this change.
- Explicitly exclude order detail, CRM, public pool, and contribution pages.

DoD:

- API draft supports task query, start, complete, and progress.
- API draft does not include photo upload, file upload, check-in, rework, skip, or append endpoints.
- Future UI boundary is documented without implementing any page.
- No customer-line or order-core UI is introduced.

## Task 10: Define verification checklist

Steps:

- Define OpenSpec file existence checks.
- Define diff checks proving only OpenSpec files changed.
- Define future backend test command for implementation stage:

```powershell
cd backend
mvn -pl zhisheng-app -am test
```

- Define future frontend validation command only if a later UI implementation is approved.
- Define boundary checks for forbidden modules.
- Define manual API verification examples for future implementation.

DoD:

- Verification checklist includes evidence requirements.
- Verification checklist states no completion claim is allowed without verification evidence.
- Checklist covers OpenSpec, backend, API, progress, order write-back, frozen structure, and boundary checks.
- Checklist can be reused before future implementation PR review.

## Task 11: Review out-of-scope protection

Steps:

- Review proposal, design, spec, and tasks for forbidden scope.
- Confirm the change does not implement photo upload, file upload, `production_step_checkin`, file asset changes, worker-uniapp, production-h5, screen-web, inventory, attendance, dashboard, CRM, public pool, contribution, order core logic, rework, skip, execution-time insertion, or BPM.
- Confirm `photo_required` and `remark_required` are retained as metadata only.
- Confirm future `production-step-checkin-photo` owns evidence capture and enforcement.
- Confirm production may write only `order_item.production_status` and `order_item.production_progress`.

DoD:

- Out-of-scope list is present in proposal, design, and spec.
- Photo and remark enforcement boundary is explicit.
- Order-line ownership is preserved.
- Cursor/Codex business-function boundary is explicit.
- The change can be reviewed without reading backend or frontend implementation code.

## Overall Acceptance Criteria

- An OpenSpec change exists at `openspec/changes/production-step-execution`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` all exist.
- `spec.md` contains explicit DoD.
- This round explicitly does not write code.
- This round explicitly does not create migrations.
- This round explicitly does not implement Controller APIs or pages.
- Photo upload, file upload, and `production_step_checkin` are explicitly out of scope.
- `worker-uniapp`, `production-h5`, and `screen-web` are explicitly out of scope.
- Inventory, attendance, dashboard, CRM, public pool, contribution, and order core logic are explicitly forbidden.
- Execution stage only allows status transitions and execution field updates.
- Frozen route and step structure cannot be modified.
- MVP serial execution by `step_order` is defined.
- Production progress calculation is integer `floor(completed_steps * 100 / total_steps)`.
- Route and order item progress use the same integer value.
- All steps completed forces progress to `100`.
- Zero total steps is treated as an error or data anomaly.
- Role-based unassigned task claiming uses atomic `PENDING → IN_PROGRESS` update and records `started_by` without writing `assigned_user_id`.
- Production may write back only:
  - `order_item.production_status`
  - `order_item.production_progress`
- `photo_required` and `remark_required` are not enforced in this change.
- Future `production-step-checkin-photo` owns photo, remark, check-in, and file binding enforcement.
- No completion claim is allowed without verification evidence.
