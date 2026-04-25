package com.seckill.goods.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动详情响应体。
 */
@Data
public class SeckillActivityDetailResponse {
    /** 活动 ID。 */
    private Long activityId;
    /** 活动名称。 */
    private String activityName;
    /** 活动图片。 */
    private String activityImg;
    /** 活动描述。 */
    private String description;
    /** 开始时间。 */
    private LocalDateTime startTime;
    /** 结束时间。 */
    private LocalDateTime endTime;
    /** 动态状态码。 */
    private Integer status;
    /** 动态状态文案。 */
    private String statusDesc;
    /** 规则文案列表。 */
    private List<String> rules;
    /** 活动下商品明细。 */
    private List<SeckillGoodsItemResponse> goodsList;
}
