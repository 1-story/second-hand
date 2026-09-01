package com.hdu.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品编辑入参（仅本人可编辑）
 */
@Data
public class ProductUpdateDTO {

    /** 分类 ID */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** 详细描述 */
    private String description;

    /** 期望售价（单位：分） */
    private Long price;

    /** 原价（单位：分） */
    private Long originalPrice;

    /** 成色等级 1~10 */
    private Integer conditionLevel;

    /** 成色文字描述 */
    private String conditionDesc;

    /** 标签，逗号分隔 */
    private String tags;

    /** 交易地点/校区 */
    private String location;

    /** 封面图 URL */
    private String coverImage;

    /** 图片列表（整体替换） */
    private List<String> images;
}
