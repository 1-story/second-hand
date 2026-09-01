package com.hdu.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdu.secondhand.ai.AiService;
import com.hdu.secondhand.ai.rules.ValuationRequest;
import com.hdu.secondhand.ai.rules.ValuationResult;
import com.hdu.secondhand.ai.rules.ValuationRuleEngine;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ProductStatus;
import com.hdu.secondhand.common.ResultCode;
import com.hdu.secondhand.dto.AiDraftRequest;
import com.hdu.secondhand.dto.AiPublishRequest;
import com.hdu.secondhand.entity.AiPublishDraft;
import com.hdu.secondhand.entity.Category;
import com.hdu.secondhand.entity.Product;
import com.hdu.secondhand.entity.ProductImage;
import com.hdu.secondhand.mapper.AiPublishDraftMapper;
import com.hdu.secondhand.mapper.CategoryMapper;
import com.hdu.secondhand.mapper.ProductImageMapper;
import com.hdu.secondhand.mapper.ProductMapper;
import com.hdu.secondhand.service.AiPublishService;
import com.hdu.secondhand.util.MoneyUtil;
import com.hdu.secondhand.vo.AiDraftVO;
import com.hdu.secondhand.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 自动填表发布链路实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPublishServiceImpl implements AiPublishService {

    private final AiService aiService;
    private final CategoryMapper categoryMapper;
    private final AiPublishDraftMapper aiPublishDraftMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ObjectMapper objectMapper;

    @Value("${ai.audit-on-publish:true}")
    private boolean auditOnPublish;

    /** 草稿状态：待确认 */
    private static final int DRAFT_WAIT = 0;
    /** 草稿状态：已发布 */
    private static final int DRAFT_PUBLISHED = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiDraftVO createDraft(AiDraftRequest req, long userId) {
        // ---- 1. 识别 ----
        AiService.RecognizeResult recognized = aiService.recognize(
                req.getImages() == null ? Collections.emptyList() : req.getImages(),
                req.getHint());

        // ---- 2. 分类确认 ----
        Long categoryId = recognized.categoryId == null ? 8L : recognized.categoryId;
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            category = categoryMapper.selectById(8L);
        }
        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND, "系统分类数据缺失，请联系管理员");
        }

        // ---- 3. 成色 ----
        int conditionLevel = recognized.conditionLevel == null ? 7 : recognized.conditionLevel;
        String conditionDesc = recognized.conditionDesc == null ? "正常使用痕迹" : recognized.conditionDesc;

        // ---- 4. 标题/描述 ----
        String title = StringUtils.hasText(recognized.title)
                ? recognized.title
                : (category.getName() + " 好物转让");
        String description = StringUtils.hasText(recognized.description)
                ? recognized.description
                : aiService.generateDescription(title, conditionDesc, "校园二手，诚信交易");

        // ---- 5. 估价（使用分类折旧率与热度） ----
        ValuationRequest valuationRequest = new ValuationRequest(
                category.getBasePrice(),
                conditionLevel,
                12, // 默认按 1 年使用时长估价，用户可在草稿确认时调整
                category.getHeatWeight() == null ? BigDecimal.ONE : category.getHeatWeight());
        ValuationResult valuation = ValuationRuleEngine.estimate(valuationRequest,
                category.getDepreciationRate() == null ? new BigDecimal("0.15") : category.getDepreciationRate());

        // ---- 6. 组装草稿 JSON ----
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("categoryId", category.getId());
        draft.put("categoryName", category.getName());
        draft.put("title", title);
        draft.put("description", description);
        draft.put("conditionLevel", conditionLevel);
        draft.put("conditionDesc", conditionDesc);
        draft.put("images", recognized.images == null ? Collections.emptyList() : recognized.images);
        // 金额统一存「分」
        draft.put("suggestPrice", MoneyUtil.toFen(valuation.getRecommend()));
        draft.put("minPrice", MoneyUtil.toFen(valuation.getMin()));
        draft.put("maxPrice", MoneyUtil.toFen(valuation.getMax()));
        draft.put("expectPrice", req.getExpectPrice());
        draft.put("estimateDetail", valuation.getDetail());
        draft.put("createdAt", LocalDateTime.now().toString());

        // ---- 7. 落库 ----
        AiPublishDraft entity = new AiPublishDraft();
        entity.setUserId(userId);
        entity.setStatus(DRAFT_WAIT);
        entity.setDraftJson(writeJson(draft));
        entity.setCreatedAt(LocalDateTime.now());
        aiPublishDraftMapper.insert(entity);

        // ---- 8. 返回确认 VO ----
        AiDraftVO vo = new AiDraftVO();
        vo.setDraftId(entity.getId());
        vo.setCategoryId(category.getId());
        vo.setCategoryName(category.getName());
        vo.setTitle(title);
        vo.setDescription(description);
        vo.setConditionLevel(conditionLevel);
        vo.setConditionDesc(conditionDesc);
        vo.setSuggestPrice(MoneyUtil.toFen(valuation.getRecommend()));
        vo.setMinPrice(MoneyUtil.toFen(valuation.getMin()));
        vo.setMaxPrice(MoneyUtil.toFen(valuation.getMax()));
        vo.setImages(recognized.images == null ? Collections.emptyList() : recognized.images);
        vo.setStatus(DRAFT_WAIT);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO publish(AiPublishRequest req, long userId) {
        if (req.getDraftId() == null) {
            throw new BizException(ResultCode.AI_DRAFT_NOT_FOUND);
        }
        AiPublishDraft draft = aiPublishDraftMapper.selectById(req.getDraftId());
        if (draft == null) {
            throw new BizException(ResultCode.AI_DRAFT_NOT_FOUND);
        }
        if (!Objects.equals(draft.getUserId(), userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能发布自己的草稿");
        }
        if (draft.getStatus() != DRAFT_WAIT) {
            throw new BizException(ResultCode.AI_DRAFT_STATUS_INVALID, "草稿已处理，不能重复发布");
        }

        // ---- 解析草稿 ----
        JsonNode node = readJson(draft.getDraftJson());
        if (node == null) {
            throw new BizException(ResultCode.AI_DRAFT_STATUS_INVALID, "草稿数据损坏");
        }
        Long categoryId = node.path("categoryId").asLong(8L);
        String title = req.getTitle() != null && !req.getTitle().isBlank()
                ? req.getTitle().trim() : node.path("title").asText("好物转让");
        String description = node.path("description").asText("");
        int conditionLevel = node.path("conditionLevel").asInt(7);
        String conditionDesc = node.path("conditionDesc").asText("");
        List<String> images = readImageList(node);

        // 草稿中金额为「分」
        Long suggestPriceFen = node.path("suggestPrice").asLong(0);
        BigDecimal suggestPrice = MoneyUtil.toYuan(suggestPriceFen);
        BigDecimal price = req.getPrice() != null && req.getPrice() > 0
                ? MoneyUtil.toYuan(req.getPrice()) : suggestPrice;

        // ---- 创建商品 ----
        Product product = new Product();
        product.setSellerId(userId);
        product.setCategoryId(categoryId);
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setEstimatedPrice(suggestPrice);
        product.setConditionLevel(conditionLevel);
        product.setConditionDesc(conditionDesc);
        product.setCoverImage(images.isEmpty() ? null : images.get(0));
        product.setViewCount(0);
        product.setFavoriteCount(0);
        // AI 辅助审核开关：true 进入审核中，false 直接上架
        product.setStatus(auditOnPublish ? ProductStatus.AUDITING : ProductStatus.ON_SALE);
        productMapper.insert(product);

        // ---- 图片 ----
        int order = 0;
        for (String url : images) {
            if (!StringUtils.hasText(url)) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProductId(product.getId());
            image.setUrl(url.trim());
            image.setSortOrder(order++);
            productImageMapper.insert(image);
        }

        // ---- 更新草稿 ----
        draft.setStatus(DRAFT_PUBLISHED);
        draft.setProductId(product.getId());
        draft.setUpdatedAt(LocalDateTime.now());
        aiPublishDraftMapper.updateById(draft);

        // ---- 返回商品 VO ----
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        // 金额字段类型不同（实体元 / VO 分），手动转换
        vo.setPrice(MoneyUtil.toFen(product.getPrice()));
        vo.setEstimatedPrice(MoneyUtil.toFen(product.getEstimatedPrice()));
        vo.setCategoryName(node.path("categoryName").asText(""));
        vo.setImages(images);
        return vo;
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new BizException(ResultCode.SERVER_ERROR, "草稿序列化失败");
        }
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("草稿 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private List<String> readImageList(JsonNode node) {
        List<String> images = new ArrayList<>();
        JsonNode arr = node.path("images");
        if (arr.isArray()) {
            arr.forEach(n -> images.add(n.asText()));
        }
        return images;
    }
}
