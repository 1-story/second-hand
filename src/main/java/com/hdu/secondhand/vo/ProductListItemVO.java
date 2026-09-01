package com.hdu.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项 VO
 */
@Data
public class ProductListItemVO {

    private Long id;
    private String title;
    private BigDecimal price;
    private BigDecimal estimatedPrice;
    private Integer conditionLevel;
    private String coverImage;
    private String location;
    private Integer viewCount;
    private Integer favoriteCount;
    private String categoryName;
    private LocalDateTime createdAt;
}
