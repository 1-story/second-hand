package com.hdu.secondhand.vo;

import lombok.Data;

/**
 * 估价区间（分），对齐《接口约定规范 v1.0》6.3 priceRange
 */
@Data
public class AiPriceRangeVO {

    /** 最低价（分） */
    private Long min;

    /** 最高价（分） */
    private Long max;

    public static AiPriceRangeVO of(Long min, Long max) {
        AiPriceRangeVO vo = new AiPriceRangeVO();
        vo.setMin(min);
        vo.setMax(max);
        return vo;
    }
}
