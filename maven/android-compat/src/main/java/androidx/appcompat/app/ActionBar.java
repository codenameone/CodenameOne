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
package androidx.appcompat.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/// AppCompat's action bar: what `getSupportActionBar()` answers. Backed by a
/// `Toolbar` after `setSupportActionBar`, otherwise by the activity's own
/// action bar.
public abstract class ActionBar {

    public static final int DISPLAY_USE_LOGO = 0x1;
    public static final int DISPLAY_SHOW_HOME = 0x2;
    public static final int DISPLAY_HOME_AS_UP = 0x4;
    public static final int DISPLAY_SHOW_TITLE = 0x8;
    public static final int DISPLAY_SHOW_CUSTOM = 0x10;
    public static final int NAVIGATION_MODE_STANDARD = 0;

    /// Layout parameters of a view in an action bar or a `Toolbar`: margins
    /// and a gravity.
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int gravity = Gravity.NO_GRAVITY;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.FrameLayout_Layout);
            gravity = a.getInt(android.R.styleable.FrameLayout_Layout_layout_gravity, Gravity.NO_GRAVITY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(int gravity) {
            this(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, gravity);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public abstract void setTitle(CharSequence title);

    public abstract void setTitle(int resId);

    public abstract CharSequence getTitle();

    public abstract void setSubtitle(CharSequence subtitle);

    public abstract void setSubtitle(int resId);

    public abstract CharSequence getSubtitle();

    public abstract void setDisplayOptions(int options);

    public abstract void setDisplayOptions(int options, int mask);

    public abstract int getDisplayOptions();

    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        setDisplayOptions(showHomeAsUp ? DISPLAY_HOME_AS_UP : 0, DISPLAY_HOME_AS_UP);
    }

    public void setDisplayShowHomeEnabled(boolean showHome) {
        setDisplayOptions(showHome ? DISPLAY_SHOW_HOME : 0, DISPLAY_SHOW_HOME);
    }

    public void setDisplayShowTitleEnabled(boolean showTitle) {
        setDisplayOptions(showTitle ? DISPLAY_SHOW_TITLE : 0, DISPLAY_SHOW_TITLE);
    }

    public void setDisplayShowCustomEnabled(boolean showCustom) {
        setDisplayOptions(showCustom ? DISPLAY_SHOW_CUSTOM : 0, DISPLAY_SHOW_CUSTOM);
    }

    public void setDisplayUseLogoEnabled(boolean useLogo) {
        setDisplayOptions(useLogo ? DISPLAY_USE_LOGO : 0, DISPLAY_USE_LOGO);
    }

    public void setHomeButtonEnabled(boolean enabled) {
    }

    public abstract void setHomeAsUpIndicator(Drawable indicator);

    public abstract void setHomeAsUpIndicator(int resId);

    public void setHomeActionContentDescription(CharSequence description) {
    }

    public void setHomeActionContentDescription(int resId) {
    }

    public void setIcon(int resId) {
    }

    public void setIcon(Drawable icon) {
    }

    public void setLogo(int resId) {
    }

    public void setLogo(Drawable logo) {
    }

    public abstract void setBackgroundDrawable(Drawable d);

    public void setElevation(float elevation) {
    }

    public float getElevation() {
        return 0;
    }

    public abstract void setCustomView(View view);

    public abstract void setCustomView(View view, LayoutParams layoutParams);

    public abstract void setCustomView(int resId);

    public abstract View getCustomView();

    public abstract int getHeight();

    public abstract void show();

    public abstract void hide();

    public abstract boolean isShowing();

    public Context getThemedContext() {
        return null;
    }

    public void setHideOnContentScrollEnabled(boolean hideOnContentScroll) {
    }

    public int getNavigationMode() {
        return NAVIGATION_MODE_STANDARD;
    }

    public void setNavigationMode(int mode) {
    }
}
