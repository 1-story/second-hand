package com.hdu.secondhand.dto;

import lombok.Data;

/**
 * AI 一键发布入参
 */
@Data
public class AiPublishRequest {

    /** 草稿 ID（必填） */
    private Long draftId;

    /** 用户对草稿的微调：最终标题（可选，默认取草稿） */
    private String title;

    /** 最终期望售价（可选，单位：分，默认取草稿估价） */
    private Long price;
}
