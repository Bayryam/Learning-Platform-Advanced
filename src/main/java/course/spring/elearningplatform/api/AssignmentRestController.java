package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.AssignmentDto;
import course.spring.elearningplatform.dto.mapper.EntityMapper;
import course.spring.elearningplatform.entity.Assignment;
import course.spring.elearningplatform.entity.Course;
import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.entity.Solution;
import course.spring.elearningplatform.entity.User;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.AssignmentService;
import course.spring.elearningplatform.service.CourseService;
import course.spring.elearningplatform.service.SolutionService;
import course.spring.elearningplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentRestController {

    private final AssignmentService assignmentService;
    private final SolutionService solutionService;
    private final CourseService courseService;
    private final ActivityLogService activityLogService;
    private final UserService userService;

    @Autowired
    public AssignmentRestController(AssignmentService assignmentService, SolutionService solutionService,
                                    CourseService courseService, ActivityLogService activityLogService, UserService userService) {
        this.assignmentService = assignmentService;
        this.solutionService = solutionService;
        this.courseService = courseService;
        this.activityLogService = activityLogService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getAllAssignments() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userDetails.user();
        User realUser = userService.getUserById(user.getId());

        List<AssignmentDto> assignments = assignmentService.getAllAssignments()
                .stream()
                .filter(assignment -> realUser.getStartedCourses()
                        .stream()
                        .map(Course::getId)
                        .toList()
                        .contains(assignment.getCourseId()))
                .collect(Collectors.toList());

        Map<Long, Boolean> userSolutionStatus = assignments.stream()
                .collect(Collectors.toMap(
                        AssignmentDto::getId,
                        assignment -> solutionService.hasUserUploadedSolution(user.getId(), assignment.getId())
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("assignments", assignments);
        response.put("userSolutionStatus", userSolutionStatus);
        response.put("courses", courseService.getAllCourses().stream().map(course -> new Course(course.getId(), course.getName(),
                course.getDescription(), course.getCategories(), null, null, null, null, null,
                null, null, null, null, null, null, null, null)).toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getAssignmentsByCourse(@PathVariable Long courseId) {
        List<AssignmentDto> assignments = assignmentService.getAssignmentsByCourseId(courseId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAssignmentById(@PathVariable Long id) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userDetails.user();

        AssignmentDto assignment = assignmentService.getAssignmentById(id);
        String courseName = courseService.findById(assignment.getCourseId()).getName();
        boolean userSolutionUploaded = solutionService.hasUserUploadedSolution(user.getId(), id);

        Map<String, Object> response = new HashMap<>();
        response.put("assignment", assignment);
        response.put("courseName", courseName);
        response.put("userSolutionUploaded", userSolutionUploaded);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createAssignment(@RequestBody AssignmentDto assignmentDto,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        assignmentService.saveAssignment(assignmentDto);
        activityLogService.logActivity("New assignment created", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Assignment created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAssignment(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        assignmentService.deleteAssignment(id);
        activityLogService.logActivity("Deleted assignment", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Assignment deleted successfully"));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSolution(@RequestParam("assignmentId") Long assignmentId,
                                           @RequestParam("solutionFile") MultipartFile file) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            User user = userDetails.user();

            if (solutionService.hasUserUploadedSolution(user.getId(), assignmentId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You have already uploaded a solution for this assignment."));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty. Please upload a valid file."));
            }

            String uploadDir = "uploads/solutions/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Solution solution = new Solution();
            solution.setFilePath(filePath.toString());
            AssignmentDto assignmentDto = assignmentService.getAssignmentById(assignmentId);
            Assignment assignment = EntityMapper.mapCreateDtoToEntity(assignmentDto, Assignment.class);
            solution.setUser(user);
            solution.setAssignment(assignment);

            solutionService.saveSolution(solution);
            activityLogService.logActivity("Assignment uploaded", user.getUsername());

            return ResponseEntity.ok(Map.of("message", "Solution uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }
}