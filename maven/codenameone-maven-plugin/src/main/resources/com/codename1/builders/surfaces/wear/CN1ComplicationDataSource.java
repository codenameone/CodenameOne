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
import androidx.wear.watchface.complications.data.CountDownTimeReference;
import androidx.wear.watchface.complications.data.CountUpTimeReference;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText;
import androidx.wear.watchface.complications.data.TimeDifferenceStyle;
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
        // A current entry this type cannot render does NOT end the search, for the same reason a
        // later one does not: the entries after it may be renderable, and giving up here threw
        // them away permanently. A complication is asked once and handed the whole timeline, and
        // UPDATE_PERIOD_SECONDS is 0 by design -- so nothing would ever ask again, and a
        // RANGED_VALUE slot whose progress node only appears in the next entry stayed empty for
        // good. No-data covers the stretch before the first renderable entry, which is exactly
        // what the default in a ComplicationDataTimeline is for.
        ComplicationData current = build(type, readings.get(0));
        List<TimelineEntry> entries = new ArrayList<TimelineEntry>();
        for (int i = 1; i < readings.size(); i++) {
            CN1WatchSurface.Reading reading = readings.get(i);
            ComplicationData entry = build(type, reading);
            if (entry == null) {
                // No-data for its interval, not a skipped entry. Skipping it leaves no entry
                // covering that stretch, and what shows then is the timeline's DEFAULT -- which
                // is the current reading. So a published timeline that moved to an entry with
                // nothing this type can show kept displaying the old value as though it were
                // still current, which is worse than showing nothing.
                entry = noData();
            }
            long end = reading.getNextFlipDate();
            entries.add(new TimelineEntry(
                    new TimeInterval(Instant.ofEpochMilli(reading.getStart()),
                            end > reading.getStart() ? Instant.ofEpochMilli(end) : Instant.MAX),
                    entry));
        }
        // Exhausted NOW -- the entry being shown is the last one -- and the app asked to be woken
        // when that happened. Asked from the ACTIVE reading and not the final one: with future
        // entries still to come the final one also has no flip date, so reading it here made the
        // request hours early and never again at the moment it was for.
        //
        // Asked NOW, whenever the timeline reloads at its end -- not only when it is already
        // exhausted. This is the only moment the provider gets: it is handed the whole timeline
        // once, the system swaps entries itself, and UPDATE_PERIOD_SECONDS is 0 by design, so
        // nothing calls back when the last entry finally takes over. Waiting for exhaustion meant
        // the request was never made for the timeline that needed it most -- one published WITH
        // future entries -- and the final value then stood for ever.
        //
        // Asking early is safe because the request is throttled (tryClaimBackgroundFetch) and is
        // a no-op for an app that declares no background-fetch listener, which is the same
        // treatment the widget path gives it. The cost of asking early is one fetch; the cost of
        // not asking is a complication frozen on its last entry.
        if (current == null && entries.isEmpty()) {
            // Nothing this type can render, now or later. Answering with a timeline of nothing
            // but no-data would replace whatever the face is showing with a blank slot, so the
            // caller's own fallback is the better answer.
            return null;
        }
        CN1WatchSurface.Reading active = readings.get(0);
        // A relative countdown has to change SIDES when it reaches its target: "in 5m" becomes
        // "5m ago", which is what formatRelative does everywhere else. The direction is fixed by
        // the reference type handed to TimeDifferenceComplicationText, so one text cannot span
        // both -- and nothing would rebuild it, the provider being asked once with no update
        // period. The timeline is the mechanism for exactly this: a second entry starting at the
        // target, composed as the moment after it, carries the count-up form.
        long crossing = relativeCrossing(active);
        long until = active.getNextFlipDate();
        // Only INSIDE the active reading's own window. A crossing at or after the next flip
        // belongs to an entry this one has already handed over to, and an interval running to
        // Instant.MAX from there would overlap every later entry and compete with them -- a stale
        // reading resurfacing after it stopped being current. The entry that is active then will
        // compute its own crossing when it is built.
        if (crossing > 0 && (until <= 0 || crossing < until)) {
            ComplicationData after = build(type, active, crossing + 1);
            if (after != null) {
                entries.add(new TimelineEntry(
                        new TimeInterval(Instant.ofEpochMilli(crossing),
                                until > crossing ? Instant.ofEpochMilli(until) : Instant.MAX),
                        after));
            }
        }
        if (active.isReloadAtEnd()) {
            CN1WidgetProvider.requestAppRefresh(this, getKindId());
        }
        return new ComplicationDataTimeline(current == null ? noData() : current, entries);
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
        // The FULL label. shortText shortens what it shows and keeps what it is given as the
        // content description, so shortening first handed the picker and TalkBack the same seven
        // characters the slot already displays -- the published path was fixed for exactly this
        // and the placeholder kept doing it.
        return shortText(null, label, null, null);
    }

    /**
     * The portable family whose layout best serves a requested complication type.
     *
     * <p>The mapping is the one {@code WidgetSize} documents, read backwards: a face asking for
     * long text wants the roomy layout, one asking for a ranged value or a glyph wants the round
     * one, and short text is the readout every family can produce.</p>
     */
    ///
    /// A PREFERENCE, not a requirement, and that is why a kind declaring only watchCircular is
    /// still right to advertise SHORT_TEXT. The name returned here is the first thing
    /// CN1WatchSurface.pickLayout tries; when the published document has no layout under it the
    /// picker falls back through the kind's other watch layouts, and only families the kind
    /// actually declared are in the document at all -- so the fallback lands on one of its own,
    /// never on unrelated content. Selecting here among the declared families would move that
    /// decision to a place that cannot see the document.
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
        return build(type, given, System.currentTimeMillis());
    }

    /// As {@link #build(ComplicationType, CN1WatchSurface.Reading)}, but resolving anything that
    /// depends on "now" against a stated moment. Used to build the entry that takes over when a
    /// relative countdown reaches its target -- that entry has to be composed as the moment AFTER
    /// the target, not as now.
    private ComplicationData build(ComplicationType type, CN1WatchSurface.Reading given,
            long asOf) {
        CN1WatchSurface.Reading reading = given != null ? given
                : CN1WatchSurface.read(this, getKindId(), familyFor(type));
        if (reading == null) {
            return null;
        }
        List<JSONObject> nodes = CN1WatchSurface.flatten(reading.getLayout());
        List<String> texts = CN1WatchSurface.texts(nodes, reading.getState());
        PendingIntent tap = tapIntent(reading.getLayout());
        reportDroppedContent(nodes, texts);
        // The node the primary text came from, so a dynamic one can be handed over as a value the
        // FACE ticks rather than a string frozen at this request. With the update period at 0
        // there is no later request to refresh it, so a countdown really did stop.
        ComplicationText primary = texts.isEmpty() ? null
                : textFor(firstTextNode(nodes), reading.getState(), texts.get(0), asOf);

        if (ComplicationType.LONG_TEXT.equals(type)) {
            String title = texts.isEmpty() ? getKindId() : texts.get(0);
            String body = texts.size() > 1 ? join(texts, 1) : "";
            ComplicationText titleText = primary != null ? primary : plain(title);
            // The BODY can tick as well, and in a rectangular layout it is the likelier place for
            // a countdown -- a static label first, the moving value beneath it. Only when the body
            // is exactly one node, though: a join of several is a string, and there is nothing to
            // hand a face that would advance part of it.
            ComplicationText bodyText = texts.size() == 2
                    ? textFor(secondTextNode(nodes), reading.getState(), texts.get(1), asOf)
                    : plain(body);
            // The whole value, not just the title. TalkBack reads this instead of the layout, so
            // omitting the body announced the label of a complication without the thing it says
            // -- the order status, the message, the number the user actually wanted.
            String spoken = body.length() == 0 ? title : title + ", " + body;
            LongTextComplicationData.Builder builder =
                    new LongTextComplicationData.Builder(
                            body.length() == 0 ? titleText : bodyText, plain(spoken))
                            .setTitle(body.length() == 0 ? null : titleText)
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
                builder.setText(primary != null ? primary : plain(shorten(texts.get(0))));
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
            // UNTRUNCATED. shortText shortens what it displays and keeps what it is given as the
            // content description, so shortening first handed a screen reader the same seven
            // characters the slot already shows -- losing exactly the text the description exists
            // to supply.
            return shortText(primary, texts.get(0),
                    texts.size() > 1 ? shorten(texts.get(1)) : null, tap);
        }
        return null;
    }

    /**
     * When this reading's primary text stops counting down and starts counting up, or 0.
     *
     * <p>Only a {@code relative} node crosses: a timer counts one way by definition, and a clock
     * or a date does not move at all. Only a target still in the FUTURE matters -- one already
     * past is counting up from the start and never changes side again.</p>
     *
     * @param reading the entry being rendered
     * @return the crossing moment in epoch millis, or 0 when nothing crosses
     */
    private long relativeCrossing(CN1WatchSurface.Reading reading) {
        if (reading == null) {
            return 0;
        }
        List<JSONObject> nodes = CN1WatchSurface.flatten(reading.getLayout());
        // BOTH nodes that can be handed over as ticking text, not only the first: a rectangular
        // layout routinely puts the label first and the moving value beneath it, and the body
        // ticks too. The earliest crossing wins, since that is when the timeline first differs.
        long first = relativeCrossingOf(firstTextNode(nodes), reading);
        long second = relativeCrossingOf(secondTextNode(nodes), reading);
        if (first <= 0) {
            return second;
        }
        if (second <= 0) {
            return first;
        }
        return Math.min(first, second);
    }

    /// One node's crossing moment, or 0 when it has none.
    private long relativeCrossingOf(JSONObject node, CN1WatchSurface.Reading reading) {
        if (node == null || !"dyn".equals(node.optString("t", ""))
                || !"relative".equals(node.optString("style", "timerDown"))) {
            return 0;
        }
        long date = CN1WatchSurface.dynamicDate(node, reading.getState());
        return date > System.currentTimeMillis() ? date : 0;
    }

    /// The second text-bearing node, which is a long-text body when there is exactly one.
    private static JSONObject secondTextNode(List<JSONObject> nodes) {
        boolean seenFirst = false;
        for (JSONObject node : nodes) {
            String type = node.optString("t", "");
            if ("text".equals(type) || "dyn".equals(type)) {
                if (seenFirst) {
                    return node;
                }
                seenFirst = true;
            }
        }
        return null;
    }

    /// The first text-bearing node, so the caller can ask what KIND of text it is.
    ///
    /// texts() returns resolved strings and deliberately says nothing about where they came from;
    /// a native ticking value needs the node itself.
    private static JSONObject firstTextNode(List<JSONObject> nodes) {
        for (JSONObject node : nodes) {
            String type = node.optString("t", "");
            if ("text".equals(type) || "dyn".equals(type)) {
                return node;
            }
        }
        return null;
    }

    /// A node's complication text: the face-ticked form where the node is dynamic and the style
    /// is one that moves, a plain string otherwise.
    private ComplicationText textFor(JSONObject node, JSONObject state, String resolved,
            long asOf) {
        if (node != null && "dyn".equals(node.optString("t", ""))) {
            ComplicationText ticking = tickingText(node, state, asOf);
            if (ticking != null) {
                return ticking;
            }
        }
        return plain(resolved);
    }

    /**
     * A dynamic node as a value the watch face advances itself, or null when it cannot be one.
     *
     * <p>Only the three time-RELATIVE styles qualify. {@code time} and {@code date} format the
     * node's OWN timestamp -- a published moment, not the current one -- so handing them to a
     * clock text would replace the value with whatever time it is now, which is a different
     * number and a wrong one. They stay plain, and correctly so: nothing about them moves.</p>
     *
     * <p>The reward for the three that do qualify is that the face redraws them from its own
     * clock with no wake-up, which is the only way a countdown ticks at all here: the generated
     * provider sets no update period, so there is no second request in which to re-render it.</p>
     *
     * @param node a {@code dyn} node
     * @param state the entry state, which may supply the date by key
     * @return the ticking text, or null to fall back to a plain string
     */
    private ComplicationText tickingText(JSONObject node, JSONObject state, long asOf) {
        long date = CN1WatchSurface.dynamicDate(node, state);
        if (date <= 0 || Build.VERSION.SDK_INT < 26) {
            return null;
        }
        String style = node.optString("style", "timerDown");
        try {
            java.time.Instant at = java.time.Instant.ofEpochMilli(date);
            if ("relative".equals(style)) {
                // "in 3m" / "3m ago" -- a single unit, which is what a relative date reads as.
                return date > asOf
                        ? new TimeDifferenceComplicationText.Builder(
                                TimeDifferenceStyle.SHORT_SINGLE_UNIT,
                                new CountDownTimeReference(at)).build()
                        : new TimeDifferenceComplicationText.Builder(
                                TimeDifferenceStyle.SHORT_SINGLE_UNIT,
                                new CountUpTimeReference(at)).build();
            }
            if ("timerUp".equals(style)) {
                return new TimeDifferenceComplicationText.Builder(TimeDifferenceStyle.STOPWATCH,
                        new CountUpTimeReference(at)).build();
            }
            if ("timerDown".equals(style)) {
                return new TimeDifferenceComplicationText.Builder(TimeDifferenceStyle.STOPWATCH,
                        new CountDownTimeReference(at)).build();
            }
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Could not build a ticking complication text for kind " + getKindId(), t);
            return null;
        }
    }

    private ShortTextComplicationData shortText(ComplicationText ticking, String text,
            String title, PendingIntent tap) {
        // The untruncated string becomes the content description, so a screen reader still hears
        // what the layout said even where the slot shows seven characters.
        //
        // A ticking value is handed over whole: shortening it would mean rendering it here, which
        // is the freezing this exists to avoid. The face sizes what it draws.
        ShortTextComplicationData.Builder builder =
                new ShortTextComplicationData.Builder(
                        ticking != null ? ticking : plain(shorten(text)), plain(text));
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

    /**
     * A request code for a tap intent, derived from its data string.
     *
     * <p>A PendingIntent request code is an {@code int} by API, so this cannot be widened the way
     * the tile resource ids were. What it can avoid is String.hashCode's constructible
     * collisions -- the ones short human-chosen strings actually hit, "Aa" and "BB" being the
     * standard example. Extras are not part of {@code filterEquals}, so two complications whose
     * data strings collided here would share one PendingIntent and the later would overwrite the
     * earlier's extras; a digest makes that an accident nobody has managed to have rather than
     * something a pair of kind ids can stumble into.</p>
     *
     * @param data the intent's data string
     * @return the request code
     */
    private static int requestCode(String data) {
        String material = data == null ? "" : data;
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes("UTF-8"));
            return ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16)
                    | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
        } catch (Exception noDigest) {
            return material.hashCode();
        }
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
            return PendingIntent.getActivity(this, requestCode(intent.getDataString()), intent,
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

    /**
     * Cuts a value down to what a short-text slot shows, without cutting a character in half.
     *
     * <p>The limit counts CODE POINTS rather than UTF-16 units, for two reasons that agree. It is
     * what the limit means -- Wear's guidance is about characters a face can show, and a
     * supplementary character is one of them. And a cut landing between the halves of one leaves
     * a lone surrogate, which PlainComplicationText replaces or rejects: the slot then shows a
     * corrupt character instead of the published value.</p>
     */
    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= SHORT_TEXT_MAX) {
            return text;
        }
        int points = text.codePointCount(0, text.length());
        if (points <= SHORT_TEXT_MAX) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, SHORT_TEXT_MAX));
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
