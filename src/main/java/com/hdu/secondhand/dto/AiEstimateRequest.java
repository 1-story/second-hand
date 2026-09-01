package com.hdu.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 估价入参
 */
@Data
public class AiEstimateRequest {

    /** 分类 ID（必填，估值参数来自分类表） */
    private Long categoryId;

    /** 输入描述（可选，规则引擎忽略，大模型补充时使用） */
    private String description;

    /** 成色等级 1~10（默认 7） */
    private Integer conditionLevel = 7;

    /** 使用月数（默认 12） */
    private Integer ageMonths = 12;

    /** 是否使用大模型补充估价（默认 false；ai.enabled=false 时自动忽略） */
    private Boolean useLlm = false;
}
