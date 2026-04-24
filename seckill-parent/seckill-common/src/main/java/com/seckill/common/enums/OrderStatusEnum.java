package com.seckill.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatusEnum {

    INIT(0, "初始化"),
    PENDING_PAY(1, "待支付"),
    PAID(2, "已支付"),
    SHIPPED(3, "已发货"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消"),
    REFUNDED(6, "已退款");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否允许支付
     */
    public static boolean allowPay(Integer code) {
        return PENDING_PAY.getCode().equals(code);
    }

    /**
     * 判断是否允许取消
     */
    public static boolean allowCancel(Integer code) {
        return PENDING_PAY.getCode().equals(code);
    }

    /**
     * 判断是否允许发货
     */
    public static boolean allowShip(Integer code) {
        return PAID.getCode().equals(code);
    }
}
