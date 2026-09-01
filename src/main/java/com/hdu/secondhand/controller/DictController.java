package com.hdu.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hdu.secondhand.common.Result;
import com.hdu.secondhand.entity.Category;
import com.hdu.secondhand.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举字典接口（对齐《接口约定规范 v1.0》附录：GET /api/dicts，免登录）
 */
@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
public class DictController {

    private final CategoryMapper categoryMapper;

    @GetMapping
    public Result<Map<String, Object>> dicts() {
        Map<String, Object> dict = new LinkedHashMap<>();

        // 商品分类（来自分类表）
        List<Map<String, Object>> categories = new ArrayList<>();
        List<Category> list = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
        for (Category c : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("parentId", c.getParentId());
            item.put("name", c.getName());
            categories.add(item);
        }
        dict.put("categories", categories);

        // 成色（规范枚举）
        dict.put("conditions", List.of(
                Map.of("value", 100, "name", "全新"),
                Map.of("value", 90, "name", "九成新"),
                Map.of("value", 80, "name", "八成新"),
                Map.of("value", 70, "name", "七成新及以下")
        ));

        // 商品状态
        dict.put("productStatus", List.of(
                Map.of("value", 0, "name", "草稿"),
                Map.of("value", 1, "name", "在售"),
                Map.of("value", 2, "name", "已下架"),
                Map.of("value", 3, "name", "已售出"),
                Map.of("value", 4, "name", "审核中"),
                Map.of("value", 5, "name", "审核驳回")
        ));

        // 订单状态（V1.0 预留）
        dict.put("orderStatus", List.of(
                Map.of("value", 0, "name", "待付款"),
                Map.of("value", 1, "name", "待发货"),
                Map.of("value", 2, "name", "待收货"),
                Map.of("value", 3, "name", "已完成"),
                Map.of("value", 4, "name", "已取消"),
                Map.of("value", 5, "name", "纠纷中")
        ));

        return Result.ok(dict);
    }
}
