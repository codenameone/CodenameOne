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
package android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.util.SparseArray;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.codename1.androidcompat.runtime.AndroidRuntime;
import com.codename1.androidcompat.runtime.FrameDriver;
import com.codename1.androidcompat.runtime.ViewPeer;
import com.codename1.ui.Component;

import java.util.ArrayList;

/// The basic building block of an Android UI, implemented over a Codename One
/// component (its *peer*).
///
/// Measurement and layout follow Android exactly -- `measure`, `onMeasure`,
/// `layout`, `onLayout`, `MeasureSpec` -- and their result is pushed onto the
/// peer's bounds. Drawing goes the other way: the peer's `paint` calls
/// [#draw(Canvas)]. Touch input enters at the activity's root and is
/// dispatched down the view tree as Android dispatches it, so
/// `onInterceptTouchEvent` and `onTouchEvent` behave as written.
public class View implements Drawable.Callback {

    public static final int NO_ID = -1;
    public static final int VISIBLE = 0x00000000;
    public static final int INVISIBLE = 0x00000004;
    public static final int GONE = 0x00000008;
    public static final int VISIBILITY_MASK = 0x0000000C;

    public static final int MEASURED_SIZE_MASK = 0x00ffffff;
    public static final int MEASURED_STATE_MASK = 0xff000000;
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;
    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;

    public static final int LAYOUT_DIRECTION_LTR = 0;
    public static final int LAYOUT_DIRECTION_RTL = 1;
    public static final int LAYOUT_DIRECTION_INHERIT = 2;
    public static final int LAYOUT_DIRECTION_LOCALE = 3;

    public static final int TEXT_ALIGNMENT_INHERIT = 0;
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;
    public static final int TEXT_ALIGNMENT_TEXT_START = 2;
    public static final int TEXT_ALIGNMENT_TEXT_END = 3;
    public static final int TEXT_ALIGNMENT_CENTER = 4;
    public static final int TEXT_ALIGNMENT_VIEW_START = 5;
    public static final int TEXT_ALIGNMENT_VIEW_END = 6;
    public static final int TEXT_DIRECTION_INHERIT = 0;
    public static final int TEXT_DIRECTION_LOCALE = 5;

    public static final int FOCUS_BACKWARD = 0x00000001;
    public static final int FOCUS_FORWARD = 0x00000002;
    public static final int FOCUS_LEFT = 0x00000011;
    public static final int FOCUS_UP = 0x00000021;
    public static final int FOCUS_RIGHT = 0x00000042;
    public static final int FOCUS_DOWN = 0x00000082;
    public static final int NOT_FOCUSABLE = 0x00000000;
    public static final int FOCUSABLE = 0x00000001;
    public static final int FOCUSABLE_AUTO = 0x00000010;

    public static final int SCROLLBARS_INSIDE_OVERLAY = 0;
    public static final int SCROLLBARS_OUTSIDE_OVERLAY = 0x02000000;
    public static final int SCROLLBAR_POSITION_DEFAULT = 0;
    public static final int OVER_SCROLL_ALWAYS = 0;
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;
    public static final int OVER_SCROLL_NEVER = 2;

    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 1;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 2;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 4;
    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 1;
    public static final int IMPORTANT_FOR_AUTOFILL_AUTO = 0;
    public static final int IMPORTANT_FOR_AUTOFILL_NO = 2;

    public static final int LAYER_TYPE_NONE = 0;
    public static final int LAYER_TYPE_SOFTWARE = 1;
    public static final int LAYER_TYPE_HARDWARE = 2;

    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 0x00000001;
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 0x00000002;
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 0x00000004;
    public static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x00000010;
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 0x00000100;
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 0x00000200;
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 0x00000400;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 0x00000800;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000;
    public static final int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 0x00002000;

    public static final int HAPTIC_FEEDBACK_ENABLED = 0x10000000;
    public static final int SOUND_EFFECTS_ENABLED = 0x08000000;
    public static final int DRAWING_CACHE_QUALITY_AUTO = 0;

    public static final int[] EMPTY_STATE_SET = {};
    public static final int[] ENABLED_STATE_SET = {android.R.attr.state_enabled};
    public static final int[] FOCUSED_STATE_SET = {android.R.attr.state_focused};
    public static final int[] SELECTED_STATE_SET = {android.R.attr.state_selected};
    public static final int[] PRESSED_STATE_SET = {android.R.attr.state_pressed};
    public static final int[] WINDOW_FOCUSED_STATE_SET = {android.R.attr.state_window_focused};
    public static final int[] PRESSED_ENABLED_STATE_SET = {android.R.attr.state_pressed, android.R.attr.state_enabled};
    public static final int[] ENABLED_FOCUSED_STATE_SET = {android.R.attr.state_enabled, android.R.attr.state_focused};
    public static final int[] ENABLED_SELECTED_STATE_SET = {android.R.attr.state_enabled, android.R.attr.state_selected};

    // ------------------------------------------------------------ listeners

    public interface OnClickListener {
        void onClick(View v);
    }

    public interface OnLongClickListener {
        boolean onLongClick(View v);
    }

    public interface OnTouchListener {
        boolean onTouch(View v, MotionEvent event);
    }

    public interface OnFocusChangeListener {
        void onFocusChange(View v, boolean hasFocus);
    }

    public interface OnKeyListener {
        boolean onKey(View v, int keyCode, KeyEvent event);
    }

