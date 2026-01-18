package course.spring.elearningplatform.entity;

import lombok.Data;
import java.util.List;

@Data
public class QuizDto {
    private String title;
    private int numberOfQuestions;
    private List<Long> selectedQuestionIds;
}