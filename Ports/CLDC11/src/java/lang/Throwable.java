package java.lang;
/**
 * The Throwable class is the superclass of all errors and exceptions in the Java language. Only objects that are instances of this class (or of one of its subclasses) are thrown by the Java Virtual Machine or can be thrown by the Java throw statement. Similarly, only this class or one of its subclasses can be the argument type in a catch clause.
 * Instances of two subclasses, Error and Exception, are conventionally used to indicate that exceptional situations have occurred. Typically, these instances are freshly created in the context of the exceptional situation so as to include relevant information (such as stack trace data).
 * By convention, class Throwable and its subclasses have two constructors, one that takes no arguments and one that takes a String argument that can be used to produce an error message.
 * A Throwable class contains a snapshot of the execution stack of its thread at the time it was created. It can also contain a message string that gives more information about the error.
 * Here is one example of catching an exception:
 * Since: JDK1.0, CLDC 1.0
 */
public class Throwable{
    /**
     * Constructs a new Throwable with null as its error message string.
     */
    public Throwable(){
         //TODO codavaj!!
    }

    /**
     * @deprecated DO NOT USE THIS METHOD, its here just to get the compiler working and isn't intended for use
     */
    public Throwable initCause(Throwable cause) {
        return null;
    }
    
    /**
     * Constructs a new Throwable with the specified error message.
     * message - the error message. The error message is saved for later retrieval by the
     * method.
     */
    public Throwable(java.lang.String message){
         //TODO codavaj!!
    }

    /**
     * Returns the error message string of this Throwable object.
     */
    public java.lang.String getMessage(){
        return null; //TODO codavaj!!
    }

    /**
     * Prints this Throwable and its backtrace to the standard error stream. This method prints a stack trace for this Throwable object on the error output stream that is the value of the field System.err. The first line of output contains the result of the
     * method for this object.
     * The format of the backtrace information depends on the implementation.
     */
    public void printStackTrace(){
        return; //TODO codavaj!!
    }

    /**
     * Returns a short description of this Throwable object. If this Throwable object was
     * with an error message string, then the result is the concatenation of three strings: The name of the actual class of this object ": " (a colon and a space) The result of the
     * method for this object If this Throwable object was
     * with no error message string, then the name of the actual class of this object is returned.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    public final void addSuppressed(Throwable exception) {
    }
}
