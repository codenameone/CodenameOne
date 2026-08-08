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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** scrubRawStack keeps frame-line coordinates while still scrubbing message PII. */
class PiiScrubberRawStackTest {

    private final PiiScrubber scrubber = new PiiScrubber();

    @Test
    void nullPassesThrough() {
        assertEquals(null, scrubber.scrubRawStack(null));
    }

    @Test
    void javaScriptColumnOffsetsSurvive() {
        // A minified bundle is one line, so the column offset runs to six-plus digits. It is the
        // location the js-error parser needs, so it must not be masked to [num].
        String stack = "TypeError: undefined is not a function\n"
                + "    at run (http://host/app.js:1:123456)\n"
                + "    at go (http://host/app.js:1:98765)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("app.js:1:123456") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("[num]") < 0, scrubbed);
    }

    @Test
    void firefoxFramesSurvive() {
        String stack = "Error: boom\nrun@http://host/app.js:1:123456\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("app.js:1:123456") >= 0, scrubbed);
    }

    @Test
    void parparVmLineNumbersSurvive() {
        String stack = "    at com.foo.Bar.baz:123456\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("baz:123456") >= 0, scrubbed);
    }

    @Test
    void messageMentioningAFileIsNotAFrame() {
        // A free-form message can mention a file and an id; it is not a frame just because it contains
        // ".js:", so its long id must still be masked. (No terminal :line:column, no leading "at ".)
        String stack = "Error: account 123456 failed in app.js: retry\n"
                + "    at run (http://host/app.js:1:98765)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account [num] failed") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        // ...while the real frame below keeps its coordinate.
        assertTrue(scrubbed.indexOf("app.js:1:98765") >= 0, scrubbed);
    }

    @Test
    void messageDigitsAndEmailsStillScrubbed() {
        // The leading message line is free-form and can carry PII: a long id/phone is masked and an
        // email is partially redacted, even though frame coordinates below are preserved.
        String stack = "java.lang.RuntimeException: user 5551234567 test@example.com\n"
                + "    at com.foo.Bar.baz(Bar.java:42)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("[num]") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("5551234567") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("tes***@example.com") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("Bar.java:42") >= 0, scrubbed);
    }
}
