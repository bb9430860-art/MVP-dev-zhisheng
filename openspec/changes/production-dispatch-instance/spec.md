# Production Dispatch Instance Specification

## ADDED Requirements

### Requirement: Configure production route before dispatch

The system SHALL allow a production manager to configure a production route for one `order_item` before confirming dispatch.

#### Scenario: Load order item configuration context

- GIVEN an `order_item` exists through the customer-line order contract or a dev-only mock/demo adapter
- WHEN the production manager opens production configuration for that `order_item`
- THEN the system provides the order item production context
- AND includes product or location information needed to choose a route template
- AND does not modify customer-line or order-line core data
- AND reads through a production-owned port such as `OrderItemReadPort`

#### Scenario: Select route template for order item

- GIVEN enabled and non-deleted route templates exist
- WHEN the production manager selects a route template for an `order_item`
- THEN the system reads the selected route template
- AND reads enabled and non-deleted step templates under that route template
- AND creates an editable request-scoped or frontend-held pre-dispatch configuration model
- AND does not persist a dispatch draft

#### Scenario: Adjust steps before dispatch

- GIVEN a pre-dispatch configuration model exists
- WHEN the production manager edits the configuration before confirm dispatch
- THEN the manager can reorder steps
- AND can add steps
- AND can delete steps
- AND can edit step name
- AND must keep `assigned_role` on every step
- AND can edit optional responsible or executing user through nullable `assigned_user_id`
- AND can edit whether photo evidence is required
- AND can edit whether remark text is required
- AND can edit whether mobile execution is allowed

#### Scenario: Submit complete configuration on confirm dispatch

- GIVEN the production manager has edited the pre-dispatch configuration
- WHEN dispatch is confirmed
- THEN the frontend submits the full route and step configuration in one request
- AND the backend persists only `production_route_instance` and `production_step_instance` after successful confirm dispatch
- AND the backend does not create or update a `dispatch_draft` table

### Requirement: Copy route template snapshots into production instances

The system SHALL copy route and step template data into production instance snapshot records when dispatch is confirmed.

#### Scenario: Confirm dispatch creates production route instance

- GIVEN an `order_item` is not dispatched
- AND a valid dispatch configuration contains at least one step
- WHEN the production manager confirms dispatch
- THEN the system creates one `production_route_instance`
- AND stores route snapshot fields copied from the selected template and dispatch configuration
- AND stores the related `order_id` and `order_item_id`
- AND rejects dispatch with `ORDER_ITEM_ALREADY_DISPATCHED` if the `order_item` already has an active production route instance

#### Scenario: Confirm dispatch creates production step instances

- GIVEN dispatch confirmation creates a production route instance
- WHEN configured steps are copied
- THEN the system creates ordered `production_step_instance` records
- AND copies step snapshot fields from the pre-dispatch configuration
- AND initializes each step status as `PENDING`
- AND keeps step order contiguous

#### Scenario: Template edits do not affect dispatched instances

- GIVEN a production route instance has been created from a route template
- WHEN the source route template or step templates are edited later
- THEN the production route instance remains unchanged
- AND the production step instances remain unchanged
- AND execution uses the instance snapshots rather than the template as live source

### Requirement: Freeze production route instances after dispatch

The system SHALL freeze production route instances immediately after successful dispatch.

#### Scenario: Route instance is frozen after dispatch

- WHEN dispatch succeeds
- THEN `production_route_instance.frozen` is `true`
- AND the route instance status is initialized as `DISPATCHED`
- AND the `order_item.production_status` is updated to a production dispatch status

#### Scenario: Step instances are frozen or route freeze controls them

- WHEN dispatch succeeds
- THEN each production step instance is either marked `frozen = true`
- OR is structurally governed by `production_route_instance.frozen = true`
- AND the chosen freeze strategy is documented before implementation

### Requirement: Prevent structural changes after freeze

The system SHALL reject structural changes to a frozen production route.

#### Scenario: Add step after freeze is rejected

- GIVEN a production route instance is frozen
- WHEN a user attempts to add a production step
- THEN the system rejects the request
- AND returns a business error such as `PRODUCTION_ROUTE_FROZEN`

#### Scenario: Delete or reorder step after freeze is rejected

