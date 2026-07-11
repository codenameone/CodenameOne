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
package com.codename1.impl.javase;

import com.codename1.io.JSONParser;
import com.codename1.surfaces.SurfaceRasterizer;
import com.codename1.surfaces.spi.SurfaceBridge;
import com.codename1.ui.Display;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JavaSE {@link SurfaceBridge}: the desktop face of the {@code com.codename1.surfaces}
 * framework. In simulator mode published widget timelines feed the Widgets preview window
 * ({@link SimulatorWidgets}, opened from the simulator's Widgets menu); in desktop mode they feed
 * frameless floating widget windows ({@link JavaSEWidgetWindows}) that persist across runs.
 *
 * <p>Everything published through this bridge is kept in memory for the UI <b>and</b> persisted
 * to disk (under {@code <app home>/cn1surfaces/<kindId>/} -- the same home directory the port
 * uses for storage) so desktop floating widgets can restore on the next launch, honoring the
 * dead-process rule of the surfaces SPI. Live activities are transient and are not persisted.</p>
 *
 * <p>Action clicks decoded by the surface windows are routed through
 * {@code com.codename1.surfaces.Surfaces.dispatchAction(...)}, which handles EDT marshaling and
 * cold-start queueing itself.</p>
 */
public class JavaSEWidgetBridge implements SurfaceBridge {

    /** Observers (the preview window and the floating windows) notified of publishes. */
    public interface Listener {
        /** A widget kind was registered (or re-registered). */
        void widgetKindRegistered(String kindId);

        /** A kind's timeline was atomically replaced; re-render its surfaces. */
        void widgetTimelinePublished(String kindId);

        /** The platform was asked to re-render from persisted timelines; kindId null means all. */
        void widgetsReloaded(String kindId);

        /** A live activity started. */
        void liveActivityStarted(String activityId);

        /** A live activity received a fresh state map. */
        void liveActivityUpdated(String activityId);

        /** A live activity ended. */
        void liveActivityEnded(String activityId, boolean dismissImmediately);
    }

    /** A running live activity: descriptor document, images and the latest state map. */
    public static final class LiveActivityRecord {
        private final String id;
        private final Map<String, Object> descriptor;
        private final Map<String, byte[]> images;
        private Map<String, Object> state;

        LiveActivityRecord(String id, Map<String, Object> descriptor, Map<String, byte[]> images,
                Map<String, Object> state) {
            this.id = id;
            this.descriptor = descriptor;
            this.images = images;
            this.state = state;
        }

        public String getId() {
            return id;
        }

        /** The parsed descriptor document (type, content, island regions, ...). */
        public Map<String, Object> getDescriptor() {
            return descriptor;
        }

        public Map<String, byte[]> getImages() {
            return images;
        }

        /** The latest state map -- replaced wholesale by each update. */
        public Map<String, Object> getState() {
            return state;
        }

        void setState(Map<String, Object> state) {
            this.state = state;
        }
    }

    private final File root;
    private final boolean simulator;
    private final Map<String, Map<String, Object>> kinds =
            new LinkedHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> timelines =
            new HashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, byte[]>> kindImages =
            new HashMap<String, Map<String, byte[]>>();
    private final Map<String, LiveActivityRecord> liveActivities =
            new LinkedHashMap<String, LiveActivityRecord>();
    private final List<Listener> listeners = new ArrayList<Listener>();
    private JavaSEWidgetWindows desktopWindows;
    private int nextActivityId = 1;

    /**
     * Creates the bridge and restores any timelines persisted by a previous run.
     *
     * @param root the persistence root, e.g. {@code <app home>/cn1surfaces}
     * @param simulator true in simulator mode, false in a desktop build
     */
    public JavaSEWidgetBridge(File root, boolean simulator) {
        this.root = root;
        this.simulator = simulator;
        restoreFromDisk();
    }

    // --- SurfaceBridge ---------------------------------------------------------

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
        if (kind == null) {
            return;
        }
        Object id = kind.get("id");
        if (!(id instanceof String)) {
            return;
        }
        String kindId = (String) id;
        synchronized (this) {
            kinds.put(kindId, kind);
        }
        writeFileSafely(new File(kindDir(kindId), "kind.json"),
                kindJson.getBytes(StandardCharsets.UTF_8));
        for (Listener l : snapshotListeners()) {
            l.widgetKindRegistered(kindId);
        }
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
        File dir = kindDir(kindId);
        writeFileSafely(new File(dir, "timeline.json"),
                timelineJson.getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, byte[]> e : imageCopy.entrySet()) {
            File png = new File(dir, e.getKey() + ".png");
            if (!png.exists()) {
                // image names are content hashes so an existing file never needs rewriting
                writeFileSafely(png, e.getValue());
            }
        }
        for (Listener l : snapshotListeners()) {
            l.widgetTimelinePublished(kindId);
        }
    }

    @Override
    public void reloadWidgets(String kindId) {
        for (Listener l : snapshotListeners()) {
            l.widgetsReloaded(kindId);
        }
    }

    @Override
    public int getInstalledWidgetCount(String kindId) {
        if (desktopWindows != null) {
            return desktopWindows.getPinnedCount(kindId);
        }
        // in the simulator the preview window counts as one installed instance so
        // publish-skipping optimizations in app code never starve the preview
        synchronized (this) {
            return kinds.containsKey(kindId) ? 1 : 0;
        }
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
        Map<String, Object> state = asMap(doc.get("state"));
        String id;
        LiveActivityRecord rec;
        synchronized (this) {
            id = "la" + nextActivityId++;
            rec = new LiveActivityRecord(id, doc, imageCopy, state);
            liveActivities.put(id, rec);
        }
        for (Listener l : snapshotListeners()) {
            l.liveActivityStarted(id);
        }
        return id;
    }

    @Override
    public void updateLiveActivity(String activityId, String stateJson) {
        LiveActivityRecord rec;
        synchronized (this) {
            rec = liveActivities.get(activityId);
        }
        if (rec == null) {
            return;
        }
        Map<String, Object> state = parse(stateJson);
        rec.setState(state == null ? new HashMap<String, Object>() : state);
        for (Listener l : snapshotListeners()) {
            l.liveActivityUpdated(activityId);
        }
    }

    @Override
    public void endLiveActivity(String activityId, String finalStateJson,
            boolean dismissImmediately) {
        LiveActivityRecord rec;
        synchronized (this) {
            rec = liveActivities.remove(activityId);
        }
        if (rec == null) {
            return;
        }
        if (finalStateJson != null) {
            Map<String, Object> state = parse(finalStateJson);
            if (state != null) {
                rec.setState(state);
            }
        }
        for (Listener l : snapshotListeners()) {
            l.liveActivityEnded(activityId, dismissImmediately);
        }
    }

    // --- accessors for the surface windows --------------------------------------

    /** True when running inside the simulator (vs a desktop app build). */
    public boolean isSimulator() {
        return simulator;
    }

    /**
     * The ids of the known widget kinds in registration order: every registered (or
     * disk-restored) kind plus any kind that published a timeline without registering first.
     */
    public synchronized List<String> getKindIds() {
        List<String> ids = new ArrayList<String>(kinds.keySet());
        for (String id : timelines.keySet()) {
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** The display name of a kind, falling back to its id. */
    public synchronized String getKindDisplayName(String kindId) {
        Map<String, Object> kind = kinds.get(kindId);
        Object name = kind == null ? null : kind.get("name");
        return name instanceof String ? (String) name : kindId;
    }

    /** The parsed timeline document last published for a kind, or null. */
    public synchronized Map<String, Object> getTimelineDoc(String kindId) {
        return timelines.get(kindId);
    }

    /** The PNG blobs shipped with a kind's timeline; never null. */
    public synchronized Map<String, byte[]> getKindImages(String kindId) {
        Map<String, byte[]> images = kindImages.get(kindId);
        return images == null ? new HashMap<String, byte[]>() : images;
    }

    /** The ids of the running live activities, in start order. */
    public synchronized List<String> getLiveActivityIds() {
        return new ArrayList<String>(liveActivities.keySet());
    }

    /** A running live activity, or null. Ended activities disappear from this lookup. */
    public synchronized LiveActivityRecord getLiveActivity(String activityId) {
        return liveActivities.get(activityId);
    }

    /** Registers a surface window observer. */
    public synchronized void addListener(Listener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /** Removes a surface window observer. */
    public synchronized void removeListener(Listener l) {
        listeners.remove(l);
    }

    /**
     * Attaches the desktop floating-window manager (desktop mode only). Also used to answer
     * {@link #getInstalledWidgetCount}.
     */
    void setDesktopWindows(JavaSEWidgetWindows windows) {
        this.desktopWindows = windows;
    }

    /**
     * Programmatic pinning entry point for desktop mode. There is no guaranteed tray
     * infrastructure in a desktop build (the port only creates a tray icon when the app shows a
     * desktop notification, and {@link JavaSEWidgetWindows#installTrayMenu} hooks it when it
     * appears), so apps can pin a floating widget for the user through this call. A no-op in the
     * simulator, where the Widgets preview window plays this role.
     *
     * @param kindId the widget kind to pin
     * @param sizeName {@code small} / {@code medium} / {@code large}, null for small
     */
    public void pinWidget(String kindId, String sizeName) {
        if (desktopWindows != null) {
            desktopWindows.pinWidget(kindId, sizeName);
        }
    }

    // --- shared rendering plumbing ----------------------------------------------

    /** Receives the pixels of an asynchronous rasterization on the AWT dispatch thread. */
    public interface RenderCallback {
        /**
         * @param image the rasterized pixels, {@code scale}x the logical size
         * @param actions hit rectangles in image (scaled) pixels
         * @param nextTickMillis 0 for static content, else the epoch time a re-render is due
         */
        void rendered(BufferedImage image, List<SurfaceRasterizer.ActionRect> actions,
                long nextTickMillis);
    }

    /**
     * Rasterizes a descriptor node asynchronously honoring the surfaces thread rule: the
     * rasterization runs on the Codename One EDT (never on the AWT thread) and the callback is
     * delivered on the AWT dispatch thread for blitting.
     *
     * @param node parsed descriptor node (dips)
     * @param state state map of the active entry / live activity
     * @param images PNG blobs by wire name
     * @param logicalWidth logical (dip) width
     * @param logicalHeight logical (dip) height
     * @param scale integer pixel scale (2 gives retina-crisp output)
     * @param dark dark mode colors
     * @param callback receives the result on the AWT thread
     */
    public static void renderAsync(final Map<String, Object> node,
            final Map<String, Object> state, final Map<String, byte[]> images,
            final int logicalWidth, final int logicalHeight, final int scale, final boolean dark,
            final RenderCallback callback) {
        if (!Display.isInitialized()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> scaled = scaleNodeDips(node, scale);
                final SurfaceRasterizer.Result result = SurfaceRasterizer.rasterize(scaled, state,
                        images, logicalWidth * scale, logicalHeight * scale, dark,
                        System.currentTimeMillis());
                final BufferedImage img = new BufferedImage(logicalWidth * scale,
                        logicalHeight * scale, BufferedImage.TYPE_INT_ARGB);
                img.setRGB(0, 0, logicalWidth * scale, logicalHeight * scale, result.getArgb(),
                        0, logicalWidth * scale);
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        callback.rendered(img, result.getActions(), result.getNextTickMillis());
                    }
                });
            }
        });
    }

    /**
     * Deep-copies a descriptor node multiplying every dip-valued attribute by {@code scale}, so
     * the rasterizer (which treats dips as pixels) produces a crisp scaled rendering. The action
     * rectangles it returns are then in scaled pixels as well.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> scaleNodeDips(Map<String, Object> node, int scale) {
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

    // --- persistence -------------------------------------------------------------

    private File kindDir(String kindId) {
        File dir = new File(root, sanitize(kindId));
        dir.mkdirs();
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

    /** Atomic write-rename so the surface windows never read a torn file. */
    private static void writeFileSafely(File file, byte[] data) {
        try {
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            OutputStream os = new FileOutputStream(tmp);
            try {
                os.write(data);
            } finally {
                os.close();
            }
            if (file.exists() && !file.delete()) {
                return;
            }
            tmp.renameTo(file);
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    private void restoreFromDisk() {
        File[] dirs = root.listFiles();
        if (dirs == null) {
            return;
        }
        for (File dir : dirs) {
            if (!dir.isDirectory()) {
                continue;
            }
            byte[] kindData = readFile(new File(dir, "kind.json"));
            byte[] timelineData = readFile(new File(dir, "timeline.json"));
            String kindId = dir.getName();
            if (kindData != null) {
                Map<String, Object> kind = parse(new String(kindData, StandardCharsets.UTF_8));
                if (kind != null && kind.get("id") instanceof String) {
                    kindId = (String) kind.get("id");
                    kinds.put(kindId, kind);
                }
            }
            if (timelineData != null) {
                Map<String, Object> doc =
                        parse(new String(timelineData, StandardCharsets.UTF_8));
                if (doc != null) {
                    timelines.put(kindId, doc);
                    kindImages.put(kindId, readImages(dir));
                }
            }
        }
    }

    private static Map<String, byte[]> readImages(File dir) {
        Map<String, byte[]> images = new HashMap<String, byte[]>();
        File[] files = dir.listFiles();
        if (files == null) {
            return images;
        }
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".png")) {
                byte[] data = readFile(f);
                if (data != null) {
                    images.put(name.substring(0, name.length() - 4), data);
                }
            }
        }
        return images;
    }

    private static byte[] readFile(File f) {
        if (!f.exists()) {
            return null;
        }
        try {
            InputStream is = new FileInputStream(f);
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len = is.read(buf);
                while (len > -1) {
                    bo.write(buf, 0, len);
                    len = is.read(buf);
                }
                return bo.toByteArray();
            } finally {
                is.close();
            }
        } catch (IOException err) {
            err.printStackTrace();
            return null;
        }
    }

    private synchronized List<Listener> snapshotListeners() {
        return new ArrayList<Listener>(listeners);
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
