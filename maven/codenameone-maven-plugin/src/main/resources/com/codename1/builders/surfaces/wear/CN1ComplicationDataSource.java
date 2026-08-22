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
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;
import androidx.wear.watchface.complications.datasource.TimeInterval;
import androidx.wear.watchface.complications.datasource.TimelineEntry;

import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
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
        ComplicationType type = request.getComplicationType();
        ComplicationDataTimeline timeline = null;
        ComplicationData data = null;
        try {
            timeline = buildTimeline(type);
            if (timeline == null) {
                data = build(type, null);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not build complication data for kind " + getKindId(), t);
        }
        try {
            if (timeline != null) {
                // The whole timeline, not the entry showing now. This service is asked once and
                // UPDATE_PERIOD_SECONDS is deliberately 0, because polling a push-driven surface
                // costs watch battery for nothing -- so an answer of one value stayed on the face
                // for ever and the entries the app published for the hours ahead never appeared.
                //
                // setValidTimeRange does NOT solve that: it says when a value may be DISPLAYED and
                // schedules no further request, so on its own it replaced a stale complication
                // with an empty one. Handing the system the entries lets it swap them itself, at
                // the stated moments, without waking this process at all -- which is the same
                // bargain WidgetKit makes on the other platform, and what the published document
                // was shaped for.
                listener.onComplicationDataTimeline(timeline);
                return;
            }
            listener.onComplicationData(data == null ? noData() : data);
        } catch (Throwable t) {
            Log.w(TAG, "Could not deliver complication data for kind " + getKindId(), t);
        }
    }

    /**
     * Every published entry as a complication timeline, or null when there is nothing to serve.
     *
     * @param type the type the face asked for
     * @return the timeline, or null
     */
    private ComplicationDataTimeline buildTimeline(ComplicationType type) {
        List<CN1WatchSurface.Reading> readings =
                CN1WatchSurface.readTimeline(this, getKindId(), familyFor(type));
        if (readings.isEmpty()) {
            return null;
        }
        ComplicationData current = build(type, readings.get(0));
        if (current == null) {
            return null;
        }
        List<TimelineEntry> entries = new ArrayList<TimelineEntry>();
        for (int i = 1; i < readings.size(); i++) {
            CN1WatchSurface.Reading reading = readings.get(i);
            ComplicationData entry = build(type, reading);
            if (entry == null) {
                // A later entry this type cannot render is skipped rather than ending the
                // timeline: the entries after it may well be renderable, and the face falls back
                // to the default in the gap.
                continue;
            }
            long end = reading.getNextFlipDate();
            entries.add(new TimelineEntry(
                    new TimeInterval(Instant.ofEpochMilli(reading.getStart()),
                            end > reading.getStart() ? Instant.ofEpochMilli(end) : Instant.MAX),
                    entry));
        }
        return new ComplicationDataTimeline(current, entries);
    }

    @Override
    public ComplicationData getPreviewData(ComplicationType type) {
        try {
            ComplicationData data = build(type, null);
            if (data != null) {
                return data;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not build preview data for kind " + getKindId(), t);
        }
        // The gallery shows this before the app has ever published, so a placeholder that names
        // the kind beats an empty slot the user cannot identify. No validity: a placeholder does
        // not go stale, and expiring it would ask the system to come back for the same answer.
        //
        // Of the REQUESTED type, which is the part that matters. Wear takes preview data as an
        // answer about the type it asked about, and a ShortText handed to a slot advertising
        // LONG_TEXT or RANGED_VALUE is rejected or drawn empty -- so the kind would be missing
        // from exactly the pickers where its layout is roomiest.
        return placeholder(type);
    }

    /**
     * A named but empty stand-in for a kind that has published nothing yet.
     *
     * @param type the type the picker asked about
     * @return placeholder data of that type, or null when there is no sensible one
     */
    private ComplicationData placeholder(ComplicationType type) {
        String label = getKindId();
        if (ComplicationType.LONG_TEXT.equals(type)) {
            return new LongTextComplicationData.Builder(plain(label), plain(label)).build();
        }
        if (ComplicationType.RANGED_VALUE.equals(type)) {
            // Empty rather than arbitrary: an invented fraction in a picker is a claim about a
            // value the app has never published.
            return new RangedValueComplicationData.Builder(0f, 0f, 1f, plain(label))
                    .setText(plain(shorten(label)))
                    .build();
        }
        if (ComplicationType.MONOCHROMATIC_IMAGE.equals(type)) {
            // No icon exists before a publish, and this type is nothing but its icon, so there is
            // no honest placeholder to give. Null lets the picker fall back to another type.
            return null;
        }
        return shortText(shorten(label), null, null);
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

    private ComplicationData build(ComplicationType type, CN1WatchSurface.Reading given) {
        CN1WatchSurface.Reading reading = given != null ? given
                : CN1WatchSurface.read(this, getKindId(), familyFor(type));
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
            LongTextComplicationData.Builder builder =
                    new LongTextComplicationData.Builder(plain(body.length() == 0 ? title : body),
                            plain(title))
                            .setTitle(body.length() == 0 ? null : plain(title))
                            .setTapAction(tap);
            return builder.build();
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
            builder.setTapAction(tap);
            return builder.build();
        }
        if (ComplicationType.MONOCHROMATIC_IMAGE.equals(type)) {
            Icon icon = monochromeIcon(nodes, reading.getState());
            if (icon == null) {
                return null;
            }
            MonochromaticImageComplicationData.Builder builder =
                    new MonochromaticImageComplicationData.Builder(
                            new MonochromaticImage.Builder(icon).build(),
                            plain(texts.isEmpty() ? getKindId() : texts.get(0)))
                            .setTapAction(tap);
            return builder.build();
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

    private ShortTextComplicationData shortText(String text, String title,
            PendingIntent tap) {
        // The untruncated string becomes the content description, so a screen reader still hears
        // what the layout said even where the slot shows seven characters.
        ShortTextComplicationData.Builder builder =
                new ShortTextComplicationData.Builder(plain(shorten(text)), plain(text));
        if (title != null && title.length() > 0) {
            builder.setTitle(plain(title));
        }
        builder.setTapAction(tap);
        return builder.build();
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
