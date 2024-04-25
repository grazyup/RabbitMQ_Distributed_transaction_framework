package com.grazy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grazy.Enum.OrderStatus;
import com.grazy.dao.OrderDetailDao;
import com.grazy.dto.OrderMessageDTO;
import com.grazy.pojo.OrderDetailPO;
import com.grazy.vo.OrderCreateVO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @Author: grazy
 * @Date: 2023-11-07 15:03
 * @Description: 处理用户关于订单的业务请求
 */

@Service
public class orderService {

    @Resource
    OrderDetailDao orderDetailDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //序列化映射
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建订单
     *
     * @param orderCreateVO 前端交互数据
     */
    public void insertOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        //创建订单对象
        OrderDetailPO orderDetailPO = new OrderDetailPO(
                orderCreateVO.getAccountId(),
                orderCreateVO.getProductId(),
                orderCreateVO.getAddress(),
                OrderStatus.ORDER_CREATING,
                new Date());
        //订单存入数据库
        orderDetailDao.insert(orderDetailPO);

        //创建订单的消息对象
        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderDetailPO.getId());
        orderMessageDTO.setProductId(orderCreateVO.getProductId());
        orderMessageDTO.setAccountId(orderCreateVO.getAccountId());
        //订单对象转换为字符类型
        String orderMessageDtoString = objectMapper.writeValueAsString(orderMessageDTO);



        //----------------使用RabbitTemplate封装类调用发送消息----------------------
        MessageProperties messageProperties = new MessageProperties();
        //设置过期时间
        messageProperties.setExpiration("15000");
        //设置发送消息内容及其属性
        Message message = new Message(orderMessageDtoString.getBytes(),messageProperties);
        //设置消息映射
        CorrelationData correlationData = new CorrelationData();
        //映射订单ID
        correlationData.setId(orderMessageDTO.getOrderId().toString());

        //这个发送方法优点是可以设置属性,缺点是需要手动转换消息的类型
        rabbitTemplate.send("exchange.order.restaurant","key.restaurant",message,correlationData);

        //这个发送方法的优点是不需要手动转换消息的类型, 缺点是设置属性较为麻烦 --> 最终接收信息端也是接收到Message类型的信息
        rabbitTemplate.convertAndSend("exchange.order.restaurant","key.restaurant",orderMessageDtoString,correlationData);





        //----------------使用原生方法调用发送消息-----------------------------
        //消息发送到商家服务端
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            //发送消息
            channel.basicPublish(
                    "exchange.order.restaurant",
                    "key.restaurant",
                    null,
                    orderMessageDtoString.getBytes()
            );
        }
    }
}
