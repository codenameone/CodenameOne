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
package com.codename1.impl.windows;

import com.codename1.io.JSONParser;
import com.codename1.io.Preferences;
import com.codename1.surfaces.SurfaceRasterizer;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.spi.SurfaceBridge;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The native Windows {@link SurfaceBridge}: the plain-exe desktop face of
 * {@code com.codename1.surfaces}. Every pinned widget kind gets a frameless,
 * always-on-top layered window (created through the {@code widget*} natives in
 * {@link WindowsNative} / {@code cn1_windows_widgets.cpp}) that renders the
 * kind's published timeline with {@link SurfaceRasterizer} -- the Windows
 * equivalent of a home-screen widget, mirroring the JavaSE port's
 * {@code JavaSEWidgetWindows}. A running live activity docks a black pill
 * window at the top-center of the primary work area.
 *
 * <p>Everything published through this bridge is kept in memory <b>and</b>
 * persisted to disk (under {@code <storageDir>\cn1surfaces\<kindId>\} --
 * timeline.json plus content-hash-named PNGs, identical layout to the JavaSE
 * bridge and to what the MSIX Widgets Board provider reads), honoring the
 * dead-process rule of the surfaces SPI. Live activities are transient and are
 * not persisted. The pinned widget set and window geometry persist in
 * {@link Preferences} ({@code cn1.surfaces.pinned} / {@code cn1.surfaces.geom.<kind>})
 * so widgets restore where the user left them on the next launch; widgets are
 * process-bound in this version.</p>
 *
 * <p>Threading: rasterization always runs on the Codename One EDT (a
 * {@code SurfaceRasterizer} requirement); the pixels are then pushed through
 * the natives, which marshal all window work to the Win32 pump thread
 * themselves. Native events (clicks inside action hit-rects, drag moves)
 * arrive on the pump thread via {@code widgetPollEvent}, drained by
 * {@code WindowsImplementation.drainInput}, and are routed through
 * {@code Surfaces.dispatchAction} which handles EDT marshaling.</p>
 */
public class WindowsWidgetBridge implements SurfaceBridge {
    private static final String[] SIZE_NAMES = {"small", "medium", "large"};
    private static final int[] SIZE_W = {158, 338, 338};
    private static final int[] SIZE_H = {158, 158, 354};
    private static final int CORNER = 24;
    private static final int PILL_W = 250;
    private static final int PILL_H = 36;
    private static final int PILL_MARGIN_TOP = 8;
    private static final long LINGER_MILLIS = 3000;
    private static final String PREF_PINNED = "cn1.surfaces.pinned";
    private static final String PREF_GEOM = "cn1.surfaces.geom.";

    /** A floating surface window: a pinned widget kind or the live activity pill. */
    private final class SurfaceWindow {
        final String kindId;        // null for the live activity pill
        final String activityId;    // null for widget windows
        String sizeName;
        long handle;
        float scale = 1;
        // last rendered action rectangles, in window pixels; written on the EDT,
        // read on the pump thread when a click event arrives
        List<SurfaceRasterizer.ActionRect> actions =
                new ArrayList<SurfaceRasterizer.ActionRect>();
        TimerTask refreshTask;
        boolean closed;

        SurfaceWindow(String kindId, String activityId, String sizeName) {
            this.kindId = kindId;
            this.activityId = activityId;
            this.sizeName = sizeName;
        }
    }

    /** A running live activity: parsed descriptor, images and the latest state. */
    private static final class ActivityRecord {
        final Map<String, Object> descriptor;
        final Map<String, byte[]> images;
        Map<String, Object> state;

        ActivityRecord(Map<String, Object> descriptor, Map<String, byte[]> images,
                Map<String, Object> state) {
            this.descriptor = descriptor;
            this.images = images;
            this.state = state;
        }
    }

    private final String root;
    private final Map<String, Map<String, Object>> kinds =
            new LinkedHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> timelines =
            new HashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, byte[]>> kindImages =
            new HashMap<String, Map<String, byte[]>>();
    private final Map<String, ActivityRecord> liveActivities =
            new LinkedHashMap<String, ActivityRecord>();
    private final Map<String, SurfaceWindow> pinned =
            new LinkedHashMap<String, SurfaceWindow>();
    private final Map<Long, SurfaceWindow> byHandle = new HashMap<Long, SurfaceWindow>();
    private SurfaceWindow pill;
    private int nextActivityId = 1;
    private final Timer timer = new Timer();

