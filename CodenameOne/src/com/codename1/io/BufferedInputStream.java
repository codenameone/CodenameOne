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

import com.codename1.ui.Display;
import java.io.*;

/**
 * Based on the buffered input stream from the JDK with some minor tweaks to allow
 * external classes to monitor stream status and progress.
 */
public class BufferedInputStream extends InputStream {
    private static int streamCount = 0;
    private int actualAvailable = defaultBufferSize;
    private Object connection;
    private InputStream in;
    private static int defaultBufferSize = 8192;
    private byte[] buf;
    private IOProgressListener progressListener;
    private boolean disableBuffering;
    private boolean closed;
    private boolean stopped;

    /**
     * The index one greater than the index of the last valid byte in 
     * the buffer. 
     * This value is always
     * in the range <code>0</code> through <code>buf.length</code>;
     * elements <code>buf[0]</code>  through <code>buf[count-1]
     * </code>contain buffered input data obtained
     * from the underlying  input stream.
     */
    private int count;
    private long lastActivityTime;
    private int totalBytesRead;

    private boolean printInput;
    private String name;
    private int yield = -1;
    private long elapsedSinceLastYield;
    

    /**
     * Indicates the name of the stream for debugging purposes
     *
     * @return the name of the stream
     */
    public String getName() {
        return name;
    }

    /**
     * The current position in the buffer. This is the index of the next 
     * character to be read from the <code>buf</code> array. 
     * <p>
     * This value is always in the range <code>0</code>
     * through <code>count</code>. If it is less
     * than <code>count</code>, then  <code>buf[pos]</code>
     * is the next byte to be supplied as input;
     * if it is equal to <code>count</code>, then
     * the  next <code>read</code> or <code>skip</code>
     * operation will require more bytes to be
     * read from the contained  input stream.
     *
     * @see     java.io.BufferedInputStream#buf
     */
    private int pos;

    /**
     * The value of the <code>pos</code> field at the time the last 
     * <code>mark</code> method was called.
     * <p>
     * This value is always
     * in the range <code>-1</code> through <code>pos</code>.
     * If there is no marked position in  the input
     * stream, this field is <code>-1</code>. If
     * there is a marked position in the input
     * stream,  then <code>buf[markpos]</code>
     * is the first byte to be supplied as input
     * after a <code>reset</code> operation. If
     * <code>markpos</code> is not <code>-1</code>,
     * then all bytes from positions <code>buf[markpos]</code>
     * through  <code>buf[pos-1]</code> must remain
     * in the buffer array (though they may be
     * moved to  another place in the buffer array,
     * with suitable adjustments to the values
     * of <code>count</code>,  <code>pos</code>,
     * and <code>markpos</code>); they may not
     * be discarded unless and until the difference
     * between <code>pos</code> and <code>markpos</code>
     * exceeds <code>marklimit</code>.
     *
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#pos
     */
    private int markpos = -1;

    /**
     * The maximum read ahead allowed after a call to the 
     * <code>mark</code> method before subsequent calls to the 
     * <code>reset</code> method fail. 
     * Whenever the difference between <code>pos</code>
     * and <code>markpos</code> exceeds <code>marklimit</code>,
     * then the  mark may be dropped by setting
     * <code>markpos</code> to <code>-1</code>.
     *
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#reset()
     */
    private int marklimit;

