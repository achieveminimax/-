package com.seckill.user.service.impl;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.user.dto.AddressRequest;
import com.seckill.user.dto.AddressResponse;
import com.seckill.user.entity.Address;
import com.seckill.user.mapper.AddressMapper;
import com.seckill.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收货地址服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    // 每个用户最多保存的地址数量
    private static final int MAX_ADDRESS_COUNT = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addAddress(Long userId, AddressRequest request) {
        // 检查地址数量是否已达上限
        int count = addressMapper.countByUserId(userId);
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "地址数量已达上限（最多" + MAX_ADDRESS_COUNT + "个）");
        }

        // 创建地址实体
        Address address = new Address();
        BeanUtils.copyProperties(request, address);
        address.setUserId(userId);

        // 如果没有设置是否默认，则默认为0
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }

        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() == 1) {
            addressMapper.cancelDefaultByUserId(userId);
        }

        // 如果是第一个地址，自动设为默认
        if (count == 0) {
            address.setIsDefault(1);
        }

        // 保存地址
        addressMapper.insert(address);

        log.info("新增收货地址成功, userId: {}, addressId: {}", userId, address.getId());
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, Long addressId, AddressRequest request) {
        // 查询地址
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(ResponseCodeEnum.ADDRESS_NOT_FOUND);
        }

        // 校验权限
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "无权操作该地址");
        }

        // 更新字段
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());

        // 处理默认地址设置
        if (request.getIsDefault() != null) {
            if (request.getIsDefault() == 1 && address.getIsDefault() == 0) {
                // 设置为默认，先取消其他默认地址
                addressMapper.cancelDefaultByUserId(userId);
                address.setIsDefault(1);
            } else if (request.getIsDefault() == 0 && address.getIsDefault() == 1) {
                // 取消默认，检查是否还有其他地址
                int count = addressMapper.countByUserId(userId);
                if (count > 1) {
                    address.setIsDefault(0);
                }
            }
        }

        addressMapper.updateById(address);
        log.info("更新收货地址成功, userId: {}, addressId: {}", userId, addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long addressId) {
        // 查询地址
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(ResponseCodeEnum.ADDRESS_NOT_FOUND);
        }

        // 校验权限
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "无权操作该地址");
        }

        // 删除地址
        addressMapper.deleteById(addressId);

        // 如果删除的是默认地址，自动将最早创建的地址设为默认
        if (address.getIsDefault() == 1) {
            List<Address> addressList = addressMapper.selectListByUserId(userId);
            if (!CollectionUtils.isEmpty(addressList)) {
                Address firstAddress = addressList.get(0);
                addressMapper.setDefault(firstAddress.getId(), userId);
            }
        }

        log.info("删除收货地址成功, userId: {}, addressId: {}", userId, addressId);
    }

    @Override
    public List<AddressResponse> getAddressList(Long userId) {
        List<Address> addressList = addressMapper.selectListByUserId(userId);
        return addressList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long addressId) {
        // 查询地址
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(ResponseCodeEnum.ADDRESS_NOT_FOUND);
        }

        // 校验权限
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "无权操作该地址");
        }

        // 已经是默认地址，无需操作
        if (address.getIsDefault() == 1) {
            return;
        }

        // 取消其他默认地址
        addressMapper.cancelDefaultByUserId(userId);

        // 设置当前地址为默认
        addressMapper.setDefault(addressId, userId);

        log.info("设置默认地址成功, userId: {}, addressId: {}", userId, addressId);
    }

    @Override
    public AddressResponse getDefaultAddress(Long userId) {
        List<Address> addressList = addressMapper.selectListByUserId(userId);
        return addressList.stream()
                .filter(addr -> addr.getIsDefault() == 1)
                .findFirst()
                .map(this::convertToResponse)
                .orElse(null);
    }

    /**
     * 转换为响应对象
     */
    private AddressResponse convertToResponse(Address address) {
        AddressResponse response = new AddressResponse();
        BeanUtils.copyProperties(address, response);
        response.setAddressId(address.getId());
        return response;
    }
}
