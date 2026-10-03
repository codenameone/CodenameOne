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
package com.google.android.material.appbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.R;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;

/// The container of a top app bar: a vertical `LinearLayout` on the bar's
/// surface, elevated in MaterialComponents. Children's scroll flags are
/// kept, but the bar does not collapse with scrolling content.
public class AppBarLayout extends LinearLayout {

    private final MaterialShapeDrawable mSurface;
    private boolean mLiftOnScroll;

    public AppBarLayout(Context context) {
        this(context, null);
    }

    public AppBarLayout(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.appBarLayoutStyle));
    }

    public AppBarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_AppBarLayout
                : R.style.Widget_MaterialComponents_AppBarLayout_Primary;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppBarLayout, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            mSurface = new MaterialShapeDrawable();
            Drawable bg = getBackground();
            if (bg instanceof ColorDrawable) {
                mSurface.setFillColor(ColorStateList.valueOf(((ColorDrawable) bg).getColor()));
                setBackground(mSurface);
            }
            mSurface.setElevation(a.getDimension(R.styleable.AppBarLayout_elevation, 0f));
            mLiftOnScroll = a.getBoolean(R.styleable.AppBarLayout_liftOnScroll, false);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected boolean drawsOutsideBounds() {
        return mSurface != null && getBackground() == mSurface && mSurface.getElevation() > 0;
    }

    public void setExpanded(boolean expanded) {
    }

    public void setExpanded(boolean expanded, boolean animate) {
    }

    public boolean isLiftOnScroll() {
        return mLiftOnScroll;
    }

    public void setLiftOnScroll(boolean liftOnScroll) {
        mLiftOnScroll = liftOnScroll;
    }

    public int getTotalScrollRange() {
        return 0;
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LinearLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /// A child's layout parameters, with its scroll flags.
    public static class LayoutParams extends LinearLayout.LayoutParams {
        public static final int SCROLL_FLAG_NO_SCROLL = 0x0;
        public static final int SCROLL_FLAG_SCROLL = 0x1;
        public static final int SCROLL_FLAG_EXIT_UNTIL_COLLAPSED = 0x2;
        public static final int SCROLL_FLAG_ENTER_ALWAYS = 0x4;
        public static final int SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED = 0x8;
        public static final int SCROLL_FLAG_SNAP = 0x10;

        private int scrollFlags = SCROLL_FLAG_SCROLL;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.AppBarLayout_Layout);
            scrollFlags = a.getInt(R.styleable.AppBarLayout_Layout_layout_scrollFlags, SCROLL_FLAG_SCROLL);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public int getScrollFlags() {
            return scrollFlags;
        }

        public void setScrollFlags(int flags) {
            scrollFlags = flags;
        }
    }
}
