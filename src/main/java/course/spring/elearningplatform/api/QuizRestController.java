package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.QuizSubmissionRequest;
import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.entity.QuestionWrapper;
import course.spring.elearningplatform.entity.QuizDto;
import course.spring.elearningplatform.entity.Response;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.CourseService;
import course.spring.elearningplatform.service.impl.QuizzesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizRestController {

    private final QuizzesService quizzesService;
    private final CourseService courseService;
    private final ActivityLogService activityLogService;

    @Autowired
    public QuizRestController(QuizzesService quizzesService, CourseService courseService,
                             ActivityLogService activityLogService) {
        this.quizzesService = quizzesService;
        this.courseService = courseService;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createQuiz(@RequestParam long courseId, @RequestBody QuizDto quizDto,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            quizzesService.addQuizToCourse(courseId, quizDto);
            activityLogService.logActivity("Quiz created", userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "Quiz created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create quiz: " + e.getMessage()));
        }
    }

    @GetMapping("/{quizId}/details")
    public ResponseEntity<?> getQuizDetails(@PathVariable long quizId) {
        try {
            var quiz = quizzesService.getQuizById(quizId);
            Map<String, Object> response = new HashMap<>();
            response.put("id", quiz.getId());
            response.put("title", quiz.getTitle());
            response.put("questions", quiz.getQuestions());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to load quiz details: " + e.getMessage()));
        }
    }

    @PutMapping("/{quizId}")
    public ResponseEntity<?> updateQuiz(@PathVariable long quizId,
                                    @RequestParam long courseId,
                                    @RequestBody QuizDto quizDto,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            quizzesService.updateQuiz(quizId, courseId, quizDto);
            activityLogService.logActivity("Quiz updated", userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "Quiz updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update quiz: " + e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getQuizForCourse(@PathVariable long courseId) {
        try {
            List<QuestionWrapper> quizQuestions = courseService.getQuestionsForCourseQuiz(courseId);
            Long quizId = courseService.getCourseQuizId(courseId);

            Map<String, Object> response = new HashMap<>();
            response.put("quizId", quizId);
            response.put("courseId", courseId);
            response.put("quizQuestions", quizQuestions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to load quiz: " + e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Integer>> submitQuiz(@RequestParam long courseId,
                                                           @RequestParam long quizId,
                                                           @RequestBody QuizSubmissionRequest submission,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Response> answers = submission.getAnswers();
        long elapsedTime = submission.getElapsedTime();
        activityLogService.logActivity("Quiz submitted", userDetails.getUsername());
        return quizzesService.calculateQuizResult(courseId, quizId, answers, elapsedTime, userDetails.getUsername());
    }
}