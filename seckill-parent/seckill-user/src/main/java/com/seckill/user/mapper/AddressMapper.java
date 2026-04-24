package com.seckill.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.user.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 收货地址 Mapper 接口
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    /**
     * 根据用户ID查询地址列表
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    @Select("SELECT * FROM t_address WHERE user_id = #{userId} ORDER BY is_default DESC, create_time DESC")
    List<Address> selectListByUserId(@Param("userId") Long userId);

    /**
     * 统计用户地址数量
     *
     * @param userId 用户ID
     * @return 地址数量
     */
    @Select("SELECT COUNT(*) FROM t_address WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 取消用户所有默认地址
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE t_address SET is_default = 0 WHERE user_id = #{userId}")
    int cancelDefaultByUserId(@Param("userId") Long userId);

    /**
     * 设置默认地址
     *
     * @param addressId 地址ID
     * @param userId    用户ID
     * @return 影响行数
     */
    @Update("UPDATE t_address SET is_default = 1 WHERE id = #{addressId} AND user_id = #{userId}")
    int setDefault(@Param("addressId") Long addressId, @Param("userId") Long userId);
}
