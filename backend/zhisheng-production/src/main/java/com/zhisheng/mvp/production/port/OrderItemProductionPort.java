package com.zhisheng.mvp.production.port;

import java.math.BigDecimal;

public interface OrderItemProductionPort {

    void markDispatched(Long orderItemId, String productionStatus, BigDecimal productionProgress, Long routeInstanceId);
}
