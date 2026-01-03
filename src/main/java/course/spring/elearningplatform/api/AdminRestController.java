package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.UserDto;
import course.spring.elearningplatform.entity.*;
import course.spring.elearningplatform.exception.DuplicateEmailException;
import course.spring.elearningplatform.exception.DuplicateUsernameException;
import course.spring.elearningplatform.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminRestController {

    private final UserService userService;
    private final GroupService groupService;
    private final ArticleService articleService;
    private final AnnouncementService announcementService;
    private final FAQService faqService;
    private final NewsService newsService;
    private final CourseService courseService;
    private final ActivityLogService activityLogService;

    @Autowired
    public AdminRestController(UserService userService, GroupService groupService,
                               ArticleService articleService, AnnouncementService announcementService,
                               FAQService faqService, NewsService newsService,
                               CourseService courseService, ActivityLogService activityLogService) {
        this.userService = userService;
        this.groupService = groupService;
        this.articleService = articleService;
        this.announcementService = announcementService;
        this.faqService = faqService;
        this.newsService = newsService;
        this.courseService = courseService;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDto userDto) {
        try {
            userService.createUser(userDto);
            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } catch (DuplicateUsernameException | DuplicateEmailException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String search,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        String loggedInUsername = userDetails.getUsername();
        Page<User> userPage;

        if (search != null && !search.isEmpty()) {
            userPage = userService.searchUsers(search, page, size, loggedInUsername);
        } else {
            userPage = userService.getAllUsers(page, size, loggedInUsername);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber() + 1);
        response.put("totalPages", userPage.getTotalPages());
        response.put("totalElements", userPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersExcept(List.of("deletedUser")));
    }

    @GetMapping("/student-groups")
    public ResponseEntity<?> getStudentGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/student-groups/{id}")
    public ResponseEntity<?> getStudentGroup(@PathVariable Long id) {
        Group group = groupService.getGroupById(id);
        List<Article> articles = group.getArticles();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy h:mm a");
        Map<Long, String> articleDateMap = articles.stream()
                .collect(Collectors.toMap(
                        Article::getId,
                        article -> article.getCreatedAt().format(formatter)
                ));

        List<String> members = group.getMembers().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
        members.add("deletedUser");

        Map<String, Object> response = new HashMap<>();
        response.put("group", group);
        response.put("articles", articles);
        response.put("articleDateMap", articleDateMap);
        response.put("availableUsers", userService.getAllUsersExcept(members));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/student-groups/{id}")
    public ResponseEntity<?> deleteStudentGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
    }

    @PostMapping("/student-groups/{groupId}/remove-member")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId, @RequestParam Long userId) {
        User user = userService.getUserById(userId);
        groupService.removeMember(groupId, user);
        return ResponseEntity.ok(Map.of("message", "User " + user.getUsername() + " removed successfully"));
    }

    @PostMapping("/student-groups/{groupId}/add-member")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestParam Long userId) {
        String username = userService.getUserById(userId).getUsername();
        groupService.addMember(groupId, username);
        return ResponseEntity.ok(Map.of("message", "User " + username + " added successfully"));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticleById(id);
        return ResponseEntity.ok(Map.of("message", "Article deleted successfully"));
    }

    @GetMapping("/announcements")
    public ResponseEntity<?> getAnnouncements() {
        List<Announcement> announcements = announcementService.getAllActiveAnnouncements();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy h:mm a");

        Map<Long, String> announcementDateMap = announcements.stream()
                .filter(a -> a.getExpiresAt() != null)
                .collect(Collectors.toMap(
                        Announcement::getId,
                        announcement -> announcement.getExpiresAt().format(formatter)
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("announcements", announcements);
        response.put("announcementDateMap", announcementDateMap);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/faq")
    public ResponseEntity<?> getFAQ() {
        return ResponseEntity.ok(faqService.getAllQuestions());
    }

    @GetMapping("/news")
    public ResponseEntity<?> getNews() {
        return ResponseEntity.ok(newsService.getAllNews());
    }

    @GetMapping("/courses")
    public ResponseEntity<?> getCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/activity-log")
    public ResponseEntity<?> getActivityLog() {
        Map<ActivityLog, String> logsMap = activityLogService.getAllLogs();
        List<Map<String, Object>> formattedLogs = new ArrayList<>();

        for (Map.Entry<ActivityLog, String> entry : logsMap.entrySet()) {
            ActivityLog log = entry.getKey();
            Map<String, Object> logData = new HashMap<>();
            logData.put("id", log.getId());
            logData.put("action", log.getAction());
            logData.put("username", log.getUsername());
            logData.put("timestamp", log.getTimestamp());
            logData.put("formattedDate", entry.getValue());
            formattedLogs.add(logData);
        }

        return ResponseEntity.ok(formattedLogs);
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        List<String> roles = Arrays.stream(Role.values())
                .map(Role::getDescription)
                .filter(description -> !description.equals(Role.UNREGISTERED.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }
}