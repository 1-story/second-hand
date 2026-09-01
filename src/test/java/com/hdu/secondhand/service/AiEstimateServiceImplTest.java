package com.hdu.secondhand.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdu.secondhand.ai.AiService;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.dto.AiEstimateRequest;
import com.hdu.secondhand.entity.AiEstimateLog;
import com.hdu.secondhand.entity.Category;
import com.hdu.secondhand.mapper.AiEstimateLogMapper;
import com.hdu.secondhand.mapper.CategoryMapper;
import com.hdu.secondhand.service.impl.AiEstimateServiceImpl;
import com.hdu.secondhand.vo.AiEstimateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 估价服务单元测试（规则引擎编排 + 落库审计）
 */
@ExtendWith(MockitoExtension.class)
class AiEstimateServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private AiEstimateLogMapper aiEstimateLogMapper;
    @Mock
    private AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiEstimateServiceImpl aiEstimateService;

    private Category phoneCategory;

    @BeforeEach
    void setUp() {
        aiEstimateService = new AiEstimateServiceImpl(
                categoryMapper, aiEstimateLogMapper, aiService, objectMapper);
        // ai.enabled 默认 false（与生产默认一致），useLlm 时走不到大模型

        phoneCategory = new Category();
        phoneCategory.setId(11L);
        phoneCategory.setName("手机");
        phoneCategory.setBasePrice(new BigDecimal("3000"));
        phoneCategory.setDepreciationRate(new BigDecimal("0.18"));
        phoneCategory.setHeatWeight(new BigDecimal("1.3"));
    }

    @Test
    @DisplayName("估价：规则引擎结果 + 落库审计")
    void estimate_basic() {
        when(categoryMapper.selectById(11L)).thenReturn(phoneCategory);
        when(aiEstimateLogMapper.insert(any(AiEstimateLog.class))).thenReturn(1);

        AiEstimateRequest req = new AiEstimateRequest();
        req.setCategoryId(11L);
        req.setConditionLevel(9);
        req.setAgeMonths(12);

        AiEstimateVO vo = aiEstimateService.estimate(req, 1L);

        assertNotNull(vo);
        // 3000 * 0.88 * (0.82)^1 * 1.3 = 3000*0.88*0.82*1.3 = 2814.24 → 2814
        assertEquals(new BigDecimal("2814"), vo.getRecommend());
        assertTrue(vo.getMin().compareTo(vo.getRecommend()) <= 0);
        assertTrue(vo.getRecommend().compareTo(vo.getMax()) <= 0);
        assertEquals(1, vo.getSource()); // 纯规则
        assertNotNull(vo.getDetail());
        // 落库审计
        verify(aiEstimateLogMapper).insert(any(AiEstimateLog.class));
    }

    @Test
    @DisplayName("估价：分类不存在报错且不落库")
    void estimate_categoryNotFound() {
        when(categoryMapper.selectById(99L)).thenReturn(null);
        AiEstimateRequest req = new AiEstimateRequest();
        req.setCategoryId(99L);
        assertThrows(BizException.class, () -> aiEstimateService.estimate(req, 1L));
        verify(aiEstimateLogMapper, never()).insert(any(AiEstimateLog.class));
    }

    @Test
    @DisplayName("估价：分类为空报参数错误")
    void estimate_missingCategory() {
        AiEstimateRequest req = new AiEstimateRequest();
        assertThrows(BizException.class, () -> aiEstimateService.estimate(req, 1L));
    }

    @Test
    @DisplayName("估价：成色越界抛参数异常（规则引擎校验）")
    void estimate_invalidCondition() {
        when(categoryMapper.selectById(11L)).thenReturn(phoneCategory);
        AiEstimateRequest req = new AiEstimateRequest();
        req.setCategoryId(11L);
        req.setConditionLevel(15);
        assertThrows(IllegalArgumentException.class, () -> aiEstimateService.estimate(req, 1L));
    }

    @Test
    @DisplayName("估价：useLlm=true 但 ai.enabled=false 时仍用规则结果（不调用大模型）")
    void estimate_llmDisabled() {
        when(categoryMapper.selectById(11L)).thenReturn(phoneCategory);
        when(aiEstimateLogMapper.insert(any(AiEstimateLog.class))).thenReturn(1);

        AiEstimateRequest req = new AiEstimateRequest();
        req.setCategoryId(11L);
        req.setUseLlm(true);

        AiEstimateVO vo = aiEstimateService.estimate(req, 1L);
        assertEquals(1, vo.getSource());
        verify(aiService, never()).llmEstimate(any(), any(), any());
    }
}
