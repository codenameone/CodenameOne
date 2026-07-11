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
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// Renders a serialized surface descriptor node (the canonical JSON wire format produced by
/// `com.codename1.surfaces.SurfaceSerializer`) into a `RemoteViews` tree composed from the
/// pre-baked layout resources the build copies into the app (`res/layout/cn1_surface_*.xml`).
/// The renderer runs while the Codename One VM may not be initialized (widget updates, alarms),
/// so it only depends on the Android SDK and `org.json`.
///
/// #### RemoteViews approximations of the LCD contract
///
/// RemoteViews is the constrained floor of the surfaces catalog; the following approximations
/// are applied and documented here once for the whole Android lowering:
///
/// - **weight**: `RemoteViews` cannot set `layout_weight` at runtime, so any child with
///   `weight >= 1` is wrapped in a pre-baked cell layout with `layout_weight="1"`. Relative
///   weights collapse to equal shares. A `spacer` without a `min` size uses the same weighted
///   cell. Weighted children expand reliably when their container is stretched by its parent
///   (the root always is); inside a container that wraps its content they may collapse to zero.
/// - **fixed size** (`w`/`h`): applied through `RemoteViews.setViewLayoutWidth/Height` on
///   API 31+ (invoked reflectively because the port compiles against an older `android.jar`);
///   ignored below API 31 except for images, which are decoded/scaled to the requested size.
/// - **alignment** (`align`): per-child gravity cannot be set through `RemoteViews`. Row
///   layouts are pre-baked with vertical-center gravity and box children stack top-leading.
///   On API 31+ (`LinearLayout.setGravity` became remotable in S) the first aligned child of a
///   column/row sets the gravity of the whole container; mixed alignments are not supported.
/// - **corner radius**: any non-zero radius uses the pre-baked 12dp rounded drawable with the
///   background color applied through `setBackgroundTintList` on API 31+; below API 31 the
///   background renders square (`setBackgroundColor`).
/// - **dynamic text**: `timerDown`/`timerUp` map to a native `Chronometer` (count-down needs
///   API 24; below that the remaining time renders as static text). `time` maps to `TextClock`
///   which always shows the *current* time. `date` and `relative` render as static text
///   computed at render time and refresh only on the next widget update. Font weight on
///   dynamic text is ignored (spans cannot be applied to OS-generated text).
/// - **progress**: determinate circular progress falls back to the linear bar. A date-interval
///   progress freezes its fraction at render time. Progress tint is applied on API 31+ only.
/// - **spacing**: `LinearLayout` has no runtime divider API, so container spacing becomes
///   leading padding on every child but the first.
/// - **dark mode**: colors resolve against the current `uiMode` at render time, so widgets
///   recolor on the next update after a light/dark switch (the OS does not re-render published
///   RemoteViews on configuration changes by itself).
/// - **images**: decoded from the published PNG blobs, downsampled so the longest edge is at
///   most 512px, with a cumulative budget of about 500kb of bitmap data per rendered tree
///   (RemoteViews parcels bitmaps over a 1mb binder transaction). Images beyond the budget are
///   skipped with a logged warning.
public final class CN1SurfaceRenderer {
    private static final String TAG = "CN1Surfaces";
    private static final int MAX_DEPTH = 8;
    private static final int MAX_BITMAP_DIMENSION = 512;
    private static final int BITMAP_BUDGET_BYTES = 500 * 1024;
    /// PendingIntent.FLAG_IMMUTABLE; declared locally because the port compiles against an
    /// android.jar that predates the constant being commonly available.
    private static final int FLAG_IMMUTABLE = 0x04000000;

    private static final int LABEL_LIGHT = 0xff1c1c1e;
    private static final int LABEL_DARK = 0xffffffff;
    private static final int SECONDARY_LABEL_LIGHT = 0xff6c6c70;
    private static final int SECONDARY_LABEL_DARK = 0xffaeaeb2;
    private static final int BACKGROUND_LIGHT = 0xffffffff;
    private static final int BACKGROUND_DARK = 0xff1c1c1e;
    private static final int ACCENT = 0xff007aff;

