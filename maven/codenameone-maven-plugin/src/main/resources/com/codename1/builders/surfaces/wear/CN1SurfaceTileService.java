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
    /// How often a Tile showing dynamic text asks to be rebuilt; see freshnessFor.
    private static final long DYNAMIC_FRESHNESS_MILLIS = 60L * 1000L;

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
                root = render(reading.getLayout(), reading.getState(), 0, false);
                freshness = freshnessFor(reading.getNextFlipDate(),
                        hasDynamicText(reading.getLayout()));
                version = resourcesVersion(reading);
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
    private static long freshnessFor(long nextFlipDate, boolean hasDynamicText) {
        long delta = nextFlipDate > 0 ? nextFlipDate - System.currentTimeMillis() : Long.MAX_VALUE;
        if (nextFlipDate <= 0 && !hasDynamicText) {
            // Nothing further to show until the app publishes again.
            return 0;
        }
        if (hasDynamicText) {
            // A clock, a countdown or a relative date. This is the fidelity gap the guide
            // records: ProtoLayout has no ticking text this can lower onto, so the value is
            // frozen at render time -- and with no flip date to ask about, nothing would ever
            // ask again and "in 5 minutes" would still say that tomorrow. A bounded refresh is
            // the honest compromise: minute-accurate rather than second-accurate, which is what
            // a Tile's refresh rate limit allows anyway.
            delta = Math.min(delta, DYNAMIC_FRESHNESS_MILLIS);
        }
        if (delta < MIN_FRESHNESS_MILLIS) {
            return MIN_FRESHNESS_MILLIS;
        }
        return Math.min(delta, MAX_FRESHNESS_MILLIS);
    }

    /// Whether the active layout shows anything that changes on its own.
    private static boolean hasDynamicText(JSONObject root) {
        for (JSONObject node : CN1WatchSurface.flatten(root)) {
            if ("dyn".equals(node.optString("t", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The version the Tile advertises for its resource set.
     *
     * <p>ONE computation, called from both the Tile and the resources it names: Wear caches
     * resources by this string and only asks for them again when it changes, so the two sides
     * disagreeing is either a stale bitmap or a rebuild on every frame.</p>
     *
     * <p>The entry state is part of it, not just the resource ids. A vector's id already covers
     * its own definition -- {@code imageId} hashes the node -- but not the state its ops read.
     * A timeline flip to an entry that only moves a hand or fills a gauge leaves every id
     * identical while the rasterizer would now draw something else, and the Tile kept showing
     * the previous entry's artwork. Folding the state in costs a resource rebuild on a flip that
     * did not need one, which is the harmless direction to be wrong in.</p>
     *
     * @param reading the timeline entry being rendered
     * @return the resources version
     */
    private static String resourcesVersion(CN1WatchSurface.Reading reading) {
        return String.valueOf((imageNames(reading.getLayout()).toString()
                + "|" + String.valueOf(reading.getState())).hashCode());
    }

    private ResourceBuilders.Resources buildResources() {
        ResourceBuilders.Resources.Builder builder = new ResourceBuilders.Resources.Builder();
        String version = "0";
        try {
            CN1WatchSurface.Reading reading =
                    CN1WatchSurface.read(this, getKindId(), "watchRectangular");
            if (reading != null) {
                version = resourcesVersion(reading);
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
            int depth, boolean inRow) {
        if (node == null || depth > 8) {
            return text("");
        }
        String type = node.optString("t", "");
        if ("col".equals(type)) {
            LayoutElementBuilders.Column.Builder col = new LayoutElementBuilders.Column.Builder();
            for (JSONObject child : children(node)) {
                col.addContent(render(child, state, depth + 1, false));
            }
            return col.setModifiers(modifiers(node)).build();
        }
        if ("row".equals(type)) {
            LayoutElementBuilders.Row.Builder row = new LayoutElementBuilders.Row.Builder();
            for (JSONObject child : children(node)) {
                row.addContent(render(child, state, depth + 1, true));
            }
            return row.setModifiers(modifiers(node)).build();
        }
        if ("box".equals(type)) {
            LayoutElementBuilders.Box.Builder box = new LayoutElementBuilders.Box.Builder();
            for (JSONObject child : children(node)) {
                box.addContent(render(child, state, depth + 1, inRow));
            }
            return box.setModifiers(modifiers(node)).build();
        }
        if ("spacer".equals(type)) {
            // A spacer carries "min" and never "w"/"h" -- SurfaceSpacer.serializeContent writes
            // the one key, and only when it is non-zero -- so reading w/h turned every declared
            // spacer into the same 4dp stub and every flexible one along with it.
            //
            // The measurement belongs to the PARENT's axis, which is why the axis is threaded
            // down: the same node is a width in a row and a height in a column. A spacer with no
            // minimum is the flexible kind that absorbs what is left over, which is expand()
            // rather than a few dips -- rendering it as 4dp collapsed exactly the push-apart
            // layouts it exists for. The cross axis stays at 1dp: a spacer never has a size of
            // its own there.
            int min = node.optInt("min", 0);
            DimensionBuilders.SpacerDimension along = min > 0
                    ? DimensionBuilders.dp(min) : DimensionBuilders.expand();
            DimensionBuilders.SpacerDimension across = DimensionBuilders.dp(1);
            return new LayoutElementBuilders.Spacer.Builder()
                    .setWidth(inRow ? along : across)
                    .setHeight(inRow ? across : along)
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
        // A dynamic node serializes a style plus a date or dateKey and carries no "text" at all,
        // so it has to be formatted rather than interpolated -- otherwise every countdown, clock
        // and relative date rendered blank. Frozen at render; see the class comment.
        String value = "dyn".equals(node.optString("t", ""))
                ? CN1WatchSurface.dynamicText(node, state)
                : CN1SurfaceRenderer.interpolate(node.optString("text", ""), state);
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
        // SurfaceNode serializes padding as the array [top, right, bottom, left], which is what
        // the RemoteViews renderer reads too. Asking for an object returned null for every valid
        // descriptor, so all declared padding was silently dropped from a Tile.
        JSONArray pad = node.optJSONArray("pad");
        if (pad != null && pad.length() == 4) {
            mods.setPadding(new ModifiersBuilders.Padding.Builder()
                    .setTop(DimensionBuilders.dp(pad.optInt(0)))
                    .setEnd(DimensionBuilders.dp(pad.optInt(1)))
                    .setBottom(DimensionBuilders.dp(pad.optInt(2)))
                    .setStart(DimensionBuilders.dp(pad.optInt(3)))
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
                    .setOnClick(launchAction(action))
                    .build());
        }
        return mods.build();
    }

    /**
     * The launch action for a tapped node, carrying what the trampoline needs to dispatch.
     *
     * <p>A Clickable's id is ProtoLayout interaction metadata and never reaches the started
     * activity, so an action built from it alone opened the app and dropped the action id,
     * source and parameters on the floor. {@code CN1SurfaceActionActivity} dispatches only when
     * {@code EXTRA_ACTION_ID} is present, so the extras are attached explicitly here -- the same
     * three a widget tap sends.</p>
     */
    private androidx.wear.protolayout.ActionBuilders.LaunchAction launchAction(JSONObject action) {
        androidx.wear.protolayout.ActionBuilders.AndroidActivity.Builder activity =
                new androidx.wear.protolayout.ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(getPackageName())
                        .setClassName(CN1SurfaceActionActivity.class.getName());
        // The trampoline is exported so the tile host can start it, which means any app on the
        // watch can too. This says the tap came from a surface THIS app drew; see
        // CN1SurfaceActionActivity.token. The layout carrying it goes to the tile host and
        // nowhere else.
        activity.addKeyToExtraMapping(CN1SurfaceActionActivity.EXTRA_TOKEN,
                stringExtra(CN1SurfaceActionActivity.token(this)));
        activity.addKeyToExtraMapping(CN1SurfaceActionActivity.EXTRA_SOURCE,
                stringExtra(getKindId()));
        activity.addKeyToExtraMapping(CN1SurfaceActionActivity.EXTRA_ACTION_ID,
                stringExtra(action.optString("id", "")));
        JSONObject params = action.optJSONObject("p");
        if (params != null) {
            activity.addKeyToExtraMapping(CN1SurfaceActionActivity.EXTRA_ACTION_PARAMS,
                    stringExtra(params.toString()));
        }
        return new androidx.wear.protolayout.ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(activity.build())
                .build();
    }

    private static androidx.wear.protolayout.ActionBuilders.AndroidStringExtra stringExtra(
            String value) {
        return new androidx.wear.protolayout.ActionBuilders.AndroidStringExtra.Builder()
                .setValue(value == null ? "" : value)
                .build();
    }

    private static LayoutElementBuilders.LayoutElement text(String value) {
        return new LayoutElementBuilders.Text.Builder().setText(value).build();
    }

    private static List<JSONObject> children(JSONObject node) {
        List<JSONObject> out = new ArrayList<JSONObject>();
        // "ch", which is what SurfaceContainer.serializeContent writes. Reading "c" found
        // nothing, so every row, column and box rendered empty.
        JSONArray array = node.optJSONArray("ch");
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
     * The resource id an image node maps to, derived from its CONTENT.
     *
     * <p>An {@code img} node already names a content hash, which is what lets the resources
     * version be a hash of the name list: unchanged art is never re-sent. A {@code vec} node
     * carries no name at all, so its serialized form is hashed instead.</p>
     *
     * <p>Content rather than object identity, because the layout request and the resources
     * request are two separate calls that each re-read and re-parse the timeline. An id taken
     * from a JSONObject's identity therefore differed between them: the layout referenced a
     * resource the returned map did not contain, and every vector rendered as a missing image.
     * Two identical vectors sharing one resource is correct -- they draw the same thing.</p>
     */
    private static String imageId(JSONObject node) {
        String name = node.optString("name", "");
        if (name.length() > 0) {
            return name;
        }
        return ROOT_ID + "_vec" + Integer.toHexString(node.toString().hashCode());
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
