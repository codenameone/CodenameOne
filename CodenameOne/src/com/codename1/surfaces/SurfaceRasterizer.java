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

import com.codename1.io.Log;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A shared software renderer for desktop ports (the JavaSE simulator, the native Windows and
/// Linux ports): draws a parsed surface descriptor node into a mutable Codename One `Image` using
/// only `Graphics` primitives -- no `Container`/`Label` components and no theme dependence, so the
/// output looks the same on every desktop surface. Must run on the EDT (it creates images and
/// measures fonts).
///
/// The input is the wire format produced by `SurfaceSerializer` parsed back with
/// `com.codename1.io.JSONParser` -- numbers may arrive as `Double` or `Long`, both are handled.
/// Alongside the pixels the rasterizer returns the absolute hit rectangles of every node carrying
/// an `action`, plus the epoch time when a re-render is due for time-driven content (countdowns,
/// clocks, date-interval progress).
public final class SurfaceRasterizer {
    /// Rendering fidelity note: image `tint` is intentionally not applied by this preview
    /// rasterizer -- template-image tinting requires per-pixel masking that core `Graphics`
    /// primitives do not offer cheaply. The image renders untinted, which is acceptable preview
    /// fidelity for desktop surfaces; mobile platforms tint natively.
    private static final int ROLE_LABEL_LIGHT = 0xff1c1c1e;
    private static final int ROLE_LABEL_DARK = 0xffffffff;
    // solid approximations of the translucent iOS secondary label colors, avoiding
    // alpha-blend complexity in the preview
    private static final int ROLE_SECONDARY_LIGHT = 0xff6c6c70;
    private static final int ROLE_SECONDARY_DARK = 0xffaeaeb2;
    private static final int ROLE_BACKGROUND_LIGHT = 0xffffffff;
    private static final int ROLE_BACKGROUND_DARK = 0xff1c1c1e;
    private static final int ROLE_ACCENT = 0xff007aff;

    private static final int DEFAULT_FONT_SIZE = 14;
    private static final long MINUTE_MILLIS = 60000L;
    private static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    /// Decoded PNGs keyed by their content-hash wire name; the name uniquely identifies the bytes
    /// so entries never go stale. Trimmed when it grows past a small cap.
    private static final Map<String, Image> decodedImages = new HashMap<String, Image>();
    private static final int DECODED_IMAGE_CACHE_CAP = 48;

    private SurfaceRasterizer() {
    }

    /// The absolute bounds of a node that carries a tap action, in pixels of the rasterized image.
    public static class ActionRect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String actionId;
        private final Map<String, Object> params;

        ActionRect(int x, int y, int width, int height, String actionId,
                Map<String, Object> params) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.actionId = actionId;
            this.params = params;
        }

        /// Returns the left edge in pixels.
        public int getX() {
            return x;
        }

        /// Returns the top edge in pixels.
        public int getY() {
            return y;
        }

        /// Returns the width in pixels.
        public int getWidth() {
            return width;
        }

        /// Returns the height in pixels.
        public int getHeight() {
            return height;
        }

        /// Returns the app-defined action id.
        public String getActionId() {
            return actionId;
        }

        /// Returns the action parameters, or null.
        public Map<String, Object> getParams() {
            return params;
        }
    }

    /// The output of a rasterization pass: pixels, action hit rectangles and the next re-render
    /// deadline.
    public static class Result {
        private final Image image;
        private final int[] argb;
        private final List<ActionRect> actions;
        private final long nextTickMillis;

        Result(Image image, int[] argb, List<ActionRect> actions, long nextTickMillis) {
            this.image = image;
            this.argb = argb;
            this.actions = actions;
            this.nextTickMillis = nextTickMillis;
        }

        /// Returns the rasterized image.
        public Image getImage() {
            return image;
        }

        /// Returns the width*height ARGB pixels of the rasterized image.
        public int[] getArgb() {
            return argb;
        }

        /// Returns the hit rectangles of every node that carries an action, in image pixels.
        public List<ActionRect> getActions() {
            return actions;
        }

        /// Returns 0 when the content is static; otherwise the epoch millis when a re-render is
        /// due -- a one second cadence while a timer countdown or a date-interval progress is
        /// visible, the next minute boundary for clock and relative text.
        public long getNextTickMillis() {
            return nextTickMillis;
        }
    }

    /// Rasterizes a descriptor node into a transparent image of the requested size. Must run on
    /// the EDT.
    ///
    /// #### Parameters
    ///
    /// - `node`: a parsed descriptor node map as produced by the serializer and parsed back with
    ///   `com.codename1.io.JSONParser`
    /// - `state`: the current entry's state map, may be null
    /// - `images`: PNG blobs keyed by wire name, may be null
    /// - `width`: target width in pixels
    /// - `height`: target height in pixels
    /// - `dark`: true to resolve dark-mode colors
    /// - `now`: the current epoch millis used for dynamic text and interval progress
    ///
    /// #### Returns
    ///
    /// the rasterization result
    public static Result rasterize(Map<String, Object> node, Map<String, Object> state,
            Map<String, byte[]> images, int width, int height, boolean dark, long now) {
        Image target = Image.createImage(width, height, 0);
        List<ActionRect> actions = new ArrayList<ActionRect>();
        long[] tick = new long[1];
        if (node != null) {
            LNode root = build(node);
            measure(root, state, images, now);
            arrange(root, 0, 0, width, height);
            Graphics g = target.getGraphics();
            draw(g, root, state, images, dark, now, actions, tick);
        }
        return new Result(target, target.getRGB(), actions, tick[0]);
    }

    // --- timeline helpers shared by all desktop ports -------------------------

    /// Returns the timeline entry whose date most recently passed, or the first entry when none
    /// passed yet, or null for an entry-less document.
    ///
    /// #### Parameters
    ///
    /// - `timelineDoc`: a parsed timeline JSON document
    /// - `now`: the current epoch millis
    ///
    /// #### Returns
    ///
    /// the active entry map (`date`, `state`), or null
    public static Map<String, Object> currentEntry(Map<String, Object> timelineDoc, long now) {
        List<Map<String, Object>> entries = entriesOf(timelineDoc);
        if (entries.isEmpty()) {
            return null;
        }
        Map<String, Object> current = null;
        for (Map<String, Object> e : entries) {
            if (asLong(e.get("date"), 0) <= now) {
                current = e;
            }
        }
        if (current == null) {
            current = entries.get(0);
        }
        return current;
    }

    /// Returns the epoch millis of the next entry flip after `now`, or 0 when no future entry
    /// exists.
    ///
    /// #### Parameters
    ///
    /// - `timelineDoc`: a parsed timeline JSON document
    /// - `now`: the current epoch millis
    ///
    /// #### Returns
    ///
    /// the next flip time, or 0
    public static long nextEntryFlip(Map<String, Object> timelineDoc, long now) {
        long next = 0;
        for (Map<String, Object> e : entriesOf(timelineDoc)) {
            long date = asLong(e.get("date"), 0);
            if (date > now && (next == 0 || date < next)) {
                next = date;
            }
        }
        return next;
    }

    /// Picks the layout of a timeline document for a size name (`small` / `medium` / `large` /
    /// `lockscreen`): the explicit per-size layout when present, else the `default` layout, else
    /// null.
    ///
    /// #### Parameters
    ///
    /// - `timelineDoc`: a parsed timeline JSON document
    /// - `sizeName`: the wire-format size name
    ///
    /// #### Returns
    ///
    /// the layout's root node map, or null
    @SuppressWarnings("unchecked")
    public static Map<String, Object> layoutForSize(Map<String, Object> timelineDoc,
            String sizeName) {
        if (timelineDoc == null) {
            return null;
        }
        Object layoutsObj = timelineDoc.get("layouts");
        if (!(layoutsObj instanceof Map)) {
            return null;
        }
        Map<String, Object> layouts = (Map<String, Object>) layoutsObj;
        Object layout = sizeName == null ? null : layouts.get(sizeName);
        if (!(layout instanceof Map)) {
            layout = layouts.get("default");
        }
        return layout instanceof Map ? (Map<String, Object>) layout : null;
    }

    // --- dynamic text ----------------------------------------------------------

    /// Formats a dynamic-text value the way the OS-native views would show it. Package-private so
    /// unit tests can cover the formatting without a `Display`.
    ///
    /// #### Parameters
    ///
    /// - `style`: the wire style name (`timerDown`, `timerUp`, `time`, `date`, `relative`)
    /// - `dateMillis`: the target epoch millis
    /// - `now`: the current epoch millis
    static String formatDynamicText(String style, long dateMillis, long now) {
        if ("timerUp".equals(style)) {
            return formatTimer(now - dateMillis);
        }
        if ("time".equals(style)) {
            Calendar c = Calendar.getInstance();
            c.setTime(new Date(dateMillis));
            int hour = c.get(Calendar.HOUR);
            if (hour == 0) {
                hour = 12;
            }
            int minute = c.get(Calendar.MINUTE);
            String ampm = c.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            return hour + ":" + (minute < 10 ? "0" : "") + minute + " " + ampm;
        }
        if ("date".equals(style)) {
            Calendar c = Calendar.getInstance();
            c.setTime(new Date(dateMillis));
            return MONTHS[c.get(Calendar.MONTH)] + " " + c.get(Calendar.DAY_OF_MONTH);
        }
        if ("relative".equals(style)) {
            return formatRelative(dateMillis, now);
        }
        // timerDown is the default style
        return formatTimer(dateMillis - now);
    }

    private static String formatTimer(long millis) {
        long total = millis / 1000;
        if (total < 0) {
            total = 0;
        }
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(':');
            if (minutes < 10) {
                sb.append('0');
            }
        }
        sb.append(minutes).append(':');
        if (seconds < 10) {
            sb.append('0');
        }
        sb.append(seconds);
        return sb.toString();
    }

    private static String formatRelative(long dateMillis, long now) {
        long diff = dateMillis - now;
        boolean future = diff >= 0;
        long minutes = Math.abs(diff) / MINUTE_MILLIS;
        String amount;
        if (minutes < 60) {
            if (minutes < 1) {
                return "now";
            }
            amount = minutes + " min";
        } else if (minutes < 24 * 60) {
            amount = (minutes / 60) + " hr";
        } else {
            long days = minutes / (24 * 60);
            amount = days + (days == 1 ? " day" : " days");
        }
        return future ? "in " + amount : amount + " ago";
    }

    // --- internal layout tree --------------------------------------------------

    private static final class LNode {
        Map<String, Object> map;
        String type;
        List<LNode> children;
        int prefW;
        int prefH;
        int x;
        int y;
        int w;
        int h;
    }

    @SuppressWarnings("unchecked")
    private static LNode build(Map<String, Object> map) {
        LNode n = new LNode();
        n.map = map;
        Object t = map.get("t");
        n.type = t instanceof String ? (String) t : "box";
        n.children = new ArrayList<LNode>();
        Object ch = map.get("ch");
        if (ch instanceof List) {
            for (Object child : (List<Object>) ch) {
                if (child instanceof Map) {
                    n.children.add(build((Map<String, Object>) child));
                }
            }
        }
        return n;
    }

    private static void measure(LNode n, Map<String, Object> state, Map<String, byte[]> images,
            long now) {
        for (LNode child : n.children) {
            measure(child, state, images, now);
        }
        int cw = 0;
        int chh = 0;
        if ("col".equals(n.type)) {
            int spacing = asInt(n.map.get("spacing"), 0);
            for (LNode child : n.children) {
                cw = Math.max(cw, child.prefW);
                chh += child.prefH;
            }
            if (n.children.size() > 1) {
                chh += spacing * (n.children.size() - 1);
            }
        } else if ("row".equals(n.type)) {
            int spacing = asInt(n.map.get("spacing"), 0);
            for (LNode child : n.children) {
                chh = Math.max(chh, child.prefH);
                cw += child.prefW;
            }
            if (n.children.size() > 1) {
                cw += spacing * (n.children.size() - 1);
            }
        } else if ("box".equals(n.type)) {
            for (LNode child : n.children) {
                cw = Math.max(cw, child.prefW);
                chh = Math.max(chh, child.prefH);
            }
        } else if ("text".equals(n.type)) {
            Font f = fontOf(n.map);
            String s = interpolate(asString(n.map.get("text"), ""), state);
            cw = f.stringWidth(s);
            chh = f.getHeight();
        } else if ("dyn".equals(n.type)) {
            Font f = fontOf(n.map);
            String s = dynamicTextOf(n.map, state, now);
            cw = f.stringWidth(s);
            chh = f.getHeight();
        } else if ("img".equals(n.type)) {
            Image img = decodeImage(asString(n.map.get("name"), null), images);
            if (img != null) {
                cw = img.getWidth();
                chh = img.getHeight();
            } else {
                cw = 24;
                chh = 24;
            }
        } else if ("prog".equals(n.type)) {
            if ("circular".equals(n.map.get("style"))) {
                cw = 28;
                chh = 28;
            } else {
                cw = 120;
                chh = 6;
            }
        } else if ("spacer".equals(n.type)) {
            // the min length applies along the parent's axis; using it on both axes is harmless
            // because the cross axis of a spacer never dominates a real sibling
            int min = asInt(n.map.get("min"), 0);
            cw = min;
            chh = min;
        }
        int[] pad = paddingOf(n.map);
        n.prefW = cw + pad[1] + pad[3];
        n.prefH = chh + pad[0] + pad[2];
        int fixedW = asInt(n.map.get("w"), 0);
        int fixedH = asInt(n.map.get("h"), 0);
        if (fixedW > 0) {
            n.prefW = fixedW;
        }
        if (fixedH > 0) {
            n.prefH = fixedH;
        }
    }

    private static void arrange(LNode n, int x, int y, int w, int h) {
        n.x = x;
        n.y = y;
        n.w = w;
        n.h = h;
        if (n.children.isEmpty()) {
            return;
        }
        int[] pad = paddingOf(n.map);
        int cx = x + pad[3];
        int cy = y + pad[0];
        int cw = Math.max(0, w - pad[1] - pad[3]);
        int ch = Math.max(0, h - pad[0] - pad[2]);
        if ("col".equals(n.type)) {
            arrangeLinear(n, cx, cy, cw, ch, true);
        } else if ("row".equals(n.type)) {
            arrangeLinear(n, cx, cy, cw, ch, false);
        } else {
            // box: overlay children aligned by their own alignment, default center
            for (LNode child : n.children) {
                int bw = Math.min(child.prefW, cw);
                int bh = Math.min(child.prefH, ch);
                int[] align = alignmentOf(child.map);
                int bx = alignPos(align[0], cx, cw, bw);
                int by = alignPos(align[1], cy, ch, bh);
                arrange(child, bx, by, bw, bh);
            }
        }
    }

    private static void arrangeLinear(LNode n, int cx, int cy, int cw, int ch, boolean vertical) {
        int spacing = asInt(n.map.get("spacing"), 0);
        int contentMain = vertical ? ch : cw;
        int naturalSum = 0;
        int totalWeight = 0;
        for (LNode child : n.children) {
            naturalSum += vertical ? child.prefH : child.prefW;
            totalWeight += weightOf(child);
        }
        if (n.children.size() > 1) {
            naturalSum += spacing * (n.children.size() - 1);
        }
        int leftover = Math.max(0, contentMain - naturalSum);
        int cursor = vertical ? cy : cx;
        for (LNode child : n.children) {
            int weight = weightOf(child);
            int extra = 0;
            if (weight > 0 && totalWeight > 0) {
                extra = leftover * weight / totalWeight;
            }
            int[] align = alignmentOf(child.map);
            if (vertical) {
                int mainSize = child.prefH + extra;
                int crossSize = Math.min(child.prefW, cw);
                int bx = alignPos(align[0], cx, cw, crossSize);
                arrange(child, bx, cursor, crossSize, mainSize);
                cursor += mainSize + spacing;
            } else {
                int mainSize = child.prefW + extra;
                int crossSize = Math.min(child.prefH, ch);
                int by = alignPos(align[1], cy, ch, crossSize);
                arrange(child, cursor, by, mainSize, crossSize);
                cursor += mainSize + spacing;
            }
        }
    }

    /// Spacers flex by default (implied weight 1) so they absorb leftover space even without an
    /// explicit weight.
    private static int weightOf(LNode n) {
        int weight = asInt(n.map.get("weight"), 0);
        if (weight == 0 && "spacer".equals(n.type)) {
            return 1;
        }
        return weight;
    }

    private static int alignPos(int align, int start, int available, int size) {
        if (align < 0) {
            return start;
        }
        if (align > 0) {
            return start + available - size;
        }
        return start + (available - size) / 2;
    }

    // --- drawing ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void draw(Graphics g, LNode n, Map<String, Object> state,
            Map<String, byte[]> images, boolean dark, long now, List<ActionRect> actions,
            long[] tick) {
        int[] savedClip = g.getClip();
        g.clipRect(n.x, n.y, n.w, n.h);
        Object bg = n.map.get("bg");
        if (bg != null) {
            int argb = resolveColor(bg, dark, 0);
            int corner = asInt(n.map.get("corner"), 0);
            setPaint(g, argb);
            if (corner > 0) {
                g.fillRoundRect(n.x, n.y, n.w, n.h, corner * 2, corner * 2);
            } else {
                g.fillRect(n.x, n.y, n.w, n.h);
            }
            g.setAlpha(255);
        }
        Object action = n.map.get("action");
        if (action instanceof Map) {
            Map<String, Object> a = (Map<String, Object>) action;
            Object id = a.get("id");
            if (id instanceof String) {
                Object p = a.get("p");
                actions.add(new ActionRect(n.x, n.y, n.w, n.h, (String) id,
                        p instanceof Map ? (Map<String, Object>) p : null));
            }
        }
        int[] pad = paddingOf(n.map);
        int cx = n.x + pad[3];
        int cy = n.y + pad[0];
        int cw = Math.max(0, n.w - pad[1] - pad[3]);
        int ch = Math.max(0, n.h - pad[0] - pad[2]);
        if ("text".equals(n.type)) {
            drawText(g, n.map, interpolate(asString(n.map.get("text"), ""), state),
                    cx, cy, cw, ch, dark);
        } else if ("dyn".equals(n.type)) {
            drawText(g, n.map, dynamicTextOf(n.map, state, now), cx, cy, cw, ch, dark);
            registerDynTick(n.map, tick, now);
        } else if ("img".equals(n.type)) {
            drawImageNode(g, n.map, images, cx, cy, cw, ch);
        } else if ("prog".equals(n.type)) {
            drawProgress(g, n.map, state, cx, cy, cw, ch, dark, now, tick);
        } else {
            for (LNode child : n.children) {
                draw(g, child, state, images, dark, now, actions, tick);
            }
        }
        g.setClip(savedClip);
    }

    private static void drawText(Graphics g, Map<String, Object> map, String text, int cx, int cy,
            int cw, int ch, boolean dark) {
        if (text == null || text.length() == 0) {
            return;
        }
        Font f = fontOf(map);
        g.setFont(f);
        setPaint(g, resolveColor(map.get("color"), dark,
                dark ? ROLE_LABEL_DARK : ROLE_LABEL_LIGHT));
        int tw = f.stringWidth(text);
        int th = f.getHeight();
        int tx = cx + Math.max(0, (cw - tw) / 2);
        int ty = cy + Math.max(0, (ch - th) / 2);
        g.drawString(text, tx, ty);
        g.setAlpha(255);
    }

    private static void drawImageNode(Graphics g, Map<String, Object> map,
            Map<String, byte[]> images, int cx, int cy, int cw, int ch) {
        Image img = decodeImage(asString(map.get("name"), null), images);
        if (img == null || cw <= 0 || ch <= 0) {
            return;
        }
        // NOTE: the tint attribute is ignored here, see the class comment
        String scale = asString(map.get("scale"), "fit");
        int iw = img.getWidth();
        int ih = img.getHeight();
        if ("center".equals(scale)) {
            g.drawImage(img, cx + (cw - iw) / 2, cy + (ch - ih) / 2);
            return;
        }
        double ratioFit = Math.min((double) cw / iw, (double) ch / ih);
        double ratioFill = Math.max((double) cw / iw, (double) ch / ih);
        double ratio = "fill".equals(scale) ? ratioFill : ratioFit;
        int dw = Math.max(1, (int) (iw * ratio));
        int dh = Math.max(1, (int) (ih * ratio));
        g.drawImage(img, cx + (cw - dw) / 2, cy + (ch - dh) / 2, dw, dh);
    }

    private static void drawProgress(Graphics g, Map<String, Object> map,
            Map<String, Object> state, int cx, int cy, int cw, int ch, boolean dark, long now,
            long[] tick) {
        if (cw <= 0 || ch <= 0) {
            return;
        }
        double fraction = progressFraction(map, state, now, tick);
        int color = resolveColor(map.get("color"), dark, ROLE_ACCENT);
        if ("circular".equals(map.get("style"))) {
            int d = Math.min(cw, ch);
            int px = cx + (cw - d) / 2;
            int py = cy + (ch - d) / 2;
            setPaint(g, color);
            g.setAlpha(51);
            g.fillArc(px, py, d, d, 0, 360);
            g.setAlpha(alphaOf(color));
            // start at 12 o'clock and sweep clockwise
            g.fillArc(px, py, d, d, 90, -(int) Math.round(360 * fraction));
        } else {
            int barH = Math.min(ch, 6);
            int by = cy + (ch - barH) / 2;
            setPaint(g, color);
            g.setAlpha(51);
            g.fillRoundRect(cx, by, cw, barH, barH, barH);
            g.setAlpha(alphaOf(color));
            int fillW = (int) Math.round(cw * fraction);
            if (fillW > 0) {
                g.fillRoundRect(cx, by, fillW, barH, barH, barH);
            }
        }
        g.setAlpha(255);
    }

    private static double progressFraction(Map<String, Object> map, Map<String, Object> state,
            long now, long[] tick) {
        double fraction = 0;
        String valueKey = asString(map.get("valueKey"), null);
        Object startObj = map.get("start");
        if (valueKey != null) {
            Object v = state == null ? null : state.get(valueKey);
            if (v instanceof Number) {
                fraction = ((Number) v).doubleValue();
            }
        } else if (startObj instanceof Number && map.get("end") instanceof Number) {
            long start = asLong(startObj, 0);
            long end = asLong(map.get("end"), 0);
            if (end > start) {
                fraction = (now - start) / (double) (end - start);
                if (now < end) {
                    // the interval animates on the OS clock; tick every second while it runs
                    requestTick(tick, now + 1000);
                }
            }
        } else if (map.get("value") instanceof Number) {
            fraction = ((Number) map.get("value")).doubleValue();
        }
        if (fraction < 0) {
            return 0;
        }
        if (fraction > 1) {
            return 1;
        }
        return fraction;
    }

    private static void registerDynTick(Map<String, Object> map, long[] tick, long now) {
        Object style = map.get("style");
        if ("timerDown".equals(style) || "timerUp".equals(style)) {
            requestTick(tick, now + 1000);
        } else if ("time".equals(style) || "relative".equals(style)) {
            requestTick(tick, now - now % MINUTE_MILLIS + MINUTE_MILLIS);
        }
    }

    private static void requestTick(long[] tick, long when) {
        if (tick[0] == 0 || when < tick[0]) {
            tick[0] = when;
        }
    }

    private static String dynamicTextOf(Map<String, Object> map, Map<String, Object> state,
            long now) {
        long date = 0;
        String dateKey = asString(map.get("dateKey"), null);
        if (dateKey != null) {
            Object v = state == null ? null : state.get(dateKey);
            if (v instanceof Number) {
                date = ((Number) v).longValue();
            } else {
                return "";
            }
        } else if (map.get("date") instanceof Number) {
            date = asLong(map.get("date"), 0);
        } else {
            return "";
        }
        return formatDynamicText(asString(map.get("style"), "timerDown"), date, now);
    }

    // --- attribute helpers --------------------------------------------------------

    /// Replaces `${key}` placeholders with `String.valueOf` of the state value; missing keys
    /// render as an empty string. Package-private for unit tests.
    static String interpolate(String text, Map<String, Object> state) {
        if (text == null || text.indexOf("${") < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        int len = text.length();
        while (i < len) {
            int start = text.indexOf("${", i);
            if (start < 0) {
                sb.append(text.substring(i));
                break;
            }
            int end = text.indexOf('}', start + 2);
            if (end < 0) {
                sb.append(text.substring(i));
                break;
            }
            sb.append(text.substring(i, start));
            String key = text.substring(start + 2, end);
            Object value = state == null ? null : state.get(key);
            if (value != null) {
                sb.append(String.valueOf(value));
            }
            i = end + 1;
        }
        return sb.toString();
    }

    /// Resolves a wire color (`{"l":..,"d":..}` pair or `{"role":..}`) to ARGB. Package-private
    /// for unit tests.
    @SuppressWarnings("unchecked")
    static int resolveColor(Object colorObj, boolean dark, int fallback) {
        if (!(colorObj instanceof Map)) {
            return fallback;
        }
        Map<String, Object> m = (Map<String, Object>) colorObj;
        Object role = m.get("role");
        if (role instanceof String) {
            if ("label".equals(role)) {
                return dark ? ROLE_LABEL_DARK : ROLE_LABEL_LIGHT;
            }
            if ("secondaryLabel".equals(role)) {
                return dark ? ROLE_SECONDARY_DARK : ROLE_SECONDARY_LIGHT;
            }
            if ("background".equals(role)) {
                return dark ? ROLE_BACKGROUND_DARK : ROLE_BACKGROUND_LIGHT;
            }
            if ("accent".equals(role)) {
                return ROLE_ACCENT;
            }
            return fallback;
        }
        Object v = dark ? m.get("d") : m.get("l");
        if (v instanceof Number) {
            return (int) ((Number) v).longValue();
        }
        return fallback;
    }

    /// Applies an ARGB color to the graphics: RGB via `setColor`, alpha via `setAlpha`. The
    /// alpha byte is honored verbatim (a fully transparent color draws nothing), matching how
    /// the iOS and Android renderers consume the same wire value.
    private static void setPaint(Graphics g, int argb) {
        g.setColor(argb & 0xffffff);
        g.setAlpha(alphaOf(argb));
    }

    private static int alphaOf(int argb) {
        return (argb >>> 24) & 0xff;
    }

    private static Font fontOf(Map<String, Object> map) {
        int size = asInt(map.get("size"), 0);
        int px = size <= 0 ? DEFAULT_FONT_SIZE : size;
        String weight = asString(map.get("fw"), "regular");
        boolean bold = "semibold".equals(weight) || "bold".equals(weight);
        try {
            // exact pixel sizing with weights when the port ships native: fonts
            String name;
            if (bold) {
                name = "native:MainBold";
            } else if ("light".equals(weight)) {
                name = "native:MainLight";
            } else {
                name = "native:MainRegular";
            }
            Font f = Font.createTrueTypeFont(name, name);
            if (f != null) {
                return f.derive(px, Font.STYLE_PLAIN);
            }
        } catch (Throwable ignored) {
            // fall through to the coarse system font buckets
        }
        int bucket;
        if (px <= 12) {
            bucket = Font.SIZE_SMALL;
        } else if (px <= 18) {
            bucket = Font.SIZE_MEDIUM;
        } else {
            bucket = Font.SIZE_LARGE;
        }
        return Font.createSystemFont(Font.FACE_SYSTEM,
                bold ? Font.STYLE_BOLD : Font.STYLE_PLAIN, bucket);
    }

    private static int[] paddingOf(Map<String, Object> map) {
        int[] pad = new int[4];
        Object p = map.get("pad");
        if (p instanceof List) {
            List<?> list = (List<?>) p;
            for (int i = 0; i < 4 && i < list.size(); i++) {
                pad[i] = asInt(list.get(i), 0);
            }
        }
        return pad;
    }

    /// Returns `{horizontal, vertical}` alignment components, each -1 (leading/top), 0 (center)
    /// or 1 (trailing/bottom). Defaults to center.
    private static int[] alignmentOf(Map<String, Object> map) {
        String a = asString(map.get("align"), "center");
        int hAlign = 0;
        int vAlign = 0;
        if ("leading".equals(a) || "topLeading".equals(a) || "bottomLeading".equals(a)) {
            hAlign = -1;
        } else if ("trailing".equals(a) || "topTrailing".equals(a) || "bottomTrailing".equals(a)) {
            hAlign = 1;
        }
        if ("top".equals(a) || "topLeading".equals(a) || "topTrailing".equals(a)) {
            vAlign = -1;
        } else if ("bottom".equals(a) || "bottomLeading".equals(a) || "bottomTrailing".equals(a)) {
            vAlign = 1;
        }
        return new int[] {hAlign, vAlign};
    }

    private static Image decodeImage(String name, Map<String, byte[]> images) {
        if (name == null || name.length() == 0 || images == null) {
            return null;
        }
        Image cached = decodedImages.get(name);
        if (cached != null) {
            return cached;
        }
        byte[] data = images.get(name);
        if (data == null) {
            return null;
        }
        try {
            Image img = Image.createImage(new ByteArrayInputStream(data));
            if (decodedImages.size() >= DECODED_IMAGE_CACHE_CAP) {
                decodedImages.clear();
            }
            decodedImages.put(name, img);
            return img;
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entriesOf(Map<String, Object> timelineDoc) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (timelineDoc == null) {
            return result;
        }
        Object entries = timelineDoc.get("entries");
        if (entries instanceof List) {
            for (Object e : (List<Object>) entries) {
                if (e instanceof Map) {
                    result.add((Map<String, Object>) e);
                }
            }
        }
        return result;
    }

    private static int asInt(Object o, int def) {
        return o instanceof Number ? ((Number) o).intValue() : def;
    }

    private static long asLong(Object o, long def) {
        return o instanceof Number ? ((Number) o).longValue() : def;
    }

    private static String asString(Object o, String def) {
        return o instanceof String ? (String) o : def;
    }
}
