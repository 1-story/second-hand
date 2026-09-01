package com.hdu.secondhand.dto;

import lombok.Data;

/**
 * 商品分页检索入参
 */
@Data
public class ProductQueryDTO {

    /** 关键词（匹配标题/描述，走 LIKE 兜底；FULLTEXT 索引已建） */
    private String keyword;

    /** 分类 ID */
    private Long categoryId;

    /** 最低价（单位：分） */
    private Long minPrice;

    /** 最高价（单位：分） */
    private Long maxPrice;

    /** 成色下限（1~10） */
    private Integer conditionLevel;

    /** 排序：1最新发布(默认) 2价格升序 3价格降序 4浏览量最多 */
    private Integer sortBy = 1;

    /** 页码（从 1 开始） */
    private Integer page = 1;

    /** 每页大小（1~100） */
    private Integer size = 10;
}
