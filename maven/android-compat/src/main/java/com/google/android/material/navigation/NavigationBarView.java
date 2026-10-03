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
package com.google.android.material.navigation;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.codename1.androidcompat.runtime.MenuImpl;
import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;

import java.util.ArrayList;
import java.util.List;

/// A bar of top-level destinations from a menu resource: each item an icon
/// and label, the selected one highlighted (in Material 3 with a pill-shaped
/// active indicator behind its icon).
public abstract class NavigationBarView extends FrameLayout {

    public static final int LABEL_VISIBILITY_AUTO = -1;
    public static final int LABEL_VISIBILITY_SELECTED = 0;
    public static final int LABEL_VISIBILITY_LABELED = 1;
    public static final int LABEL_VISIBILITY_UNLABELED = 2;

    /// Called when an item is selected; returning false keeps the
    /// selection where it was.
    public interface OnItemSelectedListener {
        boolean onNavigationItemSelected(MenuItem item);
    }

    /// Called when the already selected item is selected again.
    public interface OnItemReselectedListener {
        void onNavigationItemReselected(MenuItem item);
    }

    private final MenuImpl mMenu;
    private final LinearLayout mItems;
    private final MaterialShapeDrawable mSurface;
    private final boolean mMaterial3;
    private ColorStateList mIconTint;
    private ColorStateList mTextColor;
    private int mActiveIconColor;
    private int mActiveTextColor;
    private int mIndicatorColor;
    private int mIndicatorWidth;
    private int mIndicatorHeight;
    private int mIconSize;
    private int mLabelVisibility;
    private int mSelectedId = View.NO_ID;
    private OnItemSelectedListener mSelectedListener;
    private OnItemReselectedListener mReselectedListener;

