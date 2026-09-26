/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaf render box for {@link Text}: owns a CN1 Label (UIID "FlutterText")
 * whose intrinsic size is measured with the label's font
 * ({@code stringWidth}/{@code getHeight}). The TextStyle is applied
 * programmatically to the label's AllStyles (font derived from the current
 * style font, fg color from the Color).
 *
 * <p>M2 wraps: when the measured single line exceeds the incoming max width
 * the text is broken on words (hard character breaks for single words wider
 * than the line) via {@link #wrap}, the box reports the wrapped extent, and
 * the label paints the lines itself honoring {@link TextAlign}.</p>
 */
public class TextRenderElement extends RenderElement {

    public TextRenderElement(Text widget) {
        super(widget);
    }

    private Text text() {
        return (Text) widget();
    }

    @Override
    protected Component createComponent() {
        WrappedLabel l = new WrappedLabel(data());
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        applyStyle(l);
        return l;
    }

    @Override
    protected void updateComponent(Component c) {
        WrappedLabel l = (WrappedLabel) c;
        l.setText(data());
        l.lines = null;
        applyStyle(l);
    }

    private String data() {
        return text().getData() == null ? "" : text().getData();
    }

    /**
     * The style actually in force: the widget's own, over the ambient
     * {@code DefaultTextStyle}, field by field.
     *
     * <p>Flutter's rule, and the mechanism a container styles its text with. A
     * {@code Text} that sets only a size inside a white-on-purple app bar must
     * still come out white; reading only the widget's own style is what left
     * every themed bar with default-ink glyphs.</p>
     */
    private TextStyle effectiveStyle() {
        TextStyle own = text().getStyle();
        TextStyle ambient;
        try {
            ambient = DefaultTextStyle.of(this).getStyle();
        } catch (Throwable t) {
            ambient = null;
        }
        if (ambient == null) {
            return own;
        }
        if (own == null) {
            return ambient;
        }
        // Start from the ambient style and let the widget's own non-null fields
        // win: copyWith already ignores nulls, so this is exactly Flutter's
        // "the nearer style wins field by field".
        return ambient.copyWith(null, own.getColor(), null, own.getFontFamily(),
                own.getFontSize(), own.getFontWeight(), null, own.getLetterSpacing(),
                null, own.height(), null, null, null);
    }

    private void applyStyle(Label l) {
        TextStyle ts = effectiveStyle();
        if (l instanceof WrappedLabel) {
            double sp = ts == null || ts.getLetterSpacing() == null
                    ? 0 : Dp.px(ts.getLetterSpacing().doubleValue());
            ((WrappedLabel) l).spacingPx = sp;
            // A stated height wins; otherwise the FACE's own line height, read from the
            // font rather than taken from Codename One's typographic pair, which lays
            // text out shorter than Flutter does for the same file.
            ((WrappedLabel) l).lineHeightPx = ts == null || ts.height() == null
                    || ts.getFontSize() == null ? 0
                    : Dp.px(ts.getFontSize().doubleValue() * ts.height().doubleValue());
            if (((WrappedLabel) l).lineHeightPx <= 0 && ts != null
                    && ts.getFontSize() != null && ts.fontFamily() != null) {
                double ratio = com.codename1.flutter.fonts.FontResolver.lineHeightRatio(
                        ts.fontFamily(), ts.getFontWeight(), false);
                if (ratio > 0) {
                    ((WrappedLabel) l).lineHeightPx =
                            Dp.px(ts.getFontSize().doubleValue() * ratio);
                }
            }
            // A TRANSLUCENT ink is ordinary in Material: the 2018 type scale
            // paints its display roles at black54 and its body roles at
            // black87, and Codename One's Style carries only an opaque
            // foreground (its fgAlpha reaches the border, never the text). Kept
            // here and applied when the label paints.
            ((WrappedLabel) l).fgAlpha =
                    ts == null || ts.getColor() == null ? 255 : ts.getColor().alpha();
        }
        if (ts != null) {
            Font base = l.getUnselectedStyle().getFont();
            // A named family wins over whatever the theme put on the label:
            // the style is asking for a specific typeface, and that is the
            // difference between a study that looks like its design and one
            // painted entirely in the platform default.
            Font named = com.codename1.flutter.fonts.FontResolver.resolve(
                    ts.fontFamily(), ts.getFontWeight(), false);
            // Whether the face we ended up with ALREADY carries the asked-for
            // weight. Synthesising bold on top of a bold file doubles it.
            boolean weighted = named != null;
            if (named != null) {
                base = named;
            } else if (ts.getFontSize() != null) {
                // No bundled face, but a size was asked for -- and a size only
                // means something on a face that can derive. See
                // FontResolver.platformFace.
                Font platform = com.codename1.flutter.fonts.FontResolver.platformFace(
                        ts.getFontWeight(), false);
                if (platform != null) {
                    base = platform;
                    weighted = true;
                }
            }
            if (base == null) {
                base = Font.getDefaultFont();
            }
            if (base != null && (ts.getFontSize() != null || ts.getFontWeight() != null
                    || named != null)) {
                float sizePx = ts.getFontSize() != null
                        ? (float) Dp.px(ts.getFontSize())
                        : (base.getPixelSize() > 0 ? base.getPixelSize() : base.getHeight());
                // A resolved face ALREADY carries its weight (the Bold file was
                // picked, not the Regular one), so asking Codename One to bold
                // it again synthesises a second helping of weight on top.
                int weight = !weighted
                        && ts.getFontWeight() != null && ts.getFontWeight().isBold()
                        ? Font.STYLE_BOLD : Font.STYLE_PLAIN;
                try {
                    l.getAllStyles().setFont(base.derive(sizePx, weight));
                } catch (Exception err) {
                    // fonts that can't derive keep the base font
                }
            }
            if (ts.getColor() != null) {
                l.getAllStyles().setFgColor(ts.getColor().rgb());
            }
        }
        TextAlign a = text().getTextAlign();
        if (a != null) {
            int cn1Align;
            switch (a) {
                case right:
                case end:
                    cn1Align = Component.RIGHT;
                    break;
                case center:
                    cn1Align = Component.CENTER;
                    break;
                default:
                    cn1Align = Component.LEFT;
                    break;
            }
            l.getAllStyles().setAlignment(cn1Align);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        WrappedLabel l = (WrappedLabel) component();
        if (l == null) {
            return constraints.smallest();
        }
        final Font f = font(l);
        if (f == null) {
            return constraints.smallest();
        }
        final double spacing = l.spacingPx;
        List<String> lines = wrap(data(), new Funcs.Func1<String, Double>() {
            @Override
            public Double call(String s) {
                return spacedWidth(f, s, spacing);
            }
        }, constraints.maxWidth());
        lines = clamp(lines, effectiveMaxLines(), ellipsize(),
                new Funcs.Func1<String, Double>() {
                    @Override
                    public Double call(String s) {
                        return spacedWidth(f, s, spacing);
                    }
                }, constraints.maxWidth());
        if (!isDryPass()) {
            // Only a real pass may hand the painter its lines; see isDryPass().
            l.lines = lines;
        }
        double w = 0;
        for (String line : lines) {
            w = Math.max(w, spacedWidth(f, line, spacing));
        }
        double h = l.lineHeight(f) * Math.max(1, lines.size());
        return constraints.constrain(new Size(w, h));
    }

    /**
     * {@code Text.maxLines}, or the ambient {@code DefaultTextStyle}'s, or none.
     */
    private Long effectiveMaxLines() {
        if (text().getMaxLines() != null) {
            return text().getMaxLines();
        }
        try {
            return DefaultTextStyle.of(this).getMaxLines() == null ? null
                    : Long.valueOf(DefaultTextStyle.of(this).getMaxLines().longValue());
        } catch (Throwable t) {
            return null;
        }
    }

    /** Whether an over-long line ends in an ellipsis rather than being cut. */
    private boolean ellipsize() {
        return text().getOverflow() == com.codename1.flutter.TextOverflow.ellipsis;
    }

    /**
     * Cuts a wrapped paragraph down to {@code maxLines}, ending the last line
     * with an ellipsis when the text asked for one.
     *
     * <p>Both were parsed and dropped. A preview meant to be one line long
     * rendered its whole body instead, which does not look like a bug so much
     * as a different design: the Reply study's inbox showed every message in
     * full and pushed the rest of the list off the screen.</p>
     */
    static List<String> clamp(List<String> lines, Long maxLines, boolean ellipsis,
                              Funcs.Func1<String, Double> measure, double maxWidth) {
        if (maxLines == null || maxLines.longValue() <= 0
                || lines.size() <= maxLines.longValue()) {
            return lines;
        }
        int keep = (int) maxLines.longValue();
        List<String> out = new ArrayList<String>(lines.subList(0, keep));
        if (!ellipsis) {
            return out;
        }
        // The last kept line has to make room for the ellipsis, and the text
        // that follows it is what the ellipsis stands for.
        String last = out.get(keep - 1);
        String marker = "\u2026";
        while (last.length() > 0
                && measure.call(last + marker) > maxWidth
                && maxWidth != Double.POSITIVE_INFINITY) {
            last = last.substring(0, last.length() - 1);
        }
        out.set(keep - 1, trimEnd(last) + marker);
        return out;
    }

    private static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }

    private static Font font(Label l) {
        Font f = l.getUnselectedStyle().getFont();
        return f != null ? f : Font.getDefaultFont();
    }

    /**
     * The width of {@code s} once Flutter's letterSpacing is added BETWEEN its glyphs -
     * n-1 gaps for n characters, with no trailing space after the last, which is what
     * Flutter does. Codename One draws a whole string in one call and has no tracking of
     * its own, so both the measurement and the painting have to account for it here.
     */
    static double spacedWidth(Font f, String s, double spacing) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        // stringWidth measures the WHOLE run in one go, which is the accurate number and
        // the one the port itself would use. It is what this measures with, whether or
        // not there is tracking -- see trackingScale for how the paint path is made to
        // agree with it.
        return spacedWidth(f.stringWidth(s), s.length(), spacing);
    }

    /// The sum of the per-glyph advances the paint path would step through.
    static double sumCharWidths(Font f, String s) {
        double total = 0;
        for (int i = 0; i < s.length(); i++) {
            total += f.charWidth(s.charAt(i));
        }
        return total;
    }

    /// What to multiply each glyph's advance by so a run laid out glyph by glyph ends
    /// exactly where {@code stringWidth} says it should.
    ///
    /// Tracking forces the paint path to draw one glyph at a time, because Codename One
    /// advances a whole string in a single call and has no tracking of its own. But
    /// {@code charWidth} returns an INT, so every glyph's advance is rounded up to a
    /// whole pixel and the error accumulates: measured against the reference, the same
    /// sentence came out 607px wide where it should be 589 -- 3.1%, or about 0.6px per
    /// character. Wide text does not merely look wrong, it ellipsises strings that fit
    /// and clips the ones that do not.
    ///
    /// The whole-run {@code stringWidth} does not have that error, so use it for the
    /// total and let the per-glyph widths decide only the PROPORTIONS. Note this is not
    /// the same as measuring each glyph with {@code stringWidth}: a standalone space
    /// measures ~0 there, which is why the paint path uses charWidth in the first place
    /// -- as a proportion a space is correct, as an absolute width it is not.
    static double trackingScale(Font f, String s) {
        return trackingScale(f.stringWidth(s), sumCharWidths(f, s));
    }

    /// The scale arithmetic on its own, so the invariant it exists to hold -- that a run
    /// laid out glyph by glyph ends exactly where {@link #spacedWidth} said it would --
    /// can be pinned without a Font.
    static double trackingScale(double runWidth, double sumOfCharWidths) {
        if (sumOfCharWidths <= 0) {
            return 1;
        }
        return runWidth / sumOfCharWidths;
    }

    /** The tracking arithmetic on its own, so it can be pinned without a Font. */
    public static double spacedWidth(double baseWidth, int charCount, double spacing) {
        if (charCount <= 0) {
            return 0;
        }
        return spacing == 0 ? baseWidth : baseWidth + spacing * (charCount - 1);
    }

    // ------------------------------------------------------------------
    // Word wrapping (pure — headless-testable with stubbed metrics)
    // ------------------------------------------------------------------

    /**
     * Greedy word wrap: fills each line with whitespace-separated words up to
     * {@code maxWidth} (per the supplied measure function); a single word
     * wider than the line is hard-broken at the character level. Embedded
     * {@code \n} always breaks. An unbounded {@code maxWidth} yields the
     * paragraphs unwrapped.
     */
    public static List<String> wrap(String text, Funcs.Func1<String, Double> measure, double maxWidth) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            text = "";
        }
        for (String paragraph : split(text, '\n')) {
            if (maxWidth == Double.POSITIVE_INFINITY || measure.call(paragraph) <= maxWidth) {
                out.add(paragraph);
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : split(paragraph, ' ')) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (measure.call(candidate) <= maxWidth || line.length() == 0 && word.length() == 0) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                // the word alone: hard-break it if even alone it overflows
                while (measure.call(word) > maxWidth && word.length() > 1) {
                    int cut = word.length() - 1;
                    while (cut > 1 && measure.call(word.substring(0, cut)) > maxWidth) {
                        cut--;
                    }
                    out.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                line.append(word);
            }
            out.add(line.toString());
        }
        return out;
    }

    private static List<String> split(String s, char sep) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }

    /**
     * A Label that paints its wrapped lines itself once layout supplied them
     * (falling back to standard single-line Label painting before the first
     * layout pass).
     */
    static class WrappedLabel extends Label {

        List<String> lines;
        /** Flutter's TextStyle.letterSpacing, in device pixels. */
        double spacingPx;
        /// Flutter's {@code TextStyle.height} MULTIPLIED BY the font size, in device
        /// pixels; 0 when the style sets none and the font's own height should stand.
        ///
        /// The multiplier was parsed and merged and then never used -- the layout took
        /// the font's height and the painter advanced by it. Material specifies a height
        /// for most of its text styles (bodyMedium 1.43, titleLarge 1.27), so every block
        /// was set at the wrong leading, and down a long list the error accumulates until
        /// dividers land on the text they were meant to separate.
        double lineHeightPx;

        /** The height of one line: the style's, or the FACE's when it sets none. */
        double lineHeight(Font f) {
            if (lineHeightPx > 0) {
                return lineHeightPx;
            }
            if (f == null) {
                return 0;
            }
            // Ascent + descent, NOT getHeight(). The reference's line box for a style
            // that states no height is the face's own ascent and descent; getHeight() is
            // the platform's recommended line SPACING, which adds external leading on top
            // of that and is a different quantity.
            //
            // Measured on the typography demo, whose type scale states no height for any
            // role: two wrapped lines of a 96sp style sat 141 logical pixels apart here
            // against the reference's 115 -- a ratio of 1.47 where the face asks for
            // 1.198 -- and the error repeats on every line of every such style, so the
            // page drifted further out of register the further down it went.
            int ascent = f.getAscent();
            int descent = Math.abs(f.getDescent());
            int box = ascent + descent;
            // A port that does not answer for the face still has to lay text out.
            return box > 0 ? box : f.getHeight();
        }
        /** The ink's own alpha; see applyStyle. */
        int fgAlpha = 255;

        WrappedLabel(String text) {
            super(text, "FlutterText");
        }

        @Override
        public void paint(Graphics g) {
            // Paint from the wrapped lines whenever layout produced any — NOT
            // only when there is more than one. A single line is the interesting
            // case: it is what a clamped `maxLines: 1` produces, and falling
            // through to Label.paint here drew the label's raw text instead, so
            // every one-line preview in the Reply study rendered its whole
            // message and got cut off mid-word with no ellipsis.
            if (lines == null && spacingPx == 0 && fgAlpha >= 255) {
                super.paint(g);
                return;
            }
            boolean multiLine = lines != null;
            com.codename1.ui.plaf.Style s = getStyle();
            Font f = s.getFont();
            if (f == null) {
                f = Font.getDefaultFont();
            }
            if (f == null) {
                return;
            }
            int prevColor = g.getColor();
            Font prevFont = g.getFont();
            int prevAlpha = fgAlpha >= 255 ? -1 : g.concatenateAlpha(fgAlpha);
            g.setColor(s.getFgColor());
            g.setFont(f);
            int lh = (int) Math.round(lineHeight(f));
            int y = getY();
            // Flutter centres the glyphs in the line box, so a line taller than the font
            // pushes the text down by half the difference. Without this the run sits on
            // the box's top edge and every line is a little high.
            int glyphOffset = Math.max(0, (lh - f.getHeight()) / 2);
            int align = s.getAlignment();
            List<String> toPaint = multiLine ? lines
                    : java.util.Collections.singletonList(getText() == null ? "" : getText());
            if (toPaint.isEmpty()) {
                // The graphics is shared with every other component in the
                // frame, so an early exit still has to hand it back as it was.
                g.setColor(prevColor);
                if (prevAlpha >= 0) {
                    g.setAlpha(prevAlpha);
                }
                if (prevFont != null) {
                    g.setFont(prevFont);
                }
                return;
            }
            for (String line : toPaint) {
                int lineW = (int) Math.ceil(spacedWidth(f, line, spacingPx));
                int x = getX();
                if (align == Component.CENTER) {
                    x += (getWidth() - lineW) / 2;
                } else if (align == Component.RIGHT) {
                    x += getWidth() - lineW;
                }
                if (spacingPx == 0) {
                    g.drawString(line, x, y + glyphOffset);
                } else {
                    // One glyph at a time: the only way to add tracking, since Codename One
                    // draws a whole string in a single advance.
                    //
                    // Advance by charWidth, NOT by stringWidth of a one-character
                    // string: Codename One measures a standalone space as ~0 wide,
                    // so a space advanced by the tracking alone and the words ran
                    // together — Reply's headlines read "Packageshipped!".
                    double scale = trackingScale(f, line);
                    double cursor = x;
                    for (int i = 0; i < line.length(); i++) {
                        char ch = line.charAt(i);
                        g.drawString(line.substring(i, i + 1), (int) Math.round(cursor), y + glyphOffset);
                        cursor += f.charWidth(ch) * scale + spacingPx;
                    }
                }
                y += lh;
            }
            g.setColor(prevColor);
            if (prevAlpha >= 0) {
                g.setAlpha(prevAlpha);
            }
            if (prevFont != null) {
                g.setFont(prevFont);
            }
        }
    }
}
