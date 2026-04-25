package com.seckill.admin.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端用户列表响应。
 */
@Data
public class AdminUserListResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private Integer status;
    private String statusDesc;
    private Long orderCount;
    private BigDecimal totalAmount;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
