package course.spring.elearningplatform.dto.response;

import course.spring.elearningplatform.entity.Role;
import lombok.Data;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Set<Role> roles;
    private String profilePictureBase64;
    private boolean active;
}