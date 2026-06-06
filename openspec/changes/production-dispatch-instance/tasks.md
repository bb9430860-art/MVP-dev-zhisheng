# Production Dispatch Instance Tasks

## Task 1: Confirm scope and boundary

Steps:

- Re-read `README.md`, `docs/codex-production-line.md`, and `docs/cursor-customer-line.md`.
- Confirm this change is OpenSpec-only.
- Confirm no backend implementation, frontend implementation, database migration, API implementation, or page implementation is created in this change.
- Confirm MVP dispatch config is request-scoped or frontend-held.
- Confirm no `dispatch_draft` table is created.
- Confirm production may read `order/order_item` but cannot implement customer-line order core logic.
- Confirm production may write only `order_item.production_status`, `order_item.production_progress`, and `order_item.production_route_instance_id`.
- Confirm CRM, public pool, contribution, order core, inventory, attendance, dashboard, screen-web, worker execution, and file infrastructure remain out of scope.

DoD:

- The change contains only OpenSpec files.
- No backend or frontend implementation file is modified.
- No migration file is created.
- No dispatch draft table is designed for MVP implementation.
- The boundary with Cursor/customer-line ownership is documented.
- The implementation team can identify forbidden modules before coding.

## Task 2: Design production instance schema

Steps:

- Draft fields for `production_route_instance`.
- Draft fields for `production_step_instance`.
- Include route and step snapshot fields.
- Include `frozen` strategy.
- Include initial statuses.
- Include soft-delete fields if implementation follows the project delete-marker pattern.
- Include uniqueness strategy for one active route instance per `order_item`.
- Treat `idempotency_key` as a reserved field only; do not require idempotent replay logic in MVP.
- Do not create migration files in this task.

DoD:

- Schema draft supports frozen route instances.
- Schema draft supports ordered step instances.
- Schema draft does not require live execution reads from templates.
- Schema draft prevents duplicate active dispatch for one `order_item`.
- Schema draft supports conflict return `ORDER_ITEM_ALREADY_DISPATCHED` for repeated dispatch.
- Schema draft does not include CRM, public pool, contribution, inventory, attendance, dashboard, or check-in tables.

## Task 3: Design dispatch draft/config model

Steps:

- Use request-scoped or frontend-held config for MVP.
- Explicitly reject persisted dispatch draft tables for MVP.
- Define route config fields.
- Define step config fields.
- Define how configured steps are copied into production step snapshots.
- Define `assigned_role` as required.
- Define `assigned_user_id` as optional and nullable.
- Define validation rules for empty steps, duplicate step order, and invalid roles.

DoD:

- Pre-dispatch configuration supports reorder, add, delete, and edit operations before confirm.
- Confirm dispatch can copy config without relying on mutable template records.
- The design explains why persisted drafts are not required for MVP.
- Concrete worker assignment is optional and does not become template-level employee binding.
- Steps can be dispatched with `assigned_role` only when `assigned_user_id` is absent.

## Task 4: Design backend APIs

Steps:

- Draft API for production order-item configuration context.
- Draft API for creating editable config from a route template.
- Draft API for confirm dispatch.
- Draft API for production summary:

```http
GET /api/production/order-items/{orderItemId}/summary
```

- Define shared response envelope.
- Define error codes for missing order item, duplicate dispatch, unavailable template, empty template steps, empty dispatch steps, and frozen structure modification.
- Define duplicate dispatch behavior as `ORDER_ITEM_ALREADY_DISPATCHED`.
- Define `idempotency_key` as reserved only, without MVP replay logic.
- Define mock/demo order-item adapter approach for stage 1 inside the backend production module.
- Define ports such as `OrderItemReadPort` and `OrderItemProductionPort`.
- Mark mock/demo adapter requirement:

```text
TODO: replace with customer-line order_item contract
```

DoD:

- API draft supports production dispatch without implementing order core logic.
- API draft supports duplicate-dispatch conflict.
- API draft does not implement complex idempotent replay.
- API draft supports order-line summary display.
- Mock/demo order item adapter is isolated behind production-owned ports.
- API draft does not include worker execution endpoints.
- API draft does not include CRM, public pool, contribution, inventory, attendance, dashboard, or file upload endpoints.

