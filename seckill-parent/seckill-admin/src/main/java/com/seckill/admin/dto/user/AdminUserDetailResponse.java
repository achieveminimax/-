package com.seckill.admin.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端用户详情响应。
 */
@Data
public class AdminUserDetailResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private Integer status;
    private String statusDesc;
    private Long orderCount;
    private BigDecimal totalAmount;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
