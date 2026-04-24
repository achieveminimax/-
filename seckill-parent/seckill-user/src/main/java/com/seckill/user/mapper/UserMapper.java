package com.seckill.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM t_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户实体
     */
    @Select("SELECT * FROM t_user WHERE phone = #{phone} AND deleted = 0")
    User selectByPhone(@Param("phone") String phone);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE phone = #{phone} AND deleted = 0")
    int countByPhone(@Param("phone") String phone);
}
