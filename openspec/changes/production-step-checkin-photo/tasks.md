# Production Step Check-in Photo Tasks

## Task 1: Confirm Scope And File Boundary

### Steps

1. Read `README.md`.
2. Read `docs/codex-production-line.md`.
3. Read `docs/cursor-customer-line.md`.
4. Read `openspec/changes/production-step-execution/design.md`.
5. Confirm this change is OpenSpec-only before implementation.
6. Confirm no backend, frontend, migration, worker app, or file infrastructure files are modified in this change.

### DoD

- Scope is documented in `proposal.md`.
- Non-goals explicitly exclude code, migrations, `file_asset`, upload, bind, worker apps, inventory, attendance, dashboard, CRM, public pool, contribution, and order core logic.
- The change path contains only OpenSpec documents.

## Task 2: Design `production_step_checkin` Model

### Steps

1. Define the purpose of `production_step_checkin`.
2. Draft fields for step id, route id, order item id, operator, remark, file ids, check-in type, and check-in time.
3. Define MVP check-in type usage.
4. Define suggested indexes.
5. State that no migration is created in this OpenSpec change.

### DoD

- `design.md` includes a `production_step_checkin` table draft.
- The draft does not create a migration.
- The draft does not include file binary storage.
- The model is clearly execution evidence, not frozen process structure.

## Task 3: Design File Binding Contract

### Steps

1. Define that Cursor/shared file infrastructure owns upload and binding implementation.
2. Define production's dependency on shared file ids.
3. Define binding with `bizType = PRODUCTION_STEP_CHECKIN`.
4. Define binding with `bizId = production_step_checkin.id`.
5. Define behavior if shared file APIs are not ready.
6. Define dev/test mock file id minimum validation.
7. Define formal shared file validation after integration.

### DoD

- `design.md` states production only calls shared file APIs.
- `spec.md` includes file binding requirements and scenarios.
- The change explicitly forbids production from creating `file_asset` or implementing upload/bind endpoints.
- Dev/test mock file id fallback is allowed only as a temporary implementation strategy.
- Dev/test mock file id validation only accepts positive integer ids, rejects duplicates, and rejects more than 3 ids.
- Dev/test mock behavior must be marked `TODO: replace with shared file service contract`.
- Formal shared file integration must validate file existence, tenant ownership, image type, successful upload state, and bind result.

## Task 4: Design Photo And Remark Validation

### Steps

1. Define `photo_required = true` validation.
2. Define `remark_required = true` validation.
3. Define optional evidence behavior when the flags are false.
4. Define `PHOTO_REQUIRED`.
5. Define `REMARK_REQUIRED`.
6. Define that `fileIds` may contain at most 3 ids.
7. Define that `fileIds` must not contain duplicates.
8. Define that `remark_required = true` uses trimmed non-empty text.

### DoD

- `design.md` documents strong photo and remark validation rules.
- `spec.md` includes scenarios for required and optional photo evidence.
- `spec.md` includes scenarios for required and optional remark evidence.
- Completion without required evidence is rejected.
- `fileIds` limits are documented and testable.
- Duplicate `fileIds` are rejected.

## Task 5: Design Complete-With-Checkin Behavior

### Steps

1. Choose MVP recommended completion flow.
2. Define complete-with-checkin request shape.
3. Define check-in creation before completion.
4. Define file binding before or within completion.
5. Define that existing execution completion rules still apply.
6. Define partial-failure expectations for file bind and step completion.
7. Define that MVP first version does not implement independent `POST checkins` evidence pre-submit.
8. Define that `GET checkins` is evidence-view design and may be scheduled separately.

### DoD

- `design.md` recommends `complete-with-checkin` as the MVP path.
- `design.md` states that MVP first version implements only `complete-with-checkin` for evidence submission and completion.
- `spec.md` states that independent `POST checkins` is not part of MVP first version.
- `spec.md` requires existing `production-step-execution` ownership and status rules.
- The design does not bypass `STEP_NOT_IN_PROGRESS`.
- The design does not bypass `STEP_NOT_ASSIGNED_TO_CURRENT_USER`.
- The design does not bypass frozen structure rules.

