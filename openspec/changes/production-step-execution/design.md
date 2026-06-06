# Production Step Execution Design

## Overview

This change defines minimal execution behavior for frozen `production_step_instance` records created by `production-dispatch-instance`.

It is OpenSpec-only. It must not create code, migration files, API implementations, frontend pages, worker apps, upload flows, or database tables in this change.

The MVP execution rule is:

```text
Frozen structure cannot change.
Execution fields can change.
```

The first version focuses on step status transitions and progress calculation. Photo evidence, remarks, check-in records, and file binding are intentionally deferred to a later `production-step-checkin-photo` change.

## Step Execution Flow

1. A production route has already been dispatched.
2. `production_route_instance.frozen = true`.
3. `production_step_instance` records already exist and are ordered by `step_order`.
4. A worker or production executor opens their executable task list.
5. The task query uses current user context:
   - assigned tasks where `assigned_user_id = currentUserId`
   - unassigned tasks where `assigned_user_id is null` and `assigned_role` matches one of the current user's roles
6. The executor opens a step instance detail.
7. The executor starts the first executable step.
8. The system validates serial execution rules.
9. The system changes the step from `PENDING` to `IN_PROGRESS`.
10. The system records `started_at` and `started_by`.
11. If this is the first started step, the route changes from `DISPATCHED` to `IN_PROGRESS`.
12. The system writes `order_item.production_status = IN_PROGRESS`.
13. The executor completes an in-progress step.
14. The system changes the step from `IN_PROGRESS` to `COMPLETED`.
15. The system records `completed_at` and `completed_by`.
16. The system recalculates progress.
17. The system updates `production_route_instance.production_progress`.
18. The system writes `order_item.production_progress`.
19. If all steps are completed, route and order item production status become `COMPLETED`.

## Data Flow

```text
production_route_instance
→ production_step_instance
→ task query
→ start step
→ complete step
→ progress calculation
→ production_route_instance progress/status update
→ order_item production field write-back
```

Execution reads from production instance snapshots only. It must not read live templates as the execution source.

## State Machines

### `production_step_instance.status`

MVP execution states:

```text
PENDING
IN_PROGRESS
COMPLETED
```

Allowed transitions:

```text
PENDING → IN_PROGRESS
IN_PROGRESS → COMPLETED
```

Rejected transitions:

```text
PENDING → COMPLETED
IN_PROGRESS → PENDING
COMPLETED → IN_PROGRESS
COMPLETED → PENDING
```

`BLOCKED`, pause, rework, skip, and execution-time insertion are out of scope.

### `production_route_instance.status`

Relevant states:

```text
DISPATCHED
IN_PROGRESS
COMPLETED
```

Allowed MVP transitions:

```text
DISPATCHED → IN_PROGRESS
IN_PROGRESS → COMPLETED
```

Rules:

- Route status is `DISPATCHED` after production dispatch.
- When the first step starts, route status becomes `IN_PROGRESS`.
- When all non-deleted step instances are `COMPLETED`, route status becomes `COMPLETED`.
- Route status must not become `COMPLETED` while any active step is not completed.

### `order_item.production_status`

Production may synchronize only production status values:

```text
DISPATCHED
IN_PROGRESS
COMPLETED
```

Rules:

- Dispatch sets `DISPATCHED` in the previous change.
- First step start writes `IN_PROGRESS`.
- All steps completed writes `COMPLETED`.
- Production must not update order core lifecycle status.

## Serial Execution Rules

MVP first version uses serial execution by `step_order`.

Rules:

- A step can start only when its status is `PENDING`.
- A step can start only when every active previous step under the same route instance has status `COMPLETED`.
- The first active step can start when the route is dispatched and frozen.
- A later step cannot start while any previous active step is `PENDING` or `IN_PROGRESS`.
- A step can complete only when its status is `IN_PROGRESS`.
- Parallel execution is out of scope for this change.

If future business needs parallel branches, that requires a separate design update with dependency modeling.

## Frozen Structure Protection

This change allows execution updates only:

```text
production_step_instance.status
production_step_instance.started_at
production_step_instance.started_by
production_step_instance.completed_at
production_step_instance.completed_by
production_route_instance.status
production_route_instance.production_progress
order_item.production_status
order_item.production_progress
updated_by
updated_at
```

This change must not allow updates to frozen structure fields:

```text
step_order
step_name
assigned_role
photo_required
remark_required
mobile_enabled
source_step_template_id
step_code_snapshot
estimated_hours
operation_instruction
route snapshot fields
```

This change must not allow:

- adding production step instances
- deleting production step instances
- reordering production step instances
- skipping steps
- rework
- execution-time step insertion
- editing template or snapshot process structure during execution

If an implementation adds any endpoint that attempts structure edits after freeze, it must reject with a business error such as:

```text
PRODUCTION_ROUTE_STRUCTURE_FROZEN
```

## Current User / Worker Identity Strategy

