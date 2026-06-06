# Process Route Template Proposal

## Why

The production line needs a configurable process template layer before any order item can be dispatched to production. Signage and non-standard manufacturing orders contain different product or location types, so production managers need reusable route templates such as spirit fortress, floor sign, illuminated letters, and wayfinding signs.

This change creates the first production-side capability: process route templates and step templates. It gives administrators a controlled way to define reusable process routes while preserving the core rule of the project:

```text
Template can change. Future production instances must remain stable.
```

## Goals

- Provide CRUD for process route templates.
- Provide CRUD for process step templates under a route template.
- Support step ordering by moving up, moving down, and saving explicit order values.
- Configure step execution role using system roles.
- Configure whether each step requires photo evidence.
- Configure whether each step requires a remark.
- Configure whether each step can be executed on mobile.
- Configure applicable product type as a string or dictionary value.
- Support enabling and disabling route templates.
- Provide a template option API for later production configuration pages.

## Non-Goals

- Do not create production route instances.
- Do not bind templates to orders or order items.
- Do not dispatch production.
- Do not create worker tasks.
- Do not implement photo check-in.
- Do not implement inventory.
- Do not implement attendance.
- Do not implement dashboards or screen views.
- Do not modify CRM, customer, order, or contribution modules.
- Do not modify customer-line tables or customer-line business rules.

## Scope

Backend scope:

- `backend/zhisheng-process`
- `backend/zhisheng-app` only for wiring the process module when implementation starts
- Flyway migration under the app migration path when implementation starts

Frontend scope:

- `frontend/admin-web/src/modules/process`
- Shared frontend types only if needed by the process module

OpenSpec scope:

- `openspec/changes/process-route-template`

## Collaboration Boundaries

This change does not require customer-line APIs. It prepares the template selection contract that later production configuration will consume after the order line provides `order_item.product_type`.

The customer line may later call production summary APIs, but this change does not provide production status or progress because no production instance exists yet.

## Risks

- Soft deletion can conflict with `route_template_id + step_order` if uniqueness ignores deletion state.
- Hard-coding product types would make templates brittle across future signage categories.
- Binding a step template to a concrete employee too early would make templates unusable across shifts and teams.
- If future production execution directly references templates, editing templates could corrupt active production work. This change must explicitly document that future instances copy snapshots.
