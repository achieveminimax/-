package com.seckill.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 用户信息更新请求 DTO
 */
@Data
public class UserUpdateRequest {

    /**
     * 昵称
     */
    @Size(max = 32, message = "昵称长度不能超过32位")
    private String nickname;

    /**
     * 性别：0-未知 1-男 2-女
     */
    @Pattern(regexp = "^[012]$", message = "性别格式不正确")
    private String gender;

    /**
     * 生日
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 邮箱
     */
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;
}
