# Inventory Material Core Proposal

## Why

Process templates and production work orders can now express material demand through step material requirement templates and `production_work_order_material`, but the system still has no inventory foundation.

Without inventory, production can list required materials but cannot tell whether materials are available. The platform needs material master data, current stock balance, and stock transaction records before later readiness and shortage features can be reliable.

Inventory balance must not be changed manually without a transaction record. Every quantity change needs an auditable `inventory_transaction`, so later stock reports and production readiness decisions have a traceable source.

Work order readiness and shortage-by-step display depend on inventory data, but they are not part of this change. A later `work-order-material-readiness` change will compare `production_work_order_material` with `inventory_stock` and show shortage on the related process step.

## Goals

- Design material master data as `material_item`.
- Design current stock balance as `inventory_stock`.
- Design stock movement audit records as `inventory_transaction`.
- Support manual stock in.
- Support manual stock out.
- Support inventory adjustment.
- Support stock balance query.
- Support inventory transaction query.
- Preserve `tenant_id` on all inventory core data.
- Support material unit, specification, material code, enabled/disabled state, category, and remark.
- Keep `reserved_qty` and `available_qty` concepts so later readiness can use available quantity.
- Provide the data foundation for future `work-order-material-readiness`.

## Non-Goals

This change does not implement:

- work order material readiness check
- work order shortage calculation
- shortage display by process step
- step-start shortage guard
- automatic production material consumption
- inventory reservation
- purchase
- supplier
- finance
- cost accounting
- complex multi-warehouse or bin/location model unless a later MVP scope requires it
- barcode or RFID
- complete stocktaking workflow
- CRM
- customer public pool
- contribution value logic
- order creation
- order amount, quotation, customer, specification, quantity, or order core status mutation
- worker-uniapp
- production-h5
- screen-web

## Scope

OpenSpec scope:

```text
openspec/changes/inventory-material-core/
```

Allowed files in this change:

```text
proposal.md
design.md
spec.md
tasks.md
```

This change designs:

- `material_item`
- `inventory_stock`
- `inventory_transaction`
- admin-web inventory management draft
- manual stock operation rules
- the boundary between inventory core and future work order material readiness

Future implementation may add backend inventory APIs, persistence, tests, and admin-web management pages, but this OpenSpec-only change does not implement them.

## Collaboration Boundaries

Codex production line owns inventory core because it belongs to the production, process, inventory, and dashboard side of the platform.

Cursor customer line does not participate in this change.

This change does not read or mutate order core fields. Inventory records may later be referenced by production readiness, but inventory core does not create orders, update order amounts, change quotations, change customer data, or change order status.

This change does not modify `production_work_order_material`. It only provides the future stock foundation that readiness can compare against work order material demand.

## Risks

- If stock balance can be edited without transactions, inventory reports will not be auditable.
- If material master data is skipped, later template material requirements and work order material lines will remain free text and hard to match.
- If readiness is mixed into this change, inventory core will become coupled to production execution before stock rules are stable.
- If reservation is introduced too early, MVP stock behavior may become harder to explain and test.
- If multi-warehouse is introduced before it is needed, the MVP will carry unnecessary operational complexity.
