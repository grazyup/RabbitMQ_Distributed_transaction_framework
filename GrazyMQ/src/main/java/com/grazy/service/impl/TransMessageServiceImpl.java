package com.grazy.service.impl;

import com.grazy.Dao.TransMessageDao;
import com.grazy.enumerattion.TransMessageEnum;
import com.grazy.pojo.TransMessagePojo;
import com.grazy.service.TransMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @Author: grazy
 * @Date: 2023-11-29 15:31
 * @Description: 业务实层
 */

@Service
public class TransMessageServiceImpl implements TransMessageService {

    @Value("${MQ.service}")
    private String serviceName;

    @Resource
    private TransMessageDao transMessageDao;


    @Override
    public TransMessagePojo messageSendReady(String exchange, String routingKey, String messageBody) {
        TransMessagePojo transMessagePojo = new TransMessagePojo();
        //生成id
        final String messageId = UUID.randomUUID().toString();
        transMessagePojo.setId(messageId);
        transMessagePojo.setService(serviceName);
        transMessagePojo.setExchange(exchange);
        transMessagePojo.setRoutingKey(routingKey);
        transMessagePojo.setPayload(messageBody);
        transMessagePojo.setSequence(0);
        transMessagePojo.setType(TransMessageEnum.SEND);
        transMessagePojo.setDate(new Date());
        //持久化
        transMessageDao.insert(transMessagePojo);
        return transMessagePojo;
    }


    @Override
    public void messageSendSuccess(String id) {
        //删除对应的持久化消息
        transMessageDao.delete(id,serviceName);
    }


    @Override
    public TransMessagePojo messageSentReturn(String exchange, String routingKey, String messageBody) {
        return this.messageSendReady(exchange,routingKey,messageBody);
    }


    @Override
    public List<TransMessagePojo> listReadyMessage() {
        return transMessageDao.selectByTypeAndService(TransMessageEnum.SEND.toString(),serviceName);
    }


    @Override
    public void messageResendNumber(String id) {
        //获取数据库消息对象
        TransMessagePojo transMessagePojo = transMessageDao.selectByIdAndService(id, serviceName);
        transMessagePojo.setSequence(transMessagePojo.getSequence() + 1);
        //更细数据库
        transMessageDao.update(transMessagePojo);
    }


    @Override
    public void messageDead(String id) {
        //获取数据库消息对象
        TransMessagePojo transMessagePojo = transMessageDao.selectByIdAndService(id, serviceName);
        transMessagePojo.setType(TransMessageEnum.DEAD);
        //更细数据库
        transMessageDao.update(transMessagePojo);
    }

    @Override
    public void messageDead(String messageId, String receivedExchange, String receivedRoutingKey, String consumerQueue, String messageBody) {
        TransMessagePojo transMessagePO = new TransMessagePojo();
        transMessagePO.setId(messageId);
        transMessagePO.setService(serviceName);
        transMessagePO.setExchange(receivedExchange);
        transMessagePO.setRoutingKey(receivedRoutingKey);
        transMessagePO.setQueue(consumerQueue);
        transMessagePO.setPayload(messageBody);
        transMessagePO.setDate(new Date());
        transMessagePO.setSequence(0);
        transMessagePO.setType(TransMessageEnum.DEAD);
        transMessageDao.insert(transMessagePO);
    }


    @Override
    public TransMessagePojo messageReceiveReady(String id, String exchange,
                                                String queue, String routingKey, String messageBody) {
        //判断数据库中是否已存在数据
        TransMessagePojo transMessagePojo = transMessageDao.selectByIdAndService(id, serviceName);
        if(transMessagePojo == null) {
            transMessagePojo = new TransMessagePojo();
            transMessagePojo.setId(id);
            transMessagePojo.setExchange(exchange);
            transMessagePojo.setType(TransMessageEnum.RECEIVE);
            transMessagePojo.setQueue(queue);
            transMessagePojo.setRoutingKey(routingKey);
            transMessagePojo.setSequence(0);
            transMessagePojo.setDate(new Date());
            //暂存到数据库
            transMessageDao.insert(transMessagePojo);
        }else {
            //更新消费次数
            transMessagePojo.setSequence(transMessagePojo.getSequence() + 1);
            transMessageDao.update(transMessagePojo);
        }
        return transMessagePojo;
    }


    @Override
    public void messagesReceiveSuccess(String id) {
        transMessageDao.delete(id,serviceName);
    }

}
