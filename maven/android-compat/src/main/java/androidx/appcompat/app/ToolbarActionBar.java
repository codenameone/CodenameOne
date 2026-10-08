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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.R;
import androidx.appcompat.widget.Toolbar;

/// The support action bar `setSupportActionBar` installs: a `Toolbar` in the
/// activity's layout.
final class ToolbarActionBar extends ActionBar {

    private final Toolbar mToolbar;
    private final AppCompatActivity mActivity;
    private int mDisplayOptions = DISPLAY_SHOW_TITLE | DISPLAY_SHOW_HOME;
    private CharSequence mTitle;
    private Drawable mHomeAsUpIndicator;
    private View mCustomView;
    private final View.OnClickListener mHomeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mActivity.dispatchSupportHome();
        }
    };

    ToolbarActionBar(Toolbar toolbar, AppCompatActivity activity) {
        mToolbar = toolbar;
        mActivity = activity;
        mTitle = toolbar.getTitle();
        if (mTitle == null || mTitle.length() == 0) {
            setTitle(activity.getTitle());
        }
    }

    Toolbar toolbar() {
        return mToolbar;
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        if ((mDisplayOptions & DISPLAY_SHOW_TITLE) != 0) {
            mToolbar.setTitle(title);
        }
    }

    @Override
    public void setTitle(int resId) {
        setTitle(mToolbar.getContext().getText(resId));
    }

    @Override
    public CharSequence getTitle() {
        return mTitle;
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mToolbar.setSubtitle(subtitle);
    }

    @Override
    public void setSubtitle(int resId) {
        mToolbar.setSubtitle(resId);
    }

    @Override
    public CharSequence getSubtitle() {
        return mToolbar.getSubtitle();
    }

    @Override
    public void setDisplayOptions(int options) {
        setDisplayOptions(options, 0xffffffff);
    }

    @Override
    public void setDisplayOptions(int options, int mask) {
        int old = mDisplayOptions;
        mDisplayOptions = (mDisplayOptions & ~mask) | (options & mask);
        int changed = old ^ mDisplayOptions;
        if ((changed & DISPLAY_SHOW_TITLE) != 0) {
            mToolbar.setTitle((mDisplayOptions & DISPLAY_SHOW_TITLE) != 0 ? mTitle : null);
        }
        if ((changed & DISPLAY_HOME_AS_UP) != 0) {
            updateHomeAsUp();
        }
        if ((changed & DISPLAY_SHOW_CUSTOM) != 0 && mCustomView != null) {
            mCustomView.setVisibility((mDisplayOptions & DISPLAY_SHOW_CUSTOM) != 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public int getDisplayOptions() {
        return mDisplayOptions;
    }

    private void updateHomeAsUp() {
        if ((mDisplayOptions & DISPLAY_HOME_AS_UP) != 0) {
            Drawable icon = mHomeAsUpIndicator != null ? mHomeAsUpIndicator : themeHomeAsUpIndicator();
            mToolbar.setNavigationIcon(icon);
            if (!mToolbar.hasNavigationOnClickListener()) {
                mToolbar.setNavigationOnClickListener(mHomeListener);
            }
        } else {
            mToolbar.setNavigationIcon(null);
        }
    }

    private Drawable themeHomeAsUpIndicator() {
        Context c = mToolbar.getContext();
        TypedArray a = c.obtainStyledAttributes(new int[] {R.attr.homeAsUpIndicator});
        Drawable d = a.getDrawable(0);
        a.recycle();
        return d != null ? d : c.getDrawable(R.drawable.abc_ic_ab_back_material);
    }

    @Override
    public void setHomeAsUpIndicator(Drawable indicator) {
        mHomeAsUpIndicator = indicator;
        if ((mDisplayOptions & DISPLAY_HOME_AS_UP) != 0) {
            updateHomeAsUp();
        }
    }

    @Override
    public void setHomeAsUpIndicator(int resId) {
        setHomeAsUpIndicator(resId == 0 ? null : mToolbar.getContext().getDrawable(resId));
    }

    @Override
    public void setHomeActionContentDescription(CharSequence description) {
        mToolbar.setNavigationContentDescription(description);
    }

    @Override
    public void setHomeActionContentDescription(int resId) {
        mToolbar.setNavigationContentDescription(resId);
    }

    @Override
    public void setLogo(int resId) {
        mToolbar.setLogo(resId);
    }

    @Override
    public void setLogo(Drawable logo) {
        mToolbar.setLogo(logo);
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        mToolbar.setBackground(d);
    }

    @Override
    public void setElevation(float elevation) {
        mToolbar.setElevation(elevation);
    }

    @Override
    public float getElevation() {
        return mToolbar.getElevation();
    }

    @Override
    public void setCustomView(View view) {
        setCustomView(view, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START | Gravity.CENTER_VERTICAL));
    }

    @Override
    public void setCustomView(View view, LayoutParams layoutParams) {
        if (mCustomView != null) {
            mToolbar.removeView(mCustomView);
        }
        mCustomView = view;
        if (view != null) {
            mToolbar.addView(view, new Toolbar.LayoutParams(layoutParams));
            view.setVisibility((mDisplayOptions & DISPLAY_SHOW_CUSTOM) != 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setCustomView(int resId) {
        setCustomView(LayoutInflater.from(mToolbar.getContext()).inflate(resId, mToolbar, false));
    }

    @Override
    public View getCustomView() {
        return mCustomView;
    }

    @Override
    public int getHeight() {
        return mToolbar.getHeight();
    }

    @Override
    public void show() {
        mToolbar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hide() {
        mToolbar.setVisibility(View.GONE);
    }

    @Override
    public boolean isShowing() {
        return mToolbar.getVisibility() == View.VISIBLE;
    }

    @Override
    public Context getThemedContext() {
        return mToolbar.getContext();
    }
}
