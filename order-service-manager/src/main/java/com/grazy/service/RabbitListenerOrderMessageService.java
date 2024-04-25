package com.grazy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grazy.Enum.OrderStatus;
import com.grazy.dao.OrderDetailDao;
import com.grazy.dto.OrderMessageDTO;
import com.grazy.pojo.OrderDetailPO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.grazy.Enum.OrderStatus.ORDER_CREATED;
import static com.grazy.constant.RabbitmqConstant.*;

/**
 * @Author: grazy
 * @Date: 2023-11-19 21:48
 * @Description:  监听消息 （使用注解的方式）
 */

@Service
@Slf4j
public class RabbitListenerOrderMessageService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private OrderDetailDao orderDetailDao;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(
                        containerFactory = "rabbitListenerContainerFactory",  //可以不用设置，使用属性配置的话，不用写；想要使用自己写的配置类里面的，就写
                        admin = "rabbitAdmin",                                //可以不用设置，使用属性配置的话，不用写； 想要使用自己写的配置类里面的，就写
            //            queues = "queue.order",
            bindings = {
                    @QueueBinding(
                            value = @Queue(
                                    name = "queue.order",
                                    autoDelete = "false",
                                    arguments = {
                                            @Argument(name = "x-message-ttl",value = "1000",type = "java.lang.Integer"),
                                            @Argument(name = "x-dead-letter-exchange",value = "dlx.exchange"),
                                            @Argument(name = "x-dead-letter-routing-key",value = "key.del")
                                    }
                            ),
                            exchange = @Exchange(name = "exchange.order.restaurant",type = ExchangeTypes.DIRECT),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(name = "queue.order"),
                            exchange = @Exchange(name = "exchange.order.deliveryman"),
                            key = "key.order"
                    )
            }
    )
    public void handelMessage(@Payload Message message) {
        log.info("接收商家发过来的消息: {}",new String(message.getBody()));
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(RABBITMQ_HOST);
        try {
            //消息转为对象类型
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
            //查询数据库中的订单数据
            OrderDetailPO dbOrderDetailPO = orderDetailDao.selectOrder(orderMessageDTO.getOrderId());

            //判断是哪个服务端返回的消息
            switch (dbOrderDetailPO.getStatus()) {

                case ORDER_CREATING:
                    //商家端返回的消息
                    if (orderMessageDTO.getConfirmed() && orderMessageDTO.getPrice() != null) {
                        //商家服务端确认成功,修改订单状态为 ”商家已确认“
                        dbOrderDetailPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        dbOrderDetailPO.setPrice(orderMessageDTO.getPrice());
                        //更新数据库
                        orderDetailDao.update(dbOrderDetailPO);
                        //向下一个服务端（骑手服务端）发送消息
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            //将消息发送对象转换为字符串
                            String messageSentTo = objectMapper.writeValueAsString(orderMessageDTO);
                            //向下一个服务端（骑手服务）发送消息
                            channel.basicPublish(EXCHANGE_ORDER_DELIVERYMAN, DELIVERYMAN_ROUTING_KEY,
                                    null, messageSentTo.getBytes()
                            );
                        }
                    } else {
                        //订单设置失败
                        dbOrderDetailPO.setStatus(OrderStatus.FAILED);
                        //更新数据库
                        orderDetailDao.update(dbOrderDetailPO);
                    }
                    break;


                case RESTAURANT_CONFIRMED:
                    //骑手服务端返回的消息
                    if (orderMessageDTO.getDeliverymanId() != null) {
                        dbOrderDetailPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());
                        //更新订单状态为 “骑手已确认"
                        dbOrderDetailPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        orderDetailDao.update(dbOrderDetailPO);
                        //向下一个服务端（结算服务）发送消息
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            channel.basicPublish(EXCHANGE_SETTLEMENT_ORDER, SETTLEMENT_ROUTING_KEY,
                                    null, objectMapper.writeValueAsBytes(orderMessageDTO));
                        }
                    } else {
                        dbOrderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(dbOrderDetailPO);
                    }
                    break;


                case DELIVERYMAN_CONFIRMED:
                    //结算服务端返回的消息
                    if(orderMessageDTO.getSettlementId() != null){
                        //结算服务已确认
                        dbOrderDetailPO.setSettlementId(orderMessageDTO.getSettlementId());
                        //修改订单为 “结算确认”
                        dbOrderDetailPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderDetailDao.update(dbOrderDetailPO);
                        //先积分服务端发送消息
                        try(Connection connection = connectionFactory.newConnection();
                            Channel channel = connection.createChannel()){
                            channel.basicPublish(EXCHANGE_ORDER_REWARD, REWARD_ROUTING_KEY,
                                    null, objectMapper.writeValueAsBytes(orderMessageDTO)
                            );
                        }
                    } else {
                        dbOrderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(dbOrderDetailPO);
                    }
                    break;


                case SETTLEMENT_CONFIRMED:
                    //积分服务端返回的消息
                    if(orderMessageDTO.getRewardId() != null){
                        dbOrderDetailPO.setRewardId(orderMessageDTO.getRewardId());
                        //修改订单状态为 “已创建”
                        dbOrderDetailPO.setStatus(ORDER_CREATED);
                        orderDetailDao.update(dbOrderDetailPO);
                    } else {
                        dbOrderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(dbOrderDetailPO);
                    }
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);  // 参数是 错误信息 + 异常详细信息
        }
    }

}
