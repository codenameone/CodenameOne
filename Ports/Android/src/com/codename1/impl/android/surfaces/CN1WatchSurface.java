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
package com.codename1.impl.android.surfaces;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a published surface timeline for a Wear OS watch face, and reduces its node tree to the
 * handful of values a complication or a Tile can actually show.
 *
 * <p>This is the half of the lowering that touches no {@code androidx.wear} type, which is why
 * it lives in the port and is compiled by this repository. The generated
 * {@code CN1ComplicationDataSource} and {@code CN1SurfaceTileService} ship as build-time
 * resources because they must compile against libraries the port cannot depend on; keeping
 * everything else here is what lets CI catch a break in it.</p>
 *
 * <p><b>A complication is not a small widget.</b> A watch face asks for a typed value -- a
 * number, a short string, a ranged value, a monochrome glyph -- and composes it into its own
 * design. There is no layout to honour: padding, background, alignment, weight and colour are
 * all the face's business, not the app's. So the tree is flattened and mined for content rather
 * than rendered, and everything that cannot survive that is dropped and logged.</p>
 */
public final class CN1WatchSurface {

    private static final String TAG = "CN1Surfaces";

    /** Matches CN1SurfaceRenderer: deeper than this is a malformed descriptor, not a design. */
    private static final int MAX_DEPTH = 8;

    private CN1WatchSurface() {
    }

    /**
     * The content of one kind at one moment, already resolved to the layout and entry that
     * should be showing.
     */
    public static final class Reading {
        private final JSONObject layout;
        private final JSONObject state;
        private final long nextFlipDate;

        private final long start;
        private final boolean reloadAtEnd;

        Reading(JSONObject layout, JSONObject state, long nextFlipDate) {
            this(layout, state, nextFlipDate, 0L);
        }

        Reading(JSONObject layout, JSONObject state, long nextFlipDate, long start) {
            this(layout, state, nextFlipDate, start, true);
        }

        Reading(JSONObject layout, JSONObject state, long nextFlipDate, long start,
                boolean reloadAtEnd) {
            this.layout = layout;
            this.state = state;
            this.nextFlipDate = nextFlipDate;
            this.start = start;
            this.reloadAtEnd = reloadAtEnd;
        }

        /**
         * Whether the app asked to be woken when the timeline runs out.
         *
         * <p>{@code WidgetTimeline.RELOAD_AT_END} is the default and means the last entry stays on
         * screen while the app is asked -- throttled -- to publish fresh content. A widget already
         * honours it; a Tile that ignored it froze on its final entry for ever.</p>
         *
         * @return true for the default at-end policy, false for {@code RELOAD_NEVER}
         */
        public boolean isReloadAtEnd() {
            return reloadAtEnd;
        }

        /**
         * When this entry takes over, or 0 for the one that is current already.
         *
         * <p>Only meaningful for a reading that came from {@link #readTimeline}: a single
         * resolved reading is by definition the one showing now.</p>
         *
         * @return the entry's start in epoch millis, or 0
         */
        public long getStart() {
            return start;
        }

        public JSONObject getLayout() {
            return layout;
        }

        /** The entry's interpolation state; never null, so callers need no guard. */
        public JSONObject getState() {
            return state;
        }

        /**
         * When the next timeline entry becomes current, or 0 when none does.
         *
         * <p>A Tile turns this into its freshness interval and a complication into the point at
         * which it asks again, so an app that publishes entries covering the hours ahead is
         * refreshed by the system without ever being woken.</p>
         */
        public long getNextFlipDate() {
            return nextFlipDate;
        }
    }

