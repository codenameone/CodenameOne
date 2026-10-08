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
package android.util;

/// Android's logging API, written to the Codename One log so the messages land
/// wherever the application's logging already goes (console, log file, crash
/// reports).
public final class Log {

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private Log() {
    }

    public static int v(String tag, String msg) {
        return println(VERBOSE, tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return println(VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int d(String tag, String msg) {
        return println(DEBUG, tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return println(DEBUG, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int i(String tag, String msg) {
        return println(INFO, tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return println(INFO, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int w(String tag, String msg) {
        return println(WARN, tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return println(WARN, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int w(String tag, Throwable tr) {
        return println(WARN, tag, getStackTraceString(tr));
    }

    public static int e(String tag, String msg) {
        return println(ERROR, tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (tr != null) {
            com.codename1.io.Log.e(tr);
        }
        return println(ERROR, tag, msg);
    }

    public static int wtf(String tag, String msg) {
        return println(ASSERT, tag, msg);
    }

    public static int wtf(String tag, Throwable tr) {
        return println(ASSERT, tag, getStackTraceString(tr));
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        return println(ASSERT, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static boolean isLoggable(String tag, int level) {
        return level >= INFO;
    }

    /// Codename One cannot render another thread's stack into a string on
    /// every port, so this is the exception's own description.
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        return tr.toString();
    }

    public static int println(int priority, String tag, String msg) {
        String level;
        switch (priority) {
            case VERBOSE:
                level = "V";
                break;
            case DEBUG:
                level = "D";
                break;
            case INFO:
                level = "I";
                break;
            case WARN:
                level = "W";
                break;
            case ERROR:
                level = "E";
                break;
            default:
                level = "A";
                break;
        }
        String line = level + "/" + tag + ": " + msg;
        int cn1Level = priority >= ERROR ? com.codename1.io.Log.ERROR
                : priority >= WARN ? com.codename1.io.Log.WARNING
                : priority >= INFO ? com.codename1.io.Log.INFO : com.codename1.io.Log.DEBUG;
        com.codename1.io.Log.p(line, cn1Level);
        return line.length();
    }
}