    private static final Map<String, Integer> resourceIdCache = new HashMap<String, Integer>();

    private CN1SurfaceRenderer() {
    }

    /// Renders a descriptor node tree into a `RemoteViews` hierarchy.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: an Android context
    /// - `node`: the root node of the serialized layout
    /// - `state`: the state map whose values resolve `${key}` placeholders, may be null
    /// - `source`: the widget kind id or live activity type, delivered with action events
    /// - `imagesDir`: the directory holding the published `<name>.png` blobs, may be null
    ///
    /// #### Returns
    ///
    /// the rendered RemoteViews, never null
    public static RemoteViews render(Context ctx, JSONObject node, JSONObject state,
            String source, File imagesDir) {
        RenderContext rc = new RenderContext(ctx, state, source, imagesDir);
        return renderNode(node, rc, 0);
    }

    private static final class RenderContext {
        final Context ctx;
        final String pkg;
        final JSONObject state;
        final String source;
        final File imagesDir;
        final boolean dark;
        final float density;
        int bitmapBytes;

        RenderContext(Context ctx, JSONObject state, String source, File imagesDir) {
            this.ctx = ctx;
            this.pkg = ctx.getPackageName();
            this.state = state;
            this.source = source;
            this.imagesDir = imagesDir;
            int night = ctx.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            this.dark = night == Configuration.UI_MODE_NIGHT_YES;
            this.density = ctx.getResources().getDisplayMetrics().density;
        }

        int layout(String name) {
            return resolveId(ctx, name, "layout");
        }

        int rootId() {
            return resolveId(ctx, "cn1_surface_root", "id");
        }

        int drawable(String name) {
            return resolveId(ctx, name, "drawable");
        }

        int dip(int dips) {
            return Math.round(dips * density);
        }
    }

    private static int resolveId(Context ctx, String name, String type) {
        String key = ctx.getPackageName() + '/' + type + '/' + name;
        synchronized (resourceIdCache) {
            Integer cached = resourceIdCache.get(key);
            if (cached != null) {
                return cached.intValue();
            }
        }
        int id = ctx.getResources().getIdentifier(name, type, ctx.getPackageName());
        synchronized (resourceIdCache) {
            resourceIdCache.put(key, Integer.valueOf(id));
        }
        if (id == 0) {
            Log.w(TAG, "Missing surface resource " + type + "/" + name
                    + "; was the app built with surfaces support?");
        }
        return id;
    }

    private static RemoteViews renderNode(JSONObject node, RenderContext rc, int depth) {
        if (node == null || depth > MAX_DEPTH) {
            if (depth > MAX_DEPTH) {
                Log.w(TAG, "Surface descriptor exceeds the maximum depth of " + MAX_DEPTH
                        + "; deeper nodes are dropped");
            }
            return new RemoteViews(rc.pkg, rc.layout("cn1_surface_spacer"));
        }
        String type = node.optString("t", "");
        RemoteViews rv;
        if ("col".equals(type)) {
            rv = renderContainer(node, rc, depth, false);
        } else if ("row".equals(type)) {
            rv = renderContainer(node, rc, depth, true);
        } else if ("box".equals(type)) {
            rv = renderBox(node, rc, depth);
        } else if ("text".equals(type)) {
            rv = renderText(node, rc);
        } else if ("dyn".equals(type)) {
            rv = renderDynamicText(node, rc);
        } else if ("img".equals(type)) {
            rv = renderImage(node, rc);
        } else if ("prog".equals(type)) {
            rv = renderProgress(node, rc);
        } else if ("spacer".equals(type)) {
            rv = renderSpacer(node, rc);
        } else {
            Log.w(TAG, "Unknown surface node type '" + type + "'; rendering as empty space");
            rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_spacer"));
        }
        applyCommon(rv, node, rc);
        return rv;
    }

    // --- containers -----------------------------------------------------------

