package com.zhisheng.mvp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhisheng.mvp.inventory.entity.InventoryTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryTransactionMapper extends BaseMapper<InventoryTransaction> {
}
