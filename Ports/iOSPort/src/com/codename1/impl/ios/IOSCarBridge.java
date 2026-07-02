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
package com.codename1.impl.ios;

import com.codename1.car.CarAction;
import com.codename1.car.CarActionListener;
import com.codename1.car.CarActionStrip;
import com.codename1.car.CarContext;
import com.codename1.car.CarGridItem;
import com.codename1.car.CarGridTemplate;
import com.codename1.car.CarListTemplate;
import com.codename1.car.CarMessageTemplate;
import com.codename1.car.CarNavigationTemplate;
import com.codename1.car.CarNowPlayingTemplate;
import com.codename1.car.CarPaneTemplate;
import com.codename1.car.CarRow;
import com.codename1.car.CarScreen;
import com.codename1.car.CarSection;
import com.codename1.car.CarTemplate;
import com.codename1.car.spi.CarBridge;
import com.codename1.io.Log;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// iOS `CarBridge` backing the `com.codename1.car` API with Apple CarPlay (CarPlay.framework). Each
/// `CarTemplate` is serialised to a compact JSON description and handed to the native CarPlay scene
/// delegate (`CodenameOne_CarPlaySceneDelegate`), which builds the matching `CPTemplate` tree.
/// Images are registered out of band as PNG blobs keyed by an opaque id. Element selections come
/// back through `IOSCarPlayCallbacks#nativeElementSelected(int, String)` and are dispatched to the
/// registered `CarActionListener` on the EDT.
///
/// This whole class is dead code unless the build linked the CarPlay natives (the `CN1_USE_CARPLAY`
/// define the builder flips when the app references `com.codename1.car`).
final class IOSCarBridge implements CarBridge {
    private final IOSNative nativeInstance;
    private int nextScreenId = 1;
    private boolean rootSet;

    // screenId -> (elementId -> listener) for the templates currently on the stack.
    private final Map<Integer, Map<String, CarActionListener>> listeners =
            new HashMap<Integer, Map<String, CarActionListener>>();
    // CarScreen identity -> screenId, so invalidate() can address the right native template.
    private final Map<CarScreen, Integer> screenIds = new HashMap<CarScreen, Integer>();

    IOSCarBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    public boolean isConnected() {
        return nativeInstance.isCarPlayConnected();
    }

    public void pushScreen(CarScreen screen) {
        int id = nextScreenId++;
        screenIds.put(screen, Integer.valueOf(id));
        String json = render(screen, id);
        boolean isRoot = !rootSet;
        rootSet = true;
        nativeInstance.carPlaySetTemplate(id, json, isRoot);
    }

    public void popScreen() {
        nativeInstance.carPlayPopTemplate();
    }

    public void invalidate(CarScreen screen) {
        Integer id = screenIds.get(screen);
        if (id == null) {
            return;
        }
        String json = render(screen, id.intValue());
        nativeInstance.carPlayUpdateTemplate(id.intValue(), json);
    }

    public void finish() {
        // CarPlay apps cannot programmatically dismiss themselves from the head unit; the closest
        // behaviour is to unwind to the root. Repeated pops are harmless on the root.
        nativeInstance.carPlayPopTemplate();
    }

    public void showToast(String message, int durationSeconds) {
        nativeInstance.carPlayShowToast(message, durationSeconds);
    }

    public int getListRowLimit() {
        return 0;
    }

    public int getGridItemLimit() {
        return 0;
    }

    // --- selection dispatch (called from IOSCarPlayCallbacks) ----------------

    CarActionListener takeListener(int screenId, String elementId) {
        Map<String, CarActionListener> m = listeners.get(Integer.valueOf(screenId));
        if (m == null) {
            return null;
        }
        return m.get(elementId);
    }

    // --- JSON rendering ------------------------------------------------------

