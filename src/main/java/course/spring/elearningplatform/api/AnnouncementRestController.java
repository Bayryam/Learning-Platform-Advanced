package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.AnnouncementDto;
import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementRestController {

    private final AnnouncementService announcementService;
    private final ActivityLogService activityLogService;

    @Autowired
    public AnnouncementRestController(AnnouncementService announcementService,
                                     ActivityLogService activityLogService) {
        this.announcementService = announcementService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAllActiveAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllActiveAnnouncements());
    }

    @GetMapping("/strings")
    public ResponseEntity<List<String>> getAllActiveAnnouncementsAsStrings() {
        return ResponseEntity.ok(announcementService.getAllActiveAnnouncementsAsStrings());
    }

    @PostMapping
    public ResponseEntity<?> createAnnouncement(@RequestBody AnnouncementDto announcementDto,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        announcementService.addAnnouncement(announcementDto);
        activityLogService.logActivity("New announcement added", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Announcement created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        announcementService.deleteAnnouncement(id);
        activityLogService.logActivity("Deleted announcement", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
    }
}