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
import android.graphics.drawable.Drawable;

/// An activity's window: its decor view and window-level flags. Flags that
/// mean something on Codename One (keep screen on, fullscreen) are applied;
/// the rest are recorded for `getAttributes`.
public class Window {

    public static final int FEATURE_OPTIONS_PANEL = 0;
    public static final int FEATURE_NO_TITLE = 1;
    public static final int FEATURE_PROGRESS = 2;
    public static final int FEATURE_ACTION_BAR = 8;
    public static final int FEATURE_ACTION_BAR_OVERLAY = 9;
    public static final int FEATURE_CONTENT_TRANSITIONS = 12;
    public static final int ID_ANDROID_CONTENT = android.R.id.content;

    public interface Callback {
    }

    private final Context context;
    private final WindowManager.LayoutParams attributes = new WindowManager.LayoutParams();
    private ViewGroup decor;
    private int statusBarColor;
    private int navigationBarColor;
    private Runnable onChange;
    private boolean active;
    private boolean holdingScreenOn;

    /// Runtime use: only the foreground activity may hold a window screen lock.
    public void setActive(boolean active) {
        this.active = active;
        updateKeepScreenOn();
    }

    private void updateKeepScreenOn() {
        boolean hold = active && (attributes.flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
        if (hold != holdingScreenOn) {
            holdingScreenOn = hold;
            // Share the view request count so clearing a window flag cannot
            // release a lock still held by an attached view.
            View.updateKeepScreenOn(hold ? 1 : -1);
        }
    }

    public Window(Context context) {
        this.context = context;
    }

    public final Context getContext() {
        return context;
    }

    /// Runtime use: installs the decor and a hook run when flags change.
    public void attach(ViewGroup decor, Runnable onChange) {
        this.decor = decor;
        this.onChange = onChange;
    }

    public View getDecorView() {
        return decor;
    }

    public View peekDecorView() {
        return decor;
    }

    public <T extends View> T findViewById(int id) {
        return decor == null ? null : decor.<T>findViewById(id);
    }

    public boolean requestFeature(int featureId) {
        if (featureId == FEATURE_NO_TITLE) {
            attributes.title = null;
            noTitle = true;
            changed();
        }
        return true;
    }

    private boolean noTitle;

    /// Whether `requestWindowFeature(FEATURE_NO_TITLE)` was called.
    public boolean isNoTitleRequested() {
        return noTitle;
    }

    public boolean hasFeature(int feature) {
        return feature == FEATURE_NO_TITLE ? noTitle : feature == FEATURE_ACTION_BAR && !noTitle;
    }

    public void setFlags(int flags, int mask) {
        attributes.flags = (attributes.flags & ~mask) | (flags & mask);
        changed();
    }

    public void addFlags(int flags) {
        setFlags(flags, flags);
    }

    public void clearFlags(int flags) {
        setFlags(0, flags);
    }

    public WindowManager.LayoutParams getAttributes() {
        return attributes;
    }

    public void setAttributes(WindowManager.LayoutParams a) {
        attributes.flags = a.flags;
        attributes.softInputMode = a.softInputMode;
        attributes.screenBrightness = a.screenBrightness;
        changed();
    }

    private void changed() {
        updateKeepScreenOn();
        if (onChange != null) {
            onChange.run();
        }
    }

    public void setSoftInputMode(int mode) {
        attributes.softInputMode = mode;
    }

    public void setStatusBarColor(int color) {
        statusBarColor = color;
        changed();
    }

    public int getStatusBarColor() {
        return statusBarColor;
    }

    public void setNavigationBarColor(int color) {
        navigationBarColor = color;
    }

    public int getNavigationBarColor() {
        return navigationBarColor;
    }

    public void setDecorFitsSystemWindows(boolean decorFitsSystemWindows) {
    }

    public void setBackgroundDrawable(Drawable drawable) {
        if (decor != null) {
            decor.setBackground(drawable);
        }
    }

    public void setBackgroundDrawableResource(int resId) {
        setBackgroundDrawable(context.getDrawable(resId));
    }

    public void setTitle(CharSequence title) {
        attributes.title = title;
    }

    public void setLayout(int width, int height) {
    }

    public void setGravity(int gravity) {
        attributes.gravity = gravity;
    }

    public void setDimAmount(float amount) {
        attributes.dimAmount = amount;
    }

    public void setWindowAnimations(int resId) {
    }

    public LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(context);
    }

    public WindowManager getWindowManager() {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public View getCurrentFocus() {
        return decor == null ? null : decor.findFocus();
    }

    public boolean isFloating() {
        return false;
    }

    public void setContentView(View view) {
        if (decor != null) {
            ViewGroup content = decor.findViewById(android.R.id.content);
            if (content != null) {
                content.removeAllViews();
                content.addView(view);
            }
        }
    }
}
