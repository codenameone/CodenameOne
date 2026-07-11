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
package com.codename1.impl.linux;

import com.codename1.io.JSONParser;
import com.codename1.io.Preferences;
import com.codename1.surfaces.SurfaceRasterizer;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.spi.SurfaceBridge;
import com.codename1.ui.Display;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The Linux {@link SurfaceBridge}: every pinned widget kind gets a frameless GTK "applet"
 * window (undecorated, keep-above, sticky, skip-taskbar, translucent rounded corners) that
 * renders the kind's published timeline through the shared {@link SurfaceRasterizer} -- the
 * desktop equivalent of a home-screen widget. A running live activity docks a black pill
 * window (~250x36 dips) at the top-center of the screen.
 *
 * <p>Everything published through this bridge is kept in memory for rendering <b>and</b>
 * persisted under {@code <storage dir>/cn1surfaces/<kindId>/} (atomic write-rename) so
 * timelines survive process death per the surfaces SPI contract; the pinned set and window
 * geometry persist through {@link Preferences} ({@code cn1.surfaces.*} keys) so widgets
 * restore where the user left them on the next run. Live activities are transient.</p>
 *
 * <p>Threading: rasterization always runs on the Codename One EDT ({@link SurfaceRasterizer}
 * requirement); the {@code LinuxNative.widget*} calls are thread-agnostic (they marshal onto
 * the GTK main loop internally). Native window events arrive on the main pump thread via
 * {@link #drainNativeEvents()} (called from {@code LinuxImplementation.drainInput}); action
 * clicks are routed through {@code Surfaces.dispatchAction}, which does its own EDT
 * marshaling and cold-start queuing. Refresh timers (dynamic-text ticks, timeline entry
 * flips) run on a shared {@link Timer} and bounce back to the EDT.</p>
 *
 * <p>There is no tray UI in this port, so {@link #pinWidget} / {@link #unpinWidget} are the
 * programmatic pinning entry points (reachable via
 * {@code (LinuxWidgetBridge) Display.getInstance().getSurfaceBridge()}); previously pinned
 * widgets restore automatically when the bridge is created. Under Wayland the applet windows
 * degrade to plain floating windows (no global positioning / keep-above -- see
 * {@code cn1_linux_widgets.c}).</p>
 */
public class LinuxWidgetBridge implements SurfaceBridge {
    /** Rasterize at 2x the dip size for crisp output; the window scales it back down. */
    private static final int SCALE = 2;
    private static final int CORNER = 24;
    private static final String[] SIZE_NAMES = {"small", "medium", "large"};
    private static final int[] SIZE_W = {158, 338, 338};
    private static final int[] SIZE_H = {158, 158, 354};
    private static final int PILL_W = 250;
    private static final int PILL_H = 36;
    private static final int PILL_TOP = 8;
    private static final int MIN_REFRESH_DELAY = 50;
    private static final int PERSIST_DEBOUNCE = 500;
    private static final int END_LINGER = 3000;

    private static final String PREF_PINNED = "cn1.surfaces.pinned";
    private static final String PREF_GEOM_PREFIX = "cn1.surfaces.geom.";

    private final String root;
    private final Map<String, Map<String, Object>> kinds =
            new LinkedHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> timelines =
            new HashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, byte[]>> kindImages =
            new HashMap<String, Map<String, byte[]>>();
    private final Map<String, WidgetWindow> windows =
            new LinkedHashMap<String, WidgetWindow>();
    private final Timer timer = new Timer();
    private TimerTask persistTask;
    private PillWindow pill;
    private int nextActivityId = 1;

    /**
     * Creates the bridge, restores timelines persisted by a previous run and re-pins the
     * widgets the user had on screen.
     */
    public LinuxWidgetBridge() {
        String dir = LinuxNative.storageDir();
        if (dir == null || dir.length() == 0) {
            dir = "/tmp";
        }
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        root = dir + "/cn1surfaces";
        LinuxNative.fileMkdir(root);
        restoreFromDisk();
        restorePinned();
    }

    /* --------------------------------------------------------- SurfaceBridge */

    @Override
    public boolean areWidgetsSupported() {
        return true;
    }

    @Override
    public boolean isLiveActivitySupported() {
        return true;
    }

    @Override
    public void registerWidgetKind(String kindJson) {
        Map<String, Object> kind = parse(kindJson);
        if (kind == null || !(kind.get("id") instanceof String)) {
            return;
        }
        String kindId = (String) kind.get("id");
        synchronized (this) {
            kinds.put(kindId, kind);
        }
        writeFileSafely(kindDir(kindId) + "/kind.json", utf8(kindJson));
    }

    @Override
    public void publishWidgetTimeline(String kindId, String timelineJson,
            Map<String, byte[]> images) {
        Map<String, Object> doc = parse(timelineJson);
        if (doc == null || kindId == null) {
            return;
        }
        Map<String, byte[]> imageCopy = new HashMap<String, byte[]>();
        if (images != null) {
            imageCopy.putAll(images);
        }
        synchronized (this) {
            timelines.put(kindId, doc);
            kindImages.put(kindId, imageCopy);
        }
        String dir = kindDir(kindId);
        writeFileSafely(dir + "/timeline.json", utf8(timelineJson));
        for (Map.Entry<String, byte[]> e : imageCopy.entrySet()) {
            String png = dir + "/" + e.getKey() + ".png";
            if (!LinuxNative.fileExists(png)) {
                // image names are content hashes so an existing file never needs rewriting
                writeFileSafely(png, e.getValue());
            }
        }
        WidgetWindow w;
        synchronized (this) {
            w = windows.get(kindId);
        }
        if (w != null) {
            w.requestRender();
        }
    }

    @Override
    public void reloadWidgets(String kindId) {
        List<WidgetWindow> all;
        synchronized (this) {
            all = new ArrayList<WidgetWindow>(windows.values());
        }
        for (WidgetWindow w : all) {
            if (kindId == null || kindId.equals(w.kindId)) {
                w.requestRender();
            }
        }
    }

    @Override
    public synchronized int getInstalledWidgetCount(String kindId) {
        return windows.containsKey(kindId) ? 1 : 0;
    }

    @Override
    public String startLiveActivity(String descriptorJson, Map<String, byte[]> images) {
        Map<String, Object> doc = parse(descriptorJson);
        if (doc == null) {
            return null;
        }
        Map<String, byte[]> imageCopy = new HashMap<String, byte[]>();
        if (images != null) {
            imageCopy.putAll(images);
        }
        LiveActivityRecord rec = new LiveActivityRecord(doc, imageCopy, asMap(doc.get("state")));
        String id;
        PillWindow old;
        PillWindow created;
        synchronized (this) {
            id = "la" + nextActivityId;
            nextActivityId++;
            rec.id = id;
            old = pill;
            created = new PillWindow(rec);
            pill = created;
        }
        if (old != null) {
            old.close();
        }
        created.requestRender();
        return id;
    }

    @Override
    public void updateLiveActivity(String activityId, String stateJson) {
        PillWindow p;
        synchronized (this) {
            p = pill;
        }
        if (p == null || !p.record.id.equals(activityId) || p.record.ended) {
            return;
        }
        Map<String, Object> state = parse(stateJson);
        p.record.state = state == null ? new HashMap<String, Object>() : state;
        p.requestRender();
    }

    @Override
    public void endLiveActivity(String activityId, String finalStateJson,
            boolean dismissImmediately) {
        final PillWindow p;
        synchronized (this) {
            p = pill;
            if (p == null || !p.record.id.equals(activityId) || p.record.ended) {
                return;
            }
            p.record.ended = true;
            pill = null;
        }
        if (finalStateJson != null) {
            Map<String, Object> state = parse(finalStateJson);
            if (state != null) {
                p.record.state = state;
            }
        }
        if (dismissImmediately) {
            p.close();
            return;
        }
        // show the final state briefly before dismissing, like the mobile platforms do
        p.requestRender();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                p.close();
            }
        }, END_LINGER);
    }

    /* ------------------------------------------------------------- pinning */

    /**
     * Pins a floating applet window for a widget kind (one instance per kind), or changes
     * the size of an already-pinned one. Any thread.
     *
     * @param kindId the widget kind to pin
     * @param sizeName {@code small} / {@code medium} / {@code large}, null for small
     */
    public void pinWidget(String kindId, String sizeName) {
        if (kindId == null) {
            return;
        }
        WidgetWindow w;
        WidgetWindow created = null;
        synchronized (this) {
            w = windows.get(kindId);
            if (w == null) {
                created = new WidgetWindow(kindId, normalizeSize(sizeName),
                        loadGeometry(kindId));
                windows.put(kindId, created);
            }
        }
        if (created != null) {
            created.requestRender();
        } else if (sizeName != null) {
            w.setSizeName(normalizeSize(sizeName));
        }
        schedulePersist();
    }

    /**
     * Removes a pinned applet window. Any thread.
     *
     * @param kindId the widget kind to unpin
     */
    public void unpinWidget(String kindId) {
        WidgetWindow w;
        synchronized (this) {
            w = windows.remove(kindId);
        }
        if (w != null) {
            w.close();
            Preferences.delete(PREF_GEOM_PREFIX + w.kindId);
        }
        schedulePersist();
    }

    private void restorePinned() {
        String pinned = Preferences.get(PREF_PINNED, "");
        if (pinned.length() == 0) {
            return;
        }
        for (String entry : split(pinned, ';')) {
            int sep = entry.indexOf('|');
            if (sep <= 0) {
                continue;
            }
            pinWidget(entry.substring(0, sep), entry.substring(sep + 1));
        }
    }

    /** Persisted "x,y" screen position of a kind's window, or null. */
    private static int[] loadGeometry(String kindId) {
        String geom = Preferences.get(PREF_GEOM_PREFIX + kindId, null);
        if (geom == null) {
            return null;
        }
        int comma = geom.indexOf(',');
        if (comma <= 0) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(geom.substring(0, comma)),
                    Integer.parseInt(geom.substring(comma + 1))};
        } catch (NumberFormatException err) {
            return null;
        }
    }

    /** Debounced persistence of the pinned set + geometry (pattern: JavaSE widgets). */
    private synchronized void schedulePersist() {
        if (persistTask != null) {
            persistTask.cancel();
        }
        persistTask = new TimerTask() {
            @Override
            public void run() {
                persistPinned();
            }
        };
        timer.schedule(persistTask, PERSIST_DEBOUNCE);
    }

    private void persistPinned() {
        List<WidgetWindow> all;
        synchronized (this) {
            all = new ArrayList<WidgetWindow>(windows.values());
        }
        StringBuilder sb = new StringBuilder();
        for (WidgetWindow w : all) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(w.kindId).append('|').append(w.sizeName);
            if (w.peer != 0) {
                // the native side caches the position from configure events, so this is
                // accurate even when the throttled "moved" stream swallowed the last one
                Preferences.set(PREF_GEOM_PREFIX + w.kindId,
                        LinuxNative.widgetGetX(w.peer) + "," + LinuxNative.widgetGetY(w.peer));
            }
        }
        Preferences.set(PREF_PINNED, sb.toString());
    }

    /* ----------------------------------------------------- native event drain */

    /**
     * Drains the native widget event queue ({@code "<id>;click;<x>;<y>"} /
     * {@code "<id>;moved;<x>;<y>"}). Called from the main pump thread's input drain loop.
     */
    void drainNativeEvents() {
        String ev = LinuxNative.widgetPollEvent();
        while (ev != null) {
            handleNativeEvent(ev);
            ev = LinuxNative.widgetPollEvent();
        }
    }

    private void handleNativeEvent(String ev) {
        String[] parts = split(ev, ';');
        if (parts.length < 4) {
            return;
        }
        long peer;
        int x;
        int y;
        try {
            peer = Long.parseLong(parts[0]);
            x = Integer.parseInt(parts[2]);
            y = Integer.parseInt(parts[3]);
        } catch (NumberFormatException err) {
            return;
        }
        String kind = parts[1];
        if ("moved".equals(kind)) {
            schedulePersist();
            return;
        }
        if (!"click".equals(kind)) {
            return;
        }
        SurfaceWindow target = null;
        synchronized (this) {
            for (WidgetWindow w : windows.values()) {
                if (w.peer == peer) {
                    target = w;
                    break;
                }
            }
            if (target == null && pill != null && pill.peer == peer) {
                target = pill;
            }
        }
        if (target != null) {
            target.handleClick(x, y);
        }
    }

    /* ------------------------------------------------------- surface windows */

    /** Shared behavior of the applet windows: native peer, hit testing, refresh timing. */
    private abstract class SurfaceWindow {
        long peer;
        /** Hit rects in rasterized (scaled) pixels; written on the EDT, read on the pump thread. */
        volatile List<SurfaceRasterizer.ActionRect> actions =
                new ArrayList<SurfaceRasterizer.ActionRect>();
        TimerTask refreshTask;

        abstract void renderNow();

        abstract String actionSource();

        /** Rasterizes on the EDT (SurfaceRasterizer requirement) whatever thread calls. */
        void requestRender() {
            if (!Display.isInitialized()) {
                return;
            }
            Display d = Display.getInstance();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    renderNow();
                }
            };
            if (d.isEdt()) {
                r.run();
            } else {
                d.callSerially(r);
            }
        }

        /** Click in window (dip) coordinates from the native button handler. */
        void handleClick(int x, int y) {
            int px = x * SCALE;
            int py = y * SCALE;
            for (SurfaceRasterizer.ActionRect r : actions) {
                if (px >= r.getX() && px < r.getX() + r.getWidth()
                        && py >= r.getY() && py < r.getY() + r.getHeight()) {
                    LinuxNative.widgetFocusApp();
                    // Surfaces.dispatchAction marshals to the CN1 EDT itself
                    Surfaces.dispatchAction(actionSource(), r.getActionId(), r.getParams());
                    return;
                }
            }
        }

        /** Pushes pixels + hit rects to the native window and re-arms the refresh timer. */
        void showResult(SurfaceRasterizer.Result result, int pixelW, int pixelH,
                long nextFlipMillis) {
            if (peer == 0) {
                return;
            }
            LinuxNative.widgetUpdatePixels(peer, result.getArgb(), pixelW, pixelH);
            actions = result.getActions();
            List<SurfaceRasterizer.ActionRect> rects = result.getActions();
            int[] flat = new int[rects.size() * 4];
            for (int i = 0; i < rects.size(); i++) {
                SurfaceRasterizer.ActionRect r = rects.get(i);
                // native hit tests run in window (dip) coordinates
                flat[i * 4] = r.getX() / SCALE;
                flat[i * 4 + 1] = r.getY() / SCALE;
                flat[i * 4 + 2] = r.getWidth() / SCALE;
                flat[i * 4 + 3] = r.getHeight() / SCALE;
            }
            LinuxNative.widgetSetHitRects(peer, flat);
            long due = result.getNextTickMillis();
            if (nextFlipMillis > 0 && (due == 0 || nextFlipMillis < due)) {
                due = nextFlipMillis;
            }
            scheduleRefresh(due);
        }

        void scheduleRefresh(long dueMillis) {
            if (refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
            if (dueMillis <= 0 || peer == 0) {
                return;
            }
            refreshTask = new TimerTask() {
                @Override
                public void run() {
                    requestRender();
                }
            };
            timer.schedule(refreshTask,
                    Math.max(MIN_REFRESH_DELAY, dueMillis - System.currentTimeMillis()));
        }

        void close() {
            if (refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
            if (peer != 0) {
                LinuxNative.widgetDestroy(peer);
                peer = 0;
            }
        }
    }

    /** A pinned applet window rendering one kind's published timeline. */
    private final class WidgetWindow extends SurfaceWindow {
        final String kindId;
        String sizeName;

        WidgetWindow(String kindId, String sizeName, int[] location) {
            this.kindId = kindId;
            this.sizeName = sizeName;
            int index = sizeIndex(sizeName);
            peer = LinuxNative.widgetCreate(SIZE_W[index], SIZE_H[index]);
            if (peer != 0 && location != null) {
                LinuxNative.widgetSetPosition(peer, location[0], location[1]);
            }
        }

        /** Changing the size recreates the native window (its size is fixed at create). */
        void setSizeName(String newSize) {
            if (newSize.equals(sizeName)) {
                return;
            }
            sizeName = newSize;
            long old = peer;
            int x = 0;
            int y = 0;
            if (old != 0) {
                x = LinuxNative.widgetGetX(old);
                y = LinuxNative.widgetGetY(old);
            }
            int index = sizeIndex(newSize);
            peer = LinuxNative.widgetCreate(SIZE_W[index], SIZE_H[index]);
            if (peer != 0 && old != 0) {
                LinuxNative.widgetSetPosition(peer, x, y);
            }
            if (old != 0) {
                LinuxNative.widgetDestroy(old);
            }
            requestRender();
            schedulePersist();
        }

        @Override
        void renderNow() {
            if (peer == 0) {
                return;
            }
            Map<String, Object> doc;
            Map<String, byte[]> images;
            String displayName;
            synchronized (LinuxWidgetBridge.this) {
                doc = timelines.get(kindId);
                Map<String, byte[]> imgs = kindImages.get(kindId);
                images = imgs == null ? new HashMap<String, byte[]>() : imgs;
                Map<String, Object> kind = kinds.get(kindId);
                Object name = kind == null ? null : kind.get("name");
                displayName = name instanceof String ? (String) name : kindId;
            }
            long now = System.currentTimeMillis();
            Map<String, Object> layout = SurfaceRasterizer.layoutForSize(doc, sizeName);
            Map<String, Object> state;
            if (layout == null) {
                // nothing published yet: show the kind name on the widget background
                layout = placeholderNode(displayName);
                state = new HashMap<String, Object>();
            } else {
                Map<String, Object> entry = SurfaceRasterizer.currentEntry(doc, now);
                state = entry == null ? new HashMap<String, Object>()
                        : asMap(entry.get("state"));
            }
            int index = sizeIndex(sizeName);
            Map<String, Object> wrapped = wrapRounded(layout, roleColor("background"), CORNER);
            Map<String, Object> scaled = scaleNodeDips(wrapped, SCALE);
            SurfaceRasterizer.Result result = SurfaceRasterizer.rasterize(scaled, state,
                    images, SIZE_W[index] * SCALE, SIZE_H[index] * SCALE, isSystemDark(), now);
            showResult(result, SIZE_W[index] * SCALE, SIZE_H[index] * SCALE,
                    SurfaceRasterizer.nextEntryFlip(doc, now));
        }

        @Override
        String actionSource() {
            return kindId;
        }
    }

    /** A running live activity: descriptor document, images and the latest state map. */
    private static final class LiveActivityRecord {
        final Map<String, Object> descriptor;
        final Map<String, byte[]> images;
        Map<String, Object> state;
        String id;
        boolean ended;

        LiveActivityRecord(Map<String, Object> descriptor, Map<String, byte[]> images,
                Map<String, Object> state) {
            this.descriptor = descriptor;
            this.images = images;
            this.state = state;
        }
    }

    /** The live activity pill docked at the top-center of the screen; always dark. */
    private final class PillWindow extends SurfaceWindow {
        final LiveActivityRecord record;

        PillWindow(LiveActivityRecord record) {
            this.record = record;
            peer = LinuxNative.widgetCreate(PILL_W, PILL_H);
            if (peer != 0) {
                // x == -1 centers horizontally (computed natively from the screen width)
                LinuxNative.widgetSetPosition(peer, -1, PILL_TOP);
            }
        }

        @Override
        void renderNow() {
            if (peer == 0) {
                return;
            }
            Map<String, Object> pillNode = buildPillNode(record.descriptor);
            Map<String, Object> wrapped = wrapRounded(pillNode,
                    argbColor(0xff000000), PILL_H / 2);
            Map<String, Object> scaled = scaleNodeDips(wrapped, SCALE);
            // the pill is always dark, matching the hardware cutout it mimics
            SurfaceRasterizer.Result result = SurfaceRasterizer.rasterize(scaled, record.state,
                    record.images, PILL_W * SCALE, PILL_H * SCALE, true,
                    System.currentTimeMillis());
            showResult(result, PILL_W * SCALE, PILL_H * SCALE, 0);
        }

        @Override
        String actionSource() {
            Object type = record.descriptor.get("type");
            return type instanceof String ? (String) type : record.id;
        }
    }

    /* --------------------------------------------------------- node helpers */

    /**
     * Synthesizes the compact pill layout from the descriptor's Dynamic Island regions:
     * compact leading and trailing pushed apart by a spacer, falling back to the minimal
     * region or the activity type name (mirrors the JavaSE simulator's pill).
     */
    private static Map<String, Object> buildPillNode(Map<String, Object> descriptor) {
        Map<String, Object> island = asMap(descriptor.get("island"));
        Object leading = island.get("compactLeading");
        Object trailing = island.get("compactTrailing");
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("t", "row");
        List<Object> pad = new ArrayList<Object>();
        pad.add(Integer.valueOf(4));
        pad.add(Integer.valueOf(14));
        pad.add(Integer.valueOf(4));
        pad.add(Integer.valueOf(14));
        row.put("pad", pad);
        row.put("spacing", Integer.valueOf(8));
        List<Object> children = new ArrayList<Object>();
        if (leading instanceof Map || trailing instanceof Map) {
            if (leading instanceof Map) {
                children.add(leading);
            }
            Map<String, Object> spacer = new LinkedHashMap<String, Object>();
            spacer.put("t", "spacer");
            children.add(spacer);
            if (trailing instanceof Map) {
                children.add(trailing);
            }
        } else if (island.get("minimal") instanceof Map) {
            children.add(island.get("minimal"));
        } else {
            Map<String, Object> label = new LinkedHashMap<String, Object>();
            label.put("t", "text");
            Object type = descriptor.get("type");
            label.put("text", type instanceof String ? type : "live activity");
            label.put("size", Integer.valueOf(12));
            children.add(label);
        }
        row.put("ch", children);
        return row;
    }

    /**
     * Wraps a layout in a rounded background box so the rasterized alpha carries the
     * window's shape (the RGBA visual turns it into real translucent corners).
     */
    private static Map<String, Object> wrapRounded(Map<String, Object> node,
            Map<String, Object> bg, int corner) {
        Map<String, Object> box = new LinkedHashMap<String, Object>();
        box.put("t", "box");
        box.put("bg", bg);
        box.put("corner", Integer.valueOf(corner));
        List<Object> children = new ArrayList<Object>();
        children.add(node);
        box.put("ch", children);
        return box;
    }

    private static Map<String, Object> placeholderNode(String name) {
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("t", "text");
        text.put("text", name);
        text.put("size", Integer.valueOf(14));
        return text;
    }

    private static Map<String, Object> roleColor(String role) {
        Map<String, Object> color = new LinkedHashMap<String, Object>();
        color.put("role", role);
        return color;
    }

    private static Map<String, Object> argbColor(int argb) {
        Map<String, Object> color = new LinkedHashMap<String, Object>();
        color.put("l", Integer.valueOf(argb));
        color.put("d", Integer.valueOf(argb));
        return color;
    }

    /**
     * Deep-copies a descriptor node multiplying every dip-valued attribute by {@code scale},
     * so the rasterizer (which treats dips as pixels) produces a crisp scaled rendering; the
     * action rectangles it returns are then in scaled pixels as well. Local replica of the
     * JavaSE bridge helper (ports never import across each other).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> scaleNodeDips(Map<String, Object> node, int scale) {
        if (node == null || scale == 1) {
            return node;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> e : node.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if ("ch".equals(key) && value instanceof List) {
                List<Object> children = new ArrayList<Object>();
                for (Object child : (List<Object>) value) {
                    if (child instanceof Map) {
                        children.add(scaleNodeDips((Map<String, Object>) child, scale));
                    } else {
                        children.add(child);
                    }
                }
                out.put(key, children);
            } else if ("pad".equals(key) && value instanceof List) {
                List<Object> pad = new ArrayList<Object>();
                for (Object p : (List<Object>) value) {
                    pad.add(p instanceof Number
                            ? (Object) Integer.valueOf(((Number) p).intValue() * scale) : p);
                }
                out.put(key, pad);
            } else if (isDipKey(key) && value instanceof Number) {
                out.put(key, Integer.valueOf(((Number) value).intValue() * scale));
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    private static boolean isDipKey(String key) {
        return "w".equals(key) || "h".equals(key) || "corner".equals(key)
                || "spacing".equals(key) || "min".equals(key) || "size".equals(key);
    }

    /* ------------------------------------------------------------ persistence */

    private String kindDir(String kindId) {
        String dir = root + "/" + sanitize(kindId);
        if (!LinuxNative.fileExists(dir)) {
            LinuxNative.fileMkdir(dir);
        }
        return dir;
    }

    private static String sanitize(String kindId) {
        StringBuilder sb = new StringBuilder(kindId.length());
        for (int i = 0; i < kindId.length(); i++) {
            char c = kindId.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    /** Atomic write-rename so a platform re-render never reads a torn file. */
    private static void writeFileSafely(String path, byte[] data) {
        String tmp = path + ".tmp";
        long handle = LinuxNative.fileOpenWrite(tmp, false);
        if (handle == 0) {
            return;
        }
        LinuxNative.fileWrite(handle, data, 0, data.length);
        LinuxNative.fileClose(handle);
        if (LinuxNative.fileExists(path)) {
            LinuxNative.fileDelete(path);
        }
        // fileRename takes a leaf name and renames within the same directory
        LinuxNative.fileRename(tmp, leafOf(path));
    }

    private static String leafOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static byte[] readFile(String path) {
        if (!LinuxNative.fileExists(path)) {
            return null;
        }
        long handle = LinuxNative.fileOpenRead(path);
        if (handle == 0) {
            return null;
        }
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len = LinuxNative.fileRead(handle, buf, 0, buf.length);
            while (len > 0) {
                bo.write(buf, 0, len);
                len = LinuxNative.fileRead(handle, buf, 0, buf.length);
            }
            return bo.toByteArray();
        } finally {
            LinuxNative.fileClose(handle);
        }
    }

    private void restoreFromDisk() {
        String[] dirs = LinuxNative.fileList(root);
        if (dirs == null) {
            return;
        }
        for (String name : dirs) {
            String dir = root + "/" + name;
            if (!LinuxNative.fileIsDirectory(dir)) {
                continue;
            }
            String kindId = name;
            byte[] kindData = readFile(dir + "/kind.json");
            if (kindData != null) {
                Map<String, Object> kind = parse(fromUtf8(kindData));
                if (kind != null && kind.get("id") instanceof String) {
                    kindId = (String) kind.get("id");
                    kinds.put(kindId, kind);
                }
            }
            byte[] timelineData = readFile(dir + "/timeline.json");
            if (timelineData != null) {
                Map<String, Object> doc = parse(fromUtf8(timelineData));
                if (doc != null) {
                    timelines.put(kindId, doc);
                    kindImages.put(kindId, readImages(dir));
                }
            }
        }
    }

    private static Map<String, byte[]> readImages(String dir) {
        Map<String, byte[]> images = new HashMap<String, byte[]>();
        String[] files = LinuxNative.fileList(dir);
        if (files == null) {
            return images;
        }
        for (String name : files) {
            if (name.endsWith(".png")) {
                byte[] data = readFile(dir + "/" + name);
                if (data != null) {
                    images.put(name.substring(0, name.length() - 4), data);
                }
            }
        }
        return images;
    }

    /* ---------------------------------------------------------------- misc */

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return new JSONParser().parseJSON(new StringReader(json));
        } catch (Exception err) {
            err.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<String, Object>();
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            return s.getBytes();
        }
    }

    private static String fromUtf8(byte[] data) {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            return new String(data);
        }
    }

    private static String normalizeSize(String sizeName) {
        for (String s : SIZE_NAMES) {
            if (s.equals(sizeName)) {
                return s;
            }
        }
        return "small";
    }

    private static int sizeIndex(String sizeName) {
        for (int i = 0; i < SIZE_NAMES.length; i++) {
            if (SIZE_NAMES[i].equals(sizeName)) {
                return i;
            }
        }
        return 0;
    }

    /** Follows the display's dark-mode flag when available; defaults to light. */
    private static boolean isSystemDark() {
        try {
            Boolean dark = Display.getInstance().isDarkMode();
            return dark != null && dark.booleanValue();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Basic split (the clean target avoids regex-based String.split). */
    private static String[] split(String s, char sep) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out.toArray(new String[out.size()]);
    }
}
