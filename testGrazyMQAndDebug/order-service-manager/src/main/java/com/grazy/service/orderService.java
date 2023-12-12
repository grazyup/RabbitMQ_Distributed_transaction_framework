package com.grazy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grazy.Enum.OrderStatus;
import com.grazy.GrazyMQ.sender.TransMessageSender;
import com.grazy.dao.OrderDetailDao;
import com.grazy.dto.OrderMessageDTO;
import com.grazy.pojo.OrderDetailPO;
import com.grazy.vo.OrderCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @Author: grazy
 * @Date: 2023-11-07 15:03
 * @Description: 处理用户关于订单的业务请求
 */

@Service
@Slf4j
public class orderService {

    @Autowired
    private OrderDetailDao orderDetailDao;

    @Autowired
    TransMessageSender transMessageSender;

    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException, InterruptedException {
        log.info("createOrder:orderCreateVO:{}", orderCreateVO);
        OrderDetailPO orderPO = new OrderDetailPO();
        orderPO.setAddress(orderCreateVO.getAddress());
        orderPO.setAccountId(orderCreateVO.getAccountId());
        orderPO.setProductId(orderCreateVO.getProductId());
        orderPO.setStatus(OrderStatus.ORDER_CREATING);
        orderPO.setDate(new Date());
        orderDetailDao.insert(orderPO);

        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderPO.getId());
        orderMessageDTO.setProductId(orderPO.getProductId());
        orderMessageDTO.setAccountId(orderCreateVO.getAccountId());

        String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);

        transMessageSender.send(
                "exchange.order.restaurant",
                "key.restaurant",
                messageToSend);

        log.info("message sent");

        Thread.sleep(1000);

    }
}
