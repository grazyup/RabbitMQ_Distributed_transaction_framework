package com.grazy.config;

import com.grazy.dto.OrderMessageDTO;
import com.grazy.service.OrderMessageService;
import com.grazy.service.advanceOrderMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

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

    @Resource
    private advanceOrderMessageService advanceOrderMessageService;


    /**
     * 初始化订单消息队列
     */
    @Autowired
    public void initRabbitMQ() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        //声明订单-餐厅交换机
        Exchange exchange = new DirectExchange("exchange.order.restaurant");
        rabbitAdmin.declareExchange(exchange);

        //声明订单队列
        Queue orderQueue = new Queue("queue.order");
        rabbitAdmin.declareQueue(orderQueue);

        Binding binding = new Binding("queue.order", Binding.DestinationType.QUEUE,
                "exchange.order.restaurant", "key.order", null);
        rabbitAdmin.declareBinding(binding);

    }


    /*
        ----------------------使用bean优化上面的声明方式----------------------------
        Exchange和binging、queue全部交于spring容器管理,不需要在declare了，直接调用就行,容器直接帮我们创建声明了；省去我们使用rabbitAdmin的创建方法
            项目启动spring不会一开始就帮我们声明这些定义的队列、交换机等,只有我们开始使用容器中的连接connectionFactor、使用RabbitAdmin时
            spring 才会帮我们声明出这些东西，与懒加载类似。只有使用才会创建，不使用不创建。
     */
    @Bean
    public Exchange exchange1() {
        return new DirectExchange("exchange.order.restaurant");
    }

    @Bean
    public Queue queue() {
        return new Queue("queue.order");
    }

    @Bean
    public Binding binding1() {
        return new Binding("queue.order", Binding.DestinationType.QUEUE,
                "exchange.order.restaurant", "key.order", null);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setVirtualHost("/");
        //这句代码的意义是帮使用上容器中的连接，让容器在启动时声明创建定义的队列和交换机 （激活）
        connectionFactory.createConnection();
        //开启消息发送确认机制(使用映射枚举 --> 映射发送消息的具体信息，要在发送消息时设置)
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        //开启发送者返回机制
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(@Autowired ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }




    /*=====================================发送消息==============================================*/

    @Bean
    public RabbitTemplate rabbitTemplate(@Autowired ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        //设置消息托管 --> 用于消息返回机制（是否被正确路由）
        rabbitTemplate.setMandatory(true);
        //设置消息发送确认机制 （确认回调） --> 有确认和不确认两种情况
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            log.info("correlationData:{}, ack:{}, cause:{}", correlationData, ack, cause);
        });
        //设置返回回调  --> 一定是发生异常，无法路由，但是已确认
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("RabbitMQ确认接收到，但是消息路由失败,没有找到目标队列 --> messageInfo:{}", returned);
        });
        return rabbitTemplate;
    }



    /******************* RabbitMQ消息监听(第一版本)****************************/

    /**
     * 消息监听容器用来监听队列接收到的消息
     */
    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(@Autowired ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        //设置监听的队列 --> 一次可以设置多个监听的队列
        messageListenerContainer.setQueueNames("queue.order");
        //设置并发的消费者的线程数量 --> 类似线程池，一个队列同时有多个消费者线程在消费/监听
        messageListenerContainer.setConcurrentConsumers(3);
        //设置最大并发消费者线程数量
        messageListenerContainer.setMaxConcurrentConsumers(5);


        /***************************消费端消息自动确认************************************/
        //设置消费端消息确认(自动确认)
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.AUTO);
        //监听到的消息进行业务处理 (监听到信息回调onMessage处理业务)
        messageListenerContainer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                log.info("这里面放着处理接收到的消息的业务代码  --> OrderMessageService类中的deliverCallback中的代码");
            }
        });


        /***************************消费端消息手动确认（第一版）************************************/
        //设置消费端消息确认（手动确认）
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        //监听到的消息进行业务处理
        messageListenerContainer.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                log.info("message:{}",message);
                //调用消息处理方法 --> 以前的方法
                //advanceOrderMessageService.handelMessage(message.getBody()/message);
                // (上面的handelMessage方法可用 需要修改advanceOrderMessageService中该方法的参数为byte[]类型,也可以是message类型)
                //显示手动确认
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        });


        /***************************优雅调用业务版本(功能与上面手动确认代码块内的功能一致)************************************/
        //内部会调用onMessage,再在onMessage方法内使用反射机制调用自定义的handleMessage方法,调用的默认方法名就是handelMessage（也可以自定义其他方法名）
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(advanceOrderMessageService);

        //如果不使用默认的handelMessage作为消息处理的方法名，需要配置一下
        Map<String, String> methodMap = new HashMap<>();
        // 这样写的好处可以实现 不同队列可以直接判断调用对应的业务方法，避免上面ChannelAwareMessageListener()那样自己判断，自己调用
        methodMap.put("消息队列的名称1","不规范的处理消息的方法名");
        methodMap.put("消息队列的名称2","不规范的处理消息的方法名");
        messageListenerAdapter.setQueueOrTagToMethodName(methodMap);

        //设置消息转换 --> 调用HandelMessage方法的时候参数自动转换为自定义类型，不使用默认的byte类型
        Jackson2JsonMessageConverter messageConverter = new Jackson2JsonMessageConverter();
        messageConverter.setClassMapper(new ClassMapper() {
            @Override
            public void fromClass(Class<?> aClass, MessageProperties messageProperties) {

            }

            @Override
            public Class<?> toClass(MessageProperties messageProperties) {
                //返回自定义需要转换的类型
                return OrderMessageDTO.class;
            }
        });
        //设置对象转换
        messageListenerAdapter.setMessageConverter(messageConverter);
        //将手动确认优雅版本的MessageListenerAdapter注入监听容器中 （选择优雅手动的就执行此代码）
        messageListenerContainer.setMessageListener(messageListenerAdapter);



        //设置消费端限流机制 --> 每次只推送2个ACK应答
        messageListenerContainer.setPrefetchCount(2);
        return messageListenerContainer;

    }




    /******************* RabbitListener的形式（是springboot架构中监听消息的‘终极方案’）****************************/

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(@Autowired ConnectionFactory connectionFactory){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        //在里面也可以设置与SimpleMessageListenerContainer一样的配置，工厂类生成与上面监听对象类似的对象
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(2);
        return factory;
    }


    /*
        注意：以上所有的bean都是我们自己手动添加的，但是如果在配置文件里面直接设置rabbitmq的连接，ConnectionFactory,
            RabbitAdmin, RabbitTemplate这些都会自动生成并交于spring容器管理no，就不需要要我们自己与上面那样自己声明添加到ioc容器。

            在框架中自带的RabbitListener注解中可以直接内部使用@Exahnge,@Queue,@QueueBinding注解就可以直接完成声明和绑定,
            也不需要同上面一样写队列和交换机，绑定的binding。

            自己定义的RabbitListenerContainerFactory<SimpleMessageListenerContainer>的bean,到时候会被自动注入，调用时可以用来创建
            SimpleMessageListenerContainer消费队列监听容器，然后可以同上面定义那样，在容里面进行各种配置

            备注： @RabbitListener注解和自定义的RabbitListener的bean是不一样的, 注解是AMQP框架提供，里面有很多封装好的声明和配置的方法
                注解，然而自定义的bean使用时要我们自动注入后，手动调用创建监听容器，然后才配置属性和声明队列，交换机等操作，为此 两个东西是不
                一样的东西；业务使用可以按照实际需求选择使用哪一个

     */
}
