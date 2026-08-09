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
    void messageLineStartingWithAtIsNotAFrame() {
        // printStackTrace can wrap a message onto a line that begins with "at " but carries no frame
        // location; its long id must still be scrubbed. A real JVM frame below keeps its line number.
        String stack = "java.lang.RuntimeException: bad\n"
                + "at account 123456 failed to load\n"
                + "\tat com.foo.Bar.baz(Bar.java:4242)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account [num] failed") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("Bar.java:4242") >= 0, scrubbed);
    }

    @Test
    void messageWithIncidentalParenthesesIsNotAFrame() {
        // A message wrapped onto an "at ..." line can carry incidental parentheses that are not a frame
        // location; its id must still be scrubbed. Real parenthesized locations below survive.
        String stack = "java.lang.RuntimeException: bad\n"
                + "at account 123456 failed (retry)\n"
                + "\tat com.foo.Bar.baz(Bar.java:4242)\n"
                + "\tat com.foo.Qux.run(Native Method)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account [num] failed (retry)") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("Bar.java:4242") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("(Native Method)") >= 0, scrubbed);
    }

    @Test
    void messageWithAtPrefixAndColonNumberIsNotAFrame() {
        // A wrapped message can start with "at " AND end in a colon-number or carry a parenthetical
        // location, yet its identity has spaces, so it is not a frame and its id must be scrubbed.
        String stack = "java.lang.RuntimeException: bad\n"
                + "at account 123456 failed:789\n"
                + "at account 654321 failed (token:12)\n"
                + "\tat com.foo.Bar.baz(Bar.java:4242)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account [num] failed:789") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("654321") < 0, scrubbed);
        // The real frame keeps its coordinate.
        assertTrue(scrubbed.indexOf("Bar.java:4242") >= 0, scrubbed);
    }

    @Test
    void messageWithWhitespaceFreeIdentityIsNotAFrame() {
        // A wrapped message with no spaces around its id ("at account123456failed:789") is not a frame:
        // the bare colon-number form requires a dotted class.method or a URL, and a bare word/word+digits
        // paren location ("(attempt:123456)") is not a real location either.
        String stack = "java.lang.RuntimeException: bad\n"
                + "at account123456failed:789\n"
                + "at retry (attempt:654321)\n"
                + "\tat com.foo.Bar.baz(Bar.java:4242)\n"
                + "    at com.foo.Bar.qux:998877\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account[num]failed:789") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("attempt:[num]") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("654321") < 0, scrubbed);
        // Real frames (dotted identity) keep their coordinates.
        assertTrue(scrubbed.indexOf("Bar.java:4242") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("Bar.qux:998877") >= 0, scrubbed);
    }

    @Test
    void frameUrlQueryDataIsScrubbedButCoordinateSurvives() {
        // A JS frame URL can carry user data in its query; the terminal line:column coordinate must
        // survive for symbolication, but the identity/URL/query before it still gets scrubbed.
        String stack = "TypeError: boom\n"
                + "    at f (https://host/app.js?account=123456:1:42)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account=[num]") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf(":1:42)") >= 0, scrubbed);
    }

    @Test
    void coordinateFreeFrameMimicIsScrubbed() {
        // A message mimicking a coordinate-free frame ("(Native Method)"/"(Unknown Source)") has no
        // coordinate to protect, so the whole line is scrubbed; a real such frame's dotted identity has
        // no long digit run and is unchanged.
        String stack = "java.lang.RuntimeException: bad\n"
                + "at account123456failed (Native Method)\n"
                + "at other654321thing (Unknown Source)\n"
                + "\tat com.foo.Bar.baz(Native Method)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account[num]failed") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("654321") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("com.foo.Bar.baz(Native Method)") >= 0, scrubbed);
    }

    @Test
    void onlyTheTerminalTwoCoordinateGroupsArePreserved() {
        // A frame URL can carry colon-delimited user data before the real :line:column, e.g.
        // host/account:123456:1:42. Only the terminal two numeric groups (:1:42) are the coordinate;
        // the earlier :123456 is data and must be scrubbed.
        String stack = "TypeError: boom\n"
                + "    at f (https://host/account:123456:1:42)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("account:[num]:1:42)") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
    }

    @Test
    void customScrubMessageOverrideReachesRawStack() {
        // An app that redacts an app-specific token by overriding scrubMessage must have it redacted
        // in rawStack too, not only in the separately-scrubbed message. Frame lines stay untouched by
        // the override so coordinates survive.
        PiiScrubber custom = new PiiScrubber() {
            public String scrubMessage(String message) {
                if (message == null) {
                    return null;
                }
                return super.scrubMessage(message).replace("SECRET", "[redacted]");
            }
        };
        String stack = "java.lang.RuntimeException: token SECRET rejected\n"
                + "    at run (http://host/app.js:1:98765)\n";
        String scrubbed = custom.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("[redacted]") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("SECRET") < 0, scrubbed);
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

    @Test
    void unindentedFrameShapedMessageContinuationIsScrubbed() {
        // A message that wraps onto a line matching the frame grammar EXACTLY -- a dotted identity and a
        // (File.java:line) location -- is still a message, not a frame: printStackTrace emits it at column
        // 0 while real frames are indented. Its six-digit tail must be scrubbed, not preserved as a line
        // number. The genuine indented frame below keeps its coordinate.
        String stack = "java.lang.RuntimeException: bad\n"
                + "at account.failed(File.java:123456)\n"
                + "    at com.foo.Bar.baz(Bar.java:42)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("Bar.java:42") >= 0, scrubbed);
    }

    @Test
    void atSignMessageWithoutUrlSourceIsScrubbed() {
        // A wrapped message that merely contains an '@' and ends in two numeric groups
        // (status@host:1:123456) is NOT a Firefox/Safari frame: its source `host` is neither a URL nor
        // a file, so the six-digit tail must be scrubbed rather than preserved as a fake column. A real
        // Firefox frame (fn@http://host/app.js:10:5) whose source IS a URL keeps its coordinate.
        String stack = "java.lang.RuntimeException: verifying\n"
                + "status@host:1:123456\n"
                + "renderApp@http://host/app.js:10:5\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("app.js:10:5") >= 0, scrubbed);
    }

    @Test
    void scrubFrameOverrideRedactsSyntheticMethodNameInRawStack() {
        // An app that overrides scrubFrame to strip PII from a synthetic method name must have that
        // redaction applied to the raw stack too, not only to the structured frames -- else the raw copy
        // reintroduces the value the app explicitly removed. The frame's coordinate is still preserved.
        PiiScrubber custom = new PiiScrubber() {
            public String scrubFrame(String className, String methodName) {
                return methodName.replace("secret", "[redacted]");
            }
        };
        String stack = "java.lang.RuntimeException: boom\n"
                + "    at com.foo.Bar.secretMethod(Bar.java:42)\n";
        String scrubbed = custom.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("secretMethod") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("[redacted]Method") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("Bar.java:42") >= 0, scrubbed);
    }

    @Test
    void indentedMessageContinuationWithArbitraryLabelIsScrubbed() {
        // An INDENTED message continuation (the message itself contains "\n    at ...") whose text happens
        // to look like a frame -- an arbitrary multi-word label plus a (File.java:line) location -- must
        // still be scrubbed: "account failed" is not a V8 label shape (async/new/bound/get/set/[as]), so
        // its six-digit tail is masked, not preserved. A genuine V8 async frame below keeps its coordinate.
        String stack = "java.lang.RuntimeException: bad\n"
                + "    at account failed (File.java:123456)\n"
                + "    at async run (https://host/app.js:2:98765)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("app.js:2:98765") >= 0, scrubbed);
    }

    @Test
    void v8AccessorAliasWithFreeTextIsScrubbed() {
        // A real V8 accessor alias is a single property name ([as bar]); an indented continuation that
        // hides free text inside the brackets -- "    at account [as failed message] (File.java:123456)"
        // -- is not a frame, so its numeric tail must be scrubbed. A genuine accessor frame below (single
        // token alias) keeps its coordinate.
        String stack = "java.lang.RuntimeException: bad\n"
                + "    at account [as failed message] (File.java:123456)\n"
                + "    at handler [as onClick] (https://host/app.js:3:98765)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("app.js:3:98765") >= 0, scrubbed);
    }

    @Test
    void v8AsyncFrameWithMultiWordLabelKeepsItsCoordinate() {
        // V8 labels async/constructor/accessor frames with spaces ("async load", "new Promise",
        // "Object.x [as y]"). Such an INDENTED frame with a real (url:line:col) is a genuine frame -- its
        // minified column must survive for source-map symbolication, not be masked to [num]. An unindented
        // look-alike message is still rejected by the indentation gate.
        String stack = "Error: boom\n"
                + "    at async load (https://host/app.js:1:123456)\n"
                + "    at new Promise (https://host/app.js:2:98765)\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("app.js:1:123456") >= 0, scrubbed);
        assertTrue(scrubbed.indexOf("app.js:2:98765") >= 0, scrubbed);
    }

    @Test
    void atSignMessageWithDottedHostButNoPathIsScrubbed() {
        // An email-shaped continuation (status@host.com:1:123456) has a DOTTED domain but no URL path,
        // so it is not a Firefox frame -- a real script source is always a URL with a '/'. Its six-digit
        // tail must be scrubbed, not preserved as a fake column. The genuine URL-sourced frame below,
        // which carries a path separator, keeps its coordinate.
        String stack = "java.lang.RuntimeException: verifying\n"
                + "status@host.com:1:123456\n"
                + "renderApp@http://host/app.js:10:5\n";
        String scrubbed = scrubber.scrubRawStack(stack);
        assertTrue(scrubbed.indexOf("123456") < 0, scrubbed);
        assertTrue(scrubbed.indexOf("app.js:10:5") >= 0, scrubbed);
    }
}
