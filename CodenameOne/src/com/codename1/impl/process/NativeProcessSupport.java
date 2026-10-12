/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.process;

import com.codename1.io.Process;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

/// ParparVM adapter shared by native desktop and backend targets.
/// Native operations only poll; bounded waits park backend virtual threads.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class NativeProcessSupport extends ProcessSupport {
    public Map<String, String> environment() {
        Map<String, String> result = new HashMap<String, String>();
        String[] entries = environmentImpl();
        if (entries == null) {
            throw new IllegalStateException("Cannot read process environment");
        }
        for (String entry : entries) {
            int split = entry.indexOf('=');
            // Windows' hidden =C: drive entries are inherited when no explicit
            // environment is supplied, but are not mutable environment names.
            if (split > 0) {
                result.put(entry.substring(0, split), entry.substring(split + 1));
            }
        }
        return result;
    }
    public Process start(String[] command, String directory, Map<String, String> environment, boolean mergeError)
            throws IOException {
        String[] env = null;
        if (environment != null) {
            env = new String[environment.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                env[i++] = entry.getKey() + "=" + entry.getValue();
            }
        }
        Child child = new Child();
        long handle = launch(command, directory, env, mergeError);
        if (handle <= 0) {
            throw new IOException("Cannot start " + command[0] + " (OS error " + (-handle) + ")");
        }
        synchronized (child) {
            child.handle = handle;
            try {
                child.startReaper();
            } catch (RuntimeException ex) {
                abandon(handle);
                child.handle = 0;
                throw ex;
            } catch (Error ex) {
                abandon(handle);
                child.handle = 0;
                throw ex;
            }
        }
        return child;
    }
    private static final class Child extends Process implements Runnable {
        private long handle;
        private boolean exited;
        private int exitCode;
        private final InputStream stdout = new PipeInput(1);
        private final InputStream stderr = new PipeInput(2);
        private final OutputStream stdin = new PipeOutput();
        private final boolean[] closed = new boolean[3];
        void startReaper() {
            // Own the child until reaped even if the application drops Process.
            // No pipe is closed on exit: unread output remains available.
            Thread reaper = new Thread(this, "CN1 process reaper");
            reaper.setDaemon(true);
            reaper.start();
        }
        public void run() {
            while (isAlive()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        }
        public InputStream getInputStream() {
            return stdout;
        }
        public InputStream getErrorStream() {
            return stderr;
        }
        public OutputStream getOutputStream() {
            return stdin;
        }
        public synchronized int exitValue() {
            if (!exited) {
                long result = poll(handle);
                if (result == Long.MIN_VALUE) {
                    throw new IllegalThreadStateException("Process is running");
                }
                exited = true;
                exitCode = (int) result;
                releaseIfClosed();
            }
            return exitCode;
        }
        public synchronized void destroy() {
            if (handle != 0) {
                terminate(handle, false);
            }
        }
        public synchronized Process destroyForcibly() {
            if (handle != 0) {
                terminate(handle, true);
            }
            return this;
        }
        private synchronized void closePipe(int index) {
            if (!closed[index]) {
                closed[index] = true;
                closePipeImpl(handle, index);
            }
            releaseIfClosed();
        }
        private void releaseIfClosed() {
            if (handle != 0 && exited && closed[0] && closed[1] && closed[2]) {
                release(handle);
                handle = 0;
            }
        }
        private synchronized int transfer(int index, byte[] bytes, int offset, int length) throws IOException {
            if (closed[index]) {
                throw new IOException("Stream closed");
            }
            int n = transferImpl(handle, index, bytes, offset, length);
            if (n == -2) {
                throw new IOException("Process pipe I/O failed");
            }
            return n;
        }
        protected synchronized void finalize() {
            // The reaper holds this object until the OS child is gone. Streams
            // hold it too; finalization cannot race a poll, I/O or termination.
            if (handle != 0) {
                release(handle);
                handle = 0;
            }
        }
        private static void bounds(byte[] bytes, int offset, int length) {
            if (bytes == null) {
                throw new NullPointerException();
            }
            if (offset < 0 || length < 0 || length > bytes.length - offset) {
                throw new IndexOutOfBoundsException();
            }
        }
        protected void pause(long millis) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            pauseImpl(millis);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        private void pausePipe() throws InterruptedIOException {
            try {
                pause(5);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted process I/O");
            }
        }
        private final class PipeInput extends InputStream {
            private final int index;
            PipeInput(int index) {
                this.index = index;
            }
            public int read() throws IOException {
                byte[] one = new byte[1];
                return read(one, 0, 1) < 0 ? -1 : one[0] & 255;
            }
            private boolean reading;
            public int read(byte[] bytes, int offset, int length) throws IOException {
                bounds(bytes, offset, length);
                if (length == 0) {
                    return 0;
                }
                synchronized (this) {
                    if (reading) {
                        throw new IOException("Concurrent reads on a process pipe");
                    }
                    reading = true;
                }
                try {
                    while (true) {
                        int n = transfer(index, bytes, offset, length);
                        if (n != -3) {
                            return n;
                        }
                        try {
                            pausePipe();
                        } catch (InterruptedIOException ex) {
                            closePipe(index);
                            throw ex;
                        }
                    }
                } finally {
                    synchronized (this) {
                        reading = false;
                    }
                }
            }
            public void close() {
                closePipe(index);
            }
        }
        private final class PipeOutput extends OutputStream {
            public void write(int value) throws IOException {
                write(new byte[] {(byte) value}, 0, 1);
            }
            private boolean writing;
            public void write(byte[] bytes, int offset, int length) throws IOException {
                bounds(bytes, offset, length);
                synchronized (this) {
                    if (writing) {
                        throw new IOException("Concurrent writes on a process pipe");
                    }
                    writing = true;
                }
                int sent = 0;
                try {
                    while (sent < length) {
                        int n = transfer(0, bytes, offset + sent, length - sent);
                        if (n == -3) {
                            try {
                                pausePipe();
                            } catch (InterruptedIOException ex) {
                                closePipe(0);
                                ex.bytesTransferred = sent;
                                throw ex;
                            }
                        } else {
                            sent += n;
                        }
                    }
                } finally {
                    synchronized (this) {
                        writing = false;
                    }
                }
            }
            public void close() {
                closePipe(0);
            }
        }
    }
    private static native void pauseImpl(long millis);
    private static native String[] environmentImpl();
    private static native long launch(String[] command, String directory, String[] environment, boolean mergeError);
    private static native long poll(long handle);
    private static native void terminate(long handle, boolean force);
    private static native int transferImpl(long handle, int index, byte[] bytes, int offset, int length);
    private static native void closePipeImpl(long handle, int index);
    private static native void release(long handle);
    private static native void abandon(long handle);
}