    private static RemoteViews renderContainer(JSONObject node, RenderContext rc, int depth,
            boolean horizontal) {
        RemoteViews rv = new RemoteViews(rc.pkg,
                rc.layout(horizontal ? "cn1_surface_row" : "cn1_surface_column"));
        int rootId = rc.rootId();
        int spacing = node.optInt("spacing", 0);
        JSONArray ch = node.optJSONArray("ch");
        if (ch == null) {
            return rv;
        }
        for (int i = 0; i < ch.length(); i++) {
            JSONObject child = ch.optJSONObject(i);
            if (child == null) {
                continue;
            }
            RemoteViews childRv = renderNode(child, rc, depth + 1);
            boolean weighted = child.optInt("weight", 0) >= 1 || isExpandingSpacer(child);
            String cellName;
            if (horizontal) {
                cellName = weighted ? "cn1_surface_cell_weight1_h" : "cn1_surface_cell_h";
            } else {
                cellName = weighted ? "cn1_surface_cell_weight1_v" : "cn1_surface_cell_v";
            }
            RemoteViews cell = new RemoteViews(rc.pkg, rc.layout(cellName));
            cell.addView(rootId, childRv);
            // container spacing becomes leading padding on every cell but the first, and a
            // fixed-minimum spacer becomes cell padding along the container axis (padding
            // contributes to a wrapped view's size on every API level)
            int leadPad = i > 0 && spacing > 0 ? rc.dip(spacing) : 0;
            if ("spacer".equals(child.optString("t"))) {
                int min = child.optInt("min", 0);
                if (min > 0) {
                    leadPad += rc.dip(min);
                }
            }
            if (leadPad > 0) {
                if (horizontal) {
                    cell.setViewPadding(rootId, leadPad, 0, 0, 0);
                } else {
                    cell.setViewPadding(rootId, 0, leadPad, 0, 0);
                }
            }
            applyFixedSize(cell, child, rc);
            rv.addView(rootId, cell);
        }
        applyChildAlignment(rv, ch, rc);
        return rv;
    }

    private static RemoteViews renderBox(JSONObject node, RenderContext rc, int depth) {
        RemoteViews rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_box"));
        int rootId = rc.rootId();
        JSONArray ch = node.optJSONArray("ch");
        if (ch == null) {
            return rv;
        }
        for (int i = 0; i < ch.length(); i++) {
            JSONObject child = ch.optJSONObject(i);
            if (child == null) {
                continue;
            }
            // FrameLayout children stack top-leading; per-child gravity is not remotable.
            RemoteViews childRv = renderNode(child, rc, depth + 1);
            applyFixedSize(childRv, child, rc);
            rv.addView(rootId, childRv);
        }
        return rv;
    }

    private static boolean isExpandingSpacer(JSONObject node) {
        return "spacer".equals(node.optString("t")) && !node.has("min");
    }

    /// API 31+ approximation: the first child that declares an alignment sets the gravity of
    /// the whole LinearLayout container (LinearLayout#setGravity became remotable in S).
    private static void applyChildAlignment(RemoteViews rv, JSONArray ch, RenderContext rc) {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        for (int i = 0; i < ch.length(); i++) {
            JSONObject child = ch.optJSONObject(i);
            if (child == null) {
                continue;
            }
            String align = child.optString("align", null);
            if (align != null && align.length() > 0) {
                rv.setInt(rc.rootId(), "setGravity", gravityFor(align));
                return;
            }
        }
    }

    private static int gravityFor(String align) {
        if ("topLeading".equals(align)) {
            return Gravity.TOP | Gravity.START;
        }
        if ("top".equals(align)) {
            return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        }
        if ("topTrailing".equals(align)) {
            return Gravity.TOP | Gravity.END;
        }
        if ("leading".equals(align)) {
            return Gravity.CENTER_VERTICAL | Gravity.START;
        }
        if ("trailing".equals(align)) {
            return Gravity.CENTER_VERTICAL | Gravity.END;
        }
        if ("bottomLeading".equals(align)) {
            return Gravity.BOTTOM | Gravity.START;
        }
        if ("bottom".equals(align)) {
            return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        }
        if ("bottomTrailing".equals(align)) {
            return Gravity.BOTTOM | Gravity.END;
        }
        return Gravity.CENTER;
    }

