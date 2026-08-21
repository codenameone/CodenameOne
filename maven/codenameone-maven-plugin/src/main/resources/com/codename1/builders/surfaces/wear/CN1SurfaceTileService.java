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

import android.graphics.Bitmap;
import android.util.Log;

import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.wear.protolayout.ColorBuilders;
import androidx.wear.protolayout.DimensionBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves one Codename One surface kind as a Wear OS Tile.
 *
 * <p>Generated per kind that declares {@code WATCH_RECTANGULAR}, which is the only family roomy
 * enough for a layout rather than a readout. Ships as a build-time resource because it compiles
 * against {@code androidx.wear.tiles} and {@code androidx.wear.protolayout}, which the Android
 * port cannot depend on.</p>
 *
 * <p>Unlike a complication, a Tile really does render the node tree: ProtoLayout has a near-1:1
 * counterpart for every node in the catalog, which was cut to the RemoteViews floor in the first
 * place. Two things come out <i>better</i> here than on a phone widget -- circular progress
 * renders natively where RemoteViews has to degrade to a linear bar, and per-node tap actions
 * work where a small iOS widget honours only the root.</p>
 *
 * <p><b>The one real limitation is time.</b> A {@code SurfaceDynamicText} countdown ticks
 * natively on both phone platforms; here it is frozen at render and the Tile asks again when the
 * timeline says the value changes. ProtoLayout's dynamic expressions could animate it, but they
 * are version-sensitive and platform-gated, and a frozen value that is always correct beats a
 * ticking one that works on some watches. This is the largest fidelity gap in the feature.</p>
 */
public abstract class CN1SurfaceTileService extends TileService {

    private static final String TAG = "CN1Surfaces";

    /** ProtoLayout resource version; bumped by content, so unchanged art is not re-sent. */
    private static final String ROOT_ID = "cn1_root";

    /** A Tile refresh is rate-limited by the system, so asking more often than this is waste. */
    private static final long MIN_FRESHNESS_MILLIS = 60L * 1000L;
    private static final long MAX_FRESHNESS_MILLIS = 24L * 60L * 60L * 1000L;

    /** The widget kind this Tile serves. Supplied by the generated subclass. */
    protected abstract String getKindId();

