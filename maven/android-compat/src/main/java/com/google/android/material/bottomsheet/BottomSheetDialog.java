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
package com.google.android.material.bottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/// A modal bottom sheet: the content slides up from the bottom edge over a
/// dimmed scrim, shows its peek height first when it is taller than that,
/// and is dragged up to expand or down to dismiss. Tapping the scrim or the
/// back key cancels it.
public class BottomSheetDialog extends Dialog {

    private static final long ANIMATION_MS = 250;

    private final BottomSheetBehavior<FrameLayout> mBehavior = new BottomSheetBehavior<FrameLayout>();
    private FrameLayout mScrim;
    private SheetFrame mSheet;
    private PopupWindow mPopup;
    private boolean mSheetShowing;
    private boolean mDismissing;
    private boolean mCancelableSheet = true;
    private boolean mCancelOnTouchOutside = true;
    private boolean mDismissWithAnimation;
    private OnCancelListener mCancel;
    private OnDismissListener mDismiss;
    private OnShowListener mShow;
    private int mScrimColor;

    public BottomSheetDialog(Context context) {
        this(context, 0);
    }

    public BottomSheetDialog(Context context, int theme) {
        super(context, theme != 0 ? theme : sheetTheme(context));
        build();
    }

    protected BottomSheetDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        this(context, 0);
        setCancelable(cancelable);
        setOnCancelListener(cancelListener);
    }

    private static int sheetTheme(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.bottomSheetDialogTheme, tv, true) && tv.resourceId != 0) {
            return tv.resourceId;
        }
        return MaterialAttrs.isMaterial3(context) ? R.style.ThemeOverlay_Material3_BottomSheetDialog
                : R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog;
    }

    private void build() {
        Context c = getContext();
        boolean m3 = MaterialAttrs.isMaterial3(c);
        // The window dim Material sets for its sheets: 32% black under
        // Material 3, the framework's 60% under Material Components.
        mScrimColor = m3 ? 0x52000000 : 0x99000000;
        mScrim = new FrameLayout(c);
        mScrim.setBackground(new ColorDrawable(mScrimColor));
        mScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCancelableSheet && mCancelOnTouchOutside && isShowing()) {
                    cancel();
                }
            }
        });
        mSheet = new SheetFrame(c);
        mSheet.setId(R.id.design_bottom_sheet);
        mSheet.setClickable(true);
        int styleRes = 0;
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(R.attr.bottomSheetStyle, tv, true) && tv.resourceId != 0) {
            styleRes = tv.resourceId;
        }
        if (styleRes == 0) {
            styleRes = m3 ? R.style.Widget_Material3_BottomSheet_Modal : R.style.Widget_MaterialComponents_BottomSheet_Modal;
        }
        TypedArray a = c.obtainStyledAttributes(null, R.styleable.BottomSheetBehavior_Layout, 0, styleRes);
        ColorStateList tint;
        int shapeRes;
        try {
            tint = a.getColorStateList(R.styleable.BottomSheetBehavior_Layout_backgroundTint);
            shapeRes = a.getResourceId(R.styleable.BottomSheetBehavior_Layout_shapeAppearance, 0);
            TypedValue peek = new TypedValue();
            if (a.getValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, peek)
                    && peek.type == TypedValue.TYPE_DIMENSION) {
                mBehavior.setPeekHeight(a.getDimensionPixelSize(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight,
                        BottomSheetBehavior.PEEK_HEIGHT_AUTO));
            }
            mBehavior.setHideable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, true));
            mBehavior.setSkipCollapsed(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));
            mBehavior.setDraggable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_draggable, true));
        } finally {
            a.recycle();
        }
        if (tint == null) {
            tint = ColorStateList.valueOf(MaterialColors.getColor(c, m3 ? R.attr.colorSurfaceContainerLow
                    : R.attr.colorSurface, 0xffffffff));
        }
        ShapeAppearanceModel.Builder shape = shapeRes != 0 ? ShapeAppearanceModel.builder(c, shapeRes, 0)
                : ShapeAppearanceModel.builder();
        // A modal sheet rounds only its top corners.
        ShapeAppearanceModel full = shape.build();
        android.graphics.RectF probe = new android.graphics.RectF(0, 0, 10000, 10000);
        ShapeAppearanceModel top = full.toBuilder()
                .setTopLeftCornerSize(full.getTopLeftCornerSize(probe))
                .setTopRightCornerSize(full.getTopRightCornerSize(probe))
                .setBottomLeftCornerSize(0).setBottomRightCornerSize(0).build();
        MaterialShapeDrawable bg = new MaterialShapeDrawable(top);
        bg.setFillColor(tint);
        bg.setElevation(MaterialAttrs.dp(c, 1));
        mSheet.setBackground(bg);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        mScrim.addView(mSheet, lp);
        mBehavior.attach(mSheet, new BottomSheetBehavior.Host() {
            @Override
            public void settleTo(int state) {
                settle(state);
            }
        });
    }

    public BottomSheetBehavior<FrameLayout> getBehavior() {
        return mBehavior;
    }

    public void setDismissWithAnimation(boolean dismissWithAnimation) {
        mDismissWithAnimation = dismissWithAnimation;
    }

    public boolean getDismissWithAnimation() {
        return mDismissWithAnimation;
    }

    // ------------------------------------------------------------ content

    @Override
    public void setContentView(int layoutResID) {
        mSheet.removeAllViews();
        getLayoutInflater().inflate(layoutResID, mSheet, true);
    }

    @Override
    public void setContentView(View view) {
        setContentView(view, null);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mSheet.removeAllViews();
        if (params == null) {
            mSheet.addView(view);
        } else {
            mSheet.addView(view, params);
        }
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        mSheet.addView(view, params);
    }

    @Override
    public <T extends View> T findViewById(int id) {
        return mScrim.findViewById(id);
    }

    // ------------------------------------------------------------ cancel and dismiss state

    @Override
    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
        mCancelableSheet = cancelable;
    }

    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
        if (cancel && !mCancelableSheet) {
            mCancelableSheet = true;
        }
        mCancelOnTouchOutside = cancel;
    }

    @Override
    public void setOnCancelListener(OnCancelListener listener) {
        super.setOnCancelListener(listener);
        mCancel = listener;
    }

    @Override
    public void setOnDismissListener(OnDismissListener listener) {
        super.setOnDismissListener(listener);
        mDismiss = listener;
    }

    @Override
    public void setOnShowListener(OnShowListener listener) {
        super.setOnShowListener(listener);
        mShow = listener;
    }

    @Override
    public boolean isShowing() {
        return mSheetShowing;
    }

    // ------------------------------------------------------------ showing

    @Override
    public void show() {
        if (mSheetShowing) {
            if (mPopup != null && mScrim.getVisibility() != View.VISIBLE) {
                mScrim.setVisibility(View.VISIBLE);
            }
            return;
        }
        create();
        onStart();
        mPopup = new PopupWindow(mScrim, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopup.setBackgroundDrawable(null);
        mPopup.setClippingEnabled(false);
        mPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // The back key dismissed the popup underneath us.
                if (mSheetShowing && !mDismissing) {
                    mPopup = null;
                    if (mCancel != null) {
                        mCancel.onCancel(BottomSheetDialog.this);
                    }
                    finishDismiss();
                }
            }
        });
        mSheetShowing = true;
        mDismissing = false;
        mScrim.setVisibility(View.VISIBLE);
        mScrim.getBackground().setAlpha(0);
        mSheet.setTranslationY(screenHeight());
        mPopup.showAtLocation(mScrim, Gravity.TOP | Gravity.START, 0, 0);
        mBehavior.dispatchState(BottomSheetBehavior.STATE_SETTLING);
        mScrim.post(new Runnable() {
            @Override
            public void run() {
                int target = mSheet.getHeight() > peekHeight() && !mBehavior.getSkipCollapsed()
                        ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED;
                settle(target);
            }
        });
        if (mShow != null) {
            mShow.onShow(this);
        }
    }

    @Override
    public void hide() {
        mScrim.setVisibility(View.INVISIBLE);
    }

    @Override
    public void cancel() {
        if (!mSheetShowing) {
            return;
        }
        if (mCancel != null) {
            mCancel.onCancel(this);
        }
        dismiss();
    }

    @Override
    public void dismiss() {
        if (!mSheetShowing || mDismissing) {
            return;
        }
        mDismissing = true;
        animateTo(mSheet.getHeight(), new Runnable() {
            @Override
            public void run() {
                PopupWindow p = mPopup;
                mPopup = null;
                if (p != null) {
                    p.dismiss();
                }
                finishDismiss();
            }
        });
    }

    private void finishDismiss() {
        mSheetShowing = false;
        mDismissing = false;
        mBehavior.dispatchState(BottomSheetBehavior.STATE_HIDDEN);
        onStop();
        if (mDismiss != null) {
            mDismiss.onDismiss(this);
        }
    }

    // ------------------------------------------------------------ geometry and motion

    private int screenHeight() {
        return getContext().getResources().getDisplayMetrics().heightPixels;
    }

    /// The peek height, resolving `auto` as Material does: the larger of
    /// 64dp and the screen height less a 16:9 keyline of its width.
    int peekHeight() {
        int peek = mBehavior.getPeekHeight();
        if (peek != BottomSheetBehavior.PEEK_HEIGHT_AUTO) {
            return peek;
        }
        android.util.DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        return Math.max(MaterialAttrs.dpi(getContext(), 64), dm.heightPixels - dm.widthPixels * 9 / 16);
    }

    private float offsetFor(int state) {
        int h = mSheet.getHeight();
        switch (state) {
            case BottomSheetBehavior.STATE_HIDDEN:
                return h;
            case BottomSheetBehavior.STATE_COLLAPSED:
                return Math.max(0, h - peekHeight());
            default:
                return 0;
        }
    }

    void settle(final int state) {
        if (!mSheetShowing) {
            return;
        }
        if (state == BottomSheetBehavior.STATE_HIDDEN) {
            cancelBySheet();
            return;
        }
        mBehavior.dispatchState(BottomSheetBehavior.STATE_SETTLING);
        animateTo(offsetFor(state), new Runnable() {
            @Override
            public void run() {
                mBehavior.dispatchState(state);
            }
        });
    }

    private void cancelBySheet() {
        cancel();
    }

    private void animateTo(float translation, Runnable end) {
        mSheet.animate().cancel();
        final float from = mSheet.getTranslationY();
        mSheet.animate().translationY(translation).setDuration(ANIMATION_MS)
                .setUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                        onSheetMoved();
                    }
                }).withEndAction(end).start();
        if (Float.compare(from, translation) == 0) {
            onSheetMoved();
        }
    }

    /// Fades the scrim with the sheet and reports the slide offset: 0 at the
    /// peek height, 1 expanded, -1 hidden.
    void onSheetMoved() {
        int h = Math.max(1, mSheet.getHeight());
        float t = mSheet.getTranslationY();
        float visible = Math.max(0f, Math.min(1f, 1f - t / h));
        if (mScrim.getBackground() != null) {
            mScrim.getBackground().setAlpha(Math.round(255 * visible));
            mScrim.invalidate();
        }
        float collapsed = offsetFor(BottomSheetBehavior.STATE_COLLAPSED);
        float slide;
        if (t <= collapsed) {
            slide = collapsed <= 0 ? 1f : 1f - t / collapsed;
        } else {
            slide = -(t - collapsed) / Math.max(1f, h - collapsed);
        }
        mBehavior.dispatchSlide(slide);
    }

    /// The sheet: hosts the content, intercepts vertical drags, and owns the
    /// behavior [BottomSheetBehavior#from] returns.
    final class SheetFrame extends FrameLayout {
        private float mDownY;
        private float mStartTranslation;
        private boolean mDragging;

        SheetFrame(Context c) {
            super(c);
        }

        BottomSheetBehavior<FrameLayout> behavior() {
            return mBehavior;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Material caps a sheet at 640dp wide and keeps it below the
            // status bar.
            int max = MaterialAttrs.dpi(getContext(), 640);
            int w = MeasureSpec.getSize(widthMeasureSpec);
            if (w > max) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY);
            }
            int h = MeasureSpec.getSize(heightMeasureSpec);
            int top = MaterialAttrs.dpi(getContext(), 24);
            if (h > top) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(h - top, MeasureSpec.AT_MOST);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (!mBehavior.isDraggable()) {
                return false;
            }
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mDownY = ev.getRawY();
                    mStartTranslation = getTranslationY();
                    mDragging = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(ev.getRawY() - mDownY) > MaterialAttrs.dp(getContext(), 8)) {
                        mDragging = true;
                        mBehavior.dispatchState(BottomSheetBehavior.STATE_DRAGGING);
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mDownY = ev.getRawY();
                    mStartTranslation = getTranslationY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!mBehavior.isDraggable()) {
                        return true;
                    }
                    if (!mDragging && Math.abs(ev.getRawY() - mDownY) > MaterialAttrs.dp(getContext(), 8)) {
                        mDragging = true;
                        mBehavior.dispatchState(BottomSheetBehavior.STATE_DRAGGING);
                    }
                    if (mDragging) {
                        float t = Math.max(0f, mStartTranslation + ev.getRawY() - mDownY);
                        setTranslationY(t);
                        onSheetMoved();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mDragging) {
                        mDragging = false;
                        release(getTranslationY() - mStartTranslation);
                    }
                    return true;
                default:
                    return super.onTouchEvent(ev);
            }
        }

        /// Settles a released drag at the nearest state, hiding when it was
        /// pulled down past half the visible part of a hideable sheet.
        private void release(float moved) {
            float t = getTranslationY();
            float collapsed = offsetFor(BottomSheetBehavior.STATE_COLLAPSED);
            int h = getHeight();
            if (mBehavior.isHideable() && moved > 0 && t > collapsed + (h - collapsed) / 2f) {
                settle(BottomSheetBehavior.STATE_HIDDEN);
            } else if (mBehavior.isHideable() && moved > 0 && collapsed <= 0 && t > h / 3f) {
                settle(BottomSheetBehavior.STATE_HIDDEN);
            } else if (mBehavior.getSkipCollapsed() || collapsed <= 0 || t < collapsed / 2f) {
                settle(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                settle(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }
}
