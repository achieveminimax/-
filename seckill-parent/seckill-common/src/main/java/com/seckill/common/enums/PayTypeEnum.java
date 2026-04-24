package com.seckill.common.enums;

import lombok.Getter;

/**
 * 支付方式枚举
 */
@Getter
public enum PayTypeEnum {

    BALANCE(1, "余额支付"),
    ALIPAY(2, "支付宝"),
    WECHAT(3, "微信支付");

    private final Integer code;
    private final String desc;

    PayTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static PayTypeEnum getByCode(Integer code) {
        for (PayTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
