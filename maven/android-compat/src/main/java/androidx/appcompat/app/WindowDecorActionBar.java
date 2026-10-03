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
import android.graphics.drawable.Drawable;
import android.view.View;

/// The support action bar of an activity whose theme has an action bar: the
/// activity's own (framework) action bar.
final class WindowDecorActionBar extends ActionBar {

    private final android.app.ActionBar mBar;
    private final Context mContext;

    WindowDecorActionBar(android.app.ActionBar bar, Context context) {
        mBar = bar;
        mContext = context;
    }

    @Override
    public void setTitle(CharSequence title) {
        mBar.setTitle(title);
    }

    @Override
    public void setTitle(int resId) {
        mBar.setTitle(resId);
    }

    @Override
    public CharSequence getTitle() {
        return mBar.getTitle();
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mBar.setSubtitle(subtitle);
    }

    @Override
    public void setSubtitle(int resId) {
        mBar.setSubtitle(resId);
    }

    @Override
    public CharSequence getSubtitle() {
        return mBar.getSubtitle();
    }

    @Override
    public void setDisplayOptions(int options) {
        mBar.setDisplayOptions(options);
    }

    @Override
    public void setDisplayOptions(int options, int mask) {
        mBar.setDisplayOptions(options, mask);
    }

    @Override
    public int getDisplayOptions() {
        return mBar.getDisplayOptions();
    }

    @Override
    public void setHomeAsUpIndicator(Drawable indicator) {
        mBar.setHomeAsUpIndicator(indicator);
    }

    @Override
    public void setHomeAsUpIndicator(int resId) {
        mBar.setHomeAsUpIndicator(resId);
    }

    @Override
    public void setIcon(int resId) {
        mBar.setIcon(resId);
    }

    @Override
    public void setIcon(Drawable icon) {
        mBar.setIcon(icon);
    }

    @Override
    public void setLogo(int resId) {
        mBar.setLogo(resId);
    }

    @Override
    public void setLogo(Drawable logo) {
        mBar.setLogo(logo);
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        mBar.setBackgroundDrawable(d);
    }

    @Override
    public void setElevation(float elevation) {
        mBar.setElevation(elevation);
    }

    @Override
    public float getElevation() {
        return mBar.getElevation();
    }

    @Override
    public void setCustomView(View view) {
        mBar.setCustomView(view);
    }

    @Override
    public void setCustomView(View view, LayoutParams layoutParams) {
        mBar.setCustomView(view, new android.app.ActionBar.LayoutParams(layoutParams.width, layoutParams.height,
                layoutParams.gravity));
    }

    @Override
    public void setCustomView(int resId) {
        mBar.setCustomView(resId);
    }

    @Override
    public View getCustomView() {
        return mBar.getCustomView();
    }

    @Override
    public int getHeight() {
        return mBar.getHeight();
    }

    @Override
    public void show() {
        mBar.show();
    }

    @Override
    public void hide() {
        mBar.hide();
    }

    @Override
    public boolean isShowing() {
        return mBar.isShowing();
    }

    @Override
    public Context getThemedContext() {
        return mContext;
    }
}
