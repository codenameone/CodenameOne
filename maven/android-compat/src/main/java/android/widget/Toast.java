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
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/// A brief message floating near the bottom of the screen. Toasts are shown
/// one at a time, in the order they were shown, each for its duration; they
/// take no touches, so what is under one stays usable.
public class Toast {

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private static final long SHORT_DELAY = 2000;
    private static final long LONG_DELAY = 3500;

    /// Notified when a toast appears and disappears.
    public abstract static class Callback {
        public void onToastShown() {
        }

        public void onToastHidden() {
        }
    }

    private static final List<Toast> QUEUE = new ArrayList<Toast>();
    private static Toast current;
    private static Handler handler;

    private final Context context;
    private View view;
    private View defaultView;
    private int duration;
    private int gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    private int xOffset;
    private int yOffset;
    private float horizontalMargin;
    private float verticalMargin;
    private PopupWindow window;
    private int displayGeneration;
    private final List<Callback> callbacks = new ArrayList<Callback>();

    public Toast(Context context) {
        this.context = context;
        yOffset = Math.round(48 * context.getResources().getDisplayMetrics().density);
    }

    public static Toast makeText(Context context, CharSequence text, int duration) {
        Toast t = new Toast(context);
        View v = LayoutInflater.from(context).inflate(android.R.layout.transient_notification, null);
        TextView message = v.findViewById(android.R.id.message);
        message.setText(text);
        t.defaultView = v;
        t.view = v;
        t.duration = duration;
        return t;
    }

    public static Toast makeText(Context context, int resId, int duration) {
        return makeText(context, context.getResources().getText(resId), duration);
    }

    private static Handler handler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    /// Queues this toast; it appears once those shown before it are gone.
    public void show() {
        if (view == null) {
            throw new RuntimeException("setView must have been called");
        }
        if (current == this || QUEUE.contains(this)) {
            return;
        }
        QUEUE.add(this);
        if (current == null) {
            showNext();
        }
    }

    private static void showNext() {
        if (QUEUE.isEmpty()) {
            current = null;
            return;
        }
        final Toast t = QUEUE.remove(0);
        current = t;
        final int generation = ++t.displayGeneration;
        t.display();
        handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (current == t && t.displayGeneration == generation) {
                    t.hide();
                    showNext();
                }
            }
        }, t.duration == LENGTH_LONG ? LONG_DELAY : SHORT_DELAY);
    }

    private void display() {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        window = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setTouchable(false);
        window.setFocusable(false);
        int[] size = screen();
        window.showAtLocation(null, gravity, xOffset + Math.round(horizontalMargin * size[0]),
                yOffset + Math.round(verticalMargin * size[1]));
        for (Callback c : new ArrayList<Callback>(callbacks)) {
            c.onToastShown();
        }
    }

    private int[] screen() {
        android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return new int[] {dm.widthPixels, dm.heightPixels};
    }

    private void hide() {
        if (window != null) {
            window.dismiss();
            window = null;
            for (Callback c : new ArrayList<Callback>(callbacks)) {
                c.onToastHidden();
            }
        }
    }

    /// Hides the toast if it is showing, or drops it from the queue.
    public void cancel() {
        if (QUEUE.remove(this)) {
            return;
        }
        if (current == this) {
            displayGeneration++;
            hide();
            showNext();
        }
    }

    public void setText(CharSequence s) {
        if (defaultView == null) {
            throw new RuntimeException("This Toast was not created with Toast.makeText()");
        }
        TextView message = defaultView.findViewById(android.R.id.message);
        message.setText(s);
    }

    public void setText(int resId) {
        setText(context.getResources().getText(resId));
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return duration;
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public void setGravity(int gravity, int xOffset, int yOffset) {
        this.gravity = gravity;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public int getGravity() {
        return gravity;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void setMargin(float horizontalMargin, float verticalMargin) {
        this.horizontalMargin = horizontalMargin;
        this.verticalMargin = verticalMargin;
    }

    public float getHorizontalMargin() {
        return horizontalMargin;
    }

    public float getVerticalMargin() {
        return verticalMargin;
    }

    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }
}
