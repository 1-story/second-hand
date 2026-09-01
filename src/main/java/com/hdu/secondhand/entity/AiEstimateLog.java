package com.hdu.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 估价记录表（可追溯、可审计）
 */
@Data
@TableName("ai_estimate_log")
public class AiEstimateLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起用户 ID */
    private Long userId;

    /** 关联商品 ID */
    private Long productId;

    /** 分类 ID */
    private Long categoryId;

    /** 估价输入描述 */
    private String inputDesc;

    /** 基准价 */
    private BigDecimal basePrice;

    /** 成色分 1~10 */
    private BigDecimal conditionScore;

    /** 使用月数 */
    private Integer ageMonths;

    /** 热度系数 */
    private BigDecimal heatFactor;

    /** 最低估价 */
    private BigDecimal estimatedMin;

    /** 推荐估价 */
    private BigDecimal estimatedRec;

    /** 最高估价 */
    private BigDecimal estimatedMax;

    /** 系数明细（JSON 字符串） */
    private String detailJson;

    /** 来源 1规则引擎 2大模型 3混合 */
    private Integer source;

    private LocalDateTime createdAt;
}
