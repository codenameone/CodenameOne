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
package com.codename1.impl.backend.test;

/// The report of a compiled test run, written for the build to read back.
///
/// The generated runners call this around each test; the goal that ran the binary
/// parses the lines into Surefire's XML. One line per event, fields separated by
/// a tab, with tabs and line breaks in a message escaped:
///
/// ```
/// CN1TEST  PASS  com.example.ApiTest  greets  12
/// CN1TEST  FAIL  com.example.ApiTest  refuses  3  java.lang.AssertionError  Status expected:<400> but was:<200>
/// CN1TEST  SKIP  com.example.ApiTest  later  0  disabled
/// ```
public final class TestRun {
    private static int passed;
    private static int failed;
    private static int skipped;

    private TestRun() {
    }

    /// The clock reading a test started at.
    public static long started() {
        return System.currentTimeMillis();
    }

    /// Records the outcome of one test: passed when `failure` is null, skipped when
    /// it is an aborted assumption, failed otherwise.
    public static void finished(String cls, String method, long started, Throwable failure) {
        long millis = System.currentTimeMillis() - started;
        if (failure == null) {
            passed++;
            line("PASS", cls, method, millis, null, null);
        } else if (isAbort(failure)) {
            skipped++;
            line("SKIP", cls, method, millis, failure.getClass().getName(), failure.getMessage());
        } else {
            failed++;
            line("FAIL", cls, method, millis, failure.getClass().getName(), describe(failure));
        }
    }

    /// Records a test that did not run, and why.
    public static void skipped(String cls, String method, String reason) {
        skipped++;
        line("SKIP", cls, method, 0, "skipped", reason);
    }

    /// Records a class-level failure -- an `@BeforeAll` or `@AfterAll` that threw.
    public static void lifecycleFailed(String cls, String phase, Throwable failure) {
        failed++;
        line("FAIL", cls, phase, 0, failure.getClass().getName(), describe(failure));
    }

    /// Prints the totals and returns the number of failures.
    public static int finish() {
        System.out.println("CN1TEST\tDONE\t" + passed + "\t" + failed + "\t" + skipped);
        System.out.flush();
        return failed;
    }

    private static boolean isAbort(Throwable failure) {
        // By name, so the same check holds against opentest4j on the JVM and the
        // translated shim; both define it under this name.
        for (Class c = failure.getClass(); c != null; c = c.getSuperclass()) {
            if ("org.opentest4j.TestAbortedException".equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Throwable failure) {
        StringBuilder sb = new StringBuilder(String.valueOf(failure.getMessage()));
        Throwable cause = failure.getCause();
        int depth = 0;
        while (cause != null && cause != failure && depth++ < 5) { //NOPMD CompareObjectsWithEquals - a self-caused throwable
            sb.append(" | caused by ").append(cause.getClass().getName()).append(": ")
                    .append(cause.getMessage());
            cause = cause.getCause();
        }
        return sb.toString();
    }

    private static void line(String kind, String cls, String method, long millis, String type,
                             String message) {
        StringBuilder sb = new StringBuilder("CN1TEST\t").append(kind).append('\t').append(cls)
                .append('\t').append(escape(method)).append('\t').append(millis);
        if (type != null) {
            sb.append('\t').append(type).append('\t').append(escape(message));
        }
        System.out.println(sb.toString());
        System.out.flush();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
