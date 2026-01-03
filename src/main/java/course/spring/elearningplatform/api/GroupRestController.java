package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.ArticleDto;
import course.spring.elearningplatform.dto.GroupDto;
import course.spring.elearningplatform.dto.ImageDto;
import course.spring.elearningplatform.entity.Group;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.ArticleService;
import course.spring.elearningplatform.service.GroupService;
import course.spring.elearningplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/groups")
public class GroupRestController {

    private final GroupService groupService;
    private final ArticleService articleService;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    @Autowired
    public GroupRestController(GroupService groupService, ArticleService articleService,
                              UserService userService, ActivityLogService activityLogService) {
        this.groupService = groupService;
        this.articleService = articleService;
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroupById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("group", groupService.getGroupById(id));
        response.put("articles", articleService.getAllArticlesForAGroup(id));
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createGroup(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "members", required = false) Set<String> members,
            @AuthenticationPrincipal UserDetails userDetails) {

        GroupDto groupDto = new GroupDto();
        groupDto.setName(name);
        groupDto.setDescription(description);
        groupDto.setMembers(members);

        if (image != null && !image.isEmpty()) {
            ImageDto imageDto = new ImageDto();
            imageDto.setImage(image);
            groupDto.setImage(imageDto);
        }

        Group createdGroup = groupService.createGroup(groupDto);
        groupService.addMember(createdGroup.getId(), userDetails.getUsername());
        activityLogService.logActivity("Group created", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Group created successfully", "groupId", createdGroup.getId()));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinGroup(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        groupService.addMember(id, userDetails.getUsername());
        activityLogService.logActivity("Group joined", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Joined group successfully"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        groupService.removeMember(id, userService.getUserByUsername(userDetails.getUsername()));
        activityLogService.logActivity("Left group", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Left group successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        groupService.deleteGroup(id);
        activityLogService.logActivity("Group deleted", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
    }

    @PostMapping("/{id}/articles")
    public ResponseEntity<?> createArticle(@PathVariable Long id, @RequestBody ArticleDto articleDto,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        articleService.createArticle(id, articleDto);
        activityLogService.logActivity("Article created", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Article created successfully"));
    }

    @DeleteMapping("/{groupId}/articles/{articleId}")
    public ResponseEntity<?> deleteArticle(@PathVariable Long groupId, @PathVariable Long articleId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        articleService.deleteArticleById(articleId);
        activityLogService.logActivity("Article deleted", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Article deleted successfully"));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable Long id, @RequestParam String username,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        groupService.addMember(id, username);
        activityLogService.logActivity("Member added", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Member added successfully"));
    }
}