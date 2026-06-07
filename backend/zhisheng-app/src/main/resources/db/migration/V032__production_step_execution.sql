ALTER TABLE production_step_instance
    ADD COLUMN started_by BIGINT NULL;

ALTER TABLE production_step_instance
    ADD COLUMN completed_by BIGINT NULL;

CREATE INDEX idx_production_step_assigned_user_status
    ON production_step_instance (tenant_id, assigned_user_id, status, deleted);

CREATE INDEX idx_production_step_assigned_role_status
    ON production_step_instance (tenant_id, assigned_role, status, deleted);

CREATE INDEX idx_production_step_route_order_status
    ON production_step_instance (tenant_id, route_instance_id, step_order, status, deleted);
