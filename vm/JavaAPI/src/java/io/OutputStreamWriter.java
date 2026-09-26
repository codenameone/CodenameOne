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
 * An OutputStreamWriter is a bridge from character streams to byte streams: Characters written to it are translated into bytes. The encoding that it uses may be specified by name, or the platform's default encoding may be accepted.
 * Each invocation of a write() method causes the encoding converter to be invoked on the given character(s). The resulting bytes are accumulated in a buffer before being written to the underlying output stream. The size of this buffer may be specified, but by default it is large enough for most purposes. Note that the characters passed to the write() methods are not buffered.
 * Since: CLDC 1.0 See Also:Writer, UnsupportedEncodingException
 */
public class OutputStreamWriter extends java.io.Writer {
    private OutputStream os;
    private String enc;

    /* This class's own javadoc, above, promises that "the resulting bytes are
     * accumulated in a buffer before being written to the underlying output stream".
     * It did not do that: every write encoded a fresh byte[] and handed it straight
     * to the stream. On ParparVM each of those is a crossing into native code with a
     * GC safepoint transition on either side, so a caller writing a line at a time
     * paid one per line, plus a byte[] the collector then had to trace and sweep.
     *
     * Buffering here is what the contract already said happens, and it is where it
     * belongs: doing it in FileOutputStream would not help a writer wrapped around
     * any other stream, and asking every caller to add a BufferedWriter only moves
     * the allocation. */
    private final byte[] buf = new byte[8192];
    private int count;

    /* UTF-8 is encoded straight into the buffer rather than through getBytes, which
     * allocates a byte[] per call. It is the default encoding and by far the common
     * case; anything else keeps the getBytes path. */
    private final boolean utf8;

    /**
     * Create an OutputStreamWriter that uses the default character encoding.
     * os - An OutputStream
     */
    public OutputStreamWriter(java.io.OutputStream os){
         this.os = os;
         enc = "UTF-8";
         utf8 = true;
    }

    /**
     * Create an OutputStreamWriter that uses the named character encoding.
     * os - An OutputStreamenc - The name of a supported
     * - If the named encoding is not supported
     */
    public OutputStreamWriter(java.io.OutputStream os, java.lang.String enc) throws java.io.UnsupportedEncodingException{
         this.os = os;
         this.enc = enc;
         utf8 = isUtf8Name(enc);
    }

    /* The encoding name is ASCII by specification, so it is folded by hand.
     * String.toLowerCase is locale sensitive and would fold the I of "UTF-8" to a
     * dotless i on a Turkish device, quietly sending every write down the slow path
     * -- or worse, down a path that disagrees with the one the same app took
     * elsewhere. */
    private static boolean isUtf8Name(String e) {
        if(e == null) {
            return false;
        }
        return e.equalsIgnoreCase("UTF-8") || e.equalsIgnoreCase("UTF8")
                || e.equalsIgnoreCase("utf-8");
    }

    // Buffering hides the underlying stream from every write, so it can no longer be
    // what reports a closed writer: without this a write after close() only appended to
    // buf, returned normally, and the bytes were silently lost.
    private boolean closed;

    private void ensureOpen() throws java.io.IOException {
        if(closed) {
            throw new java.io.IOException("Stream closed");
        }
    }

    private void flushBuffer() throws java.io.IOException {
        if(count > 0) {
            os.write(buf, 0, count);
            count = 0;
        }
    }

    /**
     * Close the stream.
     */
    public void close() throws java.io.IOException{
        if(closed) {
            return;   // closing a closed writer has no effect, as in the JDK
        }
        try {
            finishPending();
            flushBuffer();
        } finally {
            closed = true;
            os.close();
        }
    }

    /**
     * Flush the stream.
     */
    public void flush() throws java.io.IOException{
        ensureOpen();
        flushBuffer();
        os.flush();
    }

