package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.AssignmentDto;
import course.spring.elearningplatform.dto.CourseDto;
import course.spring.elearningplatform.dto.LessonDto;
import course.spring.elearningplatform.dto.mapper.EntityMapper;
import course.spring.elearningplatform.dto.response.CourseResponse;
import course.spring.elearningplatform.entity.Course;
import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.entity.Lesson;
import course.spring.elearningplatform.entity.User;
import course.spring.elearningplatform.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseRestController {

    private final CourseService courseService;
    private final LessonService lessonService;
    private final UserService userService;
    private final SolutionService solutionService;
    private final CourseDashboardService courseDashboardService;
    private final ActivityLogService activityLogService;

    @Autowired
    public CourseRestController(CourseService courseService, LessonService lessonService,
                                UserService userService, SolutionService solutionService,
                                CourseDashboardService courseDashboardService,
                                ActivityLogService activityLogService) {
        this.courseService = courseService;
        this.lessonService = lessonService;
        this.userService = userService;
        this.solutionService = solutionService;
        this.courseDashboardService = courseDashboardService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAllCourses() {
        Map<String, Set<Course>> coursesByCategory = courseService.getCoursesGroupedByCategory();
        Map<String, List<CourseResponse>> responseByCategory = coursesByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(this::toCourseResponse)
                                .collect(Collectors.toList())
                ));
        return ResponseEntity.ok(responseByCategory);
    }

    @GetMapping("/top3")
    public ResponseEntity<?> getTop3CoursesByCategory() {
        Map<String, Set<Course>> coursesByCategory = courseService.getCoursesGroupedByCategory();
        Map<String, List<CourseResponse>> top3 = coursesByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .limit(3)
                                .map(this::toCourseResponse)
                                .toList()
                ));
        return ResponseEntity.ok(top3);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCourse(@Valid @ModelAttribute CourseDto courseDto,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course newCourse = courseService.addCourse(courseDto, userDetails.user());
        activityLogService.logActivity("New course created", userDetails.getUsername());
        return ResponseEntity.ok(toCourseResponse(newCourse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        Course course = courseService.getCourseById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("course", toCourseResponse(course));

        Long quizId = courseService.getCourseQuizId(id);
        response.put("hasQuiz", quizId != null);

        if (userDetails != null) {
            User user = userService.getUserByUsername(userDetails.getUsername());

            List<AssignmentDto> assignments = course.getAssignments().stream()
                    .map(assignment -> EntityMapper.mapEntityToDto(assignment, AssignmentDto.class))
                    .toList();

            Map<Long, Boolean> userSolutionStatus = assignments.stream()
                    .collect(Collectors.toMap(
                            AssignmentDto::getId,
                            assignment -> solutionService.hasUserUploadedSolution(user.getId(), assignment.getId())
                    ));

            response.put("assignments", assignments);
            response.put("userSolutionStatus", userSolutionStatus);
            response.put("highscores", courseService.getHighScoresForCourse(id));
            response.put("analytics", course.getAnalytics());
            response.put("allLessonsCompleted", courseService.areAllLessonsCompletedByUser(user, course));
            response.put("isCreator", course.getCreatedBy().getId().equals(user.getId()));
            response.put("isCourseStarted", user.getStartedCourses().contains(course));
            response.put("isCourseCompleted", user.getCompletedCourses().contains(course));
            response.put("hasQuiz", course.getQuiz() != null);
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<?> updateCourseName(@PathVariable Long id,
                                              @RequestBody Map<String, String> payload,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        String newName = payload.get("name");
        courseService.updateCourseDetails(id, "course-name", newName);
        activityLogService.logActivity("Course name updated", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Name updated successfully"));
    }

    @PatchMapping("/{id}/description")
    public ResponseEntity<?> updateCourseDescription(@PathVariable Long id,
                                                     @RequestBody Map<String, String> payload,
                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        String newDescription = payload.get("description");
        courseService.updateCourseDetails(id, "course-description", newDescription);
        activityLogService.logActivity("Course description updated", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Description updated successfully"));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startCourse(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        Course startedCourse = courseService.startCourse(id, user);
        userService.addStartedCourse(user, startedCourse);
        activityLogService.logActivity("Course started", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Course started successfully"));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getCoursesByCategory(@PathVariable String category) {
        List<Course> courses = courseService.getCoursesByCategory(category);
        List<CourseResponse> response = courses.stream()
                .map(this::toCourseResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{id}")
    public ResponseEntity<?> getStudentCourses(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        List<CourseResponse> startedCourses = courseService.getAllInProgressCoursesByUser(id).stream()
                .map(this::toCourseResponse)
                .collect(Collectors.toList());
        List<CourseResponse> completedCourses = courseService.findCompletedCoursesByUserId(id).stream()
                .map(this::toCourseResponse)
                .collect(Collectors.toList());
        response.put("startedCourses", startedCourses);
        response.put("completedCourses", completedCourses);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/lessons")
    public ResponseEntity<?> createLesson(@PathVariable Long id,
                                          @Valid @RequestBody LessonDto lessonDto,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course course = courseService.getCourseById(id);
        lessonService.addLesson(lessonDto, course);
        activityLogService.logActivity("New lesson created", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Lesson created successfully"));
    }

    @GetMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<?> getLesson(@PathVariable Long courseId, @PathVariable Long lessonId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course course = courseService.getCourseById(courseId);
        Lesson lesson = lessonService.getLessonById(lessonId);
        User user = userService.getUserByUsername(userDetails.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("course", toCourseResponse(course));
        response.put("lesson", toLessonResponse(lesson));

        // Map completed lessons to simple response objects
        List<Map<String, Object>> completedLessonsInfo = user.getCompletedLessons().stream()
                .map(l -> Map.of("id", (Object) l.getId(), "title", (Object) l.getTitle()))
                .collect(java.util.stream.Collectors.toList());

        response.put("completedLessons", completedLessonsInfo);
        response.put("isCreator", course.getCreatedBy().getId().equals(user.getId()));

        return ResponseEntity.ok(response);
    }

    // Add this helper method at the end of the class
    private course.spring.elearningplatform.dto.response.LessonResponse toLessonResponse(Lesson lesson) {
        course.spring.elearningplatform.dto.response.LessonResponse response = new course.spring.elearningplatform.dto.response.LessonResponse();
        response.setId(lesson.getId());
        response.setTitle(lesson.getTitle());
        response.setContent(lesson.getContent());
        response.setCreatedOn(lesson.getCreatedOn());
        return response;
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<?> markLessonComplete(@PathVariable Long courseId, @PathVariable Long lessonId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        Lesson lesson = lessonService.getLessonById(lessonId);
        user.getCompletedLessons().add(lesson);
        userService.save(user);
        activityLogService.logActivity("Completed lesson", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Lesson marked as completed"));
    }

    private CourseResponse toCourseResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setName(course.getName());
        response.setDescription(course.getDescription());
        response.setCategories(course.getCategories());
        response.setCreatedOn(course.getCreatedOn());
        response.setParticipantCount(course.getParticipants() != null ? course.getParticipants().size() : 0);
        response.setCompletedCount(course.getStudentsCompletedCourse() != null ? course.getStudentsCompletedCourse().size() : 0);
        response.setLessonsCount(course.getLessons() != null ? course.getLessons().size() : 0);

        if (course.getImage() != null && course.getImage().getImage() != null) {
            response.setImageBase64(java.util.Base64.getEncoder().encodeToString(course.getImage().getImage()));
        }

        if (course.getCreatedBy() != null) {
            CourseResponse.UserBasicInfo userInfo = new CourseResponse.UserBasicInfo();
            userInfo.setId(course.getCreatedBy().getId());
            userInfo.setUsername(course.getCreatedBy().getUsername());
            userInfo.setFullName(course.getCreatedBy().getFullName());
            response.setCreatedBy(userInfo);
        }

        if (course.getLessons() != null) {
            List<CourseResponse.LessonInfo> lessonInfoList = course.getLessons().stream()
                    .map(lesson -> {
                        CourseResponse.LessonInfo lessonInfo = new CourseResponse.LessonInfo();
                        lessonInfo.setId(lesson.getId());
                        lessonInfo.setTitle(lesson.getTitle());
                        return lessonInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            response.setLessons(lessonInfoList);
        }

        return response;
    }
}