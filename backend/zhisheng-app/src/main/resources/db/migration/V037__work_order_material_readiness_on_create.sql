ALTER TABLE production_work_order_material
    ADD COLUMN available_qty_snapshot DECIMAL(18, 4) NULL;

ALTER TABLE production_work_order_material
    ADD COLUMN shortage_qty DECIMAL(18, 4) NULL;

ALTER TABLE production_work_order_material
    ADD COLUMN readiness_status VARCHAR(30) NULL;

ALTER TABLE production_work_order_material
    ADD COLUMN readiness_checked_at DATETIME NULL;

ALTER TABLE production_work_order_material
    ADD COLUMN readiness_message VARCHAR(500) NULL;

CREATE INDEX idx_work_order_material_readiness
    ON production_work_order_material (tenant_id, readiness_status, deleted);
