package com.backstage.system.mapper.openproject;

import com.backstage.system.domain.openproject.vo.OpenProjectResourceOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OshOpenProjectResourceSearchMapper {

    @Select("<script>" +
            "SELECT id, 'course' AS resourceType, title AS resourceName, CONCAT('/course_detail/', id) AS resourceUrl " +
            "FROM osh_course " +
            "WHERE delete_flag = 0 " +
            "<if test='keyword != null and keyword != \"\"'>AND title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "ORDER BY update_time DESC, id DESC LIMIT #{limit}" +
            "</script>")
    List<OpenProjectResourceOptionVO> selectCourses(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT id, 'book' AS resourceType, title AS resourceName, CONCAT('/detail/book/', id) AS resourceUrl " +
            "FROM osh_book " +
            "WHERE del_flag = '0' AND status = '0' " +
            "<if test='keyword != null and keyword != \"\"'>AND title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "ORDER BY update_time DESC, id DESC LIMIT #{limit}" +
            "</script>")
    List<OpenProjectResourceOptionVO> selectBooks(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT id, 'tool' AS resourceType, tool_name AS resourceName, CONCAT('/tool?toolId=', id, '&amp;autoOpen=1') AS resourceUrl " +
            "FROM osh_tool " +
            "WHERE delete_flag = 0 AND status = 4 " +
            "<if test='keyword != null and keyword != \"\"'>AND (tool_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY update_time DESC, id DESC LIMIT #{limit}" +
            "</script>")
    List<OpenProjectResourceOptionVO> selectTools(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT id, 'course' AS resourceType, title AS resourceName, CONCAT('/course_detail/', id) AS resourceUrl " +
            "FROM osh_course WHERE id = #{id} AND delete_flag = 0 LIMIT 1")
    OpenProjectResourceOptionVO selectCourseById(@Param("id") Long id);

    @Select("SELECT id, 'book' AS resourceType, title AS resourceName, CONCAT('/detail/book/', id) AS resourceUrl " +
            "FROM osh_book WHERE id = #{id} AND del_flag = '0' AND status = '0' LIMIT 1")
    OpenProjectResourceOptionVO selectBookById(@Param("id") Long id);

    @Select("SELECT id, 'tool' AS resourceType, tool_name AS resourceName, CONCAT('/tool?toolId=', id, '&amp;autoOpen=1') AS resourceUrl " +
            "FROM osh_tool WHERE id = #{id} AND delete_flag = 0 AND status = 4 LIMIT 1")
    OpenProjectResourceOptionVO selectToolById(@Param("id") Long id);
}