Production step execution needs current user information:

```text
currentUserId
tenantId
roles
displayName optional
```

Preferred future source:

- platform authentication and JWT current user context

MVP dev/test fallback:

- a production-owned current-user port may provide a fixed or header-driven mock current user
- the adapter must be restricted to `dev` and `test`
- the adapter must be marked as temporary
- it must not implement employee management, payroll, contribution, attendance, or a complex permission matrix

Suggested port shape:

```text
CurrentProductionUserPort
```

Minimum context:

```text
user_id
tenant_id
roles
```

Task visibility strategy:

- If `assigned_user_id = currentUserId`, the user can see and execute the task.
- If `assigned_user_id is null`, `assigned_role` is in current user roles, and status is `PENDING`, all users with the matching role can see the task as role-executable.
- If both checks fail, the task must be hidden from "my tasks" and start/complete must reject with `STEP_NOT_ASSIGNED_TO_CURRENT_USER`.

This strategy keeps MVP usable without forcing concrete worker assignment at dispatch time.

Role-based unassigned task claiming strategy:

- Unassigned role-based tasks are not assigned during dispatch.
- The first matching-role user who starts the task claims execution by atomically changing status from `PENDING` to `IN_PROGRESS`.
- The start operation must use an atomic status guard equivalent to:

```text
WHERE id = {stepInstanceId}
  AND status = 'PENDING'
  AND deleted = 0
```

- If the atomic update succeeds, `started_by = currentUserId`.
- The implementation must not write `assigned_user_id` during this start operation.
- This preserves the distinction between dispatch-time assignment (`assigned_user_id`) and execution-time claiming (`started_by`).
- After the task is `IN_PROGRESS`, it is no longer returned as an unassigned `PENDING` task to other users with the same role.
- Completion of a role-claimed task defaults to the user recorded in `started_by`.
- If another user tries to complete the started task, the service must return `STEP_NOT_ASSIGNED_TO_CURRENT_USER`.
- If two matching-role users start the same unassigned task concurrently, exactly one atomic update may succeed. The losing request should return `STEP_NOT_PENDING` or `STEP_ALREADY_STARTED` after re-reading the current status.

## Order Item Limited Write-Back

Production may write only:

```text
order_item.production_status
order_item.production_progress
```

Production must not write:

```text
order status
order amount
customer fields
quotation fields
product specification
product quantity
custom order fields
contribution fields
```

Write-back should remain behind `OrderItemProductionPort`.

If order item write-back fails after step status changes, the service must treat the whole operation as failed and roll back in the implementation stage. Step execution, route progress update, and order item write-back should be one transaction boundary when backed by the same database or coordinated as a single service operation when the order contract becomes external.

Suggested error:

```text
ORDER_ITEM_WRITEBACK_FAILED
```

## Progress Calculation

MVP first version stores progress as an integer percentage from `0` to `100`.

Formula:

```text
progress = floor(completed_steps * 100 / total_steps)
```

Rules:

- `completed_steps` counts active non-deleted step instances with status `COMPLETED`.
- `total_steps` counts active non-deleted step instances under the route instance.
- `production_route_instance.production_progress` and `order_item.production_progress` must use the same integer value.
- When all active steps are completed, progress must be forced to `100`.
- `total_steps` should not be `0` because production dispatch rejects empty step configuration.
- If implementation encounters `total_steps = 0`, it must treat it as a business error or data anomaly and must not silently divide by zero.
- The first implementation must not use `BigDecimal` two-decimal progress semantics.

## Relation To `production-dispatch-instance`

This change depends on `production-dispatch-instance`.

Dispatch already provides:

- `production_route_instance`
- `production_step_instance`
- copied snapshots
- initial route status `DISPATCHED`
- initial step status `PENDING`
- `frozen = true`
- order item dispatch write-back

This change does not change dispatch configuration behavior. It starts from already-dispatched frozen instances and only adds execution status and progress rules.

## Relation To Future `production-step-checkin-photo`

Existing step snapshots include:

```text
photo_required
remark_required
mobile_enabled
```

In this change:

- these fields are displayed or carried forward as execution requirements metadata
- they are not enforced
- no photo upload is implemented
- no file upload is implemented
- no `production_step_checkin` table is created
- no `file_asset` binding is created
- remark-required validation is not enforced

Future `production-step-checkin-photo` should define:

- photo upload or file asset integration
- check-in records
- remark submission
- mandatory validation for `photo_required`
- mandatory validation for `remark_required`
- evidence display and audit behavior

This staged approach is an MVP delivery choice. It does not remove the final business requirement for photo or remark evidence.

## API Shape Draft

All APIs use the shared response envelope:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Query my executable tasks:

```http
GET /api/production/tasks/my
```

Optional filters:

```text
status=PENDING|IN_PROGRESS
assignedRole=WORKER
routeInstanceId=123
```

Response data draft:

