# Production Step Execution Proposal

## Why

Production dispatch already creates frozen `production_route_instance` and ordered `production_step_instance` records. The next production-side capability is letting executable step instances move through a minimal execution lifecycle while preserving the frozen process structure.

This change defines how a frozen dispatched route starts execution:

```text
production_step_instance
→ PENDING
→ IN_PROGRESS
→ COMPLETED
→ progress recalculation
→ order_item production progress write-back
```

The core project rule still applies:

```text
Configuration stage is flexible.
Execution stage is frozen.
```

Without this change, production can be dispatched but cannot show controlled progress from step execution.

## Goals

- Allow workers or production executors to query executable production step tasks.
- Support the first execution state transitions:
  - `PENDING`
  - `IN_PROGRESS`
  - `COMPLETED`
- Start a step:
  - `PENDING → IN_PROGRESS`
  - record `started_at`
  - record `started_by`
- Complete a step:
  - `IN_PROGRESS → COMPLETED`
  - record `completed_at`
  - record `completed_by`
- Design a dev/test mock current user strategy if platform authentication is not ready.
- Calculate production progress as:

```text
completed_steps / total_steps
```

- Update `production_route_instance.production_progress`.
- Update `order_item.production_progress`.
- Move route status from `DISPATCHED` to `IN_PROGRESS` when the first step starts.
- Move route status to `COMPLETED` when all steps are completed.
- Synchronize `order_item.production_status` to `IN_PROGRESS` and `COMPLETED`.
- Query tasks by `assigned_user_id`.
- When `assigned_user_id` is empty, allow role-based task discovery by `assigned_role` using a clearly defined MVP strategy.
- Enforce MVP serial execution by `step_order`.
- Preserve frozen structure by allowing only execution fields to change.

## Non-Goals

- Do not write backend or frontend business code in this OpenSpec-only change.
- Do not create database migrations in this OpenSpec-only change.
- Do not implement Controller APIs in this change.
- Do not implement admin-web, production-h5, worker-uniapp, or screen-web pages.
- Do not implement photo upload.
- Do not implement file upload.
- Do not create `production_step_checkin`.
- Do not bind images to steps.
- Do not modify `file_asset`.
- Do not implement worker photo check-in.
- Do not enforce `photo_required` or `remark_required` in this change.
- Do not implement remark mandatory validation.
- Do not implement rework.
- Do not implement skip step behavior.
- Do not add steps during execution.
- Do not reorder steps during execution.
- Do not implement complex `BLOCKED` or pause flows.
- Do not implement inventory.
- Do not implement attendance.
- Do not implement dashboard or boss cockpit.
- Do not implement CRM.
- Do not implement customer public pool.
- Do not implement contribution value logic.
- Do not implement order creation or order core logic.

## Scope

OpenSpec scope:

- `openspec/changes/production-step-execution`

Future backend implementation scope, after this OpenSpec is approved:

- `backend/zhisheng-production`
- `backend/zhisheng-app` only for production module wiring
- Existing `OrderItemReadPort` / `OrderItemProductionPort` only for limited production status and progress write-back
- A dev/test current-user adapter if platform authentication is not ready

Future frontend implementation scope, after this OpenSpec is approved:

- A later admin-web or worker-facing production task surface under production-owned modules only

Out-of-scope implementation paths:

- `frontend/customer-h5`
- `frontend/production-h5`
- `frontend/worker-uniapp`
- `frontend/screen-web`
- CRM, public pool, order core, contribution, inventory, attendance, dashboard implementation files
- file upload implementation files

## Collaboration Boundaries

Codex owns production execution state, production progress calculation, and frozen production step behavior.

Cursor owns CRM, customer public pool, customer archive, contribution system, order creation, and order core business logic.

Production may continue to consume the shared `order_item` contract through ports:

```text
OrderItemReadPort
OrderItemProductionPort
```

Production may write only production fields:

```text
order_item.production_status
order_item.production_progress
```

Production must not modify customer data, order amount, quotation, product specification, quantity, customer source, order core status, contribution account, or contribution transactions.

If platform authentication is not ready, this change may design a dev/test mock current-user adapter. That adapter must provide only the minimum user id and role context needed for task filtering and execution audit fields. It must not become an employee, permission, payroll, contribution, or attendance module.

## Risks

- If execution updates structural fields, frozen production instances can become inconsistent with dispatch snapshots.
- If serial execution is not enforced, later steps may start before required upstream work is complete.
- If progress write-back is not transactional with step status updates, route progress and order item progress can diverge.
- If role-based unassigned task discovery is too broad, workers may see tasks outside their role.
- If `photo_required` and `remark_required` are treated as enforced before file/check-in infrastructure exists, MVP execution may be blocked by unfinished modules.
- If this change leaks into order, contribution, inventory, attendance, or worker mobile apps, it will cross team boundaries and expand scope beyond the approved MVP step.
