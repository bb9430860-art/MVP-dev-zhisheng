# Production Dispatch Demo Acceptance

## Scope

This document records the local demo acceptance path for production dispatch and frozen production instances.

This stage only verifies the existing production dispatch API and `admin-web` production configuration page. It does not add new business modules.

## Demo Data

The backend `dev` profile uses an H2 in-memory database and loads Flyway locations:

```text
classpath:db/migration,classpath:db/dev-migration
```

The dev-only seed file `db/dev-migration/V900__seed_process_route_template_demo_data.sql` creates enabled process route templates:

```text
RT-SPIRIT-FORTRESS / SPIRIT_FORTRESS
RT-FLOOR-SIGN / FLOOR_SIGN
RT-ILLUMINATED-LETTER / ILLUMINATED_LETTER
RT-WAYFINDING-SIGN / WAYFINDING_SIGN
RT-GENERAL-SIGN / GENERAL
```

The dev/test mock order item adapter includes demo order item `1001`:

```text
orderItemId: 1001
orderId: 501
itemName: 入口精神堡垒
productType: SPIRIT_FORTRESS
quantity: 1
```

The adapter is temporary and isolated behind production-owned ports. It is marked with:

```text
TODO: replace with customer-line order_item contract
```

It must not become order creation, order amount, customer, quotation, product specification, or order core status logic.

## Startup

Run backend tests first:

```powershell
cd backend
mvn -pl zhisheng-app -am test
```

Install backend modules before starting the app. This keeps local Maven artifacts in sync when `zhisheng-production` API classes changed:

```powershell
cd backend
mvn -pl zhisheng-app -am install
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

Open the demo URL:

```text
http://127.0.0.1:5173/production/order-items/1001/configure
```

## Operation Steps

1. Open `/production/order-items/1001/configure`.
2. Confirm the page displays demo order item context:
   - product/location: `入口精神堡垒`
   - order ID: `501`
   - product type: `SPIRIT_FORTRESS`
   - production status: `NOT_DISPATCHED`
3. Select a route template. The options should include the exact `SPIRIT_FORTRESS` route template and the `GENERAL` route template.
4. Click "从模板生成工序".
5. Confirm editable steps are generated from the template.
6. Before dispatch, verify the page allows:
   - move step up
   - move step down
   - add step
   - delete step
   - edit step name
   - edit `assigned_role`
   - leave `assigned_user_id` empty
   - edit `photo_required`
   - edit `remark_required`
   - edit `mobile_enabled`
7. Click "确认下发生产".
8. Confirm the backend creates:
   - one `production_route_instance`
   - ordered `production_step_instance` records
9. Confirm the route instance has `frozen = true`.
10. Confirm each step instance initial status is `PENDING`.
11. Confirm the mock order item write-back only changes:
    - `production_status`
    - `production_progress`
    - `production_route_instance_id`
12. After the page refreshes summary, confirm it displays `frozen=true`.
13. Try to edit structure again. The page should disable template selection, route name, step order, step name, role, assignee, photo/remark/mobile flags, add, delete, move up/down, and manual order save.

## API Checks

Before dispatch:

```http
GET /api/production/order-items/1001/config-context
GET /api/process/route-templates/options?productType=SPIRIT_FORTRESS
GET /api/production/order-items/1001/summary
```

Expected before dispatch:

```text
config-context returns orderItem.id = 1001
options returns SPIRIT_FORTRESS template and GENERAL template
summary returns dispatched = false
summary returns frozen = false
```

After dispatch:

```http
GET /api/production/order-items/1001/summary
```

Expected after dispatch:

```text
productionStatus = DISPATCHED
productionProgress = 0
productionRouteInstanceId is not null
dispatched = true
frozen = true
totalSteps > 0
completedSteps = 0
currentStepName is the first pending step
```

## How To Confirm Frozen Works

UI evidence:

- The summary card shows `frozen=true`.
- The warning says the production instance is frozen and structure cannot be edited.
- Structural controls are disabled after dispatch.

API/data evidence:

- `production_route_instance.frozen = true`.
- `production_step_instance.status = PENDING` after dispatch.
- Re-dispatching the same order item returns `ORDER_ITEM_ALREADY_DISPATCHED`.
- Later edits to process templates do not change existing production instances.

## Order Boundary Check

Production dispatch consumes `order_item` through `OrderItemReadPort` and `OrderItemProductionPort`.

In the dev/demo stage, `MockOrderItemAdapter` can only:

```text
read minimum order item production context
write production_status
write production_progress
write production_route_instance_id
```

It must not implement:

```text
order creation
order amount
customer data
quotation data
product specification ownership
order core status ownership
CRM logic
public pool logic
contribution logic
```

## Out Of Scope

This demo acceptance stage does not implement:

- CRM
- customer public pool
- contribution value
- order creation
- order detail core logic
- order amount, customer, quotation, specification, quantity, or other order core field changes
- `customer-h5`
- `production-h5`
- `worker-uniapp`
- `screen-web`
- worker tasks
- step start or completion
- photo check-in
- file upload
- inventory
- attendance
- boss dashboard
- real order-line adapter
- `production_step_checkin`
- inventory, attendance, or dashboard tables
- BPM
- rework, skip, or execution-time step insertion

## Known Limits

- The demo order item is in memory and resets when the dev backend restarts.
- The mock adapter is available only in `dev` and `test` profiles.
- The production dispatch API is currently profile-limited for the same reason.
- The first version does not persist dispatch drafts.
- `assigned_user_id` is optional and may stay empty in the demo.
- Worker execution is intentionally not part of this stage.
