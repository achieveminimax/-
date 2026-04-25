package com.seckill.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.admin.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员表 Mapper。
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
