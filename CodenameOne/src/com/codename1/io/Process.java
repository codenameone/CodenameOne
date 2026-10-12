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
package com.codename1.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/// A child OS process. Stream directions and lifecycle methods follow JavaSE.
/// Read stdout and stderr concurrently to avoid filling a pipe. Never wait or do
/// blocking stream I/O on the EDT. Interrupting a wait does not kill the child.
/// Unlike JavaSE, this class is AutoCloseable: close forcibly terminates the direct
/// child and closes its three streams. Descendants are not terminated.
@com.codename1.impl.SharedWithBackend
public abstract class Process implements AutoCloseable {
    /// The child's stdin (the parent writes to it).
    public abstract OutputStream getOutputStream();
    /// The child's stdout (the parent reads from it).
    public abstract InputStream getInputStream();
    /// The child's stderr; EOF when redirectErrorStream(true) was selected.
    public abstract InputStream getErrorStream();
    /// Returns the exit code, or throws IllegalThreadStateException while alive.
    public abstract int exitValue();
    /// Requests termination of the direct child. On Windows this is forcible.
    public abstract void destroy();
    /// Forcibly terminates the direct child; completion is asynchronous.
    public abstract Process destroyForcibly();
    /// Whether the direct child is still running.
    public boolean isAlive() {
        try {
            exitValue();
            return false;
        } catch (IllegalThreadStateException ex) {
            return true;
        }
    }
    /// Waits for exit. An interrupt cancels only this wait.
    public int waitFor() throws InterruptedException {
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            try {
                return exitValue();
            } catch (IllegalThreadStateException ex) {
                pause(10);
            }
        }
    }
    /// Waits at most timeout milliseconds. Zero/negative values only poll.
    public boolean waitFor(long timeoutMillis) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!isAlive()) {
            return true;
        }
        if (timeoutMillis <= 0) {
            return false;
        }
        // Subtraction remains valid across nanoTime's signed wraparound.
        long budget = timeoutMillis > Long.MAX_VALUE / 1000000L ? Long.MAX_VALUE : timeoutMillis * 1000000L;
        long start = System.nanoTime();
        while (isAlive()) {
            long remaining = budget - (System.nanoTime() - start);
            if (remaining <= 0) {
                return false;
            }
            pause(Math.min(10L, Math.max(1L, remaining / 1000000L)));
        }
        return true;
    }
    /// Port hook for a bounded wait between exit polls.
    protected void pause(long millis) throws InterruptedException {
        Thread.sleep(millis);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
    /// JavaSE-style timed wait; sub-millisecond positive durations round up.
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return waitFor(timeout > 0 ? Math.max(1L, unit.toMillis(timeout)) : 0L);
    }
    /// Forcibly terminates the child and releases all pipes, even if one close fails.
    /// The runtime reaps it independently; this does not wait for termination.
    @Override
    public void close() throws IOException {
        destroyForcibly();
        IOException failure = null;
        try {
            getOutputStream().close();
        } catch (IOException ex) {
            failure = ex;
        }
        try {
            getInputStream().close();
        } catch (IOException ex) {
            failure = ex;
        }
        try {
            getErrorStream().close();
        } catch (IOException ex) {
            failure = ex;
        }
        if (failure != null) {
            throw failure;
        }
    }
}
