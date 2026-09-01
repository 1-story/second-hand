package com.hdu.secondhand.ai.rules;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 估价结果：区间 + 系数明细（可解释、可审计）
 */
public final class ValuationResult {

    private final BigDecimal min;
    private final BigDecimal recommend;
    private final BigDecimal max;
    private final Map<String, Object> detail;

    public ValuationResult(BigDecimal min, BigDecimal recommend, BigDecimal max, Map<String, Object> detail) {
        this.min = min;
        this.recommend = recommend;
        this.max = max;
        this.detail = detail == null ? new LinkedHashMap<>() : detail;
    }

    /** 最低估价（元） */
    public BigDecimal getMin() {
        return min;
    }

    /** 推荐估价（元） */
    public BigDecimal getRecommend() {
        return recommend;
    }

    /** 最高估价（元） */
    public BigDecimal getMax() {
        return max;
    }

    /** 系数明细：basePrice/conditionFactor/ageFactor/heatFactor/formula */
    public Map<String, Object> getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return "ValuationResult{min=" + min + ", recommend=" + recommend + ", max=" + max + ", detail=" + detail + '}';
    }
}
