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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.EventDispatcher;

/// A numeric field with increment and decrement controls: `NSStepper`, WinUI's `NumberBox`
/// with its spin buttons, `GtkSpinButton`.
///
/// Codename One had no equivalent. The nearest thing is `Slider`, which is a different
/// control for a different job -- a slider is for a value whose exact number does not
/// matter, and a stepper is for one where it does. An application that needed a bounded
/// number on the desktop had to build the composite by hand, and every hand-built one got
/// the same two things wrong: it let the field hold text that is not a number, and it let
/// the buttons walk past the bounds.
///
/// ```java
/// Stepper copies = new Stepper(1, 1, 99);
/// copies.addActionListener(e -> print(copies.getValue()));
/// ```
///
/// Three UIIDs: `Stepper` for the composite, `StepperField` for the text field and
/// `StepperButton` for the two buttons. The buttons carry `-` and `+` by default; a theme
/// or an application that wants arrows sets icons on `#getDecrementButton()` and
/// `#getIncrementButton()`.
///
/// The value is an int. A stepper over a fractional quantity is a real control on some
/// platforms, and it is deliberately not this one: doing it properly means a format, a
/// locale and a parse policy, and guessing those is worse than not offering them.
public class Stepper extends Container {
    /// The editable field.
    ///
    /// A plain NUMERIC TextField would make a negative range untypable: TextField.validChar
    /// answers `c >= '0' && c <= '9'` for NUMERIC, so in a -10..10 stepper the decrement
    /// button can reach -5 while the user cannot type it. DECIMAL is not the answer either --
    /// it admits '.' and ',', which this control cannot represent.
    ///
    /// So the minus is permitted here, and only when the range actually contains a negative
    /// value. validChar is asked about one character with no position, so it cannot insist the
    /// sign is leading; it does not need to. Text that is not an integer simply fails
    /// Integer.parseInt in commitTypedText, which returns without changing anything.
    private final TextField field = new TextField() {
        @Override
        public boolean validChar(String c) {
            if (c != null && c.length() == 1 && c.charAt(0) == '-' && minValue < 0) {
                return true;
            }
            return super.validChar(c);
        }
    };
    private final Button decrement = new Button("-", "StepperButton");
    private final Button increment = new Button("+", "StepperButton");
    private final EventDispatcher listeners = new EventDispatcher();

    private int value;
    private int minValue;
    private int maxValue;
    private int step = 1;

    /// A stepper over 0..100 starting at 0.
    public Stepper() {
        this(0, 0, 100);
    }

