package com.codenameone.fidelity.nativeref;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Builds the reference Material 3 widget tiles. This is the single source of
 * the Android native look for the fidelity suite: the tiles are captured from
 * a REAL window (MainActivity + PixelCopy) and committed as the android-m3
 * golden set. Adapted from the retired in-app factory
 * (scripts/fidelity-app/android/.../NativeWidgetFactoryImpl.java) -- the
 * widget construction is kept identical so the committed references stay
 * continuous with the previously captured goldens.
 */
final class RefWidgets {

    private RefWidgets() {
    }

    /** Builds a laid-out tile (widget anchored top-left on the appearance bg). */
    static View buildTile(Context activity, String kind, String state, String appearance,
            String text, int widthPx, int heightPx) {
        Context ctx = new ContextThemeWrapper(activity, themeFor(appearance));
        String label = text != null ? text : "";
        View view;
        if ("material_button_filled".equals(kind)) {
            MaterialButton b = new MaterialButton(ctx);
            b.setText(label);
            view = b;
        } else if ("material_button_tonal".equals(kind)) {
            // Material 3 filled-tonal (secondary container); tonal is a style, not
            // a defStyleAttr, so apply its palette onto a filled button -- with the
            // proper stateful disabled colours (see the fidelity factory history).
            MaterialButton b = new MaterialButton(ctx);
            b.setText(label);
            int container = themeColor(ctx, com.google.android.material.R.attr.colorSecondaryContainer);
            int onContainer = themeColor(ctx, com.google.android.material.R.attr.colorOnSecondaryContainer);
            int onSurface = themeColor(ctx, com.google.android.material.R.attr.colorOnSurface);
            int surface = themeColor(ctx, com.google.android.material.R.attr.colorSurface);
            int disBg = compositeOver((onSurface & 0xffffff) | (0x1f << 24), surface);
            int disText = (onSurface & 0xffffff) | (0x61 << 24);
            int[][] sts = {{-android.R.attr.state_enabled}, {}};
            b.setBackgroundTintList(new android.content.res.ColorStateList(sts, new int[]{disBg, container}));
            b.setTextColor(new android.content.res.ColorStateList(sts, new int[]{disText, onContainer}));
            view = b;
        } else if ("material_button_outlined".equals(kind) || "material_button_text".equals(kind)) {
            MaterialButton b = new MaterialButton(ctx, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(label);
            view = b;
        } else if ("material_textinput".equals(kind)) {
            TextInputLayout til = new TextInputLayout(ctx, null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            TextInputEditText edit = new TextInputEditText(til.getContext());
            edit.setText(label);
            til.addView(edit);
            view = til;
        } else if ("material_checkbox".equals(kind)) {
            MaterialCheckBox cb = new MaterialCheckBox(ctx);
            cb.setText(label);
            cb.setChecked("selected".equals(state));
            view = cb;
        } else if ("material_radio".equals(kind)) {
            MaterialRadioButton rb = new MaterialRadioButton(ctx);
            rb.setText(label);
            rb.setChecked("selected".equals(state));
            view = rb;
        } else if ("material_switch".equals(kind)) {
            MaterialSwitch sw = new MaterialSwitch(ctx);
            sw.setChecked("selected".equals(state));
            view = sw;
        } else if ("material_slider".equals(kind)) {
            Slider s = new Slider(ctx);
            s.setValueFrom(0f);
            s.setValueTo(100f);
            s.setValue(50f);
            view = s;
        } else if ("material_progress_linear".equals(kind)) {
            android.widget.ProgressBar p = new android.widget.ProgressBar(
                    ctx, null, android.R.attr.progressBarStyleHorizontal);
            p.setMax(100);
            p.setProgress(50);
            view = p;
        } else if ("material_tablayout".equals(kind)) {
            boolean dark = "dark".equals(appearance);
            int selColor = dark ? 0xff409cff : 0xff0a84ff;
            int unselColor = dark ? 0xffebebf5 : 0xff3c3c43;
            TabLayout tabs = new TabLayout(ctx);
            tabs.setTabMode(TabLayout.MODE_FIXED);
            tabs.setSelectedTabIndicatorColor(selColor);
            tabs.setTabTextColors(unselColor, selColor);
            int[][] tintStates = new int[][]{ new int[]{android.R.attr.state_selected}, new int[0] };
            tabs.setTabIconTint(new android.content.res.ColorStateList(
                    tintStates, new int[]{selColor, unselColor}));
            tabs.addTab(tabs.newTab().setText("Featured").setIcon(materialGlyph(ctx, '\uE838', 4.6f)));
            tabs.addTab(tabs.newTab().setText("Search").setIcon(materialGlyph(ctx, '\uE8B6', 4.6f)));
            tabs.addTab(tabs.newTab().setText("More").setIcon(materialGlyph(ctx, '\uE5D3', 4.6f)));
            tabs.selectTab(tabs.getTabAt(0));
            view = tabs;
        } else if ("material_toolbar".equals(kind)) {
            MaterialToolbar tb = new MaterialToolbar(ctx);
            tb.setTitle(label);
            tb.setBackgroundColor(themeColor(ctx, com.google.android.material.R.attr.colorSurface));
            view = tb;
        } else if ("material_alert_view".equals(kind)) {
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
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.topMargin = (int) (16 * density);
            body.setLayoutParams(blp);
            android.widget.LinearLayout btns = new android.widget.LinearLayout(ctx);
            btns.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btns.setGravity(Gravity.END);
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
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
            fab.setImageResource(android.R.drawable.ic_input_add);
            view = fab;
        } else {
            return null;
        }
        if ("disabled".equals(state)) {
            view.setEnabled(false);
        }

        FrameLayout tile = new FrameLayout(ctx);
        tile.setBackgroundColor("dark".equals(appearance) ? 0xFF000000 : 0xFFFFFFFF);
        boolean stretchWidth = "material_slider".equals(kind) || "material_progress_linear".equals(kind);
        int tileChildW;
        if ("material_tablayout".equals(kind) || "material_toolbar".equals(kind)) {
            tileChildW = widthPx;
        } else if (stretchWidth) {
            tileChildW = widthPx * 2 / 3;
        } else {
            tileChildW = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                tileChildW, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        tile.addView(view, lp);
        return tile;
    }

    /**
     * Applies the PRESSED visual state on a live, laid-out view: the real
     * state_pressed drawable state plus a centred ripple hotspot, with
     * drawables snapped so the capture is deterministic (no mid-ripple frame).
     * This is what the retired off-screen factory could not do honestly --
     * a real window runs the state-layer rendering exactly as a touch would.
     */
    static void applyPressedIfNeeded(View tile, String state) {
        if (!"pressed".equals(state) || !(tile instanceof ViewGroup)
                || ((ViewGroup) tile).getChildCount() == 0) {
            return;
        }
        View v = ((ViewGroup) tile).getChildAt(0);
        v.setPressed(true);
        v.refreshDrawableState();
        android.graphics.drawable.Drawable bg = v.getBackground();
        if (bg != null) {
            bg.setState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed});
            bg.setHotspot(Math.max(1, v.getWidth() / 2f), Math.max(1, v.getHeight() / 2f));
        }
        jumpDrawables(tile);
    }

    /** Recursively snap every drawable in the tree to its final state. */
    static void jumpDrawables(View v) {
        v.jumpDrawablesToCurrentState();
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                jumpDrawables(g.getChildAt(i));
            }
        }
    }

    private static int themeFor(String appearance) {
        if ("dark".equals(appearance)) {
            return com.google.android.material.R.style.Theme_Material3_Dark;
        }
        return com.google.android.material.R.style.Theme_Material3_Light;
    }

    /** Material icon-font glyph drawable (white; tinted by the tab icon tint). */
    private static android.graphics.drawable.Drawable materialGlyph(Context ctx, char glyph, float sizeMm) {
        android.graphics.Typeface tf;
        try {
            tf = android.graphics.Typeface.createFromAsset(ctx.getAssets(), "material-design-font.ttf");
        } catch (Throwable t) {
            return null;
        }
        float dpi = ctx.getResources().getDisplayMetrics().densityDpi;
        int px = Math.max(1, Math.round(sizeMm * dpi / 25.4f));
        Bitmap bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(tf);
        paint.setColor(0xffffffff);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        paint.setTextSize(px);
        android.graphics.Paint.FontMetrics fm = paint.getFontMetrics();
        float y = px / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(String.valueOf(glyph), px / 2f, y, paint);
        return new android.graphics.drawable.BitmapDrawable(ctx.getResources(), bmp);
    }

    private static int compositeOver(int fg, int bg) {
        int a = (fg >>> 24) & 0xff;
        int r = (((fg >> 16) & 0xff) * a + ((bg >> 16) & 0xff) * (255 - a)) / 255;
        int g = (((fg >> 8) & 0xff) * a + ((bg >> 8) & 0xff) * (255 - a)) / 255;
        int b = ((fg & 0xff) * a + (bg & 0xff) * (255 - a)) / 255;
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private static int themeColor(Context ctx, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (ctx.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                return ctx.getResources().getColor(tv.resourceId, ctx.getTheme());
            }
            return tv.data;
        }
        return 0;
    }
}
