package com.hdu.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品发布入参
 */
@Data
public class ProductCreateDTO {

    /** 分类 ID（必填） */
    private Long categoryId;

    /** 标题（必填，1~100 字） */
    private String title;

    /** 详细描述 */
    private String description;

    /** 期望售价（必填，单位：分，>0） */
    private Long price;

    /** 原价（可选，单位：分，AI 估价依据，规范 7.1 originalPrice） */
    private Long originalPrice;

    /** 成色等级 1~10（默认 7） */
    private Integer conditionLevel;

    /** 成色文字描述 */
    private String conditionDesc;

    /** 标签，逗号分隔 */
    private String tags;

    /** 交易地点/校区 */
    private String location;

    /** 封面图 URL */
    private String coverImage;

    /** 图片列表 */
    private List<String> images;

    /** AI 估价（可选，单位：分，自动填表发布时带入） */
    private Long estimatedPrice;

    /** 发布后状态：true=立即上架(1)，false=草稿(0)；默认立即上架 */
    private Boolean publishNow;
}
