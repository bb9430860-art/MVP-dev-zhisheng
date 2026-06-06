# Process Route Template Specification

## ADDED Requirements

### Requirement: Manage route templates

The system SHALL allow an administrator to create, view, update, soft delete, enable, and disable process route templates.

#### Scenario: Create a route template

- WHEN an administrator submits a route name and optional product type
- THEN the system creates a route template
- AND the route template is not deleted
- AND the route template can be edited later

#### Scenario: Disable a route template

- WHEN an administrator disables a route template
- THEN the template remains visible in management lists
- AND the template is excluded from template selection options

#### Scenario: Soft delete a route template

- WHEN an administrator deletes a route template
- THEN the system marks it as deleted
- AND the system does not hard delete the database row
- AND the template is hidden from normal management lists and selection options

### Requirement: Manage step templates

The system SHALL allow an administrator to create, view, update, and soft delete step templates under a route template.

#### Scenario: Create a step template

- WHEN an administrator adds a step to a route template
- THEN the step is saved under that route template
- AND the step includes a name, order, assigned role, photo requirement, remark requirement, and mobile execution flag

#### Scenario: Soft delete a step template

- WHEN an administrator deletes a step
- THEN the system marks the step as deleted
- AND the system does not hard delete the database row
- AND active step ordering can be reused without conflict

### Requirement: Sort step templates

The system SHALL support step sorting by move-up, move-down, and manual order save.

#### Scenario: Move a step up

- GIVEN a route template has at least two active steps
- WHEN an administrator moves a lower step up
- THEN the selected step order decreases by one position
- AND the affected neighboring step order increases by one position
- AND active steps remain in a contiguous order

#### Scenario: Save manual order

- GIVEN a route template has multiple active steps
- WHEN an administrator saves an explicit order list for all active steps
- THEN the system stores the submitted order
- AND normalizes active step order to contiguous values starting from 1

### Requirement: Configure role and execution requirements

The system SHALL support role and execution requirement fields on each step template.

#### Scenario: Configure step execution settings

- WHEN an administrator edits a step template
- THEN the administrator can set the assigned system role
- AND can set whether photo evidence is required
- AND can set whether remark text is required
- AND can set whether the step is mobile executable

#### Scenario: Avoid concrete employee binding

- WHEN a step template is saved
- THEN the template stores the assigned system role
- AND does not require a concrete employee assignment

### Requirement: Use flexible product type values

The system SHALL store applicable product type as a string or dictionary value and SHALL NOT hard-code product-type-specific business branching inside the template module.

#### Scenario: Create template for a product type

- WHEN an administrator creates a template with product type `SPIRIT_FORTRESS`
- THEN the system stores `SPIRIT_FORTRESS` as data
- AND does not require template-module code specific to spirit fortress

### Requirement: Provide template selection options

The system SHALL provide an API that returns enabled and non-deleted route templates for later production configuration.

#### Scenario: Query template options by product type

- GIVEN enabled templates exist for a product type
- WHEN a client queries template options with that product type
- THEN the system returns enabled and non-deleted templates whose `product_type` equals the requested value
- AND returns enabled and non-deleted general templates whose `product_type` is `GENERAL` or empty
- AND excludes disabled templates
- AND excludes deleted templates
- AND does not hard-code business branches for specific product types

### Requirement: Enable and disable step templates

The system SHALL allow an administrator to enable and disable step templates.

#### Scenario: Disable a step template

- WHEN an administrator disables a step template
- THEN the step remains visible in management lists
- AND the step is excluded from active step sorting
- AND the step is excluded from future production instance copying

#### Scenario: Enable a step template

- WHEN an administrator enables a disabled step template
- THEN the step can participate in active step sorting again
- AND the step can be included in future production instance copying
- AND active enabled step order remains contiguous

### Requirement: Preserve future instance isolation

The system SHALL document and preserve that future production instances copy template snapshots instead of executing directly against templates.

#### Scenario: Template edited after future dispatch

- GIVEN a future production route instance has been created by copying a route template
- WHEN the source template is edited later
- THEN the future production instance must not change because of the template edit

## Out of Scope

The system SHALL NOT implement production instance creation, order binding, production dispatch, worker tasks, photo check-in, inventory, attendance, dashboard aggregation, customer-line changes, order-line changes, or contribution-line changes in this change.
