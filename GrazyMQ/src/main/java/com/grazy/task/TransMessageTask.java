package com.grazy.task;

import com.grazy.pojo.TransMessagePojo;
import com.grazy.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: grazy
 * @Date: 2023-11-29 16:24
 * @Description: 定时任务--定时处理数据库中应发未发的消息
 */

@Configuration
@Slf4j
@EnableScheduling
@Component
public class TransMessageTask {

    @Value("${MQ.resendTimes}")
    private int resendTime;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private TransMessageService transMessageService;

    @Scheduled(fixedDelayString = "${MQ.resendFreq}")
    public void resentMessage(){
        //检索数据库中应发未发的消息
        List<TransMessagePojo> transMessagePojoList = transMessageService.listReadyMessage();
        for(TransMessagePojo element: transMessagePojoList){
            //判断消息是否达到重发的次数
            if(element.getSequence() > resendTime){
                log.error("resend too many times!");
                //标记消息为dead状态
                transMessageService.messageDead(element.getId());
                continue;
            }
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            Message message = new Message(element.getPayload().getBytes(), messageProperties);
            //执行重发消息
            rabbitTemplate.convertAndSend(element.getExchange(),element.getRoutingKey(),message,new CorrelationData(element.getId()));
            log.info("message sent, ID:{}", element.getId());
            //更新消息重发次数
            transMessageService.messageResendNumber(element.getId());
        }
    }
}
