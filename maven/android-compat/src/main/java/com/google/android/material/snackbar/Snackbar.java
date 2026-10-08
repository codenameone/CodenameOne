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
package com.google.android.material.snackbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/// A brief message at the bottom of the screen with an optional action:
/// Material's snackbar, styled by the theme's `snackbarStyle`,
/// `snackbarTextViewStyle` and `snackbarButtonStyle`.
public class Snackbar extends BaseTransientBottomBar<Snackbar> {

    /// The callback of the older API, with the same dismiss events.
    public static class Callback extends BaseCallback<Snackbar> {
        public static final int DISMISS_EVENT_SWIPE = BaseCallback.DISMISS_EVENT_SWIPE;
        public static final int DISMISS_EVENT_ACTION = BaseCallback.DISMISS_EVENT_ACTION;
        public static final int DISMISS_EVENT_TIMEOUT = BaseCallback.DISMISS_EVENT_TIMEOUT;
        public static final int DISMISS_EVENT_MANUAL = BaseCallback.DISMISS_EVENT_MANUAL;
        public static final int DISMISS_EVENT_CONSECUTIVE = BaseCallback.DISMISS_EVENT_CONSECUTIVE;
    }

    /// The bar's root view.
    public static final class SnackbarLayout extends FrameLayout {
        private final int maxWidth;

        SnackbarLayout(Context context, int maxWidth) {
            super(context);
            this.maxWidth = maxWidth;
            setClickable(true);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (maxWidth > 0 && MeasureSpec.getSize(widthMeasureSpec) > maxWidth) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.getMode(widthMeasureSpec));
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected boolean drawsOutsideBounds() {
            return getBackground() instanceof MaterialShapeDrawable
                    && ((MaterialShapeDrawable) getBackground()).getElevation() > 0;
        }
    }

    private final TextView mMessage;
    private final Button mAction;
    private final MaterialShapeDrawable mSurface;

    private Snackbar(ViewGroup parent, SnackbarLayout layout, TextView message, Button action,
                     MaterialShapeDrawable surface) {
        super(parent, layout);
        mMessage = message;
        mAction = action;
        mSurface = surface;
    }

    public static Snackbar make(View view, CharSequence text, int duration) {
        return make(view.getContext(), view, text, duration);
    }

    public static Snackbar make(View view, int resId, int duration) {
        return make(view, view.getResources().getText(resId), duration);
    }

