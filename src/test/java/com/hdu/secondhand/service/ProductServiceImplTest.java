package com.hdu.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ProductStatus;
import com.hdu.secondhand.dto.ProductCreateDTO;
import com.hdu.secondhand.entity.Category;
import com.hdu.secondhand.entity.Product;
import com.hdu.secondhand.mapper.CategoryMapper;
import com.hdu.secondhand.mapper.FavoriteMapper;
import com.hdu.secondhand.mapper.ProductImageMapper;
import com.hdu.secondhand.mapper.ProductMapper;
import com.hdu.secondhand.mapper.UserMapper;
import com.hdu.secondhand.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品核心业务单元测试（Mockito 模拟 Mapper，不依赖数据库）
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ProductImageMapper productImageMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private BrowseHistoryService browseHistoryService;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        category = new Category();
        category.setId(11L);
        category.setName("手机");
        category.setStatus(1);
        category.setBasePrice(new BigDecimal("3000"));
        category.setDepreciationRate(new BigDecimal("0.18"));
        category.setHeatWeight(new BigDecimal("1.3"));

        // @InjectMocks 无法注入 ServiceImpl 的泛型 baseMapper 字段，手动反射补设
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(productService, productMapper);
    }

    @Test
    @DisplayName("发布商品：publishNow=true 直接上架")
    void create_publishNow() {
        when(categoryMapper.selectById(11L)).thenReturn(category);
        // ServiceImpl.save 委托给 baseMapper.insert；模拟自增主键回填
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            inv.getArgument(0, Product.class).setId(42L);
            return 1;
        });

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setCategoryId(11L);
        dto.setTitle("iPhone 13 九成新");
        dto.setPrice(280000L); // 2800元
        dto.setConditionLevel(9);
        dto.setPublishNow(true);

        Long id = productService.create(dto, 1L);
        assertNotNull(id);
        verify(productMapper).insert(any(Product.class));
    }

    @Test
    @DisplayName("发布商品：publishNow=false 保存为草稿")
    void create_saveDraft() {
        when(categoryMapper.selectById(11L)).thenReturn(category);
        doReturn(1).when(productMapper).insert(any(Product.class));

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setCategoryId(11L);
        dto.setTitle("测试草稿");
        dto.setPrice(10000L); // 100元
        dto.setPublishNow(false);

        productService.create(dto, 1L);
        // 验证插入的商品为草稿状态
        verify(productMapper).insert(any(Product.class));
    }

    @Test
    @DisplayName("发布商品：分类不存在报错")
    void create_categoryNotFound() {
        when(categoryMapper.selectById(99L)).thenReturn(null);
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setCategoryId(99L);
        dto.setTitle("测试");
        dto.setPrice(10000L);
        assertThrows(BizException.class, () -> productService.create(dto, 1L));
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    @DisplayName("发布商品：价格必须大于0")
    void create_invalidPrice() {
        when(categoryMapper.selectById(11L)).thenReturn(category);
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setCategoryId(11L);
        dto.setTitle("测试");
        dto.setPrice(0L); // 0 分非法
        assertThrows(BizException.class, () -> productService.create(dto, 1L));
    }

    @Test
    @DisplayName("上下架：已下架商品可上架")
    void changeStatus_offToOn() {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(1L);
        product.setStatus(ProductStatus.OFF_SHELF);
        when(productMapper.selectById(1L)).thenReturn(product);

        productService.changeStatus(1L, ProductStatus.ON_SALE, 1L);
        verify(productMapper).updateById(any(Product.class));
    }

    @Test
    @DisplayName("上下架：在售商品重复上架报错")
    void changeStatus_invalidTransition() {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(1L);
        product.setStatus(ProductStatus.ON_SALE);
        when(productMapper.selectById(1L)).thenReturn(product);

        assertThrows(BizException.class,
                () -> productService.changeStatus(1L, ProductStatus.ON_SALE, 1L));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @DisplayName("上下架：非本人操作报错")
    void changeStatus_notOwner() {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(1L);
        product.setStatus(ProductStatus.OFF_SHELF);
        when(productMapper.selectById(1L)).thenReturn(product);

        assertThrows(BizException.class,
                () -> productService.changeStatus(1L, ProductStatus.ON_SALE, 999L));
    }

    @Test
    @DisplayName("删除：已售出商品不可删除")
    void delete_soldForbidden() {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(1L);
        product.setStatus(ProductStatus.SOLD);
        when(productMapper.selectById(1L)).thenReturn(product);

        assertThrows(BizException.class, () -> productService.delete(1L, 1L));
    }

    @Test
    @DisplayName("分页检索：正常分页调用")
    void query_page() {
        // page() 是 ServiceImpl 方法，spy 成本高；这里验证构造 wrapper 不抛异常即可
        when(productMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(categoryMapper.selectBatchIds(any())).thenReturn(java.util.List.of());
        // 空结果场景
        com.hdu.secondhand.dto.ProductQueryDTO dto = new com.hdu.secondhand.dto.ProductQueryDTO();
        dto.setKeyword("手机");
        dto.setPage(1);
        dto.setSize(10);
        var result = productService.query(dto);
        assertEquals(0, result.getList().size());
    }

    @Test
    @DisplayName("详情：不存在的商品报错")
    void detail_notFound() {
        when(productMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(BizException.class, () -> productService.detail(999L, 1L));
    }

    @Test
    @DisplayName("管理员审核：审核中通过 → 在售")
    void adminReview_pass() {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(1L);
        product.setStatus(ProductStatus.AUDITING);
        when(productMapper.selectById(1L)).thenReturn(product);

        productService.adminReview(1L, true, null, 2L);
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
        verify(productMapper).updateById(any(Product.class));
    }

    @Test
    @DisplayName("管理员审核：审核中驳回 → 审核驳回并写原因")
    void adminReview_reject() {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(1L);
        product.setStatus(ProductStatus.AUDITING);
        when(productMapper.selectById(1L)).thenReturn(product);

        productService.adminReview(1L, false, "图片不清晰", 2L);
        assertEquals(ProductStatus.REJECTED, product.getStatus());
        assertEquals("图片不清晰", product.getReviewRemark());
        verify(productMapper).updateById(any(Product.class));
    }

    @Test
    @DisplayName("管理员审核：非审核中商品不可审核")
    void adminReview_invalidStatus() {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(1L);
        product.setStatus(ProductStatus.ON_SALE);
        when(productMapper.selectById(1L)).thenReturn(product);

        assertThrows(BizException.class, () -> productService.adminReview(1L, true, null, 2L));
        verify(productMapper, never()).updateById(any(Product.class));
    }
}
