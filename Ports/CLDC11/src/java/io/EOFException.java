package java.io;
/**
 * Signals that an end of file or end of stream has been reached unexpectedly during input.
 * This exception is mainly used by data input streams, which generally expect a binary file in a specific format, and for which an end of stream is an unusual condition. Most other input streams return a special value on end of stream.
 * Note that some input operations react to end-of-file by returning a distinguished value (such as -1) rather than by throwing an exception.
 * Since: JDK1.0, CLDC 1.0 See Also:DataInputStream, IOException
 */
public class EOFException extends java.io.IOException{
    /**
     * Constructs an EOFException with null as its error detail message.
     */
    public EOFException(){
         //TODO codavaj!!
    }

    /**
     * Constructs an EOFException with the specified detail message. The string s may later be retrieved by the
     * method of class java.lang.Throwable.
     * s - the detail message.
     */
    public EOFException(java.lang.String s){
         //TODO codavaj!!
    }

}
