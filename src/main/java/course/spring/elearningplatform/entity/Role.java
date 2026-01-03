package course.spring.elearningplatform.entity;

import org.springframework.security.core.GrantedAuthority;
import lombok.Getter;

@Getter
public enum Role implements GrantedAuthority {

    STUDENT("ROLE_STUDENT"),
    INSTRUCTOR("ROLE_INSTRUCTOR"),
    ADMIN("ROLE_ADMIN"),
    UNREGISTERED("ROLE_UNREGISTERED"),
    INSTRUCTOR2("INSTRUCTOR");

    Role(String description) {
        this.description = description;
    }

    private final String description;

    public String getDescription() {
        return description;
    }

    @Override
    public String getAuthority() {
        return description;
    }
}
