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
 * An InputStreamReader is a bridge from byte streams to character streams: It reads bytes and translates them into characters. The encoding that it uses may be specified by name, or the platform's default encoding may be accepted.
 * Each invocation of one of an InputStreamReader's read() methods may cause one or more bytes to be read from the underlying byte input stream. To enable the efficient conversion of bytes to characters, more bytes may be read ahead from the underlying stream than are necessary to satisfy the current read operation.
 * Since: CLDC 1.0 See Also:Reader, UnsupportedEncodingException
 */
public class InputStreamReader extends java.io.Reader{
    private InputStream internal; 
    private java.lang.String enc;
    private byte[] bbuffer = new byte[8192];
    private char[] cbuffer;
    private int cbufferOff;
    
    /**
     * Create an InputStreamReader that uses the default character encoding.
     * is - An InputStream
     */
    public InputStreamReader(java.io.InputStream is) {
         internal = is;
         this.enc = "UTF-8";
    }

    /**
     * Create an InputStreamReader that uses the named character encoding.
     * is - An InputStreamenc - The name of a supported character encoding
     * - If the named encoding is not supported
     */
    public InputStreamReader(java.io.InputStream is, java.lang.String enc) throws java.io.UnsupportedEncodingException{
         internal = is;
         this.enc = enc.intern();
    }

    private static native char[] bytesToChars(byte[] b, int off, int len, String encoding); 

    /**
     * Close the stream. Closing a previously closed stream has no effect.
     */
    public void close() throws java.io.IOException{
        internal.close();
        cbufferOff = -1;
    }

    /**
     * Mark the present position in the stream.
     */
    public void mark(int readAheadLimit) throws java.io.IOException{
    }

    /**
     * Tell whether this stream supports the mark() operation.
     */
    public boolean markSupported(){
        return false; 
    }

    /**
     * Read a single character.
     */
    public int read() throws java.io.IOException{
        char[] c = new char[1];
        int count = read(c, 0, 1);
        if(count < 0) {
            return -1;
        }
        return c[0]; 
    }

    /**
     * Read characters into a portion of an array.
     */
    public int read(char[] cbuf, int off, int len) throws java.io.IOException{
        if(cbuffer == null || cbufferOff > cbuffer.length - 1) {
            int size = internal.read(bbuffer);
            if(size < 0) {
                return -1;
            }
            cbuffer = bytesToChars(bbuffer, 0, size, enc);
            cbufferOff = 0;
        }
        int count = 0;
        while(cbufferOff < cbuffer.length && len > count) {
            cbuf[off] = cbuffer[cbufferOff];
            count++;
            off++;
            cbufferOff++;
        }
        if(count == len) {
            return count;
        }
        if(count < len && cbufferOff == cbuffer.length) {
            cbuffer = null;
            cbufferOff = 0;
            int val = read(cbuf, off, len - count);
            if(val == -1) {
                return count;
            }
            return count + val;
        }
        return count; 
    }

    /**
     * Tell whether this stream is ready to be read.
     */
    public boolean ready() throws java.io.IOException{
        return true;
    }

    /**
     * Reset the stream.
     */
    public void reset() throws java.io.IOException{
    }

    /**
     * Skip characters.
     */
    public long skip(long n) throws java.io.IOException{
        return internal.skip(n);
    }

}
