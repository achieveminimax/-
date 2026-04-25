package com.seckill.admin.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端用户状态更新请求。
 */
@Data
public class AdminUserStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
