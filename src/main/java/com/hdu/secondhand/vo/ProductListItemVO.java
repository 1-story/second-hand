package com.hdu.secondhand.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品列表项 VO（对齐《接口约定规范 v1.1》7.1）
 */
@Data
public class ProductListItemVO {

    private Long id;
    private String title;

    /** 售价（分） */
    private Long price;

    /** 原价（分） */
    private Long originalPrice;

    /** 封面图 */
    private String cover;

    /** 分类 key：book/digital/living/sports/clothing/other */
    private String category;

    private String categoryName;

    /** 成色：100/90/80/70 */
    private String condition;

    private String conditionName;

    /** 浏览量 */
    private Integer views;

    private LocalDateTime createdAt;

    /** 卖家信息 */
    private SellerVO seller;
}
