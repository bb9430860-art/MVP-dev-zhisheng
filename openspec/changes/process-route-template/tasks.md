# Process Route Template Tasks

## Task 1: Confirm skeleton and scope guard

Steps:

- Confirm the repository has only the minimal process-route-template skeleton needed for this change.
- Confirm backend scope is limited to `zhisheng-common`, `zhisheng-process`, and `zhisheng-app`.
- Confirm frontend scope is limited to `frontend/admin-web/src/modules/process` plus reserved app directories.
- Confirm no customer-line, order-line, contribution-line, production-instance, inventory, attendance, or dashboard files are created for this change.

DoD:

- The root contains `openspec/`, `backend/`, `frontend/`, `database/`, `scripts/`, and `docs/`.
- `backend/pom.xml` declares only the minimal Maven modules needed now.
- `frontend/` contains reserved app directories without customer-line implementation files.
- No business implementation code exists yet.

## Task 2: Define database migration for route and step templates

Steps:

- Create a Flyway migration for `process_route_template`.
- Create a Flyway migration for `process_step_template`.
- Use soft deletion columns for both tables.
- Add `deleted` as `TINYINT` with `0` or `1` values.
- Add `delete_marker` as `BIGINT`, using `0` for active rows and the current record id or timestamp for soft-deleted rows.
- Store `product_type` as a string or dictionary value.
- Store `assigned_role` as a system role string.
- Do not add employee assignment fields as required template data.
- Design active step order uniqueness as `tenant_id + route_template_id + step_order + delete_marker`.
- Do not use `tenant_id + route_template_id + step_order + deleted` as the final unique constraint.

DoD:

- Migration creates only process template tables.
- Migration does not create production instance, check-in, inventory, attendance, dashboard, customer, order, or contribution tables.
- Route templates support enable, disable, and soft delete.
- Step templates support enable, soft delete, photo requirement, remark requirement, mobile execution flag, and system role.
- Active step order uniqueness uses the `delete_marker` strategy so soft-deleted rows do not block order reuse.

## Task 3: Implement backend route template API with tests

Steps:

- Write failing service/controller tests for creating, updating, listing, enabling, disabling, and soft deleting route templates.
- Implement the minimum route template entity, mapper, service, and controller code needed to pass the tests.
- Ensure all API responses use the shared response envelope.
- Ensure disabled and deleted records are handled differently.

DoD:

- An administrator can create a route template.
- An administrator can update route name, product type, description, and enabled state.
- Soft-deleted templates are hidden from normal lists.
- Disabled templates remain in management lists.
- Route template APIs do not call customer-line, order-line, contribution-line, production-instance, inventory, attendance, or dashboard modules.

## Task 4: Implement backend step template API with tests

Steps:

- Write failing tests for adding, updating, listing, and soft deleting step templates.
- Write failing tests for enabling and disabling step templates.
- Implement the minimum step template entity, mapper, service, and controller code needed to pass the tests.
- Validate that steps belong to the requested route template.
- Normalize active step order after create and delete operations.
- Exclude disabled steps from active ordering and future copy candidates.

DoD:

- An administrator can add multiple steps under one route template.
- Each step stores assigned role, photo requirement, remark requirement, and mobile execution flag.
- Step deletion is soft deletion.
- Disabled steps remain visible in management lists.
- Disabled steps do not participate in active step ordering.
- Active enabled step order remains contiguous after create, delete, enable, and disable.
- No concrete employee binding is required at template stage.

## Task 5: Implement step ordering with tests

Steps:

- Write failing tests for move-up behavior.
- Write failing tests for move-down behavior.
- Write failing tests for manual order save.
- Implement reorder logic that only affects active steps in the same route template.
- Reject reorder requests containing missing, duplicated, deleted, or foreign step IDs.

DoD:

- Move up swaps the selected step with the previous active step.
- Move down swaps the selected step with the next active step.
- Manual order save persists the submitted order for all active steps.
- Active steps are normalized to `1..N` after each reorder.
- Soft-deleted steps do not cause ordering conflicts.

## Task 6: Implement template option API with tests

Steps:

- Write failing tests for querying enabled template options.
- Implement option query filtered by enabled and non-deleted templates.
- Support optional `productType` filtering.
- When `productType` is present, include exact product type templates plus general templates where `product_type` is `GENERAL` or empty.
- Keep product type matching data-driven; do not hard-code signage business logic.

DoD:

- Enabled and non-deleted templates appear in options.
- Disabled templates do not appear in options.
- Deleted templates do not appear in options.
- Product type filtering works by stored string value and includes `GENERAL` or empty product type templates as general matches.
- The API returns only fields needed for selection, such as id, route name, product type, and step count.

## Task 7: Implement admin-web process template pages

Steps:

- Create the route template list page in `frontend/admin-web/src/modules/process`.
- Create the route template edit page.
- Create the step template table and form inside the process module.
- Add controls for create, edit, enable, disable, soft delete, move up, move down, and manual order save.
- Do not add order detail links or production dispatch UI.

DoD:

- Admin users can manage route templates from the process module.
- Admin users can manage step templates under a route template.
- The first version supports buttons or numeric order save; drag-and-drop is not required.
- The frontend does not modify customer, order, contribution, production-instance, inventory, attendance, or dashboard modules.

## Task 8: Add demo template data

Steps:

- Add seed data for common signage process route templates only after schema exists.
- Include examples such as spirit fortress, floor sign, illuminated letters, and wayfinding sign.
- Keep demo data in the production-side migration or seed area.

DoD:

- Demo data contains route templates and ordered step templates.
- Demo data does not create orders, production instances, worker tasks, inventory, attendance, or contribution records.
- Demo data can be removed or reset without affecting customer-line data.

## Task 9: Verify and review

Steps:

- Run backend tests for the process module.
- Run frontend type check or build for admin-web once the frontend skeleton exists.
- Review the OpenSpec requirements against implemented behavior.
- Review git diff to confirm no customer-line or out-of-scope modules changed.

DoD:

- Verification commands complete with recorded output.
- Every requirement in `specs/process-route-template/spec.md` maps to implemented behavior.
- No out-of-scope tables, APIs, pages, or customer-line modifications are present.
- Any known gaps are documented before review.
