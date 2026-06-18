-- ============================================================
-- 内部资源模块 SQL
-- ============================================================

-- 1.资源组表
DROP TABLE IF EXISTS `osh_resource_group`;
CREATE TABLE `osh_resource_group`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '资源组ID',
    `name`        varchar(200) NOT NULL COMMENT '资源组名称',
    `description` varchar(1000)         DEFAULT NULL COMMENT '资源组描述',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `delete_flag` tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time` datetime              DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time` datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='内部资源组信息表';

-- 2.资源表
DROP TABLE IF EXISTS `osh_resource`;
CREATE TABLE `osh_resource`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '资源ID',
    `name`          varchar(200) NOT NULL COMMENT '资源名称',
    `no`        varchar(200) NOT NULL COMMENT '资源编号',
    `type`          varchar(32)           DEFAULT NULL COMMENT '资源类型（doc/video/image/code/other）',
    `remark`        varchar(500)          DEFAULT NULL COMMENT '备注',
    `file_path`     varchar(500)          DEFAULT NULL COMMENT '文件连接',
    `file_platform` varchar(500)          DEFAULT NULL COMMENT '文件存储平台',
    `delete_flag`   tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`     varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`   datetime              DEFAULT NULL COMMENT '创建时间',
    `update_by`     varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`   datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='内部资源信息表';

-- 3.链接表
DROP TABLE IF EXISTS `osh_link`;
CREATE TABLE `osh_link`
(
    `id`          bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '链接ID',
    `name`        varchar(200)  NOT NULL COMMENT '链接名称',
    `url`         varchar(1000) NOT NULL COMMENT '跳转地址',
    `remark`      varchar(500)           DEFAULT NULL COMMENT '备注',
    `delete_flag` tinyint(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)            DEFAULT '' COMMENT '创建者',
    `create_time` datetime               DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)            DEFAULT '' COMMENT '更新者',
    `update_time` datetime               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='内部资源链接信息表';

-- 4.资源组 <-> 资源 关联表
DROP TABLE IF EXISTS `osh_resource_group_resource`;
CREATE TABLE `osh_resource_group_resource`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`    bigint(20) NOT NULL COMMENT '资源组ID',
    `resource_id` bigint(20) NOT NULL COMMENT '资源ID',
    `sort_no`     int(11)    NOT NULL DEFAULT 0 COMMENT '排序号',
    `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)         DEFAULT '' COMMENT '创建者',
    `create_time` datetime            DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)         DEFAULT '' COMMENT '更新者',
    `update_time` datetime            DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_resource` (`group_id`, `resource_id`),
    KEY `idx_resource_id` (`resource_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='资源组-资源 关联表';

-- 5.资源组 <-> 链接 关联表
DROP TABLE IF EXISTS `osh_resource_group_link`;
CREATE TABLE `osh_resource_group_link`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`    bigint(20) NOT NULL COMMENT '资源组ID',
    `link_id`     bigint(20) NOT NULL COMMENT '链接ID',
    `sort_no`     int(11)    NOT NULL DEFAULT 0 COMMENT '排序号',
    `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)         DEFAULT '' COMMENT '创建者',
    `create_time` datetime            DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)         DEFAULT '' COMMENT '更新者',
    `update_time` datetime            DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_link` (`group_id`, `link_id`),
    KEY `idx_link_id` (`link_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='资源组-链接 关联表';

-- 6.资源 <-> 链接 关联表
DROP TABLE IF EXISTS `osh_resource_link`;
CREATE TABLE `osh_resource_link`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `resource_id` bigint(20) NOT NULL COMMENT '资源ID',
    `link_id`     bigint(20) NOT NULL COMMENT '链接ID',
    `sort_no`     int(11)    NOT NULL DEFAULT 0 COMMENT '排序号',
    `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)         DEFAULT '' COMMENT '创建者',
    `create_time` datetime            DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)         DEFAULT '' COMMENT '更新者',
    `update_time` datetime            DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_link` (`resource_id`, `link_id`),
    KEY `idx_link_id` (`link_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='资源-链接 关联表';
