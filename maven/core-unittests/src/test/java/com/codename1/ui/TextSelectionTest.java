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
    void disablingRemovesEveryListenerEnablingAdded() {
        Form form = Display.getInstance().getCurrent();
        TextSelection selection = form.getTextSelection();
        int before = longPressListenerCount(form);
        selection.setEnabled(true);
        assertEquals(before + 1, longPressListenerCount(form));
        selection.setEnabled(false);
        // The disable path used to ADD the long press listener a second time instead of
        // removing it, so a long press kept driving a selection that was switched off.
        assertEquals(before, longPressListenerCount(form));
    }

    private static int longPressListenerCount(Component c) {
        return c.longPressListeners == null ? 0 : c.longPressListeners.getListenerCollection().size();
    }

    @FormTest
    void defaultSelectableAppliesToReadOnlyTextOnly() {
        try {
            Label label = new Label("text");
            Button button = new Button("button");
            TextArea readOnly = new TextArea("read only");
            readOnly.setEditable(false);
            TextArea editable = new TextArea("editable");
            assertFalse(label.isTextSelectionEnabled(), "off by default");

            TextSelection.setDefaultSelectable(true);
            assertTrue(label.isTextSelectionEnabled());
            assertTrue(readOnly.isTextSelectionEnabled());
            assertFalse(button.isTextSelectionEnabled(), "a button's press is its action");
            assertFalse(editable.isTextSelectionEnabled(), "editing owns an editable field's presses");
            assertTrue(label.getTextSelectionSupport().isTextSelectionEnabled(null));

            // An explicit choice outranks the default in both directions.
            label.setTextSelectionEnabled(false);
            assertFalse(label.isTextSelectionEnabled());
            button.setTextSelectionEnabled(true);
            assertTrue(button.isTextSelectionEnabled());
        } finally {
            TextSelection.setDefaultSelectable(false);
        }
    }

    @FormTest
    void defaultSelectableSkipsTextInsideALeadComponent() {
        try {
            TextSelection.setDefaultSelectable(true);
            Container lead = new Container();
            Button leadButton = new Button("lead");
            Label inside = new Label("inside");
            lead.add(leadButton).add(inside);
            lead.setLeadComponent(leadButton);
            assertFalse(inside.isTextSelectionEnabled(), "the press belongs to the lead component");
            assertTrue(new Label("outside").isTextSelectionEnabled());
        } finally {
            TextSelection.setDefaultSelectable(false);
        }
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
        assertEquals("HE", intersected.getText());
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
