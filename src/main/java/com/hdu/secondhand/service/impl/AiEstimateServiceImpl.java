package com.hdu.secondhand.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdu.secondhand.ai.AiService;
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
import com.hdu.secondhand.service.AiEstimateService;
import com.hdu.secondhand.vo.AiEstimateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 估价服务实现：规则引擎为核心，大模型可选补充
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEstimateServiceImpl implements AiEstimateService {

    private final CategoryMapper categoryMapper;
    private final AiEstimateLogMapper aiEstimateLogMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Value("${ai.enabled:false}")
    private boolean aiEnabled;

    @Override
    public AiEstimateVO estimate(AiEstimateRequest req, long userId) {
        if (req.getCategoryId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "分类不能为空");
        }
        Category category = categoryMapper.selectById(req.getCategoryId());
        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // ---- 组装规则引擎参数 ----
        int conditionLevel = req.getConditionLevel() == null ? 7 : req.getConditionLevel();
        int ageMonths = req.getAgeMonths() == null ? 12 : req.getAgeMonths();
        BigDecimal heat = category.getHeatWeight() == null ? BigDecimal.ONE : category.getHeatWeight();
        BigDecimal depRate = category.getDepreciationRate() == null
                ? new BigDecimal("0.15") : category.getDepreciationRate();

        ValuationRequest request = new ValuationRequest(
                category.getBasePrice(), conditionLevel, ageMonths, heat);
        ValuationResult result = ValuationRuleEngine.estimate(request, depRate);

        // ---- 大模型可选补充 ----
        int source = 1;
        BigDecimal rec = result.getRecommend();
        if (Boolean.TRUE.equals(req.getUseLlm()) && aiEnabled) {
            try {
                BigDecimal llm = aiService.llmEstimate(category.getName(), req.getDescription(), rec);
                if (llm != null && llm.compareTo(BigDecimal.ZERO) > 0) {
                    // 大模型结果与规则结果加权平均（大模型权重 0.4）
                    rec = rec.multiply(new BigDecimal("0.6"))
                            .add(llm.multiply(new BigDecimal("0.4")))
                            .setScale(0, java.math.RoundingMode.HALF_UP);
                    source = 3;
                }
            } catch (Exception e) {
                log.warn("大模型补充估价失败，使用规则结果: {}", e.getMessage());
            }
        }

        // ---- 落库审计 ----
        AiEstimateLog logRecord = new AiEstimateLog();
        logRecord.setUserId(userId);
        logRecord.setCategoryId(category.getId());
        logRecord.setInputDesc(req.getDescription());
        logRecord.setBasePrice(category.getBasePrice());
        logRecord.setConditionScore(BigDecimal.valueOf(conditionLevel));
        logRecord.setAgeMonths(ageMonths);
        logRecord.setHeatFactor(heat);
        logRecord.setEstimatedMin(result.getMin());
        logRecord.setEstimatedRec(rec);
        logRecord.setEstimatedMax(result.getMax());
        logRecord.setSource(source);
        try {
            logRecord.setDetailJson(objectMapper.writeValueAsString(result.getDetail()));
        } catch (Exception e) {
            logRecord.setDetailJson("{}");
        }
        logRecord.setCreatedAt(LocalDateTime.now());
        aiEstimateLogMapper.insert(logRecord);

        // ---- 返回 ----
        AiEstimateVO vo = new AiEstimateVO();
        vo.setMin(result.getMin());
        vo.setRecommend(rec);
        vo.setMax(result.getMax());
        Map<String, Object> detail = result.getDetail();
        detail.put("llmSupplement", source == 3);
        vo.setDetail(detail);
        vo.setSource(source);
        return vo;
    }
}
