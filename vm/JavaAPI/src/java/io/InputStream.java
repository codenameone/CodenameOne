/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package java.io;
/**
 * This abstract class is the superclass of all classes representing an input stream of bytes.
 * Applications that need to define a subclass of InputStream must always provide a method that returns the next byte of input.
 * Since: JDK1.0, CLDC 1.0 See Also:ByteArrayInputStream, DataInputStream, read(), OutputStream
 */
public abstract class InputStream{
    public InputStream(){
    }

    /**
     * Returns the number of bytes that can be read (or skipped over) from this input stream without blocking by the next caller of a method for this input stream. The next caller might be the same thread or another thread.
     * The available method for class InputStream always returns 0.
     * This method should be overridden by subclasses.
     */
    public int available() throws java.io.IOException{
        return 0; 
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     * The close method of InputStream does nothing.
     */
    public void close() throws java.io.IOException{
    }

    /**
     * Marks the current position in this input stream. A subsequent call to the reset method repositions this stream at the last marked position so that subsequent reads re-read the same bytes.
     * The readlimit arguments tells this input stream to allow that many bytes to be read before the mark position gets invalidated.
     * The general contract of mark is that, if the method markSupported returns true, the stream somehow remembers all the bytes read after the call to mark and stands ready to supply those same bytes again if and whenever the method reset is called. However, the stream is not required to remember any data at all if more than readlimit bytes are read from the stream before reset is called.
     * The mark method of InputStream does nothing.
     */
    public void mark(int readlimit){
    }

    /**
     * Tests if this input stream supports the mark and reset methods. The markSupported method of InputStream returns false.
     */
    public boolean markSupported(){
        return false; 
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255. If no byte is available because the end of the stream has been reached, the value -1 is returned. This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
     * A subclass must provide an implementation of this method.
     */
    public abstract int read() throws java.io.IOException;

    /**
     * Reads some number of bytes from the input stream and stores them into the buffer array b. The number of bytes actually read is returned as an integer. This method blocks until input data is available, end of file is detected, or an exception is thrown.
     * If b is null, a NullPointerException is thrown. If the length of b is zero, then no bytes are read and 0 is returned; otherwise, there is an attempt to read at least one byte. If no byte is available because the stream is at end of file, the value -1 is returned; otherwise, at least one byte is read and stored into b.
     * The first byte read is stored into element b[0], the next one into b[1], and so on. The number of bytes read is, at most, equal to the length of b. Let k be the number of bytes actually read; these bytes will be stored in elements b[0] through b[k-1], leaving elements b[k] through b[b.length-1] unaffected.
     * If the first byte cannot be read for any reason other than end of file, then an IOException is thrown. In particular, an IOException is thrown if the input stream has been closed.
     * The read(b) method for class InputStream has the same effect as: read(b, 0, b.length)
     */
    public int read(byte[] b) throws java.io.IOException{
        return read(b, 0, b.length);
    }

    /**
     * Reads up to {@code byteCount} bytes from this stream and stores them in
     * the byte array {@code buffer} starting at {@code byteOffset}.
     * Returns the number of bytes actually read or -1 if the end of the stream
     * has been reached.
     *
     * @throws IndexOutOfBoundsException
     *   if {@code byteOffset < 0 || byteCount < 0 || byteOffset + byteCount > buffer.length}.
     * @throws IOException
     *             if the stream is closed or another IOException occurs.
     */
    public int read(byte[] buffer, int byteOffset, int byteCount) throws java.io.IOException{
        for (int i = 0; i < byteCount; ++i) {
            int c;
            try {
                if ((c = read()) == -1) {
                    return i == 0 ? -1 : i;
                }
            } catch (IOException e) {
                if (i != 0) {
                    return i;
                }
                throw e;
            }
            buffer[byteOffset + i] = (byte) c;
        }
        return byteCount;
    }

    /**
     * Repositions this stream to the position at the time the mark method was last called on this input stream.
     * The general contract of reset is:
     * If the method markSupported returns true, then: If the method mark has not been called since the stream was created, or the number of bytes read from the stream since mark was last called is larger than the argument to mark at that last call, then an IOException might be thrown. If such an IOException is not thrown, then the stream is reset to a state such that all the bytes read since the most recent call to mark (or since the start of the file, if mark has not been called) will be resupplied to subsequent callers of the read method, followed by any bytes that otherwise would have been the next input data as of the time of the call to reset. If the method markSupported returns false, then: The call to reset may throw an IOException. If an IOException is not thrown, then the stream is reset to a fixed state that depends on the particular type of the input stream and how it was created. The bytes that will be supplied to subsequent callers of the read method depend on the particular type of the input stream.
     * The method reset for class InputStream does nothing and always throws an IOException.
     */
    public void reset() throws java.io.IOException{
        throw new IOException();
    }

    /**
     * Skips over and discards n bytes of data from this input stream. The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0. This may result from any of a number of conditions; reaching end of file before n bytes have been skipped is only one possibility. The actual number of bytes skipped is returned. If n is negative, no bytes are skipped.
     * The skip method of InputStream creates a byte array and then repeatedly reads into it until n bytes have been read or the end of the stream has been reached. Subclasses are encouraged to provide a more efficient implementation of this method.
     */
    public long skip(long n) throws java.io.IOException{
        return read(new byte[(int)n]); 
    }

}
