package course.spring.elearningplatform.service.impl;

import course.spring.elearningplatform.entity.Certificate;
import course.spring.elearningplatform.entity.Course;
import course.spring.elearningplatform.entity.Question;
import course.spring.elearningplatform.entity.Quiz;
import course.spring.elearningplatform.entity.QuizDto;
import course.spring.elearningplatform.entity.Response;
import course.spring.elearningplatform.entity.StudentResult;
import course.spring.elearningplatform.entity.User;
import course.spring.elearningplatform.exception.EntityNotFoundException;
import course.spring.elearningplatform.repository.CertificateRepository;
import course.spring.elearningplatform.repository.CourseRepository;
import course.spring.elearningplatform.repository.QuizRepository;
import course.spring.elearningplatform.repository.StudentResultRepository;
import course.spring.elearningplatform.service.CourseService;
import course.spring.elearningplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuizzesService {
    private final QuizRepository quizRepository;
    private final UserService userService;
    private final CourseService courseService;


    @Autowired
    public QuizzesService(QuizRepository quizRepository,
                          UserService userService,
                          CourseService courseService) {
        this.quizRepository = quizRepository;
        this.userService = userService;
        this.courseService = courseService;
    }


    public Quiz createQuiz(QuizDto quizDto, List<Question> allQuestions) {
    var quiz = new Quiz();
    quiz.setTitle(quizDto.getTitle());

    List<Question> selectedQuestions;
    
   
    if (quizDto.getSelectedQuestionIds() != null && !quizDto.getSelectedQuestionIds().isEmpty()) {
        selectedQuestions = allQuestions.stream()
            .filter(q -> quizDto.getSelectedQuestionIds().contains(q.getId()))
            .toList();
        
        if (selectedQuestions.isEmpty()) {
            throw new EntityNotFoundException("None of the selected questions were found.");
        }
    } else {
       
        if (allQuestions.isEmpty()) {
            throw new EntityNotFoundException("There are no questions for creating a quiz. Try adding some.");
        }
        
        Collections.shuffle(allQuestions);
        selectedQuestions = allQuestions.stream()
            .limit(quizDto.getNumberOfQuestions())
            .toList();
    }

    quiz.setQuestions(new ArrayList<>(selectedQuestions));
    return quizRepository.save(quiz);
}

    public Quiz getQuizById(long id) {
        return quizRepository.findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(String.format("Quiz with id %s not found", id), "redirect:/home"));

    }

    public void updateQuiz(long quizId, long courseId, QuizDto quizDto) {
        var quiz = getQuizById(quizId);
        var allQuestions = courseService.getAllQuestionsForCourse(courseId);

       
        quiz.setTitle(quizDto.getTitle());

       
        List<Question> selectedQuestions;
        if (quizDto.getSelectedQuestionIds() != null && !quizDto.getSelectedQuestionIds().isEmpty()) {
            selectedQuestions = allQuestions.stream()
                .filter(q -> quizDto.getSelectedQuestionIds().contains(q.getId()))
                .toList();

            if (selectedQuestions.isEmpty()) {
                throw new EntityNotFoundException("None of the selected questions were found.");
            }
        } else {
            throw new EntityNotFoundException("Please select at least one question.");
        }

        quiz.setQuestions(new ArrayList<>(selectedQuestions));
        quizRepository.save(quiz);
    }

    @Transactional
    public ResponseEntity<Map<String, Integer>> calculateQuizResult(long courseId, long quizId,
                                                                    List<Response> answers,
                                                                    long elapsedTime,
                                                                    String username) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        var quiz = quizOptional.get();
        List<Question> questionsDB = quiz.getQuestions();

        int rightAnswers =
                Math.toIntExact(answers.stream().filter(answer -> isCorrectAnswer(answer, questionsDB)).count());

        Map<String, Integer> result = new HashMap<>();
        result.put("score", rightAnswers);
        result.put("totalQuestions", questionsDB.size());

        int percentage = Math.toIntExact(Math.round((rightAnswers * 100.0) / questionsDB.size()));
        result.put("percentage", percentage);

        courseService.addNewStudentResult(percentage, elapsedTime, courseId);

        User user = userService.getUserByUsername(username);
        Course course = courseService.getCourseById(courseId);

        boolean isAlreadyCompleted = user.getCompletedCourses().stream()
                .anyMatch(c -> c.getId().equals(courseId));

        if (!isAlreadyCompleted) {
            user.addCompletedCourse(course);
            userService.save(user);
        }

        return ResponseEntity.ok(result);
    }

    private boolean isCorrectAnswer(Response answer, List<Question> questions) {
        return questions.stream()
                .filter(question -> question.getId() == answer.getQuestionId())
                .findFirst()
                .map(question -> question.getCorrectAnswer().equals(answer.getAnswer()))
                .orElse(false);
    }


    private void completeCourse(Course course, User user, Certificate savedCertificate) {
        user.addCertificate(savedCertificate);

        course.removeParticipant(user);
        course.addStudentCompletedCourse(user);
        Course savedCourse = courseService.save(course);
        user.addCompletedCourse(savedCourse);

        userService.save(user);
    }

    public void deleteQuestionFromQuiz(long quizId, Question question) {
        var quiz = getQuizById(quizId);
        quiz.getQuestions().remove(question);
        quizRepository.save(quiz);
    }

    public Quiz getQuizForQuestion(long id) {
        return quizRepository.findByQuestionId(id);
    }

    public Course addQuizToCourse(long courseId, QuizDto quizDto) {
        var quiz = createQuiz(quizDto, courseService.getAllQuestionsForCourse(courseId));
        return courseService.addQuizToCourse(courseId, quiz);
    }
}
