# Inventory Material Core Design

## 1. Overview

Inventory material core provides three foundations:

- material master data
- current stock balance
- stock transaction history

The core rule is that stock balance changes only through transactions. Manual in, manual out, and adjustment update `inventory_stock` and create `inventory_transaction` in the same transaction.

This change is OpenSpec-only. It does not implement Java code, migrations, APIs, Vue/TypeScript, admin-web pages, stock readiness, production consumption, purchase, supplier, finance, or customer-line behavior.

## 2. Relationship To Current Production Flow

Current production-side material concepts:

```text
process_step_material_requirement_template
-> standard material demand on process steps

production_work_order_material
-> work order material demand list

inventory-material-core
-> material master, stock balance, stock transactions
```

This change only designs the inventory foundation. It does not compare demand with stock.

Future `work-order-material-readiness` will compare:

```text
production_work_order_material.required_qty
vs
inventory_stock.available_qty
```

That future change owns shortage quantity, readiness status, shortage display by `related_step_template_id` or `usage_stage`, and any step-start warning or blocking logic.

## 3. Data Model Draft

### material_item

Field draft:

- `id`
- `tenant_id`
- `material_code`
- `material_name`
- `spec`
- `unit`
- `category`
- `enabled`
- `remark`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- `deleted`
- `delete_marker`

Rules:

- MVP chooses `material_code` as required and unique within one tenant for active records.
- `material_name` is required.
- `unit` is required.
- `enabled = false` means the material cannot receive new stock operations.
- Disabled material history remains queryable.
- Soft delete follows the existing `deleted` and `delete_marker` convention.

### inventory_stock

Field draft:

- `id`
- `tenant_id`
- `material_id`
- `material_code_snapshot`
- `material_name_snapshot`
- `spec_snapshot`
- `unit_snapshot`
- `on_hand_qty`
- `reserved_qty`
- `available_qty`
- `updated_at`

Rules:

- One tenant can have only one stock row for one `material_id`.
- `on_hand_qty >= 0`.
- `reserved_qty >= 0`.
- `available_qty = on_hand_qty - reserved_qty`.
- MVP does not implement reservation, so `reserved_qty` defaults to `0`.
- `available_qty` may be stored for query convenience or calculated consistently by service/query layer; if stored, it must be updated together with `on_hand_qty` and `reserved_qty`.
- Stock balance must not be changed without creating an `inventory_transaction`.

### inventory_transaction

Field draft:

- `id`
- `tenant_id`
- `material_id`
- `transaction_type`
- `qty`
- `before_on_hand_qty`
- `after_on_hand_qty`
- `before_reserved_qty`
- `after_reserved_qty`
- `reference_type` nullable
- `reference_id` nullable
- `reason`
- `remark`
- `operator_id`
- `occurred_at`
- `created_at`
- `idempotency_key` nullable

MVP `transaction_type` values:

- `MANUAL_IN`
- `MANUAL_OUT`
- `ADJUST_IN`
- `ADJUST_OUT`

Future transaction types may include:

- `RESERVE`
- `RELEASE_RESERVATION`
- `PRODUCTION_CONSUME`
- `PURCHASE_IN`
- `RETURN_IN`

Future types are only reserved design vocabulary. This change does not implement them.

## 4. Stock Operation Rules

Manual stock in:

- Requires enabled material.
- `qty > 0`.
- Creates stock row if it does not exist.
- Increases `on_hand_qty`.
- Leaves `reserved_qty` unchanged.
- Updates `available_qty`.
- Creates `inventory_transaction` with before and after quantities.

Manual stock out:

- Requires enabled material.
- Requires existing stock row.
- `qty > 0`.
- Decreases `on_hand_qty`.
- Must not make `on_hand_qty < reserved_qty`.
- Must not make `on_hand_qty < 0`.
- Updates `available_qty`.
- Creates `inventory_transaction`.

Inventory adjustment:

- Requires enabled material.
- `qty > 0`.
- Adjustment up creates `ADJUST_IN`.
- Adjustment down creates `ADJUST_OUT`.
- Adjustment down follows the same negative-stock and reserved-quantity guards as manual out.
- Creates `inventory_transaction`.

