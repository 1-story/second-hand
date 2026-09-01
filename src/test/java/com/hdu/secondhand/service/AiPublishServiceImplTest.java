package com.hdu.secondhand.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdu.secondhand.ai.AiService;
import com.hdu.secondhand.ai.MockAiService;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ProductStatus;
import com.hdu.secondhand.dto.AiDraftRequest;
import com.hdu.secondhand.dto.AiPublishRequest;
import com.hdu.secondhand.entity.AiPublishDraft;
import com.hdu.secondhand.entity.Category;
import com.hdu.secondhand.entity.Product;
import com.hdu.secondhand.mapper.AiPublishDraftMapper;
import com.hdu.secondhand.mapper.CategoryMapper;
import com.hdu.secondhand.mapper.ProductImageMapper;
import com.hdu.secondhand.mapper.ProductMapper;
import com.hdu.secondhand.service.impl.AiPublishServiceImpl;
import com.hdu.secondhand.vo.AiDraftVO;
import com.hdu.secondhand.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 自动填表发布链路单元测试
 */
@ExtendWith(MockitoExtension.class)
class AiPublishServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private AiPublishDraftMapper aiPublishDraftMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductImageMapper productImageMapper;

    private final AiService mockAiService = new MockAiService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiPublishServiceImpl aiPublishService;

    private Category phoneCategory;

    @BeforeEach
    void setUp() throws Exception {
        aiPublishService = new AiPublishServiceImpl(
                mockAiService, categoryMapper, aiPublishDraftMapper,
                productMapper, productImageMapper, objectMapper);
        // 关闭 AI 辅助审核，直接上架，便于断言（原生反射设置 @Value 字段）
        Field auditField = AiPublishServiceImpl.class.getDeclaredField("auditOnPublish");
        auditField.setAccessible(true);
        auditField.setBoolean(aiPublishService, false);

        phoneCategory = new Category();
        phoneCategory.setId(11L);
        phoneCategory.setName("手机");
        phoneCategory.setBasePrice(new BigDecimal("3000"));
        phoneCategory.setDepreciationRate(new BigDecimal("0.18"));
        phoneCategory.setHeatWeight(new BigDecimal("1.3"));
    }

    @Test
    @DisplayName("自动填表：识别手机关键词生成草稿")
    void createDraft_recognizePhone() {
        when(categoryMapper.selectById(11L)).thenReturn(phoneCategory);
        // 模拟 MyBatis-Plus 自增主键回填
        when(aiPublishDraftMapper.insert(any(AiPublishDraft.class))).thenAnswer(inv -> {
            inv.getArgument(0, AiPublishDraft.class).setId(1L);
            return 1;
        });

        AiDraftRequest req = new AiDraftRequest();
        req.setHint("iPhone 13 用了两年 九成新 屏幕有贴膜");
        req.setImages(List.of("http://img/1.jpg"));

        AiDraftVO vo = aiPublishService.createDraft(req, 1L);

        assertNotNull(vo.getDraftId());
        assertEquals(11L, vo.getCategoryId());
        assertEquals("手机", vo.getCategoryName());
        assertEquals(9, vo.getConditionLevel());
        assertNotNull(vo.getTitle());
        // 估价应为正数且在合理区间（3000 * 0.88 * 年限 * 1.3 附近）
        assertNotNull(vo.getSuggestPrice());
        assertTrue(vo.getSuggestPrice() > 0);
        verify(aiPublishDraftMapper).insert(any(AiPublishDraft.class));
    }

    @Test
    @DisplayName("自动填表：未识别关键词兜底到其他分类")
    void createDraft_fallbackCategory() {
        Category other = new Category();
        other.setId(8L);
        other.setName("其他");
        other.setBasePrice(new BigDecimal("100"));
        other.setDepreciationRate(new BigDecimal("0.2"));
        other.setHeatWeight(new BigDecimal("0.8"));
        when(categoryMapper.selectById(8L)).thenReturn(other);
        when(aiPublishDraftMapper.insert(any(AiPublishDraft.class))).thenAnswer(inv -> {
            inv.getArgument(0, AiPublishDraft.class).setId(2L);
            return 1;
        });

        AiDraftRequest req = new AiDraftRequest();
        req.setHint("不知道是什么的东西");
        AiDraftVO vo = aiPublishService.createDraft(req, 1L);

        assertEquals(8L, vo.getCategoryId());
        assertEquals(7, vo.getConditionLevel());
    }

    @Test
    @DisplayName("一键发布：确认草稿生成在售商品")
    void publish_ok() {
        // 构造已存在的草稿
        AiPublishDraft draft = new AiPublishDraft();
        draft.setId(100L);
        draft.setUserId(1L);
        draft.setStatus(0);
        draft.setDraftJson("{\"categoryId\":11,\"categoryName\":\"手机\",\"title\":\"iPhone 13 九成新\","
                + "\"description\":\"描述\",\"conditionLevel\":9,\"conditionDesc\":\"九成新\","
                + "\"images\":[\"http://img/1.jpg\"],\"suggestPrice\":2000}");
        when(aiPublishDraftMapper.selectById(100L)).thenReturn(draft);
        // 模拟主键回填
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            inv.getArgument(0, Product.class).setId(500L);
            return 1;
        });
        doReturn(1).when(aiPublishDraftMapper).updateById(any(AiPublishDraft.class));

        AiPublishRequest req = new AiPublishRequest();
        req.setDraftId(100L);

        ProductVO vo = aiPublishService.publish(req, 1L);

        assertNotNull(vo.getId());
        assertEquals(ProductStatus.ON_SALE, vo.getStatus());
        // 草稿 suggestPrice=2000 分（20元），发布后 VO 输出分
        assertEquals(2000L, vo.getPrice());
        assertEquals(2000L, vo.getEstimatedPrice());
        assertEquals(1, vo.getImages().size());
        // 草稿状态更新为已发布
        verify(aiPublishDraftMapper).updateById(any(AiPublishDraft.class));
    }

    @Test
    @DisplayName("一键发布：可覆盖标题与价格")
    void publish_override() {
        AiPublishDraft draft = new AiPublishDraft();
        draft.setId(101L);
        draft.setUserId(1L);
        draft.setStatus(0);
        draft.setDraftJson("{\"categoryId\":11,\"categoryName\":\"手机\",\"title\":\"旧标题\","
                + "\"description\":\"描述\",\"conditionLevel\":8,\"images\":[],\"suggestPrice\":1500}");
        when(aiPublishDraftMapper.selectById(101L)).thenReturn(draft);
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            inv.getArgument(0, Product.class).setId(501L);
            return 1;
        });
        doReturn(1).when(aiPublishDraftMapper).updateById(any(AiPublishDraft.class));

        AiPublishRequest req = new AiPublishRequest();
        req.setDraftId(101L);
        req.setTitle("新标题");
        req.setPrice(180000L); // 1800元

        ProductVO vo = aiPublishService.publish(req, 1L);
        assertEquals("新标题", vo.getTitle());
        assertEquals(180000L, vo.getPrice());
    }

    @Test
    @DisplayName("一键发布：他人草稿不可发布")
    void publish_forbidden() {
        AiPublishDraft draft = new AiPublishDraft();
        draft.setId(102L);
        draft.setUserId(1L);
        draft.setStatus(0);
        draft.setDraftJson("{}");
        when(aiPublishDraftMapper.selectById(102L)).thenReturn(draft);

        AiPublishRequest req = new AiPublishRequest();
        req.setDraftId(102L);
        assertThrows(BizException.class, () -> aiPublishService.publish(req, 999L));
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    @DisplayName("一键发布：草稿不存在或已处理不可重复发布")
    void publish_invalidDraft() {
        when(aiPublishDraftMapper.selectById(999L)).thenReturn(null);
        AiPublishRequest req = new AiPublishRequest();
        req.setDraftId(999L);
        assertThrows(BizException.class, () -> aiPublishService.publish(req, 1L));

        AiPublishDraft draft = new AiPublishDraft();
        draft.setId(103L);
        draft.setUserId(1L);
        draft.setStatus(1); // 已发布
        draft.setDraftJson("{}");
        when(aiPublishDraftMapper.selectById(103L)).thenReturn(draft);
        req.setDraftId(103L);
        assertThrows(BizException.class, () -> aiPublishService.publish(req, 1L));
    }

    @Test
    @DisplayName("一键发布：缺少草稿 ID 报错")
    void publish_missingDraftId() {
        AiPublishRequest req = new AiPublishRequest();
        assertThrows(BizException.class, () -> aiPublishService.publish(req, 1L));
    }
}
