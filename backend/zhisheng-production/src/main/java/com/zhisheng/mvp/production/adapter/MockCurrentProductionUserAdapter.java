package com.zhisheng.mvp.production.adapter;

import com.zhisheng.mvp.production.port.CurrentProductionUserContext;
import com.zhisheng.mvp.production.port.CurrentProductionUserPort;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class MockCurrentProductionUserAdapter implements CurrentProductionUserPort {

    private CurrentProductionUserContext currentUser =
            new CurrentProductionUserContext(201L, 1L, List.of("WORKER"));

    // TODO: replace with platform-auth current user contract
    @Override
    public CurrentProductionUserContext currentUser() {
        return currentUser;
    }

    public void setCurrentUser(Long currentUserId, Long tenantId, List<String> roles) {
        this.currentUser = new CurrentProductionUserContext(currentUserId, tenantId, List.copyOf(roles));
    }
}
