package com.quant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时持仓实体。
 */
@Data
@Accessors(chain = true)
@TableName("realtime_holding")
public class RealtimeHoldingEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String accountId;

    private String brokerCode;

    private String tsCode;

    private String stockCode;

    private String stockName;

    private Long quantity;

    private Long availableQuantity;

    private BigDecimal costPrice;

    private BigDecimal lastPrice;

    private BigDecimal marketValue;

    private BigDecimal profitAmount;

    private BigDecimal profitRatio;

    private LocalDateTime snapshotTime;

    private String rawPayload;
}
