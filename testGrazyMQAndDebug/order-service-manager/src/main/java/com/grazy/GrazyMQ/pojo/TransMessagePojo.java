package com.grazy.GrazyMQ.pojo;

import com.grazy.GrazyMQ.enumerattion.TransMessageEnum;
import lombok.Data;

import java.util.Date;

/**
 * @Author: grazy
 * @Date: 2023-11-29 11:23
 * @Description: 持久化消息对象
 */

@Data
public class TransMessagePojo {

    private String id;

    /**
     * 服务名称
     */
    private String service;

    /**
     * 消息状态枚举
     */
    private TransMessageEnum type;

    private String exchange;

    private String routingKey;

    private String queue;

    /**
     * 重发次数
     */
    private Integer sequence;

    /**
     * 消息内容
     */
    private String payload;

    private Date date;
}
