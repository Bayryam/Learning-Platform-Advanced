package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.AssignmentDto;
import course.spring.elearningplatform.dto.mapper.EntityMapper;
import course.spring.elearningplatform.entity.Assignment;
import course.spring.elearningplatform.entity.Course;
import course.spring.elearningplatform.entity.Event;
import course.spring.elearningplatform.entity.User;
import course.spring.elearningplatform.service.AssignmentService;
import course.spring.elearningplatform.service.CourseService;
import course.spring.elearningplatform.service.EventService;
import course.spring.elearningplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/home")
public class HomeRestController {

    private final EventService eventService;
    private final UserService userService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;

    @Autowired
    public HomeRestController(EventService eventService, CourseService courseService,
                             UserService userService, AssignmentService assignmentService) {
        this.eventService = eventService;
        this.courseService = courseService;
        this.userService = userService;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ResponseEntity<?> getHomeData(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        if (userDetails != null) {
            User loggedUser = userService.getUserByUsername(userDetails.getUsername());
            response.put("user", loggedUser);
        }

        // Get upcoming events (top 3)
        List<Event> upcomingEvents = eventService.getAllEvents().stream()
                .filter(event -> event.getStartTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Event::getStartTime))
                .limit(3)
                .collect(Collectors.toList());

        // Get top 3 courses by category
        Map<String, java.util.Set<Course>> coursesByCategory = courseService.getCoursesGroupedByCategory();
        Map<String, List<Course>> top3CoursesByCategory = coursesByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().limit(3).toList()
                ));

        // Get upcoming assignments (top 3)
        List<AssignmentDto> assignmentDtos = assignmentService.getAllAssignments();
        List<Assignment> upcomingAssignments = assignmentDtos.stream()
                .map(dto -> EntityMapper.mapCreateDtoToEntity(dto, Assignment.class))
                .filter(a -> a.getDueDate().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Assignment::getDueDate))
                .limit(3)
                .collect(Collectors.toList());

        response.put("upcomingEvents", upcomingEvents);
        response.put("top3CoursesByCategory", top3CoursesByCategory);
        response.put("upcomingAssignments", upcomingAssignments);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-role")
    public ResponseEntity<?> checkUserRole(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("role", "GUEST"));
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        response.put("isAdmin", user.isAdmin());
        response.put("isInstructor", user.hasRole("ROLE_INSTRUCTOR"));
        response.put("isStudent", !user.isAdmin() && !user.hasRole("ROLE_INSTRUCTOR"));
        response.put("username", user.getUsername());
        
        return ResponseEntity.ok(response);
    }
}