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
package com.codename1.compat.jdk;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The members of `java.lang.System`, `Runtime` and `Thread` a desktop
/// application names and the device's classes do not have, or have with
/// nothing behind them.
///
/// The build's remap step redirects each such call here; an instance method
/// arrives with its receiver as the first argument.
///
/// #### System properties
///
/// A device has no property table, so `System.getProperty` answers null for
/// every key there. Here the keys desktop code reads to find its way around
/// are answered from what the platform does know:
///
/// - `user.home`, `user.dir` and `java.io.tmpdir` are the application's home
///   directory, the one place it may always write;
/// - `os.name` is the Codename One platform name (`ios`, `and`, ...);
/// - `line.separator`, `file.separator` and `path.separator` are the ones
///   the file classes of this package use;
/// - `user.name` and `java.version` are fixed, harmless values.
///
/// Anything set through `setProperty` is remembered for the run, and any
/// other key is asked of `Display.getProperty`, which answers null for what
/// it does not know -- as the JDK does.
///
/// #### What does nothing
///
/// There is no environment, so `getenv` answers null. A thread cannot be
/// made a daemon or renamed, and is never reported interrupted. Shutdown
/// hooks are kept and run by `exit`, which is the only orderly way out an
/// application has; a platform that kills the process runs none.
public final class JdkSystem {

    private static final Map<String, String> OVERRIDES = new HashMap<String, String>();
    private static final List<Thread> HOOKS = new ArrayList<Thread>();
    private static UncaughtExceptionHandler defaultHandler;
    private static boolean handlerInstalled;

    private JdkSystem() {
    }

    private static String home() {
        String home = FileSystemStorage.getInstance().getAppHomePath();
        if (home == null) {
            return "";
        }
        // Desktop code appends a separator itself: "/tmp", never "/tmp/".
        while (home.length() > 1 && home.charAt(home.length() - 1) == '/' && !home.endsWith("://")) {
            home = home.substring(0, home.length() - 1);
        }
        return home;
    }

