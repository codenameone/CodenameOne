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
package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.runtime.PopupHost;
import com.codename1.ui.geom.Rectangle;

/// A floating window holding a content view, shown at a position on the
/// screen or dropped down from an anchor view. It is an overlay on the
/// current form: nothing behind it is dimmed, and when it is focusable a tap
/// outside it or the back key dismisses it.
public class PopupWindow {

    public static final int INPUT_METHOD_FROM_FOCUSABLE = 0;
    public static final int INPUT_METHOD_NEEDED = 1;
    public static final int INPUT_METHOD_NOT_NEEDED = 2;

    private static final int DEFAULT_ANCHORED_GRAVITY = Gravity.TOP | Gravity.START;

    public interface OnDismissListener {
        void onDismiss();
    }

    private Context mContext;
    private View mContentView;
    private int mWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
    private boolean mFocusable;
    private boolean mTouchable = true;
    private boolean mOutsideTouchable;
    private boolean mClippingEnabled = true;
    private boolean mOverlapAnchor;
    private boolean mAboveAnchor;
    private boolean mIsShowing;
    private Drawable mBackground;
    private float mElevation;
    private int mAnimationStyle = -1;
    private int mInputMethodMode = INPUT_METHOD_FROM_FOCUSABLE;
    private int mSoftInputMode;
    private OnDismissListener mOnDismissListener;
    private View.OnTouchListener mTouchInterceptor;

    private View mAnchor;
    private int mAnchorXoff;
    private int mAnchorYoff;
    private int mAnchoredGravity = DEFAULT_ANCHORED_GRAVITY;
    private int mLocationGravity;
    private int mLocationX;
    private int mLocationY;

    private PopupDecorView mDecor;
    private PopupHost mHost;
    private final Rect mTempRect = new Rect();

    public PopupWindow(Context context) {
        this(context, null);
    }

