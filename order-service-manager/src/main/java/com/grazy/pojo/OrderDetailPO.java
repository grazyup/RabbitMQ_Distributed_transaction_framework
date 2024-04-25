package com.grazy.pojo;

import com.grazy.Enum.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailPO {

    public OrderDetailPO(Integer accountId, Integer productId, String address, OrderStatus status, Date date){
        this.address = address;
        this.accountId = accountId;
        this.productId = productId;
        this.status = status;
        this.date = date;
    }

    private Integer id;
    private OrderStatus status;
    private String address;
    private Integer accountId;
    private Integer productId;
    private Integer deliverymanId;
    private Integer settlementId;
    private Integer rewardId;
    private BigDecimal price;
    private Date date;
}