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
 * AI 智能估价服务单元测试（对齐规范 6.3：原价×成色，金额分，engine 降级）
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

    private Category digitalCategory;

    @BeforeEach
    void setUp() {
        aiEstimateService = new AiEstimateServiceImpl(
                categoryMapper, aiEstimateLogMapper, aiService, objectMapper);
        // ai.enabled 默认 false（与生产默认一致）

        digitalCategory = new Category();
        digitalCategory.setId(1L);
        digitalCategory.setName("数码电子");
        digitalCategory.setBasePrice(new BigDecimal("3000"));
        digitalCategory.setDepreciationRate(new BigDecimal("0.18"));
        digitalCategory.setHeatWeight(new BigDecimal("1.3"));
    }

    @Test
    @DisplayName("估价：原价3000元、digital、九成新 → 分单位结果 + 落库")
    void estimate_basic() {
        // digital key → 分类 ID 1
        when(categoryMapper.selectById(1L)).thenReturn(digitalCategory);
        when(aiEstimateLogMapper.insert(any(AiEstimateLog.class))).thenReturn(1);

        AiEstimateRequest req = new AiEstimateRequest();
        req.setOriginalPrice(300000L); // 3000元
        req.setCategory("digital");
        req.setCondition(90);

        AiEstimateVO vo = aiEstimateService.estimate(req, 1L);

        assertNotNull(vo);
        // 3000 * 0.88(九成新) * 0.82(年折旧18%) * 1.3(热度) = 2814.24 → 2814元 = 281400分
        assertEquals(281400L, vo.getSuggestPrice());
        assertEquals(253300L, vo.getPriceRange().getMin());  // 2814*0.9=2532.6 → 2533元
        assertEquals(309500L, vo.getPriceRange().getMax());  // 2814*1.1=3095.4 → 3095元
        assertEquals("rule", vo.getEngine()); // ai.enabled=false → 规则兜底
        assertEquals(1, vo.getSource());
        assertNotNull(vo.getReason());
        assertNotNull(vo.getDetail());
        // 落库审计
        verify(aiEstimateLogMapper).insert(any(AiEstimateLog.class));
    }

    @Test
    @DisplayName("估价：分类不存在报错且不落库")
    void estimate_categoryNotFound() {
        when(categoryMapper.selectById(8L)).thenReturn(null); // other 映射 ID 8
        AiEstimateRequest req = new AiEstimateRequest();
        req.setOriginalPrice(10000L);
        req.setCategory("other");
        assertThrows(BizException.class, () -> aiEstimateService.estimate(req, 1L));
        verify(aiEstimateLogMapper, never()).insert(any(AiEstimateLog.class));
    }

    @Test
    @DisplayName("估价：原价必须大于 0")
    void estimate_invalidPrice() {
        AiEstimateRequest req = new AiEstimateRequest();
        req.setOriginalPrice(0L);
        req.setCategory("digital");
        assertThrows(BizException.class, () -> aiEstimateService.estimate(req, 1L));

        req.setOriginalPrice(null);
        assertThrows(BizException.class, () -> aiEstimateService.estimate(req, 1L));
    }

    @Test
    @DisplayName("估价：ai.enabled=false 时不调用大模型（engine=rule）")
    void estimate_llmDisabled() {
        when(categoryMapper.selectById(1L)).thenReturn(digitalCategory);
        when(aiEstimateLogMapper.insert(any(AiEstimateLog.class))).thenReturn(1);

        AiEstimateRequest req = new AiEstimateRequest();
        req.setOriginalPrice(50000L);
        req.setCategory("digital");
        req.setCondition(100);

        AiEstimateVO vo = aiEstimateService.estimate(req, 1L);
        assertEquals("rule", vo.getEngine());
        verify(aiService, never()).llmEstimate(any(), any(), any());
    }

    @Test
    @DisplayName("估价：成色越高估价越高（100 全新 > 70 七成新）")
    void estimate_conditionEffect() {
        when(categoryMapper.selectById(1L)).thenReturn(digitalCategory);
        when(aiEstimateLogMapper.insert(any(AiEstimateLog.class))).thenReturn(1);

        AiEstimateRequest fresh = new AiEstimateRequest();
        fresh.setOriginalPrice(100000L);
        fresh.setCategory("digital");
        fresh.setCondition(100);

        AiEstimateRequest old = new AiEstimateRequest();
        old.setOriginalPrice(100000L);
        old.setCategory("digital");
        old.setCondition(70);

        AiEstimateVO r1 = aiEstimateService.estimate(fresh, 1L);
        AiEstimateVO r2 = aiEstimateService.estimate(old, 1L);
        assertTrue(r1.getSuggestPrice() > r2.getSuggestPrice());
    }
}
