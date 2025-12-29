package course.spring.elearningplatform.api;

import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.service.CourseService;
import course.spring.elearningplatform.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
@PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
public class InstructorRestController {

    private final CourseService courseService;
    private final EventService eventService;

    @Autowired
    public InstructorRestController(CourseService courseService, EventService eventService) {
        this.courseService = courseService;
        this.eventService = eventService;
    }

    @GetMapping("/courses")
    public ResponseEntity<?> getCourses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(courseService.getAllCoursesByUser(userDetails.getUser()));
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(eventService.getAllEventsByUser(userDetails.getUsername()));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("courses", courseService.getAllCoursesByUser(userDetails.getUser()));
        dashboard.put("events", eventService.getAllEventsByUser(userDetails.getUsername()));
        return ResponseEntity.ok(dashboard);
    }
}