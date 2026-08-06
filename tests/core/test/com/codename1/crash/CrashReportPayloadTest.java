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

import com.codename1.testing.AbstractTest;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies the crash payload's trace-format discriminator and that the hardening /
 * raw-stack fields are emitted in the JSON. The discriminator is what tells the
 * server how to parse the raw stack, so getting it exactly right matters -- a V8
 * JavaScript stack that happens to contain "    at " must NOT be mistaken for the
 * ParparVM text format.
 */
public class CrashReportPayloadTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return false;
    }

    private CrashReportPayload payload(List<CrashReportPayload.Frame> frames, String rawStack) {
        return new CrashReportPayload("evt", "java.lang.NullPointerException",
                "boom", frames, null, null, rawStack);
    }

    @Override
    public boolean runTest() throws Exception {
        List<CrashReportPayload.Frame> empty = new ArrayList<CrashReportPayload.Frame>();

        // Structured frames present -> "structured".
        List<CrashReportPayload.Frame> withFrame = new ArrayList<CrashReportPayload.Frame>();
        withFrame.add(new CrashReportPayload.Frame("com.example.A", "b", "A.java", 5, false));
        assertTrue(payload(withFrame, null).traceFormat.equals(CrashReportPayload.TRACE_STRUCTURED),
                "frames present should be structured");

        // No frames, ParparVM text raw stack -> "parparvm-text".
        String parpar = "java.lang.NullPointerException\n"
                + "    at com.example.MyForm.onClick:142\n";
        assertTrue(payload(empty, parpar).traceFormat.equals(CrashReportPayload.TRACE_PARPARVM),
                "parparvm text should be detected");

        // No frames, V8 JS stack (has "    at " but with parens/URL) -> "js-error", NOT parparvm.
        String v8 = "Error: boom\n    at onClick (http://localhost/app.js:100:5)\n";
        assertTrue(payload(empty, v8).traceFormat.equals(CrashReportPayload.TRACE_JS),
                "a V8 stack must not be mistaken for parparvm-text");

        // SpiderMonkey JS stack (uses '@') -> "js-error".
        String sm = "onClick@http://localhost/app.js:100:5\n";
        assertTrue(payload(empty, sm).traceFormat.equals(CrashReportPayload.TRACE_JS),
                "a SpiderMonkey stack is js-error");

        // Nothing at all -> "none".
        assertTrue(payload(empty, null).traceFormat.equals(CrashReportPayload.TRACE_NONE),
                "no frames and no raw stack is none");

        // JSON carries the new fields.
        String json = payload(empty, parpar).toJson();
        assertTrue(json.contains("\"traceFormat\":\"parparvm-text\""), "traceFormat in json: " + json);
        assertTrue(json.contains("\"rawStack\":"), "rawStack in json");
        assertTrue(json.contains("\"mappingId\":"), "mappingId in json");
        assertTrue(json.contains("\"hardenLevel\":"), "hardenLevel in json");

        // Raw stack is capped.
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < CrashReportPayload.MAX_RAW_STACK_LEN + 5000; i++) {
            big.append('x');
        }
        CrashReportPayload capped = payload(empty, big.toString());
        assertTrue(capped.rawStack.length() == CrashReportPayload.MAX_RAW_STACK_LEN,
                "raw stack capped to max length");

        return true;
    }
}
