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

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Property;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/// Reads and writes an animated property by name, for `ObjectAnimator`.
/// Android finds a `setFoo`/`getFoo` pair by reflection; Codename One has
/// none, so the setters the framework classes expose are listed here, and
/// any other name is reported once and left alone. Application classes are
/// animated through an `android.util.Property` instead.
public final class AnimatableProperties {

    private AnimatableProperties() {
    }

    /// Whether `name` can be set on `target`.
    public static boolean supports(Object target, String name) {
        return get(target, name) != null || isWriteOnly(target, name);
    }

    private static boolean isWriteOnly(Object target, String name) {
        return target instanceof GradientDrawable && name.equals("color");
    }

    /// The current value, an Integer or a Float, or null when unknown.
    public static Object get(Object target, String name) {
        if (target instanceof View) {
            View v = (View) target;
            Property<View, Float> p = View.propertyNamed(name);
            if (p != null) {
                return p.get(v);
            }
            if (name.equals("elevation")) {
                return Float.valueOf(v.getElevation());
            }
            if (name.equals("pivotX")) {
                return Float.valueOf(v.getPivotX());
            }
            if (name.equals("pivotY")) {
                return Float.valueOf(v.getPivotY());
            }
            if (name.equals("scrollX")) {
                return Integer.valueOf(v.getScrollX());
            }
            if (name.equals("scrollY")) {
                return Integer.valueOf(v.getScrollY());
            }
            if (name.equals("left")) {
                return Integer.valueOf(v.getLeft());
            }
            if (name.equals("top")) {
                return Integer.valueOf(v.getTop());
            }
            if (name.equals("right")) {
                return Integer.valueOf(v.getRight());
            }
            if (name.equals("bottom")) {
                return Integer.valueOf(v.getBottom());
            }
            if (name.equals("backgroundColor")) {
                Drawable bg = v.getBackground();
                return Integer.valueOf(bg instanceof ColorDrawable ? ((ColorDrawable) bg).getColor() : 0);
            }
            if (name.equals("textColor") && target instanceof TextView) {
                return Integer.valueOf(((TextView) target).getCurrentTextColor());
            }
            if (name.equals("imageAlpha") && target instanceof ImageView) {
                return Integer.valueOf(((ImageView) target).getImageAlpha());
            }
            if (target instanceof ProgressBar) {
                ProgressBar pb = (ProgressBar) target;
                if (name.equals("progress")) {
                    return Integer.valueOf(pb.getProgress());
                }
                if (name.equals("secondaryProgress")) {
                    return Integer.valueOf(pb.getSecondaryProgress());
                }
            }
            return null;
        }
        if (target instanceof Drawable) {
            Drawable d = (Drawable) target;
            if (name.equals("alpha")) {
                return Integer.valueOf(d.getAlpha());
            }
            if (name.equals("level")) {
                return Integer.valueOf(d.getLevel());
            }
            if (name.equals("color") && target instanceof ColorDrawable) {
                return Integer.valueOf(((ColorDrawable) target).getColor());
            }
        }
        return null;
    }

    /// Sets `name` on `target` to `value` (a Number); answers whether the
    /// property exists.
    public static boolean set(Object target, String name, Object value) {
        if (!(value instanceof Number)) {
            return false;
        }
        Number n = (Number) value;
        if (target instanceof View) {
            View v = (View) target;
            Property<View, Float> p = View.propertyNamed(name);
            if (p != null) {
                p.set(v, Float.valueOf(n.floatValue()));
                return true;
            }
            if (name.equals("elevation")) {
                v.setElevation(n.floatValue());
            } else if (name.equals("pivotX")) {
                v.setPivotX(n.floatValue());
            } else if (name.equals("pivotY")) {
                v.setPivotY(n.floatValue());
            } else if (name.equals("scrollX")) {
                v.setScrollX(n.intValue());
            } else if (name.equals("scrollY")) {
                v.setScrollY(n.intValue());
            } else if (name.equals("left")) {
                v.setLeft(n.intValue());
            } else if (name.equals("top")) {
                v.setTop(n.intValue());
            } else if (name.equals("right")) {
                v.setRight(n.intValue());
            } else if (name.equals("bottom")) {
                v.setBottom(n.intValue());
            } else if (name.equals("backgroundColor")) {
                v.setBackgroundColor(n.intValue());
            } else if (name.equals("textColor") && target instanceof TextView) {
                ((TextView) target).setTextColor(n.intValue());
            } else if (name.equals("imageAlpha") && target instanceof ImageView) {
                ((ImageView) target).setImageAlpha(n.intValue());
            } else if (name.equals("progress") && target instanceof ProgressBar) {
                ((ProgressBar) target).setProgress(n.intValue());
            } else if (name.equals("secondaryProgress") && target instanceof ProgressBar) {
                ((ProgressBar) target).setSecondaryProgress(n.intValue());
            } else {
                return unsupported(target, name);
            }
            return true;
        }
        if (target instanceof Drawable) {
            Drawable d = (Drawable) target;
            if (name.equals("alpha")) {
                d.setAlpha(n.intValue());
            } else if (name.equals("level")) {
                d.setLevel(n.intValue());
            } else if (name.equals("color") && target instanceof ColorDrawable) {
                ((ColorDrawable) target).setColor(n.intValue());
            } else if (name.equals("color") && target instanceof GradientDrawable) {
                ((GradientDrawable) target).setColor(n.intValue());
            } else {
                return unsupported(target, name);
            }
            d.invalidateSelf();
            return true;
        }
        return unsupported(target, name);
    }

    private static boolean unsupported(Object target, String name) {
        CompatReport.unsupported("ObjectAnimator",
                (target == null ? "null" : target.getClass().getName()) + "." + name
                        + " (animate it through an android.util.Property)");
        return false;
    }
}
