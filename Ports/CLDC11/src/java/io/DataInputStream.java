/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.io;
/**
 * A data input stream lets an application read primitive Java data types from an underlying input stream in a machine-independent way. An application uses a data output stream to write data that can later be read by a data input stream.
 * Since: JDK1.0, CLDC 1.0 See Also:DataOutputStream
 */
public class DataInputStream extends java.io.InputStream implements java.io.DataInput{
    /**
     * The input stream.
     */
    protected java.io.InputStream in;

    /**
     * Creates a DataInputStream and saves its argument, the input stream in, for later use.
     * in - the input stream.
     */
    public DataInputStream(java.io.InputStream in){
         //TODO codavaj!!
    }

    /**
     * Returns the number of bytes that can be read from this input stream without blocking.
     * This method simply performs in.available() and returns the result.
     */
    public int available() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream. This method simply performs in.close().
     */
    public void close() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Marks the current position in this input stream. A subsequent call to the reset method repositions this stream at the last marked position so that subsequent reads re-read the same bytes.
     * The readlimit argument tells this input stream to allow that many bytes to be read before the mark position gets invalidated.
     * This method simply performs in.mark(readlimit).
     */
    public void mark(int readlimit){
        return; //TODO codavaj!!
    }

    /**
     * Tests if this input stream supports the mark and reset methods. This method simply performs in.markSupported().
     */
    public boolean markSupported(){
        return false; //TODO codavaj!!
    }

    /**
     * Reads the next byte of data from this input stream. The value byte is returned as an int in the range 0 to 255. If no byte is available because the end of the stream has been reached, the value -1 is returned. This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
     * This method simply performs in.read() and returns the result.
     */
    public int read() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * See the general contract of the read method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int read(byte[] b) throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * Reads up to len bytes of data from this input stream into an array of bytes. This method blocks until some input is available.
     * This method simply performs in.read(b, off, len) and returns the result.
     */
    public final int read(byte[] b, int off, int len) throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * See the general contract of the readBoolean method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final boolean readBoolean() throws java.io.IOException{
        return false; //TODO codavaj!!
    }

    /**
     * See the general contract of the readByte method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final byte readByte() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * See the general contract of the readChar method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final char readChar() throws java.io.IOException{
        return ' '; //TODO codavaj!!
    }

    /**
     * See the general contract of the readDouble method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final double readDouble() throws java.io.IOException{
        return 0.0d; //TODO codavaj!!
    }

    /**
     * See the general contract of the readFloat method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final float readFloat() throws java.io.IOException{
        return 0.0f; //TODO codavaj!!
    }

    /**
     * See the general contract of the readFully method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final void readFully(byte[] b) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * See the general contract of the readFully method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final void readFully(byte[] b, int off, int len) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * See the general contract of the readInt method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int readInt() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * See the general contract of the readLong method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final long readLong() throws java.io.IOException{
        return 0l; //TODO codavaj!!
    }

    /**
     * See the general contract of the readShort method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final short readShort() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * See the general contract of the readUnsignedByte method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int readUnsignedByte() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * See the general contract of the readUnsignedShort method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int readUnsignedShort() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * See the general contract of the readUTF method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final java.lang.String readUTF() throws java.io.IOException{
        return null; //TODO codavaj!!
    }

    /**
     * Reads from the stream in a representation of a Unicode character string encoded in Java modified UTF-8 format; this string of characters is then returned as a String. The details of the modified UTF-8 representation are exactly the same as for the readUTF method of DataInput.
     */
    public static final java.lang.String readUTF(java.io.DataInput in) throws java.io.IOException{
        return null; //TODO codavaj!!
    }

    /**
     * Repositions this stream to the position at the time the mark method was last called on this input stream.
     * This method simply performs in.reset().
     * Stream marks are intended to be used in situations where you need to read ahead a little to see what's in the stream. Often this is most easily done by invoking some general parser. If the stream is of the type handled by the parse, it just chugs along happily. If the stream is not of that type, the parser should toss an exception when it fails. If this happens within readlimit bytes, it allows the outer code to reset the stream and try another parser.
     */
    public void reset() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Skips over and discards n bytes of data from the input stream. The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0. The actual number of bytes skipped is returned.
     * This method simply performs in.skip(n).
     */
    public long skip(long n) throws java.io.IOException{
        return 0l; //TODO codavaj!!
    }

    /**
     * See the general contract of the skipBytes method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int skipBytes(int n) throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

}
