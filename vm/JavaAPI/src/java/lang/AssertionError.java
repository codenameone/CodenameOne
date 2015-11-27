package java.lang;

/**
 *
 * @author shai
 */
public class AssertionError extends Error {
    public AssertionError() {
        super();
    }
    public AssertionError(String detailMessage) {
    }
    
    public AssertionError(Object detailMessage) {
    }

    public AssertionError(boolean detailMessage) {}

    public AssertionError(char detailMessage) {}

    public AssertionError(int detailMessage) {}
    
    public AssertionError(long detailMessage) {}
    
    public AssertionError(float detailMessage) {}
    
    public AssertionError(double detailMessage) {}
    
    public AssertionError(String message, Throwable cause) {}
}
