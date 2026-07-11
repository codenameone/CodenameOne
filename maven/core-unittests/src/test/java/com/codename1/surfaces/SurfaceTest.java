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
import com.codename1.surfaces.spi.SurfaceBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Platform-independent coverage for the portable com.codename1.surfaces runtime: descriptor
 * serialization round-trips, timeline ordering, image registry dedup, live activity lifecycle
 * against a fake {@link SurfaceBridge} and the action dispatch cold-start queue. Needs no
 * platform Display.
 */
class SurfaceTest {

    /** Records bridge calls so publishing and live activity behaviour can be asserted. */
    private static final class FakeBridge implements SurfaceBridge {
        boolean widgetsSupported = true;
        boolean activitiesSupported = true;
        String publishedKind;
        String publishedJson;
        Map<String, byte[]> publishedImages;
        final List<String> registeredKinds = new ArrayList<String>();
        String reloadedKind = "unset";
        String startedJson;
        final List<String> updates = new ArrayList<String>();
        String endedId;
        String endedFinalState;
        boolean endedImmediately;
        int nextActivityId = 1;

        public boolean areWidgetsSupported() {
            return widgetsSupported;
        }

        public boolean isLiveActivitySupported() {
            return activitiesSupported;
        }

        public void registerWidgetKind(String kindJson) {
            registeredKinds.add(kindJson);
        }

        public void publishWidgetTimeline(String kindId, String timelineJson,
                Map<String, byte[]> images) {
            publishedKind = kindId;
            publishedJson = timelineJson;
            publishedImages = images;
        }

        public void reloadWidgets(String kindId) {
            reloadedKind = kindId;
        }

        public int getInstalledWidgetCount(String kindId) {
            return 2;
        }

        public String startLiveActivity(String descriptorJson, Map<String, byte[]> images) {
            startedJson = descriptorJson;
            return "act" + (nextActivityId++);
        }

        public void updateLiveActivity(String activityId, String stateJson) {
            updates.add(activityId + ":" + stateJson);
        }

        public void endLiveActivity(String activityId, String finalStateJson,
                boolean dismissImmediately) {
            endedId = activityId;
            endedFinalState = finalStateJson;
            endedImmediately = dismissImmediately;
        }
    }

    // a tiny valid PNG header + payload; EncodedImage wraps bytes lazily so any bytes work here
    private static byte[] pngBytes(int seed) {
        byte[] b = new byte[64];
        b[0] = (byte) 0x89;
        b[1] = 'P';
        b[2] = 'N';
        b[3] = 'G';
        for (int i = 4; i < b.length; i++) {
            b[i] = (byte) (i * seed);
        }
        return b;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) throws Exception {
        return new JSONParser().parseJSON(new StringReader(json));
    }

    private static long asLong(Object o) {
        return ((Number) o).longValue();
    }

    @AfterEach
    void tearDown() {
        Surfaces.reset();
    }

