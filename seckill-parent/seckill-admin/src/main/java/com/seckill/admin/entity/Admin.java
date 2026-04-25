package com.seckill.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员实体。
 */
@Data
@TableName("t_admin")
public class Admin {

    /** 管理员主键。 */
    @TableId
    private Long id;

    /** 登录账号。 */
    private String username;
    /** 加密后的登录密码。 */
    private String password;

    /** 管理员真实姓名。 */
    @TableField("real_name")
    private String realName;

    /** 角色标识。 */
    private String role;
    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 最后登录时间。 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
