package course.spring.elearningplatform.service.impl;

import course.spring.elearningplatform.config.RabbitMQConfig;
import course.spring.elearningplatform.dto.AssignmentNotificationDto;
import course.spring.elearningplatform.service.NotificationService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public NotificationServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendAssignmentNotification(AssignmentNotificationDto notification) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ASSIGNMENT_NOTIFICATION_QUEUE,
                    notification
            );
            System.out.println("Notification sent to RabbitMQ: " + notification.getAssignmentTitle());
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

