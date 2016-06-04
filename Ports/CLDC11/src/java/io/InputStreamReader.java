package java.io;
/**
 * An InputStreamReader is a bridge from byte streams to character streams: It reads bytes and translates them into characters. The encoding that it uses may be specified by name, or the platform's default encoding may be accepted.
 * Each invocation of one of an InputStreamReader's read() methods may cause one or more bytes to be read from the underlying byte input stream. To enable the efficient conversion of bytes to characters, more bytes may be read ahead from the underlying stream than are necessary to satisfy the current read operation.
 * Since: CLDC 1.0 See Also:Reader, UnsupportedEncodingException
 */
public class InputStreamReader extends java.io.Reader{
    /**
     * Create an InputStreamReader that uses the default character encoding.
     * is - An InputStream
     */
    public InputStreamReader(java.io.InputStream is){
         //TODO codavaj!!
    }

    /**
     * Create an InputStreamReader that uses the named character encoding.
     * is - An InputStreamenc - The name of a supported character encoding
     * - If the named encoding is not supported
     */
    public InputStreamReader(java.io.InputStream is, java.lang.String enc) throws java.io.UnsupportedEncodingException{
         //TODO codavaj!!
    }

    /**
     * Close the stream. Closing a previously closed stream has no effect.
     */
    public void close() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Mark the present position in the stream.
     */
    public void mark(int readAheadLimit) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Tell whether this stream supports the mark() operation.
     */
    public boolean markSupported(){
        return false; //TODO codavaj!!
    }

    /**
     * Read a single character.
     */
    public int read() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * Read characters into a portion of an array.
     */
    public int read(char[] cbuf, int off, int len) throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * Tell whether this stream is ready to be read.
     */
    public boolean ready() throws java.io.IOException{
        return false; //TODO codavaj!!
    }

    /**
     * Reset the stream.
     */
    public void reset() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Skip characters.
     */
    public long skip(long n) throws java.io.IOException{
        return 0l; //TODO codavaj!!
    }

}
