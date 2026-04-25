package com.seckill.goods.service;

import com.seckill.common.result.PageResult;
import com.seckill.goods.dto.AdminGoodsListResponse;
import com.seckill.goods.dto.AdminGoodsSaveRequest;
import com.seckill.goods.dto.GoodsDetailResponse;
import com.seckill.goods.dto.GoodsListResponse;

/**
 * 商品服务接口。
 * <p>
 * 同时定义前台商品查询能力和管理端商品维护能力。
 */
public interface GoodsService {

    /**
     * 查询前台商品列表。
     */
    PageResult<GoodsListResponse> getPublicGoodsList(Long categoryId, String keyword, String sort, Long current, Long size);

    /**
     * 查询前台商品详情。
     */
    GoodsDetailResponse getPublicGoodsDetail(Long goodsId);

    /**
     * 查询管理端商品列表。
     */
    PageResult<AdminGoodsListResponse> getAdminGoodsList(Long categoryId, String keyword, Integer status, Long current, Long size);

    /**
     * 新增商品。
     */
    Long createGoods(AdminGoodsSaveRequest request);

    /**
     * 修改商品。
     */
    void updateGoods(Long goodsId, AdminGoodsSaveRequest request);

    /**
     * 更新商品上下架状态。
     */
    void updateGoodsStatus(Long goodsId, Integer status);

    /**
     * 删除商品。
     */
    void deleteGoods(Long goodsId);
}
