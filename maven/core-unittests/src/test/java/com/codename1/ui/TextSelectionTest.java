package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.TextSelection.Char;
import com.codename1.ui.TextSelection.Span;
import com.codename1.ui.TextSelection.Spans;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

class TextSelectionTest extends UITestBase {

    private static class DummySelectionComponent extends Label {
        private final String text;

        DummySelectionComponent(String text) {
            super(text);
            this.text = text;
            setPreferredSize(new Dimension(text.length() * 10, 20));
        }

        private Span createSpan(TextSelection sel) {
            Span span = sel.newSpan(this);
            for (int i = 0; i < text.length(); i++) {
                span.add(sel.newChar(i, i * 10, 0, 10, 20));
            }
            return span;
        }

        @Override
        public TextSelection.TextSelectionSupport getTextSelectionSupport() {
            return new TextSelection.TextSelectionSupport() {
                private Span fullSpan(TextSelection sel) {
                    return createSpan(sel);
                }

                @Override
                public Spans getTextSelectionForBounds(TextSelection sel, Rectangle bounds) {
                    Spans spans = sel.newSpans();
                    spans.add(fullSpan(sel).getIntersection(bounds, true));
                    return spans;
                }

                @Override
                public boolean isTextSelectionEnabled(TextSelection sel) {
                    return true;
                }

                @Override
                public boolean isTextSelectionTriggerEnabled(TextSelection sel) {
                    return true;
                }

                @Override
                public Span triggerSelectionAt(TextSelection sel, int x, int y) {
                    return fullSpan(sel).getIntersection(new Rectangle(x, y, 1, 1));
                }

                @Override
                public String getTextForSpan(TextSelection sel, Span span) {
                    int start = Math.max(0, span.getStartPos());
                    int end = Math.min(text.length(), span.getEndPos());
                    if (end <= start) {
                        return "";
                    }
                    return text.substring(start, end);
                }
            };
        }
    }

    @FormTest
    void testEnableDisableTextSelection() {
        Form form = Display.getInstance().getCurrent();
        TextSelection selection = form.getTextSelection();
        implementation.resetTextSelectionTracking();
        assertFalse(selection.isEnabled());
        assertSame(form.getContentPane(), selection.getSelectionRoot());

        selection.setEnabled(true);
        assertTrue(selection.isEnabled());
        assertEquals(1, implementation.getInitializeTextSelectionCount());
        assertSame(selection, implementation.getLastInitializedTextSelection());

        selection.setEnabled(false);
        assertFalse(selection.isEnabled());
        assertEquals(1, implementation.getDeinitializeTextSelectionCount());
        assertSame(selection, implementation.getLastDeinitializedTextSelection());
    }

    @FormTest
    void testSpanOperations() {
        Form form = Display.getInstance().getCurrent();
        TextSelection selection = form.getTextSelection();
        selection.getSelectionRoot();
        DummySelectionComponent component = new DummySelectionComponent("HELLO");

        Span span = selection.newSpan(component);
        for (int i = 0; i < 5; i++) {
            Char ch = selection.newChar(i, i * 10, 0, 10, 20);
            span.add(ch);
        }

        assertEquals(0, span.getStartPos());
        assertEquals(5, span.getEndPos());
        assertEquals(5, span.size());
        assertNotNull(span.first());
        assertNotNull(span.last());
        assertEquals(0, span.first().getPosition());
        assertEquals(4, span.last().getPosition());

        Rectangle bounds = span.getBounds();
        assertEquals(0, bounds.getX());
        assertEquals(0, bounds.getY());
        assertEquals(50, bounds.getWidth());
        assertEquals(20, bounds.getHeight());

        Char located = span.charAt(25, 10);
        assertNotNull(located);
        assertEquals(2, located.getPosition());

        Span sub = span.subspan(1, 4);
        assertEquals(3, sub.size());
        assertEquals(1, sub.getStartPos());
        assertEquals(4, sub.getEndPos());

        Span intersection = span.getIntersection(new Rectangle(10, 0, 20, 20));
        assertEquals(2, intersection.size());

        Span translated = span.translate(5, 5);
        assertEquals(5, translated.size());
        Rectangle translatedBounds = translated.getBounds();
        assertEquals(bounds.getX() + 5, translatedBounds.getX());
        assertEquals(bounds.getY() + 5, translatedBounds.getY());

        Spans spans = selection.newSpans();
        spans.add(span);
        assertFalse(spans.isEmpty());
        assertEquals("HELLO", spans.getText());
        assertNotNull(spans.charAt(40, 5));
        assertNotNull(spans.spanOfCharAt(40, 5));

        Spans intersected = spans.getIntersection(new Rectangle(0, 0, 20, 20), true);
        assertFalse(intersected.isEmpty());
        assertEquals("HEL", intersected.getText());
    }

    @FormTest
    void testNewCharTranslation() {
        Form form = Display.getInstance().getCurrent();
        TextSelection selection = form.getTextSelection();
        Char original = selection.newChar(3, 15, 5, 10, 10);
        Char moved = original.translate(7, 9);
        Rectangle originalBounds = new Rectangle(15, 5, 10, 10);
        Rectangle movedBounds = new Rectangle(originalBounds.getX() + 7, originalBounds.getY() + 9, 10, 10);
        assertEquals(originalBounds.getX(), 15);
        assertEquals(movedBounds.getX(), 22);
        assertEquals(3, moved.getPosition());
    }
}
