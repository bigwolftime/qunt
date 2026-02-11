package com.quant.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.model.entity.StockSecurityEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockSecurityMapper extends BaseMapper<StockSecurityEntity> {
}
