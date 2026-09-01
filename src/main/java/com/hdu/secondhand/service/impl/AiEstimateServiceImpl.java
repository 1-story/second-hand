package com.hdu.secondhand.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdu.secondhand.ai.AiService;
import com.hdu.secondhand.ai.CategoryEnum;
import com.hdu.secondhand.ai.rules.ValuationRequest;
import com.hdu.secondhand.ai.rules.ValuationResult;
import com.hdu.secondhand.ai.rules.ValuationRuleEngine;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ResultCode;
import com.hdu.secondhand.dto.AiEstimateRequest;
import com.hdu.secondhand.entity.AiEstimateLog;
import com.hdu.secondhand.entity.Category;
import com.hdu.secondhand.mapper.AiEstimateLogMapper;
import com.hdu.secondhand.mapper.CategoryMapper;
import com.hdu.secondhand.util.MoneyUtil;
import com.hdu.secondhand.vo.AiEstimateVO;
import com.hdu.secondhand.vo.AiPriceRangeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 智能估价服务实现（对齐《接口约定规范 v1.0》6.3 双层架构）
 *
 * <p>第一层：规则引擎（原价 × 成色系数 × 年限衰减 × 热度），任何环境可用；
 * 第二层：大模型补充（ai.enabled=true 时尝试，失败自动降级第一层，engine 标记 rule）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEstimateServiceImpl implements com.hdu.secondhand.service.AiEstimateService {

    private final CategoryMapper categoryMapper;
    private final AiEstimateLogMapper aiEstimateLogMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Value("${ai.enabled:false}")
    private boolean aiEnabled;

    @Override
    public AiEstimateVO estimate(AiEstimateRequest req, long userId) {
        // ---- 入参校验 ----
        if (req.getOriginalPrice() == null || req.getOriginalPrice() <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "originalPrice（分）必须大于 0");
        }
        String key = StringUtils.hasText(req.getCategory()) ? req.getCategory().trim().toLowerCase() : "other";
        Long categoryId = CategoryEnum.toId(key);
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // ---- 第一层：规则引擎（原价模式） ----
        int level = CategoryEnum.toLevel(req.getCondition());
        BigDecimal originalYuan = MoneyUtil.toYuan(req.getOriginalPrice());
        BigDecimal heat = category.getHeatWeight() == null ? BigDecimal.ONE : category.getHeatWeight();
        BigDecimal depRate = category.getDepreciationRate() == null
                ? new BigDecimal("0.15") : category.getDepreciationRate();

        ValuationResult ruleResult = ValuationRuleEngine.estimate(
                new ValuationRequest(originalYuan, level, 12, heat), depRate);

        // ---- 第二层：大模型补充（可选，失败降级） ----
        int source = 1;
        String engine = "rule";
        BigDecimal recYuan = ruleResult.getRecommend();
        try {
            if (aiEnabled) {
                BigDecimal llm = aiService.llmEstimate(
                        category.getName(), "原价 " + originalYuan + " 元，成色 " + CategoryEnum.conditionName(req.getCondition()),
                        recYuan);
                if (llm != null && llm.compareTo(BigDecimal.ZERO) > 0) {
                    // 大模型权重 0.4 + 规则 0.6
                    recYuan = recYuan.multiply(new BigDecimal("0.6"))
                            .add(llm.multiply(new BigDecimal("0.4")))
                            .setScale(0, java.math.RoundingMode.HALF_UP);
                    source = 3;
                    engine = "llm";
                }
            }
        } catch (Exception e) {
            log.warn("大模型估价失败，降级规则引擎: {}", e.getMessage());
            // 保持 rule 结果
        }

        // ---- 组装区间（分） ----
        Long minFen = MoneyUtil.toFen(ruleResult.getMin());
        Long maxFen = MoneyUtil.toFen(ruleResult.getMax());
        Long suggestFen = MoneyUtil.toFen(recYuan);

        // ---- 落库审计 ----
        AiEstimateLog logRecord = new AiEstimateLog();
        logRecord.setUserId(userId);
        logRecord.setCategoryId(category.getId());
        logRecord.setInputDesc("原价 " + originalYuan + " 元，成色 " + CategoryEnum.conditionName(req.getCondition()));
        logRecord.setBasePrice(originalYuan);
        logRecord.setConditionScore(BigDecimal.valueOf(level));
        logRecord.setAgeMonths(12);
        logRecord.setHeatFactor(heat);
        logRecord.setEstimatedMin(ruleResult.getMin());
        logRecord.setEstimatedRec(recYuan);
        logRecord.setEstimatedMax(ruleResult.getMax());
        logRecord.setSource(source);
        try {
            logRecord.setDetailJson(objectMapper.writeValueAsString(ruleResult.getDetail()));
        } catch (Exception e) {
            logRecord.setDetailJson("{}");
        }
        logRecord.setCreatedAt(LocalDateTime.now());
        aiEstimateLogMapper.insert(logRecord);

        // ---- 返回 ----
        AiEstimateVO vo = new AiEstimateVO();
        vo.setSuggestPrice(suggestFen);
        vo.setPriceRange(AiPriceRangeVO.of(minFen, maxFen));
        vo.setReason(buildReason(originalYuan, level, ruleResult, recYuan));
        vo.setEngine(engine);
        vo.setSource(source);
        Map<String, Object> detail = ruleResult.getDetail();
        detail.put("llmSupplement", source == 3);
        vo.setDetail(detail);
        return vo;
    }

    private String buildReason(BigDecimal originalYuan, int level, ValuationResult ruleResult, BigDecimal recYuan) {
        return "原价 " + originalYuan.stripTrailingZeros().toPlainString() + " 元，"
                + CategoryEnum.conditionName(CategoryEnum.toPct(level))
                + "（成色系数约 " + ruleResult.getDetail().get("conditionFactor") + "），"
                + "结合平台同类商品估价区间 "
                + ruleResult.getMin().toPlainString() + "~" + ruleResult.getMax().toPlainString()
                + " 元，建议售价 " + recYuan.stripTrailingZeros().toPlainString() + " 元。";
    }
}