```json
{
  "records": [
    {
      "stepInstanceId": 9001,
      "routeInstanceId": 3001,
      "orderId": 501,
      "orderItemId": 1001,
      "itemName": "入口精神堡垒",
      "stepName": "下料",
      "stepOrder": 3,
      "assignedRole": "WORKER",
      "assignedUserId": null,
      "status": "PENDING",
      "photoRequired": true,
      "remarkRequired": false,
      "mobileEnabled": true,
      "canStart": true
    }
  ],
  "total": 1
}
```

Get step instance detail:

```http
GET /api/production/step-instances/{stepInstanceId}
```

Start a step:

```http
POST /api/production/step-instances/{stepInstanceId}/start
```

Request body draft:

```json
{
  "operatorId": null
}
```

`operatorId` is optional and should normally come from current user context. Dev/test may allow explicit override only if documented.

Complete a step:

```http
POST /api/production/step-instances/{stepInstanceId}/complete
```

Request body draft:

```json
{
  "operatorId": null
}
```

No photo, file, or mandatory remark payload is accepted in this change.

Query route progress:

```http
GET /api/production/route-instances/{routeInstanceId}/progress
```

Response data draft:

```json
{
  "routeInstanceId": 3001,
  "orderItemId": 1001,
  "status": "IN_PROGRESS",
  "progress": 40,
  "totalSteps": 10,
  "completedSteps": 4,
  "currentStepName": "喷漆",
  "frozen": true
}
```

## Data Field Drafts

No migration is created in this OpenSpec change. These are field expectations only.

Expected existing or future fields on `production_step_instance`:

```text
status
started_at
started_by
completed_at
completed_by
assigned_user_id
assigned_role
photo_required
remark_required
mobile_enabled
frozen
deleted
```

Expected existing or future fields on `production_route_instance`:

```text
status
production_progress
frozen
order_item_id
deleted
```

Expected order item production write-back fields:

```text
production_status
production_progress
```

If implementation finds missing audit fields such as `started_by`, `completed_by`, or progress fields, it must propose a migration in a later implementation PR after this OpenSpec is approved.

The next implementation stage may add a focused production execution migration if V031 does not contain required execution fields. Suggested name:

```text
V032__production_step_execution.sql
```

Allowed migration scope:

- `production_step_instance.started_by`
- `production_step_instance.completed_by`
- indexes for `assigned_user_id + status`
- indexes for `assigned_role + status`
- indexes for `route_instance_id + step_order` serial validation
- other minimal production execution fields required by this OpenSpec

Forbidden migration scope:

- `production_step_checkin`
- `file_asset`
- inventory tables
- attendance tables
- dashboard tables
- order core tables or fields
- contribution tables or fields

The migration must not be used to implement photo check-in, file upload, inventory, attendance, dashboard, order core, or contribution scope.

## Exception Scenarios

### `STEP_INSTANCE_NOT_FOUND`

Return when the step instance does not exist or is deleted.

### `STEP_ALREADY_STARTED`

Return when start is requested for a step already in `IN_PROGRESS`.

For a role-based unassigned task, this can also be returned when another same-role user won the atomic `PENDING → IN_PROGRESS` start race first.

### `STEP_ALREADY_COMPLETED`

Return when start or complete is requested for a `COMPLETED` step.

### `STEP_NOT_IN_PROGRESS`

Return when complete is requested for a step that is not `IN_PROGRESS`.

### `STEP_NOT_PENDING`

Return when start is requested for a step that is not `PENDING`.

For concurrent role-based start attempts, the losing request may return this after re-reading that the task is no longer `PENDING`.

### `PREVIOUS_STEP_NOT_COMPLETED`

Return when a later step is started before all previous active steps are completed.

### `PRODUCTION_ROUTE_NOT_FROZEN`

Return when execution is attempted on a route instance that was not produced by confirmed dispatch and is not frozen.

### `PRODUCTION_ROUTE_STRUCTURE_FROZEN`

Return when any operation attempts to change frozen route or step structure.

### `STEP_NOT_ASSIGNED_TO_CURRENT_USER`

Return when current user neither owns the step through `assigned_user_id` nor has the required `assigned_role` for an unassigned step.

Also return when a role-claimed task is already `IN_PROGRESS` and the current user is not the recorded `started_by` user attempting to complete it.

### `ORDER_ITEM_WRITEBACK_FAILED`

Return when production step/route updates cannot safely synchronize production fields back to the order item contract.

## Out of Scope

This change does not implement:

- backend implementation
- database migration
- Controller APIs
- admin-web pages
- production-h5 pages
- worker-uniapp pages
- screen-web
- photo upload
- file upload
- `production_step_checkin`
- `file_asset`
- image binding
- mandatory photo validation
- mandatory remark validation
- rework
- skipped steps
- execution-time step insertion
- execution-time reorder
- complex pause or `BLOCKED` flows
- inventory
- attendance
- dashboard
- CRM
- customer public pool
- contribution
- order creation
- order core logic
