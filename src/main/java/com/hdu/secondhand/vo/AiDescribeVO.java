package com.hdu.secondhand.vo;

import lombok.Data;

/**
 * AI 描述生成结果（对齐《接口约定规范 v1.0》6.2）
 */
@Data
public class AiDescribeVO {

    /** 自动生成的描述草稿 */
    private String description;

    /** 结果来源：llm / rule */
    private String engine;
}
