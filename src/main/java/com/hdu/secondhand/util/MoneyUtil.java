package com.hdu.secondhand.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额单位转换工具
 *
 * <p>《接口约定规范 v1.0》：接口层金额统一为整数「分」，前端展示时除以 100；
 * 数据库层保持 DECIMAL 元（精度可控）。本类负责两层之间的转换。</p>
 */
public final class MoneyUtil {

    private static final BigDecimal FEN_PER_YUAN = new BigDecimal("100");

    private MoneyUtil() {
    }

    /** 元 → 分（四舍五入到整数分；null 透传 null） */
    public static Long toFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.multiply(FEN_PER_YUAN).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** 分 → 元（null 透传 null） */
    public static BigDecimal toYuan(Long fen) {
        if (fen == null) {
            return null;
        }
        return BigDecimal.valueOf(fen).divide(FEN_PER_YUAN);
    }

    /** 分 → 元（int 分） */
    public static BigDecimal toYuan(int fen) {
        return BigDecimal.valueOf(fen).divide(FEN_PER_YUAN);
    }
}
