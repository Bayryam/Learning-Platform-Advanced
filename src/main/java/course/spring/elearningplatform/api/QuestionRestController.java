package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.QuestionDto;
import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.CourseService;
import course.spring.elearningplatform.service.impl.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionRestController {

    private final CourseService courseService;
    private final QuestionService questionService;
    private final ActivityLogService activityLogService;

    @Autowired
    public QuestionRestController(CourseService courseService, QuestionService questionService,
                                 ActivityLogService activityLogService) {
        this.courseService = courseService;
        this.questionService = questionService;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getAllQuestionsForCourse(@PathVariable long courseId) {
        return ResponseEntity.ok(courseService.getAllQuestionsForCourse(courseId));
    }

    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestParam long courseId,
                                           @RequestBody QuestionDto questionDto,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            courseService.addQuestionToCourse(courseId, questionDto);
            activityLogService.logActivity("Question created", userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "Question created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create question: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> deleteQuestion(@RequestParam long courseId,
                                           @PathVariable long questionId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        questionService.deleteQuestionFromCourse(courseId, questionId);
        questionService.deleteQuestionFromQuiz(questionId);
        activityLogService.logActivity("Question deleted", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Question deleted successfully"));
    }
}