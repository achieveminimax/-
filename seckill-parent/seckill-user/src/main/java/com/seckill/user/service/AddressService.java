package com.seckill.user.service;

import com.seckill.user.dto.AddressRequest;
import com.seckill.user.dto.AddressResponse;

import java.util.List;

/**
 * 收货地址服务接口
 */
public interface AddressService {

    /**
     * 新增收货地址
     *
     * @param userId  用户ID
     * @param request 地址请求
     * @return 地址ID
     */
    Long addAddress(Long userId, AddressRequest request);

    /**
     * 更新收货地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     * @param request   地址请求
     */
    void updateAddress(Long userId, Long addressId, AddressRequest request);

    /**
     * 删除收货地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 获取地址列表
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    List<AddressResponse> getAddressList(Long userId);

    /**
     * 设置默认地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void setDefaultAddress(Long userId, Long addressId);

    /**
     * 获取默认地址
     *
     * @param userId 用户ID
     * @return 默认地址
     */
    AddressResponse getDefaultAddress(Long userId);
}
