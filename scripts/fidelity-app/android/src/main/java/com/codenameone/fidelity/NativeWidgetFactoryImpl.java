/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.fidelity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.view.ContextThemeWrapper;

import com.codename1.impl.android.AndroidNativeUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Android side of {@link NativeWidgetFactory}: builds REAL Material 3 widgets to
 * serve as the fidelity reference. The CN1 build wraps the returned android.view.View
 * in a PeerComponent automatically (PeerComponent.create(view)), so each method
 * here just returns the native View.
 *
 * Views are constructed on the UI thread under a Material 3 themed context so the
 * Material components resolve their attributes; the requested appearance picks the
 * light/dark Material theme.
 */
public class NativeWidgetFactoryImpl {

    public boolean isSupported() {
        return true;
    }

    public boolean isWidgetSupported(String kind) {
        return mapsToKnownWidget(kind);
    }

    private boolean mapsToKnownWidget(String kind) {
        return "material_button_text".equals(kind)
                || "material_button_filled".equals(kind)
                || "material_button_tonal".equals(kind)
                || "material_button_outlined".equals(kind)
                || "material_textinput".equals(kind)
                || "material_checkbox".equals(kind)
                || "material_radio".equals(kind)
                || "material_switch".equals(kind)
                || "material_slider".equals(kind)
                || "material_progress_linear".equals(kind)
                || "material_fab".equals(kind)
                || "material_tablayout".equals(kind)
                || "material_toolbar".equals(kind)
                || "material_alert_view".equals(kind);
    }

