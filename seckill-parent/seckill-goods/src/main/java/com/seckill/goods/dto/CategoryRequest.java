package com.seckill.goods.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增/修改请求体。
 */
@Data
public class CategoryRequest {
    /** 父分类 ID，默认 0 代表一级分类。 */
    private Long parentId = 0L;

    /** 分类名称。 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称长度不能超过32位")
    private String name;

    /** 分类图标 URL。 */
    @Size(max = 256, message = "分类图标长度不能超过256位")
    private String icon;

    /** 排序值，值越小越靠前。 */
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort = 0;

    /** 分类状态：0-禁用，1-启用。 */
    @Min(value = 0, message = "分类状态非法")
    @Max(value = 1, message = "分类状态非法")
    private Integer status = 1;
}
