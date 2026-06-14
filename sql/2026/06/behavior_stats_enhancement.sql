-- User behavior statistics enhancement

ALTER TABLE `osh_user_action_event`
    ADD COLUMN `role_level` int NULL COMMENT 'role level' AFTER `role`,
    ADD COLUMN `resource_name` varchar(255) NULL COMMENT 'resource name' AFTER `resource_type`,
    ADD COLUMN `request_uri` varchar(255) NULL COMMENT 'request uri' AFTER `exception`,
    ADD COLUMN `request_method` varchar(16) NULL COMMENT 'request method' AFTER `request_uri`,
    ADD COLUMN `ip` varchar(64) NULL COMMENT 'client ip' AFTER `request_method`,
    ADD COLUMN `user_agent` varchar(512) NULL COMMENT 'user agent' AFTER `ip`,
    ADD COLUMN `duration_ms` bigint NULL COMMENT 'duration in ms' AFTER `user_agent`,
    ADD COLUMN `result_code` varchar(32) NULL COMMENT 'business result code' AFTER `duration_ms`,
    ADD COLUMN `trace_id` varchar(128) NULL COMMENT 'trace id' AFTER `result_code`,
    ADD COLUMN `contribution` tinyint NOT NULL DEFAULT 0 COMMENT 'whether this event can source contribution' AFTER `trace_id`;

CREATE INDEX `idx_user_action_module_time`
    ON `osh_user_action_event` (`module`, `action_type`, `happen_time`);
CREATE INDEX `idx_user_action_resource`
    ON `osh_user_action_event` (`resource_type`, `resource_id`(191));
CREATE INDEX `idx_user_action_role_time`
    ON `osh_user_action_event` (`role_level`, `happen_time`);

CREATE TABLE IF NOT EXISTS `osh_resource_contribution` (
    `id` bigint NOT NULL COMMENT 'primary id',
    `contributor_user_id` bigint NOT NULL COMMENT 'contributor user id',
    `contributor_username` varchar(64) NULL COMMENT 'contributor username snapshot',
    `contributor_role_level` int NULL COMMENT 'contributor role level snapshot',
    `resource_type` varchar(64) NOT NULL COMMENT 'resource type',
    `resource_id` bigint NOT NULL COMMENT 'resource id',
    `resource_name` varchar(255) NULL COMMENT 'resource name snapshot',
    `source_event_id` bigint NULL COMMENT 'source user action event id',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 active, 0 inactive',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by` bigint NULL,
    `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `update_by` bigint NULL,
    `delete_flag` tinyint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contribution_resource` (`resource_type`, `resource_id`, `contributor_user_id`),
    KEY `idx_contribution_user_time` (`contributor_user_id`, `create_time`),
    KEY `idx_contribution_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='resource contribution attribution';

CREATE TABLE IF NOT EXISTS `osh_resource_revenue_record` (
    `id` bigint NOT NULL COMMENT 'primary id',
    `contribution_id` bigint NOT NULL COMMENT 'resource contribution id',
    `order_id` bigint NULL COMMENT 'order id',
    `order_no` varchar(128) NULL COMMENT 'order no',
    `buyer_user_id` bigint NULL COMMENT 'buyer user id',
    `revenue_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT 'cash revenue',
    `point_amount` bigint NOT NULL DEFAULT 0 COMMENT 'point revenue',
    `biz_type` varchar(64) NULL COMMENT 'business type',
    `revenue_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by` bigint NULL,
    `delete_flag` tinyint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_revenue_contribution_time` (`contribution_id`, `revenue_time`),
    UNIQUE KEY `uk_revenue_contribution_order` (`contribution_id`, `order_no`),
    KEY `idx_revenue_order` (`order_no`),
    KEY `idx_revenue_time` (`revenue_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='resource contribution revenue record';
