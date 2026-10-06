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
package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

/// A window floating over the activity. Its content is an Android view tree
/// hosted in a modeless Codename One dialog, so it does not block the caller,
/// as on Android.
public class Dialog implements DialogInterface, Window.Callback {

    private final Context mContext;
    private final Window mWindow;
    private final DialogFrame mDecor;
    private final FrameLayout mContent;
    private com.codename1.ui.Dialog mHost;
    private boolean mCreated;
    private boolean mShowing;
    private boolean mCancelable = true;
    private boolean mCanceledOnTouchOutside = true;
    /// Set by the first `cancel()` and cleared by the next `show()`, as on
    /// Android: a second `cancel()` before that dismisses nothing new, so it
    /// must not run the `OnCancelListener` again.
    private boolean mCanceled;
    private CharSequence mTitle;
    private OnCancelListener mCancelListener;
    private OnDismissListener mDismissListener;
    private OnShowListener mShowListener;
    private Activity mOwner;

    public Dialog(Context context) {
        this(context, 0);
    }

    public Dialog(Context context, int themeResId) {
        mContext = new ContextThemeWrapper(context, themeResId != 0 ? themeResId : dialogTheme(context));
        mWindow = new Window(mContext);
        mDecor = new DialogFrame(mContext);
        mContent = new FrameLayout(mContext);
        mContent.setId(android.R.id.content);
        mDecor.addView(mContent, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mWindow.attach(mDecor, null);
        android.util.TypedValue tv = new android.util.TypedValue();
        int bg = 0xfffafafa;
        if (mContext.getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true)
                && tv.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT) {
            bg = tv.data;
        }
        mDecor.setBackground(new ColorDrawable(bg));
        if (context instanceof Activity) {
            mOwner = (Activity) context;
        }
    }

