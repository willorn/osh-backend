package com.backstage.system.mapper.course;

import com.backstage.system.domain.course.vo.CourseAnnouncementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshCourseAnnouncementMapper {

    List<CourseAnnouncementVO> selectLatestCourseAnnouncementsByChannel(@Param("channel") Integer channel);

    int insertCourseAnnouncement(@Param("title") String title,
                                 @Param("link") String link,
                                 @Param("channel") Integer channel,
                                 @Param("operator") String operator);
}
