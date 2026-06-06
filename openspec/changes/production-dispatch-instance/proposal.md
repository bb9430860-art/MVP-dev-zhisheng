# Production Dispatch Instance Proposal

## Why

The production line already has process route templates and step templates. The next production-side capability is turning an order item created by the customer/order line into a frozen production route instance.

This change defines the contract for production dispatch:

```text
order_item
→ choose route template
→ configure steps before dispatch
→ confirm dispatch
→ copy snapshots
→ freeze route and steps
```

The core project rule is:

```text
Template can change. Instance cannot be disturbed.
```

Without this change, future worker execution could accidentally execute directly against editable templates or mutate order/customer data outside the production boundary.

## Goals

- Read product/location information from the future customer-line `order_item` contract.
- Let the production manager choose an enabled and non-deleted process route template for an `order_item`.
- Read enabled and non-deleted route templates and enabled, non-deleted step templates.
- Create a request-scoped or frontend-held pre-dispatch configuration from the selected template.
- Do not persist dispatch drafts in MVP.
- Do not create a `dispatch_draft` table.
- Allow pre-dispatch step adjustments:
  - reorder steps
  - add steps
  - delete steps
  - edit step name
  - edit assigned role
  - keep `assigned_role` required
  - optionally assign responsible/executing user through nullable `assigned_user_id`
  - edit photo requirement
  - edit remark requirement
  - edit mobile execution flag
- Confirm production dispatch for one `order_item`.
- Copy route and step snapshots into:
  - `production_route_instance`
  - `production_step_instance`
- Mark the production route instance as `frozen = true` after dispatch.
- Enforce frozen production step structure through `production_step_instance.frozen = true` or `route_instance.frozen`.
- Prevent structural changes after freeze.
- Provide production summary for order-line display:

```http
GET /api/production/order-items/{orderItemId}/summary
```

- Limit order-line write-back to production fields:

```text
order_item.production_status
order_item.production_progress
order_item.production_route_instance_id
```
- Return `ORDER_ITEM_ALREADY_DISPATCHED` when the same `order_item` is dispatched again.
- Keep `idempotency_key` as a reserved field only. MVP does not implement complex idempotent replay.

## Non-Goals

- Do not implement worker photo check-in.
- Do not implement worker mobile tasks.
- Do not implement step start or completion execution.
- Do not implement inventory.
- Do not implement attendance.
- Do not implement boss dashboard.
- Do not implement `screen-web`.
- Do not implement CRM.
- Do not implement customer public pool.
- Do not implement contribution value logic.
- Do not implement file upload infrastructure.
- Do not modify order core fields.
- Do not modify customer fields.
- Do not implement rework during execution.
- Do not implement skipped steps during execution.
- Do not implement adding steps during execution.
- Do not implement complex BPM workflow.
- Do not create database migrations in this OpenSpec-only change.
- Do not write backend or frontend business code in this OpenSpec-only change.
- Do not create persistent dispatch draft tables.
- Do not implement idempotency replay logic.

## Scope

OpenSpec scope:

- `openspec/changes/production-dispatch-instance`

Future backend implementation scope, after this change is approved:

- `backend/zhisheng-production`
- `backend/zhisheng-process` only for reading existing template data
- `backend/zhisheng-app` only for wiring production module when implementation starts
- `OrderItemReadPort` / `OrderItemProductionPort` style ports in the production module for reading order item context and writing production fields
- A dev/test adapter inside the production module that can use mock/demo order item data first and later be replaced by the customer-line order contract

Future frontend implementation scope, after this change is approved:

- `frontend/admin-web/src/modules/production`
- Optional route entry from process/production-owned navigation only

Out-of-scope implementation paths:

- `frontend/customer-h5`
- `frontend/production-h5`
- `frontend/worker-uniapp`
- `frontend/screen-web`
- CRM, public pool, order core, contribution, inventory, attendance, dashboard implementation files

## Collaboration Boundaries

Codex owns production dispatch, production instances, production progress, and frozen production process rules.

Cursor owns CRM, customer public pool, customer archive, contribution system, order creation, and order core business logic.

The two developers are split by business capability, not by frontend/backend.

Production may read the order-line shared contract:

```text
order
order_item
```

Production must not modify customer-line or order-line core data. Production may only write production fields on `order_item` after successful dispatch:

```text
production_status
production_progress
production_route_instance_id
```

Until the Cursor order-line module is available, the future implementation may use a mock/demo `order_item` adapter inside the backend production module. It must be isolated behind ports such as:

```text
OrderItemReadPort
OrderItemProductionPort
```

The adapter can provide only the minimum order item read and production-field write-back capabilities needed for dispatch. It must not implement order creation, order amount, customer data, quotation, product specification, order core status, or other customer-line/order-line logic.

The adapter must be clearly marked:

```text
TODO: replace with customer-line order_item contract
```

Production must not create CRM, public pool, contribution, or order core logic to make the demo work.

## Risks

- If production instances execute directly from templates, later template edits can corrupt active or completed production work.
- If dispatch is not idempotent, the same `order_item` can be dispatched multiple times and create duplicate production instances.
- If production writes order/customer core fields, it can break customer-line ownership and make Cursor/Codex changes conflict.
- If pre-dispatch configuration is confused with frozen instances, users may expect to edit production structure after workers start.
- If mock/demo `order_item` integration is not clearly isolated, it may become accidental order-core logic.
- If summary APIs calculate status from order-line fields instead of production instances, order pages may show inconsistent production state.
