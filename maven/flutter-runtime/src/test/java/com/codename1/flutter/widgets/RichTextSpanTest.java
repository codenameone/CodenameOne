package com.codename1.flutter.widgets;

import com.codename1.flutter.Color;
import com.codename1.flutter.Colors;
import com.codename1.flutter.FontWeight;
import com.codename1.flutter.TextStyle;
import dart.core.DartList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TextSpan flattening + line layout — the pure halves of RichText, which is
 * why they are testable without a Display.
 */
public class RichTextSpanTest {

    private TextSpan span(String text, TextStyle style, TextSpan... children) {
        TextSpan s = new TextSpan();
        if (text != null) {
            s.text(text);
        }
        if (style != null) {
            s.style(style);
        }
        if (children.length > 0) {
            DartList<TextSpan> kids = new DartList<TextSpan>();
            for (TextSpan c : children) {
                kids.add(c);
            }
            s.children(kids);
        }
        return s;
    }

    private TextStyle style(Double size, FontWeight weight, Color color) {
        TextStyle t = new TextStyle();
        if (size != null) {
            t.fontSize(size);
        }
        if (weight != null) {
            t.fontWeight(weight);
        }
        if (color != null) {
            t.color(color);
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Flattening
    // ------------------------------------------------------------------

    @Test
    public void ownTextPrecedesChildrenDepthFirst() {
        TextSpan root = span("a", null,
                span("b", null, span("c", null)),
                span("d", null));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        StringBuilder sb = new StringBuilder();
        for (RichTextRenderElement.Run r : runs) {
            sb.append(r.text);
        }
        assertEquals("abcd", sb.toString());
    }

    @Test
    public void emptyOrNullTextContributesNoRunButChildrenSurvive() {
        TextSpan root = span(null, null, span("only", null));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        assertEquals(1, runs.size());
        assertEquals("only", runs.get(0).text);
    }

    @Test
    public void childInheritsParentStyleProperties() {
        TextStyle parent = style(20.0, FontWeight.bold, Colors.red);
        // child overrides only the color; size and weight must inherit
        TextSpan root = span("p", parent, span("c", style(null, null, Colors.blue)));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        assertEquals(2, runs.size());

        RichTextRenderElement.Run child = runs.get(1);
        assertEquals("c", child.text);
        assertEquals(20.0, child.style.getFontSize(), 0.001, "fontSize inherits");
        assertSame(FontWeight.bold, child.style.getFontWeight(), "fontWeight inherits");
        assertEquals(Colors.blue.value(), child.style.getColor().value(), "own color wins");
    }

    @Test
    public void deepInheritanceChains() {
        TextSpan root = span("a", style(30.0, null, null),
                span("b", null,
                        span("c", style(null, FontWeight.bold, null))));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        RichTextRenderElement.Run deepest = runs.get(2);
        assertEquals("c", deepest.text);
        assertEquals(30.0, deepest.style.getFontSize(), 0.001,
                "size inherits through an intermediate span with no style");
        assertSame(FontWeight.bold, deepest.style.getFontWeight());
    }

    // ------------------------------------------------------------------
    // Line layout (stubbed metrics: 10px per char, 20px line height)
    // ------------------------------------------------------------------

    private static final RichTextRenderElement.SpanMetrics METRICS =
            new RichTextRenderElement.SpanMetrics() {
                @Override
                public double width(String text, TextStyle style) {
                    return text.length() * 10.0;
                }

                @Override
                public double height(TextStyle style) {
                    return 20.0;
                }
            };

    private List<RichTextRenderElement.Line> layout(TextSpan root, double maxWidth) {
        return RichTextRenderElement.layoutRuns(
                RichTextRenderElement.flatten(root), METRICS, maxWidth);
    }

    @Test
    public void shortTextIsOneLine() {
        List<RichTextRenderElement.Line> lines = layout(span("hello", null), 1000);
        assertEquals(1, lines.size());
        assertEquals(50.0, lines.get(0).width, 0.001);
        assertEquals(20.0, lines.get(0).height, 0.001);
    }

    @Test
    public void wrapsOnWordBoundaries() {
        // "aaa bbb ccc" at 60px fits two 3-char words per line at most
        List<RichTextRenderElement.Line> lines = layout(span("aaa bbb ccc", null), 60);
        assertTrue(lines.size() >= 2, "must wrap: " + lines.size() + " line(s)");
        for (RichTextRenderElement.Line l : lines) {
            assertTrue(l.width <= 60.0 + 0.001, "line exceeds maxWidth: " + l.width);
        }
    }

    @Test
    public void newlineForcesLineBreak() {
        List<RichTextRenderElement.Line> lines = layout(span("a\nb", null), 1000);
        assertEquals(2, lines.size());
    }

    @Test
    public void runsFlowAcrossSpansOnTheSameLine() {
        // adjacent spans join on one line when they fit
        TextSpan root = span("ab", null, span("cd", null));
        List<RichTextRenderElement.Line> lines = layout(root, 1000);
        assertEquals(1, lines.size());
        assertEquals(40.0, lines.get(0).width, 0.001, "both runs share the line");
    }

    @Test
    public void adjacentSpansKeepDistinctStylesAsSeparateSegments() {
        // "Hello " plain + "world" bold — one line, but the bold run must
        // remain its own segment so it paints with its own font
        TextSpan root = span("Hello ", null, span("world", style(null, FontWeight.bold, null)));
        List<RichTextRenderElement.Line> lines = layout(root, 1000);
        assertEquals(1, lines.size());
        List<RichTextRenderElement.Seg> segs = lines.get(0).segs;
        assertTrue(segs.size() >= 2, "distinct styles must not merge: " + segs.size() + " seg(s)");

        RichTextRenderElement.Seg bold = segs.get(segs.size() - 1);
        assertEquals("world", bold.text);
        assertSame(FontWeight.bold, bold.style.getFontWeight());
        assertTrue(bold.x > 0, "the bold segment starts after the plain one");
    }

    @Test
    public void wordGroupsSpanRuns() {
        // "ab"+"cd" tokenize as ONE word group across the two spans (no
        // whitespace between them), so a width that fits the whole word
        // keeps it on one line rather than breaking at the span boundary
        TextSpan root = span("ab", null, span("cd", null));
        assertEquals(1, layout(root, 40).size());
    }

    @Test
    public void wordLongerThanTheLineBreaksByCharacter() {
        // Flutter hard-breaks a word that cannot fit the line at all; every
        // resulting line must still respect maxWidth
        TextSpan root = span("abcdefgh", null);
        List<RichTextRenderElement.Line> lines = layout(root, 25);
        assertTrue(lines.size() > 1, "an oversized word must break");
        for (RichTextRenderElement.Line l : lines) {
            assertTrue(l.width <= 25.0 + 0.001, "hard-broken line exceeds maxWidth: " + l.width);
        }
    }
}
