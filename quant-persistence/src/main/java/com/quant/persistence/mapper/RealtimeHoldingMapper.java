package com.quant.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.model.entity.RealtimeHoldingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RealtimeHoldingMapper extends BaseMapper<RealtimeHoldingEntity> {

    int upsertBatch(@Param("list") List<RealtimeHoldingEntity> list);
}