    // --- leaf nodes -----------------------------------------------------------

    private static RemoteViews renderText(JSONObject node, RenderContext rc) {
        RemoteViews rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_text"));
        int rootId = rc.rootId();
        String text = interpolate(node.optString("text", ""), rc.state);
        rv.setTextViewText(rootId, styledText(text, node.optString("fw", null)));
        applyTextStyle(rv, node, rc);
        int maxLines = node.optInt("maxLines", 0);
        if (maxLines > 0) {
            rv.setInt(rootId, "setMaxLines", maxLines);
        }
        return rv;
    }

    private static RemoteViews renderDynamicText(JSONObject node, RenderContext rc) {
        int rootId = rc.rootId();
        String style = node.optString("style", "timerDown");
        long date = resolveDate(node, rc);
        long now = System.currentTimeMillis();
        RemoteViews rv;
        if ("timerDown".equals(style) || "timerUp".equals(style)) {
            boolean down = "timerDown".equals(style);
            if (down && Build.VERSION.SDK_INT < 24) {
                // Chronometer count-down needs API 24; render the remaining time statically.
                rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_text"));
                rv.setTextViewText(rootId, formatElapsed(Math.max(0, date - now)));
            } else {
                rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_chronometer"));
                long base;
                if (down) {
                    base = SystemClock.elapsedRealtime() + (date - now);
                } else {
                    base = SystemClock.elapsedRealtime() - Math.max(0, now - date);
                }
                rv.setChronometer(rootId, base, null, true);
                if (down) {
                    rv.setChronometerCountDown(rootId, true);
                }
            }
        } else if ("time".equals(style)) {
            // TextClock always displays the current time; a fixed timestamp's time-of-day is
            // not representable natively on Android.
            rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_textclock"));
        } else if ("date".equals(style)) {
            rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_text"));
            rv.setTextViewText(rootId, DateFormat.getDateInstance().format(new Date(date)));
        } else {
            // relative: static approximation computed at render time; refreshes on the next
            // widget update rather than continuously.
            rv = new RemoteViews(rc.pkg, rc.layout("cn1_surface_text"));
            rv.setTextViewText(rootId, DateUtils.getRelativeTimeSpanString(date, now,
                    DateUtils.MINUTE_IN_MILLIS));
        }
        applyTextStyle(rv, node, rc);
        return rv;
    }

    private static RemoteViews renderImage(JSONObject node, RenderContext rc) {
        String scale = node.optString("scale", "fit");
        String layoutName;
        if ("fill".equals(scale)) {
            layoutName = "cn1_surface_image_fill";
        } else if ("center".equals(scale)) {
            layoutName = "cn1_surface_image_center";
        } else {
            layoutName = "cn1_surface_image";
        }
        RemoteViews rv = new RemoteViews(rc.pkg, rc.layout(layoutName));
        int rootId = rc.rootId();
        Bitmap bmp = loadBitmap(node.optString("name", ""), node, rc);
        if (bmp != null) {
            rv.setImageViewBitmap(rootId, bmp);
        }
        JSONObject tint = node.optJSONObject("tint");
        if (tint != null) {
            rv.setInt(rootId, "setColorFilter", resolveColor(tint, rc, LABEL_LIGHT, LABEL_DARK));
        }
        return rv;
    }

    private static RemoteViews renderProgress(JSONObject node, RenderContext rc) {
        // Determinate circular progress is not renderable in an app widget; the circular
        // layout is a pre-baked linear fallback per the LCD contract.
        boolean circular = "circular".equals(node.optString("style", "linear"));
        RemoteViews rv = new RemoteViews(rc.pkg,
                rc.layout(circular ? "cn1_surface_progress_circular" : "cn1_surface_progress"));
        int rootId = rc.rootId();
        double fraction = resolveFraction(node, rc);
        rv.setProgressBar(rootId, 1000, (int) Math.round(fraction * 1000), false);
        JSONObject color = node.optJSONObject("color");
        if (color != null && Build.VERSION.SDK_INT >= 31) {
            setColorStateList(rv, rootId, "setProgressTintList",
                    resolveColor(color, rc, ACCENT, ACCENT));
        }
        return rv;
    }

