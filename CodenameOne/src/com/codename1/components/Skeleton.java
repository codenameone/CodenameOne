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
import com.codename1.ui.CommonProgressAnimations;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/// A skeleton screen placeholder (also called a shimmer loader, content
/// placeholder or ghost element): the grey outline of content that is still
/// loading, with a light band sweeping across it.
///
/// Show one where content will land, so the screen says "loading" without a
/// spinner and does not jump when the content arrives:
///
/// ```java
/// Form form = new Form("Orders", new BorderLayout());
/// Container list = new Container(BoxLayout.y());
/// list.add(Skeleton.rows(4));
/// form.add(BorderLayout.CENTER, list);
/// form.show();
/// // Stands in for a network call that answers after 2 seconds.
/// UITimer.timer(2000, false, form, () -> {
///     list.removeAll();
///     for (String order : new String[] {"Order #101", "Order #102"}) {
///         list.add(new MultiButton(order));
///     }
///     list.animateLayout(200);
/// });
/// ```
///
/// Or mask a component that already exists until its data is ready:
///
/// ```java
/// Form form = new Form("Profile", BoxLayout.y());
/// SpanLabel bio = new SpanLabel();
/// form.add(bio);
/// form.show();
/// Skeleton.show(bio, Skeleton.text(3));
/// UITimer.timer(2000, false, form, () -> {
///     bio.setText("Builds mobile apps in Java.");
///     Skeleton.hide(bio, CommonTransitions.createFade(200));
/// });
/// ```
///
/// The shapes are drawn in the foreground colour of the [Skeleton] UIID. A
/// theme that does not style that UIID gets the theme's text colour at a low
/// opacity, which suits light and dark themes alike. Restyle the UIID, or pass
/// your own with `setUIID`, to change the colour, padding or margin. The shimmer runs only while
/// the placeholder is on screen; turn it off with `#shimmer(boolean)`.
///
/// [Skeleton] is a `CommonProgressAnimations.ProgressAnimation`, so it also works
/// with `CommonProgressAnimations.ProgressAnimation#markComponentLoading`.
///
/// See also [InfiniteProgress] for a blocking spinner and [EmptyState] for what
/// to show when loading finished with nothing to show.
public class Skeleton extends CommonProgressAnimations.ProgressAnimation {

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        // A UIID the theme does not define gets a default style that is
        // opaque white, which reads as a white box in dark mode. Unstyled,
        // this component draws only what it paints itself.
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }
    private static final int BLOCK = 0;
    private static final int ROWS = 1;
    private static final int TEXT = 2;
    private static final int CIRCLE = 3;
    private static final int RECT = 4;
    private static final int CARD = 5;

    /// How tall one list row is drawn, in millimetres: a row of two lines.
    private static final float ROW_MM = 9.6f;
    /// How tall one line of text is, in millimetres, including its gap.
    private static final float LINE_MM = 4.2f;
    /// One sweep of the shimmer, in milliseconds.
    private static final int PERIOD = 1400;
    /// Opacity of the placeholder shapes, and of the band over them.
    private static final int BASE_ALPHA = 0x26;
    private static final int BAND_ALPHA = 0x40;
    private static final int BAND_STEPS = 6;
    /// Widths of a row's title and second line, in percent, cycled per row.
    private static final int[] TITLE_WIDTHS = {48, 41, 34};
    private static final int[] SUBTITLE_WIDTHS = {30, 39};
    /// Widths of the full lines of a paragraph, in percent, cycled per line.
    private static final int[] TEXT_WIDTHS = {100, 92};

    private final int shape;
    private final int count;
    private final float widthMM;
    private final float heightMM;
    private boolean shimmer = true;
    private long start;

    /// A placeholder block that fills whatever size it is given. This is the
    /// shape `#show(Component)` uses to mask a component.
    public Skeleton() {
        this(BLOCK, 1, 0, 0);
    }

    private Skeleton(int shape, int count, float widthMM, float heightMM) {
        this.shape = shape;
        this.count = Math.max(1, count);
        this.widthMM = widthMM;
        this.heightMM = heightMM;
        setUIID("Skeleton");
        setFocusable(false);
    }

    /// The outline of a list: per row, a round icon, a title, a shorter second
    /// line and a value at the far end.
    ///
    /// #### Parameters
    ///
    /// - `rows`: how many rows to outline
    ///
    /// #### Returns
    ///
    /// the placeholder
    public static Skeleton rows(int rows) {
        return new Skeleton(ROWS, rows, 0, 0);
    }

    /// Lines of text, the last one shorter, as a paragraph would end.
    ///
    /// #### Parameters
    ///
    /// - `lines`: how many lines
    ///
    /// #### Returns
    ///
    /// the placeholder
    public static Skeleton text(int lines) {
        return new Skeleton(TEXT, lines, 0, 0);
    }

    /// A circle, for an avatar or an icon.
    ///
    /// #### Parameters
    ///
    /// - `diameterMM`: the diameter in millimetres
    ///
    /// #### Returns
    ///
    /// the placeholder
    public static Skeleton circle(float diameterMM) {
        return new Skeleton(CIRCLE, 1, diameterMM, diameterMM);
    }

    /// A rounded rectangle, for an image, a button or a chart.
    ///
    /// #### Parameters
    ///
    /// - `widthMM`: the width in millimetres; 0 or less takes the full width
    ///
    /// - `heightMM`: the height in millimetres
    ///
    /// #### Returns
    ///
    /// the placeholder
    public static Skeleton rect(float widthMM, float heightMM) {
        return new Skeleton(RECT, 1, widthMM, heightMM);
    }

    /// A card: an image across the top, then a title and two lines of text.
    ///
    /// #### Returns
    ///
    /// the placeholder
    public static Skeleton card() {
        return new Skeleton(CARD, 1, 0, 0);
    }

    /// Turns the moving shimmer band on or off. It is on by default; off, the
    /// placeholder is a still outline.
    ///
    /// #### Parameters
    ///
    /// - `shimmer`: true for the moving band
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public Skeleton shimmer(boolean shimmer) {
        this.shimmer = shimmer;
        repaint();
        return this;
    }

    /// Sets the UIID, for chaining: `Skeleton.rows(5).uiid("MySkeleton")`.
    ///
    /// #### Parameters
    ///
    /// - `uiid`: the style whose foreground the shapes are drawn in
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public Skeleton uiid(String uiid) {
        setUIID(uiid);
        return this;
    }

    /// Whether the shimmer band is on.
    ///
    /// #### Returns
    ///
    /// true if the band moves across the placeholder
    public boolean isShimmer() {
        return shimmer;
    }

    /// Replaces a component in its parent with a block placeholder of the same
    /// preferred size until `#hide(Component)` is called. Calling it again while
    /// the component is masked returns the placeholder already showing.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: a component that has a parent
    ///
    /// #### Returns
    ///
    /// the placeholder that took its place
    public static Skeleton show(Component cmp) {
        return show(cmp, new Skeleton());
    }

    /// Replaces a component in its parent with the given placeholder until
    /// `#hide(Component)` is called, for instance `Skeleton.text(3)` in place of
    /// a `SpanLabel`.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: a component that has a parent
    ///
    /// - `placeholder`: what to show in its place
    ///
    /// #### Returns
    ///
    /// the placeholder showing; when the component was already masked by a
    /// [Skeleton], that one; null when another kind of progress animation
    /// already masks it
    public static Skeleton show(Component cmp, Skeleton placeholder) {
        CommonProgressAnimations.ProgressAnimation p =
                CommonProgressAnimations.ProgressAnimation.markComponentLoading(cmp, placeholder);
        return p instanceof Skeleton ? (Skeleton) p : null;
    }

    /// Puts a component masked by `#show(Component)` back in its place. Does
    /// nothing if it is not masked.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to restore
    public static void hide(Component cmp) {
        hide(cmp, null);
    }

    /// Puts a component masked by `#show(Component)` back in its place with a
    /// transition, such as a fade. Does nothing if it is not masked.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to restore
    ///
    /// - `t`: the transition, or null to swap at once
    public static void hide(Component cmp, Transition t) {
        CommonProgressAnimations.ProgressAnimation.markComponentReady(cmp, t);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        start = System.currentTimeMillis();
    }

    @Override
    public boolean animate() {
        return shimmer && isVisible();
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        int w;
        int h;
        switch (shape) {
            case ROWS:
                w = CN.convertToPixels(40f);
                h = count * CN.convertToPixels(ROW_MM);
                break;
            case TEXT:
                w = CN.convertToPixels(40f);
                h = count * CN.convertToPixels(LINE_MM);
                break;
            case CIRCLE:
            case RECT:
                w = widthMM > 0 ? CN.convertToPixels(widthMM) : CN.convertToPixels(40f);
                h = CN.convertToPixels(heightMM);
                break;
            case CARD:
                w = CN.convertToPixels(50f);
                h = CN.convertToPixels(28f) + 3 * CN.convertToPixels(LINE_MM) + CN.convertToPixels(2f);
                break;
            default:
                w = CN.convertToPixels(10f);
                h = CN.convertToPixels(5f);
                break;
        }
        return new Dimension(w + style.getHorizontalPadding(), h + style.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int x = getX() + style.getPaddingLeftNoRTL();
        int y = getY() + style.getPaddingTop();
        int w = getWidth() - style.getHorizontalPadding();
        int h = getHeight() - style.getVerticalPadding();
        if (w <= 0 || h <= 0) {
            return;
        }
        int alpha = g.getAlpha();
        int color = g.getColor();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        boolean themed = getUIManager().isThemeStyleDefined(getUIID());
        g.setColor(themed ? style.getFgColor()
                : getUIManager().getComponentStyle("Label").getFgColor());
        g.setAlpha(themed ? alpha : BASE_ALPHA * alpha / 255);
        shapes(g, x, y, w, h);
        if (shimmer) {
            band(g, x, y, w, h, alpha);
        }
        g.setAlpha(alpha);
        g.setColor(color);
        g.setAntiAliased(aa);
    }

    /// The shimmer: the shapes drawn again in white through a clip that sweeps
    /// across them, which lightens a placeholder on a light theme and on a dark
    /// one alike. The band is built from a few steps of rising
    /// then falling opacity so it reads as a soft highlight, not a stripe.
    private void band(Graphics g, int x, int y, int w, int h, int alpha) {
        int bandW = Math.max(w / 3, CN.convertToPixels(12f));
        long elapsed = (System.currentTimeMillis() - start) % PERIOD;
        int travel = w + bandW;
        int offset = (int) (elapsed * travel / PERIOD) - bandW;
        int bandX = isRTL() ? x + w - offset - bandW : x + offset;
        int[] clip = g.getClip();
        g.setColor(0xffffff);
        int step = Math.max(1, bandW / BAND_STEPS);
        for (int iter = 0; iter < BAND_STEPS; iter++) {
            int peak = iter < BAND_STEPS / 2 ? iter + 1 : BAND_STEPS - iter;
            g.setClip(clip);
            g.clipRect(bandX + iter * step, y, step, h);
            g.setAlpha(BAND_ALPHA * peak * 2 / BAND_STEPS * alpha / 255);
            shapes(g, x, y, w, h);
        }
        g.setClip(clip);
    }

    private void shapes(Graphics g, int x, int y, int w, int h) {
        int radius = CN.convertToPixels(1.2f);
        switch (shape) {
            case ROWS:
                rows(g, x, y, w);
                break;
            case TEXT:
                lines(g, x, y, w, count);
                break;
            case CIRCLE: {
                int d = Math.min(w, h);
                int cx = isRTL() ? x + w - d : x;
                g.fillArc(cx, y, d, d, 0, 360);
                break;
            }
            case RECT: {
                int rw = widthMM > 0 ? Math.min(w, CN.convertToPixels(widthMM)) : w;
                int rx = isRTL() ? x + w - rw : x;
                g.fillRoundRect(rx, y, rw, h, radius * 2, radius * 2);
                break;
            }
            case CARD: {
                int line = CN.convertToPixels(LINE_MM);
                int image = Math.max(0, h - 3 * line - CN.convertToPixels(2f));
                g.fillRoundRect(x, y, w, image, radius * 2, radius * 2);
                lines(g, x, y + image + CN.convertToPixels(2f), w, 3);
                break;
            }
            default:
                g.fillRoundRect(x, y, w, h, radius * 2, radius * 2);
                break;
        }
    }

    /// Lines of text: each a bar two thirds of a line tall, the last shorter.
    private void lines(Graphics g, int x, int y, int w, int lines) {
        int line = CN.convertToPixels(LINE_MM);
        int bar = line * 2 / 3;
        for (int iter = 0; iter < lines; iter++) {
            int lw = iter == lines - 1 && lines > 1 ? w * 3 / 5 : w * TEXT_WIDTHS[iter % 2] / 100;
            bar(g, x, w, 0, y + iter * line + (line - bar) / 2, lw, bar);
        }
    }

    private void rows(Graphics g, int x, int y, int w) {
        int row = CN.convertToPixels(ROW_MM);
        int dot = CN.convertToPixels(4.4f);
        int tall = CN.convertToPixels(1.9f);
        int thin = CN.convertToPixels(1.5f);
        int gap = CN.convertToPixels(2.6f);
        if (w <= dot + gap) {
            return;
        }
        int lines = w - dot - gap;
        for (int iter = 0; iter < count; iter++) {
            int middle = y + iter * row + row / 2;
            // Each row a little shorter than the one before: a list of
            // identical bars reads as a pattern, not as a list.
            int first = lines * TITLE_WIDTHS[iter % TITLE_WIDTHS.length] / 100;
            int second = lines * SUBTITLE_WIDTHS[iter % SUBTITLE_WIDTHS.length] / 100;
            int value = lines * 14 / 100;
            bar(g, x, w, 0, middle - dot / 2, dot, dot);
            bar(g, x, w, dot + gap, middle - tall - thin / 3, first, tall);
            bar(g, x, w, dot + gap, middle + thin * 2 / 3, second, thin);
            bar(g, x, w, w - value, middle - tall / 2, value, tall);
        }
    }

    /// One bar, `from` pixels in from the side a line of text starts at.
    private void bar(Graphics g, int left, int width, int from, int y, int w, int h) {
        int x = isRTL() ? left + width - from - w : left + from;
        g.fillRoundRect(x, y, w, h, h, h);
    }
}
