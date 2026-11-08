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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/// Java SE twin of TerminalImpl. The JVM has no isatty() and no termios, so
/// this asks the way JLine does: `stty` run against `/dev/tty` for the size
/// and for raw mode, and System.console() for whether a stream is a terminal.
/// On Windows there is no `stty`; raw mode is unavailable there and the
/// prompts fall back to reading whole lines.
final class TerminalImpl {
    /// The `stty -g` state raw mode replaced, null while not raw.
    private static final AtomicReference<String> SAVED = new AtomicReference<String>();
    private static final AtomicReference<Thread> HOOK = new AtomicReference<Thread>();

    private TerminalImpl() {
    }

    static boolean isTerminal(int fd) {
        java.io.Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            Object answer = java.io.Console.class.getMethod("isTerminal").invoke(console);
            return !(answer instanceof Boolean) || ((Boolean) answer).booleanValue();
        } catch (NoSuchMethodException older) {
            // JDK 21 or earlier: a console at all means a terminal.
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    static boolean isWindows() {
        return File.separatorChar == '\\';
    }

    static int size() {
        if (!hasTty()) {
            return 0;
        }
        String out = stty("size");
        if (out == null) {
            return 0;
        }
        String[] parts = out.trim().split("\\s+");
        if (parts.length != 2) {
            return 0;
        }
        try {
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            if (rows <= 0 || cols <= 0) {
                return 0;
            }
            return ((cols & 0xffff) << 16) | (rows & 0xffff);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static void prepareOutput() {
        // Every terminal a JVM runs in on POSIX reads ANSI; Windows Terminal
        // does too, and legacy conhost cannot be reached without JNI.
    }

    static boolean enterRaw() {
        if (SAVED.get() != null) {
            return true;
        }
        if (!hasTty() || !isTerminal(0)) {
            return false;
        }
        String saved = stty("-g");
        if (saved == null || saved.trim().length() == 0) {
            return false;
        }
        if (stty("-icanon -echo -isig -ixon -icrnl min 1 time 0") == null) {
            // stty applies what it can before failing; put all of it back.
            stty(saved.trim());
            return false;
        }
        SAVED.set(saved.trim());
        installHook();
        return true;
    }

    /// Restores the mode and shows a hidden cursor when the JVM exits,
    /// Ctrl-C outside a prompt included.
    private static void installHook() {
        if (HOOK.get() == null) {
            Thread hook = new Thread(new Runnable() {
                @Override
                public void run() {
                    restore();
                    if (CURSOR_HIDDEN.getAndSet(false)) {
                        System.out.print("\u001b[?25h");
                        System.out.flush();
                    }
                }
            }, "cn1-terminal-restore");
            if (HOOK.compareAndSet(null, hook)) {
                Runtime.getRuntime().addShutdownHook(hook);
            }
        }
    }

    private static final AtomicBoolean CURSOR_HIDDEN = new AtomicBoolean();

    static void cursorHidden(boolean hidden) {
        CURSOR_HIDDEN.set(hidden);
        if (hidden) {
            installHook();
        }
    }

    static void restore() {
        String saved = SAVED.getAndSet(null);
        if (saved != null) {
            stty(saved);
            System.out.print("\u001b[?25h");
            System.out.flush();
        }
    }

    static int readByte(int timeoutMs) {
        try {
            if (timeoutMs >= 0) {
                long end = System.currentTimeMillis() + timeoutMs;
                while (System.in.available() <= 0) {
                    if (System.currentTimeMillis() >= end) {
                        return -2;
                    }
                    Thread.sleep(5);
                }
            }
            return System.in.read();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static boolean hasTty() {
        return File.separatorChar == '/' && new File("/dev/tty").exists();
    }

    /// Runs `stty args < /dev/tty` and answers its output, or null when it failed.
    private static String stty(String args) {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "stty " + args + " < /dev/tty");
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream out = p.getInputStream()) {
                byte[] buf = new byte[256];
                int n;
                while ((n = out.read(buf)) > 0) {
                    bytes.write(buf, 0, n);
                }
            }
            return p.waitFor() == 0 ? new String(bytes.toByteArray(), "UTF-8") : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /// A line read with echo off by the JDK's console, which has its own way
    /// on Windows where `stty` is not available; null without a console.
    static String readHidden() {
        java.io.Console c = System.console();
        if (c == null) {
            return null;
        }
        char[] chars = c.readPassword();
        return chars == null ? null : new String(chars);
    }
}
