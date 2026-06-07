# Production Step Check-in Photo Specification

## ADDED Requirements

### Requirement: Record production step check-in evidence

The system SHALL support recording check-in evidence for a production step instance.

#### Scenario: Complete-with-checkin creates evidence for an in-progress step

- GIVEN a `production_step_instance` exists
- AND the step belongs to a frozen production route instance
- AND the step status is `IN_PROGRESS`
- AND the current user can complete the step
- WHEN the user calls `POST /api/production/step-instances/{stepInstanceId}/complete-with-checkin`
- THEN the system records a `production_step_checkin`
- AND records the step instance id, route instance id, order item id, operator id, remark, file ids, and check-in time
- AND completes the production step if all execution and evidence rules pass
- AND does not modify frozen route or step structure

#### Scenario: Independent check-in pre-submit is not implemented in MVP

- WHEN MVP first version is implemented
- THEN the system does not implement `POST /api/production/step-instances/{stepInstanceId}/checkins`
- AND evidence is submitted through `complete-with-checkin`
- AND `GET /api/production/step-instances/{stepInstanceId}/checkins` may be scheduled separately for evidence viewing

#### Scenario: Query check-in evidence for a step

- GIVEN a production step has check-in evidence
- WHEN a management client queries `GET /api/production/step-instances/{stepInstanceId}/checkins`
- THEN the system returns check-in records for that step
- AND includes operator, check-in time, remark, and file id references
- AND may include file summaries from the shared file service

### Requirement: Enforce photo requirement before completion

The system SHALL prevent completing a step without photo evidence when the step requires photos.

#### Scenario: Photo required and file ids provided

- GIVEN a production step has `photo_required = true`
- AND the step status is `IN_PROGRESS`
- AND the current user can complete the step
- WHEN the user completes the step with 1 to 3 valid unique photo file ids
- THEN the system accepts the photo evidence
- AND may complete the step if all other completion rules pass

#### Scenario: Photo required and no file ids provided

- GIVEN a production step has `photo_required = true`
- AND the step status is `IN_PROGRESS`
- WHEN the user attempts to complete the step without any valid photo file id
- THEN the system returns `PHOTO_REQUIRED`
- AND the step remains `IN_PROGRESS`
- AND no misleading completion progress is written

#### Scenario: File ids exceed MVP limit

- GIVEN a complete-with-checkin request includes more than 3 file ids
- WHEN production validates the request
- THEN the system rejects the request
- AND the step remains `IN_PROGRESS`

#### Scenario: File ids contain duplicates

- GIVEN a complete-with-checkin request includes duplicate file ids
- WHEN production validates the request
- THEN the system rejects the request
- AND the step remains `IN_PROGRESS`

#### Scenario: Photo not required

- GIVEN a production step has `photo_required = false`
- WHEN the user completes the step with no photo file id
- THEN the photo validation passes
- AND other completion rules still apply

- WHEN the user provides optional file ids
- THEN the same max 3 and no-duplicate rules apply

### Requirement: Enforce remark requirement before completion

The system SHALL prevent completing a step without remark evidence when the step requires remarks.

#### Scenario: Remark required and non-blank remark provided

- GIVEN a production step has `remark_required = true`
- AND the step status is `IN_PROGRESS`
- WHEN the user completes the step with a non-blank remark
- THEN the remark validation passes

#### Scenario: Remark required and blank remark provided

- GIVEN a production step has `remark_required = true`
- WHEN the user attempts to complete the step with null, empty, or whitespace-only remark
- THEN the system returns `REMARK_REQUIRED`
- AND the step remains `IN_PROGRESS`

#### Scenario: Remark not required

- GIVEN a production step has `remark_required = false`
- WHEN the user completes the step without remark
- THEN the remark validation passes
- AND other completion rules still apply

### Requirement: Bind uploaded files to production check-ins

The system SHALL bind uploaded files to production check-in evidence through the shared file service.

#### Scenario: Bind uploaded files to check-in

- GIVEN shared file upload has returned file ids
- AND production has created a `production_step_checkin`
- WHEN production binds those files
- THEN production calls the shared file bind contract
- AND uses `bizType = PRODUCTION_STEP_CHECKIN`
- AND uses `bizId = production_step_checkin.id`
- AND does not modify `file_asset` directly

#### Scenario: File asset is not ready

- GIVEN a complete-with-checkin request includes file ids
- AND the shared file service reports that a file is missing, deleted, not ready, or unavailable for the tenant
- WHEN production validates the evidence
- THEN the system returns `FILE_ASSET_NOT_READY`
- AND the step remains `IN_PROGRESS`

#### Scenario: File bind fails

- GIVEN production has created or is creating check-in evidence
- WHEN shared file binding fails
- THEN the system returns `FILE_BIND_FAILED`
- AND the implementation must not leave step completion and evidence binding inconsistent

### Requirement: Complete step with check-in evidence

The system SHALL support completing a production step with check-in evidence in one MVP operation.

#### Scenario: Complete with required evidence

- GIVEN a production step is `IN_PROGRESS`
- AND the step belongs to a frozen route
- AND the current user is allowed to complete it
- AND all required photo and remark evidence is present
- WHEN the user calls `POST /api/production/step-instances/{stepInstanceId}/complete-with-checkin`
- THEN the system creates check-in evidence
- AND binds provided file ids through the shared file service
- AND completes the step using existing step execution rules
- AND records `completed_at` and `completed_by`
- AND recalculates production progress

