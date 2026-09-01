package com.hdu.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * AI 估价结果 VO
 */
@Data
public class AiEstimateVO {

    /** 最低估价 */
    private BigDecimal min;

    /** 推荐估价 */
    private BigDecimal recommend;

    /** 最高估价 */
    private BigDecimal max;

    /** 系数明细（成色系数/年限系数/热度系数等） */
    private Map<String, Object> detail;

    /** 来源 1规则引擎 2大模型 3混合 */
    private Integer source;
}
