package com.hdu.secondhand.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AiService Mock 实现（离线可用）
 *
 * <p>默认生效（ai.mock=true，规范 v1.1 第 6 节：联调与答辩演示，零网络依赖）：
 * 基于关键词规则模拟大模型能力（分类识别、成色识别、标题描述生成）。
 * 真实模型接入：ai.mock=false + ai.enabled=true，此时使用 {@link HttpAiService}。</p>
 */
@Component
@ConditionalOnProperty(name = "ai.mock", havingValue = "true", matchIfMissing = true)
public class MockAiService implements AiService {

    @Override
    public RecognizeResult recognize(List<String> images, String hint) {
        RecognizeResult result = RecognizeResult.empty();
        result.images = images == null ? List.of() : images;
        String text = hint == null ? "" : hint.trim();

        // ---- 分类识别 ----
        if (matchAny(text, "手机", "iphone", "华为", "小米", "oppo", "vivo", "荣耀", "手机")) {
            result.categoryId = 11L;
            result.categoryName = "手机";
        } else if (matchAny(text, "笔记本", "电脑", "macbook", "thinkpad", "拯救者", "轻薄本")) {
            result.categoryId = 12L;
            result.categoryName = "笔记本电脑";
        } else if (matchAny(text, "平板", "ipad", "matepad", "pad")) {
            result.categoryId = 13L;
            result.categoryName = "平板";
        } else if (matchAny(text, "耳机", "音响", "音箱", "airpods", "蓝牙")) {
            result.categoryId = 14L;
            result.categoryName = "耳机/音响";
        } else if (matchAny(text, "教材", "课本", "书", "教程", "教材书")) {
            result.categoryId = 21L;
            result.categoryName = "专业课教材";
        } else if (matchAny(text, "考研", "考证", "四六级", "六级", "四级", "复习")) {
            result.categoryId = 22L;
            result.categoryName = "考研/考证";
        } else if (matchAny(text, "台灯", "风扇", "小家电", "电饭煲", "吹风")) {
            result.categoryId = 31L;
            result.categoryName = "宿舍小家电";
        } else if (matchAny(text, "收纳", "书架", "家具", "椅子", "桌子")) {
            result.categoryId = 32L;
            result.categoryName = "家具收纳";
        } else {
            result.categoryId = 8L;
            result.categoryName = "其他";
        }

        // ---- 成色识别 ----
        int conditionLevel = 7;
        String conditionDesc = "七成新，正常使用痕迹";
        if (matchAny(text, "全新", "未拆", "未使用")) {
            conditionLevel = 10;
            conditionDesc = "全新未拆封";
        } else if (matchAny(text, "九成新", "9成新")) {
            conditionLevel = 9;
            conditionDesc = "九成新，几乎无使用痕迹";
        } else if (matchAny(text, "八成新", "8成新")) {
            conditionLevel = 8;
            conditionDesc = "八成新，轻微使用痕迹";
        } else if (matchAny(text, "六成新", "6成新")) {
            conditionLevel = 6;
            conditionDesc = "六成新，明显使用痕迹";
        } else if (matchAny(text, "划痕", "破损", "很旧", "老化")) {
            conditionLevel = 5;
            conditionDesc = "五成新，有可见划痕/瑕疵";
        }
        result.conditionLevel = conditionLevel;
        result.conditionDesc = conditionDesc;

        // ---- 标题/描述生成 ----
        String keyword = extractKeyword(text, result.categoryName);
        result.title = (result.categoryName + " " + keyword).trim();
        result.description = generateDescription(result.title, conditionDesc,
                "购于校内二手渠道，功能正常。" + (text.isBlank() ? "" : "备注：" + text));

        return result;
    }

    @Override
    public String generateDescription(String title, String conditionDesc, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(title).append("】\n");
        if (conditionDesc != null && !conditionDesc.isBlank()) {
            sb.append("成色：").append(conditionDesc).append("\n");
        }
        if (extra != null && !extra.isBlank()) {
            sb.append("说明：").append(extra).append("\n");
        }
        sb.append("支持校内面交，可小刀，有意私聊~");
        return sb.toString();
    }

    @Override
    public BigDecimal llmEstimate(String categoryName, String description, BigDecimal rulePrice) {
        // Mock 模式不使用大模型估价，返回 null 由规则引擎结果兜底
        return null;
    }

    @Override
    public String chat(Long productId, String question, List<Map<String, String>> history) {
        // Mock 模式不模拟问答，返回 null：由 /api/ai/chat 接口层（林天楚）组装降级文案
        return null;
    }

    // ==================== 工具方法 ====================

    private static boolean matchAny(String text, String... keywords) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** 从提示中提取核心关键词（去掉分类词后的剩余部分截断 20 字） */
    private static String extractKeyword(String text, String categoryName) {
        String kw = text;
        if (categoryName != null && !categoryName.isBlank()) {
            kw = kw.replace(categoryName, "");
        }
        kw = kw.replaceAll("[" + "全新未拆九成八成七成六成五成新划痕破损老化旧，。,.\\s" + "]", " ").trim();
        if (kw.isBlank()) {
            return "好物转让";
        }
        return kw.length() > 20 ? kw.substring(0, 20) : kw;
    }
}
