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
 * Abstract class for reading character streams. The only methods that a subclass must implement are read(char[], int, int) and close(). Most subclasses, however, will override some of the methods defined here in order to provide higher efficiency, additional functionality, or both.
 * Since: JDK1.1, CLDC 1.0 See Also:InputStreamReader, Writer
 */
public abstract class Reader  implements AutoCloseable {
    /**
     * The object used to synchronize operations on this stream. For efficiency, a character-stream object may use an object other than itself to protect critical sections. A subclass should therefore use the object in this field rather than this or a synchronized method.
     */
    protected java.lang.Object lock;

    /**
     * Create a new character-stream reader whose critical sections will synchronize on the reader itself.
     */
    protected Reader(){
         //TODO codavaj!!
    }

    /**
     * Create a new character-stream reader whose critical sections will synchronize on the given object.
     * lock - The Object to synchronize on.
     */
    protected Reader(java.lang.Object lock){
         //TODO codavaj!!
    }

    /**
     * Close the stream. Once a stream has been closed, further read(), ready(), mark(), or reset() invocations will throw an IOException. Closing a previously-closed stream, however, has no effect.
     */
    public abstract void close() throws java.io.IOException;

    /**
     * Mark the present position in the stream. Subsequent calls to reset() will attempt to reposition the stream to this point. Not all character-input streams support the mark() operation.
     */
    public void mark(int readAheadLimit) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Tell whether this stream supports the mark() operation. The default implementation always returns false. Subclasses should override this method.
     */
    public boolean markSupported(){
        return false; //TODO codavaj!!
    }

    /**
     * Read a single character. This method will block until a character is available, an I/O error occurs, or the end of the stream is reached.
     * Subclasses that intend to support efficient single-character input should override this method.
     */
    public int read() throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * Read characters into an array. This method will block until some input is available, an I/O error occurs, or the end of the stream is reached.
     */
    public int read(char[] cbuf) throws java.io.IOException{
        return 0; //TODO codavaj!!
    }

    /**
     * Read characters into a portion of an array. This method will block until some input is available, an I/O error occurs, or the end of the stream is reached.
     */
    public abstract int read(char[] cbuf, int off, int len) throws java.io.IOException;

    /**
     * Tell whether this stream is ready to be read.
     */
    public boolean ready() throws java.io.IOException{
        return false; //TODO codavaj!!
    }

    /**
     * Reset the stream. If the stream has been marked, then attempt to reposition it at the mark. If the stream has not been marked, then attempt to reset it in some way appropriate to the particular stream, for example by repositioning it to its starting point. Not all character-input streams support the reset() operation, and some support reset() without supporting mark().
     */
    public void reset() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Skip characters. This method will block until some characters are available, an I/O error occurs, or the end of the stream is reached.
     */
    public long skip(long n) throws java.io.IOException{
        return 0l; //TODO codavaj!!
    }

}