    private static RemoteViews renderSpacer(JSONObject node, RenderContext rc) {
        // A spacer's behavior is realized by its parent container: without "min" it rides a
        // weighted cell (expanding), with "min" the cell gets axis-aligned padding. A spacer
        // used outside a column/row (root or box child) has no effect on Android.
        return new RemoteViews(rc.pkg, rc.layout("cn1_surface_spacer"));
    }

    // --- shared styling -------------------------------------------------------

    private static void applyCommon(RemoteViews rv, JSONObject node, RenderContext rc) {
        int rootId = rc.rootId();
        JSONArray pad = node.optJSONArray("pad");
        if (pad != null && pad.length() == 4) {
            // wire order: [top, right, bottom, left]
            rv.setViewPadding(rootId, rc.dip(pad.optInt(3)), rc.dip(pad.optInt(0)),
                    rc.dip(pad.optInt(1)), rc.dip(pad.optInt(2)));
        }
        JSONObject bg = node.optJSONObject("bg");
        if (bg != null) {
            int color = resolveColor(bg, rc, BACKGROUND_LIGHT, BACKGROUND_DARK);
            int corner = node.optInt("corner", 0);
            if (corner > 0 && Build.VERSION.SDK_INT >= 31) {
                // The rounded drawable has a fixed 12dp radius; the node radius selects
                // rounded-vs-square only.
                rv.setInt(rootId, "setBackgroundResource", rc.drawable("cn1_surface_rounded"));
                setColorStateList(rv, rootId, "setBackgroundTintList", color);
            } else {
                // Below API 31 backgroundTintList is not remotable, so corners render square.
                rv.setInt(rootId, "setBackgroundColor", color);
            }
        }
        JSONObject action = node.optJSONObject("action");
        if (action != null) {
            applyAction(rv, action, rc);
        }
    }

    private static void applyTextStyle(RemoteViews rv, JSONObject node, RenderContext rc) {
        int rootId = rc.rootId();
        int size = node.optInt("size", 0);
        if (size > 0) {
            rv.setTextViewTextSize(rootId, TypedValue.COMPLEX_UNIT_DIP, size);
        }
        JSONObject color = node.optJSONObject("color");
        if (color != null) {
            rv.setTextColor(rootId, resolveColor(color, rc, LABEL_LIGHT, LABEL_DARK));
        } else {
            rv.setTextColor(rootId, rc.dark ? LABEL_DARK : LABEL_LIGHT);
        }
    }

    private static CharSequence styledText(String text, String fw) {
        if ("semibold".equals(fw) || "bold".equals(fw)) {
            SpannableString s = new SpannableString(text);
            s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, s.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return s;
        }
        // light/regular/medium render as regular per the LCD contract
        return text;
    }

