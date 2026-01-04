package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.ImageDto;
import course.spring.elearningplatform.entity.Course;
import course.spring.elearningplatform.entity.Role;
import course.spring.elearningplatform.entity.User;
import course.spring.elearningplatform.repository.UserRepository;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @Autowired
    public UserRestController(UserService userService, ActivityLogService activityLogService, UserRepository userRepository) {
        this.userService = userService;
        this.activityLogService = activityLogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query, 
                                         @RequestParam String loggedInUsername) {
        List<String> usernames = userService.getAllUsers().stream()
                .map(User::getUsername)
                .filter(username -> !username.equals(loggedInUsername))
                .filter(username -> username.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(usernames);
    }

    @GetMapping("/instructors")
    public ResponseEntity<?> getAllInstructors() {
        List<User> instructors = userService.getAllUsersByRole(Role.INSTRUCTOR2);
        return ResponseEntity.ok(instructors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUser(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, 
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserById(id);
        String fullName = user.getFullName();
        userService.deleteUser(user);
        activityLogService.logActivity("User deleted", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "User deleted successfully", "deletedUserFullName", fullName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        userService.updateUser(user);
        activityLogService.logActivity("User updated", user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User updated successfully"));
    }

    @GetMapping("/{id}/certificates")
    public ResponseEntity<?> getUserCertificates(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user.getCompletedCourses());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newRole = payload.get("role");
        userService.updateUserDetails(id, "role", newRole);
        return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
    }

    @PatchMapping("/{id}/full-name")
    public ResponseEntity<?> updateFullName(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newName = payload.get("fullName");
        userService.updateUserDetails(id, "name", newName);
        return ResponseEntity.ok(Map.of("message", "Full name updated successfully"));
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<?> updateEmail(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newEmail = payload.get("email");
        userService.updateUserDetails(id, "email", newEmail);
        return ResponseEntity.ok(Map.of("message", "Email updated successfully"));
    }

    @PatchMapping("/{id}/username")
    public ResponseEntity<?> updateUsername(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newUsername = payload.get("username");
        userService.updateUserDetails(id, "username", newUsername);
        return ResponseEntity.ok(Map.of("message", "Username updated successfully"));
    }

    @PatchMapping("/{id}/profile-picture")
    public ResponseEntity<?> updateProfilePicture(@RequestParam("profilePicture") MultipartFile file,
                                                   @RequestParam("id") Long userId) {
        ImageDto imageDto = new ImageDto(file);
        userService.updateUserDetails(userId, "profilePicture", imageDto);
        return ResponseEntity.ok(Map.of("message", "Profile picture updated successfully"));
    }

    @GetMapping("/{userId}/enrolled-courses")
    public ResponseEntity<List<Long>> getEnrolledCourses(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        List<Course> startedCourses = userRepository.findStartedCoursesByUserId(userId);

        if (startedCourses == null || startedCourses.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Long> courseIds = startedCourses.stream()
                .map(Course::getId)
                .toList();

        return ResponseEntity.ok(courseIds);
    }
}