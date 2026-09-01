package com.hdu.secondhand.controller;

import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.Result;
import com.hdu.secondhand.common.ResultCode;
import com.hdu.secondhand.dto.ProductReviewDTO;
import com.hdu.secondhand.service.ProductService;
import com.hdu.secondhand.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品管理接口（管理员，需 role=ADMIN）
 *
 * <p>审核流程（规范 6.5，组长分工）：AI 预检由 /api/ai/review（陈思瀚，AiService 能力层）
 * 先行调用，管理员在管理后台查看预检结论后人工决定，调本接口完成状态流转。</p>
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /** 审核商品：通过 → 在售(1)；驳回 → 审核驳回(5)，驳回原因入 reviewRemark */
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody ProductReviewDTO dto) {
        if (dto.getPass() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "pass 不能为空");
        }
        // 管理员身份由登录模块 JWT 拦截器（role=ADMIN）校验；当前为开发期默认用户
        productService.adminReview(id, dto.getPass(), dto.getRemark(), UserContext.currentUserId());
        return Result.ok();
    }
}
