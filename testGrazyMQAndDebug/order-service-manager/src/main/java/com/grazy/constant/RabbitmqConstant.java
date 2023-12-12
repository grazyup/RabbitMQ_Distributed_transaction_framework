package com.grazy.constant;

/**
 * @Author: grazy
 * @Date: 2023-11-10 11:32
 * @Description: 消息队列参数常量
 */
public class RabbitmqConstant {

    public static final String RABBITMQ_HOST = "localhost";

    public static final String EXCHANGE_ORDER_REWARD = "exchange.order.reward";

    public static final String EXCHANGE_ORDER_RESTAURANT = "exchange.order.restaurant";

    public static final String EXCHANGE_ORDER_SETTLEMENT = "exchange.order.settlement";

    public static final String EXCHANGE_SETTLEMENT_ORDER = "exchange.settlement.order";

    public static final String EXCHANGE_ORDER_DELIVERYMAN = "exchange.order.deliveryMan";

    public static final String REWARD_ROUTING_KEY = "key.reward";

    public static final String SETTLEMENT_ROUTING_KEY = "key.settlement";

    public static final String DELIVERYMAN_ROUTING_KEY = "key.deliveryMan";

    public static final String ORDER_ROUTING_KEY = "key.order";

    public static final String ORDER_QUEUE = "queue.order";
}
