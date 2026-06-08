# Process Template Material Requirement Proposal

## Why

The current process template model defines route templates and step templates, but it does not define what materials each process step normally needs.

`production_work_order_material` can already record work order material demand, but it lacks a standardized source from process templates. If every work order depends on manual material entry, demand cannot be standardized, reviewed, reused, or reliably used by later inventory readiness and shortage-by-step features.

Material requirements should be defined during process route and step template design. When a production work order is created in a future change, the system can generate `production_work_order_material` from the selected process template and the `order_item` quantity. The generated demand then becomes the input for future inventory/material-readiness.

Material shortage must not block the whole work order. Shortage should be shown on the step that needs the material, and later step-start guard logic may warn or block only when the worker reaches that step.

This change is OpenSpec-only. It does not implement backend code, frontend code, migrations, inventory, work order generation, dispatch changes, or customer-line logic.

## Goals

- Design material requirement templates for process step templates.
- Support editing material demand for each step during process route template management.
- Support material name, specification, unit, base quantity, fixed quantity, loss rate, quantity rule, and remark.
- Allow future `material_id` linkage while keeping `material_id` nullable before inventory/material master is ready.
- Define how step material requirement templates map to `production_work_order_material`.
- Preserve `related_step_template_id` and `usage_stage` so later shortage/readiness can be displayed by step.
- Provide a data foundation for later inventory/material-readiness.
- Keep extension room for future route graph, nested process, and parallel process models.

## Non-Goals

This change does not implement:

- inventory module
- material master
- inventory deduction
- inventory reservation
- inventory readiness
- stock in/out
- inventory transaction
- shortage check
- shortage node blocking
- purchase
- supplier
- finance
- production work order code changes
- production dispatch code changes
- non-linear, parallel, or nested process graph
- photo upload
- file upload
- worker-uniapp
- production-h5
- screen-web
- CRM
- customer public pool
- contribution value logic
- order creation
- order amount, quotation, customer, spec, quantity, or order core status changes

## Scope

OpenSpec scope:

```text
openspec/changes/process-template-material-requirement/
```

Allowed files in this change:

```text
proposal.md
design.md
spec.md
tasks.md
```

This change designs:

- material requirement templates under process route templates
- material requirement templates under process step templates
- future admin-web editing interaction
- the boundary for later generation of `production_work_order_material` from templates

Future implementation may add backend process-template APIs, persistence, and admin-web editing controls, but that implementation is not part of this OpenSpec-only change.

## Collaboration Boundaries

Codex production line owns:

- process route templates
- process step templates
- process template material requirement design
- production work order material demand
- future inventory/material-readiness integration

Cursor customer line does not participate in this change.

This change does not read or mutate order core fields. Future work order material generation may read `order_item.quantity` as production-side context, but it must not modify order amount, quotation, customer fields, product specification, product quantity, or order core status.

This change does not implement inventory. It only defines the standard demand source that later inventory/material-readiness can consume.

## Risks

- If material requirements remain only manual on work orders, standard process knowledge will not accumulate in templates.
- If material demand is not tied to step templates, later shortage display cannot reliably point to the affected process node.
- If inventory logic is mixed into this change, template editing will become coupled to stock availability before inventory rules are approved.
- If material demand is stored only as free text, later readiness and shortage calculations will be hard to automate.
- If future route graph work is ignored, material demand may need rework when GROUP/TASK and parallel process models are introduced.
