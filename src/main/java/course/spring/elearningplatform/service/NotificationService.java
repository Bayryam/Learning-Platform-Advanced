package course.spring.elearningplatform.service;

import course.spring.elearningplatform.dto.AssignmentNotificationDto;

public interface NotificationService {
    void sendAssignmentNotification(AssignmentNotificationDto notification);
}

