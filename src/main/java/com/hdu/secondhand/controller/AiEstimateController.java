package com.hdu.secondhand.controller;

import com.hdu.secondhand.common.Result;
import com.hdu.secondhand.dto.AiDescribeRequest;
import com.hdu.secondhand.dto.AiEstimateRequest;
import com.hdu.secondhand.dto.AiIdentifyRequest;
import com.hdu.secondhand.service.AiEstimateService;
import com.hdu.secondhand.service.AiIdentifyService;
import com.hdu.secondhand.util.UserContext;
import com.hdu.secondhand.vo.AiDescribeVO;
import com.hdu.secondhand.vo.AiEstimateVO;
import com.hdu.secondhand.vo.AiIdentifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 接口（对齐《接口约定规范 v1.0》第 6 节）
 * identify / describe / estimate 由本模块实现；chat（AI 问答，林天楚）、
 * review（AI 辅助审核，V1.0）接入后并列注册。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiEstimateController {

    private final AiEstimateService aiEstimateService;
    private final AiIdentifyService aiIdentifyService;

    /** 6.1 AI 商品识别：图片 → 分类 + 成色 */
    @PostMapping("/identify")
    public Result<AiIdentifyVO> identify(@RequestBody AiIdentifyRequest req) {
        return Result.ok(aiIdentifyService.identify(req, UserContext.currentUserId()));
    }

    /** 6.2 AI 描述生成 */
    @PostMapping("/describe")
    public Result<AiDescribeVO> describe(@RequestBody AiDescribeRequest req) {
        return Result.ok(aiIdentifyService.describe(req, UserContext.currentUserId()));
    }

    /** 6.3 AI 智能估价（双层架构：规则引擎 + 大模型，失败自动降级） */
    @PostMapping("/estimate")
    public Result<AiEstimateVO> estimate(@RequestBody AiEstimateRequest req) {
        return Result.ok(aiEstimateService.estimate(req, UserContext.currentUserId()));
    }
}
