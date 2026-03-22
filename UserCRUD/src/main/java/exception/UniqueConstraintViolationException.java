package exception;

public class UniqueConstraintViolationException extends RuntimeException {
    private final String constraintName;

    public UniqueConstraintViolationException(String message, String constraintName, Throwable cause) {
        super(message, cause);
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }
}
