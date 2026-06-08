# Work Order Material Generation Proposal

## Why

Process route templates can now define material requirements on process step templates, and production work orders can already store material demand in `production_work_order_material`. These two capabilities are still disconnected.

If work order material demand still depends on manual entry every time, the production team cannot standardize demand, and later inventory readiness or step-level shortage display will be unreliable. Work orders need a designed path to generate material demand from the selected process route template.

This change designs generation from `process_step_material_requirement_template` into `production_work_order_material` when a DRAFT work order is being prepared or edited. Generated lines must keep `related_step_template_id` and `usage_stage` so future shortage information can be shown on the relevant process node.

This change does not check inventory. Inventory matching, shortage calculation, and readiness status belong to a future `work-order-material-readiness` change.

## Goals

- Design generation from `process_step_material_requirement_template` into `production_work_order_material`.
- Support calculating `required_qty` from `workOrder.quantitySnapshot`.
- Support `base_qty_per_unit`, `fixed_qty`, and `loss_rate`.
- Preserve `material_id`, `material_code`, `material_name`, `spec`, and `unit`.
- Preserve `usage_stage`.
- Preserve `related_step_template_id`.
- Leave `related_step_instance_id` empty before dispatch.
- Support previewing generated material demand before writing it.
- Support applying generated material demand to DRAFT work orders.
- Preserve the existing manual work order material editing capability.
- Provide a data foundation for future inventory readiness and step-level shortage display.

## Non-Goals

- No inventory availability check.
- No inventory reservation.
- No inventory deduction.
- No inventory transaction.
- No shortage calculation.
- No readiness status.
- No step-level shortage display.
- No step-start blocking.
- No purchase.
- No supplier.
- No finance.
- No automatic production consume.
- No production dispatch redesign.
- No process graph `GROUP` / `TASK` / parallel / nested model.
- No CRM, public pool, contribution, or order core logic.
- No modification of order amount, quotation, customer fields, product spec, product quantity, unit price, subtotal, or order core status.
- No worker-uniapp, production-h5, or screen-web changes.

## Scope

This change only designs:

- Generating work order material demand from process template material demand.
- Previewing generated demand.
- Applying generated demand to DRAFT work orders.
- Boundaries with inventory readiness.
- Boundaries with production dispatch.
- Boundaries with future process graph design.

Future implementation may add backend APIs and admin-web interactions, but this OpenSpec change does not implement them.

## Collaboration Boundaries

- Codex production line owns the process, production, and inventory-side design.
- Cursor customer line remains owner of order core data and customer-line logic.
- Generation may read work order snapshots and selected process template data.
- Generation must not mutate order core fields or customer-line fields.
- Inventory stock and readiness remain separate future concerns.
