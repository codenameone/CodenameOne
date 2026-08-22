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

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

import org.json.JSONObject;

import java.util.List;

/**
 * Serves one Codename One surface kind to a Wear OS watch face.
 *
 * <p>The build generates a tiny subclass per watch-bearing kind, carrying only its id; every
 * decision lives here. Ships as a build-time resource rather than in the Android port because it
 * compiles against {@code androidx.wear.watchface.complications}, which the port cannot depend
 * on -- an app that publishes no complication must not carry the library.</p>
 *
 * <p><b>A complication is not a small widget.</b> The face asks for one specific
 * {@link ComplicationType} and composes the answer into its own design: there is no layout to
 * honour, and padding, background, alignment, weight and colour are the face's business. So the
 * published node tree is flattened by {@link CN1WatchSurface} and mined for content -- the first
 * text, the first progress value, the first image -- rather than rendered. Everything that
 * cannot survive that is dropped, and said out loud once per render under the tag
 * {@code CN1Surfaces} so a developer who wonders where their layout went can find out with
 * {@code adb logcat}.</p>
 *
 * <p>Nothing here throws. A data source that crashes takes the watch face down with it, so a
 * malformed descriptor leaves the face showing whatever it had.</p>
 */
public abstract class CN1ComplicationDataSource extends ComplicationDataSourceService {

    private static final String TAG = "CN1Surfaces";

    /** Wear's own guidance for a short-text slot; longer strings are truncated by the face. */
    private static final int SHORT_TEXT_MAX = 7;

    /** The widget kind this data source serves. Supplied by the generated subclass. */
    protected abstract String getKindId();

    @Override
    public void onComplicationRequest(ComplicationRequest request,
            ComplicationRequestListener listener) {
        ComplicationData data = null;
        try {
            data = build(request.getComplicationType());
        } catch (Throwable t) {
            Log.w(TAG, "Could not build complication data for kind " + getKindId(), t);
        }
        try {
            listener.onComplicationData(data == null ? noData() : data);
        } catch (Throwable t) {
            Log.w(TAG, "Could not deliver complication data for kind " + getKindId(), t);
        }
    }

