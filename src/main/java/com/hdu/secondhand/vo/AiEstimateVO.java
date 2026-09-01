package com.hdu.secondhand.vo;

import lombok.Data;

/**
 * AI 智能估价结果（对齐《接口约定规范 v1.0》6.3）
 */
@Data
public class AiEstimateVO {

    /** 建议售价（分） */
    private Long suggestPrice;

    /** 价格区间（分） */
    private AiPriceRangeVO priceRange;

    /** 估价理由 */
    private String reason;

    /** 结果来源：llm（大模型）/ rule（规则引擎兜底） */
    private String engine;

    /** 系数明细（可解释，附加字段） */
    private java.util.Map<String, Object> detail;

    /** 来源数字：1规则引擎 2大模型 3混合（附加字段） */
    private Integer source;
}
