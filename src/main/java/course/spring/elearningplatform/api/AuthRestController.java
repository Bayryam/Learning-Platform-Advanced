package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.UserDto;
import course.spring.elearningplatform.entity.User;
import course.spring.elearningplatform.exception.DuplicateEmailException;
import course.spring.elearningplatform.exception.DuplicateUsernameException;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    @Autowired
    private AuthenticationManager authenticationManager;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    @Autowired
    public AuthRestController(UserService userService, ActivityLogService activityLogService) {
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/logout")
public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession(false);
    if (session != null) {
        session.invalidate();
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, password);

            Authentication authentication = authenticationManager.authenticate(authToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Save to session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDto userDto) {
        try {
            userService.createUser(userDto);
            activityLogService.logActivity("New user registered", userDto.getUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            return ResponseEntity.ok(response);
        } catch (DuplicateUsernameException | DuplicateEmailException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));  // Return 200 instead of 401
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
}