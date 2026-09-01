package com.hdu.secondhand.controller;

import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.common.Result;
import com.hdu.secondhand.service.FavoriteService;
import com.hdu.secondhand.util.UserContext;
import com.hdu.secondhand.vo.UserProductItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 收藏接口（田博）
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 收藏/取消收藏（幂等切换），返回当前是否已收藏 */
    @PostMapping("/{productId}")
    public Result<Map<String, Boolean>> toggle(@PathVariable Long productId) {
        boolean favorited = favoriteService.toggle(UserContext.currentUserId(), productId);
        return Result.ok(Map.of("favorited", favorited));
    }

    /** 我的收藏列表 */
    @GetMapping
    public Result<PageResult<UserProductItemVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(favoriteService.myFavorites(UserContext.currentUserId(), page, size));
    }
}
