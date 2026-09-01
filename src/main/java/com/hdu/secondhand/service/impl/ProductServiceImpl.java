package com.hdu.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.common.ProductStatus;
import com.hdu.secondhand.common.ResultCode;
import com.hdu.secondhand.dto.ProductCreateDTO;
import com.hdu.secondhand.dto.ProductQueryDTO;
import com.hdu.secondhand.dto.ProductUpdateDTO;
import com.hdu.secondhand.entity.Category;
import com.hdu.secondhand.entity.Favorite;
import com.hdu.secondhand.entity.Product;
import com.hdu.secondhand.entity.ProductImage;
import com.hdu.secondhand.entity.User;
import com.hdu.secondhand.mapper.CategoryMapper;
import com.hdu.secondhand.mapper.FavoriteMapper;
import com.hdu.secondhand.mapper.ProductImageMapper;
import com.hdu.secondhand.mapper.ProductMapper;
import com.hdu.secondhand.mapper.UserMapper;
import com.hdu.secondhand.service.BrowseHistoryService;
import com.hdu.secondhand.service.ProductService;
import com.hdu.secondhand.util.UserContext;
import com.hdu.secondhand.vo.ProductListItemVO;
import com.hdu.secondhand.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品核心业务实现
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final CategoryMapper categoryMapper;
    private final ProductImageMapper productImageMapper;
    private final FavoriteMapper favoriteMapper;
    private final UserMapper userMapper;
    private final BrowseHistoryService browseHistoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductCreateDTO dto, long userId) {
        // ---- 参数校验 ----
        if (dto.getCategoryId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "分类不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BizException(ResultCode.BAD_REQUEST, "标题不能为空");
        }
        if (dto.getTitle().length() > 100) {
            throw new BizException(ResultCode.BAD_REQUEST, "标题不能超过 100 字");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "售价必须大于 0");
        }
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || !Objects.equals(category.getStatus(), 1)) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }
        int conditionLevel = dto.getConditionLevel() == null ? 7 : dto.getConditionLevel();
        if (conditionLevel < 1 || conditionLevel > 10) {
            throw new BizException(ResultCode.PARAM_ERROR, "成色等级必须在 1~10 之间");
        }

        // ---- 组装商品 ----
        Product product = new Product();
        product.setSellerId(userId);
        product.setCategoryId(dto.getCategoryId());
        product.setTitle(dto.getTitle().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setEstimatedPrice(dto.getEstimatedPrice());
        product.setConditionLevel(conditionLevel);
        product.setConditionDesc(dto.getConditionDesc());
        product.setTags(dto.getTags());
        product.setLocation(dto.getLocation());
        product.setCoverImage(dto.getCoverImage());
        product.setViewCount(0);
        product.setFavoriteCount(0);
        boolean publishNow = dto.getPublishNow() == null || dto.getPublishNow();
        product.setStatus(publishNow ? ProductStatus.ON_SALE : ProductStatus.DRAFT);
        save(product);

        // ---- 图片 ----
        insertImages(product.getId(), dto.getImages());
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductUpdateDTO dto, long userId) {
        Product product = requireOwnerProduct(id, userId);
        if (product.getStatus() == ProductStatus.SOLD) {
            throw new BizException(ResultCode.PRODUCT_STATUS_INVALID, "已售出商品不可编辑");
        }
        if (dto.getCategoryId() != null) {
            Category category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
            }
            product.setCategoryId(dto.getCategoryId());
        }
        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new BizException(ResultCode.BAD_REQUEST, "标题不能为空");
            }
            product.setTitle(dto.getTitle().trim());
        }
        if (dto.getPrice() != null) {
            if (dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ResultCode.BAD_REQUEST, "售价必须大于 0");
            }
            product.setPrice(dto.getPrice());
        }
        if (dto.getConditionLevel() != null) {
            if (dto.getConditionLevel() < 1 || dto.getConditionLevel() > 10) {
                throw new BizException(ResultCode.PARAM_ERROR, "成色等级必须在 1~10 之间");
            }
            product.setConditionLevel(dto.getConditionLevel());
        }
        product.setDescription(dto.getDescription());
        product.setConditionDesc(dto.getConditionDesc());
        product.setTags(dto.getTags());
        product.setLocation(dto.getLocation());
        product.setCoverImage(dto.getCoverImage());
        updateById(product);

        // 图片整体替换
        if (dto.getImages() != null) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
                    .eq(ProductImage::getProductId, id));
            insertImages(id, dto.getImages());
        }
    }

    @Override
    public void changeStatus(Long id, int targetStatus, long userId) {
        if (targetStatus != ProductStatus.ON_SALE && targetStatus != ProductStatus.OFF_SHELF) {
            throw new BizException(ResultCode.PRODUCT_STATUS_INVALID, "仅支持上架(1)或下架(2)");
        }
        Product product = requireOwnerProduct(id, userId);
        int current = product.getStatus();
        if (targetStatus == ProductStatus.ON_SALE) {
            // 草稿 / 已下架 / 审核驳回 可上架
            if (current != ProductStatus.DRAFT
                    && current != ProductStatus.OFF_SHELF
                    && current != ProductStatus.REJECTED) {
                throw new BizException(ResultCode.PRODUCT_STATUS_INVALID,
                        "当前状态（" + statusName(current) + "）不允许上架");
            }
        } else {
            // 在售/审核中 可下架
            if (current != ProductStatus.ON_SALE && current != ProductStatus.AUDITING) {
                throw new BizException(ResultCode.PRODUCT_STATUS_INVALID,
                        "当前状态（" + statusName(current) + "）不允许下架");
            }
        }
        product.setStatus(targetStatus);
        updateById(product);
    }

    @Override
    public void delete(Long id, long userId) {
        Product product = requireOwnerProduct(id, userId);
        if (product.getStatus() == ProductStatus.SOLD) {
            throw new BizException(ResultCode.PRODUCT_STATUS_INVALID, "已售出商品不可删除");
        }
        removeById(id);
    }

    @Override
    public PageResult<ProductListItemVO> query(ProductQueryDTO dto) {
        long page = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        long size = dto.getSize() == null || dto.getSize() < 1 ? 10 : Math.min(dto.getSize(), 100);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // 只看在售商品
        wrapper.eq(Product::getStatus, ProductStatus.ON_SALE);

        if (StringUtils.hasText(dto.getKeyword())) {
            String kw = dto.getKeyword().trim();
            wrapper.and(w -> w.like(Product::getTitle, kw).or().like(Product::getDescription, kw));
        }
        if (dto.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, dto.getCategoryId());
        }
        if (dto.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, dto.getMinPrice());
        }
        if (dto.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, dto.getMaxPrice());
        }
        if (dto.getConditionLevel() != null) {
            wrapper.ge(Product::getConditionLevel, dto.getConditionLevel());
        }

        // 排序
        int sortBy = dto.getSortBy() == null ? 1 : dto.getSortBy();
        switch (sortBy) {
            case 2 -> wrapper.orderByAsc(Product::getPrice);
            case 3 -> wrapper.orderByDesc(Product::getPrice);
            case 4 -> wrapper.orderByDesc(Product::getViewCount);
            default -> wrapper.orderByDesc(Product::getCreatedAt);
        }

        Page<Product> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(toListItems(result.getRecords()), result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO detail(Long id, long currentUserId) {
        Product product = getById(id);
        if (product == null || product.getDeleted() != null && product.getDeleted() == 1) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        // 非在售/非本人时不允许查看？约定：仅本人可看非在售商品，其他用户只能看在售
        if (product.getStatus() != ProductStatus.ON_SALE && !Objects.equals(product.getSellerId(), currentUserId)) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在或已下架");
        }

        // 浏览量 +1（原子 SQL）
        update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .setSql("view_count = view_count + 1"));

        // 记录浏览足迹（本人浏览自己商品不记录）
        if (!Objects.equals(product.getSellerId(), currentUserId)) {
            browseHistoryService.record(currentUserId, id);
        }

        // 组装 VO
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        User seller = userMapper.selectById(product.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerCredit(seller.getCreditScore());
        }
        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSortOrder));
        vo.setImages(images.stream().map(ProductImage::getUrl).collect(Collectors.toList()));

        // 收藏状态
        Long fav = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, currentUserId)
                .eq(Favorite::getProductId, id));
        vo.setFavorited(fav != null && fav > 0);
        return vo;
    }

    @Override
    public PageResult<ProductListItemVO> myProducts(long userId, Integer status, int page, int size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : Math.min(size, 100);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, userId)
                .orderByDesc(Product::getCreatedAt);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        Page<Product> result = page(new Page<>(p, s), wrapper);
        return PageResult.of(toListItems(result.getRecords()), result.getTotal(), p, s);
    }

    @Override
    public List<ProductListItemVO> recommend(long userId, int limit) {
        int n = Math.min(Math.max(limit, 1), 20);
        List<Product> products = baseMapper.selectRecommend(userId, n);
        return toListItems(products);
    }

    // ==================== 内部方法 ====================

    private Product requireOwnerProduct(Long id, long userId) {
        Product product = getById(id);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (!Objects.equals(product.getSellerId(), userId)) {
            throw new BizException(ResultCode.PRODUCT_NOT_OWNER);
        }
        return product;
    }

    private void insertImages(Long productId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        int order = 0;
        List<ProductImage> list = new ArrayList<>();
        for (String url : images) {
            if (!StringUtils.hasText(url)) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(url.trim());
            image.setSortOrder(order++);
            list.add(image);
        }
        if (!list.isEmpty()) {
            list.forEach(productImageMapper::insert);
        }
    }

    private List<ProductListItemVO> toListItems(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNames = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        return products.stream().map(p -> {
            ProductListItemVO vo = new ProductListItemVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setPrice(p.getPrice());
            vo.setEstimatedPrice(p.getEstimatedPrice());
            vo.setConditionLevel(p.getConditionLevel());
            vo.setCoverImage(p.getCoverImage());
            vo.setLocation(p.getLocation());
            vo.setViewCount(p.getViewCount());
            vo.setFavoriteCount(p.getFavoriteCount());
            vo.setCreatedAt(p.getCreatedAt());
            vo.setCategoryName(categoryNames.getOrDefault(p.getCategoryId(), ""));
            return vo;
        }).collect(Collectors.toList());
    }

    private static String statusName(int status) {
        return switch (status) {
            case ProductStatus.DRAFT -> "草稿";
            case ProductStatus.ON_SALE -> "在售";
            case ProductStatus.OFF_SHELF -> "已下架";
            case ProductStatus.SOLD -> "已售出";
            case ProductStatus.AUDITING -> "审核中";
            case ProductStatus.REJECTED -> "审核驳回";
            default -> "未知";
        };
    }
}
