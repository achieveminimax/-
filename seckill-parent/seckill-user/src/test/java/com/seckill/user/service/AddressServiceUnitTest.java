package com.seckill.user.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.user.dto.AddressRequest;
import com.seckill.user.dto.AddressResponse;
import com.seckill.user.entity.Address;
import com.seckill.user.mapper.AddressMapper;
import com.seckill.user.service.impl.AddressServiceImpl;
import com.seckill.user.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AddressService 纯单元测试
 * 使用纯 Mockito，避免 Spring Boot 的复杂依赖
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService 单元测试")
class AddressServiceUnitTest {

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    // ==================== 新增地址测试 ====================

    @Test
    @DisplayName("新增地址成功")
    void addAddress_Success() {
        // Given
        Long userId = 1L;
        AddressRequest request = TestDataFactory.createAddressRequest();

        when(addressMapper.countByUserId(userId)).thenReturn(0);
        when(addressMapper.insert(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setId(1L);
            return 1;
        });

        // When
        Long addressId = addressService.addAddress(userId, request);

        // Then
        assertNotNull(addressId);
        assertEquals(1L, addressId);
        verify(addressMapper).countByUserId(userId);
        verify(addressMapper).insert(any(Address.class));
    }

    @Test
    @DisplayName("新增地址失败 - 地址数量达到上限")
    void addAddress_MaxLimit() {
        // Given
        Long userId = 1L;
        AddressRequest request = TestDataFactory.createAddressRequest();

        when(addressMapper.countByUserId(userId)).thenReturn(10);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            addressService.addAddress(userId, request);
        });

        assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("地址数量已达上限"));
    }

    // ==================== 更新地址测试 ====================

    @Test
    @DisplayName("更新地址成功")
    void updateAddress_Success() {
        // Given
        Long userId = 1L;
        Long addressId = 1L;
        AddressRequest request = TestDataFactory.createAddressRequest();
        Address address = TestDataFactory.createAddress(addressId, userId);

        when(addressMapper.selectById(addressId)).thenReturn(address);

        // When
        assertDoesNotThrow(() -> addressService.updateAddress(userId, addressId, request));

        // Then
        assertEquals(request.getReceiverName(), address.getReceiverName());
        assertEquals(request.getReceiverPhone(), address.getReceiverPhone());
        assertEquals(request.getProvince(), address.getProvince());
        verify(addressMapper).updateById(address);
    }

    @Test
    @DisplayName("更新地址失败 - 地址不存在")
    void updateAddress_NotFound() {
        // Given
        Long userId = 1L;
        Long addressId = 999L;
        AddressRequest request = TestDataFactory.createAddressRequest();

        when(addressMapper.selectById(addressId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            addressService.updateAddress(userId, addressId, request);
        });

        assertEquals(ResponseCodeEnum.ADDRESS_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("更新地址失败 - 无权操作")
    void updateAddress_NoPermission() {
        // Given
        Long userId = 1L;
        Long addressId = 1L;
        AddressRequest request = TestDataFactory.createAddressRequest();
        Address address = TestDataFactory.createAddress(addressId, 2L); // 属于用户2

        when(addressMapper.selectById(addressId)).thenReturn(address);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            addressService.updateAddress(userId, addressId, request);
        });

        assertEquals(ResponseCodeEnum.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("无权操作"));
    }

    // ==================== 删除地址测试 ====================

    @Test
    @DisplayName("删除地址成功")
    void deleteAddress_Success() {
        // Given
        Long userId = 1L;
        Long addressId = 1L;
        Address address = TestDataFactory.createAddress(addressId, userId);

        when(addressMapper.selectById(addressId)).thenReturn(address);

        // When
        assertDoesNotThrow(() -> addressService.deleteAddress(userId, addressId));

        // Then
        verify(addressMapper).deleteById(addressId);
    }

    @Test
    @DisplayName("删除默认地址成功 - 自动设置最早创建地址为默认")
    void deleteAddress_DefaultAddress_ShouldSetEarliestAsDefault() {
        // Given
        Long userId = 1L;
        Long addressId = 1L;
        Address deletedDefault = TestDataFactory.createAddress(addressId, userId);
        deletedDefault.setIsDefault(1);

        Address newest = TestDataFactory.createAddress(2L, userId);
        newest.setIsDefault(0);
        newest.setCreateTime(LocalDateTime.now());

        Address earliest = TestDataFactory.createAddress(3L, userId);
        earliest.setIsDefault(0);
        earliest.setCreateTime(LocalDateTime.now().minusDays(1));

        when(addressMapper.selectById(addressId)).thenReturn(deletedDefault);
        when(addressMapper.selectListByUserId(userId)).thenReturn(Arrays.asList(newest, earliest));

        // When
        assertDoesNotThrow(() -> addressService.deleteAddress(userId, addressId));

        // Then
        verify(addressMapper).deleteById(addressId);
        verify(addressMapper).setDefault(earliest.getId(), userId);
    }

    @Test
    @DisplayName("删除地址失败 - 地址不存在")
    void deleteAddress_NotFound() {
        // Given
        Long userId = 1L;
        Long addressId = 999L;

        when(addressMapper.selectById(addressId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            addressService.deleteAddress(userId, addressId);
        });

        assertEquals(ResponseCodeEnum.ADDRESS_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("删除地址失败 - 无权操作")
    void deleteAddress_NoPermission() {
        // Given
        Long userId = 1L;
        Long addressId = 1L;
        Address address = TestDataFactory.createAddress(addressId, 2L); // 属于用户2

        when(addressMapper.selectById(addressId)).thenReturn(address);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            addressService.deleteAddress(userId, addressId);
        });

        assertEquals(ResponseCodeEnum.FORBIDDEN.getCode(), exception.getCode());
    }

    // ==================== 查询地址列表测试 ====================

    @Test
    @DisplayName("获取地址列表成功")
    void getAddressList_Success() {
        // Given
        Long userId = 1L;
        Address address1 = TestDataFactory.createAddress(1L, userId);
        address1.setIsDefault(1);
        Address address2 = TestDataFactory.createAddress(2L, userId);
        address2.setIsDefault(0);

        when(addressMapper.selectListByUserId(userId)).thenReturn(Arrays.asList(address1, address2));

        // When
        List<AddressResponse> list = addressService.getAddressList(userId);

        // Then
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getAddressId()); // 默认地址排在首位
        assertEquals(1, list.get(0).getIsDefault());
    }

    @Test
    @DisplayName("获取地址列表 - 空列表")
    void getAddressList_Empty() {
        // Given
        Long userId = 1L;

        when(addressMapper.selectListByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        List<AddressResponse> list = addressService.getAddressList(userId);

        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== 设置默认地址测试 ====================

    @Test
    @DisplayName("设置默认地址成功")
    void setDefaultAddress_Success() {
        // Given
        Long userId = 1L;
        Long addressId = 2L;
        Address address = TestDataFactory.createAddress(addressId, userId);
        address.setIsDefault(0);

        when(addressMapper.selectById(addressId)).thenReturn(address);

        // When
        assertDoesNotThrow(() -> addressService.setDefaultAddress(userId, addressId));

        // Then
        verify(addressMapper).cancelDefaultByUserId(userId);
        verify(addressMapper).setDefault(addressId, userId);
    }

    @Test
    @DisplayName("设置默认地址 - 已经是默认地址")
    void setDefaultAddress_AlreadyDefault() {
        // Given
        Long userId = 1L;
        Long addressId = 1L;
        Address address = TestDataFactory.createAddress(addressId, userId);
        address.setIsDefault(1);

        when(addressMapper.selectById(addressId)).thenReturn(address);

        // When
        assertDoesNotThrow(() -> addressService.setDefaultAddress(userId, addressId));

        // Then
        verify(addressMapper, never()).cancelDefaultByUserId(userId);
        verify(addressMapper, never()).setDefault(anyLong(), anyLong());
    }

    @Test
    @DisplayName("设置默认地址失败 - 地址不存在")
    void setDefaultAddress_NotFound() {
        // Given
        Long userId = 1L;
        Long addressId = 999L;

        when(addressMapper.selectById(addressId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            addressService.setDefaultAddress(userId, addressId);
        });

        assertEquals(ResponseCodeEnum.ADDRESS_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("设置默认地址失败 - 无权操作")
    void setDefaultAddress_NoPermission() {
        // Given
        Long userId = 1L;
        Long addressId = 1L;
        Address address = TestDataFactory.createAddress(addressId, 2L); // 属于用户2

        when(addressMapper.selectById(addressId)).thenReturn(address);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            addressService.setDefaultAddress(userId, addressId);
        });

        assertEquals(ResponseCodeEnum.FORBIDDEN.getCode(), exception.getCode());
    }

    // ==================== 获取默认地址测试 ====================

    @Test
    @DisplayName("获取默认地址成功")
    void getDefaultAddress_Success() {
        // Given
        Long userId = 1L;
        Address defaultAddress = TestDataFactory.createAddress(1L, userId);
        defaultAddress.setIsDefault(1);
        Address otherAddress = TestDataFactory.createAddress(2L, userId);
        otherAddress.setIsDefault(0);

        when(addressMapper.selectListByUserId(userId)).thenReturn(Arrays.asList(defaultAddress, otherAddress));

        // When
        AddressResponse response = addressService.getDefaultAddress(userId);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getAddressId());
        assertEquals(1, response.getIsDefault());
    }

    @Test
    @DisplayName("获取默认地址 - 无默认地址")
    void getDefaultAddress_None() {
        // Given
        Long userId = 1L;
        Address address = TestDataFactory.createAddress(1L, userId);
        address.setIsDefault(0);

        when(addressMapper.selectListByUserId(userId)).thenReturn(Collections.singletonList(address));

        // When
        AddressResponse response = addressService.getDefaultAddress(userId);

        // Then
        assertNull(response);
    }

    @Test
    @DisplayName("获取默认地址 - 无地址")
    void getDefaultAddress_Empty() {
        // Given
        Long userId = 1L;

        when(addressMapper.selectListByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        AddressResponse response = addressService.getDefaultAddress(userId);

        // Then
        assertNull(response);
    }
}
