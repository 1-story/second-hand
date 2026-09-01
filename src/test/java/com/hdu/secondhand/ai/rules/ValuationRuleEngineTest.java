package com.hdu.secondhand.ai.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 估价规则引擎单元测试
 */
class ValuationRuleEngineTest {

    @Test
    @DisplayName("正常估价：基准价1000、成色9、12个月、热度1.0、折旧15%")
    void estimate_normal() {
        ValuationRequest req = new ValuationRequest(
                new BigDecimal("1000"), 9, 12, BigDecimal.ONE);
        ValuationResult result = ValuationRuleEngine.estimate(req);

        assertNotNull(result);
        // 成色系数0.88 × 年限系数0.85 × 热度1.0 × 1000 = 748
        assertEquals(new BigDecimal("748"), result.getRecommend());
        // min = 748 * 0.9 = 673.2 → 673
        assertEquals(new BigDecimal("673"), result.getMin());
        // max = 748 * 1.1 = 822.8 → 823
        assertEquals(new BigDecimal("823"), result.getMax());

        // 明细可解释
        assertEquals("0.88", result.getDetail().get("conditionFactor"));
        assertEquals("0.8500", result.getDetail().get("ageFactor"));
        assertTrue(result.getDetail().containsKey("formula"));
    }

    @Test
    @DisplayName("分类折旧率生效：折旧率越高估价越低")
    void estimate_withDepreciationRate() {
        ValuationRequest req = new ValuationRequest(
                new BigDecimal("1000"), 8, 24, BigDecimal.ONE);
        // 折旧 10%
        ValuationResult lowDep = ValuationRuleEngine.estimate(req, new BigDecimal("0.10"));
        // 折旧 30%
        ValuationResult highDep = ValuationRuleEngine.estimate(req, new BigDecimal("0.30"));

        assertTrue(lowDep.getRecommend().compareTo(highDep.getRecommend()) > 0,
                "折旧率更高时应估价更低");
    }

    @Test
    @DisplayName("全新成色估价高于旧成色")
    void estimate_conditionMonotonic() {
        ValuationRequest fresh = new ValuationRequest(new BigDecimal("2000"), 10, 12, BigDecimal.ONE);
        ValuationRequest old = new ValuationRequest(new BigDecimal("2000"), 3, 12, BigDecimal.ONE);
        ValuationResult r1 = ValuationRuleEngine.estimate(fresh);
        ValuationResult r2 = ValuationRuleEngine.estimate(old);
        assertTrue(r1.getRecommend().compareTo(r2.getRecommend()) > 0);
    }

    @Test
    @DisplayName("使用时间越长估价越低（但不低于下限）")
    void estimate_ageDecay() {
        ValuationRequest newItem = new ValuationRequest(new BigDecimal("1000"), 7, 6, BigDecimal.ONE);
        ValuationRequest oldItem = new ValuationRequest(new BigDecimal("1000"), 7, 60, BigDecimal.ONE);
        ValuationResult r1 = ValuationRuleEngine.estimate(newItem);
        ValuationResult r2 = ValuationRuleEngine.estimate(oldItem);
        assertTrue(r1.getRecommend().compareTo(r2.getRecommend()) > 0);
        // 长年限不低于下限 5%
        assertTrue(r2.getRecommend().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("参数校验：基准价必须大于0")
    void estimate_invalidBasePrice() {
        ValuationRequest req = new ValuationRequest(BigDecimal.ZERO, 7, 12, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> ValuationRuleEngine.estimate(req));
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(new BigDecimal("-1"), 7, 12, BigDecimal.ONE)));
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(null, 7, 12, BigDecimal.ONE)));
    }

    @Test
    @DisplayName("参数校验：成色分越界")
    void estimate_invalidCondition() {
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(new BigDecimal("100"), 0, 12, BigDecimal.ONE)));
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(new BigDecimal("100"), 11, 12, BigDecimal.ONE)));
    }

    @Test
    @DisplayName("参数校验：使用月数不能为负")
    void estimate_invalidAge() {
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(new BigDecimal("100"), 7, -1, BigDecimal.ONE)));
    }

    @Test
    @DisplayName("参数校验：热度系数必须在0.5~2.0")
    void estimate_invalidHeat() {
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(new BigDecimal("100"), 7, 12, new BigDecimal("0.4"))));
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(new BigDecimal("100"), 7, 12, new BigDecimal("2.1"))));
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.estimate(new ValuationRequest(new BigDecimal("100"), 7, 12, null)));
    }

    @Test
    @DisplayName("年限系数：0月=1.0，长时间不低于5%下限")
    void computeAgeFactor_boundary() {
        assertEquals(BigDecimal.ONE, ValuationRuleEngine.computeAgeFactor(new BigDecimal("0.15"), 0));
        // 100年 → 衰减到下限
        BigDecimal factor = ValuationRuleEngine.computeAgeFactor(new BigDecimal("0.15"), 1200);
        assertEquals(0, factor.compareTo(new BigDecimal("0.05")), "年限衰减下限应为 5%");
        // 折旧率越界
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.computeAgeFactor(new BigDecimal("0.95"), 12));
        assertThrows(IllegalArgumentException.class,
                () -> ValuationRuleEngine.computeAgeFactor(new BigDecimal("-0.1"), 12));
    }

    @Test
    @DisplayName("估价区间对称：min ≤ rec ≤ max")
    void estimate_rangeOrder() {
        ValuationRequest req = new ValuationRequest(new BigDecimal("999"), 6, 18, new BigDecimal("1.2"));
        ValuationResult result = ValuationRuleEngine.estimate(req);
        assertTrue(result.getMin().compareTo(result.getRecommend()) <= 0);
        assertTrue(result.getRecommend().compareTo(result.getMax()) <= 0);
    }

    @Test
    @DisplayName("热度系数提高估价（手机热度 1.3 vs 默认 1.0）")
    void estimate_heatEffect() {
        ValuationRequest base = new ValuationRequest(new BigDecimal("3000"), 8, 12, BigDecimal.ONE);
        ValuationRequest hot = new ValuationRequest(new BigDecimal("3000"), 8, 12, new BigDecimal("1.3"));
        ValuationResult r1 = ValuationRuleEngine.estimate(base);
        ValuationResult r2 = ValuationRuleEngine.estimate(hot);
        assertTrue(r2.getRecommend().compareTo(r1.getRecommend()) > 0);
    }
}
