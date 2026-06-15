-- ============================================
-- 反馈评论表添加图片字段
-- 执行时间: 2026-06-14
-- 说明: 为评论区添加图片上传功能支持
-- ============================================

USE
backstage;

-- 添加图片字段到评论表
ALTER TABLE `assistant_feedback_comment`
    ADD COLUMN IF NOT EXISTS `images` TEXT COMMENT '评论图片（JSON数组格式，存储图片URL列表）' AFTER `content`;

-- 添加注释说明
ALTER TABLE `assistant_feedback_comment`
    MODIFY COLUMN `images` TEXT COMMENT '评论图片（JSON数组格式，存储图片URL列表，最多9张）';
