package com.quant.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.model.entity.StockDailyQuoteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StockDailyQuoteMapper extends BaseMapper<StockDailyQuoteEntity> {

    /**
     * 批量幂等写入，唯一键(ts_code, trade_date)冲突时更新行情字段。
     */
    int upsertBatch(@Param("list") List<StockDailyQuoteEntity> list);
}