    public boolean renderWidgetToFile(final String kind, final String state, final String appearance,
            final String text, final String outPath, final int widthPx, final int heightPx) {
        if (!mapsToKnownWidget(kind) || widthPx <= 0 || heightPx <= 0 || outPath == null) {
            return false;
        }
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return false;
        }
        // Build + measure + lay out the native tile on the UI thread.
        final AtomicReference<View> tileRef = new AtomicReference<View>();
        final CountDownLatch latch = new CountDownLatch(1);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    tileRef.set(buildOnUiThread(activity, kind, state, appearance, text, widthPx, heightPx));
                } catch (Throwable t) {
                    System.out.println("CN1SS:ERR:fidelity native build failed kind=" + kind + " " + t);
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        View tile = tileRef.get();
        if (tile == null) {
            return false;
        }
        // Rasterize the laid-out tile off-screen (AndroidNativeUtil draws on the
        // UI thread internally) and PNG-encode it.
        Bitmap bmp = AndroidNativeUtil.renderViewOnBitmap(tile, widthPx, heightPx);
        if (bmp == null) {
            return false;
        }
        try {
            // Write the PNG bytes to the caller-supplied outPath (a String ARG, the
            // only object-transport direction that marshals cleanly on the iOS
            // bridge). The device reads it back via FileSystemStorage.
            String fsPath = outPath;
            if (fsPath.startsWith("file://")) {
                fsPath = fsPath.substring("file://".length());
            }
            java.io.File f = new java.io.File(fsPath);
            java.io.File parent = f.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return true;
        } catch (Throwable t) {
            System.out.println("CN1SS:ERR:fidelity native png encode failed kind=" + kind + " " + t);
            return false;
        } finally {
            bmp.recycle();
        }
    }

    private View buildOnUiThread(Activity activity, String kind, String state, String appearance,
            String text, int widthPx, int heightPx) {
        Context ctx = new ContextThemeWrapper(activity, themeFor(appearance));
        String label = text != null ? text : "";
        View view;
        if ("material_button_filled".equals(kind)) {
            // Material 3 filled button (primary) -- maps to CN1 "Button".
            MaterialButton b = new MaterialButton(ctx);
            b.setText(label);
            applyEnabledPressed(b, state);
            view = b;
        } else if ("material_button_tonal".equals(kind)) {
            // Material 3 filled-tonal button (secondary container) -- maps to CN1
            // "RaisedButton". Material 1.12 exposes tonal only as a style, not a
            // defStyleAttr, so apply its palette (secondary container fill +
            // on-secondary-container text) directly onto a filled button.
            MaterialButton b = new MaterialButton(ctx);
            b.setText(label);
            int container = themeColor(ctx, com.google.android.material.R.attr.colorSecondaryContainer);
            int onContainer = themeColor(ctx, com.google.android.material.R.attr.colorOnSecondaryContainer);
            int onSurface = themeColor(ctx, com.google.android.material.R.attr.colorOnSurface);
            int surface = themeColor(ctx, com.google.android.material.R.attr.colorSurface);
            // Material 3 disabled filled/tonal button = container at on-surface @ 12%,
            // label at on-surface @ 38%. Using ColorStateList.valueOf() (a single
            // state) instead would leave the disabled button looking enabled, since
            // it overrides MaterialButton's built-in stateful colours.
            int disBg = compositeOver((onSurface & 0xffffff) | (0x1f << 24), surface);
            int disText = (onSurface & 0xffffff) | (0x61 << 24);
            int[][] sts = {{-android.R.attr.state_enabled}, {}};
            b.setBackgroundTintList(new android.content.res.ColorStateList(sts, new int[]{disBg, container}));
            b.setTextColor(new android.content.res.ColorStateList(sts, new int[]{disText, onContainer}));
            applyEnabledPressed(b, state);
            view = b;
        } else if ("material_button_outlined".equals(kind) || "material_button_text".equals(kind)) {
            // Material 3 outlined button (pill outline, transparent) -- maps to CN1
            // "FlatButton", which the theme styles with a transparent pill + stroke.
            MaterialButton b = new MaterialButton(ctx, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(label);
            applyEnabledPressed(b, state);
            view = b;
        } else if ("material_textinput".equals(kind)) {
            TextInputLayout til = new TextInputLayout(ctx, null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            TextInputEditText edit = new TextInputEditText(til.getContext());
            edit.setText(label);
            til.addView(edit);
            applyEnabled(til, state);
            view = til;
        } else if ("material_checkbox".equals(kind)) {
            MaterialCheckBox cb = new MaterialCheckBox(ctx);
            cb.setText(label);
            cb.setChecked("selected".equals(state));
            applyEnabled(cb, state);
            view = cb;
        } else if ("material_radio".equals(kind)) {
            MaterialRadioButton rb = new MaterialRadioButton(ctx);
            rb.setText(label);
            rb.setChecked("selected".equals(state));
            applyEnabled(rb, state);
            view = rb;
        } else if ("material_switch".equals(kind)) {
            MaterialSwitch sw = new MaterialSwitch(ctx);
            sw.setChecked("selected".equals(state));
            applyEnabled(sw, state);
            view = sw;
        } else if ("material_slider".equals(kind)) {
            Slider s = new Slider(ctx);
            s.setValueFrom(0f);
            s.setValueTo(100f);
            s.setValue(50f);
            applyEnabled(s, state);
            view = s;
        } else if ("material_progress_linear".equals(kind)) {
            // Material's LinearProgressIndicator does not paint when rendered
            // off-screen (it is animation/visibility driven). The classic
            // horizontal ProgressBar paints a determinate bar reliably and picks
            // up Material colors from the themed context.
            android.widget.ProgressBar p = new android.widget.ProgressBar(
                    ctx, null, android.R.attr.progressBarStyleHorizontal);
            p.setMax(100);
            p.setProgress(50);
            view = p;
        } else if ("material_tablayout".equals(kind)) {
            // Material 3 fixed tab strip, 3 tabs, first selected (the indicator
            // underlines it). Sized full-width like slider/progress below.
            TabLayout tabs = new TabLayout(ctx);
            tabs.setTabMode(TabLayout.MODE_FIXED);
            tabs.addTab(tabs.newTab().setText("Tab 1"));
            tabs.addTab(tabs.newTab().setText("Tab 2"));
            tabs.addTab(tabs.newTab().setText("Tab 3"));
            tabs.selectTab(tabs.getTabAt(0));
            view = tabs;
        } else if ("material_toolbar".equals(kind)) {
            // Material 3 small top app bar with a title. A bare MaterialToolbar is
            // transparent, so it would render against the bare tile (black in dark
            // mode) rather than the M3 surface the bar actually sits on. Pin the
            // surface colour so the reference shows the intended bar background.
            MaterialToolbar tb = new MaterialToolbar(ctx);
            tb.setTitle(label);
            tb.setBackgroundColor(themeColor(ctx, com.google.android.material.R.attr.colorSurface));
            view = tb;
        } else if ("material_alert_view".equals(kind)) {
            // Material 3 alert dialog CONTENT (not the presented modal): a rounded
            // surface-container card with a headline, supporting text and two text
            // action buttons (Cancel / OK), built directly so it renders off-screen.
            float density = ctx.getResources().getDisplayMetrics().density;
            MaterialCardView card = new MaterialCardView(ctx);
            card.setRadius(28 * density);
            card.setCardBackgroundColor(themeColor(ctx, com.google.android.material.R.attr.colorSurfaceContainerHigh));
            card.setCardElevation(0);
            android.widget.LinearLayout col = new android.widget.LinearLayout(ctx);
            col.setOrientation(android.widget.LinearLayout.VERTICAL);
            int pad = (int) (24 * density);
            col.setPadding(pad, pad, pad, (int) (18 * density));
            android.widget.TextView title = new android.widget.TextView(ctx);
            title.setText("Title");
            title.setTextSize(24);
            title.setTextColor(themeColor(ctx, com.google.android.material.R.attr.colorOnSurface));
            android.widget.TextView body = new android.widget.TextView(ctx);
            body.setText(label);
            body.setTextSize(14);
            body.setTextColor(themeColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant));
            android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.topMargin = (int) (16 * density);
            body.setLayoutParams(blp);
            android.widget.LinearLayout btns = new android.widget.LinearLayout(ctx);
            btns.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btns.setGravity(Gravity.END);
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = (int) (18 * density);
            btns.setLayoutParams(rlp);
            int accent = themeColor(ctx, com.google.android.material.R.attr.colorPrimary);
            for (String t : new String[]{"Cancel", "OK"}) {
                android.widget.TextView b2 = new android.widget.TextView(ctx);
                b2.setText(t);
                b2.setTextSize(14);
                b2.setAllCaps(false);
                b2.setTextColor(accent);
                b2.setPadding((int) (12 * density), (int) (10 * density), (int) (12 * density), (int) (10 * density));
                btns.addView(b2);
            }
            col.addView(title);
            col.addView(body);
            col.addView(btns);
            card.addView(col);
            view = card;
        } else if ("material_fab".equals(kind)) {
            FloatingActionButton fab = new FloatingActionButton(ctx);
            // A real FAB carries an icon - give it the standard "+" so it matches the
            // CN1 FAB (FontImage.MATERIAL_ADD) rather than an empty button.
            fab.setImageResource(android.R.drawable.ic_input_add);
            applyEnabledPressed(fab, state);
            view = fab;
        } else {
            return null;
        }
        // Anchor the widget TOP-LEFT at its natural (WRAP_CONTENT) size in a fixed
        // tile -- matching how the CN1 side anchors its component -- so the two are
        // laid out identically and directly comparable. The tile background
        // matches the CN1 tile backdrop (white/black per appearance) so
        // anti-aliased edges blend identically on both sides.
        FrameLayout tile = new FrameLayout(ctx);
        tile.setBackgroundColor("dark".equals(appearance) ? 0xFF000000 : 0xFFFFFFFF);
        // Sliders and progress bars are inherently full-width: at WRAP_CONTENT they
        // collapse to ~0. Give them a fixed width so they render meaningfully; the
        // CN1 side sizes its Slider to the same fraction of the tile.
        boolean stretchWidth = "material_slider".equals(kind) || "material_progress_linear".equals(kind);
        int tileChildW;
        if ("material_tablayout".equals(kind) || "material_toolbar".equals(kind)) {
            tileChildW = widthPx;                 // tab strip / app bar are edge-to-edge full-width
        } else if (stretchWidth) {
            tileChildW = widthPx * 2 / 3;
        } else {
            tileChildW = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                tileChildW, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        tile.addView(view, lp);
        tile.measure(View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
        tile.layout(0, 0, widthPx, heightPx);
        // Material controls animate their state transitions (the radio/checkbox
        // mark, the slider thumb sliding to its value, ripples). Rendering
        // immediately would capture a mid-animation frame, making the off-screen
        // raster nondeterministic run-to-run. Snap every drawable to its final
        // state so the rasterized reference is stable.
        jumpDrawables(tile);
        return tile;
    }

    /** Recursively snap every drawable in the tree to its current (final) state. */
    private void jumpDrawables(View v) {
        v.jumpDrawablesToCurrentState();
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                jumpDrawables(g.getChildAt(i));
            }
        }
    }

    private int themeFor(String appearance) {
        if ("dark".equals(appearance)) {
            return com.google.android.material.R.style.Theme_Material3_Dark;
        }
        return com.google.android.material.R.style.Theme_Material3_Light;
    }

    /** Alpha-composites a (possibly translucent) foreground colour over an opaque bg. */
    private static int compositeOver(int fg, int bg) {
        int a = (fg >>> 24) & 0xff;
        int r = (((fg >> 16) & 0xff) * a + ((bg >> 16) & 0xff) * (255 - a)) / 255;
        int g = (((fg >> 8) & 0xff) * a + ((bg >> 8) & 0xff) * (255 - a)) / 255;
        int b = ((fg & 0xff) * a + (bg & 0xff) * (255 - a)) / 255;
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    /** Resolves a Material theme colour attribute to its colour int. */
    private int themeColor(Context ctx, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (ctx.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                return ctx.getResources().getColor(tv.resourceId, ctx.getTheme());
            }
            return tv.data;
        }
        return 0;
    }

    private void applyEnabled(View v, String state) {
        if ("disabled".equals(state)) {
            v.setEnabled(false);
        }
    }

    private void applyEnabledPressed(View v, String state) {
        if ("disabled".equals(state)) {
            v.setEnabled(false);
        } else if ("pressed".equals(state)) {
            // The M3 pressed state layer / ripple only paints when the drawable's
            // state actually carries state_pressed AND (for a RippleDrawable) a
            // hotspot is set. Off-screen rendering does not run the touch pipeline,
            // so set the hotspot to the view centre and refresh the drawable state
            // explicitly before snapping -- otherwise pressed rasterizes identically
            // to normal.
            v.setPressed(true);
            v.refreshDrawableState();
            android.graphics.drawable.Drawable bg = v.getBackground();
            if (bg != null) {
                bg.setState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed});
                int cx = Math.max(1, v.getWidth() / 2);
                int cy = Math.max(1, v.getHeight() / 2);
                bg.setHotspot(cx, cy);
            }
            v.jumpDrawablesToCurrentState();
        }
    }
}
