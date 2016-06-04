package java.lang;
/**
 * Thrown when an application attempts to use null in a case where an object is required. These include: Calling the instance method of a null object. Accessing or modifying the field of a null object. Taking the length of null as if it were an array. Accessing or modifying the slots of null as if it were an array. Throwing null as if it were a Throwable value.
 * Applications should throw instances of this class to indicate other illegal uses of the null object.
 * Since: JDK1.0, CLDC 1.0
 */
public class NullPointerException extends java.lang.RuntimeException{
    /**
     * Constructs a NullPointerException with no detail message.
     */
    public NullPointerException(){
         //TODO codavaj!!
    }

    /**
     * Constructs a NullPointerException with the specified detail message.
     * s - the detail message.
     */
    public NullPointerException(java.lang.String s){
         //TODO codavaj!!
    }

}
