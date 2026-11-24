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
package com.codename1.mcp;

import com.codename1.io.JSONParser;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// The agent clock tools move frozen animation time by exactly what they are asked,
/// settle a still form, and refuse to advance a clock that is not frozen.
class McpClockTest extends UITestBase {

    @FormTest
    void clockToolsFreezeAdvanceExactlyAndRelease() throws Exception {
        Form form = new Form("Clock", new BorderLayout());
        form.show();
        DisplayTest.flushEdt();
        final MCPServer server = new MCPServer();
        final Throwable[] failure = new Throwable[1];
        try {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    try {
                        Map<String, Object> frozen = call(server, "ui_clock_freeze", "{}");
                        assertTrue(bool(frozen.get("frozen")));
                        long t0 = ((Number) frozen.get("timeMs")).longValue();
                        Thread.sleep(100);
                        Map<String, Object> adv = call(server, "ui_clock_advance", "{\"ms\":40}");
                        assertEquals(t0 + 40, ((Number) adv.get("timeMs")).longValue(),
                                "advance moves the clock by exactly what was asked, whatever the wall clock did");
                        Map<String, Object> settled = call(server, "ui_clock_settle", "{\"maxMs\":600}");
                        assertTrue(bool(settled.get("settled")), "a still form settles");
                        Map<String, Object> released = call(server, "ui_clock_release", "{}");
                        assertFalse(bool(released.get("frozen")));
                    } catch (Throwable ex) {
                        failure[0] = ex;
                    }
                }
            });
        } finally {
            AnimationTime.reset();
        }
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    @FormTest
    void aFrozenTransitionAnswersWithAdviceAndSettleFinishesIt() throws Exception {
        final Form first = new Form("First", new BorderLayout());
        final Form second = new Form("Second screen", new BorderLayout());
        first.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 400));
        Button go = new Button("Go next");
        go.addActionListener(e -> second.show());
        first.add(BorderLayout.CENTER, go);
        first.show();
        DisplayTest.flushEdt();
        final MCPServer server = new MCPServer();
        final Throwable[] failure = new Throwable[1];
        try {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    try {
                        call(server, "ui_clock_freeze", "{}");
                        String found = text(server, "ui_find", "{\"label\":\"Go next\"}");
                        long id = ((Number) ((Map<?, ?>) ((List<?>) JSONParser.parseJSON(
                                "{\"r\":" + found + "}").get("r")).get(0)).get("id")).longValue();
                        call(server, "ui_activate", "{\"nodeId\":" + id + "}");
                        String blocked = server.handleMessage(request("ui_snapshot", "{}"));
                        assertTrue(blocked.contains("ui_clock_settle"), blocked);
                        Map<String, Object> settled = call(server, "ui_clock_settle", "{\"maxMs\":3000}");
                        assertTrue(bool(settled.get("settled")), String.valueOf(settled));
                        assertTrue(text(server, "ui_snapshot", "{}").contains("Second screen"),
                                "settling runs the 400 ms transition to its end");
                    } catch (Throwable ex) {
                        failure[0] = ex;
                    }
                }
            });
        } finally {
            AnimationTime.reset();
        }
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    @FormTest
    void advanceWithoutFreezeIsAnError() throws Exception {
        final MCPServer server = new MCPServer();
        final String[] reply = new String[1];
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                reply[0] = server.handleMessage(request("ui_clock_advance", "{\"ms\":10}"));
            }
        });
        assertTrue(reply[0].contains("ui_clock_freeze"), reply[0]);
    }

    private static String request(String name, String args) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"" + name + "\",\"arguments\":" + args + "}}";
    }

    @SuppressWarnings("unchecked")
    private static String text(MCPServer server, String name, String args) throws Exception {
        Map<String, Object> envelope = parse(server.handleMessage(request(name, args)));
        Map<String, Object> result = (Map<String, Object>) envelope.get("result");
        assertFalse(bool(result.get("isError")), String.valueOf(result));
        List<Object> content = (List<Object>) result.get("content");
        return (String) ((Map<String, Object>) content.get(0)).get("text");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> call(MCPServer server, String name, String args) throws Exception {
        Map<String, Object> envelope = parse(server.handleMessage(request(name, args)));
        Map<String, Object> result = (Map<String, Object>) envelope.get("result");
        assertFalse(bool(result.get("isError")), String.valueOf(result));
        List<Object> content = (List<Object>) result.get("content");
        return parse((String) ((Map<String, Object>) content.get(0)).get("text"));
    }

    /// The CN1 JSON parser returns booleans as strings by default, so accept either.
    private static boolean bool(Object o) {
        return o instanceof Boolean ? ((Boolean) o).booleanValue() : "true".equals(String.valueOf(o));
    }

    private static Map<String, Object> parse(String json) throws Exception {
        return JSONParser.parseJSON(json);
    }
}