    protected Dialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        this(context);
        mCancelable = cancelable;
        mCancelListener = cancelListener;
    }

    private static int dialogTheme(Context c) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.dialogTheme, tv, true) && tv.resourceId != 0) {
            return tv.resourceId;
        }
        return 0;
    }

    public final Context getContext() {
        return mContext;
    }

    public final void setOwnerActivity(Activity activity) {
        mOwner = activity;
    }

    public final Activity getOwnerActivity() {
        return mOwner;
    }

    public Window getWindow() {
        return mWindow;
    }

    public LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(mContext);
    }

    protected void onCreate(Bundle savedInstanceState) {
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    public void create() {
        if (!mCreated) {
            mCreated = true;
            onCreate(null);
        }
    }

    public void setContentView(int layoutResID) {
        mContent.removeAllViews();
        getLayoutInflater().inflate(layoutResID, mContent, true);
    }

    public void setContentView(View view) {
        mContent.removeAllViews();
        mContent.addView(view);
    }

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mContent.removeAllViews();
        mContent.addView(view, params);
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        mContent.addView(view, params);
    }

    public <T extends View> T findViewById(int id) {
        return mDecor.findViewById(id);
    }

    public final <T extends View> T requireViewById(int id) {
        T v = findViewById(id);
        if (v == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Dialog");
        }
        return v;
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    public void setTitle(int titleId) {
        setTitle(mContext.getText(titleId));
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public void setCancelable(boolean flag) {
        mCancelable = flag;
    }

    public boolean isCancelable() {
        return mCancelable;
    }

    public void setCanceledOnTouchOutside(boolean cancel) {
        if (cancel && !mCancelable) {
            mCancelable = true;
        }
        mCanceledOnTouchOutside = cancel;
    }

    public void setOnCancelListener(OnCancelListener listener) {
        mCancelListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        mDismissListener = listener;
    }

    public void setOnShowListener(OnShowListener listener) {
        mShowListener = listener;
    }

    /// False after `hide()`, as on Android, which answers from the decor's
    /// visibility. `mShowing` stays true across a hide so that `show()`
    /// reuses the host and `dismiss()` still tears it down.
    public boolean isShowing() {
        return mShowing && mHost != null && mHost.isVisible();
    }

    public void show() {
        if (mShowing) {
            // Shown again after hide(): visible again, as on Android.
            if (mHost != null && !mHost.isVisible()) {
                mHost.setVisible(true);
                mHost.repaint();
            }
            return;
        }
        mCanceled = false;
        create();
        onStart();
        mHost = new Host();
        if (mTitle != null && mTitle.length() > 0 && !(this instanceof AlertDialog)) {
            mHost.setTitle(mTitle.toString());
        }
        Style s = mHost.getDialogStyle();
        s.setPadding(0, 0, 0, 0);
        if (mDecor.getBackground() != null && !(mDecor.getBackground() instanceof ColorDrawable)) {
            // The window background is a shape of its own (a Material
            // dialog's rounded surface): nothing of Codename One's dialog
            // style may show around its corners.
            s.setBgTransparency(0);
            s.setBorder(com.codename1.ui.plaf.Border.createEmpty());
            mHost.getAllStyles().setBgTransparency(0);
            mHost.getAllStyles().setBorder(com.codename1.ui.plaf.Border.createEmpty());
        }
        mHost.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
        mHost.getContentPane().getAllStyles().setMargin(0, 0, 0, 0);
        mHost.add(BorderLayout.CENTER, mDecor.getPeer());
        mHost.setDisposeWhenPointerOutOfBounds(mCancelable && mCanceledOnTouchOutside);
        mHost.setBackCommand(new com.codename1.ui.Command("") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                onBackPressed();
            }
        });
        mShowing = true;
        mDecor.dispatchAttachedToWindow(true);
        mHost.showModeless();
        if (mShowListener != null) {
            mShowListener.onShow(this);
        }
    }

    public void onBackPressed() {
        if (mCancelable) {
            cancel();
        }
    }

    public void hide() {
        if (mHost != null) {
            mHost.setVisible(false);
        }
    }

    @Override
    public void dismiss() {
        if (!mShowing) {
            return;
        }
        mShowing = false;
        if (mHost != null) {
            mHost.dispose();
            mHost.removeAll();
            mHost = null;
        }
        mDecor.dispatchAttachedToWindow(false);
        onStop();
        if (mDismissListener != null) {
            mDismissListener.onDismiss(this);
        }
    }

    @Override
    public void cancel() {
        if (!mCanceled && mCancelListener != null) {
            mCanceled = true;
            mCancelListener.onCancel(this);
        }
        dismiss();
    }

    /// The Codename One dialog showing this one, for tests.
    com.codename1.ui.Dialog host() {
        return mHost;
    }

    /// The Codename One dialog this one is shown in. Codename One closes it on
    /// its own -- a tap outside it (`setDisposeWhenPointerOutOfBounds`), its
    /// host window going away -- and every such path ends in `dispose()`.
    /// Disposing it directly skipped `cancel()`/`dismiss()`: no
    /// `OnCancelListener` or `OnDismissListener`, no `onStop()` or detach,
    /// and `isShowing()` stayed true over a disposed host. So a dispose this
    /// wrapper did not ask for is routed through it: an outside tap cancels,
    /// as on Android, and anything else dismisses.
    private final class Host extends com.codename1.ui.Dialog {
        Host() {
            super(new BorderLayout());
        }

        @Override
        public void dispose() {
            if (mShowing && mHost == this) {
                // dismiss() clears mShowing before disposing the host, so
                // this comes back through super.dispose() exactly once.
                if (isDisposeWhenPointerOutOfBounds() && wasDisposedDueToOutOfBoundsTouch()) {
                    cancel();
                } else {
                    dismiss();
                }
                return;
            }
            super.dispose();
        }
    }

    /// The dialog's root: as wide as Android makes a dialog, which is the
    /// screen minus margins and at most 560dp.
    static final class DialogFrame extends FrameLayout {
        DialogFrame(Context c) {
            super(c);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            float density = getResources().getDisplayMetrics().density;
            int screen = com.codename1.ui.Display.getInstance().getDisplayWidth();
            int max = Math.min(screen - Math.round(48 * density), Math.round(560 * density));
            int w = MeasureSpec.getSize(widthMeasureSpec);
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED || w > max) {
                w = max;
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), heightMeasureSpec);
        }
    }
}
