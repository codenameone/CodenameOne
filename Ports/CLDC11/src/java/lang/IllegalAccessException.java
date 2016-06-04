package java.lang;
/**
 * Thrown when an application tries to load in a class, but the currently executing method does not have access to the definition of the specified class, because the class is not public and in another package.
 * An instance of this class can also be thrown when an application tries to create an instance of a class using the newInstance method in class Class, but the current method does not have access to the appropriate zero-argument constructor.
 * Since: JDK1.0, CLDC 1.0 See Also:Class.forName(java.lang.String), Class.newInstance()
 */
public class IllegalAccessException extends java.lang.Exception{
    /**
     * Constructs an IllegalAccessException without a detail message.
     */
    public IllegalAccessException(){
         //TODO codavaj!!
    }

    /**
     * Constructs an IllegalAccessException with a detail message.
     * s - the detail message.
     */
    public IllegalAccessException(java.lang.String s){
         //TODO codavaj!!
    }

}
