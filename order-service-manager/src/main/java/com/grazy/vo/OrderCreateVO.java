package com.grazy.vo;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateVO {
    /**
     * 用户id
     */
    private Integer accountId;

    /**
     * 地址
     */
    private String address;

    /**
     * 产品id
     */
    private Integer productId;
}