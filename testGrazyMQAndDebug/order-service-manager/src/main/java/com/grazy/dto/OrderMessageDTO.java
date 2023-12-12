package com.grazy.dto;

import com.grazy.Enum.OrderStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Data
public class OrderMessageDTO {

    /**
     * 订单ID
     */
    private Integer orderId;

    /**
     * 订单状态
     */
    private OrderStatus orderStatus;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 骑手Id
     */
    private Integer deliverymanId;

    /**
     * 产品id
     */
    private Integer productId;

    /**
     * 用户Id
     */
    private Integer accountId;

    /**
     * 结算Id
     */
    private Integer settlementId;

    /**
     * 积分Id
     */
    private Integer rewardId;

    /**
     * 积分余额
     */
    private BigDecimal rewardAmount;

    /**
     * 商家确认
     */
    private Boolean confirmed;
}