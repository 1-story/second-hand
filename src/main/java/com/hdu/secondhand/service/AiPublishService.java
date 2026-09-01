package com.hdu.secondhand.service;

import com.hdu.secondhand.dto.AiDraftRequest;
import com.hdu.secondhand.dto.AiPublishRequest;
import com.hdu.secondhand.vo.AiDraftVO;
import com.hdu.secondhand.vo.ProductVO;

/**
 * AI 自动填表发布链路：
 * 图片/文本识别 → 描述生成 → 规则估价 → 草稿（待确认）→ 一键发布
 */
public interface AiPublishService {

    /**
     * 生成 AI 自动填表草稿（状态：待确认）
     */
    AiDraftVO createDraft(AiDraftRequest req, long userId);

    /**
     * 一键发布：确认草稿并生成商品
     */
    ProductVO publish(AiPublishRequest req, long userId);
}
