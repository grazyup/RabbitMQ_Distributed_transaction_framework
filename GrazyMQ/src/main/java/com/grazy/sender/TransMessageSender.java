package com.grazy.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grazy.pojo.TransMessagePojo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Author: grazy
 * @Date: 2023-11-29 15:52
 * @Description: 业务发送消息调用
 */

@Component
@Slf4j
public class TransMessageSender {

    @Resource
    private TransMessageService transMessageService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void send(String exchange, String routingKey, Object messageBody){
        try {
            String messageBodyStr = new ObjectMapper().writeValueAsString(messageBody);
            //暂存消息到数据库
            TransMessagePojo transMessagePojo = transMessageService.messageSendReady(exchange, routingKey, messageBodyStr);
            //配置消息
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            Message message = new Message(messageBodyStr.getBytes(), messageProperties);
            //发送消息
            rabbitTemplate.convertAndSend(exchange,routingKey,message,new CorrelationData(transMessagePojo.getId()));
        }catch (Exception ex){
            log.error(ex.getMessage(),ex);
        }
    }

}
