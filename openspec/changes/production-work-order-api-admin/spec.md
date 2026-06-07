# Production Work Order API And Admin Specification

## ADDED Requirements

### Requirement: Provide production work order API

The system SHALL provide production work order API designs for managing production work orders from admin-web in a later implementation.

#### Scenario: List work orders

- GIVEN production work orders exist
- WHEN a production manager queries `GET /api/production/work-orders`
- THEN the API returns a paged list of work orders
- AND supports draft filters for status, work order number, order item id, date range, keyword, and route link state
- AND includes whether `production_route_instance_id` is linked

#### Scenario: Get detail

- GIVEN a production work order exists
- WHEN a user queries `GET /api/production/work-orders/{workOrderId}`
- THEN the API returns work order base information
- AND returns production instruction fields
- AND returns technical configuration fields
- AND returns material requirement lines
- AND returns route instance link information

#### Scenario: Create from order item

- GIVEN an `order_item` is readable by production
- AND no active work order exists for the same `tenant_id + order_item_id`
- WHEN a production manager posts to `POST /api/production/work-orders/from-order-item`
- THEN the API creates a `DRAFT` production work order in the later implementation
- AND uses the backend-generated `WO-{yyyyMMdd}-{dailySequence}` work order number
- AND may save material requirement lines
- AND does not create a production route instance

#### Scenario: Reject duplicate active work order

- GIVEN an active work order exists for the same `tenant_id + order_item_id`
- AND active statuses are `DRAFT`, `RELEASED`, and `IN_PROGRESS`
- WHEN a production manager creates another work order from the same order item
- THEN the API rejects the request with `WORK_ORDER_ALREADY_EXISTS_FOR_ORDER_ITEM`
- AND does not automatically reuse the existing work order

#### Scenario: Update draft

- GIVEN a work order is `DRAFT`
- WHEN a production manager updates `PUT /api/production/work-orders/{workOrderId}`
- THEN the API updates editable base information in the later implementation
- AND does not change order core fields
- AND does not update material lines through this base endpoint

#### Scenario: Reject update after released

- GIVEN a work order status is `RELEASED`, `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`
- WHEN a user attempts to update base information
- THEN the API rejects the edit
- AND returns a business error such as `WORK_ORDER_EDIT_NOT_ALLOWED` or `WORK_ORDER_INVALID_STATUS_TRANSITION`

#### Scenario: Release draft

- GIVEN a work order is `DRAFT`
- WHEN a production manager posts to `POST /api/production/work-orders/{workOrderId}/release`
- THEN the API changes status to `RELEASED` in the later implementation
- AND records release user and time
- AND does not dispatch production
- AND does not reserve or deduct inventory

#### Scenario: Cancel draft or released

- GIVEN a work order is `DRAFT` or `RELEASED`
- WHEN a production manager posts to `POST /api/production/work-orders/{workOrderId}/cancel`
- THEN the API changes status to `CANCELLED` in the later implementation
- AND rejects cancellation for `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`

### Requirement: Provide order item candidate read API

The system SHALL design a production-side read API for order item candidates that can become work orders.

#### Scenario: List readable order item candidates

- WHEN a user queries `GET /api/production/work-orders/order-items/candidates`
- THEN the API returns order item fields needed for production work order creation
- AND includes `order_item.id`, `order_id`, item name, product type, quantity, production status, progress, and route instance id when available
- AND does not expose order editing behavior

#### Scenario: Do not mutate order core fields

- WHEN candidates are listed or used to create a work order
- THEN the system does not mutate order amount, quotation, customer information, product specification, product quantity, or order core status

#### Scenario: Hide or mark order item with existing active work order

- GIVEN an order item already has an active production work order
- WHEN the candidate list is queried
- THEN the API either hides the candidate or marks it as unavailable
- AND if it is shown, it includes enough state for the UI to disable duplicate creation

### Requirement: Provide material requirement editing API

The system SHALL design a material requirement editing API for DRAFT work orders.

#### Scenario: Edit materials in DRAFT

- GIVEN a work order is `DRAFT`
- WHEN a production manager updates `PUT /api/production/work-orders/{workOrderId}/materials`
- THEN the API replaces or updates the material requirement list in the later implementation
- AND keeps the lines as demand-only material requirements

#### Scenario: Reject invalid material name

- GIVEN a material line has a blank or missing `material_name`
- WHEN the material list is saved
- THEN the API rejects the request with `MATERIAL_REQUIREMENT_INVALID`

#### Scenario: Reject non-positive quantity

- GIVEN a material line has `required_qty <= 0`
- WHEN the material list is saved
- THEN the API rejects the request with `MATERIAL_REQUIREMENT_INVALID`

#### Scenario: Do not deduct stock

- WHEN material requirements are created or edited
- THEN the system does not deduct inventory
- AND does not change stock balance

#### Scenario: Do not create inventory transaction

- WHEN material requirements are created or edited
- THEN the system does not create `inventory_transaction`
- AND does not create stock in/out records

### Requirement: Provide admin work order list page

The system SHALL design an admin-web work order list page for the production module.

#### Scenario: Show work orders

