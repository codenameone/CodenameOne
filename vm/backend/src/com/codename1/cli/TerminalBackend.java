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
package com.codename1.cli;

/// Everything the console classes ask of the process: the terminal primitives,
/// the two output streams, the environment and the clock. The running process
/// answers through [SystemTerminal]; tests install a scripted one with
/// [Console#install] so a prompt or a progress bar can be driven key by key
/// and its output compared byte for byte.
interface TerminalBackend {
    /// Whether file descriptor `fd` (0, 1 or 2) is a terminal.
    boolean isTerminal(int fd);

    /// Whether the process runs on Windows.
    boolean isWindows();

    /// The window size as `columns << 16 | rows`, or 0 when unknown.
    int size();

    /// Puts standard input in raw mode; false when it cannot be.
    boolean enterRaw();

    /// Restores the mode [#enterRaw] replaced.
    void restore();

    /// One byte of standard input, -1 at its end, -2 when `timeoutMs` passed
    /// first; a negative timeout waits for ever.
    int readByte(int timeoutMs);

    /// Writes `text` to standard output and flushes it.
    void write(String text);

    /// Writes `text` to standard error and flushes it.
    void writeError(String text);

    /// The environment variable `name`, or null.
    String getenv(String name);

    /// The current time in milliseconds.
    long currentTimeMillis();

    /// Reads a line from the terminal without echoing it, when raw mode is
    /// unavailable but the platform has its own way; null when it has none.
    String readHidden();

    /// Told when a live region hides (true) or shows (false) the cursor, so
    /// a process ended by a signal or System.exit shows it again.
    void cursorHidden(boolean hidden);
}
