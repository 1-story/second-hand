package com.hdu.secondhand.ai;

import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AiService 真实 HTTP 实现（骨架，待大模型 API 就绪）
 *
 * <p>对接说明：
 * <ul>
 *   <li>配置项见 application.yml 的 ai.llm.*（base-url / api-key / model / timeout-ms）</li>
 *   <li>由陈思瀚负责大模型 API 接入后，将本类的 prompt 与响应解析替换为真实协议即可</li>
 *   <li>未启用（ai.enabled=false）或调用失败时返回兜底结果，不影响主流程</li>
 * </ul></p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.mock", havingValue = "false")
public class HttpAiService implements AiService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Value("${ai.enabled:false}")
    private boolean enabled;

    @Value("${ai.llm.base-url:}")
    private String baseUrl;

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.model:gpt-4o-mini}")
    private String model;

    /** 读取超时（规范 v1.0 第 7 节：默认 5 秒；chat 可放宽至 10 秒） */
    @Value("${ai.llm.read-timeout-ms:5000}")
    private long readTimeoutMs;

    @Override
    public RecognizeResult recognize(List<String> images, String hint) {
        if (!enabled) {
            return RecognizeResult.empty();
        }
        // TODO 大模型视觉识别：POST {baseUrl}/chat/completions，携带图片 URL 与提示词
        // 响应解析为 RecognizeResult 后返回
        log.debug("HttpAiService.recognize 尚未接入真实模型，images={}, hint={}", images, hint);
        return RecognizeResult.empty();
    }

    @Override
    public String generateDescription(String title, String conditionDesc, String extra) {
        if (!enabled) {
            return null;
        }
        String prompt = "请为二手商品生成一段简洁友好的描述文案，标题：" + title
                + "，成色：" + conditionDesc + "，附加信息：" + (extra == null ? "" : extra);
        return chat(prompt);
    }

    @Override
    public BigDecimal llmEstimate(String categoryName, String description, BigDecimal rulePrice) {
        if (!enabled) {
            return null;
        }
        String prompt = "你是校园二手交易平台的估价助手。请仅返回一个数字（人民币整数元），"
                + "商品分类：" + categoryName + "，描述：" + description
                + "，规则引擎参考价：" + rulePrice + " 元。";
        String text = chat(prompt);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            log.warn("大模型估价响应解析失败: {}", text);
            return null;
        }
    }

    @Override
    public String chat(Long productId, String question, List<Map<String, String>> history) {
        if (!enabled) {
            return null;
        }
        String messagesJson = buildMessagesJson(productId, question, history);
        return chatMessages(messagesJson);
    }

    /** 调用大模型对话接口（OpenAI 兼容协议骨架） */
    private String chat(String prompt) {
        return chatMessages("[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]");
    }

    /** 调用大模型对话接口（messages 数组版） */
    private String chatMessages(String messagesJson) {
        if (baseUrl.isBlank() || apiKey.isBlank() || "sk-xxxx".equals(apiKey)) {
            throw new BizException(ResultCode.AI_LLM_NOT_CONFIGURED, "大模型服务未配置：请设置 ai.llm.base-url 与 api-key");
        }
        String body = "{\"model\":\"" + model + "\",\"messages\":" + messagesJson + ",\"temperature\":0.3}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofMillis(readTimeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("大模型接口返回非 200: {} {}", response.statusCode(), response.body());
                return null;
            }
            return extractContent(response.body());
        } catch (Exception e) {
            log.warn("大模型接口调用失败: {}", e.getMessage());
            return null;
        }
    }

    /** 构造多轮对话 messages JSON：system 上下文 + history + 当前问题 */
    private String buildMessagesJson(Long productId, String question, List<Map<String, String>> history) {
        StringBuilder sb = new StringBuilder("[");
        // system 角色：平台助手设定（可带商品上下文）
        String system = "你是校园二手交易平台的智能助手，回答简洁友好，可引导用户私信卖家协商。";
        if (productId != null) {
            system += "用户正在查看商品ID " + productId + " 的详情页。";
        }
        sb.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(system)).append("\"}");

        if (history != null) {
            for (Map<String, String> m : history) {
                String role = m == null ? null : m.get("role");
                String content = m == null ? null : m.get("content");
                if (role == null || content == null || content.isBlank()) {
                    continue;
                }
                sb.append(",{\"role\":\"").append(escapeJson(role))
                        .append("\",\"content\":\"").append(escapeJson(content)).append("\"}");
            }
        }
        if (question != null && !question.isBlank()) {
            sb.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(question)).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    /** 从 OpenAI 兼容响应中提取 content 字段（骨架解析，接入时按真实响应调整） */
    private String extractContent(String responseBody) {
        int idx = responseBody.indexOf("\"content\":\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + "\"content\":\"".length();
        int end = responseBody.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return responseBody.substring(start, end).replace("\\n", "\n");
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
