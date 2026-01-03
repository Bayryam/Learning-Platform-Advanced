package course.spring.elearningplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@ToString(exclude = {"solutions", "startedCourses", "completedCourses", "completedLessons", "groups", "profilePicture", "courses", "tickets", "certificates"})
@AllArgsConstructor
@NoArgsConstructor(force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_UNREGISTERED = "UNREGISTERED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @EqualsAndHashCode.Include
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<Solution> solutions;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_picture_id")
    private Image profilePicture;

    @Transient
    private String profilePictureBase64;

    @NonNull
    @NotEmpty
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;


    public String getFullName() {
        return firstName + " " + lastName;
    }

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<Group> groups;

    @JsonIgnore
    @OneToMany
    private Set<Course> courses;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_started_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> startedCourses;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_completed_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> completedCourses;

    @OneToMany(mappedBy = "issuer")
    @JsonIgnore
    private List<Ticket> tickets;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_completed_lessons",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "lesson_id")
    )
    @JsonIgnore
    private Set<Lesson> completedLessons = new HashSet<>();

    @OneToMany(mappedBy = "issuedTo", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Certificate> certificates;

    public void addCertificate(Certificate certificate) {
        certificates.add(certificate);
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
    }

    public void addStartedCourse(Course course) {
        startedCourses.add(course);
    }

    public void addCompletedCourse(Course course) {
        startedCourses.remove(course);
        completedCourses.add(course);
    }

    public User(String username, String password, String firstName, String lastName, String email, @NonNull Set<String> roles) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.roles = roles;
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN.getDescription());
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}