    /// A stepper over the given range.
    ///
    /// #### Parameters
    ///
    /// - `value`: the initial value, clamped into the range
    ///
    /// - `minValue`: the lowest value the control will produce
    ///
    /// - `maxValue`: the highest value the control will produce
    public Stepper(int value, int minValue, int maxValue) {
        super(new BorderLayout());
        setUIID("Stepper");
        if (maxValue < minValue) {
            throw new IllegalArgumentException("maxValue " + maxValue
                    + " is below minValue " + minValue);
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = clamp(value);

        field.setUIID("StepperField");
        field.setConstraint(TextField.NUMERIC);
        field.setText(String.valueOf(this.value));
        field.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                commitTypedText();
            }
        });
        field.addDataChangedListener(new com.codename1.ui.events.DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                commitTypedText();
            }
        });

        decrement.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setValue(saturate((long) Stepper.this.value - Stepper.this.step));
            }
        });
        increment.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setValue(saturate((long) Stepper.this.value + Stepper.this.step));
            }
        });

        Container buttons = new Container(BoxLayout.x());
        buttons.setUIID("Container");
        buttons.add(decrement).add(increment);
        add(BorderLayout.CENTER, field);
        add(BorderLayout.EAST, buttons);
        updateButtonState();
    }

    /// The current value.
    ///
    /// #### Returns
    ///
    /// the value, always within `#getMinValue()`..`#getMaxValue()`
    public int getValue() {
        return value;
    }

    /// Sets the value, clamped into the range. Fires the action listeners only when the
    /// value actually moved, so a caller that sets the same value twice does not report a
    /// change that did not happen.
    ///
    /// #### Parameters
    ///
    /// - `newValue`: the requested value
    public void setValue(int newValue) {
        int clamped = clamp(newValue);
        if (clamped == value) {
            // Still refresh the text: the user may have typed something out of range, in
            // which case the field and the value disagree and the field is the wrong one.
            syncField();
            updateButtonState();
            return;
        }
        value = clamped;
        syncField();
        updateButtonState();
        listeners.fireActionEvent(new ActionEvent(this));
    }

    /// The lowest value this control will produce.
    ///
    /// #### Returns
    ///
    /// the minimum
    public int getMinValue() {
        return minValue;
    }

    /// The highest value this control will produce.
    ///
    /// #### Returns
    ///
    /// the maximum
    public int getMaxValue() {
        return maxValue;
    }

    /// Sets the range. The current value is clamped into the new range, which fires the
    /// listeners when that moves it.
    ///
    /// #### Parameters
    ///
    /// - `minValue`: the lowest value
    ///
    /// - `maxValue`: the highest value
    public void setRange(int minValue, int maxValue) {
        if (maxValue < minValue) {
            throw new IllegalArgumentException("maxValue " + maxValue
                    + " is below minValue " + minValue);
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
        setValue(value);
    }

    /// How far one press of a button moves the value.
    ///
    /// #### Returns
    ///
    /// the step, at least 1
    public int getStep() {
        return step;
    }

    /// Sets how far one press of a button moves the value.
    ///
    /// #### Parameters
    ///
    /// - `step`: the step, which must be positive
    public void setStep(int step) {
        if (step < 1) {
            throw new IllegalArgumentException("step must be positive: " + step);
        }
        this.step = step;
    }

    /// The decrement button, for a caller that wants to give it an icon.
    ///
    /// #### Returns
    ///
    /// the decrement button
    public Button getDecrementButton() {
        return decrement;
    }

    /// The increment button, for a caller that wants to give it an icon.
    ///
    /// #### Returns
    ///
    /// the increment button
    public Button getIncrementButton() {
        return increment;
    }

    /// The editable field, for a caller that wants to make it read-only.
    ///
    /// #### Returns
    ///
    /// the text field
    public TextField getField() {
        return field;
    }

    /// Notified when the value changes, however it changed.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Stops notifying a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    /// Takes whatever the user typed and turns it into a value.
    ///
    /// An empty field is left alone rather than treated as zero: a user clearing the field
    /// to retype it would otherwise watch it fill itself in under the caret. Anything that
    /// is not a number is ignored the same way -- the field is corrected when the value is
    /// next set, which is what leaving the field does.
    private void commitTypedText() {
        String text = field.getText();
        if (text == null || text.length() == 0) {
            return;
        }
        int typed;
        try {
            typed = Integer.parseInt(text.trim());
        } catch (NumberFormatException err) {
            return;
        }
        if (typed == value) {
            return;
        }
        int clamped = clamp(typed);
        boolean moved = clamped != value;
        value = clamped;
        updateButtonState();
        if (clamped != typed) {
            // Out of range. Correct the field now rather than at focus loss, because the
            // number under the caret is not one this control can produce.
            syncField();
        }
        if (!moved) {
            // The typed text differed but clamping put the value back where it already was --
            // at 10 in a 1..10 stepper, typing 11 means 10. The documented value did not
            // change, so no event, which is the rule setValue already follows. The field has
            // still been corrected above.
            return;
        }
        listeners.fireActionEvent(new ActionEvent(this));
    }

    private void syncField() {
        String text = String.valueOf(value);
        if (!text.equals(field.getText())) {
            field.setText(text);
        }
    }

    /// Disables the button that cannot move: a desktop stepper at its bound greys out the
    /// half that would step past it rather than accepting a press that does nothing.
    private void updateButtonState() {
        decrement.setEnabled(value > minValue);
        increment.setEnabled(value < maxValue);
    }

    /// Narrows a value computed in `long` back to `int` without wrapping.
    ///
    /// The step arithmetic has to happen in `long`. A Stepper may legitimately span
    /// `Integer.MIN_VALUE`..`Integer.MAX_VALUE`, and in that range `value + step` overflows
    /// in `int` before `#setValue(int)` ever sees it -- incrementing near the maximum wraps to
    /// a negative number and the value jumps to the other end of the range instead of stopping
    /// at the top. Saturating here means clamp() receives the number the user asked for, and
    /// does its own job.
    ///
    /// #### Parameters
    ///
    /// - `candidate`: the value in long arithmetic
    ///
    /// #### Returns
    ///
    /// candidate, saturated to the int range
    private static int saturate(long candidate) {
        if (candidate > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (candidate < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) candidate;
    }

    private int clamp(int v) {
        if (v < minValue) {
            return minValue;
        }
        if (v > maxValue) {
            return maxValue;
        }
        return v;
    }
}
