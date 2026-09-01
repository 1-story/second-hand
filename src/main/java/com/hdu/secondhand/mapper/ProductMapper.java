package com.hdu.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hdu.secondhand.entity.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 猜你喜欢：按用户浏览/收藏最多的分类，推荐同分类在售商品（按浏览量倒序）
     */
    List<Product> selectRecommend(@Param("userId") Long userId,
                                  @Param("limit") int limit);
}
