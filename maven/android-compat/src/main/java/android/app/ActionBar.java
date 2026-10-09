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
package android.app;

import android.graphics.drawable.Drawable;
import android.view.View;

/// The activity's action bar, shown as the Codename One form's toolbar.
public class ActionBar {

    public static final int DISPLAY_USE_LOGO = 0x1;
    public static final int DISPLAY_SHOW_HOME = 0x2;
    public static final int DISPLAY_HOME_AS_UP = 0x4;
    public static final int DISPLAY_SHOW_TITLE = 0x8;
    public static final int DISPLAY_SHOW_CUSTOM = 0x10;
    public static final int NAVIGATION_MODE_STANDARD = 0;

    public static class LayoutParams extends android.view.ViewGroup.MarginLayoutParams {
        public int gravity;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(int gravity) {
            this(WRAP_CONTENT, MATCH_PARENT, gravity);
        }
    }

    private final ActivityThread.Record record;
    private CharSequence title;
    private CharSequence subtitle;
    private int options = DISPLAY_SHOW_TITLE | DISPLAY_SHOW_HOME;
    private boolean showing = true;
    private Drawable background;
    private View customView;

    ActionBar(ActivityThread.Record record) {
        this.record = record;
    }

    private void update() {
        ActivityThread.updateToolbar(record);
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        update();
    }

    public void setTitle(int resId) {
        setTitle(record.activity.getText(resId));
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setSubtitle(CharSequence subtitle) {
        this.subtitle = subtitle;
        update();
    }

    public void setSubtitle(int resId) {
        setSubtitle(record.activity.getText(resId));
    }

    public CharSequence getSubtitle() {
        return subtitle;
    }

    public void setDisplayOptions(int options) {
        this.options = options;
        update();
    }

    public void setDisplayOptions(int options, int mask) {
        this.options = (this.options & ~mask) | (options & mask);
        update();
    }

    public int getDisplayOptions() {
        return options;
    }

    private void flag(int f, boolean on) {
        setDisplayOptions(on ? f : 0, f);
    }

    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        flag(DISPLAY_HOME_AS_UP, showHomeAsUp);
    }

    public void setDisplayShowHomeEnabled(boolean showHome) {
        flag(DISPLAY_SHOW_HOME, showHome);
    }

    public void setDisplayShowTitleEnabled(boolean showTitle) {
        flag(DISPLAY_SHOW_TITLE, showTitle);
    }

    public void setDisplayShowCustomEnabled(boolean showCustom) {
        flag(DISPLAY_SHOW_CUSTOM, showCustom);
    }

    public void setDisplayUseLogoEnabled(boolean useLogo) {
        flag(DISPLAY_USE_LOGO, useLogo);
    }

    public void setHomeButtonEnabled(boolean enabled) {
    }

    public void setHomeAsUpIndicator(Drawable indicator) {
    }

    public void setHomeAsUpIndicator(int resId) {
    }

    public void setIcon(int resId) {
    }

    public void setIcon(Drawable icon) {
    }

    public void setLogo(int resId) {
    }

    public void setLogo(Drawable logo) {
    }

    public void setBackgroundDrawable(Drawable d) {
        background = d;
        update();
    }

    public Drawable getBackgroundDrawable() {
        return background;
    }

    public void setElevation(float elevation) {
    }

    public float getElevation() {
        return 0;
    }

    public void setCustomView(View view) {
        customView = view;
        update();
    }

    public void setCustomView(View view, LayoutParams layoutParams) {
        setCustomView(view);
    }

    public void setCustomView(int resId) {
        setCustomView(record.activity.getLayoutInflater().inflate(resId, null, false));
    }

    public View getCustomView() {
        return customView;
    }

    public int getHeight() {
        return showing ? record.form.getToolbar() == null ? 0 : record.form.getToolbar().getHeight() : 0;
    }

    public void show() {
        showing = true;
        update();
    }

    public void hide() {
        showing = false;
        update();
    }

    public boolean isShowing() {
        return showing;
    }

    public void setNavigationMode(int mode) {
    }

    public void setHideOnContentScrollEnabled(boolean hideOnContentScroll) {
    }
}
