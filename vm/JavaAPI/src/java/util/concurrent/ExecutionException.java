package java.util.concurrent;

public class ExecutionException extends Exception {
    public ExecutionException() { }
    public ExecutionException(String message) { super(message); }
    public ExecutionException(String message, Throwable cause) { super(message, cause); }
    public ExecutionException(Throwable cause) { super(cause); }
}
