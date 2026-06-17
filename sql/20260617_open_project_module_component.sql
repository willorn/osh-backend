-- 开源项目模块、技术组件、最高负责人唯一约束迁移
-- 目标：
-- 1. 项目只允许一个最高负责人（contributor_type = 'primary'）。
-- 2. 首次 GitHub 同步默认 repo owner 为最高负责人，后续同步不再覆盖。
-- 3. 支持项目模块、模块成员、项目级技术组件维护。

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_open_project_module_component`$$
CREATE PROCEDURE `migrate_open_project_module_component`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_open_project'
          AND COLUMN_NAME = 'leader_locked'
    ) THEN
        ALTER TABLE `osh_open_project`
            ADD COLUMN `leader_locked` tinyint NOT NULL DEFAULT 0 COMMENT '最高负责人锁定：0-同步可初始化，1-已初始化/人工锁定' AFTER `homepage`;
    END IF;
END$$

CALL `migrate_open_project_module_component`()$$
DROP PROCEDURE IF EXISTS `migrate_open_project_module_component`$$

DELIMITER ;

-- 清理历史数据：每个项目只保留排序最靠前、ID最小的 active primary，其余降级为 contributor。
UPDATE `osh_open_project_contributor` c
JOIN (
    SELECT project_id, MIN(id) AS keep_id
    FROM `osh_open_project_contributor`
    WHERE delete_flag = 0 AND contributor_type = 'primary'
    GROUP BY project_id
) x ON x.project_id = c.project_id
SET c.contributor_type = 'contributor'
WHERE c.delete_flag = 0
  AND c.contributor_type = 'primary'
  AND c.id <> x.keep_id;

-- 标记已有负责人已锁定，防止后续同步覆盖。
UPDATE `osh_open_project` p
SET p.leader_locked = 1
WHERE EXISTS (
    SELECT 1
    FROM `osh_open_project_contributor` c
    WHERE c.project_id = p.id
      AND c.delete_flag = 0
      AND c.contributor_type = 'primary'
);

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_open_project_primary_guard`$$
CREATE PROCEDURE `migrate_open_project_primary_guard`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_open_project_contributor'
          AND COLUMN_NAME = 'primary_project_id'
    ) THEN
        ALTER TABLE `osh_open_project_contributor`
            ADD COLUMN `primary_project_id` bigint GENERATED ALWAYS AS (
                CASE
                    WHEN `delete_flag` = 0 AND `contributor_type` = 'primary' THEN `project_id`
                    ELSE NULL
                END
            ) STORED COMMENT '最高负责人唯一约束辅助列';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_open_project_contributor'
          AND INDEX_NAME = 'uk_open_project_one_primary'
    ) THEN
        CREATE UNIQUE INDEX `uk_open_project_one_primary`
            ON `osh_open_project_contributor` (`primary_project_id`);
    END IF;
END$$

CALL `migrate_open_project_primary_guard`()$$
DROP PROCEDURE IF EXISTS `migrate_open_project_primary_guard`$$

DELIMITER ;

CREATE TABLE IF NOT EXISTS `osh_open_project_module` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` bigint NOT NULL COMMENT '开源项目ID',
    `module_name` varchar(100) NOT NULL COMMENT '模块名称',
    `module_desc` varchar(500) DEFAULT NULL COMMENT '模块描述',
    `sort_order` int DEFAULT 0 COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_project_sort` (`project_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='开源项目模块表';

CREATE TABLE IF NOT EXISTS `osh_open_project_module_member` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module_id` bigint NOT NULL COMMENT '模块ID',
    `project_id` bigint NOT NULL COMMENT '开源项目ID',
    `contributor_id` bigint DEFAULT NULL COMMENT '项目开发团队成员ID',
    `github_account` varchar(100) NOT NULL COMMENT 'GitHub账号',
    `wechat_name` varchar(100) DEFAULT NULL COMMENT '微信名称',
    `member_role` varchar(20) NOT NULL DEFAULT 'collaborator' COMMENT '模块角色：primary-主要开发，collaborator-协同开发',
    `sort_order` int DEFAULT 0 COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_module_id` (`module_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_contributor_id` (`contributor_id`),
    KEY `idx_project_github` (`project_id`, `github_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='开源项目模块成员表';

CREATE TABLE IF NOT EXISTS `osh_open_project_tech_component` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `component_name` varchar(100) NOT NULL COMMENT '技术组件名称',
    `component_code` varchar(100) DEFAULT NULL COMMENT '技术组件编码',
    `component_desc` varchar(500) DEFAULT NULL COMMENT '技术组件描述',
    `official_url` varchar(500) DEFAULT NULL COMMENT '官网/文档链接',
    `sort_order` int DEFAULT 0 COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_component_name` (`component_name`, `delete_flag`),
    KEY `idx_component_code` (`component_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='开源项目技术组件字典表';

CREATE TABLE IF NOT EXISTS `osh_open_project_tech_component_rel` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` bigint NOT NULL COMMENT '开源项目ID',
    `component_id` bigint DEFAULT NULL COMMENT '技术组件ID',
    `component_name` varchar(100) NOT NULL COMMENT '技术组件名称（冗余）',
    `component_desc` varchar(500) DEFAULT NULL COMMENT '技术组件描述（冗余）',
    `official_url` varchar(500) DEFAULT NULL COMMENT '官网/文档链接（冗余）',
    `sort_order` int DEFAULT 0 COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_component_id` (`component_id`),
    KEY `idx_project_sort` (`project_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='开源项目技术组件关联表';

INSERT INTO `osh_open_project_tech_component`
(`component_name`, `component_code`, `component_desc`, `official_url`, `sort_order`, `delete_flag`)
VALUES
('Spring Boot', 'spring-boot', 'Java 后端应用开发框架', 'https://spring.io/projects/spring-boot', 10, 0),
('MyBatis Plus', 'mybatis-plus', 'MyBatis 增强工具', 'https://baomidou.com/', 20, 0),
('Vue', 'vue', '渐进式前端框架', 'https://vuejs.org/', 30, 0),
('Nuxt', 'nuxt', 'Vue 全栈应用框架', 'https://nuxt.com/', 40, 0),
('MySQL', 'mysql', '关系型数据库', 'https://www.mysql.com/', 50, 0),
('Redis', 'redis', '内存数据存储', 'https://redis.io/', 60, 0)
ON DUPLICATE KEY UPDATE
    `component_code` = VALUES(`component_code`),
    `component_desc` = VALUES(`component_desc`),
    `official_url` = VALUES(`official_url`),
    `sort_order` = VALUES(`sort_order`),
    `delete_flag` = 0;
