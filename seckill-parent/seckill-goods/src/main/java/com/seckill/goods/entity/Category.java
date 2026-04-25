package com.seckill.goods.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体。
 * <p>
 * 通过 parentId 维护两级分类关系：
 * 一级分类的 parentId 通常为 0，二级分类则指向所属一级分类。
 */
@Data
@TableName("t_category")
public class Category {
    /** 分类主键。 */
    @TableId
    private Long id;

    /** 父分类 ID。 */
    @TableField("parent_id")
    private Long parentId;

    /** 分类名称。 */
    private String name;
    /** 分类图标 URL。 */
    private String icon;
    /** 排序值，值越小越靠前。 */
    private Integer sort;
    /** 状态：0-禁用，1-启用。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
