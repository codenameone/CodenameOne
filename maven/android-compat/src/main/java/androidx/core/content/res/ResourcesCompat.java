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
package androidx.core.content.res;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;

/// Static helpers over `Resources`.
public final class ResourcesCompat {

    public static final int ID_NULL = 0;

    private ResourcesCompat() {
    }

    /// Answers the outcome of an asynchronous font load.
    public abstract static class FontCallback {
        public abstract void onFontRetrieved(Typeface typeface);

        public abstract void onFontRetrievalFailed(int reason);
    }

    public static Drawable getDrawable(Resources res, int id, Resources.Theme theme) {
        return res.getDrawable(id, theme);
    }

    public static Drawable getDrawableForDensity(Resources res, int id, int density, Resources.Theme theme) {
        return res.getDrawable(id, theme);
    }

    public static int getColor(Resources res, int id, Resources.Theme theme) {
        return res.getColor(id, theme);
    }

    public static ColorStateList getColorStateList(Resources res, int id, Resources.Theme theme) {
        return res.getColorStateList(id, theme);
    }

    public static float getFloat(Resources res, int id) {
        return res.getFloat(id);
    }

    public static Typeface getFont(android.content.Context context, int id) {
        return context.getResources().getFont(id);
    }

    /// Fonts are bundled, so the font is ready at once; the callback runs
    /// before this returns, on the calling thread when `handler` is null.
    public static void getFont(android.content.Context context, int id, final FontCallback callback, Handler handler) {
        final Typeface tf;
        try {
            tf = context.getResources().getFont(id);
        } catch (Resources.NotFoundException e) {
            callback.onFontRetrievalFailed(-3);
            return;
        }
        callback.onFontRetrieved(tf);
    }
}
