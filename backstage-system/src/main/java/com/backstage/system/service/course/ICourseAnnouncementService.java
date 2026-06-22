package com.backstage.system.service.course;

import com.backstage.system.domain.course.vo.CourseAnnouncementVO;

import java.util.List;

public interface ICourseAnnouncementService {

    List<CourseAnnouncementVO> listLatestSystemNotices();

    List<CourseAnnouncementVO> listLatestUserNotices();
}
