package com.zhisheng.mvp.production.port;

import java.util.List;
import java.util.Optional;

public interface OrderItemCandidateReadPort {

    List<OrderItemCandidateContext> listCandidates(OrderItemCandidateQuery query);

    Optional<OrderItemCandidateContext> findCandidateById(Long orderItemId);
}