    /**
     * Check to make sure that underlying input stream has not been
     * nulled out due to close; if not return it;
     */
    private InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        if (input == null) {
            throw new IOException("Stream closed");
        }
        return input;
    }

    /**
     * Check to make sure that buffer has not been nulled out due to
     * close; if not return it;
     */
    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null) {
            throw new IOException("Stream closed");
        }
        return buffer;
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * and saves its  argument, the input stream
     * <code>in</code>, for later use. An internal
     * buffer array is created and  stored in <code>buf</code>.
     *
     * @param   in   the underlying input stream.
     */
    public BufferedInputStream(InputStream in) {
        this(in, defaultBufferSize);
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * and saves its  argument, the input stream
     * <code>in</code>, for later use. An internal
     * buffer array is created and  stored in <code>buf</code>.
     *
     * @param   in   the underlying input stream.
     * @param name the name of the stream
     */
    public BufferedInputStream(InputStream in, String name) {
        this(in, defaultBufferSize, name);
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * with the specified buffer size,
     * and saves its  argument, the input stream
     * <code>in</code>, for later use.  An internal
     * buffer array of length  <code>size</code>
     * is created and stored in <code>buf</code>.
     *
     * @param   in     the underlying input stream.
     * @param   size   the buffer size.
     * @exception IllegalArgumentException if size <= 0.
     */
    public BufferedInputStream(InputStream in, int size) {
        this(in, size, "unnamed");
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * with the specified buffer size,
     * and saves its  argument, the input stream
     * <code>in</code>, for later use.  An internal
     * buffer array of length  <code>size</code>
     * is created and stored in <code>buf</code>.
     *
     * @param   in     the underlying input stream.
     * @param   size   the buffer size.
     * @param name the name of the stream
     * @exception IllegalArgumentException if size <= 0.
     */
    public BufferedInputStream(InputStream in, int size, String name) {
        this.in = in;
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
        streamCount++;
        this.name = name;
        Util.getImplementation().logStreamCreate(name, true, streamCount);
    }

    /**
     * Fills the buffer with more data, taking into account
     * shuffling and other tricks for dealing with marks.
     * Assumes that it is being called by a synchronized method.
     * This method also assumes that all data has already been read in,
     * hence pos > count.
     */
    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        if (markpos < 0) {
            pos = 0;		/* no mark: throw away the buffer */
        } else if (pos >= buffer.length) /* no room left in buffer */ {
            if (markpos > 0) {	/* can throw away early part of the buffer */
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);
                pos = sz;
                markpos = 0;
            } else if (buffer.length >= marklimit) {
                markpos = -1;	/* buffer got too big, invalidate mark */
                pos = 0;	/* drop buffer contents */
            } else {		/* grow buffer */
                int nsz = pos * 2;
                if (nsz > marklimit) {
                    nsz = marklimit;
                }
                byte nbuf[] = new byte[nsz];
                System.arraycopy(buffer, 0, nbuf, 0, pos);
                if (buffer != buf) {
                    throw new IOException("Stream closed");
                }
                buf = nbuf;
                buffer = nbuf;
            }
        }
        count = pos;
        if(actualAvailable < 0) {
            return;
        }
        int sizeOfBuffer = (buffer.length - pos);
        int n = getInIfOpen().read(buffer, pos, sizeOfBuffer);
        if (n > 0) {
            count = n + pos;
        } else {
            if(n < 0) {
                actualAvailable = -1;
            } 
        }
    }

    /**
     * Allows access to the underlying input stream if desired
     * @return the internal input stream
     */
    public InputStream getInternal() {
        return in;
    }
    
    /**
     * See
     * the general contract of the <code>read</code>
     * method of <code>InputStream</code>.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if this input stream has been closed by
     *				invoking its {@link #close()} method,
     *				or an I/O error occurs. 
     */
    public synchronized int read() throws IOException {
        if(stopped){
            return -1;
        }
        lastActivityTime = System.currentTimeMillis();
        if(disableBuffering) {
            int v = getInIfOpen().read();
            if(printInput && v > -1) {
                System.out.print((char)v);
            }
            totalBytesRead++;
            fireProgress();
            return v;
        }
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        totalBytesRead++;
        fireProgress();
        int v = getBufIfOpen()[pos++] & 0xff;
        if(printInput) {
            System.out.print((char)v);
        }
        return v;

    }

    private void fireProgress() {
        if (progressListener != null) {
            progressListener.ioStreamUpdate(this, totalBytesRead);
        }
    }

    /**
     * Read characters into a portion of an array, reading from the underlying
     * stream at most once if necessary.
     */
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos;
        if (avail <= 0) {
            /* If the requested length is at least as large as the buffer, and
            if there is no mark/reset activity, do not bother to copy the
            bytes into the local buffer.  In this way buffered streams will
            cascade harmlessly. */
            if (len >= getBufIfOpen().length && markpos < 0) {
                int val = getInIfOpen().read(b, off, len);
                if(val < 0) {
                    actualAvailable = -1;
                } else {
                    if(printInput) {
                        System.out.print(new String(b, off, val));
                    }
                }
                return val;
            }
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return -1;
            }
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);
        if(printInput) {
            System.out.print(new String(b, off, cnt));
        }
        pos += cnt;
        return cnt;
    }

    private void yieldTime() {
        long time = System.currentTimeMillis();
        if(time - elapsedSinceLastYield > 300) {
            try {
                Thread.sleep(yield);
            } catch (InterruptedException ex) {
            }
            elapsedSinceLastYield = time;
        }
    }

    /**
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     *
     * <p> This method implements the general contract of the corresponding
     * <code>{@link InputStream#read(byte[], int, int) read}</code> method of
     * the <code>{@link InputStream}</code> class.  As an additional
     * convenience, it attempts to read as many bytes as possible by repeatedly
     * invoking the <code>read</code> method of the underlying stream.  This
     * iterated <code>read</code> continues until one of the following
     * conditions becomes true: <ul>
     *
     *   <li> The specified number of bytes have been read,
     *
     *   <li> The <code>read</code> method of the underlying stream returns
     *   <code>-1</code>, indicating end-of-file, or
     *
     *   <li> The <code>available</code> method of the underlying stream
     *   returns zero, indicating that further input requests would block.
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of bytes
     * actually read.
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many bytes as possible in the same fashion.
     *
     * @param      b     destination buffer.
     * @param      off   offset at which to start storing bytes.
     * @param      len   maximum number of bytes to read.
     * @return     the number of bytes read, or <code>-1</code> if the end of
     *             the stream has been reached.
     * @exception  IOException  if this input stream has been closed by
     *				invoking its {@link #close()} method,
     *				or an I/O error occurs. 
     */
    public synchronized int read(byte b[], int off, int len)
            throws IOException {
        if(stopped){
            return -1;
        }
        if(yield > -1 && !Display.getInstance().isEdt()) {
            yieldTime();
        }
        lastActivityTime = System.currentTimeMillis();
        if(disableBuffering) {
            int v = getInIfOpen().read(b, off, len);
            if (v > -1) {
            	if(printInput) {
            		System.out.print(new String(b, off, v));
            	}
            
            	totalBytesRead += v;
            	fireProgress();
            }
            return v;
        }
        getBufIfOpen(); // Check for closed stream
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int n = 0;
        for (;;) {
            if(stopped){
                return -1;
            }
            lastActivityTime = System.currentTimeMillis();
            int nread = read1(b, off + n, len - n);
            if (nread <= 0) {
                n = (n == 0) ? nread : n;
                break;
            }
            n += nread;
            if (n >= len) {
                break;
            }
            // if not closed but no bytes available, return
            InputStream input = in;
            if (input != null && superAvailable() <= 0) {
                break;
            }
            if(yield > -1 && !Display.getInstance().isEdt()) {
                yieldTime();
            }
        }
        if (n > 0) {
        	totalBytesRead += n;
        	fireProgress();
        }
        lastActivityTime = System.currentTimeMillis();

        return n;
    }

    /**
     * See the general contract of the <code>skip</code>
     * method of <code>InputStream</code>.
     *
     * @exception  IOException  if the stream does not support seek,
     *				or if this input stream has been closed by
     *				invoking its {@link #close()} method, or an
     *				I/O error occurs.
     */
    public synchronized long skip(long n) throws IOException {
        if(disableBuffering) {
            long v = getInIfOpen().skip(n);
            totalBytesRead += v;
            fireProgress();
            return v;
        }
        getBufIfOpen(); // Check for closed stream
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;

        if (avail <= 0) {
            // If no mark position set then don't keep in buffer
            if (markpos < 0) {
                return getInIfOpen().skip(n);
            }

            // Fill in buffer to save bytes for reset
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return 0;
            }
        }

        long skipped = (avail < n) ? avail : n;
        pos += skipped;
        totalBytesRead += (int) skipped;
        fireProgress();
        lastActivityTime = System.currentTimeMillis();
        return skipped;
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p>
     * This method returns the sum of the number of bytes remaining to be read in
     * the buffer (<code>count&nbsp;- pos</code>) and the result of calling the
     * {@link java.io.FilterInputStream#in in}.available().
     *
     * @return     an estimate of the number of bytes that can be read (or skipped
     *             over) from this input stream without blocking.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its close() method,
     *                          or an I/O error occurs.
     */
    public synchronized int available() throws IOException {
        if(disableBuffering) {
            return available();
        }
        return superAvailable() + (count - pos);
    }

    /** 
     * See the general contract of the <code>mark</code>
     * method of <code>InputStream</code>.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     */
    public synchronized void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    /**
     * See the general contract of the <code>reset</code>
     * method of <code>InputStream</code>.
     * <p>
     * If <code>markpos</code> is <code>-1</code>
     * (no mark has been set or the mark has been
     * invalidated), an <code>IOException</code>
     * is thrown. Otherwise, <code>pos</code> is
     * set equal to <code>markpos</code>.
     *
     * @exception  IOException  if this stream has not been marked or,
     *			if the mark has been invalidated, or the stream 
     *			has been closed by invoking its {@link #close()}
     *			method, or an I/O error occurs.
     */
    public synchronized void reset() throws IOException {
        getBufIfOpen(); // Cause exception if closed
        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        pos = markpos;
    }

    /**
     * Tests if this input stream supports the <code>mark</code> 
     * and <code>reset</code> methods. The <code>markSupported</code> 
     * method of <code>BufferedInputStream</code> returns 
     * <code>true</code>. 
     *
     * @return  a <code>boolean</code> indicating if this stream type supports
     *          the <code>mark</code> and <code>reset</code> methods.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return in.markSupported();
    }

    private int superAvailable() {
        try {
            if(actualAvailable < 0) {
                return -1;
            }
            return in.available();
        } catch(IOException err) {
            // Available sometimes fails on MIDP devices and shouldn't be used
            return actualAvailable;
        }
    }

    /**
     * Closes this input stream and releases any system resources 
     * associated with the stream. 
     * Once the stream has been closed, further read(), available(), reset(),
     * or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        if(closed) {
            Util.getImplementation().logStreamDoubleClose(name, true);
            return;
        }
        closed = true;
        streamCount--;
        Util.getImplementation().logStreamClose(name, true, streamCount);
        if(connection != null) {
            Util.getImplementation().cleanup(connection);
        }
        byte[] buffer;
        while ((buffer = buf) != null) {
            if (buf == buffer) { //bufUpdater.compareAndSet(this, buffer, null)) {
                buf = null;
                InputStream input = in;
                in = null;
                if (input != null) {
                    input.close();
                }
                return;
            }
            // Else retry in case a new buf was CASed in fill()
        }
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
     * Returns the total amount of bytes read from this stream so far
     * 
     * @return the total amount of bytes read from this stream so far
     */
    public int getTotalBytesRead() {
        return totalBytesRead;
    }

    /**
     * Sets the callback for IO updates from a buffered stream
     *
     * @param progressListener the progressListener to set
     */
    public void setProgressListener(IOProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
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

    /**
     * @return the disableBuffering
     */
    public boolean isDisableBuffering() {
        return disableBuffering;
    }

    /**
     * @param disableBuffering the disableBuffering to set
     */
    public void setDisableBuffering(boolean disableBuffering) {
        this.disableBuffering = disableBuffering;
    }

    /**
     * Prints out all the data that passes through this stream to the console.
     * This is a very useful debugging tool.
     *
     * @return the printInput
     */
    public boolean isPrintInput() {
        return printInput;
    }

    /**
     * Prints out all the data that passes through this stream to the console.
     * This is a very useful debugging tool.
     * 
     * @param printInput the printInput to set
     */
    public void setPrintInput(boolean printInput) {
        this.printInput = printInput;
    }

    /**
     * Allows setting a yield duration for this stream which is useful for background
     * operations to release CPU
     *
     * @return the yield
     */
    public int getYield() {
        return yield;
    }

    /**
     * Allows setting a yield duration for this stream which is useful for background
     * operations to release CPU
     * @param yield the yield to set
     */
    public void setYield(int yield) {
        this.yield = yield;
    }
/**
     * Stop reading from the stream, invoking this will cause the read() to 
     * return -1
     */
    public void stop(){
        stopped = true;
    }

}
