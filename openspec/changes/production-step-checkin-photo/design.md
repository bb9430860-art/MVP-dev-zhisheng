# Production Step Check-in Photo Design

## Overview

This change defines production step check-in evidence for frozen `production_step_instance` records.

It is OpenSpec-only. It must not create code, migrations, Controller APIs, frontend pages, worker apps, upload flows, `file_asset` tables, or database tables in this change.

This change closes the gap left by `production-step-execution`:

```text
production-step-execution:
  status transition and progress only

production-step-checkin-photo:
  evidence, photo requirement, remark requirement, and file binding rules
```

## Check-in Flow

Recommended MVP flow:

1. A production step instance is already `IN_PROGRESS`.
2. The user opens step detail.
3. If photos are needed, the client uploads files through the shared file API.
4. The shared file API returns file ids.
5. The client submits complete-with-checkin to production with:
   - file ids
   - remark
   - optional client check-in time
6. Production validates the step and current user.
7. Production validates photo and remark requirements.
8. Production creates one `production_step_checkin` record.
9. Production binds uploaded file ids to the check-in through shared file bind API.
10. Production completes the step by delegating to or extending the existing step execution completion behavior.
11. Production recalculates route and order item production progress.
12. Management can query check-in records for the step.

Recommended MVP API style:

```text
complete-with-checkin creates the check-in and completes the step in one production operation.
```

Reasoning:

- It keeps the worker action simple.
- It gives the backend one validation point for `photo_required` and `remark_required`.
- It avoids a separate partial check-in lifecycle in the MVP.
- It still allows the client to upload files first through shared file infrastructure.

MVP first version rule:

```text
Only POST /api/production/step-instances/{stepInstanceId}/complete-with-checkin is implemented.
```

The production backend handles the full operation:

1. validate that the step can be completed
2. validate `photo_required` and `remark_required`
3. create `production_step_checkin`
4. call the shared file bind contract for `fileIds`
5. complete the step
6. update route and order item progress

MVP first version does not implement:

```text
POST /api/production/step-instances/{stepInstanceId}/checkins
```

as an independent pre-submit evidence API.

`GET /api/production/step-instances/{stepInstanceId}/checkins` remains a future evidence-view API design. It may be scheduled separately during implementation.

## Data Flow

```text
production_step_instance
-> shared /api/files/upload
-> file ids
-> production_step_checkin
-> shared /api/files/bind
-> complete production step
-> route progress update
-> order_item production progress write-back
```

Production execution state remains sourced from:

```text
production_route_instance
production_step_instance
production_step_checkin
```

Production must not use process templates as execution state.

## Relation To `production-step-execution`

`production-step-execution` already defines:

- task query
- `PENDING -> IN_PROGRESS`
- `IN_PROGRESS -> COMPLETED`
- serial execution
- `started_by`
- `completed_by`
- route progress
- order item production progress write-back

This change extends completion rules only.

The plain complete operation from `production-step-execution` must be constrained after this change:

- If `photo_required = false` and `remark_required = false`, plain complete may remain valid.
- If `photo_required = true`, completion must require at least one photo through check-in evidence.
- If `remark_required = true`, completion must require non-blank remark through check-in evidence.
- If either requirement is true, complete-with-checkin is the preferred completion path.

This change must not alter the step execution state machine except by adding evidence validation before completion.

## Photo And Remark Validation Rules

### Photo Required

Rule:

```text
If production_step_instance.photo_required = true,
the step cannot complete unless at least one valid photo file id is provided
or already bound to a valid production_step_checkin for this completion.
```

Invalid cases:

- `photo_required = true` and `fileIds` is empty
- `photo_required = true` and all file ids are invalid
- `fileIds` contains more than 3 ids
- `fileIds` contains duplicate ids
- `fileIds` contains a non-positive id
- `photo_required = true` and file API reports file asset not ready
- `photo_required = true` and file binding fails

Suggested error:

```text
PHOTO_REQUIRED
```

### Remark Required

Rule:

```text
If production_step_instance.remark_required = true,
the step cannot complete unless remark is non-blank after trimming whitespace.
```

Invalid cases:

- `remark_required = true` and remark is null
- `remark_required = true` and remark is an empty string
- `remark_required = true` and remark is whitespace only

Suggested error:

```text
REMARK_REQUIRED
```

### Not Required

If the flags are false:

- photos are optional
- remark is optional
- the user may still submit evidence voluntarily

### File Id List Rules

MVP first version supports multiple photo ids with strict limits:

```text
0 <= fileIds.length <= 3
```

Rules:

- if `photo_required = true`, `fileIds.length >= 1`
- if `photo_required = false`, `fileIds` may be empty
- if `fileIds` is provided, it may contain at most 3 ids
- `fileIds` must not contain duplicates
- each `fileId` must be a positive integer
- `remark_required = true` requires `trim(remark)` to be non-empty

## Shared File Boundary

File upload infrastructure is not owned by this change.

Production line may call:

```http
POST /api/files/upload
POST /api/files/bind
```

Production line must not:

- implement `/api/files/upload`
- implement `/api/files/bind`
- create `file_asset`
- modify `file_asset`
- store raw binary files inside production tables
- create a separate production-only upload system

Recommended file binding:

```text
bizType = PRODUCTION_STEP_CHECKIN
bizId = production_step_checkin.id
fileIds = uploaded file ids
```

If shared file APIs are unavailable during implementation:

