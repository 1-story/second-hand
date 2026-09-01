package com.hdu.secondhand.service;

import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.vo.UserProductItemVO;

/**
 * 收藏服务
 */
public interface FavoriteService {

    /**
     * 收藏/取消收藏（幂等切换）
     *
     * @return 操作后是否处于已收藏状态
     */
    boolean toggle(long userId, long productId);

    /**
     * 是否已收藏
     */
    boolean isFavorited(long userId, long productId);

    /**
     * 我的收藏列表（按收藏时间倒序）
     */
    PageResult<UserProductItemVO> myFavorites(long userId, int page, int size);
}
