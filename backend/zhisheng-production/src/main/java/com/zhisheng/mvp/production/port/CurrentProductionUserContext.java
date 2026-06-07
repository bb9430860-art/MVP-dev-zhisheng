package com.zhisheng.mvp.production.port;

import java.util.List;

public record CurrentProductionUserContext(
        Long currentUserId,
        Long tenantId,
        List<String> roles) {
}
