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

/// The terminal primitives the console API stands on: isatty() and termios on
/// POSIX, GetConsoleMode() and SetConsoleMode() on Windows
/// (`native/cn1_backend_terminal.c`).
final class TerminalImpl {
    private TerminalImpl() {
    }

    /// Whether file descriptor `fd` (0 for standard input, 1 for standard
    /// output, 2 for standard error) is attached to a terminal.
    static boolean isTerminal(int fd) {
        return isTerminalImpl(fd) != 0;
    }

    /// Whether the process runs on Windows.
    static boolean isWindows() {
        return isWindowsImpl() != 0;
    }

    /// The window size as `columns << 16 | rows`, or 0 when unknown.
    static int size() {
        return sizeImpl();
    }

    /// Turns on ANSI processing where a console needs asking (Windows).
    static void prepareOutput() {
        prepareOutputImpl();
    }

    /// Puts standard input in raw mode; false when it cannot be.
    static boolean enterRaw() {
        return enterRawImpl() != 0;
    }

    /// Restores the mode [#enterRaw] replaced.
    static void restore() {
        restoreImpl();
    }

    /// One byte of standard input, -1 at its end, -2 when `timeoutMs` passed
    /// first; a negative timeout waits for ever.
    static int readByte(int timeoutMs) {
        return readByteImpl(timeoutMs);
    }

    private static native int isTerminalImpl(int fd);

    private static native int isWindowsImpl();

    private static native int sizeImpl();

    private static native void prepareOutputImpl();

    private static native int enterRawImpl();

    private static native void restoreImpl();

    private static native int readByteImpl(int timeoutMs);

    private static native void cursorHiddenImpl(int hidden);

    /// Raw mode is available wherever a terminal is, so there is no other
    /// way to fall back on.
    static String readHidden() {
        return null;
    }

    static void cursorHidden(boolean hidden) {
        cursorHiddenImpl(hidden ? 1 : 0);
    }
}
