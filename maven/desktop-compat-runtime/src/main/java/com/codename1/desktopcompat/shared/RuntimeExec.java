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
package com.codename1.desktopcompat.shared;

import com.codename1.io.Process;
import com.codename1.io.ProcessBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// `Runtime.exec`, for desktop code relocated onto [ProcessBuilder].
///
/// The device runtime's `java.lang.Runtime` has no `exec`, so the build
/// rewrites `runtime.exec(...)` into a call here that takes the receiver
/// first, the way it rewrites every instance member the device lacks. Each
/// method does what the JavaSE overload of the same parameters does: a
/// command string is split at whitespace with no quoting (a shell is never
/// implied), and an environment array, when not null, replaces the inherited
/// environment entirely, and a null working directory inherits the parent's.
/// @hidden
public final class RuntimeExec {
    private RuntimeExec() {
    }

    /// `Runtime.exec(String)`.
    public static Process exec(Runtime runtime, String command) throws IOException {
        return exec(runtime, command, null);
    }

    /// `Runtime.exec(String, String[])`.
    public static Process exec(Runtime runtime, String command, String[] envp) throws IOException {
        return exec(runtime, command, envp, null);
    }

    /// `Runtime.exec(String, String[], File)`.
    public static Process exec(Runtime runtime, String command, String[] envp, File dir) throws IOException {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (command.length() == 0) {
            throw new IllegalArgumentException("Empty command");
        }
        return exec(runtime, split(command), envp, dir);
    }

    /// `Runtime.exec(String[])`.
    public static Process exec(Runtime runtime, String[] cmdarray) throws IOException {
        return exec(runtime, cmdarray, null);
    }

    /// `Runtime.exec(String[], String[])`.
    public static Process exec(Runtime runtime, String[] cmdarray, String[] envp) throws IOException {
        return exec(runtime, cmdarray, envp, null);
    }

    /// `Runtime.exec(String[], String[], File)`.
    public static Process exec(Runtime runtime, String[] cmdarray, String[] envp, File dir) throws IOException {
        if (runtime == null) {
            // What calling exec on a null Runtime throws.
            throw new NullPointerException("runtime");
        }
        if (cmdarray == null) {
            throw new NullPointerException("cmdarray");
        }
        if (cmdarray.length == 0) {
            // JavaSE reads cmdarray[0] unguarded.
            throw new IndexOutOfBoundsException("Empty command");
        }
        ProcessBuilder builder = new ProcessBuilder(cmdarray);
        if (envp != null) {
            Map<String, String> env = builder.environment();
            env.clear();
            for (String raw : envp) {
                // As JavaSE: anything after a NUL is dropped and an entry with
                // no '=' after its first character is skipped, not rejected.
                int nul = raw.indexOf('\0');
                String entry = nul >= 0 ? raw.substring(0, nul) : raw;
                int split = entry.indexOf('=', 1);
                if (split > 0) {
                    env.put(entry.substring(0, split), entry.substring(split + 1));
                }
            }
        }
        if (dir != null) {
            builder.directory(dir.getPath());
        }
        return builder.start();
    }

    /// The words of `command`, as `java.util.StringTokenizer` splits it with
    /// its default delimiters: space, tab, newline, carriage return, form feed.
    static String[] split(String command) {
        List<String> words = new ArrayList<String>();
        int start = -1;
        int n = command.length();
        for (int i = 0; i < n; i++) {
            if (isDelimiter(command.charAt(i))) {
                if (start >= 0) {
                    words.add(command.substring(start, i));
                    start = -1;
                }
            } else if (start < 0) {
                start = i;
            }
        }
        if (start >= 0) {
            words.add(command.substring(start));
        }
        return words.toArray(new String[words.size()]);
    }

    private static boolean isDelimiter(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }
}