    /**
     * Reads the timeline a kind last published and resolves it for one watch family.
     *
     * @param ctx any context
     * @param kindId the widget kind
     * @param family the portable family name, e.g. {@code watchCircular}
     * @return the resolved content, or null when nothing has been published
     */
    public static Reading read(Context ctx, String kindId, String family) {
        String json = CN1SurfaceStore.readWidgetTimeline(ctx, kindId);
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            JSONObject doc = new JSONObject(json);
            JSONObject layout = pickLayout(doc.optJSONObject("layouts"), family);
            if (layout == null) {
                return null;
            }
            JSONArray entries = doc.optJSONArray("entries");
            long now = System.currentTimeMillis();
            JSONObject entry = pickActiveEntry(entries, now);
            JSONObject state = entry == null ? new JSONObject() : entry.optJSONObject("state");
            return new Reading(layout, state == null ? new JSONObject() : state,
                    nextFlipDate(entries, now), 0L,
                    !"never".equals(doc.optString("reload", "atEnd")));
        } catch (Throwable t) {
            // A malformed descriptor must leave the face showing whatever it had, not crash the
            // data source -- which on Wear takes the whole watch face down with it.
            Log.w(TAG, "Could not read the published timeline for watch kind " + kindId, t);
            return null;
        }
    }

    /**
     * Reads every entry a kind published that is still ahead of it, resolved for one family.
     *
     * <p>A complication answers with a whole timeline rather than one value, and the system swaps
     * entries at the stated moments without waking anything -- so the entries the app published
     * for the hours ahead have to survive the read rather than being collapsed to whichever one
     * is current. The first element is the entry showing now; each later one carries the moment
     * it takes over in {@link Reading#getStart}.</p>
     *
     * @param ctx any context
     * @param kindId the widget kind
     * @param family the portable family name, e.g. {@code watchCircular}
     * @return the entries from now onward, or an empty list when nothing has been published
     */
    public static List<Reading> readTimeline(Context ctx, String kindId, String family) {
        List<Reading> out = new ArrayList<Reading>();
        String json = CN1SurfaceStore.readWidgetTimeline(ctx, kindId);
        if (json == null || json.length() == 0) {
            return out;
        }
        try {
            JSONObject doc = new JSONObject(json);
            JSONObject layout = pickLayout(doc.optJSONObject("layouts"), family);
            if (layout == null) {
                return out;
            }
            JSONArray entries = doc.optJSONArray("entries");
            long now = System.currentTimeMillis();
            JSONObject active = pickActiveEntry(entries, now);
            if (active != null) {
                JSONObject state = active.optJSONObject("state");
                out.add(new Reading(layout, state == null ? new JSONObject() : state,
                        nextFlipDate(entries, now), 0L,
                        !"never".equals(doc.optString("reload", "atEnd"))));
            }
            for (int i = 0; entries != null && i < entries.length(); i++) {
                JSONObject e = entries.optJSONObject(i);
                if (e == null) {
                    continue;
                }
                long date = e.optLong("date", 0);
                if (date <= now) {
                    // Already superseded, or the one already added above.
                    continue;
                }
                JSONObject state = e.optJSONObject("state");
                out.add(new Reading(layout, state == null ? new JSONObject() : state,
                        nextFlipDate(entries, date), date,
                        !"never".equals(doc.optString("reload", "atEnd"))));
            }
        } catch (Throwable t) {
            // Same contract as read(): a malformed descriptor leaves the face showing whatever it
            // had rather than taking the watch face down with the data source.
            Log.w(TAG, "Could not read the published timeline for watch kind " + kindId, t);
        }
        return out;
    }

    /**
     * Reads every entry a kind published, including the ones already superseded.
     *
     * <p>Unlike {@link #readTimeline} this does not drop the past, because its caller is not
     * asking what to show -- it is asking what it already showed. A Tile host requests resources
     * for the version the layout it is displaying advertised, and that layout can be an entry
     * behind by the time the request lands. The published descriptor is the only record of what
     * that entry was, so answering from it is what makes the answer survive a flip, a cache
     * eviction, and the service being torn down and rebuilt between the two callbacks.</p>
     *
     * @param ctx any context
     * @param kindId the widget kind
     * @param family the portable family name, e.g. {@code watchRectangular}
     * @return every entry, in published order, or an empty list when nothing has been published
     */
    public static List<Reading> readAllEntries(Context ctx, String kindId, String family) {
        List<Reading> out = new ArrayList<Reading>();
        String json = CN1SurfaceStore.readWidgetTimeline(ctx, kindId);
        if (json == null || json.length() == 0) {
            return out;
        }
        try {
            JSONObject doc = new JSONObject(json);
            JSONObject layout = pickLayout(doc.optJSONObject("layouts"), family);
            if (layout == null) {
                return out;
            }
            boolean reloadAtEnd = !"never".equals(doc.optString("reload", "atEnd"));
            JSONArray entries = doc.optJSONArray("entries");
            for (int i = 0; entries != null && i < entries.length(); i++) {
                JSONObject e = entries.optJSONObject(i);
                if (e == null) {
                    continue;
                }
                long date = e.optLong("date", 0);
                JSONObject state = e.optJSONObject("state");
                out.add(new Reading(layout, state == null ? new JSONObject() : state,
                        nextFlipDate(entries, date), date, reloadAtEnd));
            }
        } catch (Throwable t) {
            // Same contract as read(): a malformed descriptor leaves the face showing whatever it
            // had rather than taking the watch face down with the data source.
            Log.w(TAG, "Could not read the published timeline for watch kind " + kindId, t);
        }
        return out;
    }

    /**
     * Picks the layout for a family, substituting the way every other platform does.
     *
     * <p>{@code watchCorner} borrows the circular layout because Wear OS has no corner slot at
     * all and a corner complication is round; {@code watchRectangular} borrows the lock-screen
     * layout, which is the same family on Apple. Both are closer to what the developer designed
     * than {@code default}, which may well be a rectangular phone widget.</p>
     */
    static JSONObject pickLayout(JSONObject layouts, String family) {
        if (layouts == null) {
            return null;
        }
        JSONObject layout = family == null ? null : layouts.optJSONObject(family);
        if (layout == null && "watchCorner".equals(family)) {
            layout = layouts.optJSONObject("watchCircular");
        }
        if (layout == null && "watchRectangular".equals(family)) {
            layout = layouts.optJSONObject("lockscreen");
        }
        if (layout == null) {
            layout = layouts.optJSONObject("default");
        }
        if (layout == null) {
            // Last resort: any watch layout at all beats showing nothing, because a face that
            // asked for a type this kind offers will otherwise sit empty.
            String[] fallbacks = {"watchRectangular", "watchCircular", "watchInline", "medium"};
            for (String fallback : fallbacks) {
                layout = layouts.optJSONObject(fallback);
                if (layout != null) {
                    break;
                }
            }
        }
        return layout;
    }

    /** The latest entry whose date has passed, or the first when none has. */
    static JSONObject pickActiveEntry(JSONArray entries, long now) {
        if (entries == null || entries.length() == 0) {
            return null;
        }
        JSONObject active = entries.optJSONObject(0);
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.optJSONObject(i);
            if (e != null && e.optLong("date") <= now) {
                active = e;
            }
        }
        return active;
    }

    /** When the next entry becomes current, or 0 when none is ahead. */
    static long nextFlipDate(JSONArray entries, long now) {
        long next = 0;
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                JSONObject e = entries.optJSONObject(i);
                if (e == null) {
                    continue;
                }
                long date = e.optLong("date");
                if (date > now && (next == 0 || date < next)) {
                    next = date;
                }
            }
        }
        return next;
    }

    /**
     * Flattens the node tree depth-first.
     *
     * <p>Containers contribute traversal order and nothing else: a complication has no layout to
     * honour, so a row and a column produce the same reading. That is the whole reason this is a
     * flatten rather than a render.</p>
     *
     * @param root the layout root
     * @return every leaf node in document order
     */
    public static List<JSONObject> flatten(JSONObject root) {
        List<JSONObject> out = new ArrayList<JSONObject>();
        flattenInto(root, out, 0);
        return out;
    }

    private static void flattenInto(JSONObject node, List<JSONObject> out, int depth) {
        if (node == null || depth > MAX_DEPTH) {
            return;
        }
        // "ch", which is what SurfaceContainer.serializeContent writes and what the RemoteViews
        // renderer reads. Reading "c" found nothing, so every row, column and box looked empty
        // and a complication mined a layout with no text, no progress and no imagery in it.
        JSONArray children = node.optJSONArray("ch");
        if (children != null && children.length() > 0) {
            for (int i = 0; i < children.length(); i++) {
                flattenInto(children.optJSONObject(i), out, depth + 1);
            }
            return;
        }
        out.add(node);
    }

    /** The first node of a wire type, or null. */
    public static JSONObject firstOfType(List<JSONObject> nodes, String type) {
        for (JSONObject node : nodes) {
            if (type.equals(node.optString("t", ""))) {
                return node;
            }
        }
        return null;
    }

    /**
     * Every text-bearing node's resolved string, in document order.
     *
     * <p>Both {@code text} and {@code dyn} count: a countdown reads as text to a face that asked
     * for one, even where the caller can do better with a native timer.</p>
     */
    public static List<String> texts(List<JSONObject> nodes, JSONObject state) {
        List<String> out = new ArrayList<String>();
        for (JSONObject node : nodes) {
            String type = node.optString("t", "");
            if ("text".equals(type)) {
                String text = CN1SurfaceRenderer.interpolate(node.optString("text", ""), state);
                if (text != null && text.length() > 0) {
                    out.add(text);
                }
            } else if ("dyn".equals(type)) {
                // A dynamic node has no "text" field at all -- it serializes a style plus a date
                // or a dateKey, and the reader formats it. Interpolating "text" here resolved to
                // an empty string, so every countdown, clock and relative date vanished from a
                // complication rather than showing its value.
                String text = dynamicText(node, state);
                if (text != null && text.length() > 0) {
                    out.add(text);
                }
            }
        }
        return out;
    }

    /**
     * A dynamic node's value as a plain string.
     *
     * <p>A complication slot takes a string, so a countdown is formatted at render time and
     * refreshed when the timeline flips -- there is no native ticking widget to hand a watch
     * face. A caller that CAN tick natively, as the complication data source does for the timer
     * styles, should read the style and date itself and build the ticking form instead.</p>
     *
     * @param node a {@code dyn} node
     * @param state the entry state, which may supply the date by key
     * @return the formatted value, never null
     */
    public static String dynamicText(JSONObject node, JSONObject state) {
        if (node == null) {
            return "";
        }
        return CN1SurfaceRenderer.formatWatchDynamicText(node, state);
    }

    /**
     * A dynamic node's resolved timestamp, so a caller that can render it natively has the
     * value rather than a formatted string.
     *
     * @param node a {@code dyn} node
     * @param state the entry state, which may supply the date by key
     * @return epoch millis, or 0 when the node names none
     */
    public static long dynamicDate(JSONObject node, JSONObject state) {
        if (node == null) {
            return 0;
        }
        return CN1SurfaceRenderer.resolveWatchDate(node, state);
    }

    /**
     * A progress node's value, clamped to 0..1.
     *
     * <p>Literal, read from the entry's state by key, or computed from a date interval --
     * whichever the node carries. The arithmetic is the renderer's own
     * {@code resolveFraction}, not a second copy of it, because a complication and a
     * home-screen widget disagreeing about what a progress node shows is a bug nobody would
     * think to look for.</p>
     *
     * <p>What is decided HERE rather than there is emptiness. The renderer always has a bar to
     * draw and treats an unusable node as zero; a ranged complication would then read as a
     * gauge pinned at the bottom, which is a claim about the value rather than an absence of
     * one. So a node carrying no value, no resolvable key and no interval answers -1, and the
     * caller offers the slot nothing.</p>
     *
     * <p>A date interval freezes at read time, exactly as it does for a widget: the value is
     * recomputed on the next refresh. Wear has no ticking ranged-value complication to use
     * instead.</p>
     *
     * @param prog a {@code prog} node
     * @param state the entry state
     * @return the value in 0..1, or -1 when the node carries none
     */
    public static float progressValue(JSONObject prog, JSONObject state) {
        return progressValue(prog, state, System.currentTimeMillis());
    }

    /**
     * As above, but resolving a date interval against a stated moment.
     *
     * <p>A complication renders its future entries before they are current, so an interval
     * evaluated against the request's clock is frozen at today's fraction for ever -- there is no
     * later request to recompute it.</p>
     *
     * @param prog a {@code prog} node
     * @param state the entry state
     * @param asOf the moment the entry describes
     * @return the value in 0..1, or -1 when the node carries none
     */
    public static float progressValue(JSONObject prog, JSONObject state, long asOf) {
        if (prog == null) {
            return -1f;
        }
        String key = prog.optString("valueKey", "");
        boolean resolvableKey = key.length() > 0 && state != null
                && state.opt(key) instanceof Number;
        if (!prog.has("value") && !resolvableKey
                && !(prog.has("start") && prog.has("end"))) {
            return -1f;
        }
        return (float) CN1SurfaceRenderer.resolveFraction(prog, state, asOf);
    }

    /**
     * Rasterizes an image or vector node for a complication or Tile.
     *
     * <p>Reuses the renderer's own decoding and vector rasterization rather than reimplementing
     * them, so a vector degrades to a bitmap here exactly as it does for a home-screen widget.</p>
     *
     * @param ctx any context
     * @param kindId the widget kind, which locates the published imagery
     * @param node an {@code img} or {@code vec} node
     * @param state the entry state
     * @return the bitmap, or null when the node names nothing renderable
     */
    public static Bitmap bitmap(Context ctx, String kindId, JSONObject node, JSONObject state) {
        if (node == null) {
            return null;
        }
        return CN1SurfaceRenderer.renderWatchBitmap(ctx, kindId, node, state);
    }

    /**
     * The tap target for a complication or Tile: the root action, as an intent into the same
     * trampoline a widget tap uses.
     *
     * @param ctx any context
     * @param kindId the widget kind, reported to the action handler as the source
     * @param layout the resolved layout root
     * @return the intent, or null when the layout declares no action
     */
    public static Intent rootAction(Context ctx, String kindId, JSONObject layout) {
        if (layout == null) {
            return null;
        }
        JSONObject action = layout.optJSONObject("action");
        if (action == null) {
            return null;
        }
        String actionId = action.optString("id", "");
        if (actionId.length() == 0) {
            return null;
        }
        return CN1SurfaceRenderer.watchActionIntent(ctx, kindId, actionId,
                action.optJSONObject("p"));
    }

    /**
     * Whether a kind declares a watch family, from the build-time list the builder wrote.
     *
     * <p>Read from resources rather than from the timeline, because it has to be answerable for
     * a kind that has never published anything.</p>
     *
     * @param ctx any context
     * @param kindId the widget kind
     * @return true when the kind was declared with a complication family
     */
    public static boolean isWatchKind(Context ctx, String kindId) {
        if (kindId == null) {
            return false;
        }
        try {
            int id = ctx.getResources().getIdentifier("cn1_surface_watch_kinds", "array",
                    ctx.getPackageName());
            if (id == 0) {
                return false;
            }
            String[] kinds = ctx.getResources().getStringArray(id);
            for (String kind : kinds) {
                if (kindId.equals(kind)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the declared watch surface kinds", t);
        }
        return false;
    }
}
