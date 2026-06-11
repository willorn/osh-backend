package com.backstage.system.constants;

public final class CourseSectionConstants {

    public static final Long ROOT_PARENT_ID = 0L;
    public static final int CHAPTER_FREE_FLAG = 1;
    public static final int DEFAULT_FREE_FLAG = 0;
    public static final int STATUS_NORMAL = 1;
    public static final int DELETE_FLAG_NORMAL = 0;
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_TEXT = "text";
    /** 章引入另一门课程：该章为指向 linkedCourseId 的超链接 */
    public static final String TYPE_COURSE_LINK = "course_link";

    private CourseSectionConstants() {
    }
}
