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

import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/// Package-private DTO for a single crash report. Carries the structured
/// payload sent to the cloud and is serialised to JSON via
/// {@link #toJson()} (a hand-rolled writer avoids a Jackson / parser
/// dependency on the device).
final class CrashReportPayload {

    static final int MAX_FRAMES = 32;
    static final int MAX_MESSAGE_LEN = 8192;
    /// Hard cap on the attached native-log snapshot. Logcat dumps on
    /// Android and stderr ring buffers on iOS can be huge; the server
    /// rejects anything larger and we'd rather truncate client-side
    /// than have the upload silently dropped.
    static final int MAX_NATIVE_LOG_LEN = 32 * 1024;
    /// Hard cap on the native-stack string. Native backtraces from
    /// signal handlers are usually compact (~64 frames * ~120 chars),
    /// but a corrupt stack can produce arbitrarily long output.
    static final int MAX_NATIVE_STACK_LEN = 16 * 1024;

    final String eventId;
    final String buildKey;
    final String packageName;
    final String appName;
    final String appVersion;
    final String platform;
    final String osVersion;
    final String exceptionClass;
    final String messageScrubbed;
    final List<Frame> frames;
    /// Recent platform-log output captured at crash time. Provides
    /// context the Java stack frame alone can't (NSLog/os_log on iOS,
    /// logcat on Android). `null` if the platform has no readable log
    /// or the snapshot failed.
    final String nativeLog;
    /// Raw native backtrace string for crashes captured by the
    /// platform native crash handler (signal/Mach exception/uncaught
    /// Objective-C). `null` for pure-Java crashes -- their stack lives
    /// in {@link #frames}.
    final String nativeStack;
    final String locale;
    final long clientTs;

    CrashReportPayload(String eventId, String exceptionClass,
            String messageScrubbed, List<Frame> frames,
            String nativeLog, String nativeStack) {
        this.eventId = eventId;
        this.exceptionClass = exceptionClass;
        this.messageScrubbed = trim(messageScrubbed, MAX_MESSAGE_LEN);
        this.frames = capFrames(frames);
        this.nativeLog = trim(nativeLog, MAX_NATIVE_LOG_LEN);
        this.nativeStack = trim(nativeStack, MAX_NATIVE_STACK_LEN);
        Display d = Display.getInstance();
        this.buildKey = d.getProperty("build_key", "");
        this.packageName = d.getProperty("package_name", "");
        this.appName = d.getProperty("AppName", "");
        this.appVersion = d.getProperty("AppVersion", "");
        this.platform = d.getPlatformName();
        this.osVersion = d.getProperty("OSVer", "");
        Locale loc = Locale.getDefault();
        this.locale = loc == null ? "" : loc.toString();
        this.clientTs = System.currentTimeMillis();
    }

    static final class Frame {
        final String className;
        final String methodName;
        final String fileName;
        final int lineNumber;
        final boolean nativeFrame;

        Frame(String className, String methodName, String fileName,
                int lineNumber, boolean nativeFrame) {
            this.className = className == null ? "" : className;
            this.methodName = methodName == null ? "" : methodName;
            this.fileName = fileName == null ? "" : fileName;
            this.lineNumber = lineNumber;
            this.nativeFrame = nativeFrame;
        }
    }

    /// Renders the payload as a JSON object string suitable for posting
    /// in the HTTP request body. Conforms to RFC 8259.
    String toJson() {
        StringBuilder b = new StringBuilder(1024);
        b.append('{');
        appendString(b, "eventId", eventId, true);
        appendString(b, "buildKey", buildKey, false);
        appendString(b, "packageName", packageName, false);
        appendString(b, "appName", appName, false);
        appendString(b, "appVersion", appVersion, false);
        appendString(b, "platform", platform, false);
        appendString(b, "osVersion", osVersion, false);
        appendString(b, "exceptionClass", exceptionClass, false);
        appendString(b, "message", messageScrubbed, false);
        appendString(b, "locale", locale, false);
        appendString(b, "nativeLog", nativeLog, false);
        appendString(b, "nativeStack", nativeStack, false);
        b.append(",\"clientTs\":").append(clientTs);
        b.append(",\"frames\":[");
        for (int i = 0; i < frames.size(); i++) {
            Frame f = frames.get(i);
            if (i > 0) {
                b.append(',');
            }
            b.append('{');
            appendString(b, "cls", f.className, true);
            appendString(b, "method", f.methodName, false);
            appendString(b, "file", f.fileName, false);
            b.append(",\"line\":").append(f.lineNumber);
            b.append(",\"native\":").append(f.nativeFrame);
            b.append('}');
        }
        b.append("]}");
        return b.toString();
    }

    private static void appendString(StringBuilder b, String key, String value, boolean first) {
        if (!first) {
            b.append(',');
        }
        b.append('"').append(key).append("\":");
        if (value == null) {
            b.append("null");
            return;
        }
        b.append('"');
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            b.append('0');
                        }
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static List<Frame> capFrames(List<Frame> in) {
        if (in == null) {
            return new ArrayList<Frame>(0);
        }
        if (in.size() <= MAX_FRAMES) {
            return in;
        }
        return new ArrayList<Frame>(in.subList(0, MAX_FRAMES));
    }
}
