package com.hdu.secondhand.common;

/**
 * 商品状态常量（与 product.status 对应）
 * 0草稿 1在售 2已下架 3已售出 4审核中 5审核驳回
 */
public final class ProductStatus {

    /** 草稿 */
    public static final int DRAFT = 0;
    /** 在售 */
    public static final int ON_SALE = 1;
    /** 已下架 */
    public static final int OFF_SHELF = 2;
    /** 已售出 */
    public static final int SOLD = 3;
    /** 审核中 */
    public static final int AUDITING = 4;
    /** 审核驳回 */
    public static final int REJECTED = 5;

    private ProductStatus() {
    }
}
