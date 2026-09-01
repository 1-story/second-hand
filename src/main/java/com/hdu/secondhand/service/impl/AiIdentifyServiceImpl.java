package com.hdu.secondhand.service.impl;

import com.hdu.secondhand.ai.AiService;
import com.hdu.secondhand.ai.CategoryEnum;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ResultCode;
import com.hdu.secondhand.dto.AiDescribeRequest;
import com.hdu.secondhand.dto.AiIdentifyRequest;
import com.hdu.secondhand.service.AiIdentifyService;
import com.hdu.secondhand.vo.AiDescribeVO;
import com.hdu.secondhand.vo.AiIdentifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 识别与描述服务实现
 *
 * <p>Mock 模式（ai.enabled=false）下识别基于关键词提示（hint），图片仅做 base64 格式校验；
 * 大模型就绪后由 {@link AiService} 真实实现接管图片识别。</p>
 */
@Service
@RequiredArgsConstructor
public class AiIdentifyServiceImpl implements AiIdentifyService {

    private final AiService aiService;

    @Value("${ai.enabled:false}")
    private boolean aiEnabled;

    @Override
    public AiIdentifyVO identify(AiIdentifyRequest req, long userId) {
        validateImage(req.getImageBase64());

        AiService.RecognizeResult recognized = aiService.recognize(
                Collections.emptyList(), req.getHint());

        Long categoryId = recognized.categoryId == null ? 8L : recognized.categoryId;
        String key = CategoryEnum.toKey(categoryId);
        Integer conditionPct = CategoryEnum.toPct(recognized.conditionLevel == null ? 7 : recognized.conditionLevel);

        AiIdentifyVO vo = new AiIdentifyVO();
        vo.setCategory(key);
        vo.setCategoryName(CategoryEnum.nameOf(key));
        vo.setCondition(conditionPct);
        vo.setConditionName(CategoryEnum.conditionName(conditionPct));
        // Mock 识别置信度；大模型接入后由模型返回
        vo.setConfidence(aiEnabled ? 0.96 : 0.85);
        vo.setEngine(aiEnabled ? "llm" : "rule");
        return vo;
    }

    @Override
    public AiDescribeVO describe(AiDescribeRequest req, long userId) {
        validateImage(req.getImageBase64());

        String key = StringUtils.hasText(req.getCategory()) ? req.getCategory().trim().toLowerCase() : "other";
        String categoryName = CategoryEnum.nameOf(key);
        String conditionDesc = CategoryEnum.conditionName(req.getCondition());

        String title = buildTitle(categoryName, req.getKeywords());
        String description = aiService.generateDescription(title, conditionDesc, "AI 自动生成，可自行修改补充。");

        AiDescribeVO vo = new AiDescribeVO();
        vo.setDescription(description);
        vo.setEngine(aiEnabled ? "llm" : "rule");
        return vo;
    }

    private String buildTitle(String categoryName, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return categoryName + " 好物转让";
        }
        String kws = keywords.stream().filter(StringUtils::hasText)
                .limit(3).collect(Collectors.joining(" "));
        return (categoryName + " " + kws).trim();
    }

    /** 图片 base64 格式校验（含 data:image/ 前缀；Mock 模式不解析图片内容） */
    private void validateImage(String imageBase64) {
        if (!StringUtils.hasText(imageBase64) || !imageBase64.startsWith("data:image/")) {
            throw new BizException(ResultCode.IMAGE_PARSE_FAILED, "图片必须为含 data:image/...;base64, 前缀的 base64 字符串");
        }
    }
}
