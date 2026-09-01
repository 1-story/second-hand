package com.hdu.secondhand.vo;

import lombok.Data;

/**
 * AI 商品识别结果（对齐《接口约定规范 v1.0》6.1）
 */
@Data
public class AiIdentifyVO {

    /** 分类 key：book/digital/living/sports/clothing/other */
    private String category;

    /** 分类中文名 */
    private String categoryName;

    /** 成色：100/90/80/70 */
    private Integer condition;

    /** 成色中文名 */
    private String conditionName;

    /** 置信度 0~1 */
    private Double confidence;

    /** 结果来源：llm / rule */
    private String engine;
}
