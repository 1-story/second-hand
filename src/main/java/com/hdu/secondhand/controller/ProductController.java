package com.hdu.secondhand.controller;

import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.common.Result;
import com.hdu.secondhand.dto.ProductCreateDTO;
import com.hdu.secondhand.dto.ProductQueryDTO;
import com.hdu.secondhand.dto.ProductStatusDTO;
import com.hdu.secondhand.dto.ProductUpdateDTO;
import com.hdu.secondhand.service.BrowseHistoryService;
import com.hdu.secondhand.service.ProductService;
import com.hdu.secondhand.util.UserContext;
import com.hdu.secondhand.vo.ProductListItemVO;
import com.hdu.secondhand.vo.ProductVO;
import com.hdu.secondhand.vo.UserProductItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品接口（田博）
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final BrowseHistoryService browseHistoryService;

    /** 发布商品 */
    @PostMapping
    public Result<Long> create(@RequestBody ProductCreateDTO dto) {
        return Result.ok(productService.create(dto, UserContext.currentUserId()));
    }

    /** 编辑商品（仅本人） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductUpdateDTO dto) {
        productService.update(id, dto, UserContext.currentUserId());
        return Result.ok();
    }

    /** 上架/下架 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody ProductStatusDTO dto) {
        productService.changeStatus(id, dto.getStatus(), UserContext.currentUserId());
        return Result.ok();
    }

    /** 删除商品（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id, UserContext.currentUserId());
        return Result.ok();
    }

    /** 分页浏览/检索在售商品 */
    @GetMapping
    public Result<PageResult<ProductListItemVO>> query(ProductQueryDTO dto) {
        return Result.ok(productService.query(dto));
    }

    /** 我的商品列表 */
    @GetMapping("/mine")
    public Result<PageResult<ProductListItemVO>> mine(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.myProducts(UserContext.currentUserId(), status, page, size));
    }

    /** 猜你喜欢 */
    @GetMapping("/recommend")
    public Result<List<ProductListItemVO>> recommend(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(productService.recommend(UserContext.currentUserId(), limit));
    }

    /** 我的浏览足迹 */
    @GetMapping("/history")
    public Result<PageResult<UserProductItemVO>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(browseHistoryService.myHistory(UserContext.currentUserId(), page, size));
    }

    /** 商品详情（浏览 +1） */
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id, UserContext.currentUserId()));
    }
}
