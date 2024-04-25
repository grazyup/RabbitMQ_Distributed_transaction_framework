package com.grazy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grazy.Enum.OrderStatus;
import com.grazy.dao.OrderDetailDao;
import com.grazy.dto.OrderMessageDTO;
import com.grazy.pojo.OrderDetailPO;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.grazy.Enum.OrderStatus.ORDER_CREATED;
import static com.grazy.constant.RabbitmqConstant.*;

/**
 * @Author: grazy
 * @Date: 2023-11-07 15:04
 * @Description: 处理订单消息队列业务
 */

@Service
@Slf4j
public class OrderMessageService {

    @Resource
    private OrderDetailDao orderDetailDao;

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 声明消息队列、交换机、绑定、消息的处理  --> 可以结合AMQP协议图来看
     */
    @Async   //注解标注使用异步线程池的线程
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(RABBITMQ_HOST);

        //使用这样try写法，用完会自动close
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            //声明订单的队列
            channel.queueDeclare(ORDER_QUEUE, true, false, false, null);

            //声明订单-餐厅交换机
            channel.exchangeDeclare(EXCHANGE_ORDER_RESTAURANT, BuiltinExchangeType.DIRECT, true, false, null);

            //订单-餐厅交换机和订单队列进行绑定
            channel.queueBind(ORDER_QUEUE, EXCHANGE_ORDER_RESTAURANT, ORDER_ROUTING_KEY);

            //声明订单-骑手交换机
            channel.exchangeDeclare(EXCHANGE_ORDER_DELIVERYMAN, BuiltinExchangeType.DIRECT, true, false, null);

            //订单-骑手交换机和订单队列绑定
            channel.queueBind(ORDER_QUEUE, EXCHANGE_ORDER_DELIVERYMAN, ORDER_ROUTING_KEY);

            //声明接收结算服务消息交换机
            channel.exchangeDeclare(EXCHANGE_ORDER_SETTLEMENT, BuiltinExchangeType.FANOUT,true,false,null);

            //接收结算服务消息交换机交换机和订单队列绑定
            channel.queueBind(ORDER_QUEUE, EXCHANGE_ORDER_SETTLEMENT, SETTLEMENT_ROUTING_KEY,null);

            //声明积分服务交换机
            channel.exchangeDeclare(EXCHANGE_ORDER_REWARD,BuiltinExchangeType.TOPIC,true,false,null);

            //绑定订单队列和积分服务交换机
            channel.queueBind(ORDER_QUEUE, EXCHANGE_ORDER_REWARD, ORDER_ROUTING_KEY);

            //处理订单队列中接收到的消息（队列中一接收到信息就开始调用实现类的方法处理消息）
            channel.basicConsume(ORDER_QUEUE, true, deliverCallback, consumerTag -> {});
            //让当前线程不关闭，一直处理对列中的消息
            while (true) Thread.sleep(Integer.MAX_VALUE);

        }
    }


    /**
     * 处理接收到的消息实现类(实现DeliverCallback接口中的方法)
     */
    DeliverCallback deliverCallback = (consumerTag, message) -> {
        //将收到的消息由字节类型转换为字符串
        String messageBody = new String(message.getBody());
        log.info("接收商家发过来的消息: {}",messageBody);
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
                    }else {
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
                    }else{
                        dbOrderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(dbOrderDetailPO);
                    }
                    break;

            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);  // 参数是 错误信息 + 异常详细信息
        }
    };

}
