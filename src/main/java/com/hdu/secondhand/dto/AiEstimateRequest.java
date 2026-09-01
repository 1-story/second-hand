package com.hdu.secondhand.dto;

import lombok.Data;

/**
 * AI 智能估价入参（对齐《接口约定规范 v1.0》6.3）
 */
@Data
public class AiEstimateRequest {

    /** 原价（单位：分，必填，>0） */
    private Long originalPrice;

    /** 商品分类 key：book/digital/living/sports/clothing/other */
    private String category;

    /** 成色：100(全新)/90(九成新)/80(八成新)/70(七成新及以下) */
    private Integer condition;

    /** 商品图片 base64（含 data:image/...;base64, 前缀，可选，用于大模型补充） */
    private String imageBase64;
}
