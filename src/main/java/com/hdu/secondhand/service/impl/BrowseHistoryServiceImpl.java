package com.hdu.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.entity.BrowseHistory;
import com.hdu.secondhand.entity.Product;
import com.hdu.secondhand.mapper.BrowseHistoryMapper;
import com.hdu.secondhand.mapper.ProductMapper;
import com.hdu.secondhand.service.BrowseHistoryService;
import com.hdu.secondhand.util.MoneyUtil;
import com.hdu.secondhand.vo.UserProductItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 浏览足迹服务实现
 */
@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;
    private final ProductMapper productMapper;

    @Override
    public void record(long userId, long productId) {
        BrowseHistory history = browseHistoryMapper.selectOne(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getProductId, productId));
        if (history == null) {
            BrowseHistory h = new BrowseHistory();
            h.setUserId(userId);
            h.setProductId(productId);
            h.setViewCount(1);
            h.setLastViewAt(java.time.LocalDateTime.now());
            browseHistoryMapper.insert(h);
        } else {
            history.setViewCount(history.getViewCount() + 1);
            history.setLastViewAt(java.time.LocalDateTime.now());
            browseHistoryMapper.updateById(history);
        }
    }

    @Override
    public PageResult<UserProductItemVO> myHistory(long userId, int page, int size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : Math.min(size, 100);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BrowseHistory> result =
                browseHistoryMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p, s),
                        new LambdaQueryWrapper<BrowseHistory>()
                                .eq(BrowseHistory::getUserId, userId)
                                .orderByDesc(BrowseHistory::getLastViewAt));

        List<UserProductItemVO> items = toItems(result.getRecords());
        return PageResult.of(items, result.getTotal(), p, s);
    }

    private List<UserProductItemVO> toItems(List<BrowseHistory> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> productIds = records.stream()
                .map(BrowseHistory::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        List<UserProductItemVO> items = new ArrayList<>();
        for (BrowseHistory h : records) {
            Product product = productMap.get(h.getProductId());
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
            vo.setActionTime(h.getLastViewAt());
            items.add(vo);
        }
        return items;
    }
}
