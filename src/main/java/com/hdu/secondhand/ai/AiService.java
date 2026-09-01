package com.hdu.secondhand.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AiService 统一封装（契约定义）
 *
 * <p>说明：本接口由后端商品/AI 模块（田博）定义并消费；
 * 大模型 API 对接由陈思瀚负责，就绪后提供真实的 HTTP 实现即可无感切换。
 * 当前提供 {@link MockAiService}（离线可用）与 {@link HttpAiService}（骨架）。</p>
 *
 * <p>降级约定（规范 v1.1 第 6 节）：各方法在超时/异常/未启用时返回 {@code null}，
 * 由调用方（接口层）组装降级文案；AiService 只负责调用底层模型。</p>
 */
public interface AiService {

    /**
     * 图片/文本识别：提取商品基本信息（分类、成色、标题、描述）
     *
     * @param images 图片 URL 列表（可为空）
     * @param hint   用户提示/关键词
     * @return 识别结果（可能为空对象，业务层兜底）
     */
    RecognizeResult recognize(List<String> images, String hint);

    /**
     * 生成商品描述文案
     *
     * @param title         标题
     * @param conditionDesc 成色描述
     * @param extra         附加信息（如使用时长、入手渠道）
     * @return 描述文案
     */
    String generateDescription(String title, String conditionDesc, String extra);

    /**
     * 大模型补充估价（可选）：返回 null 表示不采用大模型估价
     *
     * @param categoryName 分类名称
     * @param description  商品描述
     * @param rulePrice    规则引擎估价（作为提示）
     * @return 大模型估价（元）；不可用时返回 null
     */
    BigDecimal llmEstimate(String categoryName, String description, BigDecimal rulePrice);

    /**
     * AI 智能问答（对齐《接口约定规范 v1.1》6.4）
     *
     * @param productId 商品 ID（可空，用于商品上下文）
     * @param question  用户问题
     * @param history   多轮对话历史 [{role, content}]（可空；role: user/assistant）
     * @return 大模型回答文本；超时/异常/未启用（mock）时返回 null，
     *         由 /api/ai/chat 接口层（林天楚）组装 { answer, fallback, suggestManual }
     */
    String chat(Long productId, String question, List<Map<String, String>> history);

    /**
     * 识别结果
     */
    final class RecognizeResult {
        /** 命中分类 ID（null 表示未识别，由业务层兜底） */
        public Long categoryId;
        /** 命中分类名称 */
        public String categoryName;
        /** 自动生成标题 */
        public String title;
        /** 自动生成描述 */
        public String description;
        /** 成色等级 1~10 */
        public Integer conditionLevel;
        /** 成色文字描述 */
        public String conditionDesc;
        /** 识别到的图片（原样透传） */
        public List<String> images;

        public static RecognizeResult empty() {
            return new RecognizeResult();
        }
    }
}
