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
package com.codename1.androidcompat.runtime;

import android.R;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.TypedValue;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Builds drawables and color state lists from the resource table: image
/// files scaled from their density bucket to the device's, and compiled
/// drawable XML (`<selector>`, `<shape>`, `<layer-list>`, `<inset>`,
/// `<ripple>`, `<vector>`, ...) with theme attributes resolved against the
/// theme the drawable is loaded for.
public final class DrawableInflater {

    private static final int[] STATE_ATTRS = {
        R.attr.state_pressed, R.attr.state_focused, R.attr.state_selected, R.attr.state_checked,
        R.attr.state_checkable, R.attr.state_enabled, R.attr.state_activated, R.attr.state_window_focused,
        R.attr.state_hovered, R.attr.state_first, R.attr.state_last, R.attr.state_middle, R.attr.state_single
    };

    private static final Map<String, Image> RAW_IMAGES = new HashMap<String, Image>();
    private static final Map<String, Image> SCALED_IMAGES = new HashMap<String, Image>();

    private DrawableInflater() {
    }

    // ------------------------------------------------------------ entry points

    public static Drawable load(Resources res, int id, Resources.Theme theme) {
        Object it = res.item(id);
        if (it instanceof ResValue) {
            ResValue v = (ResValue) it;
            TypedValue tv = new TypedValue();
            res.resolve(v, tv, theme(theme), true);
            if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return new ColorDrawable(tv.data);
            }
            if (tv.resourceId != 0 && tv.resourceId != id) {
                return load(res, tv.resourceId, theme);
            }
            if (v.type == TypedValue.TYPE_REFERENCE && v.data != 0 && v.data != id) {
                return load(res, v.data, theme);
            }
            return new ColorDrawable(0);
        }
        if (it instanceof ResTable.FileRef) {
            return fromFile(res, id, (ResTable.FileRef) it);
        }
        if (it instanceof XmlNode) {
            return inflate(res, (XmlNode) it, theme);
        }
        throw new Resources.NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a drawable");
    }

    /// The density bucket the chosen variant of `id` came from: 160 for an
    /// unqualified directory, 0xffff for nodpi.
    public static int densityOf(Resources res, int id) {
        ResTable.Entry e = res.getManager().entry(id);
        if (e == null) {
            return 160;
        }
        ResConfigSpec c = e.bestConfig(res.getManager().device());
        if (c == null || c.getDensity() == 0) {
            return 160;
        }
        return c.getDensity();
    }

    private static Resources.Theme theme(Resources.Theme t) {
        if (t != null) {
            return t;
        }
        AndroidRuntime rt = AndroidRuntime.getInstance();
        return rt == null ? null : rt.currentTheme();
    }

    // ------------------------------------------------------------ files

    public static Image rawImage(String flatName) {
        Image img = RAW_IMAGES.get(flatName);
        if (img != null) {
            return img;
        }
        InputStream in = Display.getInstance().getResourceAsStream(DrawableInflater.class, "/" + flatName);
        if (in == null) {
            throw new Resources.NotFoundException("Image " + flatName + " is missing from the application");
        }
        try {
            try {
                img = Image.createImage(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new Resources.NotFoundException("Cannot decode " + flatName + ": " + e.getMessage());
        }
        RAW_IMAGES.put(flatName, img);
        return img;
    }

    private static Drawable fromFile(Resources res, int id, ResTable.FileRef f) {
        return fromFile(res, f, densityOf(res, id), res.getDisplayMetrics().densityDpi);
    }

    /// `getDrawableForDensity`: an image file chosen and scaled for `density`
    /// instead of the device's, so its bitmap has the pixel size a screen of
    /// that density would get. Drawable XML (vectors, shapes) is sized in dp
    /// against the device and loads as [#load(Resources, int, Resources.Theme)]
    /// does.
    public static Drawable loadForDensity(Resources res, int id, int density, Resources.Theme theme) {
        ResTable.Entry e = density <= 0 ? null : res.getManager().entry(id);
        if (e == null) {
            return load(res, id, theme);
        }
        DeviceConfig d = res.getManager().device().withDensity(density);
        Object it = e.bestUncached(d);
        if (!(it instanceof ResTable.FileRef)) {
            return load(res, id, theme);
        }
        ResConfigSpec c = e.bestConfigUncached(d);
        int bucket = c == null || c.getDensity() == 0 ? 160 : c.getDensity();
        return fromFile(res, (ResTable.FileRef) it, bucket, density);
    }

    private static Drawable fromFile(Resources res, ResTable.FileRef f, int bucket, int device) {
        float scale = bucket == 0xffff || bucket == 0xfffe || bucket <= 0 ? 1f : device / (float) bucket;
        if (f.name.endsWith(".9.png")) {
            return new NinePatchDrawable(rawImage(f.name), scale);
        }
        String key = f.name + "@" + device;
        Image img = SCALED_IMAGES.get(key);
        if (img == null) {
            Image raw = rawImage(f.name);
            img = raw;
            if (scale != 1f) {
                int w = Math.max(1, Math.round(raw.getWidth() * scale));
                int h = Math.max(1, Math.round(raw.getHeight() * scale));
                img = raw.scaled(w, h);
            }
            SCALED_IMAGES.put(key, img);
        }
        return new BitmapDrawable(res, Bitmap.wrap(img));
    }

    // ------------------------------------------------------------ xml

    private static TypedArray attrs(Resources res, XmlNode node, Resources.Theme theme, int[] styleable) {
        CompiledAttributeSet set = new CompiledAttributeSet(node);
        Resources.Theme t = theme(theme);
        return t != null ? t.obtainStyledAttributes(set, styleable, 0, 0) : res.obtainAttributes(set, styleable);
    }

    public static Drawable inflate(Resources res, XmlNode node, Resources.Theme theme) {
        String tag = node.tag;
        if (tag.equals("selector") || tag.equals("animated-selector")) {
            return selector(res, node, theme);
        }
        if (tag.equals("shape")) {
            return shape(res, node, theme);
        }
        if (tag.equals("layer-list") || tag.equals("transition")) {
            LayerDrawable l = new LayerDrawable(new Drawable[0]);
            layers(res, node, theme, l, false);
            return l;
        }
        if (tag.equals("ripple")) {
            return ripple(res, node, theme);
        }
        if (tag.equals("inset")) {
            return inset(res, node, theme);
        }
        if (tag.equals("color")) {
            TypedArray a = attrs(res, node, theme, R.styleable.ColorDrawable);
            int c = a.getColor(R.styleable.ColorDrawable_color, 0);
            return new ColorDrawable(c);
        }
        if (tag.equals("bitmap") || tag.equals("nine-patch")) {
            return bitmap(res, node, theme);
        }
        if (tag.equals("vector")) {
            return vector(res, node, theme);
        }
        if (tag.equals("animated-vector") || tag.equals("rotate") || tag.equals("scale") || tag.equals("clip")
                || tag.equals("animated-rotate")) {
            // Static rendering of animated and level-driven wrappers: the
            // wrapped drawable as it is at rest.
            Drawable child = childDrawable(res, node, theme, R.attr.drawable);
            return child == null ? new ColorDrawable(0) : child;
        }
        if (tag.equals("level-list") || tag.equals("animation-list")) {
            for (XmlNode item : node.children) {
                if (item.tag.equals("item")) {
                    Drawable d = childDrawable(res, item, theme, R.attr.drawable);
                    if (d != null) {
                        return d;
                    }
                }
            }
            return new ColorDrawable(0);
        }
        if (tag.equals("adaptive-icon")) {
            LayerDrawable l = new LayerDrawable(new Drawable[0]);
            for (XmlNode c : node.children) {
                if (c.tag.equals("background") || c.tag.equals("foreground") || c.tag.equals("monochrome")) {
                    if (c.tag.equals("monochrome")) {
                        continue;
                    }
                    Drawable d = childDrawable(res, c, theme, R.attr.drawable);
                    if (d != null) {
                        l.addLayer(d);
                    }
                }
            }
            return l;
        }
        CompatReport.unsupported("drawable", "<" + tag + "> in " + node.getSource());
        return new ColorDrawable(0);
    }

    /// The drawable named by attr `attr` on `node`, else its first child element.
    private static Drawable childDrawable(Resources res, XmlNode node, Resources.Theme theme, int attr) {
        ResValue v = node.valueForAttr(attr);
        if (v != null) {
            TypedValue tv = new TypedValue();
            res.resolve(v, tv, theme(theme), true);
            if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return new ColorDrawable(tv.data);
            }
            int id = tv.resourceId != 0 ? tv.resourceId : (tv.type == TypedValue.TYPE_REFERENCE ? tv.data : 0);
            if (id != 0) {
                return load(res, id, theme);
            }
        }
        for (XmlNode c : node.children) {
            return inflate(res, c, theme);
        }
        return null;
    }

    private static int[] states(XmlNode item) {
        List<Integer> out = new ArrayList<Integer>();
        for (int i = 0; i < item.attrIds.length; i++) {
            int id = item.attrIds[i];
            for (int s : STATE_ATTRS) {
                if (s == id) {
                    boolean on = item.values[i].data != 0;
                    out.add(on ? id : -id);
                }
            }
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = out.get(i);
        }
        return r;
    }

    private static Drawable selector(Resources res, XmlNode node, Resources.Theme theme) {
        StateListDrawable s = new StateListDrawable();
        TypedArray a = attrs(res, node, theme, R.styleable.StateListDrawable);
        s.setVariablePadding(a.getBoolean(R.styleable.StateListDrawable_variablePadding, false));
        s.setConstantSize(a.getBoolean(R.styleable.StateListDrawable_constantSize, false));
        for (XmlNode item : node.children) {
            if (!item.tag.equals("item")) {
                continue;
            }
            Drawable d = childDrawable(res, item, theme, R.attr.drawable);
            if (d != null) {
                s.addState(states(item), d);
            }
        }
        return s;
    }

    private static Drawable shape(Resources res, XmlNode node, Resources.Theme theme) {
        GradientDrawable g = new GradientDrawable();
        TypedArray a = attrs(res, node, theme, R.styleable.GradientDrawable);
        g.setShape(a.getInt(R.styleable.GradientDrawable_shape, GradientDrawable.RECTANGLE));
        if (a.hasValue(R.styleable.GradientDrawable_innerRadius)) {
            g.setInnerRadius(a.getDimensionPixelSize(R.styleable.GradientDrawable_innerRadius, 0));
        }
        if (a.hasValue(R.styleable.GradientDrawable_thickness)) {
            g.setThickness(a.getDimensionPixelSize(R.styleable.GradientDrawable_thickness, 0));
        }
        g.setInnerRadiusRatio(a.getFloat(R.styleable.GradientDrawable_innerRadiusRatio, 3f));
        g.setThicknessRatio(a.getFloat(R.styleable.GradientDrawable_thicknessRatio, 9f));
        ColorStateList tint = a.getColorStateList(R.styleable.GradientDrawable_tint);
        if (tint != null) {
            g.setTintList(tint);
        }
        for (XmlNode c : node.children) {
            if (c.tag.equals("solid")) {
                TypedArray s = attrs(res, c, theme, R.styleable.GradientDrawableSolid);
                ColorStateList csl = s.getColorStateList(R.styleable.GradientDrawableSolid_color);
                if (csl != null) {
                    g.setColor(csl);
                }
            } else if (c.tag.equals("stroke")) {
                TypedArray s = attrs(res, c, theme, R.styleable.GradientDrawableStroke);
                int w = s.getDimensionPixelSize(R.styleable.GradientDrawableStroke_width, 0);
                ColorStateList csl = s.getColorStateList(R.styleable.GradientDrawableStroke_color);
                g.setStroke(w, csl == null ? ColorStateList.valueOf(0) : csl,
                        s.getDimension(R.styleable.GradientDrawableStroke_dashWidth, 0),
                        s.getDimension(R.styleable.GradientDrawableStroke_dashGap, 0));
            } else if (c.tag.equals("corners")) {
                TypedArray s = attrs(res, c, theme, R.styleable.DrawableCorners);
                float r = s.getDimension(R.styleable.DrawableCorners_radius, 0);
                float tl = s.getDimension(R.styleable.DrawableCorners_topLeftRadius, r);
                float tr = s.getDimension(R.styleable.DrawableCorners_topRightRadius, r);
                float br = s.getDimension(R.styleable.DrawableCorners_bottomRightRadius, r);
                float bl = s.getDimension(R.styleable.DrawableCorners_bottomLeftRadius, r);
                if (tl == r && tr == r && br == r && bl == r) {
                    g.setCornerRadius(r);
                } else {
                    g.setCornerRadii(new float[] {tl, tl, tr, tr, br, br, bl, bl});
                }
            } else if (c.tag.equals("gradient")) {
                TypedArray s = attrs(res, c, theme, R.styleable.GradientDrawableGradient);
                int start = s.getColor(R.styleable.GradientDrawableGradient_startColor, 0);
                int end = s.getColor(R.styleable.GradientDrawableGradient_endColor, 0);
                boolean hasCenter = s.hasValue(R.styleable.GradientDrawableGradient_centerColor);
                int center = s.getColor(R.styleable.GradientDrawableGradient_centerColor, 0);
                g.setColors(hasCenter ? new int[] {start, center, end} : new int[] {start, end});
                g.setGradientType(s.getInt(R.styleable.GradientDrawableGradient_type, GradientDrawable.LINEAR_GRADIENT));
                int angle = ((int) s.getFloat(R.styleable.GradientDrawableGradient_angle, 0) % 360 + 360) % 360;
                GradientDrawable.Orientation o;
                switch (angle / 45) {
                    case 0:
                        o = GradientDrawable.Orientation.LEFT_RIGHT;
                        break;
                    case 1:
                        o = GradientDrawable.Orientation.BL_TR;
                        break;
                    case 2:
                        o = GradientDrawable.Orientation.BOTTOM_TOP;
                        break;
                    case 3:
                        o = GradientDrawable.Orientation.BR_TL;
                        break;
                    case 4:
                        o = GradientDrawable.Orientation.RIGHT_LEFT;
                        break;
                    case 5:
                        o = GradientDrawable.Orientation.TR_BL;
                        break;
                    case 6:
                        o = GradientDrawable.Orientation.TOP_BOTTOM;
                        break;
                    default:
                        o = GradientDrawable.Orientation.TL_BR;
                        break;
                }
                g.setOrientation(o);
            } else if (c.tag.equals("padding")) {
                TypedArray s = attrs(res, c, theme, R.styleable.GradientDrawablePadding);
                g.setPadding(s.getDimensionPixelOffset(R.styleable.GradientDrawablePadding_left, 0),
                        s.getDimensionPixelOffset(R.styleable.GradientDrawablePadding_top, 0),
                        s.getDimensionPixelOffset(R.styleable.GradientDrawablePadding_right, 0),
                        s.getDimensionPixelOffset(R.styleable.GradientDrawablePadding_bottom, 0));
            } else if (c.tag.equals("size")) {
                TypedArray s = attrs(res, c, theme, R.styleable.GradientDrawableSize);
                g.setSize(s.getDimensionPixelSize(R.styleable.GradientDrawableSize_width, -1),
                        s.getDimensionPixelSize(R.styleable.GradientDrawableSize_height, -1));
            }
        }
        return g;
    }

    private static void layers(Resources res, XmlNode node, Resources.Theme theme, LayerDrawable l, boolean ripple) {
        for (XmlNode item : node.children) {
            if (!item.tag.equals("item")) {
                continue;
            }
            Drawable d = childDrawable(res, item, theme, R.attr.drawable);
            if (d == null) {
                continue;
            }
            TypedArray a = attrs(res, item, theme, R.styleable.LayerDrawableItem);
            int idx = l.addLayer(d);
            int id = a.getResourceId(R.styleable.LayerDrawableItem_id, -1);
            if (id != -1) {
                l.setId(idx, id);
            }
            int left = a.getDimensionPixelOffset(R.styleable.LayerDrawableItem_left, 0);
            int right = a.getDimensionPixelOffset(R.styleable.LayerDrawableItem_right, 0);
            left = a.getDimensionPixelOffset(R.styleable.LayerDrawableItem_start, left);
            right = a.getDimensionPixelOffset(R.styleable.LayerDrawableItem_end, right);
            l.setLayerInset(idx, left, a.getDimensionPixelOffset(R.styleable.LayerDrawableItem_top, 0), right,
                    a.getDimensionPixelOffset(R.styleable.LayerDrawableItem_bottom, 0));
            l.setLayerSize(idx, a.getDimensionPixelSize(R.styleable.LayerDrawableItem_width, -1),
                    a.getDimensionPixelSize(R.styleable.LayerDrawableItem_height, -1));
            l.setLayerGravity(idx, a.getInt(R.styleable.LayerDrawableItem_gravity, 0));
        }
    }

    private static Drawable ripple(Resources res, XmlNode node, Resources.Theme theme) {
        TypedArray a = attrs(res, node, theme, R.styleable.RippleDrawable);
        ColorStateList color = a.getColorStateList(R.styleable.RippleDrawable_color);
        RippleDrawable r = new RippleDrawable(color == null ? ColorStateList.valueOf(0x1f000000) : color, null, null);
        r.setRadius(a.getDimensionPixelSize(R.styleable.RippleDrawable_radius, RippleDrawable.RADIUS_AUTO));
        layers(res, node, theme, r, true);
        return r;
    }

    private static Drawable inset(Resources res, XmlNode node, Resources.Theme theme) {
        TypedArray a = attrs(res, node, theme, R.styleable.InsetDrawable);
        Drawable child = childDrawable(res, node, theme, R.attr.drawable);
        int all = a.getDimensionPixelOffset(R.styleable.InsetDrawable_inset, 0);
        float allF = a.getFraction(R.styleable.InsetDrawable_inset, 1, 1, -1);
        int[] idx = {R.styleable.InsetDrawable_insetLeft, R.styleable.InsetDrawable_insetTop,
                R.styleable.InsetDrawable_insetRight, R.styleable.InsetDrawable_insetBottom};
        int[] px = new int[4];
        float[] fr = new float[4];
        boolean anyFraction = allF >= 0;
        for (int i = 0; i < 4; i++) {
            px[i] = a.getDimensionPixelOffset(idx[i], all);
            fr[i] = a.getFraction(idx[i], 1, 1, allF);
            if (fr[i] >= 0) {
                anyFraction = true;
            }
        }
        InsetDrawable d = new InsetDrawable(child, px[0], px[1], px[2], px[3]);
        if (anyFraction) {
            d.setFractionalInsets(fr[0], fr[1], fr[2], fr[3]);
        }
        return d;
    }

    private static Drawable bitmap(Resources res, XmlNode node, Resources.Theme theme) {
        TypedArray a = attrs(res, node, theme, R.styleable.BitmapDrawable);
        int src = a.getResourceId(R.styleable.BitmapDrawable_src, 0);
        if (src == 0) {
            return new ColorDrawable(0);
        }
        Drawable d = load(res, src, theme);
        if (d instanceof BitmapDrawable) {
            BitmapDrawable b = (BitmapDrawable) d;
            b.setGravity(a.getInt(R.styleable.BitmapDrawable_gravity, android.view.Gravity.FILL));
            int tile = a.getInt(R.styleable.BitmapDrawable_tileMode, -1);
            if (tile >= 0) {
                Shader.TileMode m = tile == 1 ? Shader.TileMode.REPEAT : tile == 2 ? Shader.TileMode.MIRROR
                        : Shader.TileMode.CLAMP;
                b.setTileModeXY(m, m);
            }
        }
        ColorStateList tint = a.getColorStateList(R.styleable.BitmapDrawable_tint);
        if (tint != null) {
            d.setTintList(tint);
        }
        return d;
    }

    private static Drawable vector(Resources res, XmlNode node, Resources.Theme theme) {
        TypedArray a = attrs(res, node, theme, R.styleable.VectorDrawable);
        VectorDrawable v = new VectorDrawable();
        int w = a.getDimensionPixelSize(R.styleable.VectorDrawable_width, 0);
        int h = a.getDimensionPixelSize(R.styleable.VectorDrawable_height, 0);
        v.setIntrinsicSize(w, h);
        v.setViewport(a.getFloat(R.styleable.VectorDrawable_viewportWidth, w),
                a.getFloat(R.styleable.VectorDrawable_viewportHeight, h));
        v.setAlpha(Math.round(a.getFloat(R.styleable.VectorDrawable_alpha, 1f) * 255));
        v.setAutoMirrored(a.getBoolean(R.styleable.VectorDrawable_autoMirrored, false));
        ColorStateList tint = a.getColorStateList(R.styleable.VectorDrawable_tint);
        if (tint != null) {
            v.setTintList(tint);
            v.setTintMode(PorterDuff.intToMode(a.getInt(R.styleable.VectorDrawable_tintMode, 5)));
        }
        vectorChildren(res, node, theme, v.getRoot());
        return v;
    }

    private static void vectorChildren(Resources res, XmlNode node, Resources.Theme theme, VectorDrawable.Group g) {
        for (XmlNode c : node.children) {
            if (c.tag.equals("group")) {
                TypedArray a = attrs(res, c, theme, R.styleable.VectorDrawableGroup);
                VectorDrawable.Group sub = new VectorDrawable.Group();
                sub.rotation = a.getFloat(R.styleable.VectorDrawableGroup_rotation, 0);
                sub.pivotX = a.getFloat(R.styleable.VectorDrawableGroup_pivotX, 0);
                sub.pivotY = a.getFloat(R.styleable.VectorDrawableGroup_pivotY, 0);
                sub.scaleX = a.getFloat(R.styleable.VectorDrawableGroup_scaleX, 1);
                sub.scaleY = a.getFloat(R.styleable.VectorDrawableGroup_scaleY, 1);
                sub.translateX = a.getFloat(R.styleable.VectorDrawableGroup_translateX, 0);
                sub.translateY = a.getFloat(R.styleable.VectorDrawableGroup_translateY, 0);
                vectorChildren(res, c, theme, sub);
                g.children.add(sub);
            } else if (c.tag.equals("path")) {
                TypedArray a = attrs(res, c, theme, R.styleable.VectorDrawablePath);
                VectorDrawable.VPath p = new VectorDrawable.VPath();
                p.path = PathParser.createPathFromPathData(a.getString(R.styleable.VectorDrawablePath_pathData));
                p.fill = a.getColorStateList(R.styleable.VectorDrawablePath_fillColor);
                p.fillAlpha = a.getFloat(R.styleable.VectorDrawablePath_fillAlpha, 1);
                p.stroke = a.getColorStateList(R.styleable.VectorDrawablePath_strokeColor);
                p.strokeWidth = a.getFloat(R.styleable.VectorDrawablePath_strokeWidth, 0);
                p.strokeAlpha = a.getFloat(R.styleable.VectorDrawablePath_strokeAlpha, 1);
                p.miter = a.getFloat(R.styleable.VectorDrawablePath_strokeMiterLimit, 4);
                int cap = a.getInt(R.styleable.VectorDrawablePath_strokeLineCap, 0);
                p.cap = cap == 1 ? android.graphics.Paint.Cap.ROUND : cap == 2 ? android.graphics.Paint.Cap.SQUARE
                        : android.graphics.Paint.Cap.BUTT;
                int join = a.getInt(R.styleable.VectorDrawablePath_strokeLineJoin, 0);
                p.join = join == 1 ? android.graphics.Paint.Join.ROUND : join == 2 ? android.graphics.Paint.Join.BEVEL
                        : android.graphics.Paint.Join.MITER;
                p.evenOdd = a.getInt(R.styleable.VectorDrawablePath_fillType, 0) == 1;
                if (p.fill == null) {
                    // An inline <aapt:attr name="android:fillColor"><gradient>
                    // fills with the gradient's first color.
                    for (XmlNode inner : c.children) {
                        for (XmlNode grad : inner.children) {
                            if (grad.tag.equals("gradient")) {
                                TypedArray ga = attrs(res, grad, theme, R.styleable.GradientDrawableGradient);
                                p.fill = ColorStateList.valueOf(ga.getColor(R.styleable.GradientDrawableGradient_startColor, 0));
                            }
                        }
                    }
                }
                g.children.add(p);
            } else if (c.tag.equals("clip-path")) {
                TypedArray a = attrs(res, c, theme, R.styleable.VectorDrawableClipPath);
                VectorDrawable.ClipPath cp = new VectorDrawable.ClipPath();
                cp.path = PathParser.createPathFromPathData(a.getString(R.styleable.VectorDrawableClipPath_pathData));
                g.children.add(cp);
            }
        }
    }

    // ------------------------------------------------------------ colors

    public static ColorStateList inflateColorStateList(Resources res, XmlNode node, Resources.Theme theme) {
        if (!node.tag.equals("selector")) {
            return ColorStateList.valueOf(0xff000000);
        }
        List<int[]> specs = new ArrayList<int[]>();
        List<Integer> colors = new ArrayList<Integer>();
        for (XmlNode item : node.children) {
            if (!item.tag.equals("item")) {
                continue;
            }
            TypedArray a = attrs(res, item, theme, R.styleable.ColorStateListItem);
            int color = a.getColor(R.styleable.ColorStateListItem_color, 0xffff00ff);
            float alpha = a.getFloat(R.styleable.ColorStateListItem_alpha, 1f);
            if (alpha != 1f) {
                int ca = Math.round((color >>> 24) * alpha);
                color = (color & 0xffffff) | (Math.max(0, Math.min(255, ca)) << 24);
            }
            specs.add(states(item));
            colors.add(color);
        }
        int[][] s = specs.toArray(new int[specs.size()][]);
        int[] c = new int[colors.size()];
        for (int i = 0; i < c.length; i++) {
            c[i] = colors.get(i);
        }
        return new ColorStateList(s, c);
    }
}
