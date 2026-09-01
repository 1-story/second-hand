package com.hdu.secondhand.service;

import com.hdu.secondhand.dto.AiDescribeRequest;
import com.hdu.secondhand.dto.AiIdentifyRequest;
import com.hdu.secondhand.vo.AiDescribeVO;
import com.hdu.secondhand.vo.AiIdentifyVO;

/**
 * AI 识别与描述服务（对齐《接口约定规范 v1.0》6.1 / 6.2）
 */
public interface AiIdentifyService {

    /**
     * 商品识别：图片 → 分类 + 成色
     */
    AiIdentifyVO identify(AiIdentifyRequest req, long userId);

    /**
     * 描述生成：图片 + 分类 + 成色 + 关键词 → 描述草稿
     */
    AiDescribeVO describe(AiDescribeRequest req, long userId);
}
