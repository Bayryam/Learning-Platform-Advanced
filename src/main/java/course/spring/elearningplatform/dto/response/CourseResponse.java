package course.spring.elearningplatform.dto.response;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class CourseResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> categories;
    private UserBasicInfo createdBy;
    private Date createdOn;
    private String imageBase64;
    private int participantCount;
    private int completedCount;
    private int lessonsCount;
    private List<LessonInfo> lessons;
    
    @Data
    public static class UserBasicInfo {
        private Long id;
        private String username;
        private String fullName;
    }
    
    @Data
    public static class LessonInfo {
        private Long id;
        private String title;
    }
}