- dev/test may use mock `fileId` values
- dev/test mock validation only checks positive integer ids
- dev/test mock validation rejects duplicate ids
- dev/test mock validation rejects more than 3 ids
- dev/test mock validation does not check whether `file_asset` really exists
- dev/test mock validation does not implement file upload
- dev/test mock validation does not create `file_asset`
- implementation must mark the adapter with a TODO for shared file API replacement
- implementation must not create an alternate file infrastructure

Required TODO marker:

```text
TODO: replace with shared file service contract
```

After the shared file service is connected, production must validate:

- file id exists
- file belongs to the current tenant
- file type is image
- file upload state is successful
- shared bind operation succeeds

## API Shape Draft

All APIs use the shared response envelope:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Query check-ins for a step:

```http
GET /api/production/step-instances/{stepInstanceId}/checkins
```

Response data draft:

```json
{
  "records": [
    {
      "id": 7001,
      "stepInstanceId": 9001,
      "routeInstanceId": 3001,
      "checkinType": "COMPLETE",
      "operatorId": 201,
      "remark": "surface checked",
      "fileIds": [801, 802],
      "checkedInAt": "2026-06-07T10:00:00",
      "createdAt": "2026-06-07T10:00:01"
    }
  ]
}
```

Create check-in evidence:

```http
POST /api/production/step-instances/{stepInstanceId}/checkins
```

Request draft:

```json
{
  "checkinType": "COMPLETE",
  "fileIds": [801, 802],
  "remark": "surface checked",
  "checkedInAt": "2026-06-07T10:00:00"
}
```

MVP first version does not implement this independent `POST checkins` endpoint. It is retained only as a possible later design if evidence pre-submission is needed.

Complete step with check-in:

```http
POST /api/production/step-instances/{stepInstanceId}/complete-with-checkin
```

Request draft:

```json
{
  "fileIds": [801, 802],
  "remark": "surface checked",
  "checkedInAt": "2026-06-07T10:00:00"
}
```

Response data draft:

```json
{
  "stepInstanceId": 9001,
  "routeInstanceId": 3001,
  "status": "COMPLETED",
  "checkinId": 7001,
  "fileIds": [801, 802],
  "productionProgress": 40
}
```

These APIs are draft only. This OpenSpec change does not implement them.

Implementation priority:

```text
1. POST complete-with-checkin
2. GET checkins, if evidence viewing is scheduled
3. POST checkins, only in a later change if pre-submitted evidence is needed
```

## Data Table Draft: `production_step_checkin`

No migration is created in this OpenSpec change. This is a schema draft only.

Suggested fields:

```text
id
tenant_id
route_instance_id
step_instance_id
order_id
order_item_id
checkin_type
operator_id
remark
file_ids_json
checked_in_at
created_by
created_at
updated_by
updated_at
deleted
delete_marker
```

Suggested check-in types:

```text
COMPLETE
PHOTO
NOTE
```

MVP may use only:

```text
COMPLETE
```

Suggested indexes:

```text
tenant_id + step_instance_id + deleted
tenant_id + route_instance_id + deleted
tenant_id + operator_id + created_at
```

File ids may be stored redundantly as JSON for display convenience, but the authoritative file metadata and file lifecycle remain in shared file infrastructure.

## Frozen Structure Protection

Check-ins are execution evidence. They must not modify frozen process structure.

Allowed updates:

- create check-in evidence
- bind file ids to check-in
- complete step execution fields
- update progress through existing execution rules

Forbidden updates:

- step order
- step name
- assigned role
- assigned user id
- photo required flag
- remark required flag
- mobile enabled flag
- source template id
- snapshot fields
- route structure

Structural edit attempts must return:

```text
PRODUCTION_ROUTE_STRUCTURE_FROZEN
```

## Order-Line Ownership

Check-in and completion may update only production-owned order item fields already allowed by step execution:

```text
order_item.production_status
order_item.production_progress
```

Production must not update:

```text
order core status
order amount
customer data
quotation data
product specification
product quantity
custom order fields
contribution account
contribution transaction
```

## Evidence Query

Management can query evidence for a production step.

The query should show:

- check-in time
- operator
- remark
- file ids or file summaries returned from shared file service
- step and route references

The query must not implement:

- approval workflow
- image editing
- OCR
- face recognition
- geolocation audit
- contribution reward calculation

## Exception Scenarios

### `STEP_INSTANCE_NOT_FOUND`

Return when the step instance does not exist or is deleted.

### `STEP_NOT_IN_PROGRESS`

Return when complete-with-checkin is requested for a step that is not `IN_PROGRESS`.

### `PHOTO_REQUIRED`

Return when the step requires photo evidence but no valid photo file id is available.

### `REMARK_REQUIRED`

Return when the step requires remark evidence but no non-blank remark is available.

### `FILE_ASSET_NOT_READY`

Return when a provided file id is missing, not uploaded, deleted, not owned by the current tenant, or not ready for binding.

### `FILE_BIND_FAILED`

Return when the shared file bind operation fails.

### `STEP_NOT_ASSIGNED_TO_CURRENT_USER`

Return when the current user cannot execute or complete the step under existing execution ownership rules.

### `PRODUCTION_ROUTE_STRUCTURE_FROZEN`

Return when any operation attempts to change frozen production structure instead of recording execution evidence.

## Out of Scope

This change does not implement:

- backend business code
- database migration
- Controller APIs
- frontend pages
- `worker-uniapp`
- `production-h5`
- `screen-web`
- shared file upload infrastructure
- `file_asset`
- `/api/files/upload`
- `/api/files/bind`
- image compression
- image watermark
- OCR
- face recognition
- geolocation
- complex approval
- rework
- skip
- execution-time step insertion
- inventory
- attendance
- dashboard
- CRM
- customer public pool
- contribution
- order core logic
