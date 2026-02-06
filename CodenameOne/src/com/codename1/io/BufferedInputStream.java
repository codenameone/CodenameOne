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
import com.codename1.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

/// Based on the buffered input stream from the JDK with some minor tweaks to allow
/// external classes to monitor stream status and progress.
public class BufferedInputStream extends InputStream {
    private static int streamCount = 0;

    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private static volatile int defaultBufferSize = 8192;
    private final String name;
    private int actualAvailable = defaultBufferSize;
    private Object connection;
    private InputStream in;
    private byte[] buf;
    private IOProgressListener progressListener;
    private boolean disableBuffering;
    private boolean closed;
    private boolean stopped;
    /// The index one greater than the index of the last valid byte in
    /// the buffer.
    /// This value is always
    /// in the range `0` through `buf.length`;
    /// elements `buf[0]`  through `buf[count-1]`contain buffered input data obtained
    /// from the underlying  input stream.
    private int count;
    private long lastActivityTime;
    private int totalBytesRead;
    private boolean printInput;
    private int yield = -1;
    private long elapsedSinceLastYield;
    /// The current position in the buffer. This is the index of the next
    /// character to be read from the `buf` array.
    ///
    /// This value is always in the range `0`
    /// through `count`. If it is less
    /// than `count`, then  `buf[pos]`
    /// is the next byte to be supplied as input;
    /// if it is equal to `count`, then
    /// the  next `read` or `skip`
    /// operation will require more bytes to be
    /// read from the contained  input stream.
    ///
    /// #### See also
    ///
    /// - java.io.BufferedInputStream#buf
    private int pos;
    /// The value of the `pos` field at the time the last
    /// `mark` method was called.
    ///
    /// This value is always
    /// in the range `-1` through `pos`.
    /// If there is no marked position in  the input
    /// stream, this field is `-1`. If
    /// there is a marked position in the input
    /// stream,  then `buf[markpos]`
    /// is the first byte to be supplied as input
    /// after a `reset` operation. If
    /// `markpos` is not `-1`,
    /// then all bytes from positions `buf[markpos]`
    /// through  `buf[pos-1]` must remain
    /// in the buffer array (though they may be
    /// moved to  another place in the buffer array,
    /// with suitable adjustments to the values
    /// of `count`,  `pos`,
    /// and `markpos`); they may not
    /// be discarded unless and until the difference
    /// between `pos` and `markpos`
    /// exceeds `marklimit`.
    ///
    /// #### See also
    ///
    /// - java.io.BufferedInputStream#mark(int)
    ///
    /// - java.io.BufferedInputStream#pos
    private int markpos = -1;
    /// The maximum read ahead allowed after a call to the
    /// `mark` method before subsequent calls to the
    /// `reset` method fail.
    /// Whenever the difference between `pos`
    /// and `markpos` exceeds `marklimit`,
    /// then the  mark may be dropped by setting
    /// `markpos` to `-1`.
    ///
    /// #### See also
    ///
    /// - java.io.BufferedInputStream#mark(int)
    ///
    /// - java.io.BufferedInputStream#reset()
    private int marklimit;

    /// Creates a `BufferedInputStream`
    /// and saves its  argument, the input stream
    /// `in`, for later use. An internal
    /// buffer array is created and  stored in `buf`.
    ///
    /// #### Parameters
    ///
    /// - `in`: the underlying input stream.
    public BufferedInputStream(InputStream in) {
        this(in, defaultBufferSize);
    }

    /// Creates a `BufferedInputStream`
    /// and saves its  argument, the input stream
    /// `in`, for later use. An internal
    /// buffer array is created and  stored in `buf`.
    ///
    /// #### Parameters
    ///
    /// - `in`: the underlying input stream.
    ///
    /// - `name`: the name of the stream
    public BufferedInputStream(InputStream in, String name) {
        this(in, defaultBufferSize, name);
    }

    /// Creates a `BufferedInputStream`
    /// with the specified buffer size,
    /// and saves its  argument, the input stream
    /// `in`, for later use.  An internal
    /// buffer array of length  `size`
    /// is created and stored in `buf`.
    ///
    /// #### Parameters
    ///
    /// - `in`: the underlying input stream.
    ///
    /// - `size`: the buffer size.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if size <= 0.
    public BufferedInputStream(InputStream in, int size) {
        this(in, size, "unnamed");
    }

