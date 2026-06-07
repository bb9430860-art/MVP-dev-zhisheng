package com.zhisheng.mvp.production.port;

import java.util.List;

public interface ProductionFileBindingPort {

    void bindFiles(Long tenantId, String bizType, Long bizId, List<Long> fileIds);
}
