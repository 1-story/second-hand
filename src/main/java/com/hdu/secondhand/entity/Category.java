package com.hdu.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品分类表（含 AI 估价规则参数）
 */
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 ID，0 为一级 */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 分类图标 URL */
    private String icon;

    /** AI 估价基准价（该品类 9 成新基准） */
    private BigDecimal basePrice;

    /** 年折旧率（默认 15%/年） */
    private BigDecimal depreciationRate;

    /** 市场热度系数（0.5~2.0） */
    private BigDecimal heatWeight;

    /** 排序 */
    private Integer sortOrder;

    /** 状态 0停用 1启用 */
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
