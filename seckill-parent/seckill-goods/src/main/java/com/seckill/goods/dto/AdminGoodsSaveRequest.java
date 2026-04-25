package com.seckill.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端商品新增/修改请求体。
 */
@Data
public class AdminGoodsSaveRequest {
    /** 商品名称。 */
    @NotBlank(message = "商品名称不能为空")
    @Size(min = 2, max = 100, message = "商品名称长度必须在2-100位之间")
    private String goodsName;

    /** 分类 ID，要求是二级分类。 */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /** 商品简介。 */
    @Size(max = 500, message = "商品简介长度不能超过500位")
    private String description;

    /** 商品详情富文本。 */
    private String detail;

    /** 商品原价。 */
    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;

    /** 库存数量。 */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    /** 封面图 URL。 */
    @Size(max = 256, message = "封面图长度不能超过256位")
    private String coverImage;

    /** 商品轮播图列表。 */
    private List<String> goodsImages;

    /** 商品状态：0-下架，1-上架。 */
    private Integer status;
}