    public interface OnLayoutChangeListener {
        void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                            int oldRight, int oldBottom);
    }

    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View v);

        void onViewDetachedFromWindow(View v);
    }

    public interface OnScrollChangeListener {
        void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
    }

    public interface OnSystemUiVisibilityChangeListener {
        void onSystemUiVisibilityChange(int visibility);
    }

    public interface OnCreateContextMenuListener {
        void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo);
    }

    public interface OnGenericMotionListener {
        boolean onGenericMotion(View v, MotionEvent event);
    }

    public interface OnHoverListener {
        boolean onHover(View v, MotionEvent event);
    }

    public interface OnDragListener {
        boolean onDrag(View v, DragEvent event);
    }

    // ------------------------------------------------------------ MeasureSpec

    public static class MeasureSpec {
        private static final int MODE_SHIFT = 30;
        private static final int MODE_MASK = 0x3 << MODE_SHIFT;
        public static final int UNSPECIFIED = 0 << MODE_SHIFT;
        public static final int EXACTLY = 1 << MODE_SHIFT;
        public static final int AT_MOST = 2 << MODE_SHIFT;

        public static int makeMeasureSpec(int size, int mode) {
            return (size & ~MODE_MASK) | (mode & MODE_MASK);
        }

        public static int makeSafeMeasureSpec(int size, int mode) {
            return makeMeasureSpec(mode == UNSPECIFIED ? 0 : size, mode);
        }

        public static int getMode(int measureSpec) {
            return measureSpec & MODE_MASK;
        }

        public static int getSize(int measureSpec) {
            return measureSpec & ~MODE_MASK;
        }

        public static String toString(int measureSpec) {
            int mode = getMode(measureSpec);
            String m = mode == UNSPECIFIED ? "UNSPECIFIED" : mode == EXACTLY ? "EXACTLY" : "AT_MOST";
            return "MeasureSpec: " + m + " " + getSize(measureSpec);
        }
    }

    // ------------------------------------------------------------ state

    private static int nextGeneratedId = 1;

    protected Context mContext;
    ViewParent mParent;
    int mID = NO_ID;
    Object mTag;
    private SparseArray<Object> mKeyedTags;
    protected int mLeft;
    protected int mTop;
    protected int mRight;
    protected int mBottom;
    int mMeasuredWidth;
    int mMeasuredHeight;
    private int mOldWidthMeasureSpec = Integer.MIN_VALUE;
    private int mOldHeightMeasureSpec = Integer.MIN_VALUE;
    private boolean mMeasureCalled;
    private int mMinWidth;
    private int mMinHeight;
    protected int mPaddingLeft;
    protected int mPaddingTop;
    protected int mPaddingRight;
    protected int mPaddingBottom;
    private int mUserPaddingStart = Integer.MIN_VALUE;
    private int mUserPaddingEnd = Integer.MIN_VALUE;
    protected ViewGroup.LayoutParams mLayoutParams;
    private Drawable mBackground;
    private Drawable mForeground;
    private ColorStateList mBackgroundTint;
    private PorterDuff.Mode mBackgroundTintMode;
    private int mVisibility = VISIBLE;
    private boolean mEnabled = true;
    private boolean mClickable;
    private boolean mLongClickable;
    private boolean mFocusable;
    private boolean mFocusableInTouchMode;
    private boolean mPressed;
    private boolean mSelected;
    private boolean mActivated;
    private boolean mFocused;
    private boolean mDuplicateParentState;
    boolean mLayoutRequested = true;
    boolean mForceLayout = true;
    private boolean mAttached;
    /// Set by [#dispatchWindowFocusChanged(boolean)] while the window has lost
    /// focus; read on the root by [#hasWindowFocus()].
    private boolean mWindowFocusLost;
    private boolean mHasPerformedLongPress;
    private boolean mWillNotDraw;
    protected int mScrollX;
    protected int mScrollY;
    private float mAlpha = 1f;
    private float mTranslationX;
    private float mTranslationY;
    private float mRotation;
    private float mScaleX = 1f;
    private float mScaleY = 1f;
    private float mElevation;
    private float mPivotX = Float.NaN;
    private float mPivotY = Float.NaN;
    private CharSequence mContentDescription;
    private int mLayoutDirection = LAYOUT_DIRECTION_INHERIT;
    private int mTextAlignment = TEXT_ALIGNMENT_GRAVITY;
    private int mSystemUiVisibility;
    private int mImportantForAccessibility;
    private String mOnClickMethod;
    private int mOverScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS;

    private OnClickListener mOnClickListener;
    private OnLongClickListener mOnLongClickListener;
    private OnTouchListener mOnTouchListener;
    private OnFocusChangeListener mOnFocusChangeListener;
    private OnKeyListener mOnKeyListener;
    private OnScrollChangeListener mOnScrollChangeListener;
    private OnCreateContextMenuListener mOnCreateContextMenuListener;
    private ArrayList<OnLayoutChangeListener> mLayoutChangeListeners;
    private ArrayList<OnAttachStateChangeListener> mAttachListeners;

    private Component mPeer;
    private Runnable mPendingLongPress;
    private ViewPropertyAnimator mAnimator;
    /// The legacy view animation (`startAnimation`), its last transformation
    /// split into a translation applied by moving the peer and the rest
    /// (scale, rotation) applied while painting, about the view's center.
    private Animation mCurrentAnimation;
    private final Transformation mAnimScratch = new Transformation();
    private float mAnimDx;
    private float mAnimDy;
    private float mAnimAlpha = 1f;
    private Matrix mAnimResidual;
    private LegacyAnimationTicker mAnimTicker;
    private final Canvas mCanvas = new Canvas();
    private final Rect mTmpRect = new Rect();

    public View(Context context) {
        mContext = context;
    }

    public View(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public View(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public View(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.View, defStyleAttr, defStyleRes);
        int padding = -1;
        int paddingH = -1;
        int paddingV = -1;
        int left = -1;
        int top = -1;
        int right = -1;
        int bottom = -1;
        int start = Integer.MIN_VALUE;
        int end = Integer.MIN_VALUE;
        Drawable background = null;
        for (int i = 0, n = a.getIndexCount(); i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == android.R.styleable.View_id) {
                mID = a.getResourceId(attr, NO_ID);
            } else if (attr == android.R.styleable.View_tag) {
                mTag = a.getText(attr);
            } else if (attr == android.R.styleable.View_background) {
                background = a.getDrawable(attr);
            } else if (attr == android.R.styleable.View_backgroundTint) {
                mBackgroundTint = a.getColorStateList(attr);
            } else if (attr == android.R.styleable.View_foreground) {
                mForeground = a.getDrawable(attr);
            } else if (attr == android.R.styleable.View_padding) {
                padding = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.View_paddingHorizontal) {
                paddingH = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.View_paddingVertical) {
                paddingV = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.View_paddingLeft) {
                left = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.View_paddingTop) {
                top = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.View_paddingRight) {
                right = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.View_paddingBottom) {
                bottom = a.getDimensionPixelSize(attr, -1);
            } else if (attr == android.R.styleable.View_paddingStart) {
                start = a.getDimensionPixelSize(attr, Integer.MIN_VALUE);
            } else if (attr == android.R.styleable.View_paddingEnd) {
                end = a.getDimensionPixelSize(attr, Integer.MIN_VALUE);
            } else if (attr == android.R.styleable.View_visibility) {
                int v = a.getInt(attr, 0);
                mVisibility = v == 1 ? INVISIBLE : v == 2 ? GONE : VISIBLE;
            } else if (attr == android.R.styleable.View_clickable) {
                mClickable = a.getBoolean(attr, false);
            } else if (attr == android.R.styleable.View_longClickable) {
                mLongClickable = a.getBoolean(attr, false);
            } else if (attr == android.R.styleable.View_focusable) {
                mFocusable = a.getBoolean(attr, false);
            } else if (attr == android.R.styleable.View_focusableInTouchMode) {
                mFocusableInTouchMode = a.getBoolean(attr, false);
            } else if (attr == android.R.styleable.View_enabled) {
                mEnabled = a.getBoolean(attr, true);
            } else if (attr == android.R.styleable.View_selected) {
                mSelected = a.getBoolean(attr, false);
            } else if (attr == android.R.styleable.View_minWidth) {
                mMinWidth = a.getDimensionPixelSize(attr, 0);
            } else if (attr == android.R.styleable.View_minHeight) {
                mMinHeight = a.getDimensionPixelSize(attr, 0);
            } else if (attr == android.R.styleable.View_alpha) {
                mAlpha = a.getFloat(attr, 1f);
            } else if (attr == android.R.styleable.View_elevation) {
                mElevation = a.getDimension(attr, 0);
            } else if (attr == android.R.styleable.View_translationX) {
                mTranslationX = a.getDimension(attr, 0);
            } else if (attr == android.R.styleable.View_translationY) {
                mTranslationY = a.getDimension(attr, 0);
            } else if (attr == android.R.styleable.View_rotation) {
                mRotation = a.getFloat(attr, 0);
            } else if (attr == android.R.styleable.View_scaleX) {
                mScaleX = a.getFloat(attr, 1);
            } else if (attr == android.R.styleable.View_scaleY) {
                mScaleY = a.getFloat(attr, 1);
            } else if (attr == android.R.styleable.View_transformPivotX) {
                mPivotX = a.getDimension(attr, 0);
            } else if (attr == android.R.styleable.View_transformPivotY) {
                mPivotY = a.getDimension(attr, 0);
            } else if (attr == android.R.styleable.View_translationZ) {
                setTranslationZ(a.getDimension(attr, 0));
            } else if (attr == android.R.styleable.View_rotationX) {
                setRotationX(a.getFloat(attr, 0));
            } else if (attr == android.R.styleable.View_rotationY) {
                setRotationY(a.getFloat(attr, 0));
            } else if (attr == android.R.styleable.View_onClick) {
                mOnClickMethod = a.getString(attr);
                if (mOnClickMethod != null) {
                    mClickable = true;
                }
            } else if (attr == android.R.styleable.View_contentDescription) {
                mContentDescription = a.getText(attr);
            } else if (attr == android.R.styleable.View_layoutDirection) {
                mLayoutDirection = a.getInt(attr, LAYOUT_DIRECTION_INHERIT);
            } else if (attr == android.R.styleable.View_textAlignment) {
                mTextAlignment = a.getInt(attr, TEXT_ALIGNMENT_GRAVITY);
            } else if (attr == android.R.styleable.View_duplicateParentState) {
                mDuplicateParentState = a.getBoolean(attr, false);
            }
        }
        a.recycle();
        if (background != null) {
            setBackground(background);
        }
        if (padding >= 0) {
            left = top = right = bottom = padding;
        } else {
            if (paddingH >= 0) {
                left = right = paddingH;
            }
            if (paddingV >= 0) {
                top = bottom = paddingV;
            }
        }
        // Explicit padding wins over the background's; unset sides keep the
        // background's padding that setBackground installed. setPadding
        // clears the relative padding, so start and end are recorded after it.
        setPadding(left >= 0 ? left : mPaddingLeft, top >= 0 ? top : mPaddingTop,
                right >= 0 ? right : mPaddingRight, bottom >= 0 ? bottom : mPaddingBottom);
        if (padding < 0 && paddingH < 0) {
            if (start != Integer.MIN_VALUE) {
                mUserPaddingStart = start;
            }
            if (end != Integer.MIN_VALUE) {
                mUserPaddingEnd = end;
            }
        }
        resolvePaddingStartEnd();
        if (mForeground != null) {
            mForeground.setCallback(this);
        }
        if (mBackgroundTint != null && mBackground != null) {
            mBackground.setTintList(mBackgroundTint);
        }
        refreshDrawableState();
    }

    private void resolvePaddingStartEnd() {
        boolean rtl = isLayoutRtl();
        if (mUserPaddingStart != Integer.MIN_VALUE) {
            if (rtl) {
                mPaddingRight = mUserPaddingStart;
            } else {
                mPaddingLeft = mUserPaddingStart;
            }
        }
        if (mUserPaddingEnd != Integer.MIN_VALUE) {
            if (rtl) {
                mPaddingLeft = mUserPaddingEnd;
            } else {
                mPaddingRight = mUserPaddingEnd;
            }
        }
    }

    // ------------------------------------------------------------ peer

    /// The Codename One component that shows this view. Created on first use.
    public final Component getPeer() {
        if (mPeer == null) {
            mPeer = createPeer();
            mPeer.setVisible(peerShown());
        }
        return mPeer;
    }

    /// Whether a peer has been created yet.
    public final boolean hasPeer() {
        return mPeer != null;
    }

    /// Creates the peer. Views that wrap a native Codename One widget (a text
    /// field) override this; everything else draws itself on a plain peer.
    protected Component createPeer() {
        return new ViewPeer(this);
    }

    /// Paints this view through `g`, its peer's graphics, with the view's
    /// origin at (`x`, `y`). Called by the peer.
    public void paintPeer(com.codename1.ui.Graphics g, int x, int y) {
        FrameDriver.ensureCurrent();
        int oldAlpha = g.getAlpha();
        float alpha = paintAlpha();
        if (alpha < 1f) {
            g.setAlpha(Math.round(oldAlpha * alpha));
        }
        Matrix m = paintMatrix();
        mPaintedTransformed = m != null || drawsOutsideBounds();
        if (mPaintedTransformed) {
            widenClipToParent(g);
        }
        mCanvas.bind(g, x, y, getWidth(), getHeight());
        // As AOSP: the animation's and the view's matrices, then the view's
        // own scroll (content coordinates; drawBackground translates back).
        if (m != null) {
            mCanvas.concat(m);
        }
        mCanvas.translate(-mScrollX, -mScrollY);
        draw(mCanvas);
        g.setAlpha(oldAlpha);
    }

    private com.codename1.ui.Transform mSavedGroupTransform;

    /// Whether the last paint was rotated or scaled, so it may have covered
    /// pixels outside the view's bounds that the next repaint must clear.
    private boolean mPaintedTransformed;

    /// Whether this view paints outside its own bounds -- an elevation
    /// shadow, a focus halo. Android clips such drawing only by the parent;
    /// answering true widens the clip the same way and makes invalidation
    /// repaint the parent, so the area outside is cleared too.
    protected boolean drawsOutsideBounds() {
        return false;
    }

    /// Codename One clips a peer to its own bounds; Android clips a rotated
    /// or scaled view only by its parent. Widens the clip, which Codename One
    /// restores after the peer paints, to the parent's visible area.
    private void widenClipToParent(com.codename1.ui.Graphics g) {
        Component parent = mPeer == null ? null : mPeer.getParent();
        if (parent instanceof com.codename1.androidcompat.runtime.GroupPeer) {
            int[] c = new int[4];
            if (((com.codename1.androidcompat.runtime.GroupPeer) parent).childClip(c)) {
                g.setClip(c[0], c[1], c[2], c[3]);
            }
        }
    }

    /// Installs the group's matrix on the context, so the children's peers
    /// paint transformed with it; answers the transform to restore, or null.
    private com.codename1.ui.Transform pushGroupMatrix(com.codename1.ui.Graphics g, int x, int y) {
        Matrix m = paintMatrix();
        if (m == null || !g.isTransformSupported()) {
            return null;
        }
        com.codename1.ui.Transform saved = com.codename1.ui.Transform.makeIdentity();
        g.getTransform(saved);
        float[] v = new float[9];
        m.getValues(v);
        com.codename1.ui.Transform t = saved.copy();
        t.translate(x, y);
        t.concatenate(com.codename1.ui.Transform.makeAffine(v[0], v[3], v[1], v[4], v[2], v[5]));
        t.translate(-x, -y);
        g.setTransform(t);
        return saved;
    }

    private int mSavedAlpha;

    /// For a view group's peer: everything Android draws below the children
    /// (background and `onDraw`). Codename One also calls this when it
    /// repaints a child alone, to restore what lies underneath it.
    public void paintGroupBackground(com.codename1.ui.Graphics g, int x, int y) {
        FrameDriver.ensureCurrent();
        onGroupPaint();
        int old = g.getAlpha();
        float alpha = paintAlpha();
        if (alpha < 1f) {
            g.setAlpha(Math.round(old * alpha));
        }
        mPaintedTransformed = paintMatrix() != null || drawsOutsideBounds();
        if (mPaintedTransformed) {
            widenClipToParent(g);
        }
        com.codename1.ui.Transform saved = pushGroupMatrix(g, x, y);
        mCanvas.bind(g, x, y, getWidth(), getHeight());
        mCanvas.translate(-mScrollX, -mScrollY);
        drawBackground(mCanvas);
        if (!skipsDraw()) {
            onDraw(mCanvas);
        }
        if (saved != null) {
            g.setTransform(saved);
        }
        g.setAlpha(old);
    }

    /// Runs before a view group paints: where a pending layout animation
    /// is handed to the children.
    void onGroupPaint() {
    }

    /// For a view group's peer: applies the group's alpha before its
    /// children paint, so they fade with it.
    public void beginGroupChildren(com.codename1.ui.Graphics g) {
        beginGroupChildren(g, mPeer == null ? 0 : mPeer.getX(), mPeer == null ? 0 : mPeer.getY());
    }

    /// As [#beginGroupChildren(com.codename1.ui.Graphics)], with the group's
    /// origin in the context's coordinates, so its scale and rotation
    /// (animated or set) apply to the children too.
    public void beginGroupChildren(com.codename1.ui.Graphics g, int x, int y) {
        mSavedAlpha = g.getAlpha();
        float alpha = paintAlpha();
        if (alpha < 1f) {
            g.setAlpha(Math.round(mSavedAlpha * alpha));
        }
        if (paintMatrix() != null) {
            widenClipToParent(g);
        }
        mSavedGroupTransform = pushGroupMatrix(g, x, y);
    }

    /// For a view group's peer: everything Android draws above the children.
    public void paintGroupAbove(com.codename1.ui.Graphics g, int x, int y) {
        mCanvas.bind(g, x, y, getWidth(), getHeight());
        mCanvas.translate(-mScrollX, -mScrollY);
        dispatchDraw(mCanvas);
        onDrawForeground(mCanvas);
        if (mSavedGroupTransform != null) {
            g.setTransform(mSavedGroupTransform);
            mSavedGroupTransform = null;
        }
        g.setAlpha(mSavedAlpha);
    }

    /// Android skips `onDraw` for a view that will not draw and has nothing
    /// (background or foreground) that would make it.
    private boolean skipsDraw() {
        return mWillNotDraw && mBackground == null && mForeground == null;
    }

    // ------------------------------------------------------------ identity

    public Context getContext() {
        return mContext;
    }

    public Resources getResources() {
        return mContext == null ? Resources.getSystem() : mContext.getResources();
    }

    public int getId() {
        return mID;
    }

    public void setId(int id) {
        mID = id;
    }

    public static int generateViewId() {
        return nextGeneratedId++;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    private String mTransitionName;

    /// Recorded for `getTransitionName`; shared element transitions are not
    /// played.
    public final void setTransitionName(String transitionName) {
        mTransitionName = transitionName;
    }

    public String getTransitionName() {
        return mTransitionName;
    }

    public Object getTag(int key) {
        return mKeyedTags == null ? null : mKeyedTags.get(key);
    }

    public void setTag(int key, Object tag) {
        if (mKeyedTags == null) {
            mKeyedTags = new SparseArray<Object>(2);
        }
        mKeyedTags.put(key, tag);
    }

    public final ViewParent getParent() {
        return mParent;
    }

    void assignParent(ViewParent parent) {
        mParent = parent;
        if (parent != null && mLayoutDirection == LAYOUT_DIRECTION_INHERIT) {
            // Relative padding set before the view had a parent (inflation
            // does that) was resolved against the locale; the parent may
            // say otherwise. Whoever adds the view lays it out already.
            dispatchLayoutDirectionChanged(false);
        }
    }

    public View getRootView() {
        View v = this;
        while (v.mParent instanceof View) {
            v = (View) v.mParent;
        }
        return v;
    }

    public final <T extends View> T findViewById(int id) {
        if (id == NO_ID) {
            return null;
        }
        return findViewTraversal(id);
    }

    public final <T extends View> T requireViewById(int id) {
        T v = findViewById(id);
        if (v == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this View");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewTraversal(int id) {
        return id == mID ? (T) this : null;
    }

    public final <T extends View> T findViewWithTag(Object tag) {
        return findViewWithTagTraversal(tag);
    }

    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewWithTagTraversal(Object tag) {
        return tag != null && tag.equals(mTag) ? (T) this : null;
    }

    public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags) {
        if (mContentDescription != null && searched != null
                && mContentDescription.toString().indexOf(searched.toString()) >= 0) {
            outViews.add(this);
        }
    }

    // ------------------------------------------------------------ geometry

    public final int getLeft() {
        return mLeft;
    }

    public final int getTop() {
        return mTop;
    }

    public final int getRight() {
        return mRight;
    }

    public final int getBottom() {
        return mBottom;
    }

    public final int getWidth() {
        return mRight - mLeft;
    }

    public final int getHeight() {
        return mBottom - mTop;
    }

    public final void setLeft(int left) {
        setFrame(left, mTop, mRight, mBottom);
    }

    public final void setTop(int top) {
        setFrame(mLeft, top, mRight, mBottom);
    }

    public final void setRight(int right) {
        setFrame(mLeft, mTop, right, mBottom);
    }

    public final void setBottom(int bottom) {
        setFrame(mLeft, mTop, mRight, bottom);
    }

    public void offsetLeftAndRight(int offset) {
        setFrame(mLeft + offset, mTop, mRight + offset, mBottom);
    }

    public void offsetTopAndBottom(int offset) {
        setFrame(mLeft, mTop + offset, mRight, mBottom + offset);
    }

    public float getX() {
        return mLeft + mTranslationX;
    }

    public float getY() {
        return mTop + mTranslationY;
    }

    public void setX(float x) {
        setTranslationX(x - mLeft);
    }

    public void setY(float y) {
        setTranslationY(y - mTop);
    }

    public float getZ() {
        return mElevation;
    }

    public void setZ(float z) {
        mElevation = z;
    }

    public void getDrawingRect(Rect outRect) {
        outRect.set(mScrollX, mScrollY, mScrollX + getWidth(), mScrollY + getHeight());
    }

    public void getHitRect(Rect outRect) {
        outRect.set(mLeft, mTop, mRight, mBottom);
        outRect.offset((int) mTranslationX, (int) mTranslationY);
    }

    public void getLocationOnScreen(int[] outLocation) {
        Component p = getPeer();
        outLocation[0] = p.getAbsoluteX();
        outLocation[1] = p.getAbsoluteY();
    }

    public void getLocationInWindow(int[] outLocation) {
        getLocationOnScreen(outLocation);
    }

    public boolean getGlobalVisibleRect(Rect r) {
        return getGlobalVisibleRect(r, null);
    }

    /// The part of this view on screen: its bounds cut to every ancestor's
    /// on-screen box (a scrolled-out child of a scroll view is not visible)
    /// and to the display. Ancestors are always intersected, whatever their
    /// `clipChildren`, because the peers they map to always clip painting.
    public boolean getGlobalVisibleRect(Rect r, Point globalOffset) {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        r.set(loc[0], loc[1], loc[0] + getWidth(), loc[1] + getHeight());
        if (globalOffset != null) {
            globalOffset.set(loc[0], loc[1]);
        }
        boolean visible = isShown();
        ViewParent p = mParent;
        while (visible && p instanceof View) {
            View a = (View) p;
            a.getLocationOnScreen(loc);
            visible = r.intersect(loc[0], loc[1], loc[0] + a.getWidth(), loc[1] + a.getHeight());
            p = a.mParent;
        }
        if (visible) {
            com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
            visible = r.intersect(0, 0, d.getDisplayWidth(), d.getDisplayHeight());
        }
        return visible && !r.isEmpty();
    }

    public boolean getLocalVisibleRect(Rect r) {
        Point offset = new Point();
        boolean visible = getGlobalVisibleRect(r, offset);
        r.offset(-offset.x, -offset.y);
        return visible;
    }

    public final void getWindowVisibleDisplayFrame(Rect outRect) {
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        outRect.set(0, 0, d.getDisplayWidth(), d.getDisplayHeight());
    }

    // ------------------------------------------------------------ animatable properties

    public static final Property<View, Float> ALPHA = new FloatProperty<View>("alpha") {
        @Override
        public void setValue(View object, float value) {
            object.setAlpha(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getAlpha());
        }
    };

    public static final Property<View, Float> TRANSLATION_X = new FloatProperty<View>("translationX") {
        @Override
        public void setValue(View object, float value) {
            object.setTranslationX(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getTranslationX());
        }
    };

    public static final Property<View, Float> TRANSLATION_Y = new FloatProperty<View>("translationY") {
        @Override
        public void setValue(View object, float value) {
            object.setTranslationY(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getTranslationY());
        }
    };

    public static final Property<View, Float> TRANSLATION_Z = new FloatProperty<View>("translationZ") {
        @Override
        public void setValue(View object, float value) {
            object.setTranslationZ(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getTranslationZ());
        }
    };

    public static final Property<View, Float> X = new FloatProperty<View>("x") {
        @Override
        public void setValue(View object, float value) {
            object.setX(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getX());
        }
    };

    public static final Property<View, Float> Y = new FloatProperty<View>("y") {
        @Override
        public void setValue(View object, float value) {
            object.setY(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getY());
        }
    };

    public static final Property<View, Float> Z = new FloatProperty<View>("z") {
        @Override
        public void setValue(View object, float value) {
            object.setZ(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getZ());
        }
    };

    public static final Property<View, Float> ROTATION = new FloatProperty<View>("rotation") {
        @Override
        public void setValue(View object, float value) {
            object.setRotation(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getRotation());
        }
    };

    public static final Property<View, Float> ROTATION_X = new FloatProperty<View>("rotationX") {
        @Override
        public void setValue(View object, float value) {
            object.setRotationX(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getRotationX());
        }
    };

    public static final Property<View, Float> ROTATION_Y = new FloatProperty<View>("rotationY") {
        @Override
        public void setValue(View object, float value) {
            object.setRotationY(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getRotationY());
        }
    };

    public static final Property<View, Float> SCALE_X = new FloatProperty<View>("scaleX") {
        @Override
        public void setValue(View object, float value) {
            object.setScaleX(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getScaleX());
        }
    };

    public static final Property<View, Float> SCALE_Y = new FloatProperty<View>("scaleY") {
        @Override
        public void setValue(View object, float value) {
            object.setScaleY(value);
        }

        @Override
        public Float get(View object) {
            return Float.valueOf(object.getScaleY());
        }
    };

    private static final Property[] ANIMATABLE = {ALPHA, TRANSLATION_X, TRANSLATION_Y, TRANSLATION_Z, X, Y, Z, ROTATION, ROTATION_X, ROTATION_Y, SCALE_X, SCALE_Y};

    /// The animatable property called `name` (`"alpha"`, `"translationX"`,
    /// ...), or null. What `ObjectAnimator` and `Property.of` resolve a
    /// property name through, since there is no reflection.
    @SuppressWarnings("unchecked")
    public static Property<View, Float> propertyNamed(String name) {
        for (Property p : ANIMATABLE) {
            if (p.getName().equals(name)) {
                return (Property<View, Float>) p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ transforms

    public float getAlpha() {
        return mAlpha;
    }

    public void setAlpha(float alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidate();
        }
    }

    public float getTranslationX() {
        return mTranslationX;
    }

    public void setTranslationX(float t) {
        if (mTranslationX != t) {
            mTranslationX = t;
            syncPeerBounds();
            invalidateParent();
        }
    }

    public float getTranslationY() {
        return mTranslationY;
    }

    public void setTranslationY(float t) {
        if (mTranslationY != t) {
            mTranslationY = t;
            syncPeerBounds();
            invalidateParent();
        }
    }

    public float getTranslationZ() {
        return 0;
    }

    public void setTranslationZ(float z) {
    }

    public float getRotation() {
        return mRotation;
    }

    public void setRotation(float rotation) {
        mRotation = rotation;
        invalidate();
    }

    public float getRotationX() {
        return 0;
    }

    public void setRotationX(float r) {
    }

    public float getRotationY() {
        return 0;
    }

    public void setRotationY(float r) {
    }

    public float getScaleX() {
        return mScaleX;
    }

    public void setScaleX(float s) {
        mScaleX = s;
        invalidate();
    }

    public float getScaleY() {
        return mScaleY;
    }

    public void setScaleY(float s) {
        mScaleY = s;
        invalidate();
    }

    public float getPivotX() {
        return Float.isNaN(mPivotX) ? getWidth() / 2f : mPivotX;
    }

    public void setPivotX(float p) {
        mPivotX = p;
        invalidate();
    }

    public float getPivotY() {
        return Float.isNaN(mPivotY) ? getHeight() / 2f : mPivotY;
    }

    public void setPivotY(float p) {
        mPivotY = p;
        invalidate();
    }

    public float getElevation() {
        return mElevation;
    }

    public void setElevation(float elevation) {
        mElevation = elevation;
    }

    public void setCameraDistance(float distance) {
    }

    public ViewPropertyAnimator animate() {
        if (mAnimator == null) {
            mAnimator = new ViewPropertyAnimator(this);
        }
        return mAnimator;
    }

    public Animation getAnimation() {
        return mCurrentAnimation;
    }

    public void startAnimation(Animation animation) {
        animation.setStartTime(Animation.START_ON_FIRST_FRAME);
        setAnimation(animation);
        invalidateParent();
        invalidate();
    }

    public void clearAnimation() {
        if (mCurrentAnimation != null) {
            mCurrentAnimation.detach();
        }
        mCurrentAnimation = null;
        stopAnimationTicker();
        applyAnimationTransformation(null);
        syncPeerVisibility();
    }

    public void setAnimation(Animation animation) {
        mCurrentAnimation = animation;
        if (animation != null) {
            animation.reset();
            startAnimationTicker();
        } else {
            stopAnimationTicker();
            applyAnimationTransformation(null);
        }
        syncPeerVisibility();
    }

    /// Called when the view's animation draws its first frame.
    protected void onAnimationStart() {
    }

    /// Called when the view's animation has finished.
    protected void onAnimationEnd() {
    }

    protected boolean onSetAlpha(int alpha) {
        return false;
    }

    /// Advances the legacy animation by one frame. Android does this as part
    /// of drawing; here the Codename One animation loop drives it, so the
    /// view keeps moving without anything else repainting it.
    boolean stepLegacyAnimation() {
        Animation a = mCurrentAnimation;
        if (a == null) {
            return false;
        }
        if (!a.isInitialized()) {
            int pw = mParent instanceof View ? ((View) mParent).getWidth() : getWidth();
            int ph = mParent instanceof View ? ((View) mParent).getHeight() : getHeight();
            a.initialize(getWidth(), getHeight(), pw, ph);
            onAnimationStart();
        }
        mAnimScratch.clear();
        boolean more = a.getTransformation(AnimationUtils.currentAnimationTimeMillis(), mAnimScratch, 1f);
        if (more || a.getFillAfter()) {
            applyAnimationTransformation(mAnimScratch);
        }
        if (!more) {
            stopAnimationTicker();
            if (!a.getFillAfter()) {
                // As ViewGroup.finishAnimatingView: the view snaps back.
                mCurrentAnimation = null;
                applyAnimationTransformation(null);
                syncPeerVisibility();
            }
            onAnimationEnd();
        }
        return more;
    }

    private void startAnimationTicker() {
        if (mAnimTicker == null) {
            mAnimTicker = new LegacyAnimationTicker(this);
        }
        mAnimTicker.start();
    }

    private void stopAnimationTicker() {
        if (mAnimTicker != null) {
            mAnimTicker.stop();
        }
    }

    /// Splits `t` into the peer offset and the paint-time matrix; null
    /// removes the animation's effect.
    private void applyAnimationTransformation(Transformation t) {
        float dx = 0;
        float dy = 0;
        float alpha = 1f;
        Matrix residual = null;
        if (t != null) {
            alpha = t.getAlpha();
            Matrix m = t.getMatrix();
            if (!m.isIdentity()) {
                boolean root = mParent == null && mPeer != null && ViewPeer.isRootPlaced(mPeer);
                if (!root) {
                    float[] c = {getWidth() / 2f, getHeight() / 2f};
                    m.mapPoints(c);
                    dx = c[0] - getWidth() / 2f;
                    dy = c[1] - getHeight() / 2f;
                }
                residual = new Matrix(m);
                residual.postTranslate(-dx, -dy);
                if (nearIdentity(residual)) {
                    residual = null;
                }
            }
        }
        boolean moved = Math.round(dx) != Math.round(mAnimDx) || Math.round(dy) != Math.round(mAnimDy);
        mAnimDx = dx;
        mAnimDy = dy;
        mAnimAlpha = alpha;
        mAnimResidual = residual;
        if (moved) {
            syncPeerBounds();
        }
        invalidateParent();
        invalidate();
    }

    private static boolean nearIdentity(Matrix m) {
        float[] v = new float[9];
        m.getValues(v);
        return Math.abs(v[0] - 1) < 1e-4f && Math.abs(v[4] - 1) < 1e-4f && Math.abs(v[1]) < 1e-4f
                && Math.abs(v[3]) < 1e-4f && Math.abs(v[2]) < 0.01f && Math.abs(v[5]) < 0.01f;
    }

    /// The matrix painting applies on top of the peer's position: the
    /// animation's scale and rotation, then the view's own rotation and
    /// scale about its pivot. Null when there is none.
    private Matrix paintMatrix() {
        Matrix m = mAnimResidual == null ? null : new Matrix(mAnimResidual);
        if (mScaleX != 1f || mScaleY != 1f || mRotation != 0f) {
            if (m == null) {
                m = new Matrix();
            }
            preConcatProperties(m);
        }
        return m;
    }

    /// Pre-concatenates the view's own rotation and scale about its pivot.
    private void preConcatProperties(Matrix m) {
        float px = Float.isNaN(mPivotX) ? getWidth() / 2f : mPivotX;
        float py = Float.isNaN(mPivotY) ? getHeight() / 2f : mPivotY;
        m.preTranslate(px, py);
        m.preRotate(mRotation);
        m.preScale(mScaleX, mScaleY);
        m.preTranslate(-px, -py);
    }

    /// Maps `pt`, a point in the parent's content coordinates, into this
    /// view's own: the inverse of its position, translation, rotation and
    /// scale. A legacy animation is left out, as on Android, where it moves
    /// only the drawing and the view stays touchable where it was laid out.
    /// Answers false when the matrix cannot be inverted (a zero scale), so
    /// the view covers no area and cannot be touched.
    boolean parentToLocal(float[] pt) {
        pt[0] -= mLeft + mTranslationX;
        pt[1] -= mTop + mTranslationY;
        if (mScaleX == 1f && mScaleY == 1f && mRotation == 0f) {
            return true;
        }
        Matrix m = new Matrix();
        preConcatProperties(m);
        Matrix inverse = new Matrix();
        if (!m.invert(inverse)) {
            return false;
        }
        inverse.mapPoints(pt);
        return true;
    }

    /// The alpha painting applies: the view's own times its animation's.
    private float paintAlpha() {
        return mAlpha * mAnimAlpha;
    }

    /// Drives one view's legacy animation from the frame clock.
    private static final class LegacyAnimationTicker implements FrameDriver.FrameCallback {
        private final View view;

        LegacyAnimationTicker(View view) {
            this.view = view;
        }

        void start() {
            FrameDriver.add(this);
        }

        void stop() {
            FrameDriver.remove(this);
        }

        @Override
        public boolean doFrame(long frameTimeMillis) {
            return view.stepLegacyAnimation();
        }
    }

    // ------------------------------------------------------------ visibility & state

    public int getVisibility() {
        return mVisibility;
    }

    public void setVisibility(int visibility) {
        if (mVisibility == visibility) {
            return;
        }
        boolean gone = mVisibility == GONE || visibility == GONE;
        mVisibility = visibility;
        if (mPeer != null) {
            mPeer.setVisible(peerShown());
        }
        if (gone) {
            requestLayout();
        }
        invalidateParent();
        dispatchVisibilityChanged(this, visibility);
    }

    /// Tells this view, and every descendant through ViewGroup's override,
    /// that `changedView` (this view or an ancestor) changed visibility.
    protected void dispatchVisibilityChanged(View changedView, int visibility) {
        onVisibilityChanged(changedView, visibility);
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
    }

    /// Whether the peer paints: a view that is not VISIBLE still draws while
    /// an animation runs on it, as AOSP's dispatchDraw does, so hiding a
    /// view with an out animation shows the animation.
    private boolean peerShown() {
        return mVisibility == VISIBLE || mCurrentAnimation != null;
    }

    private void syncPeerVisibility() {
        if (mPeer != null && mPeer.isVisible() != peerShown()) {
            mPeer.setVisible(peerShown());
            invalidateParent();
        }
    }

    public boolean isShown() {
        View v = this;
        while (v != null) {
            if (v.mVisibility != VISIBLE) {
                return false;
            }
            ViewParent p = v.mParent;
            if (p == null) {
                return v.mAttached;
            }
            v = p instanceof View ? (View) p : null;
        }
        return true;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            refreshDrawableState();
            invalidate();
        }
    }

    public boolean isClickable() {
        return mClickable;
    }

    public void setClickable(boolean clickable) {
        mClickable = clickable;
    }

    public boolean isLongClickable() {
        return mLongClickable;
    }

    public void setLongClickable(boolean longClickable) {
        mLongClickable = longClickable;
    }

    public boolean isContextClickable() {
        return false;
    }

    public boolean isFocusable() {
        return mFocusable;
    }

    public void setFocusable(boolean focusable) {
        mFocusable = focusable;
        if (!focusable) {
            mFocusableInTouchMode = false;
        }
    }

    public void setFocusable(int focusable) {
        setFocusable(focusable != NOT_FOCUSABLE);
    }

    public int getFocusable() {
        return mFocusable ? FOCUSABLE : NOT_FOCUSABLE;
    }

    public boolean isFocusableInTouchMode() {
        return mFocusableInTouchMode;
    }

    public void setFocusableInTouchMode(boolean f) {
        mFocusableInTouchMode = f;
        if (f) {
            mFocusable = true;
        }
    }

    public boolean isInTouchMode() {
        return true;
    }

    public boolean isFocused() {
        return mFocused;
    }

    public boolean hasFocus() {
        return mFocused;
    }

    public boolean hasFocusable() {
        return mVisibility == VISIBLE && mFocusable;
    }

    public View findFocus() {
        return mFocused ? this : null;
    }

    public boolean isPressed() {
        return mPressed;
    }

    public void setPressed(boolean pressed) {
        if (mPressed != pressed) {
            mPressed = pressed;
            refreshDrawableState();
            dispatchSetPressed(pressed);
        }
    }

    protected void dispatchSetPressed(boolean pressed) {
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        if (mSelected != selected) {
            mSelected = selected;
            refreshDrawableState();
            invalidate();
            dispatchSetSelected(selected);
        }
    }

    protected void dispatchSetSelected(boolean selected) {
    }

    public boolean isActivated() {
        return mActivated;
    }

    public void setActivated(boolean activated) {
        if (mActivated != activated) {
            mActivated = activated;
            refreshDrawableState();
            invalidate();
            dispatchSetActivated(activated);
        }
    }

    protected void dispatchSetActivated(boolean activated) {
    }

    public void setDuplicateParentStateEnabled(boolean enabled) {
        mDuplicateParentState = enabled;
    }

    public boolean isDuplicateParentStateEnabled() {
        return mDuplicateParentState;
    }

    public boolean requestFocus() {
        return requestFocus(FOCUS_DOWN);
    }

    public final boolean requestFocus(int direction) {
        return requestFocus(direction, null);
    }

    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        if (!mFocusable || mVisibility != VISIBLE) {
            return false;
        }
        AndroidRuntime rt = AndroidRuntime.getInstance();
        View old = rt == null ? null : rt.getFocusedView();
        if (old == this) {
            return true;
        }
        if (old != null) {
            old.setFocusedInternal(false);
        }
        setFocusedInternal(true);
        if (rt != null) {
            rt.setFocusedView(this);
        }
        if (mParent != null) {
            mParent.requestChildFocus(this, this);
        }
        return true;
    }

    public void clearFocus() {
        if (mFocused) {
            setFocusedInternal(false);
            AndroidRuntime rt = AndroidRuntime.getInstance();
            if (rt != null && rt.getFocusedView() == this) {
                rt.setFocusedView(null);
            }
        }
    }

    /// Runtime use: flips the focused flag and notifies listeners.
    public void setFocusedInternal(boolean focused) {
        if (mFocused == focused) {
            return;
        }
        mFocused = focused;
        refreshDrawableState();
        onFocusChanged(focused, FOCUS_DOWN, null);
        if (mOnFocusChangeListener != null) {
            mOnFocusChangeListener.onFocusChange(this, focused);
        }
        invalidate();
    }

    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    }

    public View focusSearch(int direction) {
        return null;
    }

    public void setNextFocusDownId(int id) {
    }

    public void setNextFocusForwardId(int id) {
    }

    public int getImportantForAccessibility() {
        return mImportantForAccessibility;
    }

    public void setImportantForAccessibility(int mode) {
        mImportantForAccessibility = mode;
    }

    public void setImportantForAutofill(int mode) {
    }

    public void setAutofillHints(String... hints) {
    }

    public void setAccessibilityLiveRegion(int mode) {
    }

    public void announceForAccessibility(CharSequence text) {
    }

    public void sendAccessibilityEvent(int eventType) {
    }

    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    public void setContentDescription(CharSequence contentDescription) {
        mContentDescription = contentDescription;
    }

    public void setTooltipText(CharSequence text) {
    }

    // ------------------------------------------------------------ layout direction

    public int getLayoutDirection() {
        if (mLayoutDirection == LAYOUT_DIRECTION_RTL || mLayoutDirection == LAYOUT_DIRECTION_LTR) {
            return mLayoutDirection;
        }
        if (mLayoutDirection == LAYOUT_DIRECTION_LOCALE) {
            return getResources().getConfiguration().getLayoutDirection();
        }
        if (mParent instanceof View) {
            return ((View) mParent).getLayoutDirection();
        }
        return getResources().getConfiguration().getLayoutDirection();
    }

    public int getRawLayoutDirection() {
        return mLayoutDirection;
    }

    public void setLayoutDirection(int layoutDirection) {
        mLayoutDirection = layoutDirection;
        dispatchLayoutDirectionChanged(true);
    }

    /// Re-resolves this view's start/end padding against its current
    /// direction and relays it to every descendant that inherits the
    /// direction (ViewGroup's override), so a parent switching to RTL
    /// mirrors the whole inheriting subtree, not only itself. Each view is
    /// asked to lay out again: a child is only re-measured when forced, and
    /// a nested group re-resolves its children's margins when it measures.
    /// `relayout` is false when the caller lays the subtree out anyway.
    void dispatchLayoutDirectionChanged(boolean relayout) {
        resolvePaddingStartEnd();
        onRtlPropertiesChanged(getLayoutDirection());
        if (relayout) {
            requestLayout();
            invalidate();
        }
    }

    /// Called when this view's resolved layout direction may have changed,
    /// directly or through a parent it inherits the direction from.
    public void onRtlPropertiesChanged(int layoutDirection) {
    }

    public boolean isLayoutRtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    public int getTextAlignment() {
        return mTextAlignment;
    }

    public void setTextAlignment(int textAlignment) {
        mTextAlignment = textAlignment;
        requestLayout();
        invalidate();
    }

    public int getTextDirection() {
        return TEXT_DIRECTION_LOCALE;
    }

    public void setTextDirection(int textDirection) {
    }

    // ------------------------------------------------------------ padding

    public void setPadding(int left, int top, int right, int bottom) {
        mUserPaddingStart = Integer.MIN_VALUE;
        mUserPaddingEnd = Integer.MIN_VALUE;
        internalSetPadding(left, top, right, bottom);
    }

    protected void internalSetPadding(int left, int top, int right, int bottom) {
        boolean changed = mPaddingLeft != left || mPaddingTop != top || mPaddingRight != right
                || mPaddingBottom != bottom;
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
        if (changed) {
            requestLayout();
            invalidate();
        }
    }

    public void setPaddingRelative(int start, int top, int end, int bottom) {
        boolean rtl = isLayoutRtl();
        internalSetPadding(rtl ? end : start, top, rtl ? start : end, bottom);
        mUserPaddingStart = start;
        mUserPaddingEnd = end;
    }

    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public int getPaddingRight() {
        return mPaddingRight;
    }

    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public int getPaddingStart() {
        return isLayoutRtl() ? mPaddingRight : mPaddingLeft;
    }

    public int getPaddingEnd() {
        return isLayoutRtl() ? mPaddingLeft : mPaddingRight;
    }

    public boolean isPaddingRelative() {
        return mUserPaddingStart != Integer.MIN_VALUE || mUserPaddingEnd != Integer.MIN_VALUE;
    }

    public int getMinimumWidth() {
        return mMinWidth;
    }

    public void setMinimumWidth(int minWidth) {
        mMinWidth = minWidth;
        requestLayout();
    }

    public int getMinimumHeight() {
        return mMinHeight;
    }

    public void setMinimumHeight(int minHeight) {
        mMinHeight = minHeight;
        requestLayout();
    }

    // ------------------------------------------------------------ background / foreground

    public Drawable getBackground() {
        return mBackground;
    }

    public void setBackground(Drawable background) {
        if (background == mBackground) {
            return;
        }
        if (mBackground != null) {
            mBackground.setCallback(null);
        }
        mBackground = background;
        if (background != null) {
            Rect padding = mTmpRect;
            if (background.getPadding(padding)) {
                internalSetPadding(padding.left, padding.top, padding.right, padding.bottom);
            }
            background.setCallback(this);
            if (background.isStateful()) {
                background.setState(getDrawableState());
            }
            background.setVisible(mVisibility == VISIBLE, false);
            if (mBackgroundTint != null) {
                background.setTintList(mBackgroundTint);
            }
            if (mBackgroundTintMode != null) {
                background.setTintMode(mBackgroundTintMode);
            }
        }
        requestLayout();
        invalidate();
    }

    @Deprecated
    public void setBackgroundDrawable(Drawable background) {
        setBackground(background);
    }

    public void setBackgroundColor(int color) {
        if (mBackground instanceof ColorDrawable) {
            ((ColorDrawable) mBackground.mutate()).setColor(color);
            invalidate();
        } else {
            setBackground(new ColorDrawable(color));
        }
    }

    public void setBackgroundResource(int resid) {
        setBackground(resid == 0 ? null : mContext.getDrawable(resid));
    }

    public void setBackgroundTintList(ColorStateList tint) {
        mBackgroundTint = tint;
        if (mBackground != null) {
            mBackground.setTintList(tint);
        }
    }

    public ColorStateList getBackgroundTintList() {
        return mBackgroundTint;
    }

    /// Kept on the view, like the tint list, so a replacement background
    /// is tinted the same way.
    public void setBackgroundTintMode(PorterDuff.Mode mode) {
        mBackgroundTintMode = mode;
        if (mBackground != null) {
            mBackground.setTintMode(mode);
        }
    }

    public PorterDuff.Mode getBackgroundTintMode() {
        return mBackgroundTintMode;
    }

    public Drawable getForeground() {
        return mForeground;
    }

    public void setForeground(Drawable foreground) {
        if (mForeground != null) {
            mForeground.setCallback(null);
        }
        mForeground = foreground;
        if (foreground != null) {
            foreground.setCallback(this);
            if (foreground.isStateful()) {
                foreground.setState(getDrawableState());
            }
        }
        invalidate();
    }

    public void setForegroundGravity(int gravity) {
    }

    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground || who == mForeground;
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (verifyDrawable(drawable)) {
            invalidate();
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (verifyDrawable(who) && what != null) {
            long delay = when - android.os.SystemClock.uptimeMillis();
            postDelayed(what, Math.max(0, delay));
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        removeCallbacks(what);
    }

    public void unscheduleDrawable(Drawable who) {
    }

    // ------------------------------------------------------------ drawable state

    private int[] mDrawableState;

    public final int[] getDrawableState() {
        if (mDrawableState == null) {
            mDrawableState = onCreateDrawableState(0);
        }
        return mDrawableState;
    }

    protected int[] onCreateDrawableState(int extraSpace) {
        if (mDuplicateParentState && mParent instanceof View) {
            return ((View) mParent).onCreateDrawableState(extraSpace);
        }
        int n = 0;
        int[] tmp = new int[8 + extraSpace];
        if (mPressed) {
            tmp[n++] = android.R.attr.state_pressed;
        }
        if (mEnabled) {
            tmp[n++] = android.R.attr.state_enabled;
        }
        if (mFocused) {
            tmp[n++] = android.R.attr.state_focused;
        }
        if (mSelected) {
            tmp[n++] = android.R.attr.state_selected;
        }
        if (mActivated) {
            tmp[n++] = android.R.attr.state_activated;
        }
        tmp[n++] = android.R.attr.state_window_focused;
        int[] out = new int[n + extraSpace];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    protected static int[] mergeDrawableStates(int[] baseState, int[] additionalState) {
        int i = baseState.length - 1;
        while (i >= 0 && baseState[i] == 0) {
            i--;
        }
        System.arraycopy(additionalState, 0, baseState, i + 1, additionalState.length);
        return baseState;
    }

    public void refreshDrawableState() {
        mDrawableState = null;
        drawableStateChanged();
        if (mParent != null) {
            mParent.childDrawableStateChanged(this);
        }
    }

    protected void drawableStateChanged() {
        int[] state = getDrawableState();
        boolean changed = false;
        if (mBackground != null && mBackground.isStateful()) {
            changed |= mBackground.setState(state);
        }
        if (mForeground != null && mForeground.isStateful()) {
            changed |= mForeground.setState(state);
        }
        if (changed) {
            invalidate();
        }
    }

    public void jumpDrawablesToCurrentState() {
        if (mBackground != null) {
            mBackground.jumpToCurrentState();
        }
    }

    public void drawableHotspotChanged(float x, float y) {
        if (mBackground != null) {
            mBackground.setHotspot(x, y);
        }
    }

    // ------------------------------------------------------------ layout params

    public ViewGroup.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params == null) {
            throw new NullPointerException("Layout parameters cannot be null");
        }
        mLayoutParams = params;
        if (mParent instanceof ViewGroup) {
            ((ViewGroup) mParent).onSetLayoutParams(this, params);
        }
        requestLayout();
    }

    // ------------------------------------------------------------ measure & layout

    /// Before a view group measures: its children's layout parameters are
    /// resolved for the layout direction, as Android's RTL resolution does
    /// before onMeasure. A view has no children.
    void resolveChildLayoutParams() {
    }

    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean specChanged = widthMeasureSpec != mOldWidthMeasureSpec || heightMeasureSpec != mOldHeightMeasureSpec;
        if (mForceLayout || specChanged) {
            mMeasureCalled = false;
            resolveChildLayoutParams();
            onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (!mMeasureCalled) {
                throw new IllegalStateException(getClass().getName()
                        + "#onMeasure() did not set the measured dimension by calling setMeasuredDimension()");
            }
            mLayoutRequested = true;
        }
        mOldWidthMeasureSpec = widthMeasureSpec;
        mOldHeightMeasureSpec = heightMeasureSpec;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        mMeasuredWidth = measuredWidth;
        mMeasuredHeight = measuredHeight;
        mMeasureCalled = true;
    }

    public final int getMeasuredWidth() {
        return mMeasuredWidth & MEASURED_SIZE_MASK;
    }

    public final int getMeasuredHeight() {
        return mMeasuredHeight & MEASURED_SIZE_MASK;
    }

    public final int getMeasuredWidthAndState() {
        return mMeasuredWidth;
    }

    public final int getMeasuredHeightAndState() {
        return mMeasuredHeight;
    }

    public final int getMeasuredState() {
        return (mMeasuredWidth & MEASURED_STATE_MASK)
                | ((mMeasuredHeight >> MEASURED_HEIGHT_STATE_SHIFT) & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));
    }

    public static int combineMeasuredStates(int curState, int newState) {
        return curState | newState;
    }

    public static int resolveSize(int size, int measureSpec) {
        return resolveSizeAndState(size, measureSpec, 0) & MEASURED_SIZE_MASK;
    }

    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        final int result;
        switch (specMode) {
            case MeasureSpec.AT_MOST:
                if (specSize < size) {
                    result = specSize | MEASURED_STATE_TOO_SMALL;
                } else {
                    result = size;
                }
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
            default:
                result = size;
        }
        return result | (childMeasuredState & MEASURED_STATE_MASK);
    }

    public static int getDefaultSize(int size, int measureSpec) {
        if (MeasureSpec.getMode(measureSpec) == MeasureSpec.UNSPECIFIED) {
            return size;
        }
        return MeasureSpec.getSize(measureSpec);
    }

    protected int getSuggestedMinimumWidth() {
        return mBackground == null ? mMinWidth : Math.max(mMinWidth, mBackground.getMinimumWidth());
    }

    protected int getSuggestedMinimumHeight() {
        return mBackground == null ? mMinHeight : Math.max(mMinHeight, mBackground.getMinimumHeight());
    }

    public int getBaseline() {
        return -1;
    }

    public void layout(int l, int t, int r, int b) {
        int oldL = mLeft;
        int oldT = mTop;
        int oldR = mRight;
        int oldB = mBottom;
        boolean changed = setFrame(l, t, r, b);
        if (changed || mLayoutRequested) {
            onLayout(changed, l, t, r, b);
            if (mLayoutChangeListeners != null) {
                ArrayList<OnLayoutChangeListener> copy = new ArrayList<OnLayoutChangeListener>(mLayoutChangeListeners);
                for (OnLayoutChangeListener li : copy) {
                    li.onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
                }
            }
        }
        mLayoutRequested = false;
        mForceLayout = false;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (mLeft == left && mRight == right && mTop == top && mBottom == bottom) {
            syncPeerBounds();
            return false;
        }
        int oldWidth = mRight - mLeft;
        int oldHeight = mBottom - mTop;
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
        syncPeerBounds();
        int newWidth = right - left;
        int newHeight = bottom - top;
        if (newWidth != oldWidth || newHeight != oldHeight) {
            if (mBackground != null) {
                mBackground.setBounds(0, 0, newWidth, newHeight);
            }
            if (mForeground != null) {
                mForeground.setBounds(0, 0, newWidth, newHeight);
            }
            onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        }
        invalidate();
        return true;
    }

    /// Pushes the frame (plus translation) onto the peer.
    void syncPeerBounds() {
        if (mPeer != null && !(mParent == null && ViewPeer.isRootPlaced(mPeer))) {
            int sx = mParent instanceof View ? ((View) mParent).mScrollX : 0;
            int sy = mParent instanceof View ? ((View) mParent).mScrollY : 0;
            mPeer.setX(mLeft + Math.round(mTranslationX + mAnimDx) - sx);
            mPeer.setY(mTop + Math.round(mTranslationY + mAnimDy) - sy);
            mPeer.setWidth(mRight - mLeft);
            mPeer.setHeight(mBottom - mTop);
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    }

    public void requestLayout() {
        mForceLayout = true;
        mLayoutRequested = true;
        if (mParent != null) {
            if (!mParent.isLayoutRequested()) {
                mParent.requestLayout();
            }
        } else if (mPeer != null) {
            ViewPeer.scheduleLayout(mPeer);
        }
    }

    public void forceLayout() {
        mForceLayout = true;
        mLayoutRequested = true;
    }

    public boolean isLayoutRequested() {
        return mForceLayout;
    }

    public boolean isInLayout() {
        return false;
    }

    public boolean isLaidOut() {
        return getWidth() > 0 || getHeight() > 0 || !mLayoutRequested;
    }

    public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
        if (mLayoutChangeListeners == null) {
            mLayoutChangeListeners = new ArrayList<OnLayoutChangeListener>();
        }
        if (!mLayoutChangeListeners.contains(listener)) {
            mLayoutChangeListeners.add(listener);
        }
    }

    public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
        if (mLayoutChangeListeners != null) {
            mLayoutChangeListeners.remove(listener);
        }
    }

    // ------------------------------------------------------------ drawing

    public void invalidate() {
        if (mPeer == null) {
            return;
        }
        // A rotated or scaled view paints outside its bounds, and so did its
        // previous frame: the parent repaints, which covers both.
        if ((mPaintedTransformed || paintMatrix() != null) && mPeer.getParent() != null) {
            mPeer.getParent().repaint();
        } else {
            mPeer.repaint();
        }
    }

    public void invalidate(Rect dirty) {
        invalidate();
    }

    public void invalidate(int l, int t, int r, int b) {
        invalidate();
    }

    public void postInvalidate() {
        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    public void postInvalidateDelayed(long delayMilliseconds) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        }, delayMilliseconds);
    }

    public void postInvalidateOnAnimation() {
        postInvalidate();
    }

    void invalidateParent() {
        if (mParent instanceof View) {
            ((View) mParent).invalidate();
        } else {
            invalidate();
        }
    }

    public void setWillNotDraw(boolean willNotDraw) {
        mWillNotDraw = willNotDraw;
    }

    public boolean willNotDraw() {
        return mWillNotDraw;
    }

    public void setLayerType(int layerType, android.graphics.Paint paint) {
    }

    public int getLayerType() {
        return LAYER_TYPE_NONE;
    }

    public void setClipToOutline(boolean clipToOutline) {
    }

    public void setOutlineProvider(ViewOutlineProvider provider) {
    }

    public void setDrawingCacheEnabled(boolean enabled) {
    }

    public void buildDrawingCache() {
    }

    /// Android's draw order: background, content, children, foreground.
    /// Children of a view group are painted by their own peers, between
    /// [#onDraw(Canvas)] and the foreground; see `GroupPeer`.
    public void draw(Canvas canvas) {
        drawBackground(canvas);
        if (!skipsDraw()) {
            onDraw(canvas);
        }
        dispatchDraw(canvas);
        onDrawForeground(canvas);
    }

    /// Draws the background, translated by the scroll position like Android.
    public void drawBackground(Canvas canvas) {
        Drawable bg = mBackground;
        if (bg == null) {
            return;
        }
        if (bg.getBounds().width() != getWidth() || bg.getBounds().height() != getHeight()) {
            bg.setBounds(0, 0, getWidth(), getHeight());
        }
        if (mScrollX == 0 && mScrollY == 0) {
            bg.draw(canvas);
        } else {
            canvas.save();
            canvas.translate(mScrollX, mScrollY);
            bg.draw(canvas);
            canvas.restore();
        }
    }

    protected void onDraw(Canvas canvas) {
    }

    protected void dispatchDraw(Canvas canvas) {
    }

    public void onDrawForeground(Canvas canvas) {
        Drawable fg = mForeground;
        if (fg != null) {
            if (fg.getBounds().width() != getWidth() || fg.getBounds().height() != getHeight()) {
                fg.setBounds(0, 0, getWidth(), getHeight());
            }
            fg.draw(canvas);
        }
    }

    // ------------------------------------------------------------ scrolling

    public final int getScrollX() {
        return mScrollX;
    }

    public final int getScrollY() {
        return mScrollY;
    }

    public void setScrollX(int value) {
        scrollTo(value, mScrollY);
    }

    public void setScrollY(int value) {
        scrollTo(mScrollX, value);
    }

    public void scrollTo(int x, int y) {
        if (mScrollX != x || mScrollY != y) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = x;
            mScrollY = y;
            // Scrolling moves the children's peers rather than using the
            // container's own scroll, whose clamping and tensile behaviour are
            // Codename One's, not Android's.
            if (this instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) this;
                for (int i = 0; i < g.getChildCount(); i++) {
                    g.getChildAt(i).syncPeerBounds();
                }
            }
            onScrollChanged(x, y, oldX, oldY);
            if (mOnScrollChangeListener != null) {
                mOnScrollChangeListener.onScrollChange(this, x, y, oldX, oldY);
            }
            invalidate();
        }
    }

    public void scrollBy(int x, int y) {
        scrollTo(mScrollX + x, mScrollY + y);
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    }

    public void setOnScrollChangeListener(OnScrollChangeListener l) {
        mOnScrollChangeListener = l;
    }

    public boolean canScrollVertically(int direction) {
        final int offset = computeVerticalScrollOffset();
        final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (range == 0) {
            return false;
        }
        if (direction < 0) {
            return offset > 0;
        }
        return offset < range - 1;
    }

    public boolean canScrollHorizontally(int direction) {
        final int offset = computeHorizontalScrollOffset();
        final int range = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
        if (range == 0) {
            return false;
        }
        if (direction < 0) {
            return offset > 0;
        }
        return offset < range - 1;
    }

    protected int computeVerticalScrollRange() {
        return getHeight();
    }

    protected int computeVerticalScrollOffset() {
        return mScrollY;
    }

    protected int computeVerticalScrollExtent() {
        return getHeight();
    }

    protected int computeHorizontalScrollRange() {
        return getWidth();
    }

    protected int computeHorizontalScrollOffset() {
        return mScrollX;
    }

    protected int computeHorizontalScrollExtent() {
        return getWidth();
    }

    public void computeScroll() {
    }

    public void setVerticalScrollBarEnabled(boolean enabled) {
    }

    public void setHorizontalScrollBarEnabled(boolean enabled) {
    }

    public boolean isVerticalScrollBarEnabled() {
        return false;
    }

    public void setScrollBarStyle(int style) {
    }

    public void setScrollbarFadingEnabled(boolean enabled) {
    }

    public int getOverScrollMode() {
        return mOverScrollMode;
    }

    public void setOverScrollMode(int mode) {
        mOverScrollMode = mode;
    }

    public void setNestedScrollingEnabled(boolean enabled) {
    }

    public boolean isNestedScrollingEnabled() {
        return false;
    }

    // ------------------------------------------------------------ touch

    public void setOnClickListener(OnClickListener l) {
        // Deliberately unconditional, null included: Android's own
        // View.setOnClickListener makes the view clickable for any argument,
        // and apps that clear a listener also call setClickable(false) for
        // exactly that reason. Diverging would change which view consumes a
        // touch relative to the real platform.
        if (!isClickable()) {
            setClickable(true);
        }
        mOnClickListener = l;
    }

    public boolean hasOnClickListeners() {
        return mOnClickListener != null;
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        mOnLongClickListener = l;
    }

    public void setOnTouchListener(OnTouchListener l) {
        mOnTouchListener = l;
    }

    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        mOnFocusChangeListener = l;
    }

    public OnFocusChangeListener getOnFocusChangeListener() {
        return mOnFocusChangeListener;
    }

    public void setOnKeyListener(OnKeyListener l) {
        mOnKeyListener = l;
    }

    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        mOnCreateContextMenuListener = l;
        setLongClickable(true);
    }

    public void setOnGenericMotionListener(OnGenericMotionListener l) {
    }

    public void setOnHoverListener(OnHoverListener l) {
    }

    public void setOnDragListener(OnDragListener l) {
    }

    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l) {
    }

    public boolean performClick() {
        boolean handled = false;
        if (mOnClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            mOnClickListener.onClick(this);
            handled = true;
        } else if (mOnClickMethod != null) {
            AndroidRuntime rt = AndroidRuntime.getInstance();
            if (rt != null) {
                handled = rt.dispatchOnClick(this, mOnClickMethod);
            }
        }
        return handled;
    }

    public boolean callOnClick() {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(this);
            return true;
        }
        return false;
    }

    public boolean performLongClick() {
        boolean handled = false;
        if (mOnLongClickListener != null) {
            handled = mOnLongClickListener.onLongClick(this);
        }
        if (!handled && mOnCreateContextMenuListener != null) {
            handled = showContextMenu();
        }
        return handled;
    }

    public boolean performLongClick(float x, float y) {
        return performLongClick();
    }

    public boolean showContextMenu() {
        return mParent != null && mParent.showContextMenuForChild(this);
    }

    public void createContextMenu(ContextMenu menu) {
        if (mOnCreateContextMenuListener != null) {
            mOnCreateContextMenuListener.onCreateContextMenu(menu, this, null);
        }
    }

    public boolean performHapticFeedback(int feedbackConstant) {
        return false;
    }

    public void setHapticFeedbackEnabled(boolean enabled) {
    }

    public void setSoundEffectsEnabled(boolean enabled) {
    }

    public void playSoundEffect(int soundConstant) {
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mOnTouchListener != null && mEnabled && mOnTouchListener.onTouch(this, event)) {
            return true;
        }
        return onTouchEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        final boolean clickable = mClickable || mLongClickable;
        if (!mEnabled) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP && mPressed) {
                setPressed(false);
            }
            return clickable;
        }
        if (!clickable) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mHasPerformedLongPress = false;
                setPressed(true);
                drawableHotspotChanged(x, y);
                if (mLongClickable) {
                    checkForLongClick();
                }
                if (mFocusableInTouchMode && !mFocused) {
                    requestFocus();
                }
                break;
            case MotionEvent.ACTION_MOVE: {
                int slop = ViewConfiguration.get(mContext).getScaledTouchSlop();
                if (x < -slop || y < -slop || x >= getWidth() + slop || y >= getHeight() + slop) {
                    removeLongPressCallback();
                    setPressed(false);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                if (mPressed) {
                    removeLongPressCallback();
                    if (!mHasPerformedLongPress) {
                        performClick();
                    }
                    setPressed(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                removeLongPressCallback();
                setPressed(false);
                break;
            default:
                break;
        }
        return true;
    }

    private void checkForLongClick() {
        removeLongPressCallback();
        mPendingLongPress = new Runnable() {
            @Override
            public void run() {
                if (mPressed && mParent != null) {
                    if (performLongClick()) {
                        mHasPerformedLongPress = true;
                    }
                }
            }
        };
        postDelayed(mPendingLongPress, ViewConfiguration.getLongPressTimeout());
    }

    private void removeLongPressCallback() {
        if (mPendingLongPress != null) {
            removeCallbacks(mPendingLongPress);
            mPendingLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mOnKeyListener != null && mOnKeyListener.onKey(this, event.getKeyCode(), event)) {
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return onKeyDown(event.getKeyCode(), event);
        }
        return onKeyUp(event.getKeyCode(), event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onCheckIsTextEditor() {
        return false;
    }

    // ------------------------------------------------------------ attach & threads

    public boolean isAttachedToWindow() {
        return mAttached;
    }

    /// Runtime use: attaches or detaches this subtree.
    public void dispatchAttachedToWindow(boolean attached) {
        if (mAttached == attached) {
            return;
        }
        mAttached = attached;
        if (mKeepScreenOn) {
            updateKeepScreenOn(attached ? 1 : -1);
        }
        if (attached) {
            runPendingActions();
            onAttachedToWindow();
            if (mAttachListeners != null) {
                for (OnAttachStateChangeListener l : new ArrayList<OnAttachStateChangeListener>(mAttachListeners)) {
                    l.onViewAttachedToWindow(this);
                }
            }
        } else {
            if (mAttachListeners != null) {
                for (OnAttachStateChangeListener l : new ArrayList<OnAttachStateChangeListener>(mAttachListeners)) {
                    l.onViewDetachedFromWindow(this);
                }
            }
            removeLongPressCallback();
            onDetachedFromWindow();
        }
    }

    protected void onAttachedToWindow() {
    }

    protected void onDetachedFromWindow() {
    }

    public void addOnAttachStateChangeListener(OnAttachStateChangeListener l) {
        if (mAttachListeners == null) {
            mAttachListeners = new ArrayList<OnAttachStateChangeListener>();
        }
        mAttachListeners.add(l);
    }

    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener l) {
        if (mAttachListeners != null) {
            mAttachListeners.remove(l);
        }
    }

    protected void onFinishInflate() {
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
    }

    /// Tells this view, and a view group's whole subtree, that its window
    /// gained or lost focus: an activity covered by another one loses it.
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        mWindowFocusLost = !hasFocus;
        onWindowFocusChanged(hasFocus);
        if (mParent == null && mTreeObserver != null) {
            // The root reports once for the whole tree. Android notifies the
            // observer before the views; here it runs after the root's own
            // callback and before a root group's children, which only
            // matters to a listener comparing call order across views.
            mTreeObserver.dispatchOnWindowFocusChanged(hasFocus);
        }
    }

    /// Whether the window holding this view has focus. The root's state is
    /// the window's; a root nothing has reported on (a dialog's or a popup's)
    /// counts as focused while it is attached.
    public boolean hasWindowFocus() {
        return mAttached && !getRootView().mWindowFocusLost;
    }

    public void onWindowVisibilityChanged(int visibility) {
    }

    public int getWindowVisibility() {
        return mAttached ? VISIBLE : GONE;
    }

    public Object getWindowToken() {
        return mAttached ? this : null;
    }

    public Object getApplicationWindowToken() {
        return getWindowToken();
    }

    public Display getDisplay() {
        return Display.DEFAULT;
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
    }

    /// The main-thread handler every view posts through, as the views of an
    /// Android window share their root's: `removeCallbacks` has to search
    /// the queue `post` used, and a handler made per call left delayed work
    /// impossible to cancel.
    private static Handler sHandler;

    private static Handler mainHandler() {
        if (sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper());
        }
        return sHandler;
    }

    /// Null while the view is not attached to a window, as on Android.
    public Handler getHandler() {
        return mAttached ? mainHandler() : null;
    }

    /// Work posted while the view is detached, held until it is attached
    /// (Android's per-view run queue): a freshly inflated view's posts run
    /// once the view has a window, not before. The delay counts from the
    /// attach, as Android's does.
    private ArrayList<PendingAction> mPendingActions;

    private static final class PendingAction {
        final Runnable action;
        final long delayMillis;

        PendingAction(Runnable action, long delayMillis) {
            this.action = action;
            this.delayMillis = delayMillis;
        }
    }

    private void runPendingActions() {
        ArrayList<PendingAction> pending = mPendingActions;
        if (pending == null) {
            return;
        }
        mPendingActions = null;
        Handler h = mainHandler();
        for (PendingAction p : pending) {
            h.postDelayed(p.action, p.delayMillis);
        }
    }

    public boolean post(Runnable action) {
        return postDelayed(action, 0);
    }

    public boolean postDelayed(Runnable action, long delayMillis) {
        if (mAttached) {
            return mainHandler().postDelayed(action, delayMillis);
        }
        if (mPendingActions == null) {
            mPendingActions = new ArrayList<PendingAction>();
        }
        mPendingActions.add(new PendingAction(action, Math.max(0, delayMillis)));
        return true;
    }

    public void postOnAnimation(Runnable action) {
        post(action);
    }

    public void postOnAnimationDelayed(Runnable action, long delayMillis) {
        postDelayed(action, delayMillis);
    }

    public boolean removeCallbacks(Runnable action) {
        if (action == null) {
            return true;
        }
        if (mPendingActions != null) {
            for (int i = mPendingActions.size() - 1; i >= 0; i--) {
                if (mPendingActions.get(i).action == action) {
                    mPendingActions.remove(i);
                }
            }
        }
        mainHandler().removeCallbacks(action);
        return true;
    }

    public ViewTreeObserver getViewTreeObserver() {
        View root = getRootView();
        if (root.mTreeObserver == null) {
            root.mTreeObserver = new ViewTreeObserver();
        }
        return root.mTreeObserver;
    }

    ViewTreeObserver mTreeObserver;

    /// Runtime use: the tree's observer if anyone asked for one.
    public ViewTreeObserver peekViewTreeObserver() {
        return mTreeObserver;
    }

    /// This view's own keep-screen-on request.
    private boolean mKeepScreenOn;

    /// How many attached views request the screen stay on. The screen saver
    /// is held off while any does, so one view clearing its request (or
    /// detaching) does not override another's, as on Android where the flag
    /// is aggregated over the window. Views of a covered activity stay
    /// attached and keep counting; that is deliberate, not tracked further.
    private static int sKeepScreenOnViews;

    public void setKeepScreenOn(boolean keepScreenOn) {
        if (mKeepScreenOn == keepScreenOn) {
            return;
        }
        mKeepScreenOn = keepScreenOn;
        if (mAttached) {
            updateKeepScreenOn(keepScreenOn ? 1 : -1);
        }
    }

    public boolean getKeepScreenOn() {
        return mKeepScreenOn;
    }

    private static void updateKeepScreenOn(int delta) {
        int before = sKeepScreenOnViews;
        sKeepScreenOnViews = Math.max(0, before + delta);
        if ((before == 0) != (sKeepScreenOnViews == 0)) {
            com.codename1.ui.Display.getInstance().setScreenSaverEnabled(sKeepScreenOnViews == 0);
        }
    }

    public void setSystemUiVisibility(int visibility) {
        mSystemUiVisibility = visibility;
    }

    public int getSystemUiVisibility() {
        return mSystemUiVisibility;
    }

    public void setFitsSystemWindows(boolean fitSystemWindows) {
    }

    public boolean getFitsSystemWindows() {
        return false;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        return insets;
    }

    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener listener) {
    }

    public void requestApplyInsets() {
    }

    public interface OnApplyWindowInsetsListener {
        WindowInsets onApplyWindowInsets(View v, WindowInsets insets);
    }

    public void setSaveEnabled(boolean enabled) {
    }

    public boolean isSaveEnabled() {
        return true;
    }

    protected Parcelable onSaveInstanceState() {
        return null;
    }

    protected void onRestoreInstanceState(Parcelable state) {
    }

    public void saveHierarchyState(SparseArray<Parcelable> container) {
    }

    public void restoreHierarchyState(SparseArray<Parcelable> container) {
    }

    public boolean isHardwareAccelerated() {
        return false;
    }

    public boolean isInEditMode() {
        return false;
    }

    public boolean isDirty() {
        return false;
    }

    public boolean isOpaque() {
        return false;
    }

    public boolean hasOverlappingRendering() {
        return true;
    }

    public void bringToFront() {
        if (mParent != null) {
            mParent.bringChildToFront(this);
        }
    }

    @Override
    public String toString() {
        String name = getClass().getName();
        int dot = name.lastIndexOf('.');
        StringBuilder sb = new StringBuilder(dot >= 0 ? name.substring(dot + 1) : name);
        sb.append('{').append(mVisibility == VISIBLE ? 'V' : mVisibility == INVISIBLE ? 'I' : 'G');
        sb.append(' ').append(mLeft).append(',').append(mTop).append('-').append(mRight).append(',').append(mBottom);
        if (mID != NO_ID) {
            sb.append(" #").append(Integer.toHexString(mID));
            try {
                sb.append(' ').append(getResources().getResourceEntryName(mID));
            } catch (RuntimeException ignored) {
                // A generated id has no name.
            }
        }
        return sb.append('}').toString();
    }
}
