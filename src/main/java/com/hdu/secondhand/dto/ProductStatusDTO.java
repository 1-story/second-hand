package com.hdu.secondhand.dto;

import lombok.Data;

/**
 * 商品上下架/状态变更入参
 * 目标状态：1=上架(在售)，2=下架；不允许直接改为其他状态
 */
@Data
public class ProductStatusDTO {

    /** 目标状态：1 上架 / 2 下架 */
    private Integer status;
}
