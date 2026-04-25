package com.seckill.goods.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树响应体。
 * <p>
 * 递归 children 结构让前端可以直接渲染树状分类。
 */
@Data
public class CategoryTreeResponse {
    /** 分类 ID。 */
    private Long categoryId;
    /** 分类名称。 */
    private String categoryName;
    /** 分类图标。 */
    private String icon;
    /** 排序值。 */
    private Integer sort;
    /** 父分类 ID。 */
    private Long parentId;
    /** 子分类列表。 */
    private List<CategoryTreeResponse> children = new ArrayList<>();
}
