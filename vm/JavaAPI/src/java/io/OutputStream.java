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
 * This abstract class is the superclass of all classes representing an output stream of bytes. An output stream accepts output bytes and sends them to some sink.
 * Applications that need to define a subclass of OutputStream must always provide at least a method that writes one byte of output.
 * Since: JDK1.0, CLDC 1.0 See Also:ByteArrayOutputStream, DataOutputStream, InputStream, write(int)
 */
public abstract class OutputStream{
    public OutputStream(){
    }

    /**
     * Closes this output stream and releases any system resources associated with this stream. The general contract of close is that it closes the output stream. A closed stream cannot perform output operations and cannot be reopened.
     * The close method of OutputStream does nothing.
     */
    public void close() throws java.io.IOException{
        flush();
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be written out. The general contract of flush is that calling it is an indication that, if any bytes previously written have been buffered by the implementation of the output stream, such bytes should immediately be written to their intended destination.
     * The flush method of OutputStream does nothing.
     */
    public void flush() throws java.io.IOException{
    }

    /**
     * Writes b.length bytes from the specified byte array to this output stream. The general contract for write(b) is that it should have exactly the same effect as the call write(b, 0, b.length).
     */
    public void write(byte[] b) throws java.io.IOException{
        write(b, 0, b.length);
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to this output stream. The general contract for write(b, off, len) is that some of the bytes in the array b are written to the output stream in order; element b[off] is the first byte written and b[off+len-1] is the last byte written by this operation.
     * The write method of OutputStream calls the write method of one argument on each of the bytes to be written out. Subclasses are encouraged to override this method and provide a more efficient implementation.
     * If b is null, a NullPointerException is thrown.
     * If off is negative, or len is negative, or off+len is greater than the length of the array b, then an IndexOutOfBoundsException is thrown.
     */
    public void write(byte[] b, int off, int len) throws java.io.IOException{
        for(int iter = off ; iter < len + off ; iter++) {
            write(b[iter]);
        }
    }

    /**
     * Writes the specified byte to this output stream. The general contract for write is that one byte is written to the output stream. The byte to be written is the eight low-order bits of the argument b. The 24 high-order bits of b are ignored.
     * Subclasses of OutputStream must provide an implementation for this method.
     */
    public abstract void write(int b) throws java.io.IOException;

    public boolean checkError() {
        return false;
    }
}
