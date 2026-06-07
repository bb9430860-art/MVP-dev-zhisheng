package com.zhisheng.mvp.production.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhisheng.mvp.production.entity.ProductionWorkOrder;
import com.zhisheng.mvp.production.mapper.ProductionWorkOrderMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class WorkOrderNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ProductionWorkOrderMapper workOrderMapper;

    public WorkOrderNoGenerator(ProductionWorkOrderMapper workOrderMapper) {
        this.workOrderMapper = workOrderMapper;
    }

    public String nextNo(Long tenantId) {
        String datePart = LocalDate.now(Clock.systemDefaultZone()).format(DATE_FORMATTER);
        String prefix = "WO-" + datePart + "-";
        ProductionWorkOrder latest = workOrderMapper.selectOne(new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getTenantId, tenantId)
                .likeRight(ProductionWorkOrder::getWorkOrderNo, prefix)
                .orderByDesc(ProductionWorkOrder::getWorkOrderNo)
                .last("limit 1"));
        int nextSequence = latest == null ? 1 : parseSequence(latest.getWorkOrderNo()) + 1;
        return prefix + String.format("%04d", nextSequence);
    }

    private int parseSequence(String workOrderNo) {
        if (workOrderNo == null || workOrderNo.length() < 4) {
            return 0;
        }
        return Integer.parseInt(workOrderNo.substring(workOrderNo.length() - 4));
    }
}
