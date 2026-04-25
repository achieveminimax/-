package com.seckill.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.goods.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类表 Mapper。
 * <p>
 * 继承 {@link BaseMapper} 后，通用的单表 CRUD 能力都可直接复用。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
