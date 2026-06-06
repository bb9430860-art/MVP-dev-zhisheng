# Production Step Execution Specification

## ADDED Requirements

### Requirement: Query executable production step tasks

The system SHALL allow a worker or production executor to query production step tasks that they can execute.

#### Scenario: Query tasks assigned to current user

- GIVEN a `production_step_instance` has `assigned_user_id = currentUserId`
- AND the step belongs to a frozen production route instance
- WHEN the current user queries `GET /api/production/tasks/my`
- THEN the task appears in the result
- AND includes step name, step order, assigned role, status, route instance id, order item id, and execution requirement metadata
- AND does not expose customer-line or order-core mutation behavior

#### Scenario: Query role-executable unassigned tasks

- GIVEN a `production_step_instance` has no `assigned_user_id`
- AND the step has `assigned_role` matching one of the current user's roles
- AND the step status is `PENDING`
- AND the step belongs to a frozen production route instance
- WHEN the current user queries my tasks
- THEN the task appears as executable by role
- AND other users with the same role can also see the same unassigned `PENDING` task
- AND the system does not require concrete worker assignment for MVP execution

#### Scenario: Started role-executable task is no longer shown as unassigned

- GIVEN a role-executable step has no `assigned_user_id`
- AND a matching-role user has started it
- WHEN another matching-role user queries my tasks
- THEN the step is not returned as an unassigned `PENDING` task
- AND the system keeps `assigned_user_id` unchanged
- AND the executing user is represented by `started_by`

#### Scenario: Hide tasks outside current user's assignment or role

- GIVEN a `production_step_instance` is assigned to another user
- OR has an unassigned role that the current user does not have
- WHEN the current user queries my tasks
- THEN the task is not returned
- AND start or complete requests for that step return `STEP_NOT_ASSIGNED_TO_CURRENT_USER`

### Requirement: Start a production step

The system SHALL allow an executable `PENDING` step to start.

#### Scenario: Start a pending step

- GIVEN a step instance has status `PENDING`
- AND the step belongs to a frozen production route instance
- AND the current user can execute the step
- AND all previous active steps are completed
- WHEN the current user starts the step
- THEN the step status becomes `IN_PROGRESS`
- AND `started_at` is recorded
- AND `started_by` is recorded as the current user
- AND no frozen structure fields are changed

#### Scenario: First matching-role user claims an unassigned task by starting it

- GIVEN a step instance has `assigned_user_id = null`
- AND the step has status `PENDING`
- AND the step has `assigned_role` matching the current user's role
- WHEN the current user starts the step
- THEN the system atomically changes the step from `PENDING` to `IN_PROGRESS`
- AND records `started_by = currentUserId`
- AND does not write `assigned_user_id`
- AND the step is no longer visible as an unassigned `PENDING` task to other users with the same role

#### Scenario: Concurrent role-based start allows only one winner

- GIVEN two users with the same matching role can see the same unassigned `PENDING` step
- WHEN both users attempt to start the step concurrently
- THEN exactly one request succeeds
- AND the successful request records `started_by` as that user
- AND the losing request returns `STEP_NOT_PENDING` or `STEP_ALREADY_STARTED`
- AND `assigned_user_id` remains unchanged

#### Scenario: First step start moves route and order item to in progress

- GIVEN a production route instance has status `DISPATCHED`
- AND no step has started yet
- WHEN the first executable step starts
- THEN `production_route_instance.status` becomes `IN_PROGRESS`
- AND production writes `order_item.production_status = IN_PROGRESS`
- AND production does not update order core lifecycle status

#### Scenario: Reject start for invalid status

- GIVEN a step instance is already `IN_PROGRESS`
- WHEN start is requested
- THEN the system returns `STEP_ALREADY_STARTED`

- GIVEN a step instance is already `COMPLETED`
- WHEN start is requested
- THEN the system returns `STEP_ALREADY_COMPLETED`

### Requirement: Complete a production step

The system SHALL allow an executable `IN_PROGRESS` step to complete.

#### Scenario: Complete an in-progress step

- GIVEN a step instance has status `IN_PROGRESS`
- AND the step belongs to a frozen production route instance
- AND the current user can execute the step
- WHEN the current user completes the step
- THEN the step status becomes `COMPLETED`
- AND `completed_at` is recorded
- AND `completed_by` is recorded as the current user
- AND no frozen structure fields are changed

#### Scenario: Role-claimed task completion is limited to starter

- GIVEN an unassigned role-based step was started by `started_by = currentUserId`
- WHEN the same user completes the step
- THEN the system allows completion

- GIVEN an unassigned role-based step was started by another user
- WHEN the current user attempts to complete the step
- THEN the system returns `STEP_NOT_ASSIGNED_TO_CURRENT_USER`
- AND the step remains `IN_PROGRESS`

#### Scenario: Reject complete for non-in-progress step

- GIVEN a step instance has status `PENDING`
- WHEN complete is requested
- THEN the system returns `STEP_NOT_IN_PROGRESS`

- GIVEN a step instance has status `COMPLETED`
- WHEN complete is requested
- THEN the system returns `STEP_ALREADY_COMPLETED`

### Requirement: Enforce serial step execution in MVP

The system SHALL enforce serial execution by `step_order` for the MVP first version.

#### Scenario: Later step cannot start before previous steps complete

- GIVEN a route instance has ordered active steps
- AND a later step is `PENDING`
- AND at least one previous active step is not `COMPLETED`
- WHEN the current user starts the later step
- THEN the system returns `PREVIOUS_STEP_NOT_COMPLETED`
- AND the later step remains `PENDING`

#### Scenario: First active step can start

- GIVEN a route instance has active steps ordered by `step_order`
- AND the first active step is `PENDING`
- WHEN the assigned current user starts the first active step
- THEN the system allows the transition to `IN_PROGRESS`

