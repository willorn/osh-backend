package com.backstage.system.service.impl.course;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.system.domain.course.vo.CourseAnnouncementVO;
import com.backstage.system.mapper.course.OshCourseAnnouncementMapper;
import com.backstage.system.service.course.ICourseAnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CourseAnnouncementServiceImpl implements ICourseAnnouncementService {

    @Autowired
    private OshCourseAnnouncementMapper oshCourseAnnouncementMapper;

    @Override
    public List<CourseAnnouncementVO> listLatestSystemNotices() {
        return listByChannel(AnnouncementChannelEnum.SYSTEM_NOTICE);
    }

    @Override
    public List<CourseAnnouncementVO> listLatestUserNotices() {
        return listByChannel(AnnouncementChannelEnum.USER_NOTICE);
    }

    private List<CourseAnnouncementVO> listByChannel(AnnouncementChannelEnum channel) {
        List<CourseAnnouncementVO> list = oshCourseAnnouncementMapper.selectLatestCourseAnnouncementsByChannel(channel.getCode());
        return list == null ? Collections.emptyList() : list;
    }
}
