package com.grazy.GrazyMQ.listener;

import com.grazy.GrazyMQ.service.TransMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Author: grazy
 * @Date: 2023-12-01 0:35
 * @Description: 死信监听
 */

@Component
@ConditionalOnProperty("MQ.dlxEnabled")   //配置监听死信
@Slf4j
public class DlxListener implements ChannelAwareMessageListener {

    @Resource
    private TransMessageService transMessageService;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String messageBody = new String(message.getBody());
        log.error("dead letter! message:{}", message);
        //发邮件、打电话、发短信
        //XXXXX（）
        MessageProperties messageProperties = message.getMessageProperties();
        transMessageService.messageDead(
                messageProperties.getMessageId(),
                messageProperties.getReceivedExchange(),
                messageProperties.getReceivedRoutingKey(),
                messageProperties.getConsumerQueue(),
                messageBody
        );
        //死信队列确认消费消息,接收消息
        channel.basicAck(messageProperties.getDeliveryTag(),false);
    }
}
