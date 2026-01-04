package course.spring.elearningplatform.api;

import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.entity.News;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import course.spring.elearningplatform.service.impl.ExternalNewsService;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsRestController {

    private final NewsService newsService;
    private final ActivityLogService activityLogService;
    private final ExternalNewsService externalNewsService;

    @Autowired
    public NewsRestController(NewsService newsService, ActivityLogService activityLogService, ExternalNewsService externalNewsService) {
        this.newsService = newsService;
        this.activityLogService = activityLogService;
        this.externalNewsService = externalNewsService;
    }

    @GetMapping
    public ResponseEntity<?> getAllNews() {
        return ResponseEntity.ok(newsService.getAllNews());
    }

    @GetMapping("/external")
    public ResponseEntity<?> getExternalNews(@RequestParam(defaultValue = "6") int pageSize) {
        Map<String, Object> news = externalNewsService.getEducationalNews(pageSize);
        return ResponseEntity.ok(news);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNewsById(@PathVariable Long id) {
        News news = newsService.getNewsById(id);
        return ResponseEntity.ok(news);
    }

    @PostMapping
    public ResponseEntity<?> createNews(@RequestBody News news) {
        news.setPublishedDate(LocalDateTime.now());
        newsService.saveNews(news);  // Changed from addNews to saveNews
        activityLogService.logActivity("News added", news.getAuthor());
        return ResponseEntity.ok(Map.of("message", "News created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNews(@PathVariable Long id,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        newsService.deleteNews(id);
        activityLogService.logActivity("News deleted", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "News deleted successfully"));
    }
}