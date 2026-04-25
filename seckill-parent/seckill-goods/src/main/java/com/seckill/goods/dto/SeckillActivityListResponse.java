package com.seckill.goods.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动列表响应体。
 */
@Data
public class SeckillActivityListResponse {
    /** 活动 ID。 */
    private Long activityId;
    /** 活动名称。 */
    private String activityName;
    /** 活动展示图。 */
    private String activityImg;
    /** 开始时间。 */
    private LocalDateTime startTime;
    /** 结束时间。 */
    private LocalDateTime endTime;
    /** 动态状态码。 */
    private Integer status;
    /** 动态状态文案。 */
    private String statusDesc;
    /** 活动下商品列表。 */
    private List<SeckillGoodsItemResponse> goodsList;
}
