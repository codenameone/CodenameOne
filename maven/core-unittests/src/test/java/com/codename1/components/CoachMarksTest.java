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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class CoachMarksTest extends UITestBase {

    @FormTest
    void stepsAdvanceAndFinish() {
        Form f = new Form(BoxLayout.y());
        Button a = new Button("A");
        Button b = new Button("B");
        f.add(a).add(b);
        f.show();
        final int[] finished = {0};
        final int[] clicked = {0};
        a.addActionListener(e -> clicked[0]++);
        CoachMarks tour = new CoachMarks(f).add(a, "A", "First").add(b, null, "Second")
                .setOnFinish(() -> finished[0]++);
        assertEquals(2, tour.getStepCount());
        tour.show();
        assertTrue(tour.isShowing());
        assertEquals(0, tour.getCurrentStep());

        // A tap on the lit target is taken by the tour, not the button.
        int ax = a.getAbsoluteX() + a.getWidth() / 2;
        int ay = a.getAbsoluteY() + a.getHeight() / 2;
        f.pointerPressed(ax, ay);
        f.pointerReleased(ax, ay);
        assertEquals(0, clicked[0]);
        assertEquals(1, tour.getCurrentStep());

        tour.next();
        assertFalse(tour.isShowing());
        assertEquals(-1, tour.getCurrentStep());
        assertEquals(1, finished[0]);
        f.pointerPressed(ax, ay);
        f.pointerReleased(ax, ay);
        assertEquals(1, clicked[0], "the form gets touches back once the tour ends");
    }

    @FormTest
    void dismissEndsAtOnce() {
        Form f = new Form(BoxLayout.y());
        Button a = new Button("A");
        f.add(a);
        f.show();
        CoachMarks tour = new CoachMarks(f).add(a, "A", "First").add(a, "A", "Again");
        tour.show();
        tour.dismiss();
        assertFalse(tour.isShowing());
    }

    @FormTest
    void showOnceShowsOnlyTheFirstTime() {
        Form f = new Form(BoxLayout.y());
        Button a = new Button("A");
        f.add(a);
        f.show();
        CoachMarks.resetShowOnce("test-tour");
        CoachMarks first = new CoachMarks(f).add(a, "A", "First");
        assertTrue(first.showOnce("test-tour"));
        first.dismiss();
        CoachMarks second = new CoachMarks(f).add(a, "A", "First");
        assertFalse(second.showOnce("test-tour"));
        assertFalse(second.isShowing());
        CoachMarks.resetShowOnce("test-tour");
        assertTrue(second.showOnce("test-tour"));
        second.dismiss();
        CoachMarks.resetShowOnce("test-tour");
    }

    @FormTest
    void buttonActionsDriveTheTourWithoutAPointer() {
        Form f = new Form(BoxLayout.y());
        Button a = new Button("A");
        f.add(a);
        f.show();
        CoachMarks tour = new CoachMarks(f).add(a, "A", "First").add(a, "A", "Again");
        tour.show();
        Button next = findButton(f, "Next");
        assertNotNull(next);
        next.pressed();
        next.released();
        assertEquals(1, tour.getCurrentStep());
        tour.dismiss();
    }

    @FormTest
    void leavingTheFormEndsTheTour() {
        Form f = new Form(BoxLayout.y());
        Button a = new Button("A");
        f.add(a);
        f.show();
        final int[] finished = {0};
        CoachMarks tour = new CoachMarks(f).add(a, "A", "First").setOnFinish(() -> finished[0]++);
        tour.show();
        new Form().show();
        assertFalse(tour.isShowing());
        assertEquals(1, finished[0]);
    }

    @FormTest
    void showOnceWithoutStepsIsNotRemembered() {
        Form f = new Form(BoxLayout.y());
        f.show();
        CoachMarks.resetShowOnce("empty-tour");
        assertFalse(new CoachMarks(f).showOnce("empty-tour"));
        Button a = new Button("A");
        f.add(a);
        CoachMarks tour = new CoachMarks(f).add(a, "A", "First");
        assertTrue(tour.showOnce("empty-tour"));
        tour.dismiss();
        CoachMarks.resetShowOnce("empty-tour");
    }

    private static Button findButton(com.codename1.ui.Container root, String text) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            com.codename1.ui.Component c = root.getComponentAt(i);
            if (c instanceof Button && text.equals(((Button) c).getText())) {
                return (Button) c;
            }
            if (c instanceof com.codename1.ui.Container) {
                Button b = findButton((com.codename1.ui.Container) c, text);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }
}
