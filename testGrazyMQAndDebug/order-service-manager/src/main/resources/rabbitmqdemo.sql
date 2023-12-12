/*
 Navicat Premium Data Transfer

 Source Server         : MySQL80
 Source Server Type    : MySQL
 Source Server Version : 80030 (8.0.30)
 Source Host           : localhost:3306
 Source Schema         : rabbitmqdemo

 Target Server Type    : MySQL
 Target Server Version : 80030 (8.0.30)
 File Encoding         : 65001

 Date: 12/12/2023 08:55:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for deliveryman
-- ----------------------------
DROP TABLE IF EXISTS `deliveryman`;
CREATE TABLE `deliveryman`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '骑手id',
  `name` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '名称',
  `status` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态',
  `date` datetime NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of deliveryman
-- ----------------------------
INSERT INTO `deliveryman` VALUES (1, 'wangxiaoer', 'VALUABLE', '2020-06-10 20:30:17');

-- ----------------------------
-- Table structure for order_detail
-- ----------------------------
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '订单id',
  `status` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态',
  `address` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '订单地址',
  `account_id` int NULL DEFAULT NULL COMMENT '用户id',
  `product_id` int NULL DEFAULT NULL COMMENT '产品id',
  `deliveryman_id` int NULL DEFAULT NULL COMMENT '骑手id',
  `settlement_id` int NULL DEFAULT NULL COMMENT '结算id',
  `reward_id` int NULL DEFAULT NULL COMMENT '积分奖励id',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
  `date` datetime NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 433 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of order_detail
-- ----------------------------
INSERT INTO `order_detail` VALUES (432, 'ORDER_CREATED', '广东省', 1256, 2, 1, 571087981, 8, 23.25, '2023-11-12 12:44:26');

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '产品id',
  `name` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '名称',
  `price` decimal(9, 2) NULL DEFAULT NULL COMMENT '单价',
  `restaurant_id` int NULL DEFAULT NULL COMMENT '地址',
  `status` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态',
  `date` datetime NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (2, 'eqwe', 23.25, 1, 'VALUABLE', '2020-05-06 19:19:04');

-- ----------------------------
-- Table structure for restaurant
-- ----------------------------
DROP TABLE IF EXISTS `restaurant`;
CREATE TABLE `restaurant`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '餐厅id',
  `name` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '名称',
  `address` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '地址',
  `status` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态',
  `settlement_id` int NULL DEFAULT NULL COMMENT '结算id',
  `date` datetime NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of restaurant
-- ----------------------------
INSERT INTO `restaurant` VALUES (1, 'qeqwe', '2weqe', 'OPEN', 1, '2020-05-06 19:19:39');

-- ----------------------------
-- Table structure for reward
-- ----------------------------
DROP TABLE IF EXISTS `reward`;
CREATE TABLE `reward`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '奖励id',
  `order_id` int NULL DEFAULT NULL COMMENT '订单id',
  `amount` decimal(9, 2) NULL DEFAULT NULL COMMENT '积分量',
  `status` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态',
  `date` datetime NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of reward
-- ----------------------------
INSERT INTO `reward` VALUES (8, 432, 23.25, 'SUCCESS', '2023-11-12 12:46:18');

-- ----------------------------
-- Table structure for settlement
-- ----------------------------
DROP TABLE IF EXISTS `settlement`;
CREATE TABLE `settlement`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '结算id',
  `order_id` int NULL DEFAULT NULL COMMENT '订单id',
  `transaction_id` int NULL DEFAULT NULL COMMENT '交易id',
  `amount` decimal(9, 2) NULL DEFAULT NULL COMMENT '金额',
  `status` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态',
  `date` datetime NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1169 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of settlement
-- ----------------------------
INSERT INTO `settlement` VALUES (1168, 432, 571087981, 23.25, 'SUCCESS', '2023-11-12 12:45:43');

-- ----------------------------
-- Table structure for trans_message
-- ----------------------------
DROP TABLE IF EXISTS `trans_message`;
CREATE TABLE `trans_message`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `service` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `exchange` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `routing_Key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `queue` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sequence` int NULL DEFAULT NULL,
  `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `date` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`, `service`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of trans_message
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
