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
package androidx.appcompat.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.R;

import com.codename1.androidcompat.runtime.MenuImpl;
import com.codename1.androidcompat.runtime.MenuPopup;

import java.util.ArrayList;
import java.util.List;

/// AppCompat's toolbar: a view in the layout with a navigation button, a
/// logo, a title and subtitle, the application's own child views, and a menu
/// of action buttons with an overflow popup. `setSupportActionBar` makes it
/// the activity's action bar, so the options menu shows here.
///
/// Laid out as AppCompat's: the navigation button `maxButtonHeight` tall at
/// the start, the title at `contentInsetStart` (or
/// `contentInsetStartWithNavigation` after a navigation button), vertically
/// centered, and the menu at the end, 48dp per action and 36dp for the
/// overflow button.
public class Toolbar extends ViewGroup {

    /// Hears taps on menu items the activity does not handle.
    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem item);
    }

    /// Layout parameters of a child of a toolbar.
    public static class LayoutParams extends androidx.appcompat.app.ActionBar.LayoutParams {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height, gravity);
        }

        public LayoutParams(int gravity) {
            super(gravity);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private ImageButton mNavButtonView;
    private ImageView mLogoView;
    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private ImageButton mOverflowButton;
    private final List<View> mActionViews = new ArrayList<View>();
    private final List<View> mSystemViews = new ArrayList<View>();

    private CharSequence mTitleText;
    private CharSequence mSubtitleText;
    private ColorStateList mTitleTextColor;
    private ColorStateList mSubtitleTextColor;
    private int mTitleTextAppearance;
    private int mSubtitleTextAppearance;
    private int mTitleMarginStart;
    private int mTitleMarginEnd;
    private int mTitleMarginTop;
    private int mTitleMarginBottom;
    private int mContentInsetStart;
    private int mContentInsetEnd;
    private int mContentInsetStartWithNavigation;
    private int mContentInsetEndWithActions;
    private int mMaxButtonHeight;
    private int mGravity = Gravity.START | Gravity.CENTER_VERTICAL;
    private int mPopupTheme;
    private Drawable mOverflowIcon;
    private View.OnClickListener mNavigationListener;

    private final MenuImpl mMenu;
    private MenuImpl mPresentedMenu;
    private boolean mMenuVisible = true;
    private OnMenuItemClickListener mOnMenuItemClickListener;
    private final List<MenuImpl.Item> mOverflowItems = new ArrayList<MenuImpl.Item>();

    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, AttributeSet attrs) {
        this(context, attrs, AppCompatAttrs.defStyle(context, R.attr.toolbarStyle, 0));
    }

    public Toolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, defStyleAttr == R.attr.toolbarStyle ? 0 : R.style.Widget_AppCompat_Toolbar);
        mMenu = new MenuImpl(context);
        mMenu.setItemHandler(new MenuImpl.ItemHandler() {
            @Override
            public boolean onItemSelected(MenuItem item) {
                return mOnMenuItemClickListener != null && mOnMenuItemClickListener.onMenuItemClick(item);
            }
        });
        mMenu.setListener(new MenuImpl.Listener() {
            @Override
            public void menuChanged(MenuImpl menu) {
                if (mPresentedMenu == null) {
                    rebuildMenuViews();
                }
            }
        });
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Toolbar, defStyleAttr,
                defStyleAttr == R.attr.toolbarStyle ? 0 : R.style.Widget_AppCompat_Toolbar);
        try {
            float dp = context.getResources().getDisplayMetrics().density;
            mTitleTextAppearance = a.getResourceId(R.styleable.Toolbar_titleTextAppearance, 0);
            mSubtitleTextAppearance = a.getResourceId(R.styleable.Toolbar_subtitleTextAppearance, 0);
            mGravity = a.getInt(R.styleable.Toolbar_android_gravity, mGravity);
            int margin = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMargin, Math.round(4 * dp));
            if (a.hasValue(R.styleable.Toolbar_titleMargins)) {
                margin = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMargins, margin);
            }
            mTitleMarginStart = mTitleMarginEnd = mTitleMarginTop = mTitleMarginBottom = margin;
            mTitleMarginStart = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginStart, mTitleMarginStart);
            mTitleMarginEnd = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginEnd, mTitleMarginEnd);
            mTitleMarginTop = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginTop, mTitleMarginTop);
            mTitleMarginBottom = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginBottom, mTitleMarginBottom);
            mMaxButtonHeight = a.getDimensionPixelSize(R.styleable.Toolbar_maxButtonHeight, Math.round(56 * dp));
            mContentInsetStart = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetStart, Math.round(16 * dp));
            mContentInsetEnd = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetEnd, 0);
            if (a.hasValue(R.styleable.Toolbar_contentInsetLeft)) {
                mContentInsetStart = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetLeft, mContentInsetStart);
            }
            if (a.hasValue(R.styleable.Toolbar_contentInsetRight)) {
                mContentInsetEnd = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetRight, mContentInsetEnd);
            }
            mContentInsetStartWithNavigation = a.getDimensionPixelOffset(
                    R.styleable.Toolbar_contentInsetStartWithNavigation, Math.round(72 * dp));
            mContentInsetEndWithActions = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetEndWithActions, 0);
            mPopupTheme = a.getResourceId(R.styleable.Toolbar_popupTheme, 0);
            if (getMinimumHeight() == 0) {
                setMinimumHeight(a.getDimensionPixelSize(R.styleable.Toolbar_android_minHeight, Math.round(56 * dp)));
            }
            CharSequence title = a.getText(R.styleable.Toolbar_title);
            if (!TextUtils.isEmpty(title)) {
                setTitle(title);
            }
            CharSequence subtitle = a.getText(R.styleable.Toolbar_subtitle);
            if (!TextUtils.isEmpty(subtitle)) {
                setSubtitle(subtitle);
            }
            if (a.hasValue(R.styleable.Toolbar_titleTextColor)) {
                setTitleTextColor(a.getColorStateList(R.styleable.Toolbar_titleTextColor));
            }
            if (a.hasValue(R.styleable.Toolbar_subtitleTextColor)) {
                setSubtitleTextColor(a.getColorStateList(R.styleable.Toolbar_subtitleTextColor));
            }
            Drawable nav = a.getDrawable(R.styleable.Toolbar_navigationIcon);
            if (nav != null) {
                setNavigationIcon(nav);
            }
            CharSequence navDesc = a.getText(R.styleable.Toolbar_navigationContentDescription);
            if (!TextUtils.isEmpty(navDesc)) {
                setNavigationContentDescription(navDesc);
            }
            Drawable logo = a.getDrawable(R.styleable.Toolbar_logo);
            if (logo != null) {
                setLogo(logo);
            }
            int menu = a.getResourceId(R.styleable.Toolbar_menu, 0);
            if (menu != 0) {
                inflateMenu(menu);
            }
        } finally {
            a.recycle();
        }
    }

    // ------------------------------------------------------------ title

    public CharSequence getTitle() {
        return mTitleText;
    }

    public void setTitle(int resId) {
        setTitle(getContext().getText(resId));
    }

    public void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) {
            if (mTitleTextView == null) {
                mTitleTextView = newTextView(mTitleTextAppearance, mTitleTextColor);
                addSystemView(mTitleTextView);
            }
        } else if (mTitleTextView != null) {
            removeSystemView(mTitleTextView);
            mTitleTextView = null;
        }
        if (mTitleTextView != null) {
            mTitleTextView.setText(title);
        }
        mTitleText = title;
    }

    public CharSequence getSubtitle() {
        return mSubtitleText;
    }

    public void setSubtitle(int resId) {
        setSubtitle(getContext().getText(resId));
    }

    public void setSubtitle(CharSequence subtitle) {
        if (!TextUtils.isEmpty(subtitle)) {
            if (mSubtitleTextView == null) {
                mSubtitleTextView = newTextView(mSubtitleTextAppearance, mSubtitleTextColor);
                addSystemView(mSubtitleTextView);
            }
        } else if (mSubtitleTextView != null) {
            removeSystemView(mSubtitleTextView);
            mSubtitleTextView = null;
        }
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setText(subtitle);
        }
        mSubtitleText = subtitle;
    }

    private TextView newTextView(int appearance, ColorStateList color) {
        TextView t = new TextView(getContext());
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        if (appearance != 0) {
            t.setTextAppearance(appearance);
        }
        if (color != null) {
            t.setTextColor(color);
        }
        return t;
    }

    public void setTitleTextAppearance(Context context, int resId) {
        mTitleTextAppearance = resId;
        if (mTitleTextView != null) {
            mTitleTextView.setTextAppearance(resId);
        }
    }

    public void setSubtitleTextAppearance(Context context, int resId) {
        mSubtitleTextAppearance = resId;
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setTextAppearance(resId);
        }
    }

    public void setTitleTextColor(int color) {
        setTitleTextColor(ColorStateList.valueOf(color));
    }

    public void setTitleTextColor(ColorStateList color) {
        mTitleTextColor = color;
        if (mTitleTextView != null && color != null) {
            mTitleTextView.setTextColor(color);
        }
    }

    public void setSubtitleTextColor(int color) {
        setSubtitleTextColor(ColorStateList.valueOf(color));
    }

    public void setSubtitleTextColor(ColorStateList color) {
        mSubtitleTextColor = color;
        if (mSubtitleTextView != null && color != null) {
            mSubtitleTextView.setTextColor(color);
        }
    }

    public void setTitleMargin(int start, int top, int end, int bottom) {
        mTitleMarginStart = start;
        mTitleMarginTop = top;
        mTitleMarginEnd = end;
        mTitleMarginBottom = bottom;
        requestLayout();
    }

    public int getTitleMarginStart() {
        return mTitleMarginStart;
    }

    public void setTitleMarginStart(int margin) {
        mTitleMarginStart = margin;
        requestLayout();
    }

    public int getTitleMarginEnd() {
        return mTitleMarginEnd;
    }

    public void setTitleMarginEnd(int margin) {
        mTitleMarginEnd = margin;
        requestLayout();
    }

    // ------------------------------------------------------------ navigation and logo

    private ImageButton ensureNavButtonView() {
        if (mNavButtonView == null) {
            int style = AppCompatAttrs.defStyle(getContext(), R.attr.toolbarNavigationButtonStyle, 0);
            mNavButtonView = new ImageButton(getContext(), null, style,
                    style == 0 ? R.style.Widget_AppCompat_Toolbar_Button_Navigation : 0);
            if (style == 0) {
                mNavButtonView.setBackground(null);
            }
            mNavButtonView.setScaleType(ImageView.ScaleType.CENTER);
            mNavButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mNavigationListener != null) {
                        mNavigationListener.onClick(v);
                    }
                }
            });
            addSystemView(mNavButtonView);
        }
        return mNavButtonView;
    }

    public void setNavigationIcon(int resId) {
        setNavigationIcon(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public void setNavigationIcon(Drawable icon) {
        if (icon != null) {
            ensureNavButtonView().setImageDrawable(icon);
        } else if (mNavButtonView != null) {
            removeSystemView(mNavButtonView);
            mNavButtonView = null;
        }
    }

    public Drawable getNavigationIcon() {
        return mNavButtonView == null ? null : mNavButtonView.getDrawable();
    }

    public void setNavigationOnClickListener(View.OnClickListener listener) {
        mNavigationListener = listener;
    }

    /// Runtime use: whether the application installed its own navigation
    /// listener.
    public boolean hasNavigationOnClickListener() {
        return mNavigationListener != null;
    }

    public CharSequence getNavigationContentDescription() {
        return mNavButtonView == null ? null : mNavButtonView.getContentDescription();
    }

    public void setNavigationContentDescription(int resId) {
        setNavigationContentDescription(resId == 0 ? null : getContext().getText(resId));
    }

    public void setNavigationContentDescription(CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureNavButtonView().setContentDescription(description);
        } else if (mNavButtonView != null) {
            mNavButtonView.setContentDescription(description);
        }
    }

    public void setLogo(int resId) {
        setLogo(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public void setLogo(Drawable logo) {
        if (logo != null) {
            if (mLogoView == null) {
                mLogoView = new ImageView(getContext());
                mLogoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                addSystemView(mLogoView);
            }
            mLogoView.setImageDrawable(logo);
        } else if (mLogoView != null) {
            removeSystemView(mLogoView);
            mLogoView = null;
        }
    }

    public Drawable getLogo() {
        return mLogoView == null ? null : mLogoView.getDrawable();
    }

    // ------------------------------------------------------------ insets

    public void setContentInsetsRelative(int contentInsetStart, int contentInsetEnd) {
        mContentInsetStart = contentInsetStart;
        mContentInsetEnd = contentInsetEnd;
        requestLayout();
    }

    public void setContentInsetsAbsolute(int contentInsetLeft, int contentInsetRight) {
        setContentInsetsRelative(contentInsetLeft, contentInsetRight);
    }

    public int getContentInsetStart() {
        return mContentInsetStart;
    }

    public int getContentInsetEnd() {
        return mContentInsetEnd;
    }

    public int getContentInsetStartWithNavigation() {
        return mContentInsetStartWithNavigation;
    }

    public void setContentInsetStartWithNavigation(int insetStartWithNavigation) {
        mContentInsetStartWithNavigation = insetStartWithNavigation;
        requestLayout();
    }

    public int getContentInsetEndWithActions() {
        return mContentInsetEndWithActions;
    }

    public void setContentInsetEndWithActions(int insetEndWithActions) {
        mContentInsetEndWithActions = insetEndWithActions;
        requestLayout();
    }

    // ------------------------------------------------------------ menu

    public void setPopupTheme(int resId) {
        mPopupTheme = resId;
    }

    public int getPopupTheme() {
        return mPopupTheme;
    }

    public Menu getMenu() {
        return mPresentedMenu != null ? mPresentedMenu : mMenu;
    }

    public void inflateMenu(int resId) {
        new MenuInflater(getContext()).inflate(resId, mMenu);
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mOnMenuItemClickListener = listener;
    }

    public void setOverflowIcon(Drawable icon) {
        mOverflowIcon = icon;
        if (mOverflowButton != null) {
            mOverflowButton.setImageDrawable(icon);
        }
    }

    public Drawable getOverflowIcon() {
        if (mOverflowIcon == null) {
            mOverflowIcon = getContext().getDrawable(R.drawable.abc_ic_menu_overflow_material);
        }
        return mOverflowIcon;
    }

    public boolean showOverflowMenu() {
        if (mOverflowButton == null || mOverflowItems.isEmpty()) {
            return false;
        }
        Context c = mPopupTheme != 0 ? new ContextThemeWrapper(getContext(), mPopupTheme) : getContext();
        MenuPopup.show(c, new ArrayList<MenuImpl.Item>(mOverflowItems), mOverflowButton, Gravity.END,
                android.R.attr.actionOverflowMenuStyle, 0, null);
        return true;
    }

    public boolean hideOverflowMenu() {
        return false;
    }

    public boolean isOverflowMenuShowing() {
        return false;
    }

    public void dismissPopupMenus() {
    }

    public boolean hasExpandedActionView() {
        return false;
    }

    public void collapseActionView() {
    }

    /// Runtime use: shows an activity's options menu here instead of this
    /// toolbar's own, as `setSupportActionBar` does. Null restores its own.
    public void setPresentedMenu(MenuImpl menu, boolean visible) {
        mPresentedMenu = menu;
        mMenuVisible = visible;
        rebuildMenuViews();
    }

    private void rebuildMenuViews() {
        for (View v : mActionViews) {
            removeSystemView(v);
        }
        mActionViews.clear();
        mOverflowItems.clear();
        if (mOverflowButton != null) {
            removeSystemView(mOverflowButton);
            mOverflowButton = null;
        }
        MenuImpl menu = mPresentedMenu != null ? mPresentedMenu : mMenu;
        if (!mMenuVisible) {
            requestLayout();
            return;
        }
        float dp = getResources().getDisplayMetrics().density;
        float widthDp = getResources().getDisplayMetrics().widthPixels / dp;
        // AppCompat's ActionMenuPresenter allows more actions on wider screens.
        int maxActions = widthDp >= 600 ? 5 : widthDp >= 500 ? 4 : widthDp >= 360 ? 3 : 2;
        List<MenuImpl.Item> items = menu.visibleItems();
        List<MenuImpl.Item> actions = new ArrayList<MenuImpl.Item>();
        for (MenuImpl.Item it : items) {
            if ((it.getShowAsAction() & MenuItem.SHOW_AS_ACTION_ALWAYS) != 0) {
                actions.add(it);
            }
        }
        for (MenuImpl.Item it : items) {
            if ((it.getShowAsAction() & MenuItem.SHOW_AS_ACTION_ALWAYS) == 0
                    && (it.getShowAsAction() & MenuItem.SHOW_AS_ACTION_IF_ROOM) != 0 && actions.size() < maxActions) {
                actions.add(it);
            }
        }
        for (MenuImpl.Item it : items) {
            if (actions.contains(it)) {
                View v = newActionView(it);
                mActionViews.add(v);
                addSystemView(v);
            } else {
                mOverflowItems.add(it);
            }
        }
        if (!mOverflowItems.isEmpty()) {
            int style = AppCompatAttrs.defStyle(getContext(), R.attr.actionOverflowButtonStyle, 0);
            mOverflowButton = new ImageButton(getContext(), null, style,
                    style == 0 ? R.style.Widget_AppCompat_ActionButton_Overflow : 0);
            mOverflowButton.setImageDrawable(getOverflowIcon());
            mOverflowButton.setScaleType(ImageView.ScaleType.CENTER);
            mOverflowButton.setContentDescription("More options");
            mOverflowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showOverflowMenu();
                }
            });
            addSystemView(mOverflowButton);
        }
        requestLayout();
        invalidate();
    }

    /// Runs a menu item from its action button.
    private static final class ItemClick implements View.OnClickListener {
        private final MenuImpl.Item item;

        ItemClick(MenuImpl.Item item) {
            this.item = item;
        }

        @Override
        public void onClick(View v) {
            item.invoke();
        }
    }

    private View newActionView(final MenuImpl.Item item) {
        View.OnClickListener click = new ItemClick(item);
        int style = AppCompatAttrs.defStyle(getContext(), R.attr.actionButtonStyle, 0);
        if (item.getIcon() != null && (item.getShowAsAction() & MenuItem.SHOW_AS_ACTION_WITH_TEXT) == 0) {
            ImageButton b = new ImageButton(getContext(), null, style, style == 0 ? R.style.Widget_AppCompat_ActionButton : 0);
            b.setImageDrawable(item.getIcon());
            b.setScaleType(ImageView.ScaleType.CENTER);
            b.setContentDescription(item.getTitle());
            b.setEnabled(item.isEnabled());
            b.setOnClickListener(click);
            return b;
        }
        TextView t = new TextView(getContext(), null, 0, R.style.Widget_AppCompat_ActionButton);
        t.setText(item.getTitle());
        t.setAllCaps(true);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        TypedArray a = getContext().obtainStyledAttributes(new int[] {android.R.attr.textColorPrimary});
        ColorStateList color = a.getColorStateList(0);
        a.recycle();
        if (color != null) {
            t.setTextColor(color);
        }
        int pad = Math.round(12 * getResources().getDisplayMetrics().density);
        t.setPadding(pad, 0, pad, 0);
        t.setEnabled(item.isEnabled());
        t.setOnClickListener(click);
        return t;
    }

    // ------------------------------------------------------------ children

    private void addSystemView(View v) {
        mSystemViews.add(v);
        addView(v, generateDefaultLayoutParams());
    }

    private void removeSystemView(View v) {
        mSystemViews.remove(v);
        removeView(v);
    }

    private boolean isSystemView(View v) {
        return mSystemViews.contains(v);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((ViewGroup.LayoutParams) p);
        }
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    // ------------------------------------------------------------ measure & layout

    private int buttonHeight(int available) {
        return Math.max(0, Math.min(mMaxButtonHeight, available));
    }

    private static boolean shown(View v) {
        return v != null && v.getVisibility() != View.GONE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float dp = getResources().getDisplayMetrics().density;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int minHeight = getMinimumHeight();
        int padV = getPaddingTop() + getPaddingBottom();
        int buttonH = buttonHeight(Math.max(minHeight - padV, 0) == 0 ? mMaxButtonHeight : Math.max(minHeight - padV, 0));
        int used = getPaddingLeft() + getPaddingRight();
        int contentH = 0;
        if (shown(mNavButtonView)) {
            int w = Math.max(Math.round(56 * dp), mNavButtonView.getMinimumWidth());
            mNavButtonView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(buttonH, MeasureSpec.EXACTLY));
            used += w;
            contentH = Math.max(contentH, buttonH);
        }
        for (View v : mActionViews) {
            int w = Math.round(48 * dp);
            if (v instanceof TextView) {
                v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(buttonH, MeasureSpec.EXACTLY));
                w = Math.max(w, v.getMeasuredWidth());
            }
            v.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(buttonH, MeasureSpec.EXACTLY));
            used += w;
        }
        if (shown(mOverflowButton)) {
            int w = Math.round(36 * dp);
            mOverflowButton.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(buttonH, MeasureSpec.EXACTLY));
            used += w;
        }
        int start = shown(mNavButtonView)
                ? Math.max(mContentInsetStartWithNavigation, getPaddingLeft() + mNavButtonView.getMeasuredWidth())
                : Math.max(mContentInsetStart, getPaddingLeft());
        int endUsed = used - getPaddingLeft() - (shown(mNavButtonView) ? mNavButtonView.getMeasuredWidth() : 0);
        int available = widthMode == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE / 2
                : Math.max(0, widthSize - start - endUsed - mContentInsetEnd);
        if (shown(mLogoView)) {
            mLogoView.measure(MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(buttonH, MeasureSpec.AT_MOST));
            available = Math.max(0, available - mLogoView.getMeasuredWidth());
        }
        int titleW = 0;
        int titleBlockH = 0;
        int titleSpace = Math.max(0, available - mTitleMarginEnd);
        if (shown(mTitleTextView)) {
            mTitleTextView.measure(MeasureSpec.makeMeasureSpec(titleSpace, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            titleW = mTitleTextView.getMeasuredWidth();
            titleBlockH += mTitleTextView.getMeasuredHeight();
        }
        if (shown(mSubtitleTextView)) {
            mSubtitleTextView.measure(MeasureSpec.makeMeasureSpec(titleSpace, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            titleW = Math.max(titleW, mSubtitleTextView.getMeasuredWidth());
            titleBlockH += mSubtitleTextView.getMeasuredHeight();
        }
        contentH = Math.max(contentH, titleBlockH + (titleBlockH > 0 ? mTitleMarginTop + mTitleMarginBottom : 0));
        int remaining = Math.max(0, available - (titleW > 0 ? titleW + mTitleMarginEnd : 0));
        int customW = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (isSystemView(c) || c.getVisibility() == View.GONE) {
                continue;
            }
            ViewGroup.LayoutParams lp = c.getLayoutParams();
            int ml = 0;
            int mr = 0;
            int mt = 0;
            int mb = 0;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
                ml = m.leftMargin;
                mr = m.rightMargin;
                mt = m.topMargin;
                mb = m.bottomMargin;
            }
            int hSpec = lp.height == ViewGroup.LayoutParams.MATCH_PARENT && contentH > 0
                    ? MeasureSpec.makeMeasureSpec(Math.max(0, Math.max(minHeight - padV, contentH) - mt - mb), MeasureSpec.EXACTLY)
                    : getChildMeasureSpec(heightMeasureSpec, padV + mt + mb, lp.height);
            c.measure(getChildMeasureSpec(MeasureSpec.makeMeasureSpec(remaining, MeasureSpec.AT_MOST), ml + mr, lp.width),
                    hSpec);
            customW += c.getMeasuredWidth() + ml + mr;
            contentH = Math.max(contentH, c.getMeasuredHeight() + mt + mb);
        }
        int w = widthMode == MeasureSpec.EXACTLY ? widthSize
                : Math.min(widthMode == MeasureSpec.AT_MOST ? widthSize : Integer.MAX_VALUE,
                        start + (titleW > 0 ? titleW + mTitleMarginEnd : 0) + customW + endUsed + getPaddingRight());
        int h = Math.max(minHeight, contentH + padV);
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        int width = r - l;
        int height = b - t;
        int top = getPaddingTop();
        int bottom = height - getPaddingBottom();
        int left = getPaddingLeft();
        int right = width - getPaddingRight();
        if (shown(mNavButtonView)) {
            place(mNavButtonView, left, top, rtl, width);
            left += mNavButtonView.getMeasuredWidth();
        }
        if (shown(mOverflowButton)) {
            right -= mOverflowButton.getMeasuredWidth();
            place(mOverflowButton, right, top, rtl, width);
        }
        for (int i = mActionViews.size() - 1; i >= 0; i--) {
            View v = mActionViews.get(i);
            right -= v.getMeasuredWidth();
            place(v, right, top, rtl, width);
        }
        // The title collapses its start margin into the content inset, as
        // AppCompat's Toolbar does.
        int insetStart = shown(mNavButtonView) ? mContentInsetStartWithNavigation : mContentInsetStart;
        int contentLeft = Math.max(left, insetStart);
        int insetEnd = (mActionViews.isEmpty() && mOverflowButton == null) ? mContentInsetEnd
                : Math.max(mContentInsetEnd, mContentInsetEndWithActions);
        int contentRight = Math.min(right, width - insetEnd);
        if (shown(mLogoView)) {
            int lh = mLogoView.getMeasuredHeight();
            int lt = top + (bottom - top - lh) / 2;
            place(mLogoView, contentLeft, lt, rtl, width);
            contentLeft += mLogoView.getMeasuredWidth();
        }
        boolean hasTitle = shown(mTitleTextView);
        boolean hasSubtitle = shown(mSubtitleTextView);
        if (hasTitle || hasSubtitle) {
            int titleLeft = Math.max(contentLeft, left + mTitleMarginStart);
            int blockH = (hasTitle ? mTitleTextView.getMeasuredHeight() : 0)
                    + (hasSubtitle ? mSubtitleTextView.getMeasuredHeight() : 0);
            int y;
            int vg = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
            if (vg == Gravity.TOP) {
                y = top + mTitleMarginTop;
            } else if (vg == Gravity.BOTTOM) {
                y = bottom - mTitleMarginBottom - blockH;
            } else {
                y = top + (bottom - top - blockH) / 2;
            }
            int titleRight = titleLeft;
            if (hasTitle) {
                place(mTitleTextView, titleLeft, y, rtl, width);
                y += mTitleTextView.getMeasuredHeight();
                titleRight = Math.max(titleRight, titleLeft + mTitleTextView.getMeasuredWidth());
            }
            if (hasSubtitle) {
                place(mSubtitleTextView, titleLeft, y, rtl, width);
                titleRight = Math.max(titleRight, titleLeft + mSubtitleTextView.getMeasuredWidth());
            }
            contentLeft = titleRight + mTitleMarginEnd;
        }
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (isSystemView(c) || c.getVisibility() == View.GONE) {
                continue;
            }
            int cw = c.getMeasuredWidth();
            int ch = c.getMeasuredHeight();
            int gravity = Gravity.NO_GRAVITY;
            int ml = 0;
            int mr = 0;
            ViewGroup.LayoutParams lp = c.getLayoutParams();
            if (lp instanceof LayoutParams) {
                gravity = ((LayoutParams) lp).gravity;
            }
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ml = ((ViewGroup.MarginLayoutParams) lp).leftMargin;
                mr = ((ViewGroup.MarginLayoutParams) lp).rightMargin;
            }
            int hg = Gravity.getAbsoluteGravity(gravity == Gravity.NO_GRAVITY ? Gravity.START : gravity,
                    View.LAYOUT_DIRECTION_LTR) & Gravity.HORIZONTAL_GRAVITY_MASK;
            int x;
            if (hg == Gravity.CENTER_HORIZONTAL) {
                x = Math.max(contentLeft, (width - cw) / 2);
            } else if (hg == Gravity.RIGHT) {
                x = contentRight - mr - cw;
                contentRight = x - ml;
            } else {
                x = contentLeft + ml;
                contentLeft = x + cw + mr;
            }
            int vg = gravity & Gravity.VERTICAL_GRAVITY_MASK;
            int y = vg == Gravity.TOP ? top : vg == Gravity.BOTTOM ? bottom - ch : top + (bottom - top - ch) / 2;
            place(c, x, y, rtl, width);
        }
    }

    private static void place(View v, int x, int y, boolean rtl, int width) {
        int w = v.getMeasuredWidth();
        int left = rtl ? width - x - w : x;
        v.layout(left, y, left + w, y + v.getMeasuredHeight());
    }
}
