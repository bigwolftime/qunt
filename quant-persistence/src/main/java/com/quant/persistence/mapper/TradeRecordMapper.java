package com.quant.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.model.entity.TradeRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeRecordMapper extends BaseMapper<TradeRecordEntity> {
}
