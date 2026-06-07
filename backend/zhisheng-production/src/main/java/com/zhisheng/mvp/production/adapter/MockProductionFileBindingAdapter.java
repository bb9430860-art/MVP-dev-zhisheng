package com.zhisheng.mvp.production.adapter;

import com.zhisheng.mvp.production.exception.ProductionStepCheckinException;
import com.zhisheng.mvp.production.port.ProductionFileBindingPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class MockProductionFileBindingAdapter implements ProductionFileBindingPort {

    private int bindCallCount;
    private Long lastTenantId;
    private String lastBizType;
    private Long lastBizId;
    private List<Long> lastFileIds = List.of();
    private boolean failNextBind;

    // TODO: replace with shared file service contract
    @Override
    public void bindFiles(Long tenantId, String bizType, Long bizId, List<Long> fileIds) {
        if (failNextBind) {
            failNextBind = false;
            throw new ProductionStepCheckinException("FILE_BIND_FAILED");
        }
        this.bindCallCount++;
        this.lastTenantId = tenantId;
        this.lastBizType = bizType;
        this.lastBizId = bizId;
        this.lastFileIds = List.copyOf(fileIds == null ? List.of() : fileIds);
    }

    public void reset() {
        bindCallCount = 0;
        lastTenantId = null;
        lastBizType = null;
        lastBizId = null;
        lastFileIds = List.of();
        failNextBind = false;
    }

    public void failNextBind() {
        this.failNextBind = true;
    }

    public int bindCallCount() {
        return bindCallCount;
    }

    public Long lastTenantId() {
        return lastTenantId;
    }

    public String lastBizType() {
        return lastBizType;
    }

    public Long lastBizId() {
        return lastBizId;
    }

    public List<Long> lastFileIds() {
        return new ArrayList<>(lastFileIds);
    }
}
