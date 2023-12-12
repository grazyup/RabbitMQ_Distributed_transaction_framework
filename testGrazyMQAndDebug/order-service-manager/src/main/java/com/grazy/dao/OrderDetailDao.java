package com.grazy.dao;

import com.grazy.pojo.OrderDetailPO;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * @Author: grazy
 * @Date: 2023-11-07 14:45
 * @Description: 订单详情
 */
@Mapper
@Repository
public interface OrderDetailDao {

    @Insert("INSERT INTO order_detail (status, address, account_id, product_id, deliveryman_id, settlement_id, " +
            "reward_id, price, date) VALUES(#{status}, #{address},#{accountId},#{productId},#{deliverymanId}," +
            "#{settlementId}, #{rewardId},#{price}, #{date})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrderDetailPO orderDetailPO);

    @Update("update order_detail set status =#{status}, address =#{address}, account_id =#{accountId}, " +
            "product_id =#{productId}, deliveryman_id =#{deliverymanId}, settlement_id =#{settlementId}, " +
            "reward_id =#{rewardId}, price =#{price}, date =#{date} where id=#{id}")
    void update(OrderDetailPO orderDetailPO);

    @Select("SELECT id,status,address,account_id accountId, product_id productId,deliveryman_id deliverymanId," +
            "settlement_id settlementId,reward_id rewardId,price, date FROM order_detail WHERE id = #{id}")
    OrderDetailPO selectOrder(Integer id);
}