    private SurfaceNode deliveryLayout(Map<String, byte[]> images) {
        String imgName = SurfaceSerializer.registerImageBytes(pngBytes(3), images);
        return new SurfaceRow().setSpacing(8).setPadding(8)
                .setBackground(SurfaceColor.rgb(0xffffffff, 0xff202020))
                .setCornerRadius(12)
                .setAction("openOrder", params("orderId", "A1029"))
                .add(new SurfaceImage(imgName).setSize(40, 40))
                .add(new SurfaceColumn().setWeight(1)
                        .add(new SurfaceText("${statusLabel}").setFontSize(13)
                                .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                                .setColor(SurfaceColor.SECONDARY_LABEL).setMaxLines(1))
                        .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
                                .setFontSize(22).setFontWeight(SurfaceFontWeight.BOLD))
                        .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                                .setValueState("progress").setColor(SurfaceColor.ACCENT)))
                .add(new SurfaceSpacer());
    }

    private static Map<String, Object> params(String k, Object v) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(k, v);
        return m;
    }

    @Test
    @SuppressWarnings("unchecked")
    void timelineSerializationRoundTrips() throws Exception {
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        WidgetTimeline t = new WidgetTimeline()
                .setContent(deliveryLayout(images))
                .addEntry(new Date(1760000000000L), stateMap("Out for delivery", 1760000900000L, 0.7))
                .addEntry(new Date(1760000900000L), stateMap("Arriving now", 1760000900000L, 1.0));
        String json = SurfaceSerializer.serializeTimeline("delivery_status", t, images);

        Map<String, Object> doc = parse(json);
        assertEquals(1, asLong(doc.get("v")));
        assertEquals("delivery_status", doc.get("kind"));
        assertEquals("atEnd", doc.get("reload"));

        Map<String, Object> layouts = (Map<String, Object>) doc.get("layouts");
        Map<String, Object> root = (Map<String, Object>) layouts.get("default");
        assertEquals("row", root.get("t"));
        assertEquals(8, asLong(root.get("spacing")));
        assertEquals(12, asLong(root.get("corner")));
        List<Object> pad = (List<Object>) root.get("pad");
        assertEquals(4, pad.size());
        assertEquals(8, asLong(pad.get(0)));
        Map<String, Object> bg = (Map<String, Object>) root.get("bg");
        assertEquals(0xffffffffL, asLong(bg.get("l")) & 0xffffffffL);
        assertEquals(0xff202020L, asLong(bg.get("d")) & 0xffffffffL);
        Map<String, Object> action = (Map<String, Object>) root.get("action");
        assertEquals("openOrder", action.get("id"));
        assertEquals("A1029", ((Map<String, Object>) action.get("p")).get("orderId"));

        List<Object> children = (List<Object>) root.get("ch");
        assertEquals(3, children.size());
        Map<String, Object> img = (Map<String, Object>) children.get(0);
        assertEquals("img", img.get("t"));
        String imgName = (String) img.get("name");
        assertTrue(imgName.startsWith("img"));
        assertTrue(images.containsKey(imgName));

        Map<String, Object> col = (Map<String, Object>) children.get(1);
        assertEquals("col", col.get("t"));
        assertEquals(1, asLong(col.get("weight")));
        List<Object> colCh = (List<Object>) col.get("ch");
        Map<String, Object> text = (Map<String, Object>) colCh.get(0);
        assertEquals("${statusLabel}", text.get("text"));
        assertEquals("semibold", text.get("fw"));
        assertEquals("secondaryLabel", ((Map<String, Object>) text.get("color")).get("role"));
        Map<String, Object> dyn = (Map<String, Object>) colCh.get(1);
        assertEquals("dyn", dyn.get("t"));
        assertEquals("timerDown", dyn.get("style"));
        assertEquals("eta", dyn.get("dateKey"));
        Map<String, Object> prog = (Map<String, Object>) colCh.get(2);
        assertEquals("linear", prog.get("style"));
        assertEquals("progress", prog.get("valueKey"));

        List<Object> entries = (List<Object>) doc.get("entries");
        assertEquals(2, entries.size());
        Map<String, Object> e0 = (Map<String, Object>) entries.get(0);
        assertEquals(1760000000000L, asLong(e0.get("date")));
        Map<String, Object> state = (Map<String, Object>) e0.get("state");
        assertEquals("Out for delivery", state.get("statusLabel"));
        assertEquals(1760000900000L, asLong(state.get("eta")));

        List<Object> imageNames = (List<Object>) doc.get("images");
        assertEquals(1, imageNames.size());
        assertEquals(imgName, imageNames.get(0));
    }

    private static Map<String, Object> stateMap(String status, long eta, double progress) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("statusLabel", status);
        m.put("eta", Long.valueOf(eta));
        m.put("progress", Double.valueOf(progress));
        return m;
    }

    @Test
    @SuppressWarnings("unchecked")
    void timelineEntriesAreSortedAndEmptyTimelineGetsImplicitEntry() throws Exception {
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        WidgetTimeline t = new WidgetTimeline().setContent(new SurfaceText("x"))
                .addEntry(new Date(300L), null)
                .addEntry(new Date(100L), null)
                .addEntry(new Date(200L), null);
        Map<String, Object> doc = parse(SurfaceSerializer.serializeTimeline("k", t, images));
        List<Object> entries = (List<Object>) doc.get("entries");
        assertEquals(100L, asLong(((Map<String, Object>) entries.get(0)).get("date")));
        assertEquals(200L, asLong(((Map<String, Object>) entries.get(1)).get("date")));
        assertEquals(300L, asLong(((Map<String, Object>) entries.get(2)).get("date")));

        long before = System.currentTimeMillis();
        WidgetTimeline empty = new WidgetTimeline().setContent(new SurfaceText("x"));
        Map<String, Object> doc2 = parse(SurfaceSerializer.serializeTimeline("k", empty, images));
        List<Object> entries2 = (List<Object>) doc2.get("entries");
        assertEquals(1, entries2.size());
        long date = asLong(((Map<String, Object>) entries2.get(0)).get("date"));
        assertTrue(date >= before && date <= System.currentTimeMillis());
    }

    @Test
    void timelineWithoutContentIsRejected() {
        final WidgetTimeline t = new WidgetTimeline().addEntry(new Date(), null);
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                SurfaceSerializer.serializeTimeline("k", t,
                        new LinkedHashMap<String, byte[]>());
            }
        });
    }

    @Test
    void nestingDepthIsBounded() {
        SurfaceContainer root = new SurfaceColumn();
        SurfaceContainer cur = root;
        for (int i = 0; i < 9; i++) {
            SurfaceColumn next = new SurfaceColumn();
            cur.add(next);
            cur = next;
        }
        final WidgetTimeline t = new WidgetTimeline().setContent(root);
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                SurfaceSerializer.serializeTimeline("k", t,
                        new LinkedHashMap<String, byte[]>());
            }
        });
    }

    @Test
    void identicalImagesShipOnceAndRegisteredNamesShipNothing() throws Exception {
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        byte[] png = pngBytes(7);
        String name1 = SurfaceSerializer.registerImageBytes(png, images);
        String name2 = SurfaceSerializer.registerImageBytes(png, images);
        assertEquals(name1, name2);
        assertEquals(1, images.size());
        String other = SurfaceSerializer.registerImageBytes(pngBytes(11), images);
        assertFalse(other.equals(name1));
        assertEquals(2, images.size());

        // nodes that reference an already-registered name ship no new bytes
        SurfaceRow row = new SurfaceRow()
                .add(new SurfaceImage(name1))
                .add(new SurfaceImage("imgpreviouslyshipped"));
        Map<String, byte[]> publishImages = new LinkedHashMap<String, byte[]>();
        WidgetTimeline t = new WidgetTimeline().setContent(row);
        Map<String, Object> doc = parse(SurfaceSerializer.serializeTimeline("k", t, publishImages));
        assertTrue(publishImages.isEmpty());
        @SuppressWarnings("unchecked")
        List<Object> names = (List<Object>) doc.get("images");
        assertTrue(names.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void liveActivitySerializesRegionsAndState() throws Exception {
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        LiveActivityDescriptor d = new LiveActivityDescriptor("delivery")
                .setContent(new SurfaceText("${statusLabel}"))
                .setCompactLeading(new SurfaceImage("imgabc"))
                .setCompactTrailing(new SurfaceDynamicText(
                        SurfaceDynamicText.STYLE_TIMER_DOWN, "eta"))
                .setExpandedBottom(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState("progress"))
                .setTint(SurfaceColor.rgb(0xff00aa55))
                .setAndroidChannelId("deliveries");
        Map<String, Object> doc = parse(SurfaceSerializer.serializeLiveActivity(
                d, stateMap("Out for delivery", 1760000900000L, 0.7), images));
        assertEquals("delivery", doc.get("type"));
        assertEquals("deliveries",
                ((Map<String, Object>) doc.get("android")).get("channel"));
        Map<String, Object> island = (Map<String, Object>) doc.get("island");
        assertEquals("img", ((Map<String, Object>) island.get("compactLeading")).get("t"));
        assertEquals("dyn", ((Map<String, Object>) island.get("compactTrailing")).get("t"));
        assertEquals("prog", ((Map<String, Object>) island.get("expandedBottom")).get("t"));
        assertNull(island.get("minimal"));
        Map<String, Object> state = (Map<String, Object>) doc.get("state");
        assertEquals("Out for delivery", state.get("statusLabel"));
    }

    @Test
    void liveActivityLifecycleAgainstBridge() {
        FakeBridge bridge = new FakeBridge();
        Surfaces.setBridge(bridge);
        LiveActivity a = LiveActivity.start(
                new LiveActivityDescriptor("delivery").setContent(new SurfaceText("${s}")),
                params("s", "started"));
        assertTrue(a.isActive());
        assertEquals("act1", a.getId());
        assertNotNull(bridge.startedJson);

        a.update(params("s", "moving"));
        assertEquals(1, bridge.updates.size());
        assertTrue(bridge.updates.get(0).startsWith("act1:"));
        assertTrue(bridge.updates.get(0).contains("moving"));

        a.end(params("s", "done"), true);
        assertFalse(a.isActive());
        assertEquals("act1", bridge.endedId);
        assertTrue(bridge.endedFinalState.contains("done"));
        assertTrue(bridge.endedImmediately);

        // ended handles are inert
        a.update(params("s", "late"));
        a.end(null);
        assertEquals(1, bridge.updates.size());
    }

    @Test
    void unsupportedPlatformYieldsInertHandle() {
        FakeBridge bridge = new FakeBridge();
        bridge.activitiesSupported = false;
        Surfaces.setBridge(bridge);
        assertFalse(LiveActivity.isSupported());
        LiveActivity a = LiveActivity.start(
                new LiveActivityDescriptor("x").setContent(new SurfaceText("y")), null);
        assertFalse(a.isActive());
        assertNull(a.getId());
        a.update(params("k", "v"));
        a.end(null);
        assertTrue(bridge.updates.isEmpty());
        assertNull(bridge.endedId);
    }

    @Test
    void publishForwardsToBridgeAndNoBridgeIsNoOp() {
        // no bridge: must not throw
        Surfaces.publish("k", new WidgetTimeline().setContent(new SurfaceText("x")));
        assertEquals(0, Surfaces.getInstalledWidgetCount("k"));

        FakeBridge bridge = new FakeBridge();
        Surfaces.setBridge(bridge);
        Surfaces.registerWidgetKind(new WidgetKind("delivery_status")
                .setDisplayName("Delivery").addSupportedSize(WidgetSize.SMALL));
        assertEquals(1, bridge.registeredKinds.size());
        assertTrue(bridge.registeredKinds.get(0).contains("delivery_status"));

        Surfaces.publish("delivery_status",
                new WidgetTimeline().setContent(new SurfaceText("${s}")));
        assertEquals("delivery_status", bridge.publishedKind);
        assertNotNull(bridge.publishedJson);
        assertNotNull(bridge.publishedImages);
        assertEquals(2, Surfaces.getInstalledWidgetCount("delivery_status"));

        Surfaces.reloadWidgets(null);
        assertNull(bridge.reloadedKind);
    }

    @Test
    void invalidKindIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new WidgetKind("Delivery-Status");
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new WidgetKind("1delivery");
            }
        });
        assertEquals("delivery_status2", new WidgetKind("delivery_status2").getId());
    }

    @Test
    void actionsQueueUntilHandlerRegistersThenFlushInOrder() {
        final List<SurfaceActionEvent> received = new ArrayList<SurfaceActionEvent>();
        Surfaces.dispatchAction("delivery_status", "openOrder", params("orderId", "A1"));
        Surfaces.dispatchAction("delivery", "callCourier", null);

        Surfaces.setActionHandler(new SurfaceActionHandler() {
            public void onSurfaceAction(SurfaceActionEvent evt) {
                received.add(evt);
            }
        });
        // Display is not initialized in unit tests so delivery is direct
        assertEquals(2, received.size());
        assertEquals("openOrder", received.get(0).getActionId());
        assertEquals("A1", received.get(0).getParams().get("orderId"));
        assertTrue(received.get(0).isColdStart());
        assertEquals("callCourier", received.get(1).getActionId());
        assertTrue(received.get(1).getParams().isEmpty());

        // with a handler registered, dispatch is immediate and not cold start
        Surfaces.dispatchAction("delivery_status", "openOrder", null);
        assertEquals(3, received.size());
        assertFalse(received.get(2).isColdStart());
        assertEquals("delivery_status", received.get(2).getSource());
    }

    @Test
    void stateSerializationSortsKeys() {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("zebra", Integer.valueOf(1));
        state.put("alpha", Integer.valueOf(2));
        String json = SurfaceSerializer.serializeState(state);
        assertTrue(json.indexOf("alpha") < json.indexOf("zebra"));
        assertEquals("{}", SurfaceSerializer.serializeState(null));
    }

    @Test
    void perSizeLayoutOverridesSerializeSeparately() throws Exception {
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        SurfaceNode def = new SurfaceText("default");
        SurfaceNode small = new SurfaceText("small");
        WidgetTimeline t = new WidgetTimeline().setContent(def)
                .setContent(WidgetSize.SMALL, small);
        Map<String, Object> doc = parse(SurfaceSerializer.serializeTimeline("k", t, images));
        @SuppressWarnings("unchecked")
        Map<String, Object> layouts = (Map<String, Object>) doc.get("layouts");
        assertEquals(2, layouts.size());
        assertNotNull(layouts.get("default"));
        assertNotNull(layouts.get("small"));
        assertNull(layouts.get("medium"));
    }

    @Test
    void kindSerializationIncludesSizesAndDefaults() throws Exception {
        WidgetKind k = new WidgetKind("scores");
        Map<String, Object> doc = parse(SurfaceSerializer.serializeKind(k));
        assertEquals("scores", doc.get("id"));
        @SuppressWarnings("unchecked")
        List<Object> sizes = (List<Object>) doc.get("sizes");
        assertEquals(2, sizes.size());
        assertEquals("small", sizes.get(0));
        assertEquals("medium", sizes.get(1));
    }
}
