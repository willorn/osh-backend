-- =====================================================
-- 工具模块表结构初始化/重建脚本
-- 说明：
-- 1. 执行前请先备份生产数据
-- 2. 本脚本会先删除旧表/同名表，再按当前代码结构重建
-- 3. 适用于工具模块全量初始化或测试环境重建
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 删除旧表 / 历史遗留表
-- =====================================================

DROP TABLE IF EXISTS `osh_user_tool_quota`;
DROP TABLE IF EXISTS `osh_tool_purchase_record`;

DROP TABLE IF EXISTS `osh_tool_user_rating`;
DROP TABLE IF EXISTS `osh_tool_vote`;
DROP TABLE IF EXISTS `osh_tool_collection`;
DROP TABLE IF EXISTS `osh_tool_tag_rel`;
DROP TABLE IF EXISTS `osh_tool_tag`;
DROP TABLE IF EXISTS `osh_tool_package_purchase_record`;
DROP TABLE IF EXISTS `osh_tool_package`;
DROP TABLE IF EXISTS `osh_tool_user_quota`;
DROP TABLE IF EXISTS `osh_tool`;

-- =====================================================
-- 表：osh_tool
-- =====================================================

CREATE TABLE `osh_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `no` char(8) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '工具编号',
  `tool_name` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '工具名称',
  `description` varchar(1000) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '工具描述',
  `access_type` tinyint NOT NULL DEFAULT '1' COMMENT '访问类型：1-站内工具，2-iframe第三方工具',
  `route_path` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '站内工具前端路由',
  `iframe_url` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '第三方iframe地址',
  `github_url` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT 'GitHub地址',
  `quota_cost` int NOT NULL DEFAULT '0' COMMENT '单次消耗工具点数',
  `status` tinyint NOT NULL DEFAULT '2' COMMENT '状态：2-待审核，4-上架，6-下架',
  `good_count` int NOT NULL DEFAULT '0' COMMENT '好评数',
  `neutral_count` int NOT NULL DEFAULT '0' COMMENT '中评数',
  `bad_count` int NOT NULL DEFAULT '0' COMMENT '差评数',
  `view_count` bigint NOT NULL DEFAULT '0' COMMENT '浏览数',
  `collection_count` int NOT NULL DEFAULT '0' COMMENT '收藏数',
  `total_usage` bigint NOT NULL DEFAULT '0' COMMENT '累计使用次数',
  `remark` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '备注',
  `resource_type` varchar(20) COLLATE utf8mb4_0900_as_cs DEFAULT 'FREE' COMMENT '资源类型：FREE,CASH_POINT',
  `level` tinyint DEFAULT '1' COMMENT '资源等级',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_no` (`no`),
  KEY `idx_tool_status` (`status`,`delete_flag`),
  KEY `idx_tool_resource_type` (`resource_type`,`delete_flag`),
  KEY `idx_tool_total_usage` (`total_usage`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具表';

-- =====================================================
-- 表：osh_tool_collection
-- =====================================================

CREATE TABLE `osh_tool_collection` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `tool_id` bigint unsigned NOT NULL COMMENT '工具ID',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tool` (`user_id`,`tool_id`),
  KEY `idx_tool_id` (`tool_id`,`delete_flag`),
  KEY `idx_user_id` (`user_id`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具用户收藏表';

-- =====================================================
-- 表：osh_tool_package
-- =====================================================

CREATE TABLE `osh_tool_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `package_name` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '套餐名称',
  `use_count` int NOT NULL DEFAULT '0' COMMENT '购买后增加的工具点数',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '现金价格',
  `point_cost` int NOT NULL DEFAULT '0' COMMENT '积分价格',
  `pay_type` tinyint NOT NULL DEFAULT '1' COMMENT '支付类型：1-现金，2-积分，3-现金+积分',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`,`sort_order`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具点数套餐表';

-- =====================================================
-- 表：osh_tool_package_purchase_record
-- =====================================================

CREATE TABLE `osh_tool_package_purchase_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '统一订单号',
  `payment_no` varchar(64) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '支付流水号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `package_id` bigint NOT NULL COMMENT '套餐ID',
  `package_name_snapshot` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '套餐名称快照',
  `package_use_count_snapshot` int NOT NULL DEFAULT '0' COMMENT '套餐工具点数快照',
  `package_cash_amount_snapshot` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '套餐现金金额快照',
  `package_point_amount_snapshot` int NOT NULL DEFAULT '0' COMMENT '套餐积分金额快照',
  `package_pay_type_snapshot` tinyint NOT NULL DEFAULT '1' COMMENT '套餐支付类型快照：1-纯现金，3-现金+积分',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消，3-已关闭',
  `grant_status` tinyint NOT NULL DEFAULT '0' COMMENT '发放状态：0-待发放，1-已发放，2-发放失败',
  `grant_time` datetime DEFAULT NULL COMMENT '发放时间',
  `remark` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_user_id` (`user_id`,`delete_flag`),
  KEY `idx_package_id` (`package_id`,`delete_flag`),
  KEY `idx_order_status` (`order_status`,`delete_flag`),
  KEY `idx_grant_status` (`grant_status`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具点数购买记录表';

-- =====================================================
-- 表：osh_tool_tag
-- =====================================================

CREATE TABLE `osh_tool_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '标签名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `use_count` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `remark` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '备注',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`name`),
  KEY `idx_status_sort` (`status`,`sort`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具标签表';

-- =====================================================
-- 表：osh_tool_tag_rel
-- =====================================================

CREATE TABLE `osh_tool_tag_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_tag` (`tool_id`,`tag_id`),
  KEY `idx_tag_id` (`tag_id`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具标签关联表';

-- =====================================================
-- 表：osh_tool_user_quota
-- =====================================================

CREATE TABLE `osh_tool_user_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `remaining_count` int NOT NULL DEFAULT '0' COMMENT '剩余工具点数',
  `total_buy_count` int NOT NULL DEFAULT '0' COMMENT '累计购买工具点数',
  `used_count` int NOT NULL DEFAULT '0' COMMENT '累计已使用工具点数',
  `last_use_time` datetime DEFAULT NULL COMMENT '最后使用时间',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_remaining_count` (`remaining_count`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='用户工具点数表';

-- =====================================================
-- 表：osh_tool_user_rating
-- 说明：当前代码中未直接消费该表，按工具模块统一风格保留
-- =====================================================

CREATE TABLE `osh_tool_user_rating` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `rating_type` tinyint NOT NULL COMMENT '评分类型：1-好评，2-中评，3-差评',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tool_rating` (`user_id`,`tool_id`),
  KEY `idx_tool_rating_type` (`tool_id`,`rating_type`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具用户评分记录表';

-- =====================================================
-- 表：osh_tool_vote
-- =====================================================

CREATE TABLE `osh_tool_vote` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `type` tinyint NOT NULL COMMENT '评价类型：1-点赞，3-差评',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识：0-正常，1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tool` (`user_id`,`tool_id`),
  KEY `idx_tool_type` (`tool_id`,`type`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具点赞差评记录表';

SET FOREIGN_KEY_CHECKS = 1;
