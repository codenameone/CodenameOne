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
package com.codename1.components;

import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

/// Picks a colour: a saturation and brightness square over a hue strip, with a
/// swatch and the hex value of the result, and optionally a row of preset
/// swatches.
///
/// ```java
/// ColorPicker picker = new ColorPicker(0x3b82f6);
/// picker.setPresets(0xef4444, 0xf59e0b, 0x10b981, 0x3b82f6, 0x8b5cf6);
/// picker.addActionListener(e -> preview.getAllStyles().setBgColor(picker.getColor()));
/// form.add(picker);
///
/// // or ask in a dialog; null when the user cancels
/// Integer chosen = ColorPicker.showDialog("Highlight", 0xffcc00);
/// ```
///
/// Colours are 0xRRGGBB. Action listeners fire as the user drags and when a
/// preset is tapped.
///
/// UIIDs: `ColorPicker` for the container and `ColorPickerValue` for the hex
/// text, falling back to `Label`.
public class ColorPicker extends Container {
    private float hue;
    private float saturation;
    private float brightness;
    private int[] presets = new int[0];
    private final SaturationArea area = new SaturationArea();
    private final HueStrip strip = new HueStrip();
    private final Swatch preview = new Swatch(true);
    private final Label hex = new Label();
    private final Container presetRow = new Container(new com.codename1.ui.layouts.FlowLayout());
    private final EventDispatcher listeners = new EventDispatcher();

    /// A picker starting at the accent colour of the theme.
    public ColorPicker() {
        this(0x3b82f6);
    }

