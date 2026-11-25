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
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.plaf.Style;

/// A ring that fills clockwise to show how far something has got, with the
/// percentage (or any short text) in the middle -- a "percent indicator",
/// "progress ring" or "donut" progress.
///
/// ```java
/// CircularProgress upload = new CircularProgress();
/// upload.setProgress(40);
/// form.add(FlowLayout.encloseCenter(upload));
/// // later
/// upload.setProgress(85);
/// upload.setText("3 of 4");   // instead of "85%"
/// ```
///
/// It shows a known amount of progress. For work of unknown length use
/// `InfiniteProgress`, and for content still loading in place use `Skeleton`.
///
/// UIIDs: `CircularProgress` for the component and its label font. The ring
/// takes the foreground of `CircularProgressFill` and the track the background
/// of `CircularProgressTrack` when the theme defines them, and otherwise the
/// theme's `Slider` colours.
public class CircularProgress extends Component {
    private int progress;
    private String text;
    private boolean showText = true;
    private float diameterMM = 14f;
    private float thicknessMM = 1.5f;

    /// An empty ring.
    public CircularProgress() {
        setUIID("CircularProgress");
    }

    /// A ring filled to `progress` percent.
    ///
    /// #### Parameters
    ///
    /// - `progress`: 0 to 100
    public CircularProgress(int progress) {
        this();
        setProgress(progress);
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
            unselectedStyle.setFgColor(Palette.text(this));
        }
    }

    /// The progress, 0 to 100.
    ///
    /// #### Returns
    ///
    /// the percentage filled
    public int getProgress() {
        return progress;
    }

    /// Sets the progress, clamped to 0 to 100.
    ///
    /// #### Parameters
    ///
    /// - `progress`: the percentage filled
    public void setProgress(int progress) {
        int p = Math.max(0, Math.min(100, progress));
        if (p != this.progress) {
            this.progress = p;
            repaint();
        }
    }

    /// The text in the middle, or null when it shows the percentage.
    ///
    /// #### Returns
    ///
    /// the custom text
    public String getText() {
        return text;
    }

    /// Replaces the percentage in the middle with other text, such as "3 of 4".
    ///
    /// #### Parameters
    ///
    /// - `text`: the text, or null for the percentage
    public void setText(String text) {
        this.text = text;
        repaint();
    }

    /// Whether the middle shows any text.
    ///
    /// #### Returns
    ///
    /// true by default
    public boolean isShowText() {
        return showText;
    }

    /// Shows or hides the text in the middle.
    ///
    /// #### Parameters
    ///
    /// - `showText`: false for a bare ring
    public void setShowText(boolean showText) {
        this.showText = showText;
        repaint();
    }

    /// Sets the ring's size.
    ///
    /// #### Parameters
    ///
    /// - `diameterMM`: the outer diameter in millimetres, 14 by default
    ///
    /// - `thicknessMM`: the ring's width in millimetres, 1.5 by default
    public void setRingSize(float diameterMM, float thicknessMM) {
        this.diameterMM = diameterMM;
        this.thicknessMM = thicknessMM;
        setShouldCalcPreferredSize(true);
        repaint();
    }

    /// The text drawn in the middle right now.
    ///
    /// #### Returns
    ///
    /// the custom text, or the percentage
    public String getDisplayedText() {
        return text != null ? text : progress + "%";
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        int d = CN.convertToPixels(diameterMM);
        return new Dimension(d + s.getHorizontalPadding(), d + s.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style s = getStyle();
        int stroke = Math.max(1, CN.convertToPixels(thicknessMM));
        int w = getWidth() - s.getHorizontalPadding();
        int h = getHeight() - s.getVerticalPadding();
        int d = Math.min(w, h) - stroke;
        if (d <= 0) {
            return;
        }
        int x = getX() + s.getPaddingLeftNoRTL() + (w - d) / 2;
        int y = getY() + s.getPaddingTop() + (h - d) / 2;
        int color = g.getColor();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(Palette.track(this, "CircularProgressTrack"));
        ring(g, x, y, d, stroke, 90, -360);
        if (progress > 0) {
            g.setColor(Palette.accent(this, "CircularProgressFill"));
            ring(g, x, y, d, stroke, 90, -Math.round(360 * progress / 100f));
        }
        if (showText) {
            Font f = s.getFont();
            String t = getDisplayedText();
            g.setColor(s.getFgColor());
            g.setFont(f);
            g.drawString(t, x + (d - f.stringWidth(t)) / 2, y + (d - f.getHeight()) / 2);
        }
        g.setColor(color);
        g.setAntiAliased(aa);
    }

    /// Strokes an arc of the ring. Angles are degrees, anticlockwise from three
    /// o'clock as `Graphics.drawArc` takes them, so a clockwise sweep from the
    /// top starts at 90 and is negative.
    private static void ring(Graphics g, int x, int y, int d, int stroke, int start, int sweep) {
        if (g.isShapeSupported()) {
            GeneralPath path = new GeneralPath();
            path.arc(x, y, d, d, start * Math.PI / 180, sweep * Math.PI / 180);
            int cap = sweep <= -360 ? Stroke.CAP_BUTT : Stroke.CAP_ROUND;
            g.drawShape(path, new Stroke(stroke, cap, Stroke.JOIN_ROUND, 1f));
        } else {
            int half = stroke / 2;
            for (int i = -half; i < stroke - half; i++) {
                g.drawArc(x + i, y + i, d - 2 * i, d - 2 * i, start, sweep);
            }
        }
    }
}
