package com.hdu.secondhand.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 描述生成入参（对齐《接口约定规范 v1.0》6.2）
 */
@Data
public class AiDescribeRequest {

    /** 商品图片 base64（含 data:image/...;base64, 前缀） */
    private String imageBase64;

    /** 分类 key */
    private String category;

    /** 成色 100/90/80/70 */
    private Integer condition;

    /** 卖家补充关键词 */
    private List<String> keywords;
}
