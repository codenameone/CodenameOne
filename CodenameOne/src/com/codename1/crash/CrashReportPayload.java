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
    /// Hard cap on the raw (pre-rendered) Java stack string captured via
    /// `printStackTrace` -- the verbatim platform rendering plus the cause
    /// chain. It complements the structured {@link #frames} (populated on
    /// every port), and on the JS port, where there are no structured
    /// frames, it carries the JavaScript engine stack. Mirrors
    /// {@link #MAX_NATIVE_STACK_LEN}.
    static final int MAX_RAW_STACK_LEN = 16 * 1024;

    /// Trace-format discriminator values. Tells the server how to parse
    /// {@link #rawStack} for this build.
    static final String TRACE_STRUCTURED = "structured";
    static final String TRACE_PARPARVM = "parparvm-text";
    static final String TRACE_JS = "js-error";
    static final String TRACE_NONE = "none";

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
    /// The pre-rendered Java stack captured via `printStackTrace`, which
    /// works identically on every port. On the ParparVM C targets this is
    /// the only readable Java trace once obfuscated; the server parses it
    /// with the mapping. `null` when no stack was available.
    final String rawStack;
    /// One of {@link #TRACE_STRUCTURED}, {@link #TRACE_PARPARVM},
    /// {@link #TRACE_JS} or {@link #TRACE_NONE}: how the server should read
    /// {@link #rawStack}. Derived, never guessed.
    final String traceFormat;
    /// SHA-256 of the obfuscation mapping this build was hardened with,
    /// stamped into the app so a report can be tied to the exact mapping.
    /// Empty for unhardened builds.
    final String mappingId;
    /// The hardening level the build shipped with (`off` / `standard` /
    /// `aggressive` / `paranoid`); lets the server answer "why can't I
    /// retrace this?" with the honest reason.
    final String hardenLevel;
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
            String nativeLog, String nativeStack, String rawStack) {
        this.eventId = eventId;
        this.exceptionClass = exceptionClass;
        this.messageScrubbed = trim(messageScrubbed, MAX_MESSAGE_LEN);
        this.frames = capFrames(frames);
        this.nativeLog = trim(nativeLog, MAX_NATIVE_LOG_LEN);
        this.nativeStack = trim(nativeStack, MAX_NATIVE_STACK_LEN);
        this.rawStack = trim(rawStack, MAX_RAW_STACK_LEN);
        Display d = Display.getInstance();
        this.platform = d.getPlatformName();
        this.traceFormat = deriveTraceFormat(this.frames, this.rawStack, this.platform);
        this.buildKey = d.getProperty("build_key", "");
        this.packageName = d.getProperty("package_name", "");
        this.appName = d.getProperty("AppName", "");
        this.appVersion = d.getProperty("AppVersion", "");
        this.osVersion = d.getProperty("OSVer", "");
        this.mappingId = d.getProperty("cn1.mappingId", "");
        this.hardenLevel = d.getProperty("cn1.hardenLevel", "");
        Locale loc = Locale.getDefault();
        this.locale = loc == null ? "" : loc.toString();
        this.clientTs = System.currentTimeMillis();
    }

    /// Derives the trace format from what we actually have, so the server picks the right parser --
    /// never a guess. Structured frames win. Otherwise: the JavaScript port's raw stack is a JS engine
    /// stack; a ParparVM C target's is the "    at <fqcn>.<method>:<line>" text; a JVM target
    /// (Android/desktop) reaching here has an ordinary JVM printStackTrace (e.g. a stackless throwable
    /// whose only frames are in its cause) that the JS parser must NOT touch. The old heuristic looked
    /// only for the 4-space ParparVM shape and labeled everything else -- including the tab-indented
    /// JVM trace -- as JavaScript; derive from the platform so that never happens.
    static String deriveTraceFormat(List<Frame> frames, String rawStack, String platform) {
        if (frames != null && !frames.isEmpty()) {
            return TRACE_STRUCTURED;
        }
        if (rawStack == null || rawStack.length() == 0) {
            return TRACE_NONE;
        }
        if (isJavaScriptPlatform(platform)) {
            return TRACE_JS;
        }
        // A ParparVM frame line is exactly "    at <fqcn>.<method>:<line>" -- no '(', URL or '@'.
        int at = rawStack.indexOf("    at ");
        if (at >= 0) {
            int lineEnd = rawStack.indexOf('\n', at);
            String body = lineEnd < 0 ? rawStack.substring(at + 7) : rawStack.substring(at + 7, lineEnd);
            if (body.indexOf('(') < 0 && body.indexOf('/') < 0 && body.indexOf('@') < 0) {
                return TRACE_PARPARVM;
            }
        }
        // Not JavaScript and not the ParparVM text shape: an ordinary JVM printStackTrace body. There
        // is no JVM raw parser, so report NONE and let the server keep the text verbatim rather than
        // misparsing it as a JavaScript stack.
        return TRACE_NONE;
    }

    /// The JavaScript port's platform name; its raw stack is a JS engine {@code Error().stack}.
    private static boolean isJavaScriptPlatform(String platform) {
        if (platform == null) {
            return false;
        }
        String p = platform.toLowerCase();
        return p.indexOf("html") >= 0 || p.indexOf("javascript") >= 0 || "js".equals(p);
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
        appendString(b, "rawStack", rawStack, false);
        appendString(b, "traceFormat", traceFormat, false);
        appendString(b, "mappingId", mappingId, false);
        appendString(b, "hardenLevel", hardenLevel, false);
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
