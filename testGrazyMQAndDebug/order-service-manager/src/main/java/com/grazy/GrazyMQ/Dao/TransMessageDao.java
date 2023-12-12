package com.grazy.GrazyMQ.Dao;

import com.grazy.GrazyMQ.pojo.TransMessagePojo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @Author: grazy
 * @Date: 2023-11-29 11:30
 * @Description: 消息持久层
 */

@Mapper
public interface TransMessageDao {

    @Insert("insert into trans_message (id, type, service, exchange, routing_key, queue, sequence, payload,date) " +
            "VALUES (#{id},#{type},#{service},#{exchange},#{routingKey},#{queue},#{sequence},#{payload},#{date})")
    void insert(TransMessagePojo transMessagePojo);


    @Update("update trans_message set type=#{type},service=#{service}, exchange =#{exchange},routing_key =#{routingKey}, " +
            "queue =#{queue},sequence =#{sequence}, payload =#{payload}, date =#{date} " +
            "where id=#{id} and service=#{service}")
    void update(TransMessagePojo transMessagePojo);


    @Delete("DELETE FROM trans_message " +
            "where id=#{id} and service=#{service}")
    void delete(@Param("id") String id, @Param("service") String service);


    @Select("SELECT id, type, service, exchange,routing_key routingKey, queue, sequence,payload, date " +
            "FROM trans_message " +
            "where id=#{id} and service=#{service}")
    TransMessagePojo selectByIdAndService(@Param("id") String id, @Param("service") String service);


    @Select("SELECT id, type, service, exchange, routing_key routingKey, queue, sequence, payload, date " +
            "FROM trans_message " +
            "WHERE type = #{type} and service = #{service}")
    List<TransMessagePojo> selectByTypeAndService(@Param("type")String type, @Param("service")String service);

}
