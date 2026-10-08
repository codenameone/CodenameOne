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
package com.google.android.material.tabs;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;

import java.util.ArrayList;
import java.util.List;

/// A row of tabs with a sliding indicator under the selected one. Fixed
/// tabs share the width; scrollable tabs keep their natural width and scroll.
public class TabLayout extends HorizontalScrollView {

    public static final int MODE_SCROLLABLE = 0;
    public static final int MODE_FIXED = 1;
    public static final int MODE_AUTO = 2;
    public static final int GRAVITY_FILL = 0;
    public static final int GRAVITY_CENTER = 1;
    public static final int GRAVITY_START = 2;

    /// Called when a tab is selected, unselected or selected again.
    public interface BaseOnTabSelectedListener<T extends Tab> {
        void onTabSelected(T tab);

        void onTabUnselected(T tab);

        void onTabReselected(T tab);
    }

    /// The listener for this layout's [Tab]s.
    public interface OnTabSelectedListener extends BaseOnTabSelectedListener<Tab> {
    }

    /// One tab: its text, icon or custom view, and its position.
    public static class Tab {
        public static final int INVALID_POSITION = -1;
        private Object tag;
        private CharSequence text;
        private Drawable icon;
        private View customView;
        private CharSequence contentDescription;
        private int position = INVALID_POSITION;
        TabLayout parent;
        TabView view;

        public Object getTag() {
            return tag;
        }

        public Tab setTag(Object tag) {
            this.tag = tag;
            return this;
        }

        public CharSequence getText() {
            return text;
        }

        public Tab setText(CharSequence text) {
            this.text = text;
            update();
            return this;
        }

        public Tab setText(int resId) {
            if (parent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            return setText(parent.getResources().getText(resId));
        }

        public Drawable getIcon() {
            return icon;
        }

        public Tab setIcon(Drawable icon) {
            this.icon = icon;
            update();
            return this;
        }

        public Tab setIcon(int resId) {
            if (parent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            return setIcon(parent.getContext().getDrawable(resId));
        }

        public View getCustomView() {
            return customView;
        }

        public Tab setCustomView(View view) {
            customView = view;
            update();
            return this;
        }

        public Tab setCustomView(int layoutResId) {
            if (parent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            return setCustomView(LayoutInflater.from(parent.getContext()).inflate(layoutResId, view, false));
        }

        public Tab setContentDescription(CharSequence description) {
            contentDescription = description;
            update();
            return this;
        }

        public CharSequence getContentDescription() {
            return contentDescription;
        }

        public int getPosition() {
            return position;
        }

        public void select() {
            if (parent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            parent.selectTab(this);
        }

        public boolean isSelected() {
            return parent != null && parent.getSelectedTabPosition() == position && position != INVALID_POSITION;
        }

        public TabView getView() {
            return view;
        }

        private void update() {
            if (view != null) {
                view.update();
            }
        }
    }

    private final Strip mStrip;
    private final List<Tab> mTabs = new ArrayList<Tab>();
    private final List<BaseOnTabSelectedListener> mListeners = new ArrayList<BaseOnTabSelectedListener>();
    private BaseOnTabSelectedListener mSelectedListener;
    private Tab mSelected;
    private int mMode;
    private int mGravity;
    private int mIndicatorColor;
    private int mIndicatorHeight;
    private boolean mIndicatorFullWidth;
    private ColorStateList mTextColors;
    private int mSelectedTextColor;
    private ColorStateList mIconTint;
    private int mTextAppearance;
    private int mTabPaddingStart;
    private int mTabPaddingEnd;
    private int mTabMinWidth;
    private int mTabMaxWidth;
    private boolean mInlineLabel;

    public TabLayout(Context context) {
        this(context, null);
    }

    public TabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.tabStyle));
    }

