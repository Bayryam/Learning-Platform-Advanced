package course.spring.elearningplatform.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super(String.format("User with email '%s' already exists!", email));
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super(String.format("User with email '%s' already exists!", email), cause);
    }

    public DuplicateEmailException(Throwable cause) {
        super(cause);
    }

    public DuplicateEmailException() {
        super("User with that email already exists!");
    }
}