#### Scenario: Parallel execution is not supported

- WHEN this change is implemented
- THEN it does not support parallel branches, dependency graphs, skipped steps, rework, or execution-time inserted steps

### Requirement: Update production progress

The system SHALL recalculate production progress when step execution changes.

#### Scenario: Progress updates after step completion

- GIVEN a route instance has `total_steps` active non-deleted step instances
- AND one or more steps are completed
- WHEN a step is completed
- THEN the system calculates integer progress as `floor(completed_steps * 100 / total_steps)`
- AND updates `production_route_instance.production_progress`
- AND writes the same integer value to `order_item.production_progress`

#### Scenario: Zero total steps is treated as data anomaly

- GIVEN progress is recalculated
- AND `total_steps = 0`
- WHEN the system detects the zero-step route instance
- THEN the system returns a business error or data anomaly error
- AND does not silently divide by zero
- AND does not write misleading progress

#### Scenario: Route completes when all steps complete

- GIVEN every active step instance under a route instance has status `COMPLETED`
- WHEN progress is recalculated
- THEN `production_route_instance.status` becomes `COMPLETED`
- AND production writes `order_item.production_status = COMPLETED`
- AND production writes `order_item.production_progress = 100`
- AND `production_route_instance.production_progress = 100`

#### Scenario: Progress write-back failure rolls back execution

- GIVEN a step status update requires order item production write-back
- WHEN the order item write-back fails
- THEN the system returns `ORDER_ITEM_WRITEBACK_FAILED`
- AND the implementation must not leave step status, route progress, and order item progress inconsistent

### Requirement: Preserve frozen production structure

The system SHALL preserve frozen route and step structure during execution.

#### Scenario: Execution updates only execution fields

- WHEN a step starts or completes
- THEN the system may update status, started/completed timestamps, started/completed user ids, route status, route progress, and order item production fields
- AND the system must not update step order, step name, assigned role, photo requirement, remark requirement, mobile execution flag, or snapshot fields

#### Scenario: Structural modification is rejected

- GIVEN a production route instance is frozen
- WHEN any operation attempts to add, delete, reorder, rename, skip, rework, or append production steps
- THEN the system rejects the operation with `PRODUCTION_ROUTE_STRUCTURE_FROZEN`
- AND the route and step structure remains unchanged

#### Scenario: Execution requires frozen dispatched route

- GIVEN a production route instance is not frozen
- WHEN step start or complete is requested
- THEN the system returns `PRODUCTION_ROUTE_NOT_FROZEN`

### Requirement: Preserve order-line ownership

The system SHALL preserve customer-line and order-line ownership while updating production progress.

#### Scenario: Production writes only production fields

- WHEN step execution starts or completes
- THEN production may write only:
  - `order_item.production_status`
  - `order_item.production_progress`
- AND production must not update customer data, order amount, product specification, product quantity, quotation data, order custom fields, contribution account, or contribution transaction records

#### Scenario: Order item access remains behind production ports

- WHEN production needs order item read or write behavior
- THEN the system uses production-owned ports such as `OrderItemReadPort` and `OrderItemProductionPort`
- AND does not implement order creation or order core business logic

### Requirement: Keep photo check-in out of this change

The system SHALL NOT implement photo check-in, file upload, or mandatory evidence validation in this change.

#### Scenario: Photo and remark requirement fields are retained but not enforced

- GIVEN a production step instance has `photo_required = true`
- OR has `remark_required = true`
- WHEN the step is started or completed in this change
- THEN the fields may be displayed or returned as metadata
- AND completion does not require photo upload
- AND completion does not require file binding
- AND completion does not require mandatory remark text

#### Scenario: Future check-in change owns evidence enforcement

- WHEN photo evidence, remarks, check-in records, or file binding are needed
- THEN they must be defined in a later `production-step-checkin-photo` change
- AND this change must not create `production_step_checkin`
- AND this change must not modify `file_asset`
- AND this change must not implement upload infrastructure

## DoD

- The OpenSpec change exists under `openspec/changes/production-step-execution`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change explicitly states that it does not write business code.
- This change explicitly states that it does not create database migrations.
- This change explicitly does not implement Controller APIs or pages.
- This change explicitly forbids photo upload, file upload, and `production_step_checkin`.
- This change explicitly forbids `worker-uniapp`, `production-h5`, and `screen-web`.
- This change explicitly forbids inventory, attendance, dashboard, CRM, public pool, contribution, and order core logic.
- Execution stage only allows status transitions and execution field updates.
- Execution stage must not modify frozen route or step structure.
- MVP serial execution by `step_order` is defined.
- Production progress calculation is defined as integer `floor(completed_steps * 100 / total_steps)`.
- `production_route_instance.production_progress` and `order_item.production_progress` use the same integer value.
- All steps completed forces progress to `100`.
- `total_steps = 0` is treated as an error or data anomaly, not silent division by zero.
- Route and order item status synchronization rules are defined.
- Production may write back only `order_item.production_status` and `order_item.production_progress`.
- `photo_required` and `remark_required` are retained but not enforced in this change.
- Future `production-step-checkin-photo` owns photo, remark, check-in, and file binding enforcement.
- No completion claim may be made without verification evidence.

## Out of Scope

The system SHALL NOT implement backend business code, migrations, Controller APIs, admin-web pages, `production-h5`, `worker-uniapp`, `screen-web`, photo upload, file upload, `production_step_checkin`, image binding, file asset changes, mandatory photo validation, mandatory remark validation, rework, skipped steps, execution-time step insertion, execution-time reordering, complex pause or `BLOCKED` flows, inventory, attendance, dashboard, CRM, public pool, contribution value, order creation, order core field modification, or customer field modification in this change.
