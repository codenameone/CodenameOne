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
package com.codename1.retrace;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the pre-rendered stack string ParparVM produces for a {@code Throwable}
 * on the C targets (iOS, tvOS, watchOS, mac-native, win32, linux). The format,
 * emitted by {@code java_lang_Throwable_getStack} in the translator's
 * {@code nativeMethods.m}, is:
 *
 * <pre>
 * &lt;throwable class name&gt;
 *     at &lt;fqcn&gt;.&lt;method&gt;:&lt;line&gt;
 *     at &lt;fqcn&gt;.&lt;method&gt;:&lt;line&gt;
 *     ...
 * </pre>
 *
 * <p>This is the canonical, unit-tested reference for that grammar. The on-device
 * {@code java.lang.Throwable.getStackTrace()} in {@code vm/JavaAPI} carries a
 * hand-inlined copy of the same logic (it cannot depend on this module), so the
 * two must stay in lockstep -- change them together and keep this class's tests
 * green.
 *
 * <p>On the ParparVM JavaScript port the same {@code stack} field instead holds a
 * JavaScript engine's {@code Error().stack}, whose frames carry {@code '('},
 * {@code '/'} or {@code '@'} -- characters a Java class or method name never
 * contains. The parser rejects the whole trace in that case (returning no frames)
 * rather than fabricate bogus frames from a foreign format. Parsing is
 * {@code indexOf}-based and never throws: on device this code runs while another
 * failure is already being reported.
 */
public final class ParparVmTraceParser {

    private ParparVmTraceParser() {
    }

    /**
     * Parses a ParparVM trace string into structured frames. Returns an empty
     * list for {@code null}/empty input, a header-only trace, or any input that
     * is not the ParparVM text format (e.g. a JavaScript {@code Error().stack}).
     */
    public static List<Frame> parse(String stack) {
        List<Frame> frames = new ArrayList<Frame>();
        if (stack == null || stack.length() == 0) {
            return frames;
        }
        int pos = 0;
        int len = stack.length();
        while (pos < len) {
            String line;
            int nl = stack.indexOf('\n', pos);
            if (nl < 0) {
                line = stack.substring(pos);
                pos = len;
            } else {
                line = stack.substring(pos, nl);
                pos = nl + 1;
            }
            // Only "    at ..." lines are frames; the class-name header and blank
            // lines are skipped.
            if (line.indexOf("    at ") != 0) {
                continue;
            }
            String body = line.substring(7);
            // Any of these characters means the trace is a JavaScript Error().stack
            // (URLs, parentheses, or '@'), not the ParparVM text format. Bail on the
            // whole trace rather than emit a made-up frame.
            if (body.indexOf('(') >= 0 || body.indexOf('/') >= 0
                    || body.indexOf('@') >= 0 || body.indexOf(' ') >= 0) {
                return new ArrayList<Frame>();
            }
            int colon = body.lastIndexOf(':');
            if (colon < 0) {
                continue;
            }
            int dot = body.lastIndexOf('.', colon - 1);
            if (dot < 0) {
                continue;
            }
            String cls = body.substring(0, dot);
            String method = body.substring(dot + 1, colon);
            if (cls.length() == 0 || method.length() == 0) {
                continue;
            }
            int lineNumber = parseLineNumber(body, colon + 1);
            // Synthesize a source file from the simple class name so the frame is
            // not flagged native (fileName == null). ParparVM does not carry the
            // original source file, so this is best-effort, not authoritative.
            String fileName = simpleClassName(cls) + ".java";
            frames.add(new Frame(cls, method, fileName, lineNumber));
        }
        return frames;
    }

    static int parseLineNumber(String s, int from) {
        int len = s.length();
        int i = from;
        boolean negative = false;
        if (i < len && s.charAt(i) == '-') {
            negative = true;
            i++;
        }
        int value = 0;
        boolean any = false;
        for (; i < len; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }
            value = value * 10 + (c - '0');
            any = true;
        }
        if (!any) {
            return -1;
        }
        return negative ? -value : value;
    }

    static String simpleClassName(String fqcn) {
        int d = fqcn.lastIndexOf('.');
        String simple = d < 0 ? fqcn : fqcn.substring(d + 1);
        int dollar = simple.indexOf('$');
        if (dollar > 0) {
            simple = simple.substring(0, dollar);
        }
        return simple;
    }
}
