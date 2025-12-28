package course.spring.elearningplatform.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super(String.format("User with username '%s' already exists!", username));
    }

    public DuplicateUsernameException(String username, Throwable cause) {
        super(String.format("User with username '%s' already exists!", username), cause);
    }

    public DuplicateUsernameException(Throwable cause) {
        super(cause);
    }

    public DuplicateUsernameException() {
        super("User with that username already exists!");
    }
}
