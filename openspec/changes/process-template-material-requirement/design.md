# Process Template Material Requirement Design

## 1. Overview

This change designs standard material requirements during process route template editing. A process step template can define the materials normally needed by that step, including quantity rules and usage stage.

The design creates a template-level demand source. A future work order material generation change can copy these records into `production_work_order_material` according to the selected process template and `order_item.quantity`.

This change is OpenSpec-only. It does not create tables, migrations, APIs, Java code, Vue/TypeScript code, inventory logic, work order generation logic, or dispatch behavior.

## 2. Current Process Template Model

Current process template concepts:

- `process_route_template`: standard process route template.
- `process_step_template`: standard process step template under a route template.
- `step_order`: current process execution order is linear and serial.

The current model can define what steps should happen, but not what material each step needs.

## 3. Target Material Requirement Template Model

Future table draft:

```text
process_step_material_requirement_template
```

Field draft:

- `id`
- `tenant_id`
- `route_template_id`
- `step_template_id`
- `material_id` nullable
- `material_code` nullable
- `material_name`
- `spec` nullable
- `unit`
- `base_qty_per_unit` nullable
- `fixed_qty` nullable
- `loss_rate` nullable
- `required_qty_expression` nullable
- `usage_stage` nullable
- `remark` nullable
- `enabled`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- `deleted`
- `delete_marker`

Rules:

- `material_id` is nullable because material master and inventory may not exist yet.
- `material_name` is required.
- `unit` is required.
- `base_qty_per_unit` describes material demand per one unit of `order_item.quantity`.
- `fixed_qty` describes fixed material demand per work order item, independent from quantity.
- `loss_rate` is an optional multiplier for waste, such as 5%.
- `required_qty_expression` is reserved for future advanced rules and does not need to be implemented in MVP.
- `step_template_id` tells which process step uses the material.
- `usage_stage` can be used for display when strict step binding is not enough, such as cutting, assembly, electrical, or packaging.

## 4. Quantity Calculation Draft

Future generation calculation:

```text
requiredQty = baseQtyPerUnit * orderItem.quantity + fixedQty
```

If `lossRate` is provided:

```text
requiredQty = requiredQty * (1 + lossRate)
```

Examples:

- If one product needs 2 power supplies and `order_item.quantity = 3`, then `requiredQty = 6`.
- If fixed auxiliary material is one batch, then `fixedQty = 1`.
- If calculated demand is 10 and `lossRate = 0.05`, then `requiredQty = 10.5`.

MVP should support simple numeric rules first. It should not introduce a complex expression engine. `required_qty_expression` remains a future extension field.

Validation draft:

- At least one of `base_qty_per_unit`, `fixed_qty`, or future `required_qty_expression` should describe demand.
- Numeric quantities must be greater than or equal to zero.
- The final calculated demand must be greater than zero when generating work order material demand.
- `loss_rate` must not be negative. A future implementation may cap it according to business rules.

## 5. Mapping To Production Work Order Material

Future generation mapping:

```text
process_step_material_requirement_template
-> production_work_order_material
```

Mapping draft:

- `material_id` -> `material_id`
- `material_code` -> `material_code`
- `material_name` -> `material_name`
- `spec` -> `spec`
- `unit` -> `unit`
- calculated quantity -> `required_qty`
- `usage_stage` -> `usage_stage`
- `step_template_id` -> `related_step_template_id`
- `related_step_instance_id` remains null before dispatch
- `remark` -> `remark`

`requirement_status` should be initialized as demand-only, such as `DEMAND_ONLY` or `PENDING_CHECK`. The exact value belongs to the later inventory/material-readiness change.

This OpenSpec does not implement generation logic. Existing manual editing of `production_work_order_material` remains unchanged.

## 6. Admin-Web Template Editing Draft

Future admin-web process route template editing should allow material demand editing at step level.

Possible interaction designs:

- Expand each step row to edit material requirements.
- Add a "Material Requirements" tab inside the step edit dialog.

Expected controls:

- add material
- delete material
- edit material name
- edit specification
- edit unit
- edit base quantity per unit
- edit fixed quantity
- edit loss rate
- edit usage stage
- edit remark
- enable or disable a template material line

The UI should show a boundary warning:

```text
Template material requirements are demand templates only. They do not reserve stock, deduct stock, or prove inventory readiness.
```

## 7. Inventory Boundary

This change does not:

- query inventory
- reserve inventory
- deduct inventory
- create stock in/out
- create `inventory_transaction`
- check available quantity
- generate shortage
- calculate material readiness
- block work order creation, dispatch, or step start

Future inventory/material-readiness owns stock availability, shortage calculation, shortage display by step, and step-start shortage guard.

## 8. Work Order Boundary

This change does not modify existing `production_work_order` APIs, admin pages, services, or database schema.

Existing manual `production_work_order_material` editing remains available.

Future work-order material generation should be a separate change. That change can use:

```text
order_item.quantity
process_step_material_requirement_template
production_work_order
production_work_order_material
```

It must preserve order-line ownership and must not mutate order amount, quotation, customer fields, product specification, product quantity, or order core status.

## 9. Dispatch Boundary

This change does not affect work-order-driven dispatch.

Production dispatch still starts from a released work order, selects a route template, creates frozen route and step instances, and links the route instance to the work order.

Template material requirements do not block dispatch in this change.

## 10. Process Graph Boundary

Current template material requirements attach to linear `process_step_template` records.

Future route graph design should map material requirements to executable `TASK` leaf nodes:

- `GROUP` nodes organize process structure and do not directly consume material.
- `TASK` nodes represent executable work and can own material demand.
- Shortage display and step-start checks should apply to `TASK` leaf nodes.

This change does not implement GROUP/TASK, dependency graph, parallel branches, nested process, or process graph migration.

## 11. Future Implementation API Draft

Future API drafts only:

```http
GET /api/process/route-templates/{routeTemplateId}/step-materials
PUT /api/process/route-templates/{routeTemplateId}/steps/{stepTemplateId}/materials
POST /api/process/route-templates/{routeTemplateId}/generate-material-preview
```

Notes:

- `GET step-materials` may return materials grouped by `stepTemplateId`.
- `PUT step materials` may replace the material requirement template list for one step.
- `generate-material-preview` may calculate demand from a sample quantity or order item context, but it is not part of this change.

## 12. Error Handling Draft

Error code draft:

- `PROCESS_ROUTE_TEMPLATE_NOT_FOUND`
- `PROCESS_STEP_TEMPLATE_NOT_FOUND`
- `STEP_MATERIAL_REQUIREMENT_INVALID`
- `MATERIAL_NAME_REQUIRED`
- `MATERIAL_UNIT_REQUIRED`
- `MATERIAL_QUANTITY_RULE_INVALID`
- `MATERIAL_LOSS_RATE_INVALID`
- `STEP_MATERIAL_TEMPLATE_NOT_FOUND`

Validation should reject missing required fields and invalid quantity rules before saving material requirement templates.

## 13. Out Of Scope

This OpenSpec does not implement Java code, Controller/API, Service, Mapper, Entity, Flyway migration, Vue/TypeScript, admin-web pages, inventory, material master, stock reservation, stock deduction, inventory readiness, shortage checks, purchase, supplier, finance, work order code changes, dispatch code changes, process graph, photo upload, file upload, worker-uniapp, production-h5, screen-web, CRM, public pool, contribution, order creation, or order core mutation.
