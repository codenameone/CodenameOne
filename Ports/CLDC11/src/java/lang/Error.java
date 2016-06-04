package java.lang;
/**
 * An Error is a subclass of Throwable that indicates serious problems that a reasonable application should not try to catch. Most such errors are abnormal conditions.
 * A method is not required to declare in its throws clause any subclasses of Error that might be thrown during the execution of the method but not caught, since these errors are abnormal conditions that should never occur.
 * Since: JDK1.0, CLDC 1.0
 */
public class Error extends java.lang.Throwable{
    /**
     * Constructs an Error with no specified detail message.
     */
    public Error(){
         //TODO codavaj!!
    }

    /**
     * Constructs an Error with the specified detail message.
     * s - the detail message.
     */
    public Error(java.lang.String s){
         //TODO codavaj!!
    }

}
