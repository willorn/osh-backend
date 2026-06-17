-- 为反馈表添加图片字段
-- 日期: 2026-06-14
-- 说明: 支持用户在提交反馈时上传图片（最多9张）

ALTER TABLE assistant_feedback
    ADD COLUMN images TEXT COMMENT '反馈图片JSON数组（最多9张）' AFTER content;