    public PopupWindow(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.popupWindowStyle);
    }

    public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.PopupWindow, defStyleAttr, defStyleRes);
        mBackground = a.getDrawable(android.R.styleable.PopupWindow_popupBackground);
        mElevation = a.getDimension(android.R.styleable.PopupWindow_popupElevation, 0);
        mOverlapAnchor = a.getBoolean(android.R.styleable.PopupWindow_overlapAnchor, false);
        mAnimationStyle = a.getResourceId(android.R.styleable.PopupWindow_popupAnimationStyle, -1);
        a.recycle();
    }

    public PopupWindow() {
        this(null, 0, 0);
    }

    public PopupWindow(View contentView) {
        this(contentView, 0, 0);
    }

    public PopupWindow(int width, int height) {
        this(null, width, height);
    }

    public PopupWindow(View contentView, int width, int height) {
        this(contentView, width, height, false);
    }

    public PopupWindow(View contentView, int width, int height, boolean focusable) {
        if (contentView != null) {
            mContext = contentView.getContext();
        }
        setContentView(contentView);
        setWidth(width);
        setHeight(height);
        setFocusable(focusable);
    }

    // ------------------------------------------------------------ properties

    public View getContentView() {
        return mContentView;
    }

    public void setContentView(View contentView) {
        if (isShowing()) {
            return;
        }
        mContentView = contentView;
        if (mContext == null && contentView != null) {
            mContext = contentView.getContext();
        }
    }

    public Drawable getBackground() {
        return mBackground;
    }

    public void setBackgroundDrawable(Drawable background) {
        mBackground = background;
    }

    public float getElevation() {
        return mElevation;
    }

    public void setElevation(float elevation) {
        mElevation = elevation;
    }

    public int getAnimationStyle() {
        return mAnimationStyle;
    }

    /// Recorded; popups appear and disappear without a window animation.
    public void setAnimationStyle(int animationStyle) {
        mAnimationStyle = animationStyle;
    }

    public boolean isFocusable() {
        return mFocusable;
    }

    public void setFocusable(boolean focusable) {
        mFocusable = focusable;
    }

    public int getInputMethodMode() {
        return mInputMethodMode;
    }

    public void setInputMethodMode(int mode) {
        mInputMethodMode = mode;
    }

    public int getSoftInputMode() {
        return mSoftInputMode;
    }

    public void setSoftInputMode(int mode) {
        mSoftInputMode = mode;
    }

    public boolean isTouchable() {
        return mTouchable;
    }

    public void setTouchable(boolean touchable) {
        mTouchable = touchable;
    }

    public boolean isOutsideTouchable() {
        return mOutsideTouchable;
    }

    public void setOutsideTouchable(boolean touchable) {
        mOutsideTouchable = touchable;
    }

    public boolean isClippingEnabled() {
        return mClippingEnabled;
    }

    public void setClippingEnabled(boolean enabled) {
        mClippingEnabled = enabled;
    }

    public boolean getOverlapAnchor() {
        return mOverlapAnchor;
    }

    public void setOverlapAnchor(boolean overlapAnchor) {
        mOverlapAnchor = overlapAnchor;
    }

    public boolean isSplitTouchEnabled() {
        return false;
    }

    public void setSplitTouchEnabled(boolean enabled) {
    }

    public boolean isAttachedInDecor() {
        return true;
    }

    public void setAttachedInDecor(boolean enabled) {
    }

    public void setIgnoreCheekPress() {
    }

    public void setWindowLayoutType(int layoutType) {
    }

    public int getWindowLayoutType() {
        return 1000;
    }

    public void setTouchInterceptor(View.OnTouchListener l) {
        mTouchInterceptor = l;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    public boolean isAboveAnchor() {
        return mAboveAnchor;
    }

    // ------------------------------------------------------------ showing

    public void showAtLocation(View parent, int gravity, int x, int y) {
        if (isShowing() || mContentView == null) {
            return;
        }
        mAnchor = null;
        mLocationGravity = gravity;
        mLocationX = x;
        mLocationY = y;
        attach();
        placeAtLocation();
    }

    public void showAsDropDown(View anchor) {
        showAsDropDown(anchor, 0, 0);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff) {
        showAsDropDown(anchor, xoff, yoff, DEFAULT_ANCHORED_GRAVITY);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        if (isShowing() || mContentView == null || anchor == null) {
            return;
        }
        mAnchor = anchor;
        mAnchorXoff = xoff;
        mAnchorYoff = yoff;
        mAnchoredGravity = gravity;
        attach();
        placeAsDropDown();
    }

    public void update() {
        reposition();
    }

    public void update(int width, int height) {
        mWidth = width;
        mHeight = height;
        reposition();
    }

    public void update(int x, int y, int width, int height) {
        update(x, y, width, height, false);
    }

    public void update(int x, int y, int width, int height, boolean force) {
        if (width >= 0) {
            mWidth = width;
        }
        if (height >= 0) {
            mHeight = height;
        }
        if (mAnchor == null && (x != -1 || y != -1)) {
            mLocationX = x;
            mLocationY = y;
        }
        reposition();
    }

    public void update(View anchor, int width, int height) {
        update(anchor, mAnchorXoff, mAnchorYoff, width, height);
    }

    public void update(View anchor, int xoff, int yoff, int width, int height) {
        if (anchor != null) {
            mAnchor = anchor;
            mAnchorXoff = xoff;
            mAnchorYoff = yoff;
        }
        if (width >= 0) {
            mWidth = width;
        }
        if (height >= 0) {
            mHeight = height;
        }
        reposition();
    }

    private void reposition() {
        if (!mIsShowing) {
            return;
        }
        if (mAnchor != null) {
            placeAsDropDown();
        } else {
            placeAtLocation();
        }
    }

    /// The height available below or above the anchor, whichever is larger.
    public int getMaxAvailableHeight(View anchor) {
        return getMaxAvailableHeight(anchor, 0);
    }

    public int getMaxAvailableHeight(View anchor, int yOffset) {
        return getMaxAvailableHeight(anchor, yOffset, false);
    }

    public int getMaxAvailableHeight(View anchor, int yOffset, boolean ignoreBottomDecorations) {
        Rectangle frame = PopupHost.displayFrame();
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int frameTop = frame.getY();
        int frameBottom = frame.getY() + frame.getHeight();
        int distanceToBottom = frameBottom - (mOverlapAnchor ? loc[1] : loc[1] + anchor.getHeight()) - yOffset;
        int distanceToTop = loc[1] - frameTop + yOffset;
        int returnedHeight = Math.max(distanceToBottom, distanceToTop);
        if (mBackground != null) {
            mBackground.getPadding(mTempRect);
            returnedHeight -= mTempRect.top + mTempRect.bottom;
        }
        return returnedHeight;
    }

    // ------------------------------------------------------------ dismissing

    public void dismiss() {
        if (!mIsShowing) {
            return;
        }
        mIsShowing = false;
        PopupHost host = mHost;
        mHost = null;
        if (host != null) {
            host.close();
        }
        finishDismiss();
    }

    private void finishDismiss() {
        PopupDecorView decor = mDecor;
        mDecor = null;
        mAnchor = null;
        if (decor != null) {
            decor.dispatchAttachedToWindow(false);
            decor.removeAllViews();
        }
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    // ------------------------------------------------------------ internals

    private float density() {
        Context c = mContext != null ? mContext : mContentView.getContext();
        return c.getResources().getDisplayMetrics().density;
    }

    private void attach() {
        Context c = mContext != null ? mContext : mContentView.getContext();
        if (mContentView.getParent() instanceof ViewGroup) {
            ((ViewGroup) mContentView.getParent()).removeView(mContentView);
        }
        mDecor = new PopupDecorView(c);
        if (mBackground != null) {
            mDecor.setBackground(mBackground);
        }
        mDecor.addView(mContentView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        float dp = density();
        int shadow = Math.round(Math.min(mElevation, 24 * dp) * 0.5f);
        int radius = mBackground == null ? 0 : Math.round(2 * dp);
        mHost = new PopupHost(mDecor.getPeer(), shadow, radius, new PopupHost.Owner() {
            @Override
            public void onHostDismissed() {
                if (mIsShowing) {
                    mIsShowing = false;
                    mHost = null;
                    finishDismiss();
                }
            }

            @Override
            public boolean dismissesOnBack() {
                return mFocusable;
            }

            @Override
            public void dismissFromHost() {
                dismiss();
            }
        });
        mHost.setPassThrough(!mTouchable);
        mIsShowing = true;
        mDecor.dispatchAttachedToWindow(true);
    }

    private static int spec(int size, int max) {
        if (size >= 0) {
            return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
        }
        if (size == ViewGroup.LayoutParams.MATCH_PARENT) {
            return View.MeasureSpec.makeMeasureSpec(Math.max(0, max), View.MeasureSpec.EXACTLY);
        }
        return View.MeasureSpec.makeMeasureSpec(Math.max(0, max), View.MeasureSpec.AT_MOST);
    }

    private void measure(int maxWidth, int maxHeight) {
        mDecor.measure(spec(mWidth, maxWidth), spec(mHeight, maxHeight));
    }

    private void placeAtLocation() {
        Rectangle frame = PopupHost.displayFrame();
        measure(frame.getWidth(), frame.getHeight());
        int w = mDecor.getMeasuredWidth();
        int h = mDecor.getMeasuredHeight();
        int gravity = mLocationGravity == Gravity.NO_GRAVITY ? Gravity.TOP | Gravity.START : mLocationGravity;
        int dir = mDecor.getLayoutDirection();
        int abs = Gravity.getAbsoluteGravity(gravity, dir);
        int x;
        int y;
        switch (abs & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.RIGHT:
                x = frame.getX() + frame.getWidth() - w - mLocationX;
                break;
            case Gravity.CENTER_HORIZONTAL:
                x = frame.getX() + (frame.getWidth() - w) / 2 + mLocationX;
                break;
            default:
                x = frame.getX() + mLocationX;
                break;
        }
        switch (abs & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.BOTTOM:
                y = frame.getY() + frame.getHeight() - h - mLocationY;
                break;
            case Gravity.CENTER_VERTICAL:
                y = frame.getY() + (frame.getHeight() - h) / 2 + mLocationY;
                break;
            default:
                y = frame.getY() + mLocationY;
                break;
        }
        if (mClippingEnabled) {
            x = clamp(x, frame.getX(), frame.getX() + frame.getWidth() - w);
            y = clamp(y, frame.getY(), frame.getY() + frame.getHeight() - h);
        }
        mHost.place(x, y, w, h, outsideDismisses());
    }

    private void placeAsDropDown() {
        Rectangle frame = PopupHost.displayFrame();
        int[] loc = new int[2];
        mAnchor.getLocationOnScreen(loc);
        int anchorW = mAnchor.getWidth();
        int anchorH = mAnchor.getHeight();
        measure(frame.getWidth(), frame.getHeight());
        int w = mDecor.getMeasuredWidth();
        int h = mDecor.getMeasuredHeight();
        int x = loc[0] + mAnchorXoff;
        int abs = Gravity.getAbsoluteGravity(mAnchoredGravity, mAnchor.getLayoutDirection());
        if ((abs & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
            x -= w - anchorW;
        }
        int below = (mOverlapAnchor ? loc[1] : loc[1] + anchorH) + mAnchorYoff;
        int frameTop = frame.getY();
        int frameBottom = frame.getY() + frame.getHeight();
        int y = below;
        mAboveAnchor = false;
        if (below + h > frameBottom) {
            // Android flips the popup above the anchor when it fits there.
            int above = loc[1] - h + mAnchorYoff + (mOverlapAnchor ? anchorH : 0);
            if (above >= frameTop) {
                y = above;
                mAboveAnchor = true;
            } else if (mClippingEnabled) {
                y = Math.max(frameTop, frameBottom - h);
            }
        }
        if (mClippingEnabled) {
            x = clamp(x, frame.getX(), frame.getX() + frame.getWidth() - w);
        }
        mHost.place(x, y, w, Math.min(h, frameBottom - Math.max(frameTop, y)), outsideDismisses());
    }

    private boolean outsideDismisses() {
        return mTouchable && (mFocusable || mOutsideTouchable);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, Math.max(min, max)));
    }

    /// The popup's root: the background, the content, and the touch
    /// interceptor and back handling a popup window applies.
    private final class PopupDecorView extends FrameLayout {
        PopupDecorView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (!mTouchable) {
                return false;
            }
            if (mTouchInterceptor != null && mTouchInterceptor.onTouch(this, ev)) {
                return true;
            }
            return super.dispatchTouchEvent(ev);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && mFocusable) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    dismiss();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }
}