- WHEN a production user opens `/production/work-orders`
- THEN the page displays production work orders from the production API
- AND shows work order number, status, order item snapshot, schedule, owner, and route link state

#### Scenario: Filter by status

- WHEN a user selects a status filter
- THEN the page queries the list by `DRAFT`, `RELEASED`, `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`

#### Scenario: Search by work order number or keyword

- WHEN a user enters a work order number or product/location keyword
- THEN the page queries matching work orders

#### Scenario: Show route instance linked or not

- WHEN a work order has `production_route_instance_id`
- THEN the list shows it as linked to a production instance
- AND when it is null, the list shows it as not yet dispatched or not yet linked

### Requirement: Provide admin work order detail and edit flow

The system SHALL design admin-web detail and edit interactions for production work orders.

#### Scenario: View detail

- WHEN a user opens a work order detail
- THEN the UI shows instruction, technical configuration, schedule, people, material requirements, and route link state

#### Scenario: Edit DRAFT

- GIVEN a work order is `DRAFT`
- WHEN a production manager edits it
- THEN the UI allows editing base fields and material requirements

#### Scenario: Release DRAFT

- GIVEN a work order is `DRAFT`
- WHEN a production manager clicks release
- THEN the UI calls the release action
- AND updates the visible status to `RELEASED` after success

#### Scenario: Cancel DRAFT or RELEASED

- GIVEN a work order is `DRAFT` or `RELEASED`
- WHEN a production manager cancels it
- THEN the UI calls the cancel action
- AND updates the visible status to `CANCELLED` after success

#### Scenario: Reject edit for in-progress completed or cancelled

- GIVEN a work order is `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`
- WHEN the user views it
- THEN the UI does not show editable controls
- AND backend still rejects direct edit attempts

### Requirement: Preserve dispatch boundary

The system SHALL preserve the boundary between work order API/admin management and production dispatch.

#### Scenario: This change does not create route instance

- WHEN a work order is created, edited, released, or cancelled
- THEN this change does not create `production_route_instance`

#### Scenario: This change does not freeze route instance

- WHEN a work order is managed through admin-web
- THEN this change does not freeze route structure
- AND does not create or modify `production_step_instance`

#### Scenario: This change only shows or links route instance status

- WHEN a work order has a `production_route_instance_id`
- THEN the page may show the link state
- AND the draft link endpoint may validate same tenant and same order item
- AND it must not mutate frozen route structure

#### Scenario: Work-order-driven dispatch is future change

- WHEN this OpenSpec is applied
- THEN existing `production-dispatch-instance` remains unchanged
- AND work-order-driven dispatch requires a separate later change

### Requirement: Preserve inventory boundary

The system SHALL preserve the boundary between material requirements and inventory/material-readiness.

#### Scenario: Material requirements are demand only

- WHEN material requirements are shown or edited
- THEN they represent demand only
- AND they are not stock readiness results

#### Scenario: No reservation deduction or stock transaction

- WHEN a work order material line is saved
- THEN the system does not reserve stock
- AND does not deduct stock
- AND does not create inventory transaction
- AND does not create stock in/out records

#### Scenario: No purchase supplier or finance

- WHEN material demand is entered
- THEN this change does not create purchase requests
- AND does not create supplier records
- AND does not create finance records

#### Scenario: Readiness is future change

- WHEN users need available quantity, shortage, node shortage display, or step start blocking
- THEN that behavior belongs to a later inventory/material-readiness change

### Requirement: Preserve order-line boundary

The system SHALL preserve customer-line and order-line ownership.

#### Scenario: Only read order item

- WHEN production lists candidates or creates a work order
- THEN it only reads `order_item` through a production-side read contract or agreed API

#### Scenario: No order creation

- WHEN using work order API/admin flows
- THEN the system does not create orders

#### Scenario: No order amount quotation customer spec quantity or core status mutation

- WHEN work orders are created, edited, released, cancelled, or linked
- THEN the system does not modify order amount, quotation, customer information, product specification, product quantity, or order core lifecycle status

#### Scenario: No CRM public pool or contribution

- WHEN implementing work order API/admin in the future
- THEN the implementation must not add CRM, customer public pool, or contribution value logic

## DoD

- The OpenSpec change exists under `openspec/changes/production-work-order-api-admin`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change explicitly states that it is OpenSpec-only.
- This change explicitly forbids Java business code, Controller code, Mapper/Service/Entity changes, Flyway migration, Vue pages, TypeScript API files, routes, package metadata, and backend core changes.
- API endpoints are designed but not implemented.
- Admin-web pages are designed but not implemented.
- Work order statuses remain `DRAFT`, `RELEASED`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED`.
- DRAFT edit and material edit rules are documented.
- RELEASED and later edit restrictions are documented.
- Material requirements are demand-only.
- Inventory reservation, deduction, stock in/out, inventory transaction, purchase, supplier, and finance are excluded.
- Existing `production-dispatch-instance` is not refactored.
- This change does not create or freeze route instances.
- Work-order-driven dispatch is documented as a future change.
- Production only reads `order_item` and does not mutate order core fields.
- CRM, public pool, contribution, and order core logic are excluded.
- No completion claim is allowed without verification evidence.
