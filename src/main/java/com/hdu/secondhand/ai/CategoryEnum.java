package com.hdu.secondhand.ai;

import java.util.HashMap;
import java.util.Map;

/**
 * 规范枚举映射（对齐《接口约定规范 v1.0》第 8 节）
 *
 * <p>规范枚举：
 * <ul>
 *   <li>商品分类 category：book(教材图书) / digital(数码产品) / living(生活用品) / sports(运动户外) / clothing(服饰鞋包) / other(其他)</li>
 *   <li>成色 condition：100(全新) / 90(九成新) / 80(八成新) / 70(七成新及以下)</li>
 * </ul>
 * 数据库层使用分类数字 ID 与成色 1~10，本类负责两侧映射。</p>
 */
public final class CategoryEnum {

    /** 规范分类 key → 数据库一级分类 ID */
    private static final Map<String, Long> KEY_TO_ID = new HashMap<>();

    /** 数据库分类 ID → 规范分类 key */
    private static final Map<Long, String> ID_TO_KEY = new HashMap<>();

    /** 规范分类 key → 中文名（接口 categoryName 用） */
    private static final Map<String, String> KEY_TO_NAME = new HashMap<>();

    static {
        KEY_TO_NAME.put("digital", "数码产品");
        KEY_TO_NAME.put("book", "教材图书");
        KEY_TO_NAME.put("living", "生活用品");
        KEY_TO_NAME.put("sports", "运动户外");
        KEY_TO_NAME.put("clothing", "服饰鞋包");
        KEY_TO_NAME.put("other", "其他");
    }

    static {
        // 一级分类（与 sql/schema.sql 种子数据一致）
        KEY_TO_ID.put("digital", 1L);
        KEY_TO_ID.put("book", 2L);
        KEY_TO_ID.put("living", 3L);
        KEY_TO_ID.put("sports", 4L);
        KEY_TO_ID.put("clothing", 5L);
        KEY_TO_ID.put("other", 8L);
        // 未在规范中的一级分类（美妆 6、乐器 7）归入 other
        ID_TO_KEY.put(1L, "digital");
        ID_TO_KEY.put(2L, "book");
        ID_TO_KEY.put(3L, "living");
        ID_TO_KEY.put(4L, "sports");
        ID_TO_KEY.put(5L, "clothing");
        ID_TO_KEY.put(6L, "other");
        ID_TO_KEY.put(7L, "other");
        ID_TO_KEY.put(8L, "other");
        // 二级分类映射到对应一级 key
        for (long id = 11; id <= 14; id++) {
            ID_TO_KEY.put(id, "digital");
        }
        for (long id = 21; id <= 22; id++) {
            ID_TO_KEY.put(id, "book");
        }
        for (long id = 31; id <= 32; id++) {
            ID_TO_KEY.put(id, "living");
        }
    }

    private CategoryEnum() {
    }

    /** 规范分类 key → 数据库分类 ID；未知 key 返回 other(8) */
    public static Long toId(String key) {
        if (key == null) {
            return 8L;
        }
        return KEY_TO_ID.getOrDefault(key.trim().toLowerCase(), 8L);
    }

    /** 数据库分类 ID → 规范分类 key；未知 ID 返回 other */
    public static String toKey(Long categoryId) {
        if (categoryId == null) {
            return "other";
        }
        return ID_TO_KEY.getOrDefault(categoryId, "other");
    }

    /** 规范分类 key → 中文名（默认"其他"） */
    public static String nameOf(String key) {
        if (key == null) {
            return "其他";
        }
        return KEY_TO_NAME.getOrDefault(key.trim().toLowerCase(), "其他");
    }

    /** 规范成色百分比 → 数据库成色等级 1~10；未知/越界默认 7 */
    public static int toLevel(Integer conditionPct) {
        if (conditionPct == null) {
            return 7;
        }
        if (conditionPct >= 100) {
            return 10;
        }
        if (conditionPct >= 90) {
            return 9;
        }
        if (conditionPct >= 80) {
            return 8;
        }
        if (conditionPct >= 70) {
            return 7;
        }
        if (conditionPct >= 60) {
            return 6;
        }
        if (conditionPct >= 50) {
            return 5;
        }
        return Math.max(1, conditionPct / 10);
    }

    /** 数据库成色等级 1~10 → 规范成色百分比 */
    public static int toPct(Integer level) {
        if (level == null) {
            return 70;
        }
        if (level >= 10) {
            return 100;
        }
        if (level >= 9) {
            return 90;
        }
        if (level >= 8) {
            return 80;
        }
        if (level >= 7) {
            return 70;
        }
        return Math.max(10, level * 10);
    }

    /** 规范成色百分比 → 名称 */
    public static String conditionName(Integer pct) {
        if (pct == null) {
            return "七成新及以下";
        }
        if (pct >= 100) {
            return "全新";
        }
        if (pct >= 90) {
            return "九成新";
        }
        if (pct >= 80) {
            return "八成新";
        }
        return "七成新及以下";
    }
}