    public static Snackbar make(Context context, View view, CharSequence text, int duration) {
        ViewGroup parent = findSuitableParent(view);
        if (parent == null) {
            throw new IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view.");
        }
        Context c = parent.getContext();
        boolean m3 = MaterialAttrs.isMaterial3(c);
        int barAttr = MaterialAttrs.defStyleAttr(c, R.attr.snackbarStyle);
        TypedArray a = c.obtainStyledAttributes((AttributeSet) null, R.styleable.SnackbarLayout, barAttr,
                barAttr == 0 ? (m3 ? R.style.Widget_Material3_Snackbar : R.style.Widget_MaterialComponents_Snackbar) : 0);
        SnackbarLayout layout;
        MaterialShapeDrawable surface;
        try {
            int shapeRes = a.getResourceId(R.styleable.SnackbarLayout_shapeAppearance, 0);
            ShapeAppearanceModel shape = shapeRes != 0 ? ShapeAppearanceModel.builder(c, shapeRes, 0).build()
                    : ShapeAppearanceModel.builder().setAllCornerSizes(MaterialAttrs.dp(c, 4)).build();
            surface = new MaterialShapeDrawable(shape);
            ColorStateList bg = a.getColorStateList(R.styleable.SnackbarLayout_backgroundTint);
            surface.setFillColor(bg != null ? bg
                    : ColorStateList.valueOf(MaterialColors.getColor(c, R.attr.colorSurfaceInverse, 0xff322f35)));
            surface.setElevation(a.getDimension(R.styleable.SnackbarLayout_elevation, MaterialAttrs.dp(c, 6)));
            layout = new SnackbarLayout(c, a.getDimensionPixelSize(R.styleable.SnackbarLayout_android_maxWidth, 0));
        } finally {
            a.recycle();
        }
        layout.setBackground(surface);
        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setMinimumHeight(MaterialAttrs.dpi(c, 48));
        int textAttr = MaterialAttrs.defStyleAttr(c, R.attr.snackbarTextViewStyle);
        TextView message = textAttr != 0 ? new TextView(c, null, textAttr)
                : new TextView(c, null, 0, m3 ? R.style.Widget_Material3_Snackbar_TextView
                        : R.style.Widget_MaterialComponents_Snackbar_TextView);
        message.setId(R.id.snackbar_text);
        message.setText(text);
        message.setMaxLines(2);
        message.setEllipsize(TextUtils.TruncateAt.END);
        int pad = MaterialAttrs.dpi(c, 16);
        message.setPaddingRelative(pad, MaterialAttrs.dpi(c, 14), pad, MaterialAttrs.dpi(c, 14));
        content.addView(message, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int buttonAttr = MaterialAttrs.defStyleAttr(c, R.attr.snackbarButtonStyle);
        MaterialButton action = new MaterialButton(c, null, buttonAttr);
        action.setId(R.id.snackbar_action);
        action.setVisibility(View.GONE);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.setMarginEnd(MaterialAttrs.dpi(c, 8));
        content.addView(action, alp);
        layout.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Snackbar s = new Snackbar(parent, layout, message, action, surface);
        s.setDuration(duration);
        return s;
    }

    /// The nearest coordinator layout, else the activity's content frame,
    /// else the outermost frame above `view`, as Material Components picks.
    private static ViewGroup findSuitableParent(View view) {
        ViewGroup fallback = null;
        View v = view;
        while (v != null) {
            if (v instanceof ViewGroup && v.getClass().getName().endsWith(".CoordinatorLayout")) {
                return (ViewGroup) v;
            }
            if (v instanceof FrameLayout) {
                if (v.getId() == android.R.id.content) {
                    return (ViewGroup) v;
                }
                fallback = (ViewGroup) v;
            }
            Object p = v.getParent();
            v = p instanceof View ? (View) p : null;
        }
        return fallback;
    }

    public Snackbar setText(CharSequence message) {
        mMessage.setText(message);
        return this;
    }

    public Snackbar setText(int resId) {
        return setText(getContext().getText(resId));
    }

    public Snackbar setAction(int resId, View.OnClickListener listener) {
        return setAction(getContext().getText(resId), listener);
    }

    /// Shows an action button; tapping it runs `listener` and dismisses the bar.
    public Snackbar setAction(CharSequence text, final View.OnClickListener listener) {
        if (TextUtils.isEmpty(text) || listener == null) {
            mAction.setVisibility(View.GONE);
            mAction.setOnClickListener(null);
        } else {
            mAction.setVisibility(View.VISIBLE);
            mAction.setText(text);
            mAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(v);
                    dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION);
                }
            });
        }
        return this;
    }

    public Snackbar setActionTextColor(ColorStateList colors) {
        mAction.setTextColor(colors);
        return this;
    }

    public Snackbar setActionTextColor(int color) {
        mAction.setTextColor(color);
        return this;
    }

    public Snackbar setTextColor(ColorStateList colors) {
        mMessage.setTextColor(colors);
        return this;
    }

    public Snackbar setTextColor(int color) {
        mMessage.setTextColor(color);
        return this;
    }

    public Snackbar setTextMaxLines(int maxLines) {
        mMessage.setMaxLines(maxLines);
        return this;
    }

    public Snackbar setBackgroundTint(int color) {
        mSurface.setFillColor(ColorStateList.valueOf(color));
        getView().invalidate();
        return this;
    }

    public Snackbar setBackgroundTintList(ColorStateList colors) {
        mSurface.setFillColor(colors);
        getView().invalidate();
        return this;
    }

    public Snackbar setMaxInlineActionWidth(int width) {
        return this;
    }

    /// The listener set by the previous call is replaced, as the older API did.
    public Snackbar setCallback(Callback callback) {
        if (mCallback != null) {
            removeCallback(mCallback);
        }
        mCallback = callback;
        if (callback != null) {
            addCallback(callback);
        }
        return this;
    }

    private Callback mCallback;
}
