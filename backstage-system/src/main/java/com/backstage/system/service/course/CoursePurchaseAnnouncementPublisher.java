package com.backstage.system.service.course;

public interface CoursePurchaseAnnouncementPublisher {

    void publishPurchaseSuccess(Long userId, Long courseId, String orderNo);
}
