# Process Route Template Design

## Overview

This change introduces a production-side template layer composed of route templates and step templates. A route template describes a reusable process route for a product type. Step templates describe ordered operations inside that route.

The implementation must stay template-only. It must not create production instances, dispatch orders, or expose worker execution behavior.

## Data Model

### `process_route_template`

Planned fields:

```text
id
tenant_id
route_code
route_name
product_type
description
enabled
version
created_by
created_at
updated_by
updated_at
deleted
delete_marker
```

Notes:

- `product_type` is a string or dictionary value. Business logic must not hard-code product type branches in the template module.
- `enabled` controls whether the template can be selected later.
- `deleted` supports soft deletion. Deleted templates are hidden from normal list and option APIs.
- `delete_marker` is `0` for active records. When soft deleted, it is set to the current record id or a timestamp value so unique indexes do not conflict with future active records.
- `version` is reserved for optimistic locking or edit conflict detection.

### `process_step_template`

Planned fields:

```text
id
tenant_id
route_template_id
step_code
step_name
step_order
assigned_role
photo_required
remark_required
mobile_enabled
estimated_hours
operation_instruction
enabled
created_by
created_at
updated_by
updated_at
deleted
delete_marker
```

Notes:

- `assigned_role` uses system roles such as `PRODUCTION_MANAGER`, `WORKER`, or `WAREHOUSE`.
- The template stage must not strongly bind a step to a concrete employee. Concrete assignment belongs to later production configuration or dispatch.
- `photo_required`, `remark_required`, and `mobile_enabled` are copied into future production step snapshots.
- Step deletion uses soft deletion.
- `delete_marker` is `0` for active records. When soft deleted, it is set to the current record id or a timestamp value.

## Sorting

The first version supports:

- Move step up.
- Move step down.
- Save explicit manual order values.

Drag-and-drop sorting is not required for the first version.

After each reorder operation, active steps under a route template should be normalized to contiguous `step_order` values starting from 1.

The database uniqueness strategy for `route_template_id + step_order` must use a delete marker. Soft-deleted rows must not block active rows from reusing an order number.

Required soft delete fields:

```text
deleted: TINYINT, 0 or 1
delete_marker: BIGINT, 0 for active rows, current record id or timestamp for soft-deleted rows
```

The active step order unique index should be:

```text
tenant_id + route_template_id + step_order + delete_marker
```

Do not use `tenant_id + route_template_id + step_order + deleted` as the final unique constraint. A boolean `deleted` value can still conflict when multiple historical soft-deleted rows share the same route and order.

## Template Lifecycle

Templates support these administrative states:

```text
enabled = true
enabled = false
deleted = true
```

Rules:

- Enabled templates can appear in the template option API.
- Disabled templates remain editable but cannot be selected for new production configuration.
- Deleted templates are hidden by default and cannot be selected.
- A template cannot be enabled unless it has at least one active enabled step.
- Hard deletion is not allowed for route templates or step templates.

## API Shape

Route template APIs:

```http
GET    /api/process/route-templates
POST   /api/process/route-templates
GET    /api/process/route-templates/{id}
PUT    /api/process/route-templates/{id}
PATCH  /api/process/route-templates/{id}/enabled
DELETE /api/process/route-templates/{id}
```

Step template APIs:

```http
GET    /api/process/route-templates/{routeTemplateId}/steps
POST   /api/process/route-templates/{routeTemplateId}/steps
PUT    /api/process/route-templates/{routeTemplateId}/steps/{stepId}
DELETE /api/process/route-templates/{routeTemplateId}/steps/{stepId}
PUT    /api/process/route-templates/{routeTemplateId}/steps/reorder
```

Template option API:

```http
GET /api/process/route-templates/options?productType={productType}
```

When `productType` is provided, the option API returns enabled and non-deleted templates where:

```text
product_type = {productType}
OR product_type = GENERAL
OR product_type is empty
```

This is a data-driven match. The template module must not hard-code business branches for specific product types such as spirit fortress, floor sign, illuminated letters, or wayfinding signs.

All APIs must use the shared response envelope:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## Frontend Design

The admin web process module should provide:

- Route template list page.
- Route template create and edit page.
- Step template table inside the edit page.
- Step create and edit form.
- Controls for move up, move down, and manual order save.
- Enable and disable action for route templates.

The first version does not require a drag-and-drop UI.

The first version does not add links from order pages, because order binding is outside this change.

## Future Production Instance Constraint

Future production dispatch must copy route and step template data into production instance snapshot tables. Production execution must never directly reference templates as the live source of truth.

Required future copy behavior:

```text
process_route_template
→ snapshot copy
→ production_route_instance

process_step_template
→ snapshot copy
→ production_step_instance
```

This protects the rule:

```text
Template can change. Instance cannot be disturbed.
```

## Out of Scope

This change must not introduce:

- Production route instance tables.
- Production step instance tables.
- Production check-in tables.
- Worker mobile pages.
- Order item production configuration pages.
- Inventory tables or APIs.
- Attendance tables or APIs.
- Dashboard aggregation.
- Customer-line, order-line, or contribution-line modifications.
