package com.hdu.secondhand.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情 VO（对齐《接口约定规范 v1.1》7.2；扩展字段保留供发布/个人中心使用）
 */
@Data
public class ProductVO {

    private Long id;
    private String title;
    private String description;

    /** 售价（分） */
    private Long price;

    /** 原价（分） */
    private Long originalPrice;

    /** AI 估价（分，扩展） */
    private Long estimatedPrice;

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

    // ---- 扩展字段 ----
    private Long sellerId;
    private Long categoryId;
    private Integer conditionLevel;
    private String conditionDesc;
    private String tags;
    private String location;
    private Integer status;
    private Integer favoriteCount;
    private Boolean favorited;
    private List<String> images;
}
