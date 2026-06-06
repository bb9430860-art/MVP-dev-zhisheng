package com.zhisheng.mvp.production.port;

import java.util.Optional;

public interface OrderItemReadPort {

    Optional<OrderItemProductionContext> findById(Long orderItemId);
}
