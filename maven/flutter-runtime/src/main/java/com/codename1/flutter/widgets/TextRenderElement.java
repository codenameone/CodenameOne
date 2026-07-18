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

    private void applyStyle(Label l) {
        TextStyle ts = text().getStyle();
        if (ts != null) {
            Font base = l.getUnselectedStyle().getFont();
            if (base == null) {
                base = Font.getDefaultFont();
            }
            if (base != null && (ts.getFontSize() != null || ts.getFontWeight() != null)) {
                float sizePx = ts.getFontSize() != null
                        ? (float) Dp.px(ts.getFontSize())
                        : (base.getPixelSize() > 0 ? base.getPixelSize() : base.getHeight());
                int weight = (ts.getFontWeight() != null && ts.getFontWeight().isBold())
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
        List<String> lines = wrap(data(), new Funcs.Func1<String, Double>() {
            @Override
            public Double call(String s) {
                return (double) f.stringWidth(s);
            }
        }, constraints.maxWidth());
        l.lines = lines;
        double w = 0;
        for (String line : lines) {
            w = Math.max(w, f.stringWidth(line));
        }
        double h = (double) f.getHeight() * Math.max(1, lines.size());
        return constraints.constrain(new Size(w, h));
    }

    private static Font font(Label l) {
        Font f = l.getUnselectedStyle().getFont();
        return f != null ? f : Font.getDefaultFont();
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

        WrappedLabel(String text) {
            super(text, "FlutterText");
        }

        @Override
        public void paint(Graphics g) {
            if (lines == null || lines.size() <= 1) {
                super.paint(g);
                return;
            }
            com.codename1.ui.plaf.Style s = getStyle();
            Font f = s.getFont();
            if (f == null) {
                f = Font.getDefaultFont();
            }
            if (f == null) {
                return;
            }
            g.setColor(s.getFgColor());
            g.setFont(f);
            int lh = f.getHeight();
            int y = getY();
            int align = s.getAlignment();
            for (String line : lines) {
                int x = getX();
                if (align == Component.CENTER) {
                    x += (getWidth() - f.stringWidth(line)) / 2;
                } else if (align == Component.RIGHT) {
                    x += getWidth() - f.stringWidth(line);
                }
                g.drawString(line, x, y);
                y += lh;
            }
        }
    }
}
