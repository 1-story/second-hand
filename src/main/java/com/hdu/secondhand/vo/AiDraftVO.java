package com.hdu.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 自动填表草稿 VO（返回给前端确认）
 */
@Data
public class AiDraftVO {

    /** 草稿 ID */
    private Long draftId;

    /** 识别分类 ID */
    private Long categoryId;

    /** 识别分类名称 */
    private String categoryName;

    /** 自动生成标题 */
    private String title;

    /** 自动生成描述 */
    private String description;

    /** 成色等级 1~10 */
    private Integer conditionLevel;

    /** 成色文字描述 */
    private String conditionDesc;

    /** 推荐售价（AI 估价推荐值，分） */
    private Long suggestPrice;

    /** 估价区间 [min, max]（分） */
    private Long minPrice;

    private Long maxPrice;

    /** 图片列表 */
    private List<String> images;

    /** 草稿状态 0待确认 */
    private Integer status;
}
