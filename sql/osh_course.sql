
-- ============================================================
-- 课程管理模块 SQL
-- 五个标准字段说明：
--   delete_flag  tinyint(1)  NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常 1-删除'
--   create_by    varchar(64) DEFAULT ''           COMMENT '创建者'
--   create_time  datetime    DEFAULT NULL         COMMENT '创建时间'
--   update_by    varchar(64) DEFAULT ''           COMMENT '更新者'
--   update_time  datetime    DEFAULT NULL         COMMENT '更新时间'
-- ============================================================

-- 1.使用专栏页面改过来 详情部分包含 课程介绍 课程服务周期 具体包含服务等
-- 9.查询列表界面 右上角显示新增课程 仅内部成员或者年vip用户 发起新增课程的流程，普通用户不行

DROP TABLE IF EXISTS `osh_course`;
CREATE TABLE `osh_course` (
    `id`                  bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '课程ID',
    `title`               varchar(200)    NOT NULL                        COMMENT '课程标题',
    `cover`               varchar(500)    DEFAULT NULL                    COMMENT '课程封面图URL',
    `type`                varchar(20)     DEFAULT NULL                    COMMENT '课程类型（media-视频课 live-直播课 text-图文课）',
    `status`              tinyint(1)      NOT NULL DEFAULT 0              COMMENT '课程状态：0-草稿 1-已发布 2-已下架',
    `price`               decimal(10,2)   NOT NULL DEFAULT 0.00           COMMENT '当前售价（0=免费）',
    `t_price`             decimal(10,2)   DEFAULT NULL                    COMMENT '原价/市场价',
    `intro`               text            DEFAULT NULL                    COMMENT '课程介绍（文本，详情页Tab1）',
    `service_period`      int(11)         DEFAULT NULL                    COMMENT '课程人工服务周期（月，NULL=没有）',
    `service_content`     varchar(1000)   DEFAULT NULL                    COMMENT '具体包含服务（如：答疑+资料+复盘）',
    `free_lesson_count`   int(11)         NOT NULL DEFAULT 0              COMMENT '免费试看章节数量（前N节）',
    `sub_count`           int(11)         NOT NULL DEFAULT 0              COMMENT '章节总数量',
    `buy_count`           int(11)         NOT NULL DEFAULT 0              COMMENT '购买人数',
    `view_count`          int(11)         NOT NULL DEFAULT 0              COMMENT '浏览次数',
    `good_count`          int(11)         NOT NULL DEFAULT 0              COMMENT '好评数量',
    `mid_count`           int(11)         NOT NULL DEFAULT 0              COMMENT '中评数量',
    `bad_count`           int(11)         NOT NULL DEFAULT 0              COMMENT '差评数量',
    `column_id`           bigint(20)      DEFAULT NULL                    COMMENT '所属专栏ID',
    `appid`               varchar(64)     DEFAULT NULL                    COMMENT '网校appid',
    `exam_id`             bigint(20)      DEFAULT NULL                    COMMENT '关联考试ID（NULL=无需考试）',
    `remark`              varchar(500)    DEFAULT NULL                    COMMENT '备注',
    `delete_flag`         tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`           varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time`         datetime        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`           varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time`         datetime        DEFAULT NULL                    COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_column_id`   (`column_id`),
    KEY `idx_appid`       (`appid`),
    KEY `idx_status`      (`status`),
    KEY `idx_type`        (`type`),
    KEY `idx_buy_count`   (`buy_count`),
    KEY `idx_exam_id`     (`exam_id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程信息表';


-- 2.课程前几节显示试听 右侧显示免费
-- 课程小节中 还需要什么详情内容？？？

DROP TABLE IF EXISTS `osh_course_section`;
CREATE TABLE `osh_course_section` (
    `id`                bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '章节ID',
    `course_id`         bigint(20)      NOT NULL                        COMMENT '所属课程ID',
    `parent_id`         bigint(20)      NOT NULL DEFAULT 0              COMMENT '父级ID（0=章 >0=节）',
    `title`             varchar(200)    NOT NULL                        COMMENT '章节标题',
    `sort`              int(11)         NOT NULL DEFAULT 0              COMMENT '排序序号',
    `is_free`           tinyint(1)      NOT NULL DEFAULT 0              COMMENT '是否免费试看：0-否 1-是',
    `duration`          int(11)         DEFAULT 0                       COMMENT '视频时长（秒）',
    `media_url`         varchar(500)    DEFAULT NULL                    COMMENT '媒体资源URL',
    `cover`             varchar(500)    DEFAULT NULL                    COMMENT '视频封面图URL',
    `video_codec`       varchar(20)     DEFAULT NULL                    COMMENT '视频编码格式（h264/h265/av1）',
    `video_bitrate`     int(11)         DEFAULT NULL                    COMMENT '视频比特率（kbps）',
    `video_resolution`  varchar(20)     DEFAULT NULL                    COMMENT '视频分辨率标识（720p/1080p/4k）',
    `file_size`         bigint(20)      DEFAULT 0                       COMMENT '视频文件大小（字节）',
    `subtitle_url`      varchar(500)    DEFAULT NULL                    COMMENT '字幕文件URL',
    `type`              varchar(20)     DEFAULT NULL                    COMMENT '小节类型：video/audio/text/live/course_link',
    `linked_course_id`  bigint(20)      DEFAULT NULL                    COMMENT '引入的课程ID（type=course_link 时该章为指向此课程的超链接）',
    `status`            tinyint(1)      NOT NULL DEFAULT 1              COMMENT '状态：0-隐藏 1-发布',
    `exam_id`           bigint(20)      DEFAULT NULL                    COMMENT '关联考试ID（学完跳转答题）',
    `delete_flag`       tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`         varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time`       datetime        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`         varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time`       datetime        DEFAULT NULL                    COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id`      (`course_id`),
    KEY `idx_parent_id`      (`parent_id`),
    KEY `idx_exam_id`        (`exam_id`),
    KEY `idx_delete_flag`    (`delete_flag`),
    KEY `idx_course_status`  (`course_id`, `status`, `delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程章节表（支持二级：章-节）';


-- 3.课程大纲页面可以点击立即学习然后支付后即可解锁付费课程与资料
-- 课程购买记录表：记录用户对课程的购买/解锁状态，作为鉴权核心依据
-- 免费课程 → pay_method='free', pay_status='paid', order_no 可为空
-- 付费课程 → 支付成功回调后写入，pay_status='paid'
-- 重复购买保护：UNIQUE KEY uk_user_course

DROP TABLE IF EXISTS `osh_course_buy`;
CREATE TABLE `osh_course_buy` (
    `id`            bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '主键ID',
    `user_id`       bigint(20)      NOT NULL                        COMMENT '购买用户ID',
    `course_id`     bigint(20)      NOT NULL                        COMMENT '购买课程ID',
    `order_no`      varchar(64)     DEFAULT NULL                    COMMENT '关联支付订单号（免费课可为空）',
    `pay_status`    varchar(20)     NOT NULL DEFAULT 'pending'      COMMENT '支付状态：pending-待支付 paid-已支付 refunded-已退款',
    `pay_method`    varchar(20)     DEFAULT NULL                    COMMENT '支付方式：free-免费 wxpay-微信 alipay-支付宝',
    `pay_price`     decimal(10,2)   NOT NULL DEFAULT 0.00           COMMENT '实际支付金额',
    `origin_price`  decimal(10,2)   DEFAULT NULL                    COMMENT '课程原价（下单时快照）',
    `expire_time`   datetime        DEFAULT NULL                    COMMENT '有效期截止时间（NULL=永久有效）',
    `pay_time`      datetime        DEFAULT NULL                    COMMENT '支付完成时间',
    `delete_flag`   tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`     varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time`   datetime        DEFAULT NULL                    COMMENT '记录创建时间',
    `update_by`     varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time`   datetime        DEFAULT NULL                    COMMENT '记录更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_course`  (`user_id`, `course_id`),
    KEY `idx_user_id`            (`user_id`),
    KEY `idx_course_id`          (`course_id`),
    KEY `idx_order_no`           (`order_no`),
    KEY `idx_pay_status`         (`pay_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程购买记录表（鉴权核心表）';


-- 4.支付后课程大纲界面显示资料下载选项
-- 10.课程新增界面 显示资料列表 可以上传 下载 删除资料 资料必须为压缩包格式

DROP TABLE IF EXISTS `osh_course_material`;
CREATE TABLE `osh_course_material` (
    `id`             bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '资料ID',
    `course_id`      bigint(20)      NOT NULL                        COMMENT '所属课程ID',
    `name`           varchar(200)    NOT NULL                        COMMENT '资料名称',
    `url`            varchar(500)    NOT NULL                        COMMENT '资料文件URL（仅支持.zip/.rar）',
    `file_size`      bigint(20)      DEFAULT 0                       COMMENT '文件大小（字节）',
    `file_type`      varchar(20)     DEFAULT NULL                    COMMENT '文件类型：zip/rar',
    `download_count` int(11)         DEFAULT 0                       COMMENT '下载次数',
    `is_pay_only`    tinyint(1)      NOT NULL DEFAULT 1              COMMENT '是否仅购买后可下载：0-否 1-是',
    `sort`           int(11)         NOT NULL DEFAULT 0              COMMENT '排序',
    `delete_flag`    tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`      varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time`    datetime        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`      varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time`    datetime        DEFAULT NULL                    COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id`   (`course_id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程资料表（付费后可下载）';


-- 5.课程大纲部分已学部分右侧显示已学完 有疑问两种状态 点击疑问可以跳转到问答板块问题详情界面

DROP TABLE IF EXISTS `osh_user_learn_progress`;
CREATE TABLE `osh_user_learn_progress` (
    `id`             bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '主键ID',
    `user_id`        bigint(20)      NOT NULL                        COMMENT '用户ID',
    `course_id`      bigint(20)      NOT NULL                        COMMENT '课程ID',
    `section_id`     bigint(20)      NOT NULL                        COMMENT '章节ID',
    `status`         tinyint(1)      NOT NULL DEFAULT 0              COMMENT '学习状态：0-未开始 1-学习中 2-有疑问 3-已完成',
    `progress`       tinyint(3)      NOT NULL DEFAULT 0              COMMENT '进度百分比（0-100）',
    `last_position`  int(11)         DEFAULT 0                       COMMENT '上次播放位置（秒）',
    `learn_time`     int(11)         DEFAULT 0                       COMMENT '累计学习时长（秒）',
    `watch_count`    int(11)         NOT NULL DEFAULT 0              COMMENT '观看次数',
    `is_completed`   tinyint(1)      NOT NULL DEFAULT 0              COMMENT '是否完成：0-否 1-是',
    `complete_time`  datetime        DEFAULT NULL                    COMMENT '首次完成时间',
    `finish_time`    datetime        DEFAULT NULL                    COMMENT '最近完成时间',
    `delete_flag`    tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`      varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time`    datetime        DEFAULT NULL                    COMMENT '首次学习时间',
    `update_by`      varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time`    datetime        DEFAULT NULL                    COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_section` (`user_id`, `section_id`),
    KEY `idx_user_course`  (`user_id`, `course_id`),
    KEY `idx_section_id`   (`section_id`),
    KEY `idx_user_status`  (`user_id`, `status`),
    KEY `idx_delete_flag`  (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户章节学习进度表';


-- 7.课程小节详情内 可以提问 问题会被显示在问答板块的课程问答部分（还有非课程问答）
-- 调用问答板块的接口，将数据存储到问答模块的表中

DROP TABLE IF EXISTS `osh_course_question`;
CREATE TABLE `osh_course_question` (
    `id`          bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '问答ID',
    `course_id`   bigint(20)      NOT NULL                        COMMENT '课程ID',
    `section_id`  bigint(20)      DEFAULT NULL                    COMMENT '章节ID',
    `user_id`     bigint(20)      NOT NULL                        COMMENT '提问用户ID',
    `parent_id`   bigint(20)      NOT NULL DEFAULT 0              COMMENT '父ID（0=提问 >0=回答）',
    `content`     text            NOT NULL                        COMMENT '内容',
    `is_solved`   tinyint(1)      NOT NULL DEFAULT 0              COMMENT '是否解决：0-否 1-是 2-待解决',
    `delete_flag` tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time` datetime        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`   varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time` datetime        DEFAULT NULL                    COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id`   (`course_id`),
    KEY `idx_section_id`  (`section_id`),
    KEY `idx_parent_id`   (`parent_id`),
    KEY `idx_user_id`     (`user_id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程问答表';


-- 8.课程界面新增查询功能 可以通过标签下拉框多选 + 关键字搜索查询（ES）
--   标签目前包含 开源项目 ai 企业刚需 标签默认按课程使用数量从上往下排序
-- 11.课程有标签 好评 中评 差评数量（只能选一个）直接显示在课程界面上

DROP TABLE IF EXISTS `osh_course_tag`;
CREATE TABLE `osh_course_tag` (
    `id`          bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '标签ID',
    `name`        varchar(50)     NOT NULL                        COMMENT '标签名称',
    `sort`        int(11)         NOT NULL DEFAULT 0              COMMENT '排序权重（越大越靠前）',
    `use_count`   int(11)         NOT NULL DEFAULT 0              COMMENT '关联课程使用数量',
    `status`      tinyint(1)      NOT NULL DEFAULT 1              COMMENT '状态：0-禁用 1-启用',
    `remark`      varchar(500)    DEFAULT NULL                    COMMENT '备注',
    `delete_flag` tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time` datetime        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`   varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time` datetime        DEFAULT NULL                    COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name`      (`name`),
    KEY `idx_use_count`       (`use_count` DESC),
    KEY `idx_sort`            (`sort` DESC),
    KEY `idx_delete_flag`     (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程标签表';

-- 初始化标签数据
INSERT INTO `osh_course_tag` (`name`, `sort`, `use_count`, `status`, `create_by`, `create_time`)
VALUES ('开源项目', 100, 0, 1, 'admin', NOW()),
       ('AI企业刚需', 90,  0, 1, 'admin', NOW()),
       ('企业刚需',   80,  0, 1, 'admin', NOW());


-- 课程标签关联表（课程与标签多对多）
-- 通过标签下拉框多选 + 关键字搜索（ES），默认按课程访问数量、购买数量、好评数权重排序

DROP TABLE IF EXISTS `osh_course_tag_rel`;
CREATE TABLE `osh_course_tag_rel` (
    `id`          bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '主键ID',
    `course_id`   bigint(20)      NOT NULL                        COMMENT '课程ID',
    `tag_id`      bigint(20)      NOT NULL                        COMMENT '标签ID',
    `delete_flag` tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time` datetime        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`   varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time` datetime        DEFAULT NULL                    COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_tag`    (`course_id`, `tag_id`),
    KEY `idx_course_id`           (`course_id`),
    KEY `idx_tag_id`              (`tag_id`),
    KEY `idx_delete_flag`         (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程标签关联表（多对多）';


-- 12.通过考试与审核的人可以申请成为该课程服务人员
-- 点击申请 → 判断是否需要考试 → 调考试模块接口获取试卷 → 提交答卷 → 自动判断是否合格
-- 合格后写入申请记录 → 推送 sys_notice 消息给审核人 → 管理员审核通过/拒绝 → 推送结果消息给申请人

DROP TABLE IF EXISTS `osh_course_teacher_apply`;
CREATE TABLE `osh_course_teacher_apply` (
    `id`            bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '申请ID',
    `course_id`     bigint(20)      NOT NULL                        COMMENT '课程ID',
    `user_id`       bigint(20)      NOT NULL                        COMMENT '申请人ID',
    `exam_id`       bigint(20)      DEFAULT NULL                    COMMENT '考试ID',
    `exam_score`    decimal(5,2)    DEFAULT NULL                    COMMENT '考试得分',
    `apply_status`  tinyint(1)      NOT NULL DEFAULT 0              COMMENT '状态：0-待审核 1-通过 2-拒绝',
    `apply_time`    datetime        DEFAULT NULL                    COMMENT '申请时间',
    `audit_time`    datetime        DEFAULT NULL                    COMMENT '审核时间',
    `audit_by`      varchar(64)     DEFAULT NULL                    COMMENT '审核人',
    `audit_remark`  varchar(500)    DEFAULT NULL                    COMMENT '审核备注（拒绝时必填）',
    `delete_flag`   tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`     varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time`   datetime        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`     varchar(64)     DEFAULT ''                      COMMENT '更新者',
    `update_time`   datetime        DEFAULT NULL                    COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_user`    (`course_id`, `user_id`),
    KEY `idx_apply_status`   (`apply_status`),
    KEY `idx_delete_flag`    (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程讲师申请审核表';


-- 13.用户点赞记录表（统一管理评论和问答的点赞）
-- 支持用户对问答、回答等内容进行点赞操作

DROP TABLE IF EXISTS `osh_user_like`;
CREATE TABLE `osh_user_like` (
    `id`          bigint(20)      NOT NULL AUTO_INCREMENT         COMMENT '主键ID',
    `user_id`     bigint(20)      NOT NULL                        COMMENT '用户ID',
    `target_type` varchar(20)     NOT NULL                        COMMENT '目标类型：question-问答 answer-回答 comment-评论',
    `target_id`   bigint(20)      NOT NULL                        COMMENT '目标ID',
    `delete_flag` tinyint(1)      NOT NULL DEFAULT 0              COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`   varchar(64)     DEFAULT ''                      COMMENT '创建者',
    `create_time` datetime        DEFAULT NULL                    COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户点赞记录表';
