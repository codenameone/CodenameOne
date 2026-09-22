/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three controls the desktop design languages have and Codename One did not:
 * {@link Separator}, {@link GroupBox} and {@link Stepper}.
 */
class DesktopComponentsTest extends UITestBase {

    // ---- Separator ---------------------------------------------------------------

    @FormTest
    void separatorIsNeverThinnerThanAPixel() {
        // A theme is free to ask for a hairline, and a hairline rounds to zero millimetres
        // worth of pixels on a dense screen. Zero would make the rule invisible rather than
        // thin, and invisible is the one thing a separator must not be.
        Hashtable props = new Hashtable();
        props.put("@separatorThicknessMM", "0.0001");
        UIManager.getInstance().addThemeProps(props);

        Separator s = new Separator();
        assertTrue(s.getThickness() >= 1, "a hairline must still occupy a pixel");
    }

    @FormTest
    void separatorFallsBackWhenTheConstantIsMalformed() {
        Hashtable props = new Hashtable();
        props.put("@separatorThicknessMM", "not a number");
        UIManager.getInstance().addThemeProps(props);

        // Must not throw: this is read from paint, where an exception takes the whole form
        // down rather than one rule.
        assertEquals(1, new Separator().getThickness(), "a malformed constant falls back to 1px");
    }

    @FormTest
    void separatorHonoursTheThemeConstant() {
        Hashtable props = new Hashtable();
        props.put("@separatorThicknessMM", "2");
        UIManager.getInstance().addThemeProps(props);

        int expected = Display.getInstance().convertToPixels(2f);
        assertEquals(Math.max(1, expected), new Separator().getThickness());
    }

    @FormTest
    void separatorIsNotFocusable() {
        // Decoration. Stopping on it with Tab would be a bug on every platform, and the
        // desktop traversal filter is focusability, so this is what keeps it out.
        assertFalse(new Separator().isFocusable(), "a rule must not take focus");
    }

    @FormTest
    void separatorPrefersItsThicknessOnTheRightAxis() {
        Hashtable props = new Hashtable();
        props.put("@separatorThicknessMM", "2");
        UIManager.getInstance().addThemeProps(props);

        Separator horizontal = new Separator(Separator.HORIZONTAL);
        Separator vertical = new Separator(Separator.VERTICAL);
        Form f = new Form("Rules", BoxLayout.y());
        f.add(horizontal).add(vertical);
        f.show();
        DisplayTest.flushEdt();

        assertTrue(horizontal.getPreferredH() >= horizontal.getThickness(),
                "a horizontal rule is as tall as it is thick");
        assertTrue(vertical.getPreferredW() >= vertical.getThickness(),
                "a vertical rule is as wide as it is thick");
    }

    // ---- GroupBox ----------------------------------------------------------------

    @FormTest
    void groupBoxAddsIntoItsContentPane() {
        // Container.add(Component) is final and delegates to addComponent, which is what the
        // routing overrides. If that ever stops being true this is the test that says so.
        GroupBox g = new GroupBox("Appearance");
        CheckBox accent = new CheckBox("Use the system accent colour");
        g.add(accent);

        assertSame(g.getContentPane(), accent.getParent(),
                "an ordinary add belongs to the group, not beside the caption");
    }

    @FormTest
    void groupBoxRoutesAConstrainedAddIntoTheContentPane() {
        GroupBox g = new GroupBox("Layout", new BorderLayout());
        Label south = new Label("bottom");
        g.add(BorderLayout.SOUTH, south);

        assertSame(g.getContentPane(), south.getParent(),
                "BorderLayout.SOUTH means 'below the other controls in this group'");
    }

    @FormTest
    void groupBoxWithNoCaptionReservesNoStrip() {
        GroupBox g = new GroupBox();
        assertTrue(g.getTitleComponent().isHidden(),
                "an untitled group is a plain box, not a box with a blank strip");

        g.setTitle("Now titled");
        assertFalse(g.getTitleComponent().isHidden(), "setting a caption brings the strip back");

        g.setTitle("");
        assertTrue(g.getTitleComponent().isHidden(), "and clearing it takes the strip away again");
    }