    private String render(CarScreen screen, int screenId) {
        Map<String, CarActionListener> registry = new HashMap<String, CarActionListener>();
        listeners.put(Integer.valueOf(screenId), registry);
        CarTemplate t = screen.dispatchCreateTemplate();
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendStr(sb, "screenId", String.valueOf(screenId));
        Counter c = new Counter(screenId, registry);
        if (t instanceof CarListTemplate) {
            renderList(sb, (CarListTemplate) t, c);
        } else if (t instanceof CarGridTemplate) {
            renderGrid(sb, (CarGridTemplate) t, c);
        } else if (t instanceof CarPaneTemplate) {
            renderPane(sb, (CarPaneTemplate) t, c);
        } else if (t instanceof CarMessageTemplate) {
            renderMessage(sb, (CarMessageTemplate) t, c);
        } else if (t instanceof CarNavigationTemplate) {
            renderNavigation(sb, (CarNavigationTemplate) t, c);
        } else if (t instanceof CarNowPlayingTemplate) {
            renderNowPlaying(sb, (CarNowPlayingTemplate) t, c);
        } else {
            sb.append(",\"type\":\"unsupported\"");
        }
        sb.append('}');
        return sb.toString();
    }

    private void renderList(StringBuilder sb, CarListTemplate t, Counter c) {
        sb.append(",\"type\":\"list\"");
        appendStr(sb, "title", t.getTitle());
        appendBool(sb, "loading", t.isLoading());
        appendActions(sb, "headerActions", t.getHeaderActions(), c);
        sb.append(",\"sections\":[");
        List<CarSection> sections = t.getSections();
        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            CarSection s = sections.get(i);
            sb.append('{');
            appendStrFirst(sb, "header", s.getHeader());
            sb.append(",\"rows\":[");
            List<CarRow> rows = s.getRows();
            for (int j = 0; j < rows.size(); j++) {
                if (j > 0) {
                    sb.append(',');
                }
                renderRow(sb, rows.get(j), c, true);
            }
            sb.append("]}");
        }
        sb.append(']');
    }

    private void renderRow(StringBuilder sb, CarRow row, Counter c, boolean selectable) {
        String id = c.add(selectable ? row.getOnAction() : null);
        sb.append('{');
        appendStrFirst(sb, "id", id);
        appendStr(sb, "title", row.getTitle());
        appendStr(sb, "text", row.getText());
        appendBool(sb, "browsable", row.isBrowsable());
        appendImage(sb, "image", row.getImage(), c);
        sb.append('}');
    }

    private void renderGrid(StringBuilder sb, CarGridTemplate t, Counter c) {
        sb.append(",\"type\":\"grid\"");
        appendStr(sb, "title", t.getTitle());
        appendBool(sb, "loading", t.isLoading());
        appendActions(sb, "headerActions", t.getHeaderActions(), c);
        sb.append(",\"items\":[");
        List<CarGridItem> items = t.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            CarGridItem it = items.get(i);
            String id = c.add(it.getOnAction());
            sb.append('{');
            appendStrFirst(sb, "id", id);
            appendStr(sb, "title", it.getTitle());
            appendStr(sb, "text", it.getText());
            appendImage(sb, "image", it.getImage(), c);
            sb.append('}');
        }
        sb.append(']');
    }

    private void renderPane(StringBuilder sb, CarPaneTemplate t, Counter c) {
        sb.append(",\"type\":\"pane\"");
        appendStr(sb, "title", t.getTitle());
        appendBool(sb, "loading", t.isLoading());
        appendActions(sb, "headerActions", t.getHeaderActions(), c);
        sb.append(",\"rows\":[");
        List<CarRow> rows = t.getRows();
        for (int j = 0; j < rows.size(); j++) {
            if (j > 0) {
                sb.append(',');
            }
            renderRow(sb, rows.get(j), c, false);
        }
        sb.append(']');
        appendActionList(sb, "actions", t.getActions(), c);
    }

    private void renderMessage(StringBuilder sb, CarMessageTemplate t, Counter c) {
        sb.append(",\"type\":\"message\"");
        appendStr(sb, "title", t.getTitle());
        appendStr(sb, "message", t.getMessage());
        appendBool(sb, "loading", t.isLoading());
        appendImage(sb, "icon", t.getIcon(), c);
        appendActions(sb, "headerActions", t.getHeaderActions(), c);
        appendActionList(sb, "actions", t.getActions(), c);
    }

    private void renderNavigation(StringBuilder sb, CarNavigationTemplate t, Counter c) {
        sb.append(",\"type\":\"navigation\"");
        appendStr(sb, "title", t.getTitle());
        appendBool(sb, "navigating", t.isNavigating());
        appendStr(sb, "nextManeuver", t.getNextManeuver());
        appendStr(sb, "distanceRemaining", t.getDistanceRemaining());
        appendStr(sb, "timeRemaining", t.getTimeRemaining());
        appendActions(sb, "headerActions", t.getHeaderActions(), c);
        appendActions(sb, "mapActions", t.getMapActions(), c);
    }

    private void renderNowPlaying(StringBuilder sb, CarNowPlayingTemplate t, Counter c) {
        sb.append(",\"type\":\"nowplaying\"");
        appendBool(sb, "upNext", t.isUpNextVisible());
        appendActionList(sb, "actions", t.getActions(), c);
    }

    private void appendActions(StringBuilder sb, String key, CarActionStrip strip, Counter c) {
        if (strip == null) {
            return;
        }
        appendActionList(sb, key, strip.getActions(), c);
    }

    private void appendActionList(StringBuilder sb, String key, List<CarAction> actions, Counter c) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        sb.append(',').append('"').append(key).append("\":[");
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            CarAction a = actions.get(i);
            String id = c.add(a.getOnAction());
            sb.append('{');
            appendStrFirst(sb, "id", id);
            appendStr(sb, "title", a.getTitle());
            appendStr(sb, "color", a.getBackgroundColor() == null ? null : a.getBackgroundColor().name());
            appendImage(sb, "icon", a.getIcon(), c);
            sb.append('}');
        }
        sb.append(']');
    }

    private void appendImage(StringBuilder sb, String key, Image img, Counter c) {
        if (img == null) {
            return;
        }
        byte[] png = encode(img);
        if (png == null) {
            return;
        }
        String imgKey = "img" + c.screenId + "_" + (c.imageSeq++);
        nativeInstance.carPlayRegisterImage(imgKey, png);
        appendStr(sb, key, imgKey);
    }

    private byte[] encode(Image img) {
        try {
            if (img instanceof EncodedImage) {
                return ((EncodedImage) img).getImageData();
            }
            ImageIO io = ImageIO.getImageIO();
            if (io != null) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                io.save(img, bo, ImageIO.FORMAT_PNG, 1f);
                return bo.toByteArray();
            }
        } catch (Throwable t) {
            Log.e(t);
        }
        return null;
    }

    // --- JSON helpers --------------------------------------------------------

    private static void appendStr(StringBuilder sb, String key, String value) {
        if (value == null) {
            return;
        }
        sb.append(',').append('"').append(key).append("\":");
        appendQuoted(sb, value);
    }

    private static void appendStrFirst(StringBuilder sb, String key, String value) {
        if (value == null) {
            sb.append('"').append(key).append("\":null");
            return;
        }
        sb.append('"').append(key).append("\":");
        appendQuoted(sb, value);
    }

    private static void appendBool(StringBuilder sb, String key, boolean value) {
        sb.append(',').append('"').append(key).append("\":").append(value ? "true" : "false");
    }

    private static void appendQuoted(StringBuilder sb, String s) {
        sb.append('"');
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int p = hex.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(ch);
                    }
            }
        }
        sb.append('"');
    }

    /// Per-render element id allocator + listener registry + image sequence.
    private static final class Counter {
        final int screenId;
        final Map<String, CarActionListener> registry;
        int seq;
        int imageSeq;

        Counter(int screenId, Map<String, CarActionListener> registry) {
            this.screenId = screenId;
            this.registry = registry;
        }

        String add(CarActionListener l) {
            String id = "e" + (seq++);
            if (l != null) {
                registry.put(id, l);
            }
            return id;
        }
    }

    // exposed for the EDT dispatch in IOSCarPlayCallbacks
    static void invoke(final CarActionListener l, final CarContext ctx) {
        if (l == null) {
            return;
        }
        l.actionPerformed(ctx);
    }
}
