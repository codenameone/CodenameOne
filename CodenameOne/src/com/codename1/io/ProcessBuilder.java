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

import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.process.ProcessSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Creates desktop/backend subprocesses using JavaSE-style command lists and pipes.
/// The working directory is a `java.io.File`, as in JavaSE.
/// Shell parsing is never implicit: invoke a shell explicitly if needed.
/// Mobile/browser ports compile this API but start() throws
/// UnsupportedOperationException. Support does not imply permission to run every
/// executable. Builders are mutable and are not thread safe.
///
/// File redirection and inheritIO are not provided in this initial subset. Use
/// the streams and drain stdout/stderr concurrently, or merge them first.
///
/// The members shared with `java.lang.ProcessBuilder` have its names and
/// parameter types, so desktop code compiled against the JavaSE class runs
/// against this one once its class references are renamed. JavaSE members
/// with no counterpart: `inheritIO()`, the `redirectInput`/`redirectOutput`/`redirectError`
/// family with `ProcessBuilder.Redirect` (no port can attach a child's
/// streams to anything but pipes), and `startPipeline(List)`, which needs
/// those redirects.
@com.codename1.impl.SharedWithBackend
public final class ProcessBuilder {
    private List<String> command;
    private File directory;
    private Map<String, String> environment;
    private boolean mergeError;

    public ProcessBuilder(String... command) {
        command(command);
    }
    public ProcessBuilder(List<String> command) {
        command(command);
    }

    /// Whether this port implements subprocess creation (no Display init required).
    public static boolean isSupported() {
        return support() != null;
    }
    private static ProcessSupport support() {
        return (ProcessSupport) ImplementationFactory.getInstance().createProcessSupport();
    }
    /// Returns the live command list, as in JavaSE.
    public List<String> command() {
        return command;
    }
    /// Keeps the supplied list, as in JavaSE. Validated again at start().
    public ProcessBuilder command(List<String> command) {
        if (command == null) {
            throw new NullPointerException("command");
        }
        this.command = command;
        return this;
    }
    public ProcessBuilder command(String... command) {
        if (command == null) {
            throw new NullPointerException("command");
        }
        this.command = new ArrayList<String>();
        for (String arg : command) {
            this.command.add(arg);
        }
        return this;
    }
    /// Null inherits the parent's working directory.
    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }
    public File directory() {
        return directory;
    }
    /// A mutable copy of the inherited environment, initialized on first access.
    /// Changes are validated at start(), including changes through entrySet().
    /// Throws UnsupportedOperationException on unsupported ports.
    public Map<String, String> environment() {
        if (environment == null) {
            ProcessSupport support = support();
            if (support == null) {
                throw new UnsupportedOperationException("Processes are not supported on this port");
            }
            environment = support.environment();
        }
        return environment;
    }
    public ProcessBuilder redirectErrorStream(boolean merge) {
        mergeError = merge;
        return this;
    }
    public boolean redirectErrorStream() {
        return mergeError;
    }

    /// Launches a child. Invalid arguments throw IllegalArgumentException (or
    /// NullPointerException); launch/permission errors throw IOException or
    /// SecurityException. An unsupported port throws UnsupportedOperationException.
    public Process start() throws IOException {
        ProcessSupport support = support();
        if (support == null) {
            throw new UnsupportedOperationException("Processes are not supported on this port");
        }
        String[] args = command.toArray(new String[command.size()]);
        if (args.length == 0) {
            throw new IllegalArgumentException("Empty command");
        }
        for (String arg : args) {
            checkString(arg);
        }
        if (args[0].length() == 0) {
            throw new IllegalArgumentException("Empty executable");
        }
        Map<String, String> env = null;
        if (environment != null) {
            env = new HashMap<String, String>(environment);
            for (Map.Entry<String, String> entry : env.entrySet()) {
                checkString(entry.getKey());
                checkString(entry.getValue());
                if (entry.getKey().length() == 0 || entry.getKey().indexOf('=') >= 0) {
                    throw new IllegalArgumentException("Invalid environment name");
                }
            }
        }
        String dir = null;
        if (directory != null) {
            dir = osPath(directory.getAbsolutePath());
            checkString(dir);
        }
        return support.start(args, dir, env, mergeError);
    }

    /// The OS path of an absolute `File` path. On a port whose `java.io.File`
    /// sits over `FileSystemStorage` that path can carry the `file:` scheme,
    /// which a child process's working directory cannot.
    static String osPath(String path) {
        if (!path.startsWith("file:")) {
            return path;
        }
        int i = 5;
        while (i + 1 < path.length() && path.charAt(i) == '/' && path.charAt(i + 1) == '/') {
            i++;
        }
        String p = path.substring(i);
        // file:///C:/x names the drive path C:/x.
        if (p.length() >= 4 && p.charAt(0) == '/' && p.charAt(2) == ':' && p.charAt(3) == '/') {
            char d = p.charAt(1);
            if ((d >= 'A' && d <= 'Z') || (d >= 'a' && d <= 'z')) {
                return p.substring(1);
            }
        }
        return p;
    }
    private static void checkString(String value) {
        if (value == null) {
            throw new NullPointerException();
        }
        if (value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Embedded NUL");
        }
    }
}
