package com.hdu.secondhand.ai.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 估价规则引擎（核心算法，纯 Java，零第三方依赖）
 *
 * <p>估价公式：</p>
 * <pre>
 *   推荐价 = 分类基准价 × 成色系数 × 年限衰减系数 × 市场热度系数
 *   区间   = 推荐价 × [0.90, 1.10]（四舍五入到元）
 * </pre>
 *
 * <p>系数来源：
 * <ul>
 *   <li>成色系数：成色分 1~10 映射分档表（人工标定）</li>
 *   <li>年限衰减：ageFactor = (1 - 折旧率)^(月数/12)，下限 5%</li>
 *   <li>热度系数：分类表 heat_weight（0.5~2.0），由运营维护</li>
 * </ul></p>
 *
 * <p>本类可独立单元测试；业务层负责从分类表取参数并落库审计。</p>
 */
public final class ValuationRuleEngine {

    /** 区间下浮比例 */
    private static final BigDecimal RANGE_LOW = new BigDecimal("0.90");
    /** 区间上浮比例 */
    private static final BigDecimal RANGE_HIGH = new BigDecimal("1.10");
    /** 年限衰减下限 */
    private static final BigDecimal MIN_AGE_FACTOR = new BigDecimal("0.05");

    /** 成色分档系数表（成色分 → 系数），index 0 不使用，成色 1~10 */
    private static final BigDecimal[] CONDITION_FACTORS = {
            new BigDecimal("0.12"),  // 1：极差
            new BigDecimal("0.17"),  // 2
            new BigDecimal("0.24"),  // 3
            new BigDecimal("0.32"),  // 4
            new BigDecimal("0.42"),  // 5
            new BigDecimal("0.52"),  // 6
            new BigDecimal("0.64"),  // 7
            new BigDecimal("0.76"),  // 8
            new BigDecimal("0.88"),  // 9
            new BigDecimal("1.00")   // 10：全新
    };

    private ValuationRuleEngine() {
    }

    /**
     * 执行估价（使用默认通用折旧率 15%/年）
     *
     * @param req 估价参数
     * @return 估价结果（含系数明细）
     * @throws IllegalArgumentException 参数越界时抛出
     */
    public static ValuationResult estimate(ValuationRequest req) {
        return estimate(req, new BigDecimal("0.15"));
    }

    /**
     * 执行估价（使用分类表配置的年折旧率）
     *
     * @param req              估价参数
     * @param depreciationRate 年折旧率（0~0.9，通常来自分类表）
     * @return 估价结果（含系数明细）
     * @throws IllegalArgumentException 参数越界时抛出
     */
    public static ValuationResult estimate(ValuationRequest req, BigDecimal depreciationRate) {
        if (req == null) {
            throw new IllegalArgumentException("估价参数不能为空");
        }
        BigDecimal basePrice = req.getBasePrice();
        int conditionScore = req.getConditionScore();
        int ageMonths = req.getAgeMonths();
        BigDecimal heatFactor = req.getHeatFactor();

        // ---- 参数校验 ----
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("基准价必须大于 0");
        }
        if (conditionScore < 1 || conditionScore > 10) {
            throw new IllegalArgumentException("成色分必须在 1~10 之间，当前：" + conditionScore);
        }
        if (ageMonths < 0) {
            throw new IllegalArgumentException("使用月数不能为负数：" + ageMonths);
        }
        if (heatFactor == null
                || heatFactor.compareTo(new BigDecimal("0.5")) < 0
                || heatFactor.compareTo(new BigDecimal("2.0")) > 0) {
            throw new IllegalArgumentException("热度系数必须在 0.5~2.0 之间，当前：" + heatFactor);
        }

        // ---- 系数计算 ----
        BigDecimal conditionFactor = CONDITION_FACTORS[conditionScore - 1];

        // 年限衰减系数：按分类表折旧率计算
        BigDecimal ageFactor = computeAgeFactor(depreciationRate, ageMonths);

        // ---- 推荐价 ----
        BigDecimal recommend = basePrice
                .multiply(conditionFactor)
                .multiply(ageFactor)
                .multiply(heatFactor)
                .setScale(0, RoundingMode.HALF_UP);

        // ---- 区间 ----
        BigDecimal min = recommend.multiply(RANGE_LOW).setScale(0, RoundingMode.HALF_UP);
        BigDecimal max = recommend.multiply(RANGE_HIGH).setScale(0, RoundingMode.HALF_UP);

        // ---- 明细（可解释） ----
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("basePrice", basePrice.stripTrailingZeros().toPlainString());
        detail.put("conditionScore", conditionScore);
        detail.put("conditionFactor", conditionFactor.toPlainString());
        detail.put("ageMonths", ageMonths);
        detail.put("ageFactor", ageFactor.toPlainString());
        detail.put("heatFactor", heatFactor.stripTrailingZeros().toPlainString());
        detail.put("formula", "推荐价 = 基准价 × 成色系数 × 年限系数 × 热度系数；区间 = 推荐价 × [0.90, 1.10]");

        return new ValuationResult(min, recommend, max, detail);
    }

    /**
     * 年限衰减系数：ageFactor = (1 - depreciationRate)^(ageMonths/12)，下限 5%
     *
     * @param depreciationRate 年折旧率（0~0.9）
     * @param ageMonths        使用月数
     */
    public static BigDecimal computeAgeFactor(BigDecimal depreciationRate, int ageMonths) {
        if (depreciationRate == null
                || depreciationRate.compareTo(BigDecimal.ZERO) < 0
                || depreciationRate.compareTo(new BigDecimal("0.9")) > 0) {
            throw new IllegalArgumentException("折旧率必须在 0~0.9 之间");
        }
        if (ageMonths < 0) {
            throw new IllegalArgumentException("使用月数不能为负数");
        }
        if (ageMonths == 0) {
            return BigDecimal.ONE;
        }
        double rate = 1.0 - depreciationRate.doubleValue();
        double factor = Math.pow(rate, ageMonths / 12.0);
        BigDecimal result = BigDecimal.valueOf(factor).setScale(4, RoundingMode.HALF_UP);
        if (result.compareTo(MIN_AGE_FACTOR) < 0) {
            return MIN_AGE_FACTOR;
        }
        return result;
    }
}