    /**
     * Write a portion of an array of characters.
     */
    public void write(char[] cbuf, int off, int len) throws java.io.IOException{
        // The bounds check has to be explicit now. It used to happen by accident,
        // inside the String the old body built; the encode loop below would simply
        // run zero times for a negative length and return as though it had written
        // something, where Writer.write says it throws.
        ensureOpen();
        if(off < 0 || len < 0 || off + len > cbuf.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        if(utf8) {
            int end = off + len;
            for(int i = off ; i < end ; i++) {
                encodeChar(cbuf[i]);
            }
            return;
        }
        write(new String(cbuf, off, len));
    }

    /**
     * Write a single character.
     */
    public void write(int c) throws java.io.IOException{
        ensureOpen();
        if(utf8) {
            encodeChar((char)c);
            return;
        }
        write(new String(new char[] {(char)c}));
    }

    /**
     * Write a portion of a string.
     */
    public void write(java.lang.String str, int off, int len) throws java.io.IOException{
        // Same reasoning as the char[] overload: substring used to raise this, and
        // the encode loop does not.
        ensureOpen();
        if(off < 0 || len < 0 || off + len > str.length() || off + len < 0) {
            throw new StringIndexOutOfBoundsException();
        }
        if(utf8) {
            int end = off + len;
            for(int i = off ; i < end ; i++) {
                encodeChar(str.charAt(i));
            }
            return;
        }
        if(off > 0 || len != str.length()) {
            // substring takes an END index, not a length. With off > 0 this used to
            // ask for [off, len), which is the wrong range and is shorter than the
            // caller asked for -- or throws when len < off.
            str = str.substring(off, off + len);
        }
        byte[] b = str.getBytes(enc);
        writeBytes(b, 0, b.length);
    }

    private void writeBytes(byte[] b, int off, int len) throws java.io.IOException {
        if(len >= buf.length) {
            // Larger than the buffer: flushing first keeps the bytes in order, and
            // passing it straight through avoids copying it twice.
            flushBuffer();
            os.write(b, off, len);
            return;
        }
        if(count + len > buf.length) {
            flushBuffer();
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /* A high surrogate at the end of one write is joined with a low surrogate that
     * arrives in the NEXT write: the JDK's writer carries that state across calls, so
     * writing the two halves of an astral character separately still produces the one
     * four byte sequence rather than two replacements. Holding the half here is also
     * what lets the loops above iterate a char at a time with no lookahead, which is
     * why a pair inside a single string needs no special case.
     *
     * An unpaired surrogate becomes '?', which is what String.getBytes("UTF-8") does
     * with malformed input; emitting the three byte CESU-8 form instead would
     * disagree with every other encoder. A pair that never completes is resolved the
     * same way when the stream is closed. */
    private char pendingHigh;

    private void encodeChar(char c) throws java.io.IOException {
        if(count + 4 > buf.length) {
            flushBuffer();
        }
        if(pendingHigh != 0) {
            char hi = pendingHigh;
            pendingHigh = 0;
            if(c >= 0xdc00 && c <= 0xdfff) {
                int cp = 0x10000 + ((hi - 0xd800) << 10) + (c - 0xdc00);
                buf[count++] = (byte)(0xf0 | (cp >> 18));
                buf[count++] = (byte)(0x80 | ((cp >> 12) & 0x3f));
                buf[count++] = (byte)(0x80 | ((cp >> 6) & 0x3f));
                buf[count++] = (byte)(0x80 | (cp & 0x3f));
                return;
            }
            buf[count++] = (byte)'?';
            if(count + 4 > buf.length) {
                flushBuffer();
            }
        }
        if(c < 0x80) {
            buf[count++] = (byte)c;
            return;
        }
        if(c < 0x800) {
            buf[count++] = (byte)(0xc0 | (c >> 6));
            buf[count++] = (byte)(0x80 | (c & 0x3f));
            return;
        }
        if(c >= 0xd800 && c <= 0xdbff) {
            pendingHigh = c;
            return;
        }
        if(c >= 0xdc00 && c <= 0xdfff) {
            buf[count++] = (byte)'?';
            return;
        }
        buf[count++] = (byte)(0xe0 | (c >> 12));
        buf[count++] = (byte)(0x80 | ((c >> 6) & 0x3f));
        buf[count++] = (byte)(0x80 | (c & 0x3f));
    }

    /* Only the end of the stream can decide that a held high surrogate will never be
     * completed. flush does not, because more characters may still follow it. */
    private void finishPending() throws java.io.IOException {
        if(pendingHigh != 0) {
            pendingHigh = 0;
            if(count + 1 > buf.length) {
                flushBuffer();
            }
            buf[count++] = (byte)'?';
        }
    }

    public void write(CharSequence csq) throws IOException {
        write(csq.toString());
    }
}
