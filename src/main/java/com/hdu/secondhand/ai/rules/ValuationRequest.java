package com.hdu.secondhand.ai.rules;

import java.math.BigDecimal;

/**
 * 估价请求参数
 *
 * <p>所有参数由业务层从分类表与用户输入组装，
 * 本类只承载计算输入，不依赖任何框架。</p>
 */
public final class ValuationRequest {

    private final BigDecimal basePrice;
    private final int conditionScore;
    private final int ageMonths;
    private final BigDecimal heatFactor;

    public ValuationRequest(BigDecimal basePrice, int conditionScore, int ageMonths, BigDecimal heatFactor) {
        this.basePrice = basePrice;
        this.conditionScore = conditionScore;
        this.ageMonths = ageMonths;
        this.heatFactor = heatFactor;
    }

    /** 分类基准价（元） */
    public BigDecimal getBasePrice() {
        return basePrice;
    }

    /** 成色分 1~10（10=全新） */
    public int getConditionScore() {
        return conditionScore;
    }

    /** 使用月数 */
    public int getAgeMonths() {
        return ageMonths;
    }

    /** 市场热度系数 0.5~2.0 */
    public BigDecimal getHeatFactor() {
        return heatFactor;
    }

    @Override
    public String toString() {
        return "ValuationRequest{" +
                "basePrice=" + basePrice +
                ", conditionScore=" + conditionScore +
                ", ageMonths=" + ageMonths +
                ", heatFactor=" + heatFactor +
                '}';
    }
}
