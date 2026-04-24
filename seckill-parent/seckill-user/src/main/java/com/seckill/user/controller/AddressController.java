package com.seckill.user.controller;

import com.seckill.common.result.Result;
import com.seckill.common.utils.UserContext;
import com.seckill.user.dto.AddressRequest;
import com.seckill.user.dto.AddressResponse;
import com.seckill.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收货地址控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 新增收货地址
     */
    @PostMapping
    public Result<Map<String, Long>> addAddress(@Valid @RequestBody AddressRequest request) {
        Long userId = UserContext.getCurrentUserId();
        Long addressId = addressService.addAddress(userId, request);
        Map<String, Long> data = new HashMap<>();
        data.put("addressId", addressId);
        return Result.created(data);
    }

    /**
     * 更新收货地址
     */
    @PutMapping("/{id}")
    public Result<Void> updateAddress(@PathVariable("id") Long addressId,
                                       @RequestBody AddressRequest request) {
        Long userId = UserContext.getCurrentUserId();
        addressService.updateAddress(userId, addressId, request);
        return Result.success();
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable("id") Long addressId) {
        Long userId = UserContext.getCurrentUserId();
        addressService.deleteAddress(userId, addressId);
        return Result.success();
    }

    /**
     * 获取地址列表
     */
    @GetMapping("/list")
    public Result<List<AddressResponse>> getAddressList() {
        Long userId = UserContext.getCurrentUserId();
        List<AddressResponse> list = addressService.getAddressList(userId);
        return Result.success(list);
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/default/{id}")
    public Result<Void> setDefaultAddress(@PathVariable("id") Long addressId) {
        Long userId = UserContext.getCurrentUserId();
        addressService.setDefaultAddress(userId, addressId);
        return Result.success();
    }

    /**
     * 获取默认地址
     */
    @GetMapping("/default")
    public Result<AddressResponse> getDefaultAddress() {
        Long userId = UserContext.getCurrentUserId();
        AddressResponse address = addressService.getDefaultAddress(userId);
        return Result.success(address);
    }
}