    /// A picker starting at `rgb`.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the initial colour, 0xRRGGBB
    public ColorPicker(int rgb) {
        super(BoxLayout.y());
        setUIID("ColorPicker");
        hex.setUIID(getUIManager().isThemeStyleDefined("ColorPickerValue") ? "ColorPickerValue" : "Label");
        presetRow.setHidden(true);
        add(area).add(strip).add(BorderLayout.centerEastWest(hex, null, preview)).add(presetRow);
        setColor(rgb);
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    /// Shows a picker in a dialog with OK and Cancel.
    ///
    /// #### Parameters
    ///
    /// - `title`: the dialog's title
    ///
    /// - `initial`: the starting colour, 0xRRGGBB
    ///
    /// #### Returns
    ///
    /// the chosen colour, or null when the user cancelled
    public static Integer showDialog(String title, int initial) {
        ColorPicker p = new ColorPicker(initial);
        Command ok = new Command("OK");
        Command cancel = new Command("Cancel");
        Command result = Dialog.show(title, p, cancel, ok);
        if (result != ok) { //NOPMD CompareObjectsWithEquals
            return null;
        }
        return Integer.valueOf(p.getColor());
    }

    /// The colour picked.
    ///
    /// #### Returns
    ///
    /// 0xRRGGBB
    public int getColor() {
        return hsvToRgb(hue, saturation, brightness);
    }

    /// Sets the colour.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: 0xRRGGBB
    public final void setColor(int rgb) {
        float[] hsv = rgbToHsv(rgb & 0xffffff);
        // A grey has no hue; keep the one the strip shows instead of jumping to red.
        if (hsv[1] > 0) {
            hue = hsv[0];
        }
        saturation = hsv[1];
        brightness = hsv[2];
        changed(false);
    }

    /// The hue, 0 to 360.
    ///
    /// #### Returns
    ///
    /// the hue in degrees
    public float getHue() {
        return hue;
    }

    /// Sets the colours offered as a row of presets under the picker.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the preset colours, 0xRRGGBB; none hides the row
    public void setPresets(int... rgb) {
        presets = new int[rgb == null ? 0 : rgb.length];
        presetRow.removeAll();
        for (int i = 0; i < presets.length; i++) {
            presets[i] = rgb[i] & 0xffffff;
            final int color = presets[i];
            Swatch swatch = new Swatch(false);
            swatch.color = color;
            swatch.setGrabsPointerEvents(true);
            swatch.onTap = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    setColor(color);
                    fire();
                }
            };
            presetRow.add(swatch);
        }
        presetRow.setHidden(presets.length == 0);
        revalidateLater();
    }

    /// The preset colours.
    ///
    /// #### Returns
    ///
    /// a copy of the presets
    public int[] getPresets() {
        int[] copy = new int[presets.length];
        System.arraycopy(presets, 0, copy, 0, presets.length);
        return copy;
    }

    /// Adds a listener called whenever the user changes the colour.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    /// The colour as "#RRGGBB".
    ///
    /// #### Parameters
    ///
    /// - `rgb`: 0xRRGGBB
    ///
    /// #### Returns
    ///
    /// the hex string, upper case
    public static String toHex(int rgb) {
        StringBuilder sb = new StringBuilder("#");
        String digits = "0123456789ABCDEF";
        for (int shift = 20; shift >= 0; shift -= 4) {
            sb.append(digits.charAt((rgb >> shift) & 0xf));
        }
        return sb.toString();
    }

    /// Converts hue, saturation and brightness to a colour.
    ///
    /// #### Parameters
    ///
    /// - `h`: hue, 0 to 360
    ///
    /// - `s`: saturation, 0 to 1
    ///
    /// - `v`: brightness, 0 to 1
    ///
    /// #### Returns
    ///
    /// 0xRRGGBB
    public static int hsvToRgb(float h, float s, float v) {
        float hh = ((h % 360) + 360) % 360 / 60f;
        int sector = (int) hh;
        float f = hh - sector;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    private static int channel(float c) {
        return Math.max(0, Math.min(255, Math.round(c * 255)));
    }

    /// Converts a colour to hue, saturation and brightness.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: 0xRRGGBB
    ///
    /// #### Returns
    ///
    /// {hue 0 to 360, saturation 0 to 1, brightness 0 to 1}
    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xff) / 255f;
        float g = ((rgb >> 8) & 0xff) / 255f;
        float b = (rgb & 0xff) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) {
                h = 60 * (((g - b) / d) % 6);
            } else if (max == g) {
                h = 60 * ((b - r) / d + 2);
            } else {
                h = 60 * ((r - g) / d + 4);
            }
        }
        if (h < 0) {
            h += 360;
        }
        return new float[] {h, max == 0 ? 0 : d / max, max};
    }

    private void changed(boolean byUser) {
        int c = getColor();
        hex.setText(toHex(c));
        preview.color = c;
        area.repaint();
        strip.repaint();
        preview.repaint();
        if (byUser) {
            fire();
        }
    }

    private void fire() {
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Change));
    }

    /// Paints a thumb: a ring in white with a dark outline, visible on any colour.
    static void thumb(Graphics g, int cx, int cy, int size) {
        g.setColor(0x000000);
        g.setAlpha(110);
        g.fillArc(cx - size / 2 - 1, cy - size / 2 - 1, size + 2, size + 2, 0, 360);
        g.setAlpha(255);
        g.setColor(0xffffff);
        g.fillArc(cx - size / 2, cy - size / 2, size, size, 0, 360);
        int inner = Math.max(1, size - CN.convertToPixels(1f));
        g.setColor(0x000000);
        g.setAlpha(110);
        g.fillArc(cx - inner / 2 - 1, cy - inner / 2 - 1, inner + 2, inner + 2, 0, 360);
        g.setAlpha(255);
    }

    /// The saturation (x) and brightness (y) square for the current hue.
    final class SaturationArea extends Component {
        private Image cache;
        private float cacheHue = -1;

        SaturationArea() {
            setUIID("Container");
            // A plain Component is skipped by hit testing unless it asks for pointer events.
            setGrabsPointerEvents(true);
            getAllStyles().setBgTransparency(0);
            getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
            getAllStyles().setMargin(0, 1, 0, 0);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(50), CN.convertToPixels(32));
        }

        @Override
        public void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            if (cache == null || cache.getWidth() != w || cache.getHeight() != h || Float.compare(cacheHue, hue) != 0) {
                int[] rgb = new int[w * h];
                for (int y = 0; y < h; y++) {
                    float v = 1 - y / (float) Math.max(1, h - 1);
                    for (int x = 0; x < w; x++) {
                        float s = x / (float) Math.max(1, w - 1);
                        rgb[y * w + x] = 0xff000000 | hsvToRgb(hue, s, v);
                    }
                }
                cache = Image.createImage(rgb, w, h);
                cacheHue = hue;
            }
            g.drawImage(cache, getX(), getY());
            int alpha = g.getAlpha();
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            int cx = getX() + Math.round(saturation * (w - 1));
            int cy = getY() + Math.round((1 - brightness) * (h - 1));
            thumb(g, cx, cy, CN.convertToPixels(4));
            g.setAlpha(alpha);
            g.setAntiAliased(aa);
        }

        @Override
        public void pointerPressed(int x, int y) {
            super.pointerPressed(x, y);
            pick(x, y);
        }

        @Override
        public void pointerDragged(int x, int y) {
            pick(x, y);
        }

        @Override
        protected boolean isStickyDrag() {
            return true;
        }

        private void pick(int x, int y) {
            int w = Math.max(1, getWidth() - 1);
            int h = Math.max(1, getHeight() - 1);
            saturation = Math.max(0, Math.min(1, (x - getAbsoluteX()) / (float) w));
            brightness = 1 - Math.max(0, Math.min(1, (y - getAbsoluteY()) / (float) h));
            changed(true);
        }
    }

    /// The hue strip.
    final class HueStrip extends Component {
        private Image cache;

        HueStrip() {
            setUIID("Container");
            // A plain Component is skipped by hit testing unless it asks for pointer events.
            setGrabsPointerEvents(true);
            getAllStyles().setBgTransparency(0);
            getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
            getAllStyles().setMargin(1, 1, 0, 0);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(50), CN.convertToPixels(5));
        }

        @Override
        public void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            int bar = Math.max(1, h * 3 / 5);
            if (cache == null || cache.getWidth() != w || cache.getHeight() != bar) {
                int[] rgb = new int[w * bar];
                for (int x = 0; x < w; x++) {
                    int c = 0xff000000 | hsvToRgb(360f * x / Math.max(1, w - 1), 1, 1);
                    for (int y = 0; y < bar; y++) {
                        rgb[y * w + x] = c;
                    }
                }
                cache = Image.createImage(rgb, w, bar);
            }
            int top = getY() + (h - bar) / 2;
            g.drawImage(cache, getX(), top);
            int alpha = g.getAlpha();
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            int cx = getX() + Math.round(hue / 360f * (w - 1));
            thumb(g, cx, getY() + h / 2, h);
            g.setAlpha(alpha);
            g.setAntiAliased(aa);
        }

        @Override
        public void pointerPressed(int x, int y) {
            super.pointerPressed(x, y);
            pick(x);
        }

        @Override
        public void pointerDragged(int x, int y) {
            pick(x);
        }

        @Override
        protected boolean isStickyDrag() {
            return true;
        }

        private void pick(int x) {
            int w = Math.max(1, getWidth() - 1);
            hue = 360f * Math.max(0, Math.min(1, (x - getAbsoluteX()) / (float) w));
            changed(true);
        }
    }

    /// A rounded square of one colour.
    static final class Swatch extends Component {
        int color;
        ActionListener onTap;
        private final boolean large;

        Swatch(boolean large) {
            this.large = large;
            setUIID("Container");
            Style s = getAllStyles();
            s.setBgTransparency(0);
            s.setMarginUnit(Style.UNIT_TYPE_DIPS);
            s.setMargin(0.5f, 0.5f, 0.5f, 0.5f);
        }

        @Override
        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y);
            if (onTap != null && contains(x, y)) {
                onTap.actionPerformed(new ActionEvent(this, ActionEvent.Type.PointerReleased, x, y));
            }
        }

        @Override
        protected Dimension calcPreferredSize() {
            int d = CN.convertToPixels(large ? 8 : 6);
            return new Dimension(d, d);
        }

        @Override
        public void paint(Graphics g) {
            int c = g.getColor();
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            int r = CN.convertToPixels(1.5f);
            g.setColor(Palette.text(this));
            g.setAlpha(60);
            g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), r, r);
            g.setAlpha(255);
            g.setColor(color);
            g.fillRoundRect(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, r, r);
            g.setColor(c);
            g.setAntiAliased(aa);
        }

        @Override
        protected void paintBorder(Graphics g) {
        }

        @Override
        protected void paintBackground(Graphics g) {
        }
    }
}
