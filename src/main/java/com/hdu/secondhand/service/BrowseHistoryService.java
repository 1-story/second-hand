package com.hdu.secondhand.service;

import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.vo.UserProductItemVO;

/**
 * 浏览足迹服务
 */
public interface BrowseHistoryService {

    /**
     * 记录浏览（同商品合并计数）
     */
    void record(long userId, long productId);

    /**
     * 我的浏览足迹（按最后浏览时间倒序）
     */
    PageResult<UserProductItemVO> myHistory(long userId, int page, int size);
}
