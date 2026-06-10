package com.backstage.system.domain.homepage.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 首页模块前端路径常量
 * <p>
 * 统一管理各模块的详情页路径前缀和列表页路径，
 * 避免在各 ServiceImpl 中硬编码字符串。
 *
 * @author jayTatum
 */
public final class FrontPathConstants {

    private FrontPathConstants() {}

    // ===== 详情页路径前缀（拼接 id 使用） =====

    /** 课程详情页前缀 */
    public static final String COURSE_DETAIL = "/course_detail/";

    /** 电子书详情页前缀 */
    public static final String BOOK_DETAIL = "/detail/book/";

    /** 试卷详情页前缀 */
    public static final String EXAM_DETAIL = "/paper_test/";

    /** 用户反馈详情页前缀 */
    public static final String FEEDBACK_DETAIL = "/feedback/detail/";

    /** 拼团详情页前缀（拼接 goodsId） */
    public static final String GROUP_DETAIL = "/course_detail/";

    /** 信息差详情页前缀 */
    public static final String INFO_GAP_DETAIL = "/info_gap/detail/";

    /** 开源项目详情页前缀 */
    public static final String OPEN_PROJECT_DETAIL = "/openproject/detail/";

    /** 问答详情页前缀 */
    public static final String QA_DETAIL = "/question_answer/detail/";

    /** 工具详情页前缀 */
    public static final String TOOL_DETAIL = "/tool/detail/";

    /** 实用网站详情页前缀 */
    public static final String WEBSITE_DETAIL = "/usefull/detail/";

    // ===== 秒杀商品详情页前缀（按商品类型区分） =====

    /** 秒杀-课程详情页前缀 */
    public static final String SECKILL_COURSE_DETAIL = "/seckill/detail/";

    /** 秒杀-书籍详情页前缀 */
    public static final String SECKILL_BOOK_DETAIL = "/seckill/detail/";

    /** 秒杀-商品详情页前缀 */
    public static final String SECKILL_GOODS_DETAIL = "/seckill/detail/";

    // ===== 列表页路径（"查看全部"按钮跳转） =====

    /** 课程列表页 */
    public static final String COURSE_LIST = "/course/1";

    /** 电子书列表页 */
    public static final String BOOK_LIST = "/list/book/1";

    /** 试卷列表页 */
    public static final String EXAM_LIST = "/paper/1";

    /** 用户反馈列表页 */
    public static final String FEEDBACK_LIST = "/feedback/list";

    /** 拼团列表页 */
    public static final String GROUP_LIST = "/group";

    /** 信息差列表页 */
    public static final String INFO_GAP_LIST = "/info_gap/1";

    /** 开源项目列表页 */
    public static final String OPEN_PROJECT_LIST = "/openproject/list";

    /** 问答列表页 */
    public static final String QA_LIST = "/question_answer/1";

    /** 秒杀列表页 */
    public static final String SECKILL_LIST = "/seckill";

    /** 工具列表页 */
    public static final String TOOL_LIST = "/tool/1";

    /** 实用网站列表页 */
    public static final String WEBSITE_LIST = "/usefull/list";

    // ===== 路径映射表（供 OshHomePageModulePathServiceImpl 使用） =====

    /**
     * 模块详情页路径前缀映射
     * key: 模块名，value: 前端详情页路径前缀（拼接 id 使用）
     */
    public static final Map<String, String> DETAIL_PATH_MAP = buildDetailPathMap();

    /**
     * 模块列表页路径映射
     * key: 模块名，value: 前端列表页完整路径
     */
    public static final Map<String, String> LIST_PATH_MAP = buildListPathMap();

    private static Map<String, String> buildDetailPathMap() {
        Map<String, String> map = new HashMap<>();
        map.put("course",         "/course_detail/");
        map.put("book",           "/detail/book/");
        map.put("exam",           "/paper/");
        map.put("feedback",       "/feedback/detail/");
        map.put("group",          "/course_detail/");
        map.put("info_gap",       "/info_gap/detail/");
        map.put("openproject",    "/openproject/detail/");
        map.put("qa",             "/question_answer/detail/");
        map.put("tool",           "/tool/detail/");
        map.put("usefull",        "/usefull/detail/");
        map.put("seckill",        "/seckill/detail/");
        map.put("seckill_course", "/seckill/detail/");
        map.put("seckill_book",   "/seckill/detail/");
        map.put("seckill_goods",  "/seckill/detail/");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildListPathMap() {
        Map<String, String> map = new HashMap<>();
        map.put("course",       "/course/1");
        map.put("book",         "/list/book/1");
        map.put("exam",         "/paper/1");
        map.put("feedback",     "/feedback/list");
        map.put("group",        "/group");
        map.put("info_gap",     "/info_gap/1");
        map.put("openproject",  "/openproject/list");
        map.put("qa",           "/question_answer/1");
        map.put("seckill",      "/seckill");
        map.put("tool",         "/tool/1");
        map.put("usefull",      "/usefull/list");
        return Collections.unmodifiableMap(map);
    }
}
