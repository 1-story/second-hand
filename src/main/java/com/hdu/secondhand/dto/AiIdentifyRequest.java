package com.hdu.secondhand.dto;

import lombok.Data;

/**
 * AI 商品识别入参（对齐《接口约定规范 v1.0》6.1）
 */
@Data
public class AiIdentifyRequest {

    /** 商品图片 base64（含 data:image/...;base64, 前缀，单张 ≤5MB） */
    private String imageBase64;

    /** 用户提示/关键词（可选，辅助识别；Mock 模式主要依据） */
    private String hint;
}
