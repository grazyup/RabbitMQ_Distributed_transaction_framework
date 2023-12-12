package com.grazy;

import com.grazy.sender.TransMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.Resource;

@SpringBootTest
class GrazyMqApplicationTests {

    @Resource
    private TransMessageSender transMessageSender;

    /**
     * 测试分布式事务发送消息
     */
    @Test
    void contextLoads() {
        transMessageSender.send("exchange.order.restaurant","key.order","测试test001");
    }

}
