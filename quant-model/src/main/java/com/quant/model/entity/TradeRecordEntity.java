package com.quant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录实体。
 */
@Data
@Accessors(chain = true)
@TableName("trade_record")
public class TradeRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String accountId;

    private String brokerCode;

    private String tsCode;

    private String stockCode;

    private String stockName;

    private String side;

    private BigDecimal limitPrice;

    private Long requestedQuantity;

    private BigDecimal executedPrice;

    private Long executedQuantity;

    private String brokerOrderNo;

    private String status;

    private String statusMessage;

    private String rawPayload;

    private LocalDateTime requestedAt;

    private LocalDateTime executedAt;
}
