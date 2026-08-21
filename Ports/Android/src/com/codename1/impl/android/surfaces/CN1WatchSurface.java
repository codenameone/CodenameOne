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

        Reading(JSONObject layout, JSONObject state, long nextFlipDate) {
            this.layout = layout;
            this.state = state;
            this.nextFlipDate = nextFlipDate;
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
                    nextFlipDate(entries, now));
        } catch (Throwable t) {
            // A malformed descriptor must leave the face showing whatever it had, not crash the
            // data source -- which on Wear takes the whole watch face down with it.
            Log.w(TAG, "Could not read the published timeline for watch kind " + kindId, t);
            return null;
        }
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
        JSONArray children = node.optJSONArray("c");
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
            if ("text".equals(type) || "dyn".equals(type)) {
                String text = CN1SurfaceRenderer.interpolate(node.optString("text", ""), state);
                if (text != null && text.length() > 0) {
                    out.add(text);
                }
            }
        }
        return out;
    }

    /**
     * A progress node's value, clamped to 0..1.
     *
     * <p>Either literal or read from the entry's state by key, matching what the renderer does
     * for a progress bar on every other platform.</p>
     *
     * @param prog a {@code prog} node
     * @param state the entry state
     * @return the value in 0..1, or -1 when the node carries none
     */
    public static float progressValue(JSONObject prog, JSONObject state) {
        if (prog == null) {
            return -1f;
        }
        double value;
        if (prog.has("value")) {
            value = prog.optDouble("value", -1);
        } else {
            String key = prog.optString("valueKey", "");
            if (key.length() == 0 || state == null || !state.has(key)) {
                return -1f;
            }
            value = state.optDouble(key, -1);
        }
        if (value < 0) {
            return -1f;
        }
        return (float) Math.min(1.0, value);
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
