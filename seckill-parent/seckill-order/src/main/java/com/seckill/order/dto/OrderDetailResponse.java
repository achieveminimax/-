package com.seckill.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情响应
 */
@Data
public class OrderDetailResponse {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片
     */
    private String goodsImage;

    /**
     * 秒杀价格
     */
    private BigDecimal orderPrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 订单状态描述
     */
    private String statusDesc;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 收货地址快照
     */
    private AddressSnapshot addressSnapshot;

    /**
     * 快递公司
     */
    private String expressCompany;

    /**
     * 快递单号
     */
    private String expressNo;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 支付截止时间
     */
    private LocalDateTime payDeadline;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 收货地址快照内部类
     */
    @Data
    public static class AddressSnapshot {
        /**
         * 收货人姓名
         */
        private String receiverName;

        /**
         * 收货人电话
         */
        private String receiverPhone;

        /**
         * 省
         */
        private String province;

        /**
         * 市
         */
        private String city;

        /**
         * 区
         */
        private String district;

        /**
         * 详细地址
         */
        private String detailAddress;

        /**
         * 完整地址字符串
         */
        public String getFullAddress() {
            return String.format("%s%s%s%s", province, city, district, detailAddress);
        }
    }
}
