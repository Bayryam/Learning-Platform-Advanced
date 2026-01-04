package course.spring.elearningplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentNotificationDto {
    private Long assignmentId;
    private Long courseId;
    private String courseName;
    private String assignmentTitle;
    private String assignmentDescription;
    private String teacherName;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
}

