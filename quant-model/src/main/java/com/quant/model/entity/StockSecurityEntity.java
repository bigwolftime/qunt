package com.quant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 股票基础信息实体。
 */
@TableName("stock_security")
@Data
@Accessors(chain = true)
public class StockSecurityEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 统一证券代码，如 002342.SZ。 */
    private String tsCode;

    /** 纯证券代码，如 002342。 */
    private String code;

    /** 交易所编码，如 SSE/SZSE/BSE。 */
    private String exchange;

    /** 证券简称。 */
    private String name;

    /** 地区编码（来源于上游 areacode）。 */
    private String area;

    /** 板块类型，如 MAIN/STAR/GEM/BSE/OTHER。 */
    private String boardType;

}
