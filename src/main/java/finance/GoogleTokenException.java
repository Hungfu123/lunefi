package finance;

public class GoogleTokenException extends RuntimeException {

    public GoogleTokenException(String message) {
        super(message);
    }

    public GoogleTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
