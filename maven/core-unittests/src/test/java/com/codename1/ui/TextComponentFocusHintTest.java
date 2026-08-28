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
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moving focus between two empty {@link TextComponent}s in the floating hint mode used to leave the
 * label the blur animation created behind in the component, so the hint was painted twice - once by
 * the field and once by the leftover label (issue #5600).
 */
class TextComponentFocusHintTest extends UITestBase {

    private static void pump(Form form) {
        AnimationManager am = form.getAnimationManager();
        long deadline = System.currentTimeMillis() + 3000;
        int idle = 0;
        while (System.currentTimeMillis() < deadline && idle < 10) {
            am.updateAnimations();
            if (am.isAnimating()) {
                idle = 0;
            } else {
                idle++;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static int countLabelsWithText(Container c, String text) {
        int count = 0;
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component cmp = c.getComponentAt(i);
            if (cmp instanceof Container) {
                count += countLabelsWithText((Container) cmp, text);
            } else if (cmp instanceof Label && text.equals(((Label) cmp).getText())) {
                count++;
            }
        }
        return count;
    }

    private static TextComponent floatingHintComponent(String label) {
        TextComponent tc = new TextComponent().label(label);
        tc.focusAnimation(true).onTopMode(true);
        return tc;
    }

    private static void assertBlurred(TextComponent tc, String label) {
        assertEquals(1, countLabelsWithText(tc, label),
                "a blurred " + label + " field must carry exactly one label with its text");
        assertEquals(label, tc.getField().getHint(), "the blurred field paints its own hint");
        assertFalse(tc.getLabel().isVisible(), "the floating label is hidden while the hint shows");
    }

    private static void assertFocused(TextComponent tc, String label) {
        assertEquals(1, countLabelsWithText(tc, label),
                "a focused " + label + " field must carry exactly one label with its text");
        assertEquals("", tc.getField().getHint(), "the focused field hands its hint to the label");
        assertTrue(tc.getLabel().isVisible(), "the floating label replaces the hint on focus");
    }

    @FormTest
    void movingFocusBetweenEmptyFieldsDoesNotDuplicateTheHint() {
        Form form = new Form("TextComponent Test", BoxLayout.y());
        TextComponent first = floatingHintComponent("Firstname");
        TextComponent last = floatingHintComponent("Lastname");
        form.addAll(first, last);
        form.show();
        form.revalidate();

        assertBlurred(first, "Firstname");
        assertBlurred(last, "Lastname");

        // the reproducer: tab between the two fields without ever typing anything
        for (int round = 0; round < 2; round++) {
            first.getField().requestFocus();
            pump(form);
            assertFocused(first, "Firstname");
            assertBlurred(last, "Lastname");

            last.getField().requestFocus();
            pump(form);
            assertBlurred(first, "Firstname");
            assertFocused(last, "Lastname");
        }
    }

    @FormTest
    void focusMovingFasterThanTheAnimationStillSettlesOnTheFocusedField() {
        Form form = new Form("TextComponent Test", BoxLayout.y());
        TextComponent first = floatingHintComponent("Firstname");
        TextComponent last = floatingHintComponent("Lastname");
        form.addAll(first, last);
        form.show();
        form.revalidate();

        // four focus changes with no frame in between: every one of them lands while the animation the
        // previous one started is still queued
        first.getField().requestFocus();
        last.getField().requestFocus();
        first.getField().requestFocus();
        last.getField().requestFocus();
        pump(form);

        assertBlurred(first, "Firstname");
        assertFocused(last, "Lastname");
    }

    @FormTest
    void aFormTornDownMidTransitionLeavesNothingBehind() {
        Form form = new Form("TextComponent Test", BoxLayout.y());
        TextComponent first = floatingHintComponent("Firstname");
        form.add(first);
        form.show();
        form.revalidate();

        // start the transition and abandon it: deinitializing the form flushes the animation queue
        // without running completion callbacks
        first.getField().requestFocus();
        Form other = new Form("Elsewhere", BoxLayout.y());
        other.show();

        assertEquals(1, countLabelsWithText(first, "Firstname"),
                "the temporary label must not survive the form it was animating in");

        // and the transition guard must have reopened, so the component still reacts to focus
        form.show();
        form.revalidate();
        first.getField().requestFocus();
        pump(form);
        assertFocused(first, "Firstname");
    }

    @FormTest
    void abandoningATransitionAfterFocusMovedOnRestsInTheBlurredState() {
        Form form = new Form("TextComponent Test", BoxLayout.y());
        TextComponent first = floatingHintComponent("Firstname");
        TextComponent last = floatingHintComponent("Lastname");
        form.addAll(first, last);
        form.show();
        form.revalidate();

        // the first field's hint-to-label transition is still queued when focus moves on, so the
        // reverse transition is never started; leaving the form must not commit the stale end state
        first.getField().requestFocus();
        last.getField().requestFocus();
        Form other = new Form("Elsewhere", BoxLayout.y());
        other.show();

        assertBlurred(first, "Firstname");
    }

    @FormTest
    void contentArrivingWhileTheReverseTransitionIsQueuedKeepsTheFloatingLabel() {
        Form form = new Form("TextComponent Test", BoxLayout.y());
        TextComponent first = floatingHintComponent("Firstname");
        TextComponent last = floatingHintComponent("Lastname");
        form.addAll(first, last);
        form.show();
        form.revalidate();

        first.getField().requestFocus();
        pump(form);
        assertFocused(first, "Firstname");

        // the label-to-hint transition is queued while the field is empty, then content arrives
        last.getField().requestFocus();
        first.text("value");
        pump(form);

        assertTrue(first.getLabel().isVisible(),
                "a blurred field holding content keeps its floating label");
        assertEquals(1, countLabelsWithText(first, "Firstname"));
    }

    @FormTest
    void contentArrivingBeforeTheFormIsHiddenKeepsTheFloatingLabel() {
        Form form = new Form("TextComponent Test", BoxLayout.y());
        TextComponent first = floatingHintComponent("Firstname");
        TextComponent last = floatingHintComponent("Lastname");
        form.addAll(first, last);
        form.show();
        form.revalidate();

        first.getField().requestFocus();
        pump(form);

        last.getField().requestFocus();
        first.text("value");
        Form other = new Form("Elsewhere", BoxLayout.y());
        other.show();

        assertTrue(first.getLabel().isVisible(),
                "a blurred field holding content keeps its floating label");
    }
}