## Task 5: Design admin-web production configuration page

Steps:

- Draft future route under Codex-owned admin-web production module.
- Define page responsibilities:
  - show order item context
  - choose route template
  - preview copied steps
  - edit step config before dispatch
  - confirm dispatch
  - show frozen result after dispatch
- Define controls for reorder, add, delete, edit role, optional assignee, photo required, remark required, and mobile enabled.
- Explicitly exclude order detail implementation and customer-line pages.

DoD:

- Page design is limited to `frontend/admin-web/src/modules/production`.
- Page design does not modify `customer-h5`, CRM pages, order core pages, contribution pages, production-h5, worker-uniapp, or screen-web.
- Page design makes frozen state clear after dispatch.
- Page design does not include worker task execution.

## Task 6: Define tests for dispatch and freeze rules

Steps:

- Define backend tests for successful dispatch.
- Define tests that copied instances are independent from later template edits.
- Define tests for duplicate dispatch rejection with `ORDER_ITEM_ALREADY_DISPATCHED`.
- Define tests for missing `order_item`.
- Define tests for disabled, deleted, or missing route templates.
- Define tests for templates without enabled steps.
- Define tests for empty dispatch step configuration.
- Define tests for frozen structure modification rejection.
- Define tests that order write-back is limited to production fields.
- Define tests for production summary.

DoD:

- Test plan covers dispatch success and all listed exception scenarios.
- Test plan proves `frozen = true` after dispatch.
- Test plan proves repeated dispatch returns `ORDER_ITEM_ALREADY_DISPATCHED`.
- Test plan proves generated production instances do not execute directly from templates.
- Test plan proves no customer-line, order-core, contribution, inventory, attendance, dashboard, or worker execution behavior is introduced.

## Task 7: Define verification checklist

Steps:

- Define OpenSpec file existence checks.
- Define diff checks proving no backend/frontend implementation files changed during OpenSpec-only work.
- Define future backend test command for implementation stage.
- Define future frontend type-check/build command for implementation stage.
- Define manual API verification examples for dispatch and summary after implementation.
- Define boundary self-check items.

DoD:

- Verification checklist includes evidence requirements.
- Verification checklist states no completion claim is allowed without verification evidence.
- Verification checklist includes OpenSpec, backend, frontend, API, and boundary checks.
- Verification checklist can be reused before future implementation PR review.

## Task 8: Review out-of-scope protection

Steps:

- Review proposal, design, spec, and tasks for forbidden scope.
- Confirm the change does not include worker check-in, inventory, attendance, dashboard, screen-web, CRM, public pool, contribution value, file upload infrastructure, order core field modification, customer field modification, rework, skipped steps, execution-time step insertion, or BPM workflow.
- Confirm production consumes `order_item` as an external contract.
- Confirm mock/demo `order_item` usage is inside the backend production module, temporary, replaceable, and behind ports.
- Confirm mock/demo adapter does not implement order creation, order amount, customer data, quotation, product specification ownership, or order core status ownership.

DoD:

- Out-of-scope list is present in proposal, design, and spec.
- Order-line ownership is preserved.
- Cursor/Codex business-function boundary is explicit.
- The change can be reviewed without reading backend or frontend implementation code.

## Overall Acceptance Criteria

- An OpenSpec change exists at `openspec/changes/production-dispatch-instance`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` all exist.
- `spec.md` contains explicit DoD.
- The change explicitly says this round does not write code.
- The change explicitly says this round does not create migrations.
- The change explicitly says MVP does not persist dispatch drafts.
- CRM, public pool, contribution, and order core logic are explicitly forbidden.
- Worker check-in, inventory, attendance, dashboard, and screen-web are explicitly forbidden.
- Production dispatch results in `frozen = true`.
- Later template edits do not affect generated production instances.
- Production instances do not directly reference templates as execution source.
- Production only consumes `order_item` and may write back at most:
  - `production_status`
  - `production_progress`
  - `production_route_instance_id`
- Duplicate dispatch returns `ORDER_ITEM_ALREADY_DISPATCHED`.
- `assigned_role` is required and `assigned_user_id` is optional for dispatch steps.
- No completion claim is allowed without verification evidence.