#### Scenario: Complete-with-checkin preserves serial and ownership rules

- GIVEN existing `production-step-execution` rules apply
- WHEN complete-with-checkin is requested
- THEN the system enforces `STEP_NOT_IN_PROGRESS` for non-in-progress steps
- AND enforces `STEP_NOT_ASSIGNED_TO_CURRENT_USER` for users who cannot complete the step
- AND does not bypass role-based `started_by` completion restrictions

#### Scenario: Plain complete is constrained by evidence flags

- GIVEN a production step has `photo_required = true`
- OR has `remark_required = true`
- WHEN a user tries to complete the step without evidence
- THEN the system must reject completion with `PHOTO_REQUIRED` or `REMARK_REQUIRED`
- AND the implementation should route completion through complete-with-checkin or equivalent evidence validation

### Requirement: Preserve frozen production structure

The system SHALL preserve frozen production route and step structure while recording evidence.

#### Scenario: Check-in does not change structure

- WHEN a check-in is created or a step is completed with check-in evidence
- THEN the system may create evidence records and update execution status fields
- AND must not update step order, step name, assigned role, assigned user id, photo required flag, remark required flag, mobile enabled flag, source template id, or snapshot fields

#### Scenario: Structural edit attempt is rejected

- GIVEN a production route instance is frozen
- WHEN any check-in or completion request attempts to alter route or step structure
- THEN the system returns `PRODUCTION_ROUTE_STRUCTURE_FROZEN`
- AND the frozen structure remains unchanged

### Requirement: Keep file upload infrastructure out of this change

The system SHALL NOT implement shared file upload infrastructure in this change.

#### Scenario: Production consumes shared file APIs only

- WHEN production needs photo evidence
- THEN production uses file ids produced by shared file upload infrastructure
- AND calls shared file bind behavior
- AND does not create `file_asset`
- AND does not implement `/api/files/upload`
- AND does not implement `/api/files/bind`

#### Scenario: Shared file API unavailable during implementation

- GIVEN shared file upload and bind APIs are not ready
- WHEN production check-in implementation needs a development path
- THEN it may use dev/test mock file ids with minimum validation only
- AND accepts only positive integer file ids
- AND rejects duplicate file ids
- AND rejects more than 3 file ids
- AND does not validate whether `file_asset` really exists
- AND does not implement file upload
- AND does not create `file_asset`
- AND must mark the adapter with `TODO: replace with shared file service contract`
- AND must not create an alternate production upload system

#### Scenario: Shared file service is available

- GIVEN the shared file service contract is available
- WHEN production validates submitted file ids
- THEN production verifies file id existence through the shared service
- AND verifies the file belongs to the current tenant
- AND verifies the file type is image
- AND verifies the file upload state is successful
- AND treats bind failure as `FILE_BIND_FAILED`

### Requirement: Preserve order-line ownership

The system SHALL preserve customer-line and order-line ownership while completing steps with evidence.

#### Scenario: Production writes only production fields

- WHEN a step is completed with check-in evidence
- THEN production may write only production status and progress fields already allowed by step execution
- AND must not update order core status, order amount, customer data, quotation data, product specification, product quantity, custom order fields, contribution account, or contribution transaction records

#### Scenario: Check-in does not create order or customer logic

- WHEN production records check-in evidence
- THEN it does not implement order creation, customer public pool, CRM, or contribution logic

## DoD

- The OpenSpec change exists under `openspec/changes/production-step-checkin-photo`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- This change explicitly states that it does not write business code.
- This change explicitly states that it does not create database migrations.
- This change explicitly does not implement Controller APIs or pages.
- This change explicitly does not implement `file_asset`, `/api/files/upload`, or `/api/files/bind`.
- This change explicitly forbids `worker-uniapp`, `production-h5`, and `screen-web`.
- This change explicitly forbids inventory, attendance, dashboard, CRM, public pool, contribution, and order core logic.
- Future strong validation is defined for `photo_required` and `remark_required`.
- `photo_required = true` requires at least one valid photo file id before completion.
- `fileIds` supports multiple ids but MVP allows at most 3 unique positive ids.
- `remark_required = true` requires non-blank remark before completion.
- MVP first version uses `POST /api/production/step-instances/{stepInstanceId}/complete-with-checkin` for evidence submission and completion.
- MVP first version does not implement independent `POST /api/production/step-instances/{stepInstanceId}/checkins` evidence pre-submit.
- Production uses shared file interfaces and does not create a separate upload system.
- Recommended file binding uses `bizType = PRODUCTION_STEP_CHECKIN` and `bizId = production_step_checkin.id`.
- Check-in evidence must not modify frozen production structure.
- Production may update only order item production fields already allowed by step execution.
- No completion claim may be made without verification evidence.

## Out of Scope

The system SHALL NOT implement backend business code, migrations, Controller APIs, admin-web pages, `worker-uniapp`, `production-h5`, `screen-web`, file upload infrastructure, `file_asset`, `/api/files/upload`, `/api/files/bind`, image compression, watermarking, OCR, face recognition, geolocation, complex approval, rework, skipped steps, execution-time step insertion, inventory, attendance, dashboard, CRM, public pool, contribution value, order creation, order core field modification, or customer field modification in this change.
