package com.hdu.secondhand.dto;

import lombok.Data;

/**
 * 商品分页检索入参
 */
@Data
public class ProductQueryDTO {

    /** 关键词（匹配标题/描述） */
    private String keyword;

    /** 分类 ID（旧参数，兼容） */
    private Long categoryId;

    /** 分类 key：book/digital/living/sports/clothing/other（规范 7.1，优先于 categoryId） */
    private String category;

    /** 最低价（单位：分） */
    private Long minPrice;

    /** 最高价（单位：分） */
    private Long maxPrice;

    /** 成色下限 1~10（旧参数，兼容） */
    private Integer conditionLevel;

    /** 成色：100/90/80/70（规范 7.1，优先于 conditionLevel） */
    private Integer condition;

    /** 排序：1最新(默认) 2价格升 3价格降 4浏览量（旧参数，兼容） */
    private Integer sortBy = 1;

    /** 排序：latest/price_asc/price_desc/hot（规范 7.1，优先于 sortBy） */
    private String sort;

    /** 页码（从 1 开始） */
    private Integer page = 1;

    /** 每页大小（1~100） */
    private Integer size = 10;
}
