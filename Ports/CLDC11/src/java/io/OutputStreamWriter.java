package java.io;
/**
 * An OutputStreamWriter is a bridge from character streams to byte streams: Characters written to it are translated into bytes. The encoding that it uses may be specified by name, or the platform's default encoding may be accepted.
 * Each invocation of a write() method causes the encoding converter to be invoked on the given character(s). The resulting bytes are accumulated in a buffer before being written to the underlying output stream. The size of this buffer may be specified, but by default it is large enough for most purposes. Note that the characters passed to the write() methods are not buffered.
 * Since: CLDC 1.0 See Also:Writer, UnsupportedEncodingException
 */
public class OutputStreamWriter extends java.io.Writer{
    /**
     * Create an OutputStreamWriter that uses the default character encoding.
     * os - An OutputStream
     */
    public OutputStreamWriter(java.io.OutputStream os){
         //TODO codavaj!!
    }

    /**
     * Create an OutputStreamWriter that uses the named character encoding.
     * os - An OutputStreamenc - The name of a supported
     * - If the named encoding is not supported
     */
    public OutputStreamWriter(java.io.OutputStream os, java.lang.String enc) throws java.io.UnsupportedEncodingException{
         //TODO codavaj!!
    }

    /**
     * Close the stream.
     */
    public void close() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Flush the stream.
     */
    public void flush() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Write a portion of an array of characters.
     */
    public void write(char[] cbuf, int off, int len) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Write a single character.
     */
    public void write(int c) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Write a portion of a string.
     */
    public void write(java.lang.String str, int off, int len) throws java.io.IOException{
        return; //TODO codavaj!!
    }

}
