package com.zhisheng.mvp.production.enums;

import java.util.Set;

public enum ProductionWorkOrderStatus {
    DRAFT,
    RELEASED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Set<ProductionWorkOrderStatus> ACTIVE = Set.of(DRAFT, RELEASED, IN_PROGRESS);

    public boolean active() {
        return ACTIVE.contains(this);
    }

    public static boolean isActive(String status) {
        if (status == null) {
            return false;
        }
        try {
            return ACTIVE.contains(valueOf(status));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
