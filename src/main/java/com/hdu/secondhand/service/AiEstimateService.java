package com.hdu.secondhand.service;

import com.hdu.secondhand.dto.AiEstimateRequest;
import com.hdu.secondhand.vo.AiEstimateVO;

/**
 * AI 估价服务：规则引擎 + 可选大模型补充，落库审计
 */
public interface AiEstimateService {

    /**
     * 执行估价
     *
     * @param req    估价入参
     * @param userId 发起用户
     * @return 估价结果
     */
    AiEstimateVO estimate(AiEstimateRequest req, long userId);
}