    @Override
    public ComplicationData getPreviewData(ComplicationType type) {
        try {
            ComplicationData data = build(type);
            if (data != null) {
                return data;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not build preview data for kind " + getKindId(), t);
        }
        // The gallery shows this before the app has ever published, so a placeholder that names
        // the kind beats an empty slot the user cannot identify.
        return shortText(getKindId(), null, null);
    }

    /**
     * The portable family whose layout best serves a requested complication type.
     *
     * <p>The mapping is the one {@code WidgetSize} documents, read backwards: a face asking for
     * long text wants the roomy layout, one asking for a ranged value or a glyph wants the round
     * one, and short text is the readout every family can produce.</p>
     */
    private static String familyFor(ComplicationType type) {
        if (ComplicationType.LONG_TEXT.equals(type)) {
            return "watchRectangular";
        }
        if (ComplicationType.SHORT_TEXT.equals(type)) {
            return "watchInline";
        }
        return "watchCircular";
    }

    private ComplicationData build(ComplicationType type) {
        CN1WatchSurface.Reading reading = CN1WatchSurface.read(this, getKindId(), familyFor(type));
        if (reading == null) {
            return null;
        }
        List<JSONObject> nodes = CN1WatchSurface.flatten(reading.getLayout());
        List<String> texts = CN1WatchSurface.texts(nodes, reading.getState());
        PendingIntent tap = tapIntent(reading.getLayout());
        reportDroppedContent(nodes, texts);

        if (ComplicationType.LONG_TEXT.equals(type)) {
            String title = texts.isEmpty() ? getKindId() : texts.get(0);
            String body = texts.size() > 1 ? join(texts, 1) : "";
            return new LongTextComplicationData.Builder(plain(body.length() == 0 ? title : body),
                    plain(title))
                    .setTitle(body.length() == 0 ? null : plain(title))
                    .setTapAction(tap)
                    .build();
        }
        if (ComplicationType.RANGED_VALUE.equals(type)) {
            JSONObject prog = CN1WatchSurface.firstOfType(nodes, "prog");
            float value = CN1WatchSurface.progressValue(prog, reading.getState());
            if (value < 0) {
                // The face asked for a gauge and the layout has none. Answering with no data
                // lets it fall back to another type rather than showing an empty ring.
                return null;
            }
            RangedValueComplicationData.Builder builder =
                    new RangedValueComplicationData.Builder(value, 0f, 1f,
                            plain(texts.isEmpty() ? getKindId() : texts.get(0)));
            if (!texts.isEmpty()) {
                builder.setText(plain(shorten(texts.get(0))));
            }
            return builder.setTapAction(tap).build();
        }
        if (ComplicationType.MONOCHROMATIC_IMAGE.equals(type)) {
            Icon icon = monochromeIcon(nodes, reading.getState());
            if (icon == null) {
                return null;
            }
            return new MonochromaticImageComplicationData.Builder(
                    new MonochromaticImage.Builder(icon).build(),
                    plain(texts.isEmpty() ? getKindId() : texts.get(0)))
                    .setTapAction(tap)
                    .build();
        }
        if (ComplicationType.SHORT_TEXT.equals(type)) {
            if (texts.isEmpty()) {
                return null;
            }
            return shortText(shorten(texts.get(0)), texts.size() > 1 ? shorten(texts.get(1)) : null,
                    tap);
        }
        return null;
    }

    private ShortTextComplicationData shortText(String text, String title, PendingIntent tap) {
        // The untruncated string becomes the content description, so a screen reader still hears
        // what the layout said even where the slot shows seven characters.
        ShortTextComplicationData.Builder builder =
                new ShortTextComplicationData.Builder(plain(shorten(text)), plain(text));
        if (title != null && title.length() > 0) {
            builder.setTitle(plain(title));
        }
        return builder.setTapAction(tap).build();
    }

    private Icon monochromeIcon(List<JSONObject> nodes, JSONObject state) {
        JSONObject node = CN1WatchSurface.firstOfType(nodes, "img");
        if (node == null) {
            // A vector is the better source anyway: a gauge or a ring is exactly what this slot
            // is for, and it rasterizes to a mask the face can tint.
            node = CN1WatchSurface.firstOfType(nodes, "vec");
        }
        Bitmap bitmap = CN1WatchSurface.bitmap(this, getKindId(), node, state);
        if (bitmap == null || Build.VERSION.SDK_INT < 26) {
            return null;
        }
        return Icon.createWithBitmap(bitmap);
    }

    private PendingIntent tapIntent(JSONObject layout) {
        Intent intent = CN1WatchSurface.rootAction(this, getKindId(), layout);
        if (intent == null) {
            return null;
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            // FLAG_IMMUTABLE, named by value because the port compiles against an older SDK.
            flags |= 0x04000000;
        }
        try {
            return PendingIntent.getActivity(this, intent.getDataString().hashCode(), intent,
                    flags);
        } catch (Throwable t) {
            Log.w(TAG, "Could not build the complication tap action for " + getKindId(), t);
            return null;
        }
    }

    /**
     * Names what the face will not be showing.
     *
     * <p>A complication reduces a layout to a few values, and a developer whose careful design
     * arrives as one number deserves to know that is by design rather than a bug.</p>
     */
    private void reportDroppedContent(List<JSONObject> nodes, List<String> texts) {
        int images = 0;
        for (JSONObject node : nodes) {
            String type = node.optString("t", "");
            if ("img".equals(type) || "vec".equals(type)) {
                images++;
            }
        }
        if (hasDynamicNode(nodes)) {
            Log.i(TAG, "Complication \"" + getKindId() + "\" renders a dynamic value as text, "
                    + "formatted when the face asks and refreshed when the timeline flips. A "
                    + "watch face slot takes a string, so it does not tick between updates.");
        }
        if (texts.size() > 2 || images > 1) {
            Log.i(TAG, "Complication \"" + getKindId() + "\" reduces its layout to what a watch "
                    + "face slot can show: " + Math.min(texts.size(), 2) + " of " + texts.size()
                    + " text node(s) and " + Math.min(images, 1) + " of " + images + " image(s). "
                    + "Containers, padding, background, alignment and per-node colour are the "
                    + "face's own, not the app's.");
        }
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= SHORT_TEXT_MAX ? text : text.substring(0, SHORT_TEXT_MAX);
    }

    /**
     * Whether a text came from a dynamic node, which a watch face can tick for itself.
     *
     * <p>Not used to choose the value -- {@link CN1WatchSurface#texts} already formats it -- but
     * to say so in the log, because a frozen countdown in a slot is the one degradation a
     * developer is most likely to mistake for a bug.</p>
     */
    private static boolean hasDynamicNode(List<JSONObject> nodes) {
        for (JSONObject node : nodes) {
            if ("dyn".equals(node.optString("t", ""))) {
                return true;
            }
        }
        return false;
    }

    private static String join(List<String> texts, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < texts.size(); i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(texts.get(i));
        }
        return sb.toString();
    }

    private static ComplicationText plain(String text) {
        return new PlainComplicationText.Builder(text == null ? "" : text).build();
    }

    private static ComplicationData noData() {
        return new NoDataComplicationData();
    }
}