    public TabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setHorizontalScrollBarEnabled(false);
        setFillViewport(true);
        mStrip = new Strip(context);
        super.addView(mStrip, 0, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_TabLayout
                : R.style.Widget_MaterialComponents_TabLayout;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabLayout, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            int primary = MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6750a4);
            mMode = a.getInt(R.styleable.TabLayout_tabMode, MODE_FIXED);
            mGravity = a.getInt(R.styleable.TabLayout_tabGravity, GRAVITY_FILL);
            mIndicatorColor = a.getColor(R.styleable.TabLayout_tabIndicatorColor, primary);
            mIndicatorHeight = a.getDimensionPixelSize(R.styleable.TabLayout_tabIndicatorHeight,
                    MaterialAttrs.dpi(context, 2));
            mIndicatorFullWidth = a.getBoolean(R.styleable.TabLayout_tabIndicatorFullWidth, true);
            mTextColors = a.getColorStateList(R.styleable.TabLayout_tabTextColor);
            if (mTextColors == null) {
                mTextColors = ColorStateList.valueOf(MaterialColors.getColor(context, R.attr.colorOnSurfaceVariant,
                        0x99000000));
            }
            mSelectedTextColor = a.getColor(R.styleable.TabLayout_tabSelectedTextColor, primary);
            mIconTint = a.getColorStateList(R.styleable.TabLayout_tabIconTint);
            mTextAppearance = a.getResourceId(R.styleable.TabLayout_tabTextAppearance, 0);
            mTabPaddingStart = a.getDimensionPixelSize(R.styleable.TabLayout_tabPaddingStart,
                    MaterialAttrs.dpi(context, 12));
            mTabPaddingEnd = a.getDimensionPixelSize(R.styleable.TabLayout_tabPaddingEnd, MaterialAttrs.dpi(context, 12));
            mTabMinWidth = a.getDimensionPixelSize(R.styleable.TabLayout_tabMinWidth, MaterialAttrs.dpi(context, 72));
            mTabMaxWidth = a.getDimensionPixelSize(R.styleable.TabLayout_tabMaxWidth, MaterialAttrs.dpi(context, 264));
            mInlineLabel = a.getBoolean(R.styleable.TabLayout_tabInlineLabel, false);
        } finally {
            a.recycle();
        }
        applyMode();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof TabItem) {
            TabItem item = (TabItem) child;
            Tab tab = newTab();
            if (item.text != null) {
                tab.setText(item.text);
            }
            if (item.icon != null) {
                tab.setIcon(item.icon);
            }
            if (item.customLayout != 0) {
                tab.setCustomView(item.customLayout);
            }
            addTab(tab);
            return;
        }
        if (child == mStrip) {
            super.addView(child, index, params);
            return;
        }
        throw new IllegalArgumentException("Only TabItem instances can be added to TabLayout");
    }

    private void applyMode() {
        mStrip.setGravity(mMode == MODE_FIXED && mGravity == GRAVITY_CENTER ? Gravity.CENTER_HORIZONTAL
                : Gravity.START);
        for (Tab t : mTabs) {
            t.view.setLayoutParams(tabParams());
        }
        mStrip.requestLayout();
    }

    private LinearLayout.LayoutParams tabParams() {
        if (mMode == MODE_FIXED && mGravity == GRAVITY_FILL) {
            return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        }
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean icons = false;
        boolean texts = false;
        for (Tab t : mTabs) {
            icons |= t.icon != null;
            texts |= t.text != null && t.text.length() > 0;
        }
        int h = MaterialAttrs.dpi(getContext(), icons && texts && !mInlineLabel ? 72 : 48);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        if (hMode != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    hMode == MeasureSpec.AT_MOST ? Math.min(h, MeasureSpec.getSize(heightMeasureSpec)) : h,
                    MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // ------------------------------------------------------------ tabs

    public Tab newTab() {
        Tab tab = new Tab();
        tab.parent = this;
        tab.view = new TabView(getContext(), tab);
        return tab;
    }

    public void addTab(Tab tab) {
        addTab(tab, mTabs.isEmpty());
    }

    public void addTab(Tab tab, boolean setSelected) {
        addTab(tab, mTabs.size(), setSelected);
    }

    public void addTab(Tab tab, int position) {
        addTab(tab, position, mTabs.isEmpty());
    }

    public void addTab(Tab tab, int position, boolean setSelected) {
        if (tab.parent != this) {
            throw new IllegalArgumentException("Tab belongs to a different TabLayout.");
        }
        mTabs.add(position, tab);
        for (int i = 0; i < mTabs.size(); i++) {
            mTabs.get(i).position = i;
        }
        tab.view.update();
        mStrip.addView(tab.view, position, tabParams());
        if (setSelected) {
            selectTab(tab);
        }
    }

    public void removeTab(Tab tab) {
        removeTabAt(tab.getPosition());
    }

    public void removeTabAt(int position) {
        Tab removed = mTabs.remove(position);
        mStrip.removeView(removed.view);
        removed.position = Tab.INVALID_POSITION;
        for (int i = 0; i < mTabs.size(); i++) {
            mTabs.get(i).position = i;
        }
        if (removed == mSelected) {
            mSelected = null;
            if (!mTabs.isEmpty()) {
                selectTab(mTabs.get(Math.max(0, position - 1)));
            }
        }
    }

    public void removeAllTabs() {
        mStrip.removeAllViews();
        for (Tab t : mTabs) {
            t.position = Tab.INVALID_POSITION;
        }
        mTabs.clear();
        mSelected = null;
    }

    public int getTabCount() {
        return mTabs.size();
    }

    public Tab getTabAt(int index) {
        return index < 0 || index >= mTabs.size() ? null : mTabs.get(index);
    }

    public int getSelectedTabPosition() {
        return mSelected == null ? -1 : mSelected.getPosition();
    }

    public void selectTab(Tab tab) {
        selectTab(tab, true);
    }

    @SuppressWarnings("unchecked")
    public void selectTab(Tab tab, boolean updateIndicator) {
        Tab previous = mSelected;
        if (previous == tab) {
            if (previous != null) {
                for (BaseOnTabSelectedListener l : new ArrayList<BaseOnTabSelectedListener>(mListeners)) {
                    l.onTabReselected(tab);
                }
            }
            return;
        }
        mSelected = tab;
        if (previous != null) {
            previous.view.update();
            for (BaseOnTabSelectedListener l : new ArrayList<BaseOnTabSelectedListener>(mListeners)) {
                l.onTabUnselected(previous);
            }
        }
        if (tab != null) {
            tab.view.update();
            mStrip.animateTo(tab.getPosition(), updateIndicator && previous != null);
            for (BaseOnTabSelectedListener l : new ArrayList<BaseOnTabSelectedListener>(mListeners)) {
                l.onTabSelected(tab);
            }
            if (mMode == MODE_SCROLLABLE) {
                int target = tab.view.getLeft() - (getWidth() - tab.view.getWidth()) / 2;
                smoothScrollTo(Math.max(0, target), 0);
            }
        }
    }

    /// Places the indicator `positionOffset` of the way from tab `position`
    /// to the next one, as a pager being dragged does, and scrolls a
    /// scrollable row to follow it. The selected tab is unchanged.
    public void setScrollPosition(int position, float positionOffset, boolean updateSelectedText) {
        setScrollPosition(position, positionOffset, updateSelectedText, true);
    }

    public void setScrollPosition(int position, float positionOffset, boolean updateSelectedText,
            boolean updateIndicatorPosition) {
        int count = getTabCount();
        if (position < 0 || position >= count) {
            return;
        }
        if (updateIndicatorPosition) {
            mStrip.setFromPosition(position, positionOffset);
        }
        if (mMode == MODE_SCROLLABLE) {
            View tab = mStrip.getChildAt(position);
            View next = position + 1 < count ? mStrip.getChildAt(position + 1) : null;
            if (tab != null && tab.getWidth() > 0) {
                float center = tab.getLeft() + tab.getWidth() / 2f;
                if (next != null) {
                    float nextCenter = next.getLeft() + next.getWidth() / 2f;
                    center += (nextCenter - center) * positionOffset;
                }
                scrollTo(Math.max(0, Math.round(center - getWidth() / 2f)), 0);
            }
        }
        if (updateSelectedText) {
            int nearest = Math.round(position + positionOffset);
            for (int i = 0; i < count; i++) {
                View v = mStrip.getChildAt(i);
                if (v != null) {
                    v.setSelected(i == nearest);
                }
            }
        }
    }

    public void addOnTabSelectedListener(BaseOnTabSelectedListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void addOnTabSelectedListener(OnTabSelectedListener listener) {
        addOnTabSelectedListener((BaseOnTabSelectedListener) listener);
    }

    public void removeOnTabSelectedListener(BaseOnTabSelectedListener listener) {
        mListeners.remove(listener);
    }

    public void removeOnTabSelectedListener(OnTabSelectedListener listener) {
        removeOnTabSelectedListener((BaseOnTabSelectedListener) listener);
    }

    public void clearOnTabSelectedListeners() {
        mListeners.clear();
    }

    /// Replaces the listener set by the previous call, as the deprecated
    /// API did.
    public void setOnTabSelectedListener(BaseOnTabSelectedListener listener) {
        if (mSelectedListener != null) {
            mListeners.remove(mSelectedListener);
        }
        mSelectedListener = listener;
        if (listener != null) {
            addOnTabSelectedListener(listener);
        }
    }

    public void setTabMode(int mode) {
        mMode = mode == MODE_AUTO ? MODE_SCROLLABLE : mode;
        applyMode();
    }

    public int getTabMode() {
        return mMode;
    }

    public void setTabGravity(int gravity) {
        mGravity = gravity;
        applyMode();
    }

    public int getTabGravity() {
        return mGravity;
    }

    public void setSelectedTabIndicatorColor(int color) {
        mIndicatorColor = color;
        mStrip.invalidate();
    }

    public void setSelectedTabIndicatorHeight(int height) {
        mIndicatorHeight = height;
        mStrip.invalidate();
    }

    public void setTabIndicatorFullWidth(boolean fullWidth) {
        mIndicatorFullWidth = fullWidth;
        mStrip.invalidate();
    }

    public void setTabTextColors(ColorStateList textColor) {
        mTextColors = textColor;
        for (Tab t : mTabs) {
            t.view.update();
        }
    }

    public void setTabTextColors(int normalColor, int selectedColor) {
        mSelectedTextColor = selectedColor;
        setTabTextColors(ColorStateList.valueOf(normalColor));
    }

    public ColorStateList getTabTextColors() {
        return mTextColors;
    }

    public void setTabIconTint(ColorStateList iconTint) {
        mIconTint = iconTint;
        for (Tab t : mTabs) {
            t.view.update();
        }
    }

    private int textColor(boolean selected) {
        if (mTextColors.isStateful()) {
            return mTextColors.getColorForState(selected ? new int[] {android.R.attr.state_selected}
                    : new int[0], mTextColors.getDefaultColor());
        }
        return selected ? mSelectedTextColor : mTextColors.getDefaultColor();
    }

    /// The view of one tab: icon over (or beside) the label, or the tab's
    /// custom view.
    public final class TabView extends LinearLayout {
        private final Tab tab;
        private final ImageView iconView;
        private final TextView textView;
        private View custom;

        TabView(Context context, Tab tab) {
            super(context);
            this.tab = tab;
            setGravity(Gravity.CENTER);
            setOrientation(mInlineLabel ? HORIZONTAL : VERTICAL);
            setPaddingRelative(mTabPaddingStart, 0, mTabPaddingEnd, 0);
            setMinimumWidth(mMode == MODE_SCROLLABLE ? mTabMinWidth : 0);
            setClickable(true);
            iconView = new ImageView(context);
            int size = MaterialAttrs.dpi(context, 24);
            addView(iconView, new LinearLayout.LayoutParams(size, size));
            textView = new TextView(context);
            if (mTextAppearance != 0) {
                textView.setTextAppearance(mTextAppearance);
            }
            textView.setMaxLines(2);
            textView.setGravity(Gravity.CENTER);
            addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TabView.this.tab.select();
                }
            });
        }

        public Tab getTab() {
            return tab;
        }

        void update() {
            boolean selected = tab.isSelected();
            setSelected(selected);
            if (tab.customView != null) {
                if (custom != tab.customView) {
                    if (custom != null) {
                        removeView(custom);
                    }
                    custom = tab.customView;
                    if (custom.getParent() instanceof ViewGroup) {
                        ((ViewGroup) custom.getParent()).removeView(custom);
                    }
                    addView(custom);
                }
                iconView.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
                custom.setSelected(selected);
                return;
            }
            if (custom != null) {
                removeView(custom);
                custom = null;
            }
            int color = textColor(selected);
            textView.setText(tab.text);
            textView.setTextColor(color);
            textView.setVisibility(tab.text != null && tab.text.length() > 0 ? View.VISIBLE : View.GONE);
            if (tab.icon != null) {
                ColorStateList tint = mIconTint != null && mIconTint.isStateful() ? mIconTint
                        : ColorStateList.valueOf(color);
                iconView.setImageDrawable(MaterialAttrs.tinted(tab.icon, tint));
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
            setContentDescription(tab.contentDescription != null ? tab.contentDescription : tab.text);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // A scrollable row caps each tab at tabMaxWidth (264dp by default).
            if (mMode != MODE_FIXED && mTabMaxWidth > 0) {
                int mode = MeasureSpec.getMode(widthMeasureSpec);
                int size = MeasureSpec.getSize(widthMeasureSpec);
                if (mode == MeasureSpec.UNSPECIFIED || size > mTabMaxWidth) {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(mTabMaxWidth, MeasureSpec.AT_MOST);
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        /// The extent of the tab's content, for an indicator that is as
        /// wide as the content rather than the tab.
        int contentWidth() {
            int w = 0;
            if (custom != null) {
                return custom.getWidth();
            }
            if (iconView.getVisibility() == View.VISIBLE) {
                w = iconView.getWidth();
            }
            if (textView.getVisibility() == View.VISIBLE) {
                w = mInlineLabel ? w + textView.getWidth() : Math.max(w, textView.getWidth());
            }
            return w;
        }
    }

    /// The row of tab views, which draws the indicator.
    private final class Strip extends LinearLayout {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float left = -1f;
        private float right = -1f;
        private int target = -1;
        private ValueAnimator animator;

        Strip(Context context) {
            super(context);
            setOrientation(HORIZONTAL);
            setWillNotDraw(false);
        }

        private float[] boundsOf(int position) {
            View v = getChildAt(position);
            if (!(v instanceof TabView) || v.getWidth() == 0) {
                return null;
            }
            float l = v.getLeft();
            float r = v.getRight();
            if (!mIndicatorFullWidth) {
                int cw = Math.max(((TabView) v).contentWidth(), MaterialAttrs.dpi(getContext(), 24));
                float c = (l + r) / 2f;
                l = c - cw / 2f;
                r = c + cw / 2f;
            }
            return new float[] {l, r};
        }

        void setFromPosition(int position, float offset) {
            float[] from = boundsOf(position);
            if (from == null) {
                return;
            }
            if (animator != null) {
                animator.cancel();
            }
            float[] to = offset > 0f && position + 1 < getChildCount() ? boundsOf(position + 1) : null;
            if (to == null) {
                left = from[0];
                right = from[1];
            } else {
                left = from[0] + (to[0] - from[0]) * offset;
                right = from[1] + (to[1] - from[1]) * offset;
            }
            target = offset >= 0.5f && to != null ? position + 1 : position;
            invalidate();
        }

        void animateTo(int position, boolean animate) {
            target = position;
            final float[] to = boundsOf(position);
            if (to == null || left < 0 || !animate) {
                if (to != null) {
                    left = to[0];
                    right = to[1];
                }
                invalidate();
                return;
            }
            if (animator != null) {
                animator.cancel();
            }
            final float fromLeft = left;
            final float fromRight = right;
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(250);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator a) {
                    float f = a.getAnimatedFraction();
                    left = fromLeft + (to[0] - fromLeft) * f;
                    right = fromRight + (to[1] - fromRight) * f;
                    invalidate();
                }
            });
            animator.start();
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (target >= 0 && (animator == null || !animator.isRunning())) {
                float[] to = boundsOf(target);
                if (to != null) {
                    left = to[0];
                    right = to[1];
                }
            }
        }

        @Override
        public void onDrawForeground(Canvas canvas) {
            super.onDrawForeground(canvas);
            if (left < 0 || right <= left || mIndicatorHeight <= 0) {
                return;
            }
            paint.setColor(mIndicatorColor);
            float bottom = getHeight();
            float top = bottom - mIndicatorHeight;
            if (!mIndicatorFullWidth && MaterialAttrs.isMaterial3(getContext())) {
                // Material 3's primary tabs: rounded top corners.
                float r = mIndicatorHeight;
                Path p = new Path();
                p.addRoundRect(new RectF(left, top, right, bottom), new float[] {r, r, r, r, 0, 0, 0, 0},
                        Path.Direction.CW);
                canvas.drawPath(p, paint);
            } else {
                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
    }
}
