# Production Step Check-in Photo Proposal

## Why

`production-step-execution` intentionally keeps `photo_required` and `remark_required` as metadata only. The MVP now needs a clear specification for completing production steps with evidence, so workers can provide photos and remarks while preserving the frozen production process structure.

This change defines production step check-in evidence rules without implementing file upload infrastructure.

## Goals

- Define how production step check-in evidence is recorded for `production_step_instance`.
- Define the future `production_step_checkin` business model.
- Define mandatory validation for `photo_required` and `remark_required`.
- Define how production check-ins bind uploaded files through the shared file system.
- Define MVP completion behavior with `complete-with-checkin` evidence.
- Define evidence query behavior for management review.
- Preserve frozen production route and step structure.
- Preserve customer-line and order-line ownership.

## Non-Goals

This change does not implement:

- backend business code
- database migrations
- Controller APIs
- frontend pages
- `worker-uniapp`
- `production-h5`
- `screen-web`
- file upload infrastructure
- `file_asset` table or schema
- `/api/files/upload`
- `/api/files/bind`
- image compression
- watermarking
- OCR
- face recognition
- geolocation
- approval workflows
- rework
- skipped steps
- execution-time step insertion
- inventory
- attendance
- dashboard
- CRM
- customer public pool
- contribution
- order core logic

## Scope

In scope for the OpenSpec:

- Check-in evidence flow for `production_step_instance`.
- Photo file id list and remark submission.
- MVP first version uses only `POST /api/production/step-instances/{stepInstanceId}/complete-with-checkin` for submitting evidence and completing a step.
- MVP first version does not implement independent `POST /api/production/step-instances/{stepInstanceId}/checkins` pre-submit evidence.
- Check-in time and operator capture.
- Draft fields for `production_step_checkin`.
- Strong validation rules:
  - `photo_required = true` requires at least one photo before completion.
  - `remark_required = true` requires non-blank remark before completion.
  - `fileIds` supports multiple photos but is limited to 3 ids in MVP.
  - duplicate `fileIds` are rejected.
- Shared file boundary:
  - production uses shared file APIs
  - production does not own `file_asset`
  - production does not implement upload or bind endpoints
- Recommended business binding:
  - `bizType = PRODUCTION_STEP_CHECKIN`
  - `bizId = production_step_checkin.id`

## Collaboration Boundaries

Production line owns:

- `production_step_instance` evidence requirements
- `production_step_checkin` business record design
- validation before completing production steps
- binding uploaded file ids to production check-ins through shared file APIs
- evidence query semantics for production management

Cursor/customer/file line owns:

- file upload infrastructure
- `file_asset` table and lifecycle
- `/api/files/upload`
- `/api/files/bind`
- common file viewing and deletion contracts

Production line must not:

- create its own file upload system
- create or alter `file_asset`
- bypass shared file APIs
- modify CRM, public pool, contribution, or order core logic
- update order fields except production fields already allowed by execution specs

If shared file APIs are not ready during implementation, production may use mock `fileId` values in dev/test only. It must not create an alternate upload system.

Dev/test mock `fileId` behavior is limited to:

- `fileId` must be a positive integer.
- `fileIds` must not contain duplicates.
- `fileIds` may contain at most 3 ids.
- mock validation does not check whether `file_asset` exists.
- mock validation does not implement upload.
- mock validation does not create `file_asset`.

The mock implementation must be marked:

```text
TODO: replace with shared file service contract
```

After the shared file service is available, production must validate file existence, tenant ownership, image file type, successful upload state, and bind result through the shared contract.

## Risks

- Shared file APIs may not be ready when production check-in implementation starts.
- Binding files after creating a check-in can create partial-failure cases if not handled transactionally or compensatingly.
- Enforcing `photo_required` and `remark_required` changes current completion behavior and must be tested carefully.
- Evidence records must not become a back door for changing frozen process structure.
- Management evidence display can grow into approval, OCR, or audit workflows; those are explicitly out of scope for MVP.

## Acceptance Criteria

- The OpenSpec change exists under `openspec/changes/production-step-checkin-photo`.
- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- The change states that it does not write code.
- The change states that it does not create migrations.
- The change states that production does not implement `file_asset`, upload, or bind.
- The change states that MVP first version only implements `complete-with-checkin` for evidence submission and completion.
- The change defines future strong validation for `photo_required` and `remark_required`.
- The change limits `fileIds` to at most 3 unique positive ids.
- The change defines shared file integration boundaries.
- The change preserves frozen production structure and order-line ownership.
- No completion claim may be made without verification evidence.