- GIVEN a production route instance is frozen
- WHEN a user attempts to delete a step or reorder steps
- THEN the system rejects the request
- AND does not change production step structure

#### Scenario: Skip, rework, or append during execution is rejected

- GIVEN a production route instance is frozen
- WHEN a user attempts to skip a step, trigger rework, or append execution-time steps
- THEN the system rejects the request
- AND keeps the frozen process structure unchanged

### Requirement: Preserve order-line ownership

The system SHALL preserve customer-line and order-line ownership while allowing limited production write-back.

#### Scenario: Production reads order item but does not own order core

- WHEN production configuration reads an `order_item`
- THEN the system treats `order_item` as an external shared contract
- AND does not implement CRM, customer public pool, contribution, or order core logic

#### Scenario: Dispatch writes only production fields to order item

- WHEN dispatch succeeds
- THEN production may update only:
  - `order_item.production_status`
  - `order_item.production_progress`
  - `order_item.production_route_instance_id`
- AND production must not update customer data, order amount, product specification, product quantity, quotation data, or order custom fields

#### Scenario: Mock order item adapter is isolated

- GIVEN the customer-line order module is not available
- WHEN production dispatch is implemented for dev/demo use
- THEN the implementation may use mock/demo `order_item` data through production-owned ports such as `OrderItemReadPort` and `OrderItemProductionPort`
- AND the adapter must be marked `TODO: replace with customer-line order_item contract`
- AND the adapter must not become customer-line order core logic
- AND the adapter can only read minimum order item fields and write production fields
- AND the adapter must not implement order creation, order amount, customer data, quotation, product specification ownership, or order core status ownership

#### Scenario: Duplicate dispatch returns conflict

- GIVEN an `order_item` already has an active production route instance
- WHEN dispatch is requested again
- THEN the system returns `ORDER_ITEM_ALREADY_DISPATCHED`
- AND does not create another route instance
- AND does not implement complex idempotent replay in MVP

### Requirement: Provide production summary for order items

The system SHALL provide a production summary API for order-line display.

#### Scenario: Query summary for dispatched order item

- GIVEN an `order_item` has a production route instance
- WHEN a client calls `GET /api/production/order-items/{orderItemId}/summary`
- THEN the response includes dispatch state
- AND includes `productionRouteInstanceId`
- AND includes production status
- AND includes progress
- AND includes total step count
- AND includes completed step count
- AND includes current step name when available
- AND includes frozen state

#### Scenario: Query summary for not dispatched order item

- GIVEN an `order_item` has not been dispatched
- WHEN a client calls the production summary API
- THEN the response indicates `dispatched = false`
- AND does not create a production instance

### Requirement: Keep worker execution out of this change

The system SHALL NOT implement worker execution behavior in this change.

#### Scenario: No worker task execution

- WHEN this change is applied
- THEN it does not implement worker task list, worker mobile pages, step start, step completion, photo check-in, or remark check-in

#### Scenario: Future execution may update status only through later change

- GIVEN production route and step instances are frozen
- WHEN a later worker execution change is implemented
- THEN that later change may update execution status fields
- AND must not change frozen route or step structure

## DoD

- The OpenSpec change exists under `openspec/changes/production-dispatch-instance`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change explicitly states that it does not write business code.
- This change explicitly states that it does not create database migrations.
- This change explicitly states that it does not persist dispatch drafts.
- This change explicitly forbids CRM, public pool, contribution, and order core logic.
- This change explicitly forbids worker check-in, inventory, attendance, dashboard, and screen-web work.
- Production dispatch results in `frozen = true`.
- Template changes after dispatch do not affect generated production instances.
- Production execution cannot directly use templates as the live execution source.
- Production only consumes `order_item` and may write back at most `production_status`, `production_progress`, and `production_route_instance_id`.
- Duplicate dispatch returns `ORDER_ITEM_ALREADY_DISPATCHED`.
- `assigned_role` is required and `assigned_user_id` is optional for dispatch steps.
- No completion claim may be made without verification evidence.

## Out of Scope

The system SHALL NOT implement worker photo check-in, worker mobile tasks, step start or completion, inventory, attendance, boss dashboard, `screen-web`, CRM, public pool, contribution value, file upload infrastructure, order core field modification, customer field modification, execution-time rework, execution-time step skipping, execution-time step insertion, or complex BPM workflow in this change.
