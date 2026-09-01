package com.hdu.secondhand.controller;

import com.hdu.secondhand.common.Result;
import com.hdu.secondhand.dto.AiEstimateRequest;
import com.hdu.secondhand.service.AiEstimateService;
import com.hdu.secondhand.util.UserContext;
import com.hdu.secondhand.vo.AiEstimateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 估价接口（田博）
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiEstimateController {

    private final AiEstimateService aiEstimateService;

    /** AI 智能估价（规则引擎 + 可选大模型补充） */
    @PostMapping("/estimate")
    public Result<AiEstimateVO> estimate(@RequestBody AiEstimateRequest req) {
        return Result.ok(aiEstimateService.estimate(req, UserContext.currentUserId()));
    }
}
