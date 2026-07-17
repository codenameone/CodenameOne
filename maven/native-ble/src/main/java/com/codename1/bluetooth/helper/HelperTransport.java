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
package com.codename1.bluetooth.helper;

import java.io.IOException;
import java.util.List;

/**
 * The abstraction over the {@code cn1-ble-helper} child process's standard
 * I/O. {@link HelperBleBackend} speaks the line-delimited JSON protocol
 * through this seam only, so a host that has no operating-system process API
 * (the native Windows/Linux ports, which reach the subprocess through a
 * native bridge) can supply its own implementation, while the JavaSE
 * simulator supplies a subprocess-backed one.
 *
 * <p>All methods are called from at most one reader thread and the caller
 * thread; implementations must make {@link #readLine()}/{@link #writeLine}
 * and {@link #close()}/{@link #isAlive()} safe for that pairing.</p>
 */
public interface HelperTransport {

    /**
     * Launches the helper. {@code command} is the launch command line (the
     * resolved helper binary path, optionally with arguments). A transport
     * that was pre-configured with its own command by its factory may
     * ignore a {@code null} argument and launch that instead.
     *
     * @param command the launch command line, or {@code null} to use the
     *                transport's pre-configured command
     * @throws IOException when the child process cannot be started
     */
    void start(List<String> command) throws IOException;

    /**
     * Reads one line of the helper's standard output, blocking until a line
     * is available.
     *
     * @return the next line without its terminator, or {@code null} at
     *         end of stream (the helper closed stdout / exited)
     * @throws IOException when the pipe fails
     */
    String readLine() throws IOException;

    /**
     * Writes one command line to the helper's standard input. The line
     * terminator is appended by the transport and the stream is flushed.
     *
     * @param line the single-line JSON command, without a terminator
     * @throws IOException when the pipe fails
     */
    void writeLine(String line) throws IOException;

    /** Releases the transport and terminates the child process. */
    void close();

    /** True while the child process is running. */
    boolean isAlive();
}
