package java.io;
/**
 * A ByteArrayInputStream contains an internal buffer that contains bytes that may be read from the stream. An internal counter keeps track of the next byte to be supplied by the read method.
 * Since: JDK1.0, CLDC 1.0
 */
public class ByteArrayInputStream extends java.io.InputStream{
    /**
     * An array of bytes that was provided by the creator of the stream. Elements buf[0] through buf[count-1] are the only bytes that can ever be read from the stream; element buf[pos] is the next byte to be read.
     */
    protected byte[] buf;

    /**
     * The index one greater than the last valid character in the input stream buffer. This value should always be nonnegative and not larger than the length of buf. It is one greater than the position of the last byte within buf that can ever be read from the input stream buffer.
     */
    protected int count;

    /**
     * The currently marked position in the stream. ByteArrayInputStream objects are marked at position zero by default when constructed. They may be marked at another position within the buffer by the mark() method. The current buffer position is set to this point by the reset() method.
     * Since: JDK1.1
     */
    protected int mark;

    /**
     * The index of the next character to read from the input stream buffer. This value should always be nonnegative and not larger than the value of count. The next byte to be read from the input stream buffer will be buf[pos].
     */
    protected int pos;

    /**
     * Creates a ByteArrayInputStream so that it uses buf as its buffer array. The buffer array is not copied. The initial value of pos is 0 and the initial value of count is the length of buf.
     * buf - the input buffer.
     */
    public ByteArrayInputStream(byte[] buf){
         //TODO codavaj!!
    }

    /**
     * Creates ByteArrayInputStream that uses buf as its buffer array. The initial value of pos is offset and the initial value of count is offset+length. The buffer array is not copied.
     * Note that if bytes are simply read from the resulting input stream, elements buf[pos] through buf[pos+len-1] will be read; however, if a reset operation is performed, then bytes buf[0] through buf[pos-1] will then become available for input.
     * buf - the input buffer.offset - the offset in the buffer of the first byte to read.length - the maximum number of bytes to read from the buffer.
     */
    public ByteArrayInputStream(byte[] buf, int offset, int length){
         //TODO codavaj!!
    }

    /**
     * Returns the number of bytes that can be read from this input stream without blocking. The value returned is count
     * - pos, which is the number of bytes remaining to be read from the input buffer.
     */
    public int available(){
        return 0; //TODO codavaj!!
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     */
    public void close() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Set the current marked position in the stream. ByteArrayInputStream objects are marked at position zero by default when constructed. They may be marked at another position within the buffer by this method.
     */
    public void mark(int readAheadLimit){
        return; //TODO codavaj!!
    }

    /**
     * Tests if ByteArrayInputStream supports mark/reset.
     */
    public boolean markSupported(){
        return false; //TODO codavaj!!
    }

    /**
     * Reads the next byte of data from this input stream. The value byte is returned as an int in the range 0 to 255. If no byte is available because the end of the stream has been reached, the value -1 is returned.
     * This read method cannot block.
     */
    public int read(){
        return 0; //TODO codavaj!!
    }

    /**
     * Reads up to len bytes of data into an array of bytes from this input stream. If pos equals count, then -1 is returned to indicate end of file. Otherwise, the number k of bytes read is equal to the smaller of len and count-pos. If k is positive, then bytes buf[pos] through buf[pos+k-1] are copied into b[off] through b[off+k-1] in the manner performed by System.arraycopy. The value k is added into pos and k is returned.
     * This read method cannot block.
     */
    public int read(byte[] b, int off, int len){
        return 0; //TODO codavaj!!
    }

    /**
     * Resets the buffer to the marked position. The marked position is the beginning unless another position was marked. The value of pos is set to 0.
     */
    public void reset(){
        return; //TODO codavaj!!
    }

    /**
     * Skips n bytes of input from this input stream. Fewer bytes might be skipped if the end of the input stream is reached. The actual number k of bytes to be skipped is equal to the smaller of n and count-pos. The value k is added into pos and k is returned.
     */
    public long skip(long n){
        return 0l; //TODO codavaj!!
    }

}
