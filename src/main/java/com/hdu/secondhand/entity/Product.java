package com.hdu.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品主表（核心业务表）
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家 ID */
    private Long sellerId;

    /** 分类 ID */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** 详细描述 */
    private String description;

    /** 期望售价 */
    private BigDecimal price;

    /** 原价/入手价（AI 估价依据，规范 7.1 originalPrice） */
    private BigDecimal originalPrice;

    /** AI 估价（可空） */
    private BigDecimal estimatedPrice;

    /** 实际成交价 */
    private BigDecimal finalPrice;

    /** 成色等级 1~10（10=全新） */
    private Integer conditionLevel;

    /** 成色文字描述 */
    private String conditionDesc;

    /** 标签，逗号分隔 */
    private String tags;

    /** 交易地点/校区 */
    private String location;

    /** 封面图 URL */
    private String coverImage;

    /** 状态 0草稿 1在售 2已下架 3已售出 4审核中 5审核驳回 */
    private Integer status;

    /** 浏览量 */
    private Integer viewCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 审核驳回原因（管理员审核，规范 6.5 流程） */
    private String reviewRemark;

    /** 逻辑删除 0否 1是 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
