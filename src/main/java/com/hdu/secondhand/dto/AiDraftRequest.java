package com.hdu.secondhand.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 自动填表入参：图片 + 关键词提示
 */
@Data
public class AiDraftRequest {

    /** 图片 URL 列表（由前端上传后传入；Mock 模式可不传） */
    private List<String> images;

    /** 用户提示/关键词，例如："iPad 用了两年 屏幕有划痕" */
    private String hint;

    /** 期望售价（可选，单位：分） */
    private Long expectPrice;
}
