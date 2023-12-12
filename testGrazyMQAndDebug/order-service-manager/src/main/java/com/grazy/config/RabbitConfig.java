package com.grazy.config;

import com.grazy.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @Author: grazy
 * @Date: 2023-11-08 16:17
 * @Description: 使用配置类在项目启动后加载调用handleMessage()方法, 使得服务启动就开始处理消息
 */

@Slf4j
@Configuration
public class RabbitConfig {

    @Resource
    private OrderMessageService orderMessageService;



    @Bean
    public Exchange exchange1() {
        return new DirectExchange("exchange.order.restaurant");
    }

    @Bean
    public Queue queue1() {
        return new Queue("queue.order");
    }

    @Bean
    public Binding binding1() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.restaurant",
                "key.order",
                null);
    }

    /*---------------------deliveryman---------------------*/
    @Bean
    public Exchange exchange2() {
        return new DirectExchange("exchange.order.deliveryman");
    }

    @Bean
    public Binding binding2() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.deliveryman",
                "key.order",
                null);
    }


    /*---------settlement---------*/
    @Bean
    public Exchange exchange3() {
        return new FanoutExchange("exchange.order.settlement");
    }

    @Bean
    public Exchange exchange4() {
        return new FanoutExchange("exchange.settlement.order");
    }

    @Bean
    public Binding binding3() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.settlement.order",
                "key.order",
                null);
    }

    /*--------------reward----------------*/
    @Bean
    public Exchange exchange5() {
        return new TopicExchange("exchange.order.reward");
    }

    @Bean
    public Binding binding4() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.reward",
                "key.order",
                null);
    }


    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory, OrderMessageService orderMessageService){
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames("queue.order");
        container.setExposeListenerChannel(true);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        //监听并处理业务
        container.setMessageListener(orderMessageService);
        return container;
    }


}
