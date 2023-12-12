package com.grazy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grazy.Enum.OrderStatus;
import com.grazy.GrazyMQ.sender.TransMessageSender;
import com.grazy.dao.OrderDetailDao;
import com.grazy.dto.OrderMessageDTO;
import com.grazy.pojo.OrderDetailPO;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.grazy.Enum.OrderStatus.ORDER_CREATED;
import static com.grazy.constant.RabbitmqConstant.RABBITMQ_HOST;

/**
 * @Author: grazy
 * @Date: 2023-11-07 15:04
 * @Description: 处理订单消息队列业务
 */

@Service
@Slf4j
public class OrderMessageService extends com.grazy.GrazyMQ.listener.AbstractMessageListener {

    @Resource
    private OrderDetailDao orderDetailDao;

    @Resource
    private TransMessageSender transMessageSender;

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理消息业务
     * @param message 消息
     */
    @Override
    public void receiveMessage(Message message) {
        //将收到的消息由字节类型转换为字符串
        String messageBody = new String(message.getBody());
        log.info("接收商家发过来的消息: {}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(RABBITMQ_HOST);

        try {
            //消息转为对象类型
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);
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
                        transMessageSender.send(
                                "exchange.order.deliveryman",
                                "key.deliveryman",
                                orderMessageDTO
                        );
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
                        transMessageSender.send(
                                "exchange.order.settlement",
                                "key.settlement",
                                orderMessageDTO
                        );
                    } else {
                        dbOrderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(dbOrderDetailPO);
                    }
                    break;


                case DELIVERYMAN_CONFIRMED:
                    //结算服务端返回的消息
                    if (orderMessageDTO.getSettlementId() != null) {
                        //结算服务已确认
                        dbOrderDetailPO.setSettlementId(orderMessageDTO.getSettlementId());
                        //修改订单为 “结算确认”
                        dbOrderDetailPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderDetailDao.update(dbOrderDetailPO);
                        //先积分服务端发送消息
                        transMessageSender.send(
                                "exchange.order.reward",
                                "key.reward",
                                orderMessageDTO
                        );
                    } else {
                        dbOrderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(dbOrderDetailPO);
                    }
                    break;


                case SETTLEMENT_CONFIRMED:
                    //积分服务端返回的消息
                    if (orderMessageDTO.getRewardId() != null) {
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

