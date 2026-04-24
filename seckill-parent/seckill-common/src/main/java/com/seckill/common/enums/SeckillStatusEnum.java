package com.seckill.common.enums;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 秒杀活动状态枚举
 */
@Getter
public enum SeckillStatusEnum {

    NOT_START(0, "未开始"),
    ONGOING(1, "进行中"),
    ENDED(2, "已结束"),
    OFFLINE(3, "已下架");

    private final Integer code;
    private final String desc;

    SeckillStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static SeckillStatusEnum getByCode(Integer code) {
        for (SeckillStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据时间计算活动状态
     */
    public static SeckillStatusEnum calculateStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return NOT_START;
        } else if (now.isAfter(endTime)) {
            return ENDED;
        } else {
            return ONGOING;
        }
    }
}