    public NavigationBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        mMaterial3 = MaterialAttrs.isMaterial3(context);
        mMenu = new MenuImpl(context);
        mItems = new LinearLayout(context);
        mItems.setOrientation(LinearLayout.HORIZONTAL);
        addView(mItems, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NavigationBarView, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        int menuRes;
        try {
            int surface = MaterialColors.getColor(context, R.attr.colorSurface, 0xffffffff);
            ColorStateList bg = a.getColorStateList(R.styleable.NavigationBarView_backgroundTint);
            mSurface = new MaterialShapeDrawable();
            setBackground(mSurface);
            mSurface.setFillColor(bg != null ? bg : ColorStateList.valueOf(surface));
            mSurface.setElevation(a.getDimension(R.styleable.NavigationBarView_elevation, 0f));
            int inactive = MaterialColors.getColor(context, R.attr.colorOnSurfaceVariant, 0x99000000);
            mIconTint = a.getColorStateList(R.styleable.NavigationBarView_itemIconTint);
            if (mIconTint == null) {
                mIconTint = ColorStateList.valueOf(inactive);
            }
            mTextColor = a.getColorStateList(R.styleable.NavigationBarView_itemTextColor);
            if (mTextColor == null) {
                mTextColor = ColorStateList.valueOf(inactive);
            }
            if (mMaterial3) {
                mActiveIconColor = MaterialColors.getColor(context, R.attr.colorOnSecondaryContainer, 0xff1d192b);
                mActiveTextColor = MaterialColors.getColor(context, R.attr.colorOnSurface, 0xff1d1b20);
            } else {
                int primary = MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6200ee);
                boolean coloredBar = bg != null && bg.getDefaultColor() == primary;
                mActiveIconColor = coloredBar
                        ? MaterialColors.getColor(context, R.attr.colorOnPrimary, 0xffffffff) : primary;
                mActiveTextColor = mActiveIconColor;
            }
            mIconSize = a.getDimensionPixelSize(R.styleable.NavigationBarView_itemIconSize, MaterialAttrs.dpi(context, 24));
            mLabelVisibility = a.getInt(R.styleable.NavigationBarView_labelVisibilityMode, LABEL_VISIBILITY_AUTO);
            menuRes = a.getResourceId(R.styleable.NavigationBarView_menu, 0);
        } finally {
            a.recycle();
        }
        TypedArray ind = context.obtainStyledAttributes(attrs, R.styleable.NavigationBarActiveIndicator, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            mIndicatorColor = ind.getColor(R.styleable.NavigationBarActiveIndicator_activeIndicatorColor,
                    mMaterial3 ? MaterialColors.getColor(context, R.attr.colorSecondaryContainer, 0xffe8def8) : 0);
            mIndicatorWidth = ind.getDimensionPixelSize(R.styleable.NavigationBarActiveIndicator_activeIndicatorWidth,
                    mMaterial3 ? MaterialAttrs.dpi(context, 64) : 0);
            mIndicatorHeight = ind.getDimensionPixelSize(R.styleable.NavigationBarActiveIndicator_activeIndicatorHeight,
                    mMaterial3 ? MaterialAttrs.dpi(context, 32) : 0);
        } finally {
            ind.recycle();
        }
        if (getMinimumHeight() == 0) {
            setMinimumHeight(MaterialAttrs.dpi(context, mMaterial3 ? 80 : 56));
        }
        mMenu.setListener(new MenuImpl.Listener() {
            @Override
            public void menuChanged(MenuImpl menu) {
                // The bar checks its own items while it rebuilds; those
                // changes are its own, not the application's.
                if (!mRebuilding) {
                    rebuild();
                }
            }
        });
        if (menuRes != 0) {
            inflateMenu(menuRes);
        }
    }

    @Override
    protected boolean drawsOutsideBounds() {
        return mSurface != null && mSurface.getElevation() > 0;
    }

    /// The most items a bar shows.
    public int getMaxItemCount() {
        return 5;
    }

    public Menu getMenu() {
        return mMenu;
    }

    public void inflateMenu(int resId) {
        new MenuInflater(getContext()).inflate(resId, mMenu);
        rebuild();
    }

    private boolean mRebuilding;

    private void rebuild() {
        mRebuilding = true;
        try {
            rebuildItems();
        } finally {
            mRebuilding = false;
        }
    }

    private void rebuildItems() {
        mItems.removeAllViews();
        List<MenuImpl.Item> items = mMenu.visibleItems();
        if (items.size() > getMaxItemCount()) {
            throw new IllegalArgumentException("Maximum number of items supported by " + getClass().getSimpleName()
                    + " is " + getMaxItemCount());
        }
        boolean found = false;
        for (MenuImpl.Item item : items) {
            if (item.getItemId() == mSelectedId) {
                found = true;
            }
        }
        if (!found) {
            mSelectedId = items.isEmpty() ? View.NO_ID : items.get(0).getItemId();
        }
        for (final MenuImpl.Item item : items) {
            ItemView v = new ItemView(getContext(), item);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    select(item);
                }
            });
            mItems.addView(v, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        }
        syncChecked();
    }

    private void syncChecked() {
        boolean outer = !mRebuilding;
        mRebuilding = true;
        try {
            for (MenuImpl.Item item : mMenu.visibleItems()) {
                item.setChecked(item.getItemId() == mSelectedId);
            }
        } finally {
            if (outer) {
                mRebuilding = false;
            }
        }
        for (int i = 0; i < mItems.getChildCount(); i++) {
            mItems.getChildAt(i).invalidate();
        }
    }

    private void select(MenuItem item) {
        if (item.getItemId() == mSelectedId) {
            if (mReselectedListener != null) {
                mReselectedListener.onNavigationItemReselected(item);
            }
            return;
        }
        if (mSelectedListener != null && !mSelectedListener.onNavigationItemSelected(item)) {
            return;
        }
        mSelectedId = item.getItemId();
        syncChecked();
    }

    public void setSelectedItemId(int itemId) {
        MenuItem item = mMenu.findItem(itemId);
        if (item != null) {
            select(item);
        }
    }

    public int getSelectedItemId() {
        return mSelectedId;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mSelectedListener = listener;
    }

    public void setOnItemReselectedListener(OnItemReselectedListener listener) {
        mReselectedListener = listener;
    }

    public void setLabelVisibilityMode(int mode) {
        mLabelVisibility = mode;
        syncChecked();
    }

    public int getLabelVisibilityMode() {
        return mLabelVisibility;
    }

    public void setItemIconTintList(ColorStateList tint) {
        mIconTint = tint;
        syncChecked();
    }

    public ColorStateList getItemIconTintList() {
        return mIconTint;
    }

    public void setItemTextColor(ColorStateList color) {
        mTextColor = color;
        syncChecked();
    }

    public ColorStateList getItemTextColor() {
        return mTextColor;
    }

    public void setItemIconSize(int size) {
        mIconSize = size;
        syncChecked();
    }

    public void setItemActiveIndicatorColor(ColorStateList color) {
        mIndicatorColor = color == null ? 0 : color.getDefaultColor();
        syncChecked();
    }

    private boolean labelShown(boolean selected) {
        int mode = mLabelVisibility;
        if (mode == LABEL_VISIBILITY_AUTO) {
            mode = mMenu.visibleItems().size() > 3 ? LABEL_VISIBILITY_SELECTED : LABEL_VISIBILITY_LABELED;
        }
        return mode == LABEL_VISIBILITY_LABELED || (mode == LABEL_VISIBILITY_SELECTED && selected);
    }

    private static int colorFor(ColorStateList csl, boolean checked, int activeOverride) {
        if (csl.isStateful()) {
            return csl.getColorForState(checked ? new int[] {android.R.attr.state_checked, android.R.attr.state_enabled}
                    : new int[] {android.R.attr.state_enabled}, csl.getDefaultColor());
        }
        return checked ? activeOverride : csl.getDefaultColor();
    }

    /// One destination: the indicator, icon and label drawn in place.
    private final class ItemView extends View {
        private final MenuImpl.Item item;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        /// As tall as the bar's minimum (80dp under Material 3, 56dp under
        /// Material Components 2), not the whole space offered: View's default
        /// measure takes all of an at-most spec, which made a wrap_content bar
        /// fill its parent.
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                    resolveSize(NavigationBarView.this.getMinimumHeight(), heightMeasureSpec));
        }

        ItemView(Context context, MenuImpl.Item item) {
            super(context);
            this.item = item;
            setClickable(true);
            setContentDescription(item.getTitle());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            boolean selected = item.getItemId() == mSelectedId;
            Context c = getContext();
            boolean label = labelShown(selected);
            float labelSize = MaterialAttrs.dp(c, 12) * getResources().getDisplayMetrics().scaledDensity
                    / getResources().getDisplayMetrics().density;
            if (!mMaterial3 && selected) {
                labelSize = labelSize * 14f / 12f;
            }
            paint.setTextSize(labelSize);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            Paint.FontMetrics fm = paint.getFontMetrics();
            float labelHeight = fm.descent - fm.ascent;
            float gap = MaterialAttrs.dp(c, 4);
            float block = mMaterial3 ? Math.max(mIndicatorHeight, mIconSize) : mIconSize;
            float total = block + (label ? gap + labelHeight : 0);
            float top = (getHeight() - total) / 2f;
            float cx = getWidth() / 2f;
            float iconCy = top + block / 2f;
            if (mMaterial3 && selected && (mIndicatorColor >>> 24) > 0 && mIndicatorWidth > 0) {
                paint.setColor(mIndicatorColor);
                float w = Math.min(mIndicatorWidth, getWidth() - MaterialAttrs.dp(c, 8));
                RectF r = new RectF(cx - w / 2f, iconCy - mIndicatorHeight / 2f, cx + w / 2f,
                        iconCy + mIndicatorHeight / 2f);
                canvas.drawRoundRect(r, mIndicatorHeight / 2f, mIndicatorHeight / 2f, paint);
            }
            Drawable icon = item.getIcon();
            if (icon != null) {
                Drawable d = MaterialAttrs.tinted(icon,
                        ColorStateList.valueOf(colorFor(mIconTint, selected, mActiveIconColor)));
                int half = mIconSize / 2;
                d.setBounds(Math.round(cx) - half, Math.round(iconCy) - half, Math.round(cx) - half + mIconSize,
                        Math.round(iconCy) - half + mIconSize);
                d.draw(canvas);
            }
            if (label && item.getTitle() != null) {
                String t = item.getTitle().toString();
                paint.setColor(colorFor(mTextColor, selected, mActiveTextColor));
                float tw = paint.measureText(t);
                float baseline = top + block + gap - fm.ascent;
                canvas.drawText(t, cx - tw / 2f, baseline, paint);
            }
        }
    }
}