    private static void applyFixedSize(RemoteViews rv, JSONObject node, RenderContext rc) {
        int w = node.optInt("w", 0);
        int h = node.optInt("h", 0);
        if ((w <= 0 && h <= 0) || Build.VERSION.SDK_INT < 31) {
            // setViewLayoutWidth/Height exist from API 31 only; fixed sizes are ignored below
            // (images still honor them through bitmap scaling in loadBitmap).
            return;
        }
        try {
            Method m = RemoteViews.class.getMethod("setViewLayoutWidth",
                    int.class, float.class, int.class);
            if (w > 0) {
                m.invoke(rv, Integer.valueOf(rc.rootId()), Float.valueOf(w),
                        Integer.valueOf(TypedValue.COMPLEX_UNIT_DIP));
            }
            if (h > 0) {
                Method mh = RemoteViews.class.getMethod("setViewLayoutHeight",
                        int.class, float.class, int.class);
                mh.invoke(rv, Integer.valueOf(rc.rootId()), Float.valueOf(h),
                        Integer.valueOf(TypedValue.COMPLEX_UNIT_DIP));
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to apply fixed size", t);
        }
    }

    private static void applyAction(RemoteViews rv, JSONObject action, RenderContext rc) {
        String actionId = action.optString("id", "");
        JSONObject params = action.optJSONObject("p");
        String paramsJson = params == null ? null : params.toString();
        Intent intent = new Intent(rc.ctx, CN1SurfaceActionActivity.class);
        intent.putExtra(CN1SurfaceActionActivity.EXTRA_SOURCE, rc.source);
        intent.putExtra(CN1SurfaceActionActivity.EXTRA_ACTION_ID, actionId);
        if (paramsJson != null) {
            intent.putExtra(CN1SurfaceActionActivity.EXTRA_ACTION_PARAMS, paramsJson);
        }
        // The canonical deep-link form doubles as a uniqueness key so PendingIntents with
        // different extras never collide.
        StringBuilder uri = new StringBuilder("cn1surface://a?src=");
        uri.append(Uri.encode(rc.source == null ? "" : rc.source));
        uri.append("&id=").append(Uri.encode(actionId));
        if (paramsJson != null) {
            uri.append("&p=").append(Uri.encode(paramsJson));
        }
        intent.setData(Uri.parse(uri.toString()));
        int requestCode = uri.toString().hashCode();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(rc.ctx, requestCode, intent, flags);
        rv.setOnClickPendingIntent(rc.rootId(), pi);
    }

    /// RemoteViews#setColorStateList exists from API 31 only and the port compiles against an
    /// older android.jar, so the call goes through reflection (an SDK method name, safe under
    /// obfuscation).
    private static void setColorStateList(RemoteViews rv, int viewId, String method, int color) {
        try {
            Method m = RemoteViews.class.getMethod("setColorStateList",
                    int.class, String.class, android.content.res.ColorStateList.class);
            m.invoke(rv, Integer.valueOf(viewId), method,
                    android.content.res.ColorStateList.valueOf(color));
        } catch (Throwable t) {
            Log.w(TAG, "Failed to apply color state list", t);
        }
    }

    // --- value resolution -----------------------------------------------------

    private static int resolveColor(JSONObject color, RenderContext rc, int fallbackLight,
            int fallbackDark) {
        String role = color.optString("role", null);
        if (role != null && role.length() > 0) {
            if ("label".equals(role)) {
                return rc.dark ? LABEL_DARK : LABEL_LIGHT;
            }
            if ("secondaryLabel".equals(role)) {
                return rc.dark ? SECONDARY_LABEL_DARK : SECONDARY_LABEL_LIGHT;
            }
            if ("background".equals(role)) {
                return rc.dark ? BACKGROUND_DARK : BACKGROUND_LIGHT;
            }
            if ("accent".equals(role)) {
                return ACCENT;
            }
            return rc.dark ? fallbackDark : fallbackLight;
        }
        if (color.has("l") || color.has("d")) {
            long l = color.optLong("l", fallbackLight);
            long d = color.optLong("d", l);
            return (int) (rc.dark ? d : l);
        }
        return rc.dark ? fallbackDark : fallbackLight;
    }

    private static long resolveDate(JSONObject node, RenderContext rc) {
        String dateKey = node.optString("dateKey", null);
        if (dateKey != null && dateKey.length() > 0 && rc.state != null) {
            Object v = rc.state.opt(dateKey);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            if (v instanceof String) {
                try {
                    return Long.parseLong((String) v);
                } catch (NumberFormatException ignore) {
                }
            }
        }
        return node.optLong("date", System.currentTimeMillis());
    }

    private static double resolveFraction(JSONObject node, RenderContext rc) {
        double fraction;
        String valueKey = node.optString("valueKey", null);
        if (valueKey != null && valueKey.length() > 0 && rc.state != null
                && rc.state.opt(valueKey) instanceof Number) {
            fraction = ((Number) rc.state.opt(valueKey)).doubleValue();
        } else if (node.has("start") && node.has("end")) {
            // Date-interval progress freezes at render time on Android; the next widget
            // update recomputes it.
            long start = node.optLong("start");
            long end = node.optLong("end");
            long now = System.currentTimeMillis();
            fraction = end <= start ? 1d : (now - start) / (double) (end - start);
        } else {
            fraction = node.optDouble("value", 0d);
        }
        return Math.max(0d, Math.min(1d, fraction));
    }

    private static Bitmap loadBitmap(String name, JSONObject node, RenderContext rc) {
        if (name == null || name.length() == 0 || rc.imagesDir == null) {
            return null;
        }
        // published names are content-hash based ("img<hex>"); reject anything path-like
        if (!name.matches("[a-zA-Z0-9_-]+")) {
            Log.w(TAG, "Ignoring surface image with suspicious name '" + name + "'");
            return null;
        }
        File f = new File(rc.imagesDir, name + ".png");
        if (!f.exists()) {
            Log.w(TAG, "Surface image " + f + " was not published");
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), bounds);
            int maxDim = Math.max(bounds.outWidth, bounds.outHeight);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (maxDim / opts.inSampleSize > MAX_BITMAP_DIMENSION) {
                opts.inSampleSize *= 2;
            }
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            if (bmp == null) {
                return null;
            }
            int w = node.optInt("w", 0);
            int h = node.optInt("h", 0);
            if (w > 0 || h > 0) {
                bmp = scaleToFixedSize(bmp, w, h, "fill".equals(node.optString("scale")), rc);
            }
            int bytes = bmp.getWidth() * bmp.getHeight() * 4;
            if (rc.bitmapBytes + bytes > BITMAP_BUDGET_BYTES) {
                Log.w(TAG, "Skipping surface image '" + name + "': the rendered tree exceeds "
                        + (BITMAP_BUDGET_BYTES / 1024) + "kb of bitmap data (binder "
                        + "transactions cap RemoteViews payloads)");
                return null;
            }
            rc.bitmapBytes += bytes;
            return bmp;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to decode surface image " + f, t);
            return null;
        }
    }

    private static Bitmap scaleToFixedSize(Bitmap bmp, int wDips, int hDips, boolean fill,
            RenderContext rc) {
        int targetW = wDips > 0 ? rc.dip(wDips) : 0;
        int targetH = hDips > 0 ? rc.dip(hDips) : 0;
        if (targetW <= 0) {
            targetW = Math.round(bmp.getWidth() * (targetH / (float) bmp.getHeight()));
        }
        if (targetH <= 0) {
            targetH = Math.round(bmp.getHeight() * (targetW / (float) bmp.getWidth()));
        }
        targetW = Math.min(targetW, MAX_BITMAP_DIMENSION);
        targetH = Math.min(targetH, MAX_BITMAP_DIMENSION);
        if (targetW <= 0 || targetH <= 0) {
            return bmp;
        }
        try {
            if (fill) {
                // center-crop to the requested aspect before scaling
                float scale = Math.max(targetW / (float) bmp.getWidth(),
                        targetH / (float) bmp.getHeight());
                int cropW = Math.min(bmp.getWidth(), Math.round(targetW / scale));
                int cropH = Math.min(bmp.getHeight(), Math.round(targetH / scale));
                int x = (bmp.getWidth() - cropW) / 2;
                int y = (bmp.getHeight() - cropH) / 2;
                bmp = Bitmap.createBitmap(bmp, x, y, cropW, cropH);
            }
            return Bitmap.createScaledBitmap(bmp, targetW, targetH, true);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to scale surface image", t);
            return bmp;
        }
    }

    private static String formatElapsed(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
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

    /// Resolves `${key}` placeholders from the state map. Unknown keys resolve to an empty
    /// string so stale layouts degrade gracefully.
    static String interpolate(String text, JSONObject state) {
        if (text == null || text.indexOf("${") < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        int n = text.length();
        while (i < n) {
            int start = text.indexOf("${", i);
            if (start < 0) {
                sb.append(text, i, n);
                break;
            }
            int end = text.indexOf('}', start + 2);
            if (end < 0) {
                sb.append(text, i, n);
                break;
            }
            sb.append(text, i, start);
            String key = text.substring(start + 2, end);
            Object v = state == null ? null : state.opt(key);
            if (v != null && v != JSONObject.NULL) {
                sb.append(String.valueOf(v));
            }
            i = end + 1;
        }
        return sb.toString();
    }
}
