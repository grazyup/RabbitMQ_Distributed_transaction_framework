package com.grazy.controller;

import com.grazy.service.orderService;
import com.grazy.vo.OrderCreateVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Author: grazy
 * @Date: 2023-11-08 11:38
 * @Description: 订单控制层
 */
@RestController
@RequestMapping("/order")
public class orderController {

    @Resource
    private orderService orderService;

    @PostMapping("/add")
    public String addOrder(@RequestBody OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        orderService.insertOrder(orderCreateVO);
        return "添加成功";
    }

}