## Task 6: Design Evidence Query Behavior

### Steps

1. Define query check-ins by step instance id.
2. Define returned evidence fields.
3. Define optional shared file summary enrichment.
4. Exclude approval, OCR, image editing, and other advanced evidence workflows.

### DoD

- `design.md` includes evidence query behavior.
- `spec.md` includes a scenario for querying check-in evidence.
- The query behavior is management review only.
- No complex approval scope is introduced.

## Task 7: Design Backend Tests

### Steps

1. Define tests for creating check-in evidence.
2. Define tests for `PHOTO_REQUIRED`.
3. Define tests for `REMARK_REQUIRED`.
4. Define tests for max 3 `fileIds`.
5. Define tests that duplicate `fileIds` are rejected.
6. Define tests that dev/test mock file ids must be positive integers.
7. Define tests for file asset not ready and file bind failure after shared file service integration.
8. Define tests for complete-with-checkin success.
9. Define tests that independent `POST checkins` is not implemented in MVP first version.
10. Define tests that frozen structure fields remain unchanged.
11. Define tests that order core fields are not modified.

### DoD

- `tasks.md` lists backend behavior that future implementation must test.
- Tests cover success, validation failures, file failures, frozen structure protection, and order-line boundary.
- Tests explicitly avoid upload infrastructure implementation.
- Tests cover `fileIds` max count, uniqueness, and dev/test positive-integer mock validation.
- Tests confirm `complete-with-checkin` is the MVP completion entry point.

## Task 8: Design Future Admin-Web / Worker UI Boundary

### Steps

1. Define future UI needs for viewing and submitting evidence.
2. Define that this OpenSpec does not implement UI.
3. Define that `admin-web` may later view evidence.
4. Define that worker UI may later upload photos through shared file APIs.
5. Exclude `worker-uniapp`, `production-h5`, and `screen-web` in this change.

### DoD

- UI responsibilities are documented as future implementation scope.
- This change does not create pages.
- This change does not create upload widgets.
- This change does not implement worker mobile apps.

## Task 9: Define Verification Checklist

### Steps

1. Verify the OpenSpec change directory exists.
2. Verify all four OpenSpec files exist.
3. Verify the files mention no-code and no-migration scope.
4. Verify upload and file binding are explicitly external shared contracts.
5. Verify forbidden modules are not touched.
6. Verify hidden or bidirectional Unicode controls are absent.

### DoD

- `proposal.md`, `design.md`, `spec.md`, and `tasks.md` exist.
- `git status` shows only files under `openspec/changes/production-step-checkin-photo/`.
- Hidden or bidirectional Unicode control scan returns zero.
- No completion claim is made without verification evidence.

## Task 10: Review Out-Of-Scope Protection

### Steps

1. Review the change for accidental file upload implementation.
2. Review the change for accidental `file_asset` ownership.
3. Review the change for accidental worker app scope.
4. Review the change for accidental inventory, attendance, dashboard, CRM, public pool, contribution, or order core scope.
5. Review the change for frozen structure violations.

### DoD

- Out-of-scope items are listed in all relevant documents.
- The change does not define rework, skip, execution-time step insertion, or execution-time reorder.
- The change preserves order-line ownership.
- The change preserves frozen production structure.

## Future Implementation Verification Checklist

Future implementation must provide evidence for:

- backend tests pass
- frontend tests pass if frontend is touched
- build passes if frontend is touched
- check-in creation works
- `photo_required` blocks completion without photo
- `remark_required` blocks completion without remark
- `fileIds` with more than 3 ids are rejected
- duplicate `fileIds` are rejected
- dev/test mock file ids accept only positive integers
- file bind failures do not complete steps
- complete-with-checkin completes steps only when execution rules pass
- independent `POST checkins` is not required for MVP first version
- frozen structure fields are unchanged
- order core fields are unchanged
- no production-owned upload system exists
- no `file_asset` table or endpoint is created by production
