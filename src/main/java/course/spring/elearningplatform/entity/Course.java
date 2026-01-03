package course.spring.elearningplatform.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Course {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    private String name;
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> categories;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    private User createdBy;
    private Date createdOn;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "image_id")
    private Image image;

    @Transient
    private String imageBase64;

    @JsonIgnore
    @ManyToMany(mappedBy = "startedCourses")
    private List<User> participants;

    @JsonIgnore
    @ManyToMany(mappedBy = "completedCourses")
    private List<User> studentsCompletedCourse;

    @OneToMany
    @JsonIgnore
    private List<Lesson> lessons;

    @OneToMany
    @JsonIgnore
    private List<Question> questions;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Quiz quiz;

    @ManyToMany(cascade = CascadeType.ALL)
    private List<StudentResult> highScores;
    @JsonIgnore
    @OneToMany(mappedBy = "forCourse")
    private List<Ticket> tickets;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Assignment> assignments = new ArrayList<>();

    @OneToOne
    private CourseAnalytics analytics;

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
    }

    public void addParticipant(User user) {
        participants.add(user);
    }

    public void removeParticipant(User user) {
        participants.remove(user);
    }

    public void addStudentCompletedCourse(User user) {
        studentsCompletedCourse.add(user);
    }

    public void addCategory(String category) {
        categories.add(category);
    }

    public Lesson getLessonById(Long lessonId) {
        return lessons.stream()
                .filter(lesson -> lesson.getId().equals(lessonId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found with ID: " + lessonId));
    }
}
