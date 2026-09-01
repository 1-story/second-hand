package com.hdu.secondhand.dto;

import lombok.Data;

/**
 * 商品审核入参（规范 6.5 流程：AI 预检结论 → 管理员人工决定）
 */
@Data
public class ProductReviewDTO {

    /** true=通过（审核中→在售） false=驳回（审核中→审核驳回） */
    private Boolean pass;

    /** 驳回原因（pass=false 时建议填写，写入 reviewRemark） */
    private String remark;
}
