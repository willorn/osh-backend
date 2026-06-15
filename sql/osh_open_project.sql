-- 开源项目表
DROP TABLE IF EXISTS `osh_open_project`;
CREATE TABLE `osh_open_project` (
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_name`     varchar(100) NOT NULL COMMENT '项目名称',
    `project_desc`     varchar(500) DEFAULT NULL COMMENT '项目描述',
    `project_url`      varchar(500) NOT NULL COMMENT '项目链接（Gitee/GitHub）',
    `author_name`      varchar(100) DEFAULT NULL COMMENT '作者名称',
    `project_cover`    varchar(500) DEFAULT NULL COMMENT '封面图片URL',
    `status`           tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1-可见',
    `click_count`      int          NOT NULL DEFAULT 0 COMMENT '点击次数',
    `reject_reason`    varchar(500) DEFAULT NULL COMMENT '拒绝原因',

    -- GitHub 同步字段
    `star_count`       int          NOT NULL DEFAULT 0 COMMENT 'GitHub Star 数',
    `fork_count`       int          NOT NULL DEFAULT 0 COMMENT 'GitHub Fork 数',
    `last_commit_time` datetime     DEFAULT NULL COMMENT '最近一次提交时间（从 GitHub 同步）',
    `is_archived`      tinyint      NOT NULL DEFAULT 0 COMMENT '是否已归档：0-活跃，1-已归档',
    `last_sync_time`   datetime     DEFAULT NULL COMMENT '最后一次从 GitHub 同步数据的时间',

    -- 基础字段
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`        bigint       DEFAULT NULL COMMENT '创建人（用户ID）',
    `update_time`      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`        bigint       DEFAULT NULL COMMENT '更新人（用户ID）',
    `delete_flag`      tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    PRIMARY KEY (`id`),
    INDEX `idx_status`        (`status`),
    INDEX `idx_delete_flag`   (`delete_flag`),
    INDEX `idx_create_time`   (`create_time`),
    INDEX `idx_star_count`    (`star_count`),
    INDEX `idx_last_sync_time`(`last_sync_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='开源项目表';

-- 开源项目标签表
DROP TABLE IF EXISTS `osh_open_project_tag`;
CREATE TABLE `osh_open_project_tag` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tag_name`    varchar(50) NOT NULL COMMENT '标签名称',
    `tag_code`    varchar(50) DEFAULT NULL COMMENT '标签编码',
    `sort_order`  int         DEFAULT 0 COMMENT '排序',
    `create_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   bigint      DEFAULT NULL COMMENT '创建人（用户ID）',
    `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   bigint      DEFAULT NULL COMMENT '更新人（用户ID）',
    `delete_flag` tinyint     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='开源项目标签表';

-- 开源项目-标签关联表
DROP TABLE IF EXISTS `osh_open_project_tag_rel`;
CREATE TABLE `osh_open_project_tag_rel` (
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  bigint   NOT NULL COMMENT '项目ID',
    `tag_id`      bigint   NOT NULL COMMENT '标签ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   bigint   DEFAULT NULL COMMENT '创建人（用户ID）',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   bigint   DEFAULT NULL COMMENT '更新人（用户ID）',
    `delete_flag` tinyint  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_project_id` (`project_id`),
    INDEX `idx_tag_id`     (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='开源项目标签关联表';

-- 初始化标签数据
INSERT INTO `osh_open_project_tag` (`tag_name`, `tag_code`, `sort_order`, `delete_flag`) VALUES
('Vue3',    'vue3',    1, 0),
('Nuxt3',   'nuxt3',   2, 0),
('Node.js', 'nodejs',  3, 0),
('Python',  'python',  4, 0),
('AI实战',  'ai',      5, 0),
('Java',    'java',    6, 0),
('React',   'react',   7, 0),
('Go',      'go',      8, 0),
('信息差',  'infogap', 9, 0);