    /// Creates a `BufferedInputStream`
    /// with the specified buffer size,
    /// and saves its  argument, the input stream
    /// `in`, for later use.  An internal
    /// buffer array of length  `size`
    /// is created and stored in `buf`.
    ///
    /// #### Parameters
    ///
    /// - `in`: the underlying input stream.
    ///
    /// - `size`: the buffer size.
    ///
    /// - `name`: the name of the stream
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if size <= 0.
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

    /// The default size for a stream buffer
    ///
    /// #### Returns
    ///
    /// the defaultBufferSize
    public static int getDefaultBufferSize() {
        return defaultBufferSize;
    }

    /// The default size for a stream buffer
    ///
    /// #### Parameters
    ///
    /// - `aDefaultBufferSize`: the defaultBufferSize to set
    public static void setDefaultBufferSize(int aDefaultBufferSize) {
        defaultBufferSize = aDefaultBufferSize;
    }

    /// Indicates the name of the stream for debugging purposes
    ///
    /// #### Returns
    ///
    /// the name of the stream
    public String getName() {
        return name;
    }

    /// Check to make sure that the underlying input stream has not been
    /// nulled out due to close; if not return it;
    private synchronized InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        if (input == null) {
            throw new IOException("Stream closed");
        }
        return input;
    }

    /// Check to make sure that buffer has not been nulled out due to
    /// close; if not return it;
    private synchronized byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null) {
            throw new IOException("Stream closed");
        }
        return buffer;
    }

    /// Fills the buffer with more data, taking into account
    /// shuffling and other tricks for dealing with marks.
    /// Assumes that it is being called by a synchronized method.
    /// This method also assumes that all data has already been read in,
    /// hence pos > count.
    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        if (markpos < 0) {
            pos = 0;        /* no mark: throw away the buffer */
        } else if (pos >= buffer.length) /* no room left in buffer */ {
            if (markpos > 0) {    /* can throw away early part of the buffer */
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);
                pos = sz;
                markpos = 0;
            } else if (buffer.length >= marklimit) {
                markpos = -1;    /* buffer got too big, invalidate mark */
                pos = 0;    /* drop buffer contents */
            } else {        /* grow buffer */
                int nsz = pos * 2;
                if (nsz > marklimit) {
                    nsz = marklimit;
                }
                byte[] nbuf = new byte[nsz];
                System.arraycopy(buffer, 0, nbuf, 0, pos);
                if (buffer != buf) { //NOPMD CompareObjectsWithEquals
                    throw new IOException("Stream closed");
                }
                buf = nbuf;
                buffer = nbuf;
            }
        }
        count = pos;
        if (actualAvailable < 0) {
            return;
        }
        int sizeOfBuffer = (buffer.length - pos);
        int n = getInIfOpen().read(buffer, pos, sizeOfBuffer);
        if (n > 0) {
            count = n + pos;
        } else {
            if (n < 0) {
                actualAvailable = -1;
            }
        }
    }

    /// Allows access to the underlying input stream if desired
    ///
    /// #### Returns
    ///
    /// the internal input stream
    public InputStream getInternal() {
        return in;
    }

    /// See
    /// the general contract of the `read`
    /// method of `InputStream`.
    ///
    /// #### Returns
    ///
    /// @return the next byte of data, or `-1` if the end of the
    /// stream is reached.
    ///
    /// #### Throws
    ///
    /// - `IOException`: @throws IOException if this input stream has been closed by
    /// invoking its `#close()` method,
    /// or an I/O error occurs.
    @Override
    public synchronized int read() throws IOException {
        if (stopped) {
            return -1;
        }
        lastActivityTime = System.currentTimeMillis();
        if (disableBuffering) {
            int v = getInIfOpen().read();
            if (printInput && v > -1) {
                System.out.print((char) v);
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
        if (printInput) {
            System.out.print((char) v);
        }
        return v;

    }

    private void fireProgress() {
        if (progressListener != null) {
            progressListener.ioStreamUpdate(this, totalBytesRead);
        }
    }

    /// Read characters into a portion of an array, reading from the underlying
    /// stream at most once if necessary.
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos;
        if (avail <= 0) {
            /* If the requested length is at least as large as the buffer, and
            if there is no mark/reset activity, do not bother to copy the
            bytes into the local buffer.  In this way buffered streams will
            cascade harmlessly. */
            if (len >= getBufIfOpen().length && markpos < 0) {
                int val = getInIfOpen().read(b, off, len);
                if (val < 0) {
                    actualAvailable = -1;
                } else {
                    if (printInput) {
                        System.out.print(StringUtil.newString(b, off, val));
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
        if (printInput) {
            System.out.print(StringUtil.newString(b, off, cnt));
        }
        pos += cnt;
        return cnt;
    }

    private void yieldTime() {
        long time = System.currentTimeMillis();
        long sleepDuration = 0;
        synchronized (this) {
            if (time - elapsedSinceLastYield > 300) {
                elapsedSinceLastYield = time;
                sleepDuration = yield;
            }
        }
        if (sleepDuration > 0) {
            int sleepMs = sleepDuration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sleepDuration;
            Util.sleep(sleepMs);
        }
    }

    /// Reads bytes from this byte-input stream into the specified byte array,
    /// starting at the given offset.
    ///
    ///  This method implements the general contract of the corresponding
    /// ```int, int) read``` method of
    /// the ```InputStream``` class.  As an additional
    /// convenience, it attempts to read as many bytes as possible by repeatedly
    /// invoking the `read` method of the underlying stream.  This
    /// iterated `read` continues until one of the following
    /// conditions becomes true:
    ///
    /// -  The specified number of bytes have been read,
    ///
    /// -  The `read` method of the underlying stream returns
    /// `-1`, indicating end-of-file, or
    ///
    /// -  The `available` method of the underlying stream
    /// returns zero, indicating that further input requests would block.
    ///
    ///  If the first `read` on the underlying stream returns
    /// `-1` to indicate end-of-file then this method returns
    /// `-1`.  Otherwise this method returns the number of bytes
    /// actually read.
    ///
    ///  Subclasses of this class are encouraged, but not required, to
    /// attempt to read as many bytes as possible in the same fashion.
    ///
    /// #### Parameters
    ///
    /// - `b`: destination buffer.
    ///
    /// - `off`: offset at which to start storing bytes.
    ///
    /// - `len`: maximum number of bytes to read.
    ///
    /// #### Returns
    ///
    /// @return the number of bytes read, or `-1` if the end of
    /// the stream has been reached.
    ///
    /// #### Throws
    ///
    /// - `IOException`: @throws IOException if this input stream has been closed by
    ///                     invoking its `#close()` method,
    ///                     or an I/O error occurs.
    @Override
    public synchronized int read(byte[] b, int off, int len)
            throws IOException {
        if (stopped) {
            return -1;
        }
        if (yield > -1 && !Display.getInstance().isEdt()) {
            yieldTime();
        }
        lastActivityTime = System.currentTimeMillis();
        if (disableBuffering) {
            int v = getInIfOpen().read(b, off, len);
            if (v > -1) {
                if (printInput) {
                    System.out.print(StringUtil.newString(b, off, v));
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
        for (; ; ) {
            if (stopped) {
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
            InputStream input = in; //NOPMD CloseResource - stream managed by BufferedInputStream#close
            if (input != null && superAvailable() <= 0) {
                break;
            }
            if (yield > -1 && !Display.getInstance().isEdt()) {
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

    /// See the general contract of the `skip`
    /// method of `InputStream`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: @throws IOException if the stream does not support seek,
    ///                     or if this input stream has been closed by
    ///                     invoking its `#close()` method, or an
    ///                     I/O error occurs.
    @Override
    public synchronized long skip(long n) throws IOException {
        if (disableBuffering) {
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

    /// Returns an estimate of the number of bytes that can be read (or
    /// skipped over) from this input stream without blocking by the next
    /// invocation of a method for this input stream. The next invocation might be
    /// the same thread or another thread.  A single read or skip of this
    /// many bytes will not block, but may read or skip fewer bytes.
    ///
    /// This method returns the sum of the number of bytes remaining to be read in
    /// the buffer (`count - pos`) and the result of calling the
    /// `in`.available().
    ///
    /// #### Returns
    ///
    /// @return an estimate of the number of bytes that can be read (or skipped
    /// over) from this input stream without blocking.
    ///
    /// #### Throws
    ///
    /// - `IOException`: @throws IOException if this input stream has been closed by
    ///                     invoking its close() method,
    ///                     or an I/O error occurs.
    @Override
    public synchronized int available() throws IOException {
        if (disableBuffering) {
            return in.available();
        }
        return superAvailable() + (count - pos);
    }

    /// See the general contract of the `mark`
    /// method of `InputStream`.
    ///
    /// #### Parameters
    ///
    /// - `readlimit`: @param readlimit the maximum limit of bytes that can be read before
    ///                  the mark position becomes invalid.
    @Override
    public synchronized void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    /// See the general contract of the `reset`
    /// method of `InputStream`.
    ///
    /// If `markpos` is `-1`
    /// (no mark has been set or the mark has been
    /// invalidated), an `IOException`
    /// is thrown. Otherwise, `pos` is
    /// set equal to `markpos`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: @throws IOException if this stream has not been marked or,
    ///                     if the mark has been invalidated, or the stream
    ///                     has been closed by invoking its `#close()`
    ///                     method, or an I/O error occurs.
    @Override
    public synchronized void reset() throws IOException {
        getBufIfOpen(); // Cause exception if closed
        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        pos = markpos;
    }

    /// Tests if this input stream supports the `mark`
    /// and `reset` methods. The `markSupported`
    /// method of `BufferedInputStream` returns
    /// `true`.
    ///
    /// #### Returns
    ///
    /// @return a `boolean` indicating if this stream type supports
    /// the `mark` and `reset` methods.
    ///
    /// #### See also
    ///
    /// - java.io.InputStream#mark(int)
    ///
    /// - java.io.InputStream#reset()
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    private int superAvailable() {
        try {
            if (actualAvailable < 0) {
                return -1;
            }
            return in.available();
        } catch (IOException err) {
            // Available sometimes fails on MIDP devices and shouldn't be used
            return actualAvailable;
        }
    }

    /// Closes this input stream and releases any system resources
    /// associated with the stream.
    /// Once the stream has been closed, further read(), available(), reset(),
    /// or skip() invocations will throw an IOException.
    /// Closing a previously closed stream has no effect.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if an I/O error occurs.
    @Override
    public void close() throws IOException {
        if (closed) {
            Util.getImplementation().logStreamDoubleClose(name, true);
            return;
        }
        closed = true;
        streamCount--;
        Util.getImplementation().logStreamClose(name, true, streamCount);
        if (connection != null) {
            Util.getImplementation().cleanup(connection);
        }
        byte[] buffer = buf;
        if (buffer != null) {
            buf = null;
            InputStream input = in; //NOPMD CloseResource
            in = null;
            if (input != null) {
                Util.cleanup(input);
            }
        }
    }

    /// Returns the time of the last activity
    ///
    /// #### Returns
    ///
    /// time of the last activity on this stream
    public synchronized long getLastActivityTime() {
        return lastActivityTime;
    }

    /// Returns the total number of bytes read from this stream so far
    ///
    /// #### Returns
    ///
    /// the total number of bytes read from this stream so far
    public synchronized int getTotalBytesRead() {
        return totalBytesRead;
    }

    /// Sets the callback for IO updates from a buffered stream
    ///
    /// #### Parameters
    ///
    /// - `progressListener`: the progressListener to set
    public void setProgressListener(IOProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /// {@inheritDoc}
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /// If applicable this member represents the connection object for the stream
    ///
    /// #### Returns
    ///
    /// the connection
    public Object getConnection() {
        return connection;
    }

    /// If applicable this member represents the connection object for the stream
    ///
    /// #### Parameters
    ///
    /// - `connection`: the connection to set
    public void setConnection(Object connection) {
        this.connection = connection;
    }

    /// #### Returns
    ///
    /// the disableBuffering
    public synchronized boolean isDisableBuffering() {
        return disableBuffering;
    }

    /// #### Parameters
    ///
    /// - `disableBuffering`: the disableBuffering to set
    public synchronized void setDisableBuffering(boolean disableBuffering) {
        this.disableBuffering = disableBuffering;
    }

    /// Prints out all the data that passes through this stream to the console.
    /// This is a very useful debugging tool.
    ///
    /// #### Returns
    ///
    /// the printInput
    public synchronized boolean isPrintInput() {
        return printInput;
    }

    /// Prints out all the data that passes through this stream to the console.
    /// This is a very useful debugging tool.
    ///
    /// #### Parameters
    ///
    /// - `printInput`: the printInput to set
    public synchronized void setPrintInput(boolean printInput) {
        this.printInput = printInput;
    }

    /// Allows setting a yield duration for this stream which is useful for background
    /// operations to release CPU
    ///
    /// #### Returns
    ///
    /// the yield
    public synchronized int getYield() {
        return yield;
    }

    /// Allows setting a yield duration for this stream which is useful for background
    /// operations to release CPU
    ///
    /// #### Parameters
    ///
    /// - `yield`: the yield to set
    public synchronized void setYield(int yield) {
        this.yield = yield;
    }

    /// Stop reading from the stream, invoking this will cause the read() to
    /// return -1
    public synchronized void stop() {
        stopped = true;
    }

}