Global rules:

- Every balance change must create `inventory_transaction`.
- Stock and transaction writes happen in one database transaction.
- Negative stock is forbidden.
- Direct stock update without transaction is forbidden.
- Idempotency key may be used to prevent duplicated external/manual submits.

## 5. API Draft

Future API drafts only. This OpenSpec does not implement them.

Material:

```http
GET /api/inventory/materials
POST /api/inventory/materials
PUT /api/inventory/materials/{materialId}
POST /api/inventory/materials/{materialId}/enable
POST /api/inventory/materials/{materialId}/disable
```

Stock:

```http
GET /api/inventory/stocks
GET /api/inventory/stocks/{materialId}
```

Transactions:

```http
GET /api/inventory/transactions
POST /api/inventory/transactions/manual-in
POST /api/inventory/transactions/manual-out
POST /api/inventory/transactions/adjust
```

Notes:

- Material APIs manage master data only.
- Stock APIs query current balance only.
- Transaction APIs are the only entry points that change stock quantities.
- Future readiness APIs should be defined in `work-order-material-readiness`, not here.

## 6. Admin-Web Draft

Suggested pages:

```text
/inventory/materials
/inventory/stocks
/inventory/transactions
```

MVP may also use one inventory management page with tabs:

- material item list
- create/edit material item
- enable/disable material
- stock balance list
- manual stock in
- manual stock out
- inventory adjustment
- transaction record list

The UI must not present work order readiness, shortage-by-step, stock reservation, purchase status, supplier status, or finance data in this change.

## 7. Work Order / Readiness Boundary

This change does not:

- read `production_work_order_material` for readiness
- calculate shortage
- calculate `readiness_status`
- show shortage on process steps
- block work order release
- block production dispatch
- block step start
- reserve stock for work order demand
- automatically deduct stock for work order demand

Future `work-order-material-readiness` owns:

- `required_qty` vs `available_qty`
- `shortage_qty`
- `readiness_status`
- shortage display by `related_step_template_id`
- shortage display by `usage_stage`
- possible step-start reminder or guard

## 8. Process Template Material Boundary

This change does not modify `process_step_material_requirement_template`.

Future implementation may allow `process_step_material_requirement_template.material_id` to reference `material_item.id`.

This change does not automatically match existing template material names to material items, and it does not repair historical template material rows.

## 9. Production Dispatch Boundary

This change does not modify work-order-driven dispatch or legacy direct dispatch.

Production dispatch does not deduct inventory.

Step completion does not deduct inventory.

Automatic production consumption belongs to a future change and must not be introduced as a side effect of inventory core.

## 10. Error Handling Draft

Error code draft:

- `MATERIAL_NOT_FOUND`
- `MATERIAL_CODE_DUPLICATED`
- `MATERIAL_NAME_REQUIRED`
- `MATERIAL_UNIT_REQUIRED`
- `MATERIAL_DISABLED`
- `INVENTORY_STOCK_NOT_FOUND`
- `INVENTORY_QTY_INVALID`
- `INVENTORY_INSUFFICIENT_STOCK`
- `INVENTORY_NEGATIVE_STOCK_NOT_ALLOWED`
- `INVENTORY_TRANSACTION_REQUIRED`
- `INVENTORY_IDEMPOTENCY_KEY_DUPLICATED`

Validation should reject invalid material fields and invalid stock quantities before changing stock.

## 11. Out Of Scope

This OpenSpec does not implement Java code, Controller/API, Service, Mapper, Entity, Flyway migration, Vue/TypeScript, admin-web pages, work order readiness, shortage calculation, shortage display by step, step-start blocking, automatic production consume, reservation, purchase, supplier, finance, cost accounting, complex warehouse/bin model, barcode/RFID, full stocktaking workflow, CRM, public pool, contribution, order creation, order amount or quotation mutation, customer field mutation, product spec or quantity mutation, order core status mutation, worker-uniapp, production-h5, or screen-web.