    /**
     * Creates the bridge, restores timelines persisted by a previous run,
     * re-pins the widgets the user had pinned, and delivers any actions the
     * MSIX Widgets Board provider queued for a cold start.
     */
    public WindowsWidgetBridge() {
        String storage = WindowsNative.storageDir();
        this.root = (storage == null ? "." : storage) + "\\cn1surfaces";
        WindowsNative.fileMkdir(root);
        restoreFromDisk();
        drainBoardActions();
        restorePinned();
    }

    // --- SurfaceBridge -----------------------------------------------------------

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
        writeFileSafely(kindDir(kindId) + "\\kind.json", utf8(kindJson));
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
        SurfaceWindow window;
        synchronized (this) {
            timelines.put(kindId, doc);
            kindImages.put(kindId, imageCopy);
            window = pinned.get(kindId);
        }
        String dir = kindDir(kindId);
        writeFileSafely(dir + "\\timeline.json", utf8(timelineJson));
        for (Map.Entry<String, byte[]> e : imageCopy.entrySet()) {
            String png = dir + "\\" + e.getKey() + ".png";
            if (!WindowsNative.fileExists(png)) {
                // image names are content hashes so an existing file never needs rewriting
                writeFileSafely(png, e.getValue());
            }
        }
        if (window != null) {
            requestRender(window);
        }
    }

    @Override
    public void reloadWidgets(String kindId) {
        List<SurfaceWindow> windows;
        synchronized (this) {
            windows = new ArrayList<SurfaceWindow>(pinned.values());
        }
        for (SurfaceWindow w : windows) {
            if (kindId == null || kindId.equals(w.kindId)) {
                requestRender(w);
            }
        }
    }

    @Override
    public synchronized int getInstalledWidgetCount(String kindId) {
        return pinned.containsKey(kindId) ? 1 : 0;
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
        String id;
        SurfaceWindow window;
        synchronized (this) {
            id = "la" + nextActivityId++;
            liveActivities.put(id, new ActivityRecord(doc, imageCopy, asMap(doc.get("state"))));
            if (pill != null) {
                closeWindow(pill);
            }
            window = new SurfaceWindow(null, id, null);
            pill = window;
        }
        float scale = WindowsNative.widgetGetDpiScale(0);
        if (scale <= 0) {
            scale = 1;
        }
        window.scale = scale;
        window.handle = WindowsNative.widgetCreate(Math.round(PILL_W * scale),
                Math.round(PILL_H * scale));
        if (window.handle == 0) {
            synchronized (this) {
                pill = null;
                liveActivities.remove(id);
            }
            return null;
        }
        synchronized (this) {
            byHandle.put(Long.valueOf(window.handle), window);
        }
        WindowsNative.widgetSetPosition(window.handle, WindowsNative.WIDGET_POS_CENTER_H,
                Math.round(PILL_MARGIN_TOP * scale));
        requestRender(window);
        return id;
    }

    @Override
    public void updateLiveActivity(String activityId, String stateJson) {
        ActivityRecord rec;
        SurfaceWindow window;
        synchronized (this) {
            rec = liveActivities.get(activityId);
            window = pill != null && activityId != null && activityId.equals(pill.activityId)
                    ? pill : null;
        }
        if (rec == null) {
            return;
        }
        Map<String, Object> state = parse(stateJson);
        rec.state = state == null ? new HashMap<String, Object>() : state;
        if (window != null) {
            requestRender(window);
        }
    }

    @Override
    public void endLiveActivity(final String activityId, String finalStateJson,
            boolean dismissImmediately) {
        ActivityRecord rec;
        final SurfaceWindow window;
        synchronized (this) {
            rec = liveActivities.get(activityId);
            if (pill != null && activityId != null && activityId.equals(pill.activityId)) {
                window = pill;
                pill = null;
            } else {
                window = null;
            }
            if (window == null || dismissImmediately) {
                liveActivities.remove(activityId);
            }
        }
        if (window == null) {
            return;
        }
        if (dismissImmediately) {
            closeWindow(window);
            return;
        }
        // show the final state briefly before dismissing, like the platforms do;
        // the record stays registered until the linger ends so the async render
        // can still resolve it
        if (rec != null && finalStateJson != null) {
            Map<String, Object> state = parse(finalStateJson);
            if (state != null) {
                rec.state = state;
                requestRender(window);
            }
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (WindowsWidgetBridge.this) {
                    liveActivities.remove(activityId);
                }
                closeWindow(window);
            }
        }, LINGER_MILLIS);
    }

    // --- pin management ------------------------------------------------------------

    /**
     * Pins a floating widget for a kind (one instance per kind), creating its
     * native layered window sized {@code small}/{@code medium}/{@code large}
     * dips scaled by the monitor's DPI. Re-pinning with a different size
     * re-renders at the new size. Any thread.
     *
     * @param kindId the widget kind to pin
     * @param sizeName {@code small} / {@code medium} / {@code large}, null for small
     */
    public void pinWidget(String kindId, String sizeName) {
        if (kindId == null) {
            return;
        }
        String size = normalizeSize(sizeName);
        SurfaceWindow window;
        boolean create = false;
        synchronized (this) {
            window = pinned.get(kindId);
            if (window == null) {
                window = new SurfaceWindow(kindId, null, size);
                pinned.put(kindId, window);
                create = true;
            } else if (sizeName != null) {
                window.sizeName = size;
            }
        }
        if (create) {
            float scale = WindowsNative.widgetGetDpiScale(0);
            if (scale <= 0) {
                scale = 1;
            }
            window.scale = scale;
            int index = sizeIndex(size);
            window.handle = WindowsNative.widgetCreate(Math.round(SIZE_W[index] * scale),
                    Math.round(SIZE_H[index] * scale));
            if (window.handle == 0) {
                synchronized (this) {
                    pinned.remove(kindId);
                }
                return;
            }
            synchronized (this) {
                byHandle.put(Long.valueOf(window.handle), window);
            }
            // restore the persisted position, if the user placed this kind before
            String geom = Preferences.get(PREF_GEOM + kindId, "");
            int comma = geom.indexOf(',');
            if (comma > 0) {
                try {
                    WindowsNative.widgetSetPosition(window.handle,
                            Integer.parseInt(geom.substring(0, comma)),
                            Integer.parseInt(geom.substring(comma + 1)));
                } catch (NumberFormatException ignore) {
                    // stale preference; keep the native default placement
                }
            }
        }
        savePinned();
        requestRender(window);
    }

    /**
     * Removes a pinned widget window. Any thread.
     *
     * @param kindId the widget kind to unpin
     */
    public void unpinWidget(String kindId) {
        SurfaceWindow window;
        synchronized (this) {
            window = pinned.remove(kindId);
        }
        if (window != null) {
            closeWindow(window);
            savePinned();
        }
    }

    private void restorePinned() {
        String list = Preferences.get(PREF_PINNED, "");
        if (list.length() == 0) {
            return;
        }
        for (String entry : split(list, ';')) {
            int sep = entry.indexOf('|');
            if (sep > 0) {
                pinWidget(entry.substring(0, sep), normalizeSize(entry.substring(sep + 1)));
            }
        }
    }

    private void savePinned() {
        StringBuilder sb = new StringBuilder();
        synchronized (this) {
            for (SurfaceWindow w : pinned.values()) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(w.kindId).append('|').append(w.sizeName);
            }
        }
        Preferences.set(PREF_PINNED, sb.toString());
    }

    private void closeWindow(SurfaceWindow window) {
        final long handle;
        synchronized (this) {
            if (window.closed) {
                return;
            }
            window.closed = true;
            handle = window.handle;
            if (handle != 0) {
                byHandle.remove(Long.valueOf(handle));
            }
            if (window.refreshTask != null) {
                window.refreshTask.cancel();
                window.refreshTask = null;
            }
        }
        if (handle != 0 && Display.isInitialized()) {
            // destroy from the EDT so it serializes behind any in-flight render:
            // a renderOnEdt that already passed its closed check must post its
            // pixel op before the destroy op, or the native handler would touch
            // a freed widget struct
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    WindowsNative.widgetDestroy(handle);
                }
            });
        } else if (handle != 0) {
            WindowsNative.widgetDestroy(handle);
        }
    }

    // --- native event routing --------------------------------------------------------

    /**
     * Routes one native widget event drained by
     * {@code WindowsImplementation.drainInput} (pump thread):
     * {@code "<handle>;click;<x>;<y>"} focuses the app and dispatches the
     * matching action through {@code Surfaces.dispatchAction} (which marshals
     * to the EDT itself); {@code "<handle>;moved;<x>;<y>"} persists the new
     * position and re-renders when the monitor's DPI scale changed.
     *
     * @param event the encoded native event
     */
    void handleNativeEvent(String event) {
        if (event == null) {
            return;
        }
        String[] parts = split(event, ';');
        if (parts.length < 4) {
            return;
        }
        long handle;
        int x;
        int y;
        try {
            handle = Long.parseLong(parts[0]);
            x = Integer.parseInt(parts[2]);
            y = Integer.parseInt(parts[3]);
        } catch (NumberFormatException err) {
            return;
        }
        SurfaceWindow window;
        synchronized (this) {
            window = byHandle.get(Long.valueOf(handle));
        }
        if (window == null) {
            return;
        }
        if ("click".equals(parts[1])) {
            SurfaceRasterizer.ActionRect match = null;
            synchronized (this) {
                for (SurfaceRasterizer.ActionRect r : window.actions) {
                    if (x >= r.getX() && x < r.getX() + r.getWidth()
                            && y >= r.getY() && y < r.getY() + r.getHeight()) {
                        match = r;
                        break;
                    }
                }
            }
            if (match != null) {
                WindowsNative.widgetFocusApp();
                // Surfaces.dispatchAction marshals to the CN1 EDT itself
                Surfaces.dispatchAction(actionSource(window), match.getActionId(),
                        match.getParams());
            }
        } else if ("moved".equals(parts[1])) {
            if (window.kindId != null) {
                final String key = PREF_GEOM + window.kindId;
                final String value = x + "," + y;
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        Preferences.set(key, value);
                    }
                });
            }
            // dragged onto a monitor with a different scale: re-render at the new
            // pixel size (requestRender re-reads the scale)
            float scale = WindowsNative.widgetGetDpiScale(handle);
            if (scale > 0 && Math.abs(scale - window.scale) > 0.01f) {
                requestRender(window);
            }
        }
    }

    private String actionSource(SurfaceWindow window) {
        if (window.kindId != null) {
            return window.kindId;
        }
        ActivityRecord rec;
        synchronized (this) {
            rec = liveActivities.get(window.activityId);
        }
        Object type = rec == null ? null : rec.descriptor.get("type");
        return type instanceof String ? (String) type : window.activityId;
    }

    // --- rendering -------------------------------------------------------------------

    /**
     * Re-renders a surface window: rasterizes on the EDT (a SurfaceRasterizer
     * requirement), composes the rounded platform background, pushes pixels +
     * hit-rects through the natives and schedules the next time-driven
     * re-render (dynamic text tick / timeline entry flip). Any thread.
     */
    private void requestRender(final SurfaceWindow window) {
        if (!Display.isInitialized()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                renderOnEdt(window);
            }
        });
    }

    private void renderOnEdt(SurfaceWindow window) {
        long handle;
        synchronized (this) {
            if (window.closed) {
                return;
            }
            handle = window.handle;
        }
        if (handle == 0) {
            return;
        }
        float scale = WindowsNative.widgetGetDpiScale(handle);
        if (scale <= 0) {
            scale = 1;
        }
        window.scale = scale;
        long now = System.currentTimeMillis();
        Map<String, Object> node;
        Map<String, Object> state;
        Map<String, byte[]> images;
        int logicalW;
        int logicalH;
        boolean dark;
        int bgColor;
        long nextFlip = 0;
        if (window.kindId != null) {
            Map<String, Object> doc;
            synchronized (this) {
                doc = timelines.get(window.kindId);
                images = imagesOf(window.kindId);
            }
            int index = sizeIndex(window.sizeName);
            logicalW = SIZE_W[index];
            logicalH = SIZE_H[index];
            node = SurfaceRasterizer.layoutForSize(doc, window.sizeName);
            if (node == null) {
                // nothing published yet: show the kind's display name as a placeholder
                node = placeholderNode(window.kindId);
                state = new HashMap<String, Object>();
            } else {
                Map<String, Object> entry = SurfaceRasterizer.currentEntry(doc, now);
                state = entry == null ? new HashMap<String, Object>()
                        : asMap(entry.get("state"));
                nextFlip = SurfaceRasterizer.nextEntryFlip(doc, now);
            }
            dark = isSystemDark();
            bgColor = dark ? 0x1c1c1e : 0xffffff;
        } else {
            ActivityRecord rec;
            synchronized (this) {
                rec = liveActivities.get(window.activityId);
            }
            if (rec == null) {
                // ended: keep whatever pixels are showing during the linger period
                return;
            }
            logicalW = PILL_W;
            logicalH = PILL_H;
            node = buildPillNode(rec.descriptor);
            state = rec.state;
            images = rec.images;
            // the pill is always dark, matching the hardware cutout it mimics
            dark = true;
            bgColor = 0x000000;
        }

        int pw = Math.max(1, Math.round(logicalW * scale));
        int ph = Math.max(1, Math.round(logicalH * scale));
        SurfaceRasterizer.Result result = SurfaceRasterizer.rasterize(
                scaleNodeDips(node, scale), state, images, pw, ph, dark, now);

        // compose over the rounded platform background: transparent corners in
        // the final ARGB become per-pixel alpha in the layered window, so the
        // widget's rounded shape comes straight from these pixels
        Image composed = Image.createImage(pw, ph, 0);
        Graphics g = composed.getGraphics();
        g.setColor(bgColor);
        int arc = window.kindId != null ? Math.round(CORNER * scale) : ph;
        g.fillRoundRect(0, 0, pw, ph, arc, arc);
        g.drawImage(result.getImage(), 0, 0);

        WindowsNative.widgetUpdatePixels(handle, composed.getRGB(), pw, ph);
        List<SurfaceRasterizer.ActionRect> actions = result.getActions();
        int[] packed = new int[actions.size() * 4];
        for (int i = 0; i < actions.size(); i++) {
            SurfaceRasterizer.ActionRect r = actions.get(i);
            packed[i * 4] = r.getX();
            packed[i * 4 + 1] = r.getY();
            packed[i * 4 + 2] = r.getWidth();
            packed[i * 4 + 3] = r.getHeight();
        }
        WindowsNative.widgetSetHitRects(handle, packed);
        synchronized (this) {
            window.actions = actions;
        }
        scheduleRefresh(window, result.getNextTickMillis(), nextFlip, now);
    }

    /** Schedules the next time-driven re-render (countdown tick / entry flip). */
    private void scheduleRefresh(final SurfaceWindow window, long nextTick, long nextFlip,
            long now) {
        long due = nextTick;
        if (nextFlip > 0 && (due == 0 || nextFlip < due)) {
            due = nextFlip;
        }
        synchronized (this) {
            if (window.refreshTask != null) {
                window.refreshTask.cancel();
                window.refreshTask = null;
            }
            if (due <= 0 || window.closed) {
                return;
            }
            window.refreshTask = new TimerTask() {
                @Override
                public void run() {
                    requestRender(window);
                }
            };
            timer.schedule(window.refreshTask, Math.max(50, due - now));
        }
    }

    /** A minimal descriptor showing the kind's display name until content is published. */
    private Map<String, Object> placeholderNode(String kindId) {
        String name = kindId;
        synchronized (this) {
            Map<String, Object> kind = kinds.get(kindId);
            if (kind != null && kind.get("name") instanceof String) {
                name = (String) kind.get("name");
            }
        }
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("t", "text");
        text.put("text", name);
        Map<String, Object> box = new LinkedHashMap<String, Object>();
        box.put("t", "box");
        List<Object> children = new ArrayList<Object>();
        children.add(text);
        box.put("ch", children);
        return box;
    }

    /**
     * Synthesizes the compact live-activity pill layout: compact leading and
     * trailing regions pushed apart by a spacer, falling back to the minimal
     * region or the activity type name (the same shape the JavaSE pill uses).
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
     * Deep-copies a descriptor node multiplying every dip-valued attribute by
     * {@code scale}, so the rasterizer (which treats dips as pixels) produces a
     * crisp DPI-scaled rendering; the action rectangles it returns are then in
     * scaled pixels as well. A local float-scale replica of the JavaSE bridge's
     * helper (that class is not on the Windows port classpath).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> scaleNodeDips(Map<String, Object> node, float scale) {
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
                            ? (Object) Integer.valueOf(Math.round(((Number) p).intValue() * scale))
                            : p);
                }
                out.put(key, pad);
            } else if (isDipKey(key) && value instanceof Number) {
                out.put(key, Integer.valueOf(Math.round(((Number) value).intValue() * scale)));
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

    // --- persistence -------------------------------------------------------------

    private String kindDir(String kindId) {
        String dir = root + "\\" + sanitize(kindId);
        WindowsNative.fileMkdir(dir);
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

    /**
     * Atomic write-rename (MoveFileEx REPLACE_EXISTING under the native) so a
     * concurrent reader -- including the MSIX Widgets Board provider process --
     * never sees a torn file.
     */
    private static void writeFileSafely(String path, byte[] data) {
        if (data == null) {
            return;
        }
        String tmp = path + ".tmp";
        long handle = WindowsNative.fileOpenWrite(tmp, false);
        if (handle == 0) {
            return;
        }
        WindowsNative.fileWrite(handle, data, 0, data.length);
        WindowsNative.fileClose(handle);
        WindowsNative.fileRename(tmp, path);
    }

    private static byte[] readFile(String path) {
        if (!WindowsNative.fileExists(path)) {
            return null;
        }
        long handle = WindowsNative.fileOpenRead(path);
        if (handle == 0) {
            return null;
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len = WindowsNative.fileRead(handle, buf, 0, buf.length);
        while (len > 0) {
            bo.write(buf, 0, len);
            len = WindowsNative.fileRead(handle, buf, 0, buf.length);
        }
        WindowsNative.fileClose(handle);
        return bo.toByteArray();
    }

    private void restoreFromDisk() {
        String[] dirs = WindowsNative.fileList(root);
        if (dirs == null) {
            return;
        }
        for (String name : dirs) {
            if (name == null || ".".equals(name) || "..".equals(name)) {
                continue;
            }
            String dir = root + "\\" + name;
            if (!WindowsNative.fileIsDirectory(dir)) {
                continue;
            }
            String kindId = name;
            byte[] kindData = readFile(dir + "\\kind.json");
            if (kindData != null) {
                Map<String, Object> kind = parse(string(kindData));
                if (kind != null && kind.get("id") instanceof String) {
                    kindId = (String) kind.get("id");
                    kinds.put(kindId, kind);
                }
            }
            byte[] timelineData = readFile(dir + "\\timeline.json");
            if (timelineData != null) {
                Map<String, Object> doc = parse(string(timelineData));
                if (doc != null) {
                    timelines.put(kindId, doc);
                    kindImages.put(kindId, readImages(dir));
                }
            }
        }
    }

    private static Map<String, byte[]> readImages(String dir) {
        Map<String, byte[]> images = new HashMap<String, byte[]>();
        String[] files = WindowsNative.fileList(dir);
        if (files == null) {
            return images;
        }
        for (String name : files) {
            if (name != null && name.endsWith(".png")) {
                byte[] data = readFile(dir + "\\" + name);
                if (data != null) {
                    images.put(name.substring(0, name.length() - 4), data);
                }
            }
        }
        return images;
    }

    /**
     * Delivers actions the MSIX Widgets Board provider queued while the app
     * was not running: the out-of-process provider appends
     * {@code source<TAB>actionId<TAB>paramsJson} lines to
     * {@code cn1surfaces\pending_actions.txt} and launches the app; this drains
     * the file into {@code Surfaces.dispatchAction} (whose cold-start queue
     * holds them until the app registers a handler) and deletes it.
     */
    private void drainBoardActions() {
        String path = root + "\\pending_actions.txt";
        byte[] data = readFile(path);
        if (data == null) {
            return;
        }
        WindowsNative.fileDelete(path);
        for (String rawLine : split(string(data), '\n')) {
            String line = rawLine.trim();
            if (line.length() == 0) {
                continue;
            }
            String[] fields = split(line, '\t');
            if (fields.length < 2) {
                continue;
            }
            Map<String, Object> params = fields.length > 2 ? parse(fields[2]) : null;
            Surfaces.dispatchAction(fields[0], fields[1], params);
        }
    }

    // --- small helpers -------------------------------------------------------------

    private synchronized Map<String, byte[]> imagesOf(String kindId) {
        Map<String, byte[]> images = kindImages.get(kindId);
        return images == null ? new HashMap<String, byte[]>() : images;
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

    /** Follows the display's dark-mode flag when the port reports one; defaults to light. */
    private static boolean isSystemDark() {
        // Assign-in-try / decide-outside-try: returning a freshly merged boolean
        // from inside the translated try block ICEs gcc (SSA corruption) on the
        // ParparVM-generated C -- see the identical note in LinuxWidgetBridge.
        Boolean dark = null;
        try {
            dark = Display.getInstance().isDarkMode();
        } catch (Throwable t) {
            // headless or uninitialized display: default to light
        }
        return dark != null && dark.booleanValue();
    }

    /** String.split without regex (the translated runtime keeps regex minimal). */
    private static String[] split(String s, char sep) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts.toArray(new String[parts.size()]);
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (IOException err) {
            return s.getBytes();
        }
    }

    private static String string(byte[] data) {
        try {
            return new String(data, "UTF-8");
        } catch (IOException err) {
            return new String(data);
        }
    }

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
}
