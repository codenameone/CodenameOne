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
package com.codename1.surfaces;

import com.codename1.io.JSONParser;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The wire-format facts a Wear complication and Tile reader depends on.
///
/// Both readers live in the Android port and cannot be unit tested here, and both got these
/// wrong: they looked for container children under `c` and for a dynamic node's value under
/// `text`. Neither mistake fails to compile and neither throws -- a complication just renders
/// empty, which is indistinguishable from an app that published nothing. Pinning the field names
/// against the serializer is what makes that kind of drift visible.
class SurfaceWatchWireFormatTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) throws Exception {
        return new JSONParser().parseJSON(new StringReader(json));
    }

    private static Map<String, Object> serializeRoot(SurfaceNode root) throws Exception {
        Map<String, byte[]> images = new HashMap<String, byte[]>();
        String json = SurfaceSerializer.serializeTimeline("k",
                new WidgetTimeline().setContent(root), images);
        Map<String, Object> doc = parse(json);
        Map<String, Object> layouts = (Map<String, Object>) doc.get("layouts");
        return (Map<String, Object>) layouts.get("default");
    }

    /// A container's children are `ch`. A reader looking for `c` finds an empty container and
    /// mines a layout with no text, no progress and no imagery in it.
    @Test
    @SuppressWarnings("unchecked")
    void containersSerializeTheirChildrenUnderCh() throws Exception {
        Map<String, Object> root = serializeRoot(new SurfaceColumn()
                .add(new SurfaceText("first"))
                .add(new SurfaceText("second")));

        assertEquals("col", root.get("t"));
        assertTrue(root.get("ch") instanceof List, "children belong under \"ch\": " + root);
        assertEquals(2, ((List<Object>) root.get("ch")).size());
        assertTrue(root.get("c") == null, "nothing is published under \"c\": " + root);
    }

    /// Rows and boxes use the same key, so a reader that special-cases one of the three is wrong
    /// about the other two.
    @Test
    void everyContainerKindUsesTheSameChildrenKey() throws Exception {
        assertNotNull(serializeRoot(new SurfaceRow().add(new SurfaceText("x"))).get("ch"));
        assertNotNull(serializeRoot(new SurfaceBox().add(new SurfaceText("x"))).get("ch"));
        assertNotNull(serializeRoot(new SurfaceColumn().add(new SurfaceText("x"))).get("ch"));
    }

    /// A dynamic node carries a style and a date, never a `text` field. A reader interpolating
    /// `text` gets an empty string and the countdown silently disappears.
    @Test
    void dynamicTextSerializesAStyleAndADateRatherThanText() throws Exception {
        Map<String, Object> root = serializeRoot(
                new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN,
                        new java.util.Date(1700000000000L)));

        assertEquals("dyn", root.get("t"));
        assertEquals("timerDown", root.get("style"));
        assertNotNull(root.get("date"), "a literal date belongs under \"date\": " + root);
        assertTrue(root.get("text") == null, "a dyn node publishes no \"text\": " + root);
    }

    /// The state-driven form names its key instead, which a reader has to resolve against the
    /// entry rather than reading a literal.
    @Test
    void aStateDrivenDynamicNodeNamesItsKey() throws Exception {
        Map<String, Object> root = serializeRoot(
                new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN, "deadline"));

        assertEquals("deadline", root.get("dateKey"));
        assertTrue(root.get("text") == null, "a dyn node publishes no \"text\": " + root);
    }

    /// The formatter a surface without a native ticking widget uses. Public so the Wear readers
    /// share it rather than each formatting for themselves -- a countdown has to read the same on
    /// a watch face as in the simulator preview.
    @Test
    void theSharedFormatterCoversEveryStyle() {
        long now = 1700000000000L;
        assertEquals("1:00", SurfaceRasterizer.formatDynamicText("timerDown", now + 60000, now));
        assertEquals("1:00", SurfaceRasterizer.formatDynamicText("timerUp", now - 60000, now));
        assertNotNull(SurfaceRasterizer.formatDynamicText("time", now, now));
        assertNotNull(SurfaceRasterizer.formatDynamicText("date", now, now));
        assertNotNull(SurfaceRasterizer.formatDynamicText("relative", now - 3600000, now));
    }
}