    @Override
    protected ListenableFuture<TileBuilders.Tile> onTileRequest(
            RequestBuilders.TileRequest request) {
        return CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<TileBuilders.Tile>() {
                    @Override
                    public Object attachCompleter(
                            CallbackToFutureAdapter.Completer<TileBuilders.Tile> completer) {
                        completer.set(buildTile());
                        return "cn1TileRequest";
                    }
                });
    }

    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(
            RequestBuilders.ResourcesRequest request) {
        return CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<ResourceBuilders.Resources>() {
                    @Override
                    public Object attachCompleter(
                            CallbackToFutureAdapter.Completer<ResourceBuilders.Resources>
                                    completer) {
                        completer.set(buildResources());
                        return "cn1TileResources";
                    }
                });
    }

    private TileBuilders.Tile buildTile() {
        LayoutElementBuilders.LayoutElement root;
        long freshness = MAX_FRESHNESS_MILLIS;
        String version = "0";
        try {
            CN1WatchSurface.Reading reading =
                    CN1WatchSurface.read(this, getKindId(), "watchRectangular");
            if (reading == null) {
                root = text("No data yet");
            } else {
                root = render(reading.getLayout(), reading.getState(), 0);
                freshness = freshnessFor(reading.getNextFlipDate());
                version = String.valueOf(imageNames(reading.getLayout()).hashCode());
            }
        } catch (Throwable t) {
            // A Tile that throws is removed from the carousel, so a malformed descriptor must
            // degrade to something rather than nothing.
            Log.w(TAG, "Could not build the Tile for kind " + getKindId(), t);
            root = text("");
        }
        return new TileBuilders.Tile.Builder()
                .setResourcesVersion(version)
                .setFreshnessIntervalMillis(freshness)
                .setTileTimeline(new TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(new TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(new LayoutElementBuilders.Layout.Builder()
                                        .setRoot(root)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    /**
     * How long before the Tile should ask again.
     *
     * <p>Driven by the timeline rather than a fixed poll: an app that publishes entries covering
     * the hours ahead is refreshed by the system on its own clock without ever being woken, which
     * is the same deal the phone platforms give a widget. Zero when the timeline is exhausted --
     * there is nothing further to show until the app publishes again.</p>
     */
    private static long freshnessFor(long nextFlipDate) {
        if (nextFlipDate <= 0) {
            return 0;
        }
        long delta = nextFlipDate - System.currentTimeMillis();
        if (delta < MIN_FRESHNESS_MILLIS) {
            return MIN_FRESHNESS_MILLIS;
        }
        return Math.min(delta, MAX_FRESHNESS_MILLIS);
    }

    private ResourceBuilders.Resources buildResources() {
        ResourceBuilders.Resources.Builder builder = new ResourceBuilders.Resources.Builder();
        String version = "0";
        try {
            CN1WatchSurface.Reading reading =
                    CN1WatchSurface.read(this, getKindId(), "watchRectangular");
            if (reading != null) {
                version = String.valueOf(imageNames(reading.getLayout()).hashCode());
                for (Map.Entry<String, JSONObject> e
                        : imageNodes(reading.getLayout()).entrySet()) {
                    Bitmap bitmap = CN1WatchSurface.bitmap(this, getKindId(), e.getValue(),
                            reading.getState());
                    if (bitmap == null) {
                        continue;
                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    builder.addIdToImageMapping(e.getKey(),
                            new ResourceBuilders.ImageResource.Builder()
                                    .setInlineResource(
                                            new ResourceBuilders.InlineImageResource.Builder()
                                                    .setData(out.toByteArray())
                                                    .setWidthPx(bitmap.getWidth())
                                                    .setHeightPx(bitmap.getHeight())
                                                    .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
                                                    .build())
                                    .build());
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not build Tile resources for kind " + getKindId(), t);
        }
        return builder.setVersion(version).build();
    }

    // --- node tree to ProtoLayout ------------------------------------------------

    private LayoutElementBuilders.LayoutElement render(JSONObject node, JSONObject state,
            int depth) {
        if (node == null || depth > 8) {
            return text("");
        }
        String type = node.optString("t", "");
        if ("col".equals(type)) {
            LayoutElementBuilders.Column.Builder col = new LayoutElementBuilders.Column.Builder();
            for (JSONObject child : children(node)) {
                col.addContent(render(child, state, depth + 1));
            }
            return col.setModifiers(modifiers(node)).build();
        }
        if ("row".equals(type)) {
            LayoutElementBuilders.Row.Builder row = new LayoutElementBuilders.Row.Builder();
            for (JSONObject child : children(node)) {
                row.addContent(render(child, state, depth + 1));
            }
            return row.setModifiers(modifiers(node)).build();
        }
        if ("box".equals(type)) {
            LayoutElementBuilders.Box.Builder box = new LayoutElementBuilders.Box.Builder();
            for (JSONObject child : children(node)) {
                box.addContent(render(child, state, depth + 1));
            }
            return box.setModifiers(modifiers(node)).build();
        }
        if ("spacer".equals(type)) {
            return new LayoutElementBuilders.Spacer.Builder()
                    .setWidth(DimensionBuilders.dp(Math.max(1, node.optInt("w", 4))))
                    .setHeight(DimensionBuilders.dp(Math.max(1, node.optInt("h", 4))))
                    .build();
        }
        if ("img".equals(type) || "vec".equals(type)) {
            String name = imageId(node);
            return new LayoutElementBuilders.Image.Builder()
                    .setResourceId(name)
                    .setWidth(DimensionBuilders.dp(Math.max(1, node.optInt("w", 24))))
                    .setHeight(DimensionBuilders.dp(Math.max(1, node.optInt("h", 24))))
                    .setModifiers(modifiers(node))
                    .build();
        }
        if ("prog".equals(type)) {
            // A ProtoLayout arc renders the ring natively -- the one place a Tile beats the phone
            // widget, which has to degrade a circular bar to a linear one.
            float value = CN1WatchSurface.progressValue(node, state);
            return new LayoutElementBuilders.Arc.Builder()
                    .addContent(new LayoutElementBuilders.ArcLine.Builder()
                            .setLength(DimensionBuilders.degrees(
                                    360f * (value < 0 ? 0f : value)))
                            .setThickness(DimensionBuilders.dp(6))
                            .build())
                    .build();
        }
        // text, dyn and anything unknown: whatever string the node resolves to. A dyn value is
        // frozen here; see the class comment.
        return styledText(node, state);
    }

    private LayoutElementBuilders.LayoutElement styledText(JSONObject node, JSONObject state) {
        String value = CN1SurfaceRenderer.interpolate(node.optString("text", ""), state);
        LayoutElementBuilders.FontStyle.Builder font = new LayoutElementBuilders.FontStyle.Builder();
        int size = node.optInt("size", 0);
        if (size > 0) {
            font.setSize(DimensionBuilders.sp(size));
        }
        String weight = node.optString("fw", "");
        if ("semibold".equals(weight) || "bold".equals(weight) || "black".equals(weight)) {
            // The same collapse to regular/bold the RemoteViews path documents.
            font.setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD);
        }
        JSONObject color = node.optJSONObject("color");
        if (color != null && color.has("d")) {
            font.setColor(ColorBuilders.argb(color.optInt("d")));
        }
        LayoutElementBuilders.Text.Builder text = new LayoutElementBuilders.Text.Builder()
                .setText(value == null ? "" : value)
                .setFontStyle(font.build());
        int maxLines = node.optInt("maxLines", 0);
        if (maxLines > 0) {
            text.setMaxLines(maxLines);
        }
        return text.setModifiers(modifiers(node)).build();
    }

    private ModifiersBuilders.Modifiers modifiers(JSONObject node) {
        ModifiersBuilders.Modifiers.Builder mods = new ModifiersBuilders.Modifiers.Builder();
        JSONObject pad = node.optJSONObject("pad");
        if (pad != null) {
            mods.setPadding(new ModifiersBuilders.Padding.Builder()
                    .setStart(DimensionBuilders.dp(pad.optInt("l", 0)))
                    .setEnd(DimensionBuilders.dp(pad.optInt("r", 0)))
                    .setTop(DimensionBuilders.dp(pad.optInt("t", 0)))
                    .setBottom(DimensionBuilders.dp(pad.optInt("b", 0)))
                    .build());
        }
        JSONObject bg = node.optJSONObject("bg");
        if (bg != null && bg.has("d")) {
            ModifiersBuilders.Background.Builder background =
                    new ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(bg.optInt("d")));
            int corner = node.optInt("corner", 0);
            if (corner > 0) {
                background.setCorner(new ModifiersBuilders.Corner.Builder()
                        .setRadius(DimensionBuilders.dp(corner))
                        .build());
            }
            mods.setBackground(background.build());
        }
        // Per-node tap actions work on a Tile, which a small iOS widget cannot do.
        JSONObject action = node.optJSONObject("action");
        if (action != null && action.optString("id", "").length() > 0) {
            mods.setClickable(new ModifiersBuilders.Clickable.Builder()
                    .setId(action.optString("id"))
                    .setOnClick(new androidx.wear.protolayout.ActionBuilders.LaunchAction.Builder()
                            .setAndroidActivity(
                                    new androidx.wear.protolayout.ActionBuilders.AndroidActivity
                                            .Builder()
                                            .setPackageName(getPackageName())
                                            .setClassName(CN1SurfaceActionActivity.class.getName())
                                            .build())
                            .build())
                    .build());
        }
        return mods.build();
    }

    private static LayoutElementBuilders.LayoutElement text(String value) {
        return new LayoutElementBuilders.Text.Builder().setText(value).build();
    }

    private static List<JSONObject> children(JSONObject node) {
        List<JSONObject> out = new ArrayList<JSONObject>();
        JSONArray array = node.optJSONArray("c");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject child = array.optJSONObject(i);
                if (child != null) {
                    out.add(child);
                }
            }
        }
        return out;
    }

    /**
     * The resource id an image node maps to.
     *
     * <p>Published names are content hashes, which is what lets the resources version be a
     * hash of the name list: unchanged art is never re-sent.</p>
     */
    private static String imageId(JSONObject node) {
        String name = node.optString("name", "");
        return name.length() > 0 ? name : (ROOT_ID + "_" + System.identityHashCode(node));
    }

    private static Map<String, JSONObject> imageNodes(JSONObject root) {
        Map<String, JSONObject> out = new LinkedHashMap<String, JSONObject>();
        for (JSONObject node : CN1WatchSurface.flatten(root)) {
            String type = node.optString("t", "");
            if ("img".equals(type) || "vec".equals(type)) {
                out.put(imageId(node), node);
            }
        }
        return out;
    }

    private static List<String> imageNames(JSONObject root) {
        return new ArrayList<String>(imageNodes(root).keySet());
    }
}
