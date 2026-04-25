package com.seckill.admin.dto.seckill;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端秒杀活动创建/修改请求。
 */
@Data
public class AdminSeckillActivityRequest {

    private Long activityId;

    @NotBlank(message = "活动名称不能为空")
    @Size(min = 2, max = 50, message = "活动名称长度必须在2-50位之间")
    private String activityName;

    @Size(max = 500, message = "活动描述长度不能超过500位")
    private String description;

    private String activityImg;

    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;

    private List<String> rules;

    @NotEmpty(message = "秒杀商品列表不能为空")
    @Valid
    private List<AdminSeckillGoodsItemRequest> goodsList;
}
