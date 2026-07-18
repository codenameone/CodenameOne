package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom-painted leaf render box for {@link RichText} (UIID
 * "FlutterRichText", derived from Label):
 * <ol>
 *   <li>{@link #flatten} the TextSpan tree into styled {@link Run}s — each
 *       run's TextStyle is RESOLVED (child properties override, null
 *       properties inherit down the span chain);</li>
 *   <li>{@link #layoutRuns} wraps the runs into {@link Line}s of positioned
 *       {@link Seg}ments, measuring every piece with ITS OWN style (the
 *       multi-font generalization of TextRenderElement.wrap): greedy word
 *       wrap, words spanning run boundaries stay unbreakable, embedded
 *       {@code \n} always breaks, an over-long word is hard-broken at the
 *       character level;</li>
 *   <li>the retained label paints the segments at their offsets with
 *       per-segment fonts and colors, honoring {@link TextAlign} per
 *       line.</li>
 * </ol>
 *
 * <p>Mixed font sizes on one line are bottom-aligned — an approximation of
 * baseline alignment (CN1 Fonts expose no baseline metric).</p>
 */
public class RichTextRenderElement extends RenderElement {

    public RichTextRenderElement(RichText widget) {
        super(widget);
    }

    private RichText richText() {
        return (RichText) widget();
    }

    // ------------------------------------------------------------------
    // Component
    // ------------------------------------------------------------------

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        RichLabel l = new RichLabel();
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        applyAlignment(l);
        return l;
    }

    @Override
    protected void updateComponent(Component c) {
        RichLabel l = (RichLabel) c;
        l.lines = null;
        l.fonts = null;
        applyAlignment(l);
    }

    private void applyAlignment(Label l) {
        TextAlign a = richText().getTextAlign();
        int cn1Align;
        if (a == null) {
            cn1Align = Component.LEFT;
        } else {
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
        }
        l.getAllStyles().setAlignment(cn1Align);
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RichLabel l = (RichLabel) component();
        if (l == null) {
            return constraints.smallest();
        }
        Font base = l.getUnselectedStyle().getFont();
        if (base == null) {
            base = Font.getDefaultFont();
        }
        if (base == null) {
            return constraints.smallest();
        }
        final Font baseFont = base;
        final Map<TextStyle, Font> cache = new IdentityHashMap<TextStyle, Font>();
        SpanMetrics m = new SpanMetrics() {
            @Override
            public double width(String text, TextStyle style) {
                return fontFor(style, baseFont, cache).stringWidth(text);
            }

            @Override
            public double height(TextStyle style) {
                return fontFor(style, baseFont, cache).getHeight();
            }
        };
        List<Run> runs = flatten(richText().getText());
        List<Line> lines = layoutRuns(runs, m, constraints.maxWidth());
        l.lines = lines;
        l.fonts = cache;
        double w = 0;
        double h = 0;
        for (Line line : lines) {
            w = Math.max(w, line.width);
            h += line.height;
        }
        return constraints.constrain(new Size(w, h));
    }

    /**
     * Derives the CN1 font for a resolved style from the label's base font
     * (the same derivation TextRenderElement.applyStyle uses).
     */
    static Font fontFor(TextStyle style, Font base, Map<TextStyle, Font> cache) {
        if (style == null || (style.getFontSize() == null && style.getFontWeight() == null)) {
            return base;
        }
        Font f = cache.get(style);
        if (f != null) {
            return f;
        }
        f = base;
        try {
            float sizePx = style.getFontSize() != null
                    ? (float) Dp.px(style.getFontSize())
                    : (base.getPixelSize() > 0 ? base.getPixelSize() : base.getHeight());
            int weight = (style.getFontWeight() != null && style.getFontWeight().isBold())
                    ? Font.STYLE_BOLD : Font.STYLE_PLAIN;
            f = base.derive(sizePx, weight);
        } catch (Exception err) {
            // fonts that can't derive keep the base font
        }
        cache.put(style, f);
        return f;
    }

    // ------------------------------------------------------------------
    // Span flattening (pure — headless-testable)
    // ------------------------------------------------------------------

    /**
     * A flattened text run with its fully RESOLVED style.
     */
    public static class Run {
        public final String text;
        public final TextStyle style;

        public Run(String text, TextStyle style) {
            this.text = text;
            this.style = style;
        }
    }

    /**
     * Depth-first flattening of a span tree: a span's own text precedes its
     * children; each node's effective style is its own style with null
     * properties inherited from the parent chain. Null/empty texts
     * contribute no run (their children still do).
     */
    public static List<Run> flatten(TextSpan root) {
        List<Run> out = new ArrayList<Run>();
        collect(root, null, out);
        return out;
    }

    private static void collect(TextSpan span, TextStyle inherited, List<Run> out) {
        if (span == null) {
            return;
        }
        TextStyle eff = resolve(inherited, span.getStyle());
        if (span.getText() != null && span.getText().length() > 0) {
            out.add(new Run(span.getText(), eff));
        }
        if (span.getChildren() != null) {
            for (TextSpan c : span.getChildren()) {
                collect(c, eff, out);
            }
        }
    }

    /**
     * Style inheritance: the child's non-null properties win, everything
     * else comes from the parent. Identity is preserved when one side is
     * null (so the font cache can key resolved styles by identity).
     */
    public static TextStyle resolve(TextStyle parent, TextStyle child) {
        if (child == null) {
            return parent;
        }
        if (parent == null) {
            return child;
        }
        TextStyle out = new TextStyle();
        if (child.getFontSize() != null) {
            out.fontSize(child.getFontSize());
        } else if (parent.getFontSize() != null) {
            out.fontSize(parent.getFontSize());
        }
        if (child.getFontWeight() != null) {
            out.fontWeight(child.getFontWeight());
        } else if (parent.getFontWeight() != null) {
            out.fontWeight(parent.getFontWeight());
        }
        if (child.getColor() != null) {
            out.color(child.getColor());
        } else if (parent.getColor() != null) {
            out.color(parent.getColor());
        }
        if (child.getFontFamily() != null) {
            out.fontFamily(child.getFontFamily());
        } else if (parent.getFontFamily() != null) {
            out.fontFamily(parent.getFontFamily());
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Multi-run line layout (pure — headless-testable with stubbed metrics)
    // ------------------------------------------------------------------

    /**
     * Text measurement per resolved style; stubbed in unit tests, backed by
     * derived CN1 fonts at runtime.
     */
    public interface SpanMetrics {
        double width(String text, TextStyle style);

        double height(TextStyle style);
    }

    /**
     * One painted piece of a line: a run of characters sharing one style,
     * positioned at {@code x} from the line start.
     */
    public static class Seg {
        public String text;
        public final TextStyle style;
        public double x;
        public double width;

        Seg(String text, TextStyle style, double x, double width) {
            this.text = text;
            this.style = style;
            this.x = x;
            this.width = width;
        }
    }

    /**
     * One laid-out line: its segments, total advance width and height (the
     * tallest segment).
     */
    public static class Line {
        public final List<Seg> segs = new ArrayList<Seg>();
        public double width;
        public double height;
    }

    private static final int T_WORD = 0;
    private static final int T_SPACE = 1;
    private static final int T_NEWLINE = 2;

    private static class Frag {
        final String text;
        final TextStyle style;

        Frag(String text, TextStyle style) {
            this.text = text;
            this.style = style;
        }
    }

    private static class Tok {
        final int kind;
        final List<Frag> frags = new ArrayList<Frag>();
        TextStyle style;

        Tok(int kind) {
            this.kind = kind;
        }
    }

    /**
     * Greedy word wrap over styled runs: whitespace-separated words fill
     * each line up to {@code maxWidth}; adjacent word characters ACROSS run
     * boundaries form one unbreakable word (mid-word style changes don't
     * create break opportunities); {@code \n} always breaks; spaces at a
     * soft-wrapped line start are dropped; a word wider than a whole line is
     * hard-broken at the character level. An unbounded {@code maxWidth}
     * never soft-wraps.
     */
    public static List<Line> layoutRuns(List<Run> runs, SpanMetrics m, double maxWidth) {
        // Phase 1: tokenize into word groups (cross-run), spaces, newlines.
        List<Tok> toks = new ArrayList<Tok>();
        for (Run r : runs) {
            String t = r.text;
            int i = 0;
            int n = t.length();
            while (i < n) {
                char ch = t.charAt(i);
                if (ch == '\n') {
                    toks.add(new Tok(T_NEWLINE));
                    i++;
                } else if (ch == ' ') {
                    Tok sp = new Tok(T_SPACE);
                    sp.style = r.style;
                    toks.add(sp);
                    i++;
                } else {
                    int j = i;
                    while (j < n && t.charAt(j) != ' ' && t.charAt(j) != '\n') {
                        j++;
                    }
                    Tok last = toks.isEmpty() ? null : toks.get(toks.size() - 1);
                    if (last == null || last.kind != T_WORD) {
                        last = new Tok(T_WORD);
                        toks.add(last);
                    }
                    last.frags.add(new Frag(t.substring(i, j), r.style));
                    i = j;
                }
            }
        }

        // Phase 2: greedy fill.
        List<Line> lines = new ArrayList<Line>();
        Line cur = new Line();
        List<Tok> pendSpaces = new ArrayList<Tok>();
        boolean softBreak = false;
        TextStyle fallbackStyle = runs.isEmpty() ? null : runs.get(0).style;

        for (Tok tok : toks) {
            if (tok.kind == T_NEWLINE) {
                commit(lines, cur, m, fallbackStyle);
                cur = new Line();
                pendSpaces.clear();
                softBreak = false;
                continue;
            }
            if (tok.kind == T_SPACE) {
                if (cur.segs.isEmpty() && softBreak) {
                    continue; // spaces at a soft-wrapped line start are dropped
                }
                pendSpaces.add(tok);
                continue;
            }
            // word group
            double spaceW = 0;
            for (Tok s : pendSpaces) {
                spaceW += m.width(" ", s.style);
            }
            double gW = 0;
            for (Frag f : tok.frags) {
                gW += m.width(f.text, f.style);
            }
            if (!cur.segs.isEmpty() && cur.width + spaceW + gW > maxWidth) {
                // soft wrap; the separating spaces are dropped
                commit(lines, cur, m, fallbackStyle);
                cur = new Line();
                pendSpaces.clear();
                softBreak = true;
            }
            for (Tok s : pendSpaces) {
                emit(cur, " ", s.style, m);
            }
            pendSpaces.clear();
            if (cur.width + gW <= maxWidth || maxWidth == Double.POSITIVE_INFINITY) {
                for (Frag f : tok.frags) {
                    emit(cur, f.text, f.style, m);
                }
                if (!tok.frags.isEmpty()) {
                    fallbackStyle = tok.frags.get(tok.frags.size() - 1).style;
                }
                continue;
            }
            // the word alone overflows the line: hard-break char-wise
            for (Frag f : tok.frags) {
                String rem = f.text;
                while (rem.length() > 0) {
                    int cut = rem.length();
                    while (cut > 1 && cur.width + m.width(rem.substring(0, cut), f.style) > maxWidth) {
                        cut--;
                    }
                    if (cut == 1 && !cur.segs.isEmpty()
                            && cur.width + m.width(rem.substring(0, 1), f.style) > maxWidth) {
                        // not even one character fits on this line
                        commit(lines, cur, m, fallbackStyle);
                        cur = new Line();
                        softBreak = true;
                        continue;
                    }
                    emit(cur, rem.substring(0, cut), f.style, m);
                    rem = rem.substring(cut);
                    if (rem.length() > 0) {
                        commit(lines, cur, m, fallbackStyle);
                        cur = new Line();
                        softBreak = true;
                    }
                }
                fallbackStyle = f.style;
            }
        }
        if (!cur.segs.isEmpty()) {
            commit(lines, cur, m, fallbackStyle);
        }
        return lines;
    }

    /**
     * Appends text to the line at the current advance, merging into the last
     * segment when the style is the same instance.
     */
    private static void emit(Line line, String text, TextStyle style, SpanMetrics m) {
        double w = m.width(text, style);
        Seg last = line.segs.isEmpty() ? null : line.segs.get(line.segs.size() - 1);
        if (last != null && last.style == style) {
            last.text = last.text + text;
            last.width += w;
        } else {
            line.segs.add(new Seg(text, style, line.width, w));
        }
        line.width += w;
    }

    private static void commit(List<Line> lines, Line line, SpanMetrics m, TextStyle fallbackStyle) {
        double h = 0;
        for (Seg s : line.segs) {
            h = Math.max(h, m.height(s.style));
        }
        if (line.segs.isEmpty()) {
            h = m.height(fallbackStyle);
        }
        line.height = h;
        lines.add(line);
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * A Label that paints the laid-out segment lines itself (falling back to
     * empty standard painting before the first layout pass).
     */
    static class RichLabel extends Label {

        List<Line> lines;
        Map<TextStyle, Font> fonts;

        RichLabel() {
            super("", "FlutterRichText");
            setTickerEnabled(false);
        }

        @Override
        public void paint(Graphics g) {
            if (lines == null) {
                super.paint(g);
                return;
            }
            com.codename1.ui.plaf.Style s = getStyle();
            Font baseFont = s.getFont();
            if (baseFont == null) {
                baseFont = Font.getDefaultFont();
            }
            if (baseFont == null) {
                return;
            }
            int align = s.getAlignment();
            int y = getY();
            for (Line line : lines) {
                int shift = 0;
                if (align == Component.CENTER) {
                    shift = (int) Math.round((getWidth() - line.width) / 2);
                } else if (align == Component.RIGHT) {
                    shift = (int) Math.round(getWidth() - line.width);
                }
                for (Seg seg : line.segs) {
                    Font f = fonts == null ? null : fonts.get(seg.style);
                    if (f == null) {
                        f = baseFont;
                    }
                    g.setFont(f);
                    g.setColor(seg.style != null && seg.style.getColor() != null
                            ? seg.style.getColor().rgb()
                            : s.getFgColor());
                    // bottom-align mixed-size fonts (baseline approximation)
                    int dy = (int) Math.round(line.height - f.getHeight());
                    g.drawString(seg.text, getX() + shift + (int) Math.round(seg.x), y + dy);
                }
                y += Math.round(line.height);
            }
        }
    }
}
