package com.hdu.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.common.ProductStatus;
import com.hdu.secondhand.common.ResultCode;
import com.hdu.secondhand.entity.Favorite;
import com.hdu.secondhand.entity.Product;
import com.hdu.secondhand.mapper.FavoriteMapper;
import com.hdu.secondhand.mapper.ProductMapper;
import com.hdu.secondhand.service.FavoriteService;
import com.hdu.secondhand.util.MoneyUtil;
import com.hdu.secondhand.vo.UserProductItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(long userId, long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (Objects.equals(product.getSellerId(), userId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不能收藏自己发布的商品");
        }
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BizException(ResultCode.PRODUCT_STATUS_INVALID, "该商品当前不可收藏");
        }

        Favorite favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (favorite == null) {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setProductId(productId);
            favoriteMapper.insert(f);
            // 收藏数 +1
            product.setFavoriteCount(product.getFavoriteCount() == null ? 1 : product.getFavoriteCount() + 1);
            productMapper.updateById(product);
            return true;
        } else {
            favoriteMapper.deleteById(favorite.getId());
            // 收藏数 -1（不低于 0）
            int count = product.getFavoriteCount() == null ? 0 : product.getFavoriteCount();
            product.setFavoriteCount(Math.max(0, count - 1));
            productMapper.updateById(product);
            return false;
        }
    }

    @Override
    public boolean isFavorited(long userId, long productId) {
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        return count != null && count > 0;
    }

    @Override
    public PageResult<UserProductItemVO> myFavorites(long userId, int page, int size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : Math.min(size, 100);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Favorite> result =
                favoriteMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p, s),
                        new LambdaQueryWrapper<Favorite>()
                                .eq(Favorite::getUserId, userId)
                                .orderByDesc(Favorite::getCreatedAt));

        List<UserProductItemVO> items = new ArrayList<>();
        List<Favorite> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> productIds = records.stream()
                    .map(Favorite::getProductId).collect(Collectors.toList());
            Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p2 -> p2, (a, b) -> a));
            for (Favorite fav : records) {
                Product product = productMap.get(fav.getProductId());
                if (product == null) {
                    continue;
                }
                UserProductItemVO vo = new UserProductItemVO();
                vo.setProductId(product.getId());
                vo.setTitle(product.getTitle());
                vo.setPrice(MoneyUtil.toFen(product.getPrice()));
                vo.setCoverImage(product.getCoverImage());
                vo.setLocation(product.getLocation());
                vo.setStatus(product.getStatus());
                vo.setActionTime(fav.getCreatedAt());
                items.add(vo);
            }
        }
        return PageResult.of(items, result.getTotal(), p, s);
    }
}
