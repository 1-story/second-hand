package com.hdu.secondhand.service;

import com.hdu.secondhand.common.PageResult;
import com.hdu.secondhand.dto.ProductCreateDTO;
import com.hdu.secondhand.dto.ProductQueryDTO;
import com.hdu.secondhand.dto.ProductUpdateDTO;
import com.hdu.secondhand.vo.ProductListItemVO;
import com.hdu.secondhand.vo.ProductVO;

import java.util.List;

/**
 * 商品核心业务接口（田博：发布/浏览检索/上下架/详情/推荐）
 */
public interface ProductService {

    /**
     * 发布商品
     *
     * @param dto    入参
     * @param userId 卖家 ID
     * @return 新商品 ID
     */
    Long create(ProductCreateDTO dto, long userId);

    /**
     * 编辑商品（仅本人；草稿/在售/已下架/审核驳回状态可编辑）
     */
    void update(Long id, ProductUpdateDTO dto, long userId);

    /**
     * 上架/下架（状态机：草稿/已下架/审核驳回 → 上架；在售 → 下架）
     *
     * @param targetStatus 1 上架 / 2 下架
     */
    void changeStatus(Long id, int targetStatus, long userId);

    /**
     * 管理员审核商品（组长分工：商品审核业务流程归田博）
     * 审核中(4) → 通过:在售(1) / 驳回:审核驳回(5)；驳回原因写入 reviewRemark。
     * AI 预检由 /api/ai/review（陈思瀚，AiService 能力层）先行调用，管理员人工决定后调本接口。
     *
     * @param id      商品 ID
     * @param pass    true=通过 false=驳回
     * @param remark  驳回原因（可选）
     * @param adminId 管理员 ID
     */
    void adminReview(Long id, boolean pass, String remark, long adminId);

    /**
     * 删除商品（逻辑删除，仅本人，非已售出）
     */
    void delete(Long id, long userId);

    /**
     * 分页检索在售商品
     */
    PageResult<ProductListItemVO> query(ProductQueryDTO dto);

    /**
     * 商品详情（浏览 +1，记录浏览足迹）
     */
    ProductVO detail(Long id, long currentUserId);

    /**
     * 我的商品列表（可按状态过滤）
     */
    PageResult<ProductListItemVO> myProducts(long userId, Integer status, int page, int size);

    /**
     * 猜你喜欢：基于浏览/收藏分类偏好推荐在售商品
     */
    List<ProductListItemVO> recommend(long userId, int limit);
}
