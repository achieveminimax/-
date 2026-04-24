package com.seckill.common.enums;

import lombok.Getter;

/**
 * 秒杀记录状态枚举
 */
@Getter
public enum SeckillRecordStatusEnum {

    QUEUING(0, "排队中"),
    SUCCESS(1, "秒杀成功"),
    FAILED(2, "秒杀失败");

    private final Integer code;
    private final String desc;

    SeckillRecordStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static SeckillRecordStatusEnum getByCode(Integer code) {
        for (SeckillRecordStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
