package com.hdu.secondhand.controller;

import com.hdu.secondhand.common.Result;
import com.hdu.secondhand.dto.AiDraftRequest;
import com.hdu.secondhand.dto.AiPublishRequest;
import com.hdu.secondhand.service.AiPublishService;
import com.hdu.secondhand.util.UserContext;
import com.hdu.secondhand.vo.AiDraftVO;
import com.hdu.secondhand.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 自动填表发布接口（田博）
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiPublishController {

    private final AiPublishService aiPublishService;

    /** AI 自动填表：图片/关键词 → 识别 → 估价 → 生成草稿（待确认） */
    @PostMapping("/draft")
    public Result<AiDraftVO> createDraft(@RequestBody AiDraftRequest req) {
        return Result.ok(aiPublishService.createDraft(req, UserContext.currentUserId()));
    }

    /** AI 一键发布：确认草稿 → 生成商品 */
    @PostMapping("/publish")
    public Result<ProductVO> publish(@RequestBody AiPublishRequest req) {
        return Result.ok(aiPublishService.publish(req, UserContext.currentUserId()));
    }
}
