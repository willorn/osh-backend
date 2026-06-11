-- 为课程章节表增加「引入课程作为章」所需字段。
-- 当 type = 'course_link' 时，linked_course_id 指向被引入的课程ID，
-- 该章在前端显示为超链接，点击章节标题跳转到 /course_detail/{linked_course_id}。
-- 普通章/小节该字段为 NULL，不受影响。

ALTER TABLE `osh_course_section`
    ADD COLUMN `linked_course_id` bigint(20) DEFAULT NULL
        COMMENT '引入的课程ID（type=course_link 时该章为指向此课程的超链接）'
        AFTER `type`;
