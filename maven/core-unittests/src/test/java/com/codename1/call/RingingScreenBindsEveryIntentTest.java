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
package com.codename1.call;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ringing screen draws the call it is about to answer.
 *
 * <p>The activity is {@code singleTop}, so a second incoming call while one
 * is ringing reuses the instance rather than stacking another. That makes the
 * redelivery a real path, and it used to assign the new call id without
 * redrawing: the caller line still named whoever {@code onCreate} had drawn
 * while Answer and Decline acted on the new call. A screen that says one name
 * and answers a different call is worse than either mistake alone, because
 * the user cannot see that anything is wrong.</p>
 *
 * <p>Read from source because an Activity cannot run in this module -- there
 * is no Android framework here. That makes this a shape check, and it is
 * narrow on purpose: it asserts that both entry points go through one
 * binding method, which is the single decision that was wrong.</p>
 *
 * <p>Skipped rather than failed when the port is not in the tree, which is
 * how this module is built in some checkouts; CI has the whole
 * repository.</p>
 */
class RingingScreenBindsEveryIntentTest {

    private static final String ACTIVITY =
            "../../Ports/Android/src/com/codename1/impl/android/call/"
            + "CN1IncomingCallActivity.java";

    static boolean portPresent() {
        return new File(ACTIVITY).exists();
    }

    private static String source() throws Exception {
        return new String(Files.readAllBytes(new File(ACTIVITY).toPath()),
                StandardCharsets.UTF_8);
    }

    private static String body(String src, String signature) {
        int at = src.indexOf(signature);
        assertTrue(at >= 0, signature + " has to exist");
        int end = src.indexOf("\n    }\n", at);
        assertTrue(end > at, signature + " has to end");
        StringBuilder sb = new StringBuilder();
        for (String line : src.substring(at, end).split("\n", -1)) {
            if (!line.trim().startsWith("//") && !line.trim().startsWith("///")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    @Test
    @EnabledIf("portPresent")
    void bothEntryPointsBindThroughOneMethod() throws Exception {
        String src = source();
        assertTrue(body(src, "protected void onCreate(").contains("bindTo("),
                "the first intent binds through the shared path");
        assertTrue(body(src, "protected void onNewIntent(")
                        .contains("bindTo("),
                "and so does a singleTop redelivery, or the screen shows one"
                + " call and answers another");
        assertFalse(body(src, "protected void onNewIntent(")
                        .contains("callId ="),
                "the redelivery must not assign the id itself; that is how it"
                + " drifted from what is drawn");
    }

    @Test
    @EnabledIf("portPresent")
    void bindingRedrawsAndRefusesADeadCall() throws Exception {
        String bind = body(source(), "private void bindTo(");
        assertTrue(bind.contains("setContentView("),
                "binding has to redraw, or the caller line is stale");
        assertTrue(bind.contains("finish()"),
                "and refuse a call that is already gone, on every bind rather"
                + " than only the first");
        int assign = bind.indexOf("callId = id");
        int draw = bind.indexOf("setContentView(");
        assertTrue(assign >= 0 && assign < draw,
                "the id is set before the draw that reads it back");
    }
}
