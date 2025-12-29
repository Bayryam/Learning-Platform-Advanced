package course.spring.elearningplatform.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class LessonResponse {
    private Long id;
    private String title;
    private String content;
    private Date createdOn;
}