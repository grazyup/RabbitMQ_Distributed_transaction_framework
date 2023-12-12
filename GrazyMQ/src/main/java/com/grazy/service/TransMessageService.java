package com.grazy.service;

import com.grazy.pojo.TransMessagePojo;

import java.util.List;

/**
 * @Author: grazy
 * @Date: 2023-11-29 15:19
 * @Description: 跨消息业务层
 */

public interface TransMessageService {

    /**
     * 发送前暂存消息（持久化）
     */
    TransMessagePojo messageSendReady(String exchange, String routingKey, String messageBody);


    /**
     * 消息发送成功
     * @param id 消息Id
     */
    void messageSendSuccess(String id);


    /**
     * 消息返回（路由失败，重新持久化）
     */
    TransMessagePojo messageSentReturn(String exchange, String routingKey, String messageBody);


    /**
     * 查询应发未发的消息
     */
    List<TransMessagePojo> listReadyMessage();


    /**
     * 记录消息发送次数
     * @param id 消息Id
     */
    void messageResendNumber(String id);


    /**
     * 重发多次，放弃（修改消息状态）
     * @param id 消息Id
     */
    void messageDead(String id);


    /**
     * 消息消费前保存
     */
    TransMessagePojo messageReceiveReady(String id, String exchange, String queue, String routingKey, String messageBody);


    /**
     * 消息消费成功
     */
    void messagesReceiveSuccess(String id);


    /**
     * 保存监听到的死信消息
     */
    void messageDead(String messageId, String receivedExchange, String receivedRoutingKey, String consumerQueue, String messageBody);
}