    public static String getProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("key can't be empty");
        }
        if (OVERRIDES.containsKey(key)) {
            return OVERRIDES.get(key);
        }
        if ("user.home".equals(key) || "user.dir".equals(key) || "java.io.tmpdir".equals(key)) {
            return home();
        }
        if ("os.name".equals(key)) {
            return Display.getInstance().getPlatformName();
        }
        if ("line.separator".equals(key)) {
            return "\n";
        }
        if ("file.separator".equals(key)) {
            return File.separator;
        }
        if ("path.separator".equals(key)) {
            return File.pathSeparator;
        }
        if ("user.name".equals(key)) {
            return "user";
        }
        if ("java.version".equals(key)) {
            return "1.8.0";
        }
        return Display.getInstance().getProperty(key, null);
    }

    public static String getProperty(String key, String def) {
        String value = getProperty(key);
        return value == null ? def : value;
    }

    public static String setProperty(String key, String value) {
        if (value == null) {
            throw new NullPointerException("value can't be null");
        }
        String old = getProperty(key);
        OVERRIDES.put(key, value);
        return old;
    }

    /// Removes the property for this run: a later read answers null, also
    /// for a key that has a built-in answer.
    public static String clearProperty(String key) {
        String old = getProperty(key);
        OVERRIDES.put(key, null);
        return old;
    }

    public static String getenv(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        return null;
    }

    public static Map<String, String> getenv() {
        return new HashMap<String, String>();
    }

    public static String lineSeparator() {
        return "\n";
    }

    /// Runs the shutdown hooks, in the order they were added, and asks the
    /// platform to close the application. The status has nowhere to go.
    public static void exit(int status) {
        List<Thread> hooks = new ArrayList<Thread>(HOOKS);
        HOOKS.clear();
        for (Thread hook : hooks) {
            hook.run();
        }
        Display.getInstance().exitApplication();
    }

    public static void exit(Runtime runtime, int status) {
        exit(status);
    }

    public static void halt(Runtime runtime, int status) {
        HOOKS.clear();
        Display.getInstance().exitApplication();
    }

    public static void addShutdownHook(Runtime runtime, Thread hook) {
        if (hook == null) {
            throw new NullPointerException();
        }
        if (HOOKS.contains(hook)) {
            throw new IllegalArgumentException("Hook previously registered");
        }
        HOOKS.add(hook);
    }

    public static boolean removeShutdownHook(Runtime runtime, Thread hook) {
        if (hook == null) {
            throw new NullPointerException();
        }
        return HOOKS.remove(hook);
    }

    /// One: application code is confined to a single thread, and sizing a
    /// pool by this number should not start more.
    public static int availableProcessors(Runtime runtime) {
        return 1;
    }

    public static long maxMemory(Runtime runtime) {
        return runtime.totalMemory();
    }

    // ---- Thread ----

    public static void setDaemon(Thread thread, boolean on) {
        // Every Codename One thread ends with the application.
    }

    public static boolean isDaemon(Thread thread) {
        return true;
    }

    public static void setName(Thread thread, String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
    }

    public static boolean isInterrupted(Thread thread) {
        return false;
    }

    public static boolean interrupted() {
        return false;
    }

    public static long getId(Thread thread) {
        return System.identityHashCode(thread) & 0xffffffffL;
    }

    /// Waits for `thread` to end, at most `millis` milliseconds; 0 waits
    /// without a limit.
    public static void join(Thread thread, long millis) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        if (millis == 0) {
            thread.join();
            return;
        }
        long end = System.currentTimeMillis() + millis;
        while (thread.isAlive()) {
            long left = end - System.currentTimeMillis();
            if (left <= 0) {
                return;
            }
            Thread.sleep(left < 10 ? left : 10);
        }
    }

    public static void join(Thread thread, long millis, int nanos) throws InterruptedException {
        join(thread, nanos > 0 ? millis + 1 : millis);
    }

    public static void sleep(long millis, int nanos) throws InterruptedException {
        Thread.sleep(nanos >= 500000 || millis == 0 && nanos > 0 ? millis + 1 : millis);
    }

    /// Makes `handler` the receiver of every exception that escapes on the
    /// event dispatch thread, through Codename One's own error handling. An
    /// exception that is handed over is not reported a second time by the
    /// framework's crash dialog.
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        defaultHandler = handler;
        if (handler != null && !handlerInstalled) {
            handlerInstalled = true;
            Display.getInstance().addEdtErrorHandler(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    cn1Dispatch(evt);
                }
            });
        }
    }

    static void cn1Dispatch(ActionEvent evt) {
        UncaughtExceptionHandler handler = defaultHandler;
        Object source = evt.getSource();
        if (handler != null && source instanceof Throwable) {
            evt.consume();
            handler.uncaughtException(Thread.currentThread(), (Throwable) source);
        }
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultHandler;
    }

    /// A handler for one thread has nothing to hook into; the default
    /// handler is the one that runs.
    public static void setUncaughtExceptionHandler(Thread thread, UncaughtExceptionHandler handler) {
    }

    public static UncaughtExceptionHandler getUncaughtExceptionHandler(Thread thread) {
        return defaultHandler;
    }

    // ---- Throwable ----

    /// Writes the exception and its causes to `out`. The frames themselves
    /// are not available as text on every platform, so they go where the
    /// platform prints them: the application log.
    public static void printStackTrace(Throwable t, PrintWriter out) {
        Throwable current = t;
        String lead = "";
        int depth = 0;
        while (current != null && depth < 16) {
            out.println(lead + current);
            lead = "Caused by: ";
            Throwable cause = current.getCause();
            current = cause == current ? null : cause;
            depth++;
        }
        out.flush();
        com.codename1.io.Log.e(t);
    }
}
