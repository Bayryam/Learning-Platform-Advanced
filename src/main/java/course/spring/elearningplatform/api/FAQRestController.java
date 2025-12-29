package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.FAQDto;
import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.FAQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/faq")
public class FAQRestController {

    private final FAQService faqService;
    private final ActivityLogService activityLogService;

    @Autowired
    public FAQRestController(FAQService faqService, ActivityLogService activityLogService) {
        this.faqService = faqService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAllQuestions() {
        return ResponseEntity.ok(faqService.getAllQuestions());
    }

    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody FAQDto faqDto,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        faqService.addQuestion(faqDto);
        activityLogService.logActivity("FAQ added", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "FAQ created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        faqService.deleteQuestion(id);
        activityLogService.logActivity("FAQ deleted", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "FAQ deleted successfully"));
    }
}