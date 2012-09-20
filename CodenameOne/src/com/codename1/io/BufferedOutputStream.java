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
package com.codename1.io;

import java.io.*;

/**
 * Based on the buffered output stream from the JDK with some minor tweaks to allow
 * external classes to monitor stream status and progress.
 */
public class BufferedOutputStream extends OutputStream {
    private Object connection;
    private boolean closed;
    private static int streamCount = 0;

    private static int defaultBufferSize = 8192;
    private OutputStream out;

    /**
     * The internal buffer where data is stored. 
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer. This value is always 
     * in the range <tt>0</tt> through <tt>buf.length</tt>; elements 
     * <tt>buf[0]</tt> through <tt>buf[count-1]</tt> contain valid 
     * byte data.
     */
    protected int count;
    private long lastActivityTime;
    private int totalBytesWritten;
    private IOProgressListener progressListener;
    private String name;

    /**
     * Indicates the name of the stream for debugging purposes
     *
     * @return the name of the stream
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a new buffered output stream to write data to the
     * specified underlying output stream.
     *
     * @param   out   the underlying output stream.
     */
    public BufferedOutputStream(OutputStream out) {
        this(out, defaultBufferSize);
    }

    /**
     * Creates a new buffered output stream to write data to the
     * specified underlying output stream.
     *
     * @param   out   the underlying output stream.
     * @param  name the name of the stream used for debugging/logging purposes
     */
    public BufferedOutputStream(OutputStream out, String name) {
        this(out, defaultBufferSize, name);
    }

    /**
     * Creates a new buffered output stream to write data to the 
     * specified underlying output stream with the specified buffer 
     * size. 
     *
     * @param   out    the underlying output stream.
     * @param   size   the buffer size.
     * @exception IllegalArgumentException if size &lt;= 0.
     */
    public BufferedOutputStream(OutputStream out, int size) {
        this(out, size, "unnamed");
    }

    /**
     * Creates a new buffered output stream to write data to the
     * specified underlying output stream with the specified buffer
     * size.
     *
     * @param   out    the underlying output stream.
     * @param   size   the buffer size.
     * @param  name the name of the stream used for debugging/logging purposes
     * @exception IllegalArgumentException if size &lt;= 0.
     */
    public BufferedOutputStream(OutputStream out, int size, String name) {
        this.out = out;
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
        streamCount++;
        this.name = name;
        Util.getImplementation().logStreamCreate(name, false, streamCount);
    }

    /** Flush the internal buffer */
    public void flushBuffer() throws IOException {
        if(closed) {
            return;
        }
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    /**
     * Writes the specified byte to this buffered output stream. 
     *
     * @param      b   the byte to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flushBuffer();
        }
        totalBytesWritten++;
        fireProgress();
        lastActivityTime = System.currentTimeMillis();
        buf[count++] = (byte) b;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this buffered output stream.
     *
     * <p> Ordinarily this method stores bytes from the given array into this
     * stream's buffer, flushing the buffer to the underlying output stream as
     * needed.  If the requested length is at least as large as this stream's
     * buffer, however, then this method will flush the buffer and write the
     * bytes directly to the underlying output stream.  Thus redundant
     * <code>BufferedOutputStream</code>s will not copy data unnecessarily.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized void write(byte b[], int off, int len) throws IOException {
        if (len >= buf.length) {
            /* If the request length exceeds the size of the output buffer,
            flush the output buffer and then write the data directly.
            In this way buffered streams will cascade harmlessly. */
            flushBuffer();
            out.write(b, off, len);
        } else {
        	if (len > buf.length - count) {
        		flushBuffer();
        	}
        	System.arraycopy(b, off, buf, count, len);
        	count += len;
        }
        totalBytesWritten += len;
        fireProgress();
        lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Flushes this buffered output stream. This forces any buffered 
     * output bytes to be written out to the underlying output stream. 
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized void flush() throws IOException {
        if(closed) {
            return;
        }
        flushBuffer();
        out.flush();
        lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Returns the time of the last activity
     *
     * @return time of the last activity on this stream
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Returns the total amount of bytes written to this stream so far
     *
     * @return the total amount of bytes written to this stream so far
     */
    public int getTotalBytesWritten() {
        return totalBytesWritten;
    }

    /**
     * Sets the callback for IO updates from a buffered stream
     *
     * @param progressListener the progressListener to set
     */
    public void setProgressListener(IOProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private void fireProgress() {
        if (progressListener != null) {
            progressListener.ioStreamUpdate(this, totalBytesWritten);
        }
    }

    /**
     * The default size for a stream buffer
     *
     * @return the defaultBufferSize
     */
    public static int getDefaultBufferSize() {
        return defaultBufferSize;
    }

    /**
     * The default size for a stream buffer
     *
     * @param aDefaultBufferSize the defaultBufferSize to set
     */
    public static void setDefaultBufferSize(int aDefaultBufferSize) {
        defaultBufferSize = aDefaultBufferSize;
    }

    /**
     * Writes <code>b.length</code> bytes to this output stream.
     * <p>
     * The <code>write</code> method of <code>FilterOutputStream</code>
     * calls its <code>write</code> method of three arguments with the
     * arguments <code>b</code>, <code>0</code>, and
     * <code>b.length</code>.
     * <p>
     * Note that this method does not call the one-argument
     * <code>write</code> method of its underlying stream with the single
     * argument <code>b</code>.
     *
     * @param      b   the data to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     * <p>
     * The <code>close</code> method of <code>FilterOutputStream</code>
     * calls its <code>flush</code> method, and then calls the
     * <code>close</code> method of its underlying output stream.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        if(!closed) {
            streamCount--;
            Util.getImplementation().closingOutput(out);
            Util.getImplementation().logStreamClose(name, false, streamCount);
            try {
                flush();
            } catch (Exception ignored) {
            } finally {
                Util.cleanup(out);
            }
            if(connection != null) {
                Util.getImplementation().cleanup(connection);
            }
            closed = true;
        } else {
            Util.getImplementation().logStreamDoubleClose(name, false);
        }
    }

    /**
     * If applicable this member represents the connection object for the stream
     *
     * @return the connection
     */
    public Object getConnection() {
        return connection;
    }

    /**
     * If applicable this member represents the connection object for the stream
     *
     * @param connection the connection to set
     */
    public void setConnection(Object connection) {
        this.connection = connection;
    }
}
