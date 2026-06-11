CREATE TABLE IF NOT EXISTS `osh_member_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `plan_code` varchar(64) NOT NULL COMMENT '套餐编码',
  `plan_name` varchar(100) NOT NULL COMMENT '套餐名称',
  `member_type` varchar(32) NOT NULL COMMENT '会员类型: vip/small_class',
  `period_type` varchar(16) NOT NULL COMMENT '周期类型: month/year',
  `duration_months` int NOT NULL COMMENT '有效期月数',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `description` varchar(500) DEFAULT NULL COMMENT '套餐说明',
  `min_purchase_quantity` int NOT NULL DEFAULT 1 COMMENT '最小购买数量',
  `max_purchase_quantity` int NOT NULL DEFAULT 36 COMMENT '最大购买数量',
  `growth_coefficient` decimal(6,4) NOT NULL DEFAULT 0.9000 COMMENT '价格增长系数',
  `cap_plan_code` varchar(64) DEFAULT NULL COMMENT '封顶参考套餐编码',
  `cap_ratio` decimal(6,4) NOT NULL DEFAULT 1.0000 COMMENT '封顶比例',
  `sort` int DEFAULT 0 COMMENT '排序',
  `status` tinyint DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint NOT NULL DEFAULT 0 COMMENT '更新人',
  `delete_flag` tinyint DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_plan_code` (`plan_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员套餐表';

SET @member_plan_min_quantity_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'osh_member_plan'
    AND COLUMN_NAME = 'min_purchase_quantity'
);
SET @member_plan_min_quantity_sql = IF(
  @member_plan_min_quantity_column_exists = 0,
  'ALTER TABLE `osh_member_plan` ADD COLUMN `min_purchase_quantity` int NOT NULL DEFAULT 1 COMMENT ''最小购买数量'' AFTER `description`',
  'SELECT 1'
);
PREPARE member_plan_min_quantity_stmt FROM @member_plan_min_quantity_sql;
EXECUTE member_plan_min_quantity_stmt;
DEALLOCATE PREPARE member_plan_min_quantity_stmt;

SET @member_plan_max_quantity_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'osh_member_plan'
    AND COLUMN_NAME = 'max_purchase_quantity'
);
SET @member_plan_max_quantity_sql = IF(
  @member_plan_max_quantity_column_exists = 0,
  'ALTER TABLE `osh_member_plan` ADD COLUMN `max_purchase_quantity` int NOT NULL DEFAULT 36 COMMENT ''最大购买数量'' AFTER `min_purchase_quantity`',
  'SELECT 1'
);
PREPARE member_plan_max_quantity_stmt FROM @member_plan_max_quantity_sql;
EXECUTE member_plan_max_quantity_stmt;
DEALLOCATE PREPARE member_plan_max_quantity_stmt;

SET @member_plan_growth_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'osh_member_plan'
    AND COLUMN_NAME = 'growth_coefficient'
);
SET @member_plan_growth_sql = IF(
  @member_plan_growth_column_exists = 0,
  'ALTER TABLE `osh_member_plan` ADD COLUMN `growth_coefficient` decimal(6,4) NOT NULL DEFAULT 0.9000 COMMENT ''价格增长系数'' AFTER `max_purchase_quantity`',
  'SELECT 1'
);
PREPARE member_plan_growth_stmt FROM @member_plan_growth_sql;
EXECUTE member_plan_growth_stmt;
DEALLOCATE PREPARE member_plan_growth_stmt;

SET @member_plan_cap_code_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'osh_member_plan'
    AND COLUMN_NAME = 'cap_plan_code'
);
SET @member_plan_cap_code_sql = IF(
  @member_plan_cap_code_column_exists = 0,
  'ALTER TABLE `osh_member_plan` ADD COLUMN `cap_plan_code` varchar(64) DEFAULT NULL COMMENT ''封顶参考套餐编码'' AFTER `growth_coefficient`',
  'SELECT 1'
);
PREPARE member_plan_cap_code_stmt FROM @member_plan_cap_code_sql;
EXECUTE member_plan_cap_code_stmt;
DEALLOCATE PREPARE member_plan_cap_code_stmt;

SET @member_plan_cap_ratio_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'osh_member_plan'
    AND COLUMN_NAME = 'cap_ratio'
);
SET @member_plan_cap_ratio_sql = IF(
  @member_plan_cap_ratio_column_exists = 0,
  'ALTER TABLE `osh_member_plan` ADD COLUMN `cap_ratio` decimal(6,4) NOT NULL DEFAULT 1.0000 COMMENT ''封顶比例'' AFTER `cap_plan_code`',
  'SELECT 1'
);
PREPARE member_plan_cap_ratio_stmt FROM @member_plan_cap_ratio_sql;
EXECUTE member_plan_cap_ratio_stmt;
DEALLOCATE PREPARE member_plan_cap_ratio_stmt;

CREATE TABLE IF NOT EXISTS `osh_member_benefit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权益ID',
  `plan_id` bigint NOT NULL COMMENT '套餐ID',
  `benefit_title` varchar(80) NOT NULL COMMENT '权益标题',
  `benefit_description` varchar(300) DEFAULT NULL COMMENT '权益说明',
  `icon` varchar(64) DEFAULT NULL COMMENT '图标标识',
  `sort` int DEFAULT 0 COMMENT '排序',
  `status` tinyint DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint NOT NULL DEFAULT 0 COMMENT '更新人',
  `delete_flag` tinyint DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_member_benefit_plan` (`plan_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员套餐权益文案表';

CREATE TABLE IF NOT EXISTS `osh_member_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_id` bigint NOT NULL COMMENT '套餐ID',
  `order_no` varchar(64) NOT NULL COMMENT '统一订单号',
  `member_type` varchar(32) NOT NULL COMMENT '会员类型: vip/small_class',
  `plan_name_snapshot` varchar(100) NOT NULL COMMENT '套餐名称快照',
  `purchase_quantity` int NOT NULL DEFAULT 1 COMMENT '购买份数',
  `duration_months` int NOT NULL COMMENT '购买月数',
  `original_amount` decimal(10,2) DEFAULT NULL COMMENT '原价金额',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_status` tinyint DEFAULT 0 COMMENT '支付状态: 0待支付 1已支付 2已关闭',
  `grant_status` tinyint DEFAULT 0 COMMENT '发放状态: 0待发放 1已发放 2发放失败',
  `grant_message` varchar(500) DEFAULT NULL COMMENT '发放结果信息',
  `start_time` datetime DEFAULT NULL COMMENT '本次权益开始时间',
  `expire_time` datetime DEFAULT NULL COMMENT '本次权益到期时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `grant_time` datetime DEFAULT NULL COMMENT '发放时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint NOT NULL DEFAULT 0 COMMENT '更新人',
  `delete_flag` tinyint DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_order_no` (`order_no`),
  KEY `idx_member_order_user` (`user_id`, `create_time`),
  KEY `idx_member_order_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员充值订单表';

SET @member_order_quantity_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'osh_member_order'
    AND COLUMN_NAME = 'purchase_quantity'
);
SET @member_order_quantity_sql = IF(
  @member_order_quantity_column_exists = 0,
  'ALTER TABLE `osh_member_order` ADD COLUMN `purchase_quantity` int NOT NULL DEFAULT 1 COMMENT ''购买份数'' AFTER `plan_name_snapshot`',
  'SELECT 1'
);
PREPARE member_order_quantity_stmt FROM @member_order_quantity_sql;
EXECUTE member_order_quantity_stmt;
DEALLOCATE PREPARE member_order_quantity_stmt;

SET @member_order_original_amount_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'osh_member_order'
    AND COLUMN_NAME = 'original_amount'
);
SET @member_order_original_amount_sql = IF(
  @member_order_original_amount_column_exists = 0,
  'ALTER TABLE `osh_member_order` ADD COLUMN `original_amount` decimal(10,2) DEFAULT NULL COMMENT ''原价金额'' AFTER `duration_months`',
  'SELECT 1'
);
PREPARE member_order_original_amount_stmt FROM @member_order_original_amount_sql;
EXECUTE member_order_original_amount_stmt;
DEALLOCATE PREPARE member_order_original_amount_stmt;

INSERT INTO `osh_member_plan`
(`plan_code`, `plan_name`, `member_type`, `period_type`, `duration_months`, `price`, `original_price`, `description`, `min_purchase_quantity`, `max_purchase_quantity`, `growth_coefficient`, `cap_plan_code`, `cap_ratio`, `sort`, `status`, `create_by`, `update_by`, `delete_flag`)
VALUES
('vip_month', 'VIP月卡', 'vip', 'month', 1, 88.00, 88.00, '适合按阶段学习，支持多月续费，权益按月叠加', 1, 3, 0.7800, 'vip_year', 0.9500, 10, 1, 0, 0, 0),
('vip_year', 'VIP年卡', 'vip', 'year', 12, 288.00, 1056.00, '适合长期学习，年付比月付更划算', 1, 3, 0.9000, NULL, 1.0000, 20, 1, 0, 0, 0),
('small_class_year', '小班用户年卡', 'small_class', 'year', 12, 1888.00, 2888.00, '小班用户仅支持年付，享受更深度的陪伴式学习权益', 1, 3, 0.9500, NULL, 1.0000, 30, 1, 0, 0, 0)
ON DUPLICATE KEY UPDATE
  `plan_name` = VALUES(`plan_name`),
  `member_type` = VALUES(`member_type`),
  `period_type` = VALUES(`period_type`),
  `duration_months` = VALUES(`duration_months`),
  `price` = VALUES(`price`),
  `original_price` = VALUES(`original_price`),
  `description` = VALUES(`description`),
  `min_purchase_quantity` = VALUES(`min_purchase_quantity`),
  `max_purchase_quantity` = VALUES(`max_purchase_quantity`),
  `growth_coefficient` = VALUES(`growth_coefficient`),
  `cap_plan_code` = VALUES(`cap_plan_code`),
  `cap_ratio` = VALUES(`cap_ratio`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `update_by` = VALUES(`update_by`),
  `delete_flag` = 0;

INSERT INTO `osh_member_benefit`
(`plan_id`, `benefit_title`, `benefit_description`, `icon`, `sort`, `status`, `create_by`, `update_by`, `delete_flag`)
SELECT p.id, b.benefit_title, b.benefit_description, b.icon, b.sort, 1, 0, 0, 0
FROM `osh_member_plan` p
JOIN (
  SELECT 'vip_month' plan_code, '专属内容访问' benefit_title, '解锁 VIP 课程、电子书与考试题库中的会员内容' benefit_description, 'book' icon, 10 sort
  UNION ALL SELECT 'vip_month', '学习工具额度', '获得会员可用的工具使用次数与学习辅助能力', 'tool', 20
  UNION ALL SELECT 'vip_month', '会员身份标识', '个人中心展示 VIP 身份，到期前可继续续费叠加', 'badge', 30
  UNION ALL SELECT 'vip_year', '全年 VIP 内容访问', '12 个月持续解锁 VIP 课程、电子书与考试题库', 'book', 10
  UNION ALL SELECT 'vip_year', '年付价格保护', '按年付费锁定低价，适合长期学习路线', 'shield', 20
  UNION ALL SELECT 'vip_year', '优先功能体验', '优先体验新学习工具、新题库和会员专属活动', 'spark', 30
  UNION ALL SELECT 'small_class_year', '小班专属内容', '解锁小班课程、资料包与高阶学习路径', 'class', 10
  UNION ALL SELECT 'small_class_year', '深度学习陪伴', '享受更高优先级的答疑、反馈和学习支持', 'chat', 20
  UNION ALL SELECT 'small_class_year', '年度成长计划', '适合一年制系统学习，权益按年续期', 'growth', 30
) b ON b.plan_code = p.plan_code
WHERE NOT EXISTS (
  SELECT 1 FROM `osh_member_benefit` e
  WHERE e.plan_id = p.id
    AND e.benefit_title = b.benefit_title
    AND e.delete_flag = 0
);
