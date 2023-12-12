package com.grazy.GrazyMQ.listener;

import com.grazy.GrazyMQ.pojo.TransMessagePojo;
import com.grazy.GrazyMQ.service.TransMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @Author: grazy
 * @Date: 2023-11-30 23:30
 * @Description: 消息监听
 */

@Slf4j
public abstract class AbstractMessageListener implements ChannelAwareMessageListener {

    @Value("${MQ.resendTimes}")
    private int resendTimes;

    @Resource
    private TransMessageService transMessageService;

    public abstract void receiveMessage (Message message);

    @Override
    public void onMessage(Message message, Channel channel) throws IOException, InterruptedException {
        MessageProperties messageProperties = message.getMessageProperties();
        //消息的唯一标识
        long deliveryTag = messageProperties.getDeliveryTag();
        //消息暂存
        TransMessagePojo dbTransMessage = transMessageService.messageReceiveReady(messageProperties.getMessageId(), messageProperties.getReceivedExchange(),
                messageProperties.getConsumerQueue(), messageProperties.getReceivedRoutingKey(),
                new String(message.getBody()));
        log.info("收到消息{}, 消费次数{}", messageProperties.getMessageId(), dbTransMessage.getSequence());

        try {
            //调用业务
           this.receiveMessage(message);
           channel.basicAck(deliveryTag,false);
           transMessageService.messagesReceiveSuccess(dbTransMessage.getId());
        }catch (Exception e){
            log.error(e.getMessage(),e);
            if(dbTransMessage.getSequence() >= resendTimes){
                //超出重新消费的次数，消费端拒收，不重回队列，直接进去死信队列
                channel.basicReject(deliveryTag,false);
                //删除原先数据库中receive的消息
                transMessageService.messageSendSuccess(dbTransMessage.getId());
            } else {
                Thread.sleep((long) Math.pow(2, dbTransMessage.getSequence()) * 1000);
                //拒收，重回队列
                channel.basicNack(deliveryTag,false,true);
            }
        }

    }

}
