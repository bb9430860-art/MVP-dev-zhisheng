# Production Step Execution Demo Acceptance

## Scope

This document records the local demo acceptance path for production step execution.

This stage only verifies the existing production step execution API and the `admin-web` task execution pages. It does not add new business modules.

## Prerequisites

- The production dispatch demo path is available.
- The backend `dev` profile can load process route template demo seed data.
- The dev/test mock order item adapter includes demo `order_item=1001`.
- The production dispatch API can dispatch `order_item=1001` before step execution starts.
- The production step execution API is currently limited to `dev` and `test` profiles because the current user context still comes from a mock `CurrentProductionUserPort`.

The mock adapters are temporary. They must be replaced by customer-line order item and platform JWT current user contracts later.

## Startup

Run backend tests first:

```powershell
cd backend
mvn -pl zhisheng-app -am test
```

Start the backend with the local dev profile.

Windows PowerShell:

```powershell
cd backend
mvn -pl zhisheng-app spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Git Bash, macOS, and Linux:

```bash
cd backend
mvn -pl zhisheng-app spring-boot:run -Dspring-boot.run.profiles=dev
```

Start admin-web:

```powershell
cd frontend/admin-web
npm install
npm run dev
```

Open these demo URLs:

```text
http://127.0.0.1:5173/production/order-items/1001/configure
http://127.0.0.1:5173/production/tasks
```

## Prepare Demo Data

The step execution demo depends on a dispatched production route instance.

1. Open `/production/order-items/1001/configure`.
2. Confirm the page displays the demo order item.
3. Select a route template that matches `SPIRIT_FORTRESS`, or select the `GENERAL` template.
4. Generate editable steps from the template.
5. Confirm the step list is not empty.
6. Click confirm dispatch.
7. Confirm the summary shows:
   - `dispatched = true`
   - `frozen = true`
   - `productionProgress = 0`
   - a non-empty step list with initial status `PENDING`

After dispatch, the production route instance and step instances are execution snapshots. Later template edits must not be used as the execution source.

## Operation Steps

1. Open `/production/tasks`.
2. Confirm the page displays PENDING tasks for the current mock production user.
3. Confirm each task shows:
   - order item context
   - step order
   - step name
   - assigned role
   - status
   - photo required metadata
   - remark required metadata
   - route progress as an integer percent
4. Start the first executable step.
5. Confirm the step status changes from `PENDING` to `IN_PROGRESS`.
6. Confirm the route or task progress is still an integer percent.
7. Complete the same step.
8. Confirm the step status changes from `IN_PROGRESS` to `COMPLETED`.
9. Confirm route progress increases according to:

```text
progress = floor(completed_steps * 100 / total_steps)
```

10. Open the step detail page from the task list.
11. Confirm the detail page shows the same status and progress.

## Serial Execution Check

MVP step execution is serial.

To confirm the rule:

1. Dispatch a route with at least two steps.
2. Before the first step is completed, try to start the second step through API or UI if it is visible.
3. The backend must reject the request with `PREVIOUS_STEP_NOT_COMPLETED`.
4. Complete the first step.
5. Start the second step again.
6. The second step can start only after all previous active steps are `COMPLETED`.

API example:

```http
POST /api/production/step-instances/{secondStepInstanceId}/start
```

Expected error before the previous step is completed:

```text
PREVIOUS_STEP_NOT_COMPLETED
```

## Metadata Check

The current step execution stage keeps `photo_required` and `remark_required` only as metadata.

Verify that the task list and detail page may display:

```text
photo_required
remark_required
mobile_enabled
```

But this stage must not require or provide:

```text
photo upload
file upload
camera button
file picker
check-in record
mandatory remark input
production_step_checkin
file_asset binding
```

Photo evidence, remark enforcement, and file binding belong to a later `production-step-checkin-photo` change.

## API Checks

After dispatching `order_item=1001`, check my tasks:

```http
GET /api/production/tasks/my
```

Expected:

```text
code = 0
data contains executable production step tasks
PENDING role-based tasks may be visible when assigned_user_id is empty and assigned_role matches current user roles
```

Check step detail:

```http
GET /api/production/step-instances/{stepInstanceId}
```

Expected:

```text
code = 0
data.status is PENDING, IN_PROGRESS, or COMPLETED
data.photoRequired and data.remarkRequired are metadata flags
```

Start a step:

```http
POST /api/production/step-instances/{stepInstanceId}/start
```

Expected:

```text
code = 0
data.status = IN_PROGRESS
started_at is recorded
started_by is recorded
assigned_user_id is not changed for role-based claimed tasks
```

Complete a step:

```http
POST /api/production/step-instances/{stepInstanceId}/complete
```

Expected:

```text
code = 0
data.status = COMPLETED
completed_at is recorded
completed_by is recorded
```

Check route progress:

```http
GET /api/production/route-instances/{routeInstanceId}/progress
```

Expected:

```text
code = 0
data.progress is an integer from 0 to 100
data.completedSteps <= data.totalSteps
all steps completed forces progress = 100
```

## Order Boundary Check

Production step execution may only update production-owned fields through `OrderItemProductionPort`:

```text
order_item.production_status
order_item.production_progress
```

It must not modify order core fields, including:

```text
order creation
order amount
customer data
quotation data
product specification ownership
quantity
order core status ownership
```

The route instance remains the production execution source. Step execution must not read process route templates or process step templates as execution state.

## Known Limits

- The demo order item is in memory and resets when the dev backend restarts.
- The mock current production user is available only in `dev` and `test` profiles.
- The production step execution API is profile-limited until JWT current user is connected.
- The first version supports serial execution only.
- Role-based unassigned tasks are claimed by `started_by`; starting a role-based task does not write back `assigned_user_id`.
- Photo and remark flags are displayed but not enforced in this stage.
- There is no worker mobile app page in this stage.

## Out Of Scope

This demo acceptance stage does not implement:

- photo upload
- file upload
- camera capture
- `production_step_checkin`
- `file_asset`
- forced remark input
- worker-uniapp
- production-h5
- screen-web
- inventory
- attendance
- boss dashboard
- CRM
- customer public pool
- contribution value
- order creation
- order core logic changes
- rework
- skip
- execution-time step insertion
- execution-time step ordering changes
- parallel workflow

## Acceptance Record Template

Use this section when recording a manual run:

```text
Date:
Backend command:
Frontend command:
Demo order item:
Dispatched route instance:
First step started:
First step completed:
Progress after completion:
Serial execution error checked:
Photo/upload/check-in absent:
Order core fields unchanged:
Notes:
```
