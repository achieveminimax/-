package com.seckill.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址请求 DTO
 */
@Data
public class AddressRequest {

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(min = 2, max = 20, message = "收货人姓名长度必须在2-20位之间")
    private String receiverName;

    /**
     * 收货人电话
     */
    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    /**
     * 省
     */
    @NotBlank(message = "省份不能为空")
    private String province;

    /**
     * 市
     */
    @NotBlank(message = "城市不能为空")
    private String city;

    /**
     * 区
     */
    @NotBlank(message = "区县不能为空")
    private String district;

    /**
     * 详细地址
     */
    @NotBlank(message = "详细地址不能为空")
    @Size(min = 5, max = 200, message = "详细地址长度必须在5-200位之间")
    private String detailAddress;

    /**
     * 是否默认：0-否 1-是
     */
    private Integer isDefault;
}
