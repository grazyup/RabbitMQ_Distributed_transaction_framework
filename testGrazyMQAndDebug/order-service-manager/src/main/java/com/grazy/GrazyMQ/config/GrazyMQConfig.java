package com.grazy.GrazyMQ.config;

import com.grazy.GrazyMQ.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @Author: grazy
 * @Date: 2023-11-29 11:04
 * @Description: 自定义分布式事务框架配置类
 */

@Configuration
@Slf4j
public class GrazyMQConfig {

    @Value("${MQ.host}")
    private String host;

    @Value("${MQ.vhost}")
    private String vhost;

    @Value("${MQ.username}")
    private String username;

    @Value("${MQ.password}")
    private String password;

    @Value("${MQ.port}")
    private int port;

    @Resource
    private TransMessageService transMessageService;

    /**
     * 配置连接
     */
    @Bean
    public ConnectionFactory connectionFactory(){
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setVirtualHost(vhost);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setPort(port);
        //开启消息确认机制
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        //开启消息返回机制
        connectionFactory.setPublisherReturns(true);
        connectionFactory.createConnection();
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }


    /**
     * 配置消息发送
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        //配置消息托管
        rabbitTemplate.setMandatory(true);

        //配置消息确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            //删除成功确认的数据库中的消息
            if(correlationData != null && ack){
                String messageId = correlationData.getId();
                log.info("消息已经正确投递到交换机， id:{}", messageId);
                transMessageService.messageSendSuccess(messageId);
            }else{
                //失败，消息仍然会保存到数据库，然后交给定时任务定时重新扫描发送
                log.error("消息投递至交换机失败，correlationData:{}", correlationData);
            }
        });

        //配置返回回调 --> 路由失败,但是已被确认删除，需要重新持久化
        rabbitTemplate.setReturnsCallback(returned -> {
            //重新持久化消息
            log.error("消息无法路由！info:{}",returned);
            transMessageService.messageSentReturn(returned.getExchange()
                    ,returned.getRoutingKey(),new String(returned.getMessage().getBody()));
        });
        return rabbitTemplate;
    }


    /**
     * 配置消息监听
     */
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(ConnectionFactory connectionFactory){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
