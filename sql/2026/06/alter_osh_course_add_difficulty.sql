-- 课程难度（与 level 适用人群等级分离）
-- level：资源/适用人群等级（VIP、小班等，预留）
-- difficulty：课程难度 1-新手入门 2-基础巩固 3-能力提升
ALTER TABLE `osh_course`
    ADD COLUMN `difficulty` tinyint(4) DEFAULT NULL COMMENT '课程难度：1-新手入门 2-基础巩固 3-能力提升' AFTER `level`;

-- 已有课程默认设为「新手入门」，上线后执行一次即可
UPDATE `osh_course` SET `difficulty` = 1 WHERE `difficulty` IS NULL;
