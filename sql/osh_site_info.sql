/**
  网站信息表
 */
DROP TABLE IF EXISTS `osh_site_info`;
CREATE TABLE `osh_site_info`
(
    `id`                bigint unsigned                         NOT NULL AUTO_INCREMENT COMMENT '网站编号（主键）',
    `site_name`         varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '网站名称',
    `cover`             varchar(500) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '网站封面地址',
    `site_url`          varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '网站访问路径',
    `description`       varchar(2000) COLLATE utf8mb4_unicode_ci         DEFAULT NULL COMMENT '网站描述信息',
    `status`            tinyint                                          DEFAULT '1' COMMENT '状态：1=正常，0=异常',
    `site_type`         varchar(50) COLLATE utf8mb4_unicode_ci          DEFAULT '{}' COMMENT '网站类型：demo=演示站点',
    `site_config`       text COLLATE utf8mb4_unicode_ci                 DEFAULT NULL COMMENT '站点配置 JSON（按类型约定结构）',
    `create_by`        bigint unsigned                         NOT NULL COMMENT '创建人ID/账号',
    `create_time`       timestamp                               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_check_time`   timestamp                               DEFAULT NULL COMMENT '上次检查时间',
    `last_check_status` tinyint COMMENT '上次检查状态',
    `update_by`         bigint unsigned                                  DEFAULT NULL COMMENT '更新人ID/账号',
    `update_time`       timestamp                               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`        tinyint                                 NOT NULL DEFAULT '0' COMMENT '是否删除：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_site_name` (`site_name`, `delete_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 6
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='网站信息表';


/**
  网站用户点击统计表
 */
DROP TABLE IF EXISTS `osh_site_usage`;
CREATE TABLE `osh_site_usage`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `site_id`     bigint unsigned NOT NULL COMMENT '关联网站ID（website_info.id）',
    `user_id`     bigint unsigned NOT NULL COMMENT '操作用户ID/账号',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次点击时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`  tinyint         NOT NULL DEFAULT '0' COMMENT '是否删除：0=未删除，1=已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 9
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='网站用户点击统计表';

/**
  网站标签表
 */
DROP TABLE IF EXISTS `osh_site_tag`;
CREATE TABLE `osh_site_tag`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `tag_name`    varchar(100)    NOT NULL COMMENT '标签名称',
    `create_by`  bigint unsigned NOT NULL COMMENT '创建人ID/账号',
    `create_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint unsigned          DEFAULT NULL COMMENT '更新人ID/账号',
    `update_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`  tinyint         NOT NULL DEFAULT '0' COMMENT '是否删除：0=未删除，1=已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='网站标签表';

/**
  网站-网站标签关联表
 */
DROP TABLE IF EXISTS `osh_site_tag_relation`;
CREATE TABLE `osh_site_tag_relation`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `tag_id`      bigint unsigned NOT NULL COMMENT '标签ID',
    `site_id`     bigint unsigned NOT NULL COMMENT '关联网站ID',
    `create_by`  bigint unsigned NOT NULL COMMENT '创建人ID/账号',
    `create_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint unsigned          DEFAULT NULL COMMENT '更新人ID/账号',
    `update_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`  tinyint         NOT NULL DEFAULT '0' COMMENT '是否删除：0=未删除，1=已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='网站-网站标签关联表';

/**
  网站负责人表
 */
DROP TABLE IF EXISTS `osh_site_maintainer`;
CREATE TABLE `osh_site_maintainer`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `site_id`     bigint unsigned NOT NULL COMMENT '关联网站ID',
    `user_id`     bigint unsigned NOT NULL COMMENT '负责人用户ID',
    `create_by`  bigint unsigned NOT NULL COMMENT '创建人ID/账号',
    `create_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint unsigned          DEFAULT NULL COMMENT '更新人ID/账号',
    `update_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`  tinyint         NOT NULL DEFAULT '0' COMMENT '是否删除：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_site_id` (`site_id`, `delete_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='网站维护人表';


/**
  网站资源关联表
 */
DROP TABLE IF EXISTS `osh_site_resource_relation`;
CREATE TABLE `osh_site_resource_relation`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `site_id`       bigint unsigned NOT NULL COMMENT '关联网站ID',
    `resource_id`   bigint unsigned NOT NULL COMMENT '关联资源ID',
    `resource_type` varchar(50)     NOT NULL COMMENT '关联资源类型',
    `create_by`     bigint unsigned NOT NULL COMMENT '创建人ID/账号',
    `create_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     bigint unsigned          DEFAULT NULL COMMENT '更新人ID/账号',
    `update_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`   tinyint         NOT NULL DEFAULT '0' COMMENT '是否删除：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_site_id` (`site_id`, `delete_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='网站资源关联表';