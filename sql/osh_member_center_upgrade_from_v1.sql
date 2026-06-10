-- 会员中心初版已建表时执行本升级脚本。
-- 作用：
-- 1. 给 osh_member_plan 增加最小/最大购买数量字段
-- 2. 新建 osh_member_benefit 权益文案表
-- 3. 给 osh_member_order 增加购买份数字段
-- 4. 给套餐增加增长系数和封顶配置
-- 5. 给订单增加原价金额字段
-- 6. 更新默认价格和默认权益文案

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

UPDATE `osh_member_plan`
SET `price` = 88.00,
    `original_price` = 88.00,
    `description` = '适合按阶段学习，支持多月续费，权益按月叠加',
    `min_purchase_quantity` = 1,
    `max_purchase_quantity` = 3,
    `growth_coefficient` = 0.7800,
    `cap_plan_code` = 'vip_year',
    `cap_ratio` = 0.9500,
    `update_by` = 0,
    `delete_flag` = 0
WHERE `plan_code` = 'vip_month';

UPDATE `osh_member_plan`
SET `price` = 288.00,
    `original_price` = 1056.00,
    `description` = '适合长期学习，年付比月付更划算',
    `min_purchase_quantity` = 1,
    `max_purchase_quantity` = 3,
    `growth_coefficient` = 0.9000,
    `cap_plan_code` = NULL,
    `cap_ratio` = 1.0000,
    `update_by` = 0,
    `delete_flag` = 0
WHERE `plan_code` = 'vip_year';

UPDATE `osh_member_plan`
SET `price` = 1888.00,
    `original_price` = 2888.00,
    `description` = '小班用户仅支持年付，享受更深度的陪伴式学习权益',
    `min_purchase_quantity` = 1,
    `max_purchase_quantity` = 3,
    `growth_coefficient` = 0.9500,
    `cap_plan_code` = NULL,
    `cap_ratio` = 1.0000,
    `update_by` = 0,
    `delete_flag` = 0
WHERE `plan_code` = 'small_class_year';

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
