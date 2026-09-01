package com.hdu.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情 VO（含图片与卖家信息）
 */
@Data
public class ProductVO {

    private Long id;
    private Long sellerId;
    private String sellerNickname;
    private Integer sellerCredit;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    /** 价格（分） */
    private Long price;
    /** AI 估价（分） */
    private Long estimatedPrice;
    private Integer conditionLevel;
    private String conditionDesc;
    private String tags;
    private String location;
    private String coverImage;
    private Integer status;
    private Integer viewCount;
    private Integer favoriteCount;
    private Boolean favorited;
    private LocalDateTime createdAt;
    private List<String> images;
}
