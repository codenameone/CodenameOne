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
package com.codename1.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** How the crash payload labels a raw stack so the server picks the right parser. */
class TraceFormatTest {

    @Test
    void emptyOrNoStack() {
        assertEquals(CrashReportPayload.TRACE_NONE,
                CrashReportPayload.deriveTraceFormat(null, null, "Android"));
        assertEquals(CrashReportPayload.TRACE_NONE,
                CrashReportPayload.deriveTraceFormat(null, "", "HTML5"));
    }

    @Test
    void javaScriptPortIsJsError() {
        // The JavaScript port reports platform HTML5 and a JS engine stack.
        assertEquals(CrashReportPayload.TRACE_JS,
                CrashReportPayload.deriveTraceFormat(null,
                        "at run (http://host/app.js:12:34)\n", "HTML5"));
    }

    @Test
    void parparVmTextIsRecognized() {
        assertEquals(CrashReportPayload.TRACE_PARPARVM,
                CrashReportPayload.deriveTraceFormat(null,
                        "    at com.foo.Bar.baz:42\n    at com.foo.Bar.qux:7\n", "ios"));
    }

    @Test
    void jvmMessageWithParparvmShapedLineIsNotParparvm() {
        // A real-JVM (Android/desktop) throwable whose MESSAGE contains a line shaped like a ParparVM frame
        // ("    at X.Y:N", no parentheses); printStackTrace echoes the message into the raw stack. The
        // platform gate must keep it NONE so the server does not fabricate a frame from message text.
        String raw = "java.lang.IllegalStateException: bad input:\n    at fake.Type.method:12\n";
        assertEquals(CrashReportPayload.TRACE_NONE,
                CrashReportPayload.deriveTraceFormat(null, raw, "Android"));
        assertEquals(CrashReportPayload.TRACE_NONE,
                CrashReportPayload.deriveTraceFormat(null, raw, "SE"));
        // The very same raw text on an actual ParparVM-C platform IS parparvm-text -- only the platform
        // distinguishes them, which is the point of the gate.
        assertEquals(CrashReportPayload.TRACE_PARPARVM,
                CrashReportPayload.deriveTraceFormat(null, raw, "win"));
    }

    @Test
    void jvmStackIsNotMislabeledJavaScript() {
        // A stackless JVM/Android throwable whose only frames are in its cause: printStackTrace
        // produces a tab-indented JVM trace with parentheses. It must NOT be labeled js-error.
        String jvm = "java.lang.RuntimeException: boom\n"
                + "\tat com.foo.Bar.baz(Bar.java:42)\n"
                + "\tat com.foo.Bar.main(Bar.java:7)\n";
        assertEquals(CrashReportPayload.TRACE_NONE,
                CrashReportPayload.deriveTraceFormat(null, jvm, "Android"));
        assertEquals(CrashReportPayload.TRACE_NONE,
                CrashReportPayload.deriveTraceFormat(null, jvm, "SE"));
    }
}