    @FormTest
    void groupBoxRemoveAllKeepsItsOwnStructure() {
        GroupBox g = new GroupBox("Privacy");
        g.add(new CheckBox("One")).add(new CheckBox("Two"));
        g.removeAll();

        assertEquals(0, g.getContentPane().getComponentCount(), "the grouped controls are gone");
        assertSame(g, g.getTitleComponent().getParent(), "the caption is not one of them");
        assertSame(g, g.getContentPane().getParent(), "and neither is the content pane");
    }

    // ---- Stepper -----------------------------------------------------------------

    @FormTest
    void stepperClampsToItsRange() {
        Stepper s = new Stepper(5, 1, 10);
        s.setValue(99);
        assertEquals(10, s.getValue(), "a value above the range clamps to the maximum");
        s.setValue(-99);
        assertEquals(1, s.getValue(), "and below it clamps to the minimum");
    }

    @FormTest
    void stepperConstructorClampsTheInitialValue() {
        assertEquals(10, new Stepper(50, 1, 10).getValue());
        assertEquals(1, new Stepper(-50, 1, 10).getValue());
    }

    @FormTest
    void stepperFiresOnlyWhenTheValueMoved() {
        final List<Integer> seen = new ArrayList<Integer>();
        final Stepper s = new Stepper(5, 1, 10);
        s.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                seen.add(Integer.valueOf(s.getValue()));
            }
        });

        s.setValue(6);
        s.setValue(6);
        s.setValue(10);
        s.setValue(99);

        assertEquals(2, seen.size(), "setting the same value twice is not a change, and neither"
                + " is clamping to a bound the value already sat on");
        assertEquals(Integer.valueOf(6), seen.get(0));
        assertEquals(Integer.valueOf(10), seen.get(1));
    }

    @FormTest
    void stepperButtonsStepAndGreyOutAtTheBounds() {
        Stepper s = new Stepper(2, 1, 3);
        Button down = s.getDecrementButton();
        Button up = s.getIncrementButton();
        assertTrue(down.isEnabled() && up.isEnabled(), "mid-range both halves work");

        up.released();
        assertEquals(3, s.getValue());
        assertFalse(up.isEnabled(), "at the maximum the half that would step past it greys out");
        assertTrue(down.isEnabled());

        down.released();
        down.released();
        assertEquals(1, s.getValue());
        assertFalse(down.isEnabled(), "and likewise at the minimum");
        assertTrue(up.isEnabled());
    }

    @FormTest
    void stepperHonoursItsStep() {
        Stepper s = new Stepper(0, 0, 100);
        s.setStep(25);
        s.getIncrementButton().released();
        assertEquals(25, s.getValue());
        assertThrows(IllegalArgumentException.class, () -> s.setStep(0),
                "a zero step is a button that does nothing, which is a bug not a configuration");
    }

    @FormTest
    void stepperRejectsAnInvertedRange() {
        assertThrows(IllegalArgumentException.class, () -> new Stepper(0, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new Stepper(0, 0, 10).setRange(10, 1));
    }

    @FormTest
    void stepperTakesTypedTextAndCorrectsWhatItCannotProduce() {
        Stepper s = new Stepper(5, 1, 10);
        s.getField().setText("7");
        assertEquals(7, s.getValue(), "a typed number in range becomes the value");
        assertEquals("7", s.getField().getText(), "and the field is left as typed");

        s.getField().setText("99");
        assertEquals(10, s.getValue(), "out of range clamps");
        assertEquals("10", s.getField().getText(),
                "and the field is corrected, because the number under the caret is not one"
                        + " this control can produce");
    }

    @FormTest
    void stepperLeavesAnEmptyOrUnparseableFieldAlone() {
        Stepper s = new Stepper(5, 1, 10);
        s.getField().setText("");
        assertEquals(5, s.getValue(), "clearing the field to retype it must not reset the value");
        assertEquals("", s.getField().getText(), "and must not fill itself in under the caret");

        s.getField().setText("abc");
        assertEquals(5, s.getValue(), "nor does text that is not a number");
    }

    @FormTest
    void stepperPartsAreReachableByTheDesktopKeyboard() {
        // The composite is three focusable controls, which is what a desktop user tabs
        // through. Asserted because the parts are built internally: nothing else would
        // notice if one of them stopped being focusable.
        Stepper s = new Stepper(5, 1, 10);
        Form f = new Form("Stepper", BoxLayout.y());
        f.add(s);
        f.show();
        DisplayTest.flushEdt();

        assertTrue(s.getField().isFocusable(), "the field takes focus");
        assertTrue(s.getDecrementButton().isFocusable(), "so does the decrement button");
        assertTrue(s.getIncrementButton().isFocusable(), "and the increment button");
    }

    @FormTest
    void aStepperDoesNotFireWhenClampingLeavesTheValueWhereItWas() {
        // At the top of the range, typing something above it is not a change: the value was
        // 10 and clamping makes it 10 again. setValue already refuses to fire in that case;
        // the typed path used to fire anyway, so a listener saw a change event for a value
        // that never moved.
        Stepper s = new Stepper(10, 1, 10);
        Form f = new Form("Stepper", BoxLayout.y());
        f.add(s);
        f.show();
        DisplayTest.flushEdt();

        final int[] events = new int[1];
        s.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                events[0]++;
            }
        });

        s.getField().setText("11");
        DisplayTest.flushEdt();

        assertEquals(10, s.getValue(), "11 clamps back to the maximum");
        assertEquals(0, events[0],
                "no event: the documented value did not move, which is the rule setValue follows");
        assertEquals("10", s.getField().getText(), "the field is still corrected");
    }

    @FormTest
    void stepperArithmeticSaturatesInsteadOfWrapping() {
        // A legitimate full-int range. Incrementing near the top used to overflow in int
        // before clamp ever saw the number, so the value jumped to the bottom of the range
        // instead of stopping at the top.
        Stepper s = new Stepper(Integer.MAX_VALUE - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        s.setStep(10);
        Form f = new Form("Stepper", BoxLayout.y());
        f.add(s);
        f.show();
        DisplayTest.flushEdt();

        s.getIncrementButton().released();
        DisplayTest.flushEdt();
        assertEquals(Integer.MAX_VALUE, s.getValue(),
                "incrementing past the maximum must stop at it, not wrap to the minimum");

        s.setValue(Integer.MIN_VALUE + 1);
        s.getDecrementButton().released();
        DisplayTest.flushEdt();
        assertEquals(Integer.MIN_VALUE, s.getValue(),
                "and the same on the way down");
    }

    @FormTest
    void aNegativeStepperRangeAcceptsATypedMinusSign() {
        // TextField.validChar answers digits only for NUMERIC, so a -10..10 stepper could
        // reach -5 with the button and never by typing. The minus is permitted only when the
        // range actually contains a negative value.
        Stepper negative = new Stepper(0, -10, 10);
        Stepper positive = new Stepper(5, 1, 10);

        assertTrue(negative.getField().validChar("-"),
                "a range that includes negatives must accept the sign");
        assertTrue(negative.getField().validChar("4"), "digits still pass");
        assertFalse(positive.getField().validChar("-"),
                "a range with no negative value has no use for it");
        assertFalse(negative.getField().validChar("."),
                "and this is still an integer control -- DECIMAL would have allowed a point");
    }

    @FormTest
    void anUntitledGroupBoxReportsAnEmptyTitleRatherThanNull() {
        // getTitle() documents a non-null result and setTitle(null) already normalises;
        // the constructor stored the null it was given.
        assertEquals("", new GroupBox(null).getTitle(),
                "a null caption is the untitled case, not a null title");
        assertTrue(new GroupBox(null).getTitleComponent().isHidden(),
                "and it still hides the caption");
    }

    @FormTest
    void aStepperRestoresUnusableTextWhenEditingEnds() throws Exception {
        // Permitting '-' so a negative range is typable means the field can legitimately hold
        // "-" mid-edit. When editing ends it must not stay that way: getValue() still reports
        // the last good number, so the control would show one thing and report another.
        Stepper s = new Stepper(3, -10, 10);
        Form f = new Form("Stepper", BoxLayout.y());
        f.add(s);
        f.show();
        DisplayTest.flushEdt();

        s.getField().setText("-");
        DisplayTest.flushEdt();
        assertEquals("-", s.getField().getText(),
                "mid-edit the partial sign is left alone, or typing -5 would be impossible");
        assertEquals(3, s.getValue(), "and the value has not moved");

        // The edit-completion event TextField fires on focus loss. Not public across
        // packages, so reached the way this suite already reaches MenuBar's soft-key fields.
        java.lang.reflect.Method fire =
                com.codename1.ui.TextArea.class.getDeclaredMethod("fireActionEvent");
        fire.setAccessible(true);
        fire.invoke(s.getField());
        DisplayTest.flushEdt();

        assertEquals("3", s.getField().getText(),
                "when editing ends the field must go back to the value it reports");
        assertEquals(3, s.getValue(), "which is unchanged");
    }

    @FormTest
    void anAllNullContextMenuArrayIsNoMenuAtAll() {
        // An all-null array is nonempty, so a length test called it a menu: the component
        // consumed the right click, opened nothing, and an ancestor that did have a menu never
        // got to answer.
        Label child = new Label("child");
        child.setContextMenuCommands((com.codename1.ui.Command) null);
        assertNull(child.getContextMenuCommands(),
                "an array with no usable command is not a menu");

        Label real = new Label("real");
        real.setContextMenuCommands(new com.codename1.ui.Command("Cut"), null);
        assertNotNull(real.getContextMenuCommands(), "a usable command still makes a menu");
        assertEquals(1, real.getContextMenuCommands().length,
                "and the null beside it is dropped rather than carried");
    }

    @FormTest
    void aDisabledStepperStaysDisabledAcrossValueChanges() {
        // updateButtonState took only the bound into account, so anything that called it --
        // setValue, a typed commit -- re-enabled whichever button could move, and a control
        // the application had switched off became partly interactive again.
        Stepper s = new Stepper(5, 1, 10);
        Form f = new Form("Stepper", BoxLayout.y());
        f.add(s);
        f.show();
        DisplayTest.flushEdt();

        s.setEnabled(false);
        assertFalse(s.getDecrementButton().isEnabled(), "disabling the stepper disables its buttons");
        assertFalse(s.getIncrementButton().isEnabled(), "both of them");

        s.setValue(7);
        DisplayTest.flushEdt();
        assertFalse(s.getDecrementButton().isEnabled(),
                "and a value change must not quietly switch them back on");
        assertFalse(s.getIncrementButton().isEnabled(), "either of them");
    }

    @FormTest
    void aReEnabledStepperKeepsItsBoundState() {
        // Enabling a Container enables its children, so without re-applying the bounds a
        // stepper sitting at its maximum came back with an active increment button that
        // could not move the value.
        Stepper s = new Stepper(10, 1, 10);
        Form f = new Form("Stepper", BoxLayout.y());
        f.add(s);
        f.show();
        DisplayTest.flushEdt();

        s.setEnabled(false);
        s.setEnabled(true);
        DisplayTest.flushEdt();

        assertTrue(s.getDecrementButton().isEnabled(), "it can still go down from the maximum");
        assertFalse(s.getIncrementButton().isEnabled(),
                "but not up, and re-enabling must not pretend otherwise");
    }
}
