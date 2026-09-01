package com.hdu.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hdu.secondhand.ai.CategoryEnum;
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
import com.hdu.secondhand.util.MoneyUtil;
import com.hdu.secondhand.util.UserContext;
import com.hdu.secondhand.vo.ProductListItemVO;
import com.hdu.secondhand.vo.ProductVO;
import com.hdu.secondhand.vo.SellerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品核心业务实现（对齐《接口约定规范 v1.1》第 7 节）
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
        if (dto.getPrice() == null || dto.getPrice() <= 0) {
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
        product.setPrice(MoneyUtil.toYuan(dto.getPrice()));
        product.setOriginalPrice(MoneyUtil.toYuan(dto.getOriginalPrice()));
        product.setEstimatedPrice(MoneyUtil.toYuan(dto.getEstimatedPrice()));
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
            if (dto.getPrice() <= 0) {
                throw new BizException(ResultCode.BAD_REQUEST, "售价必须大于 0");
            }
            product.setPrice(MoneyUtil.toYuan(dto.getPrice()));
        }
        if (dto.getOriginalPrice() != null) {
            product.setOriginalPrice(MoneyUtil.toYuan(dto.getOriginalPrice()));
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
            if (current != ProductStatus.DRAFT
                    && current != ProductStatus.OFF_SHELF
                    && current != ProductStatus.REJECTED) {
                throw new BizException(ResultCode.PRODUCT_STATUS_INVALID,
                        "当前状态（" + statusName(current) + "）不允许上架");
            }
        } else {
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
    @Transactional(rollbackFor = Exception.class)
    public void adminReview(Long id, boolean pass, String remark, long adminId) {
        // 权限校验由登录模块 JWT 拦截器（role=ADMIN）把关；此处仅做状态流转
        Product product = getById(id);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() != ProductStatus.AUDITING) {
            throw new BizException(ResultCode.PRODUCT_STATUS_INVALID,
                    "仅审核中(4)的商品可审核，当前状态：" + statusName(product.getStatus()));
        }
        if (pass) {
            product.setStatus(ProductStatus.ON_SALE);
            product.setReviewRemark(null);
        } else {
            product.setStatus(ProductStatus.REJECTED);
            product.setReviewRemark(remark);
        }
        updateById(product);
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

        // 分类：规范 category key 优先（含一级及二级子分类），categoryId 兼容
        List<Long> categoryIds = null;
        if (StringUtils.hasText(dto.getCategory())) {
            categoryIds = resolveCategoryIds(CategoryEnum.toId(dto.getCategory()));
        } else if (dto.getCategoryId() != null) {
            categoryIds = resolveCategoryIds(dto.getCategoryId());
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            wrapper.in(Product::getCategoryId, categoryIds);
        }

        // 价格（分 → 元）
        if (dto.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, MoneyUtil.toYuan(dto.getMinPrice()));
        }
        if (dto.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, MoneyUtil.toYuan(dto.getMaxPrice()));
        }

        // 成色：规范 condition（100/90/80/70）优先
        if (dto.getCondition() != null) {
            wrapper.ge(Product::getConditionLevel, CategoryEnum.toLevel(dto.getCondition()));
        } else if (dto.getConditionLevel() != null) {
            wrapper.ge(Product::getConditionLevel, dto.getConditionLevel());
        }

        // 排序：规范 sort 优先
        String sort = dto.getSort();
        if (!StringUtils.hasText(sort)) {
            sort = switch (dto.getSortBy() == null ? 1 : dto.getSortBy()) {
                case 2 -> "price_asc";
                case 3 -> "price_desc";
                case 4 -> "hot";
                default -> "latest";
            };
        }
        switch (sort) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice);
            case "hot" -> wrapper.orderByDesc(Product::getViewCount);
            default -> wrapper.orderByDesc(Product::getCreatedAt);
        }

        Page<Product> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(toListItems(result.getRecords()), result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO detail(Long id, long currentUserId) {
        Product product = getById(id);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        // 非在售/非本人时不允许查看
        if (product.getStatus() != ProductStatus.ON_SALE && !Objects.equals(product.getSellerId(), currentUserId)) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在或已下架");
        }

        // 浏览量 +1（原子 SQL）
        update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .setSql("view_count = view_count + 1"));
        product.setViewCount(product.getViewCount() == null ? 1 : product.getViewCount() + 1);

        // 记录浏览足迹（本人浏览自己商品不记录）
        if (!Objects.equals(product.getSellerId(), currentUserId)) {
            browseHistoryService.record(currentUserId, id);
        }

        // 组装 VO
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setPrice(MoneyUtil.toFen(product.getPrice()));
        vo.setOriginalPrice(MoneyUtil.toFen(product.getOriginalPrice()));
        vo.setEstimatedPrice(MoneyUtil.toFen(product.getEstimatedPrice()));
        vo.setCover(product.getCoverImage());
        vo.setViews(product.getViewCount());
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        vo.setCategory(CategoryEnum.toKey(product.getCategoryId()));
        vo.setCondition(String.valueOf(CategoryEnum.toPct(product.getConditionLevel())));
        vo.setConditionName(CategoryEnum.conditionName(CategoryEnum.toPct(product.getConditionLevel())));
        vo.setSeller(buildSeller(product.getSellerId()));

        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSortOrder));
        vo.setImages(images.stream().map(ProductImage::getUrl).collect(Collectors.toList()));

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

    /** 解析分类检索范围：一级分类自身 + 其二级子分类 */
    private List<Long> resolveCategoryIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        List<Category> children = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryId));
        children.forEach(c -> ids.add(c.getId()));
        return ids;
    }

    private List<ProductListItemVO> toListItems(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNames = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<Long, SellerVO> sellers = buildSellers(products);

        return products.stream().map(p -> {
            ProductListItemVO vo = new ProductListItemVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setPrice(MoneyUtil.toFen(p.getPrice()));
            vo.setOriginalPrice(MoneyUtil.toFen(p.getOriginalPrice()));
            vo.setCover(p.getCoverImage());
            vo.setCategory(CategoryEnum.toKey(p.getCategoryId()));
            vo.setCategoryName(categoryNames.getOrDefault(p.getCategoryId(), ""));
            vo.setCondition(String.valueOf(CategoryEnum.toPct(p.getConditionLevel())));
            vo.setConditionName(CategoryEnum.conditionName(CategoryEnum.toPct(p.getConditionLevel())));
            vo.setViews(p.getViewCount());
            vo.setCreatedAt(p.getCreatedAt());
            vo.setSeller(sellers.get(p.getSellerId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, SellerVO> buildSellers(List<Product> products) {
        List<Long> userIds = products.stream()
                .map(Product::getSellerId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::toSellerVO, (a, b) -> a));
    }

    private SellerVO buildSeller(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? new SellerVO() : toSellerVO(user);
    }

    private SellerVO toSellerVO(User user) {
        SellerVO vo = new SellerVO();
        vo.setId(user.getId());
        vo.setName(StringUtils.hasText(user.getName()) ? user.getName() : user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreditScore(user.getCreditScore());
        vo.setRealNameVerified(user.getRealNameVerified() != null && user.getRealNameVerified() == 1);
        return vo;
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
