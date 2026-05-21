/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.Container;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;

/// Segmented one-time-password input -- one box per digit, auto-advances to
/// the next box on input and steps back on backspace. Standard pattern for
/// SMS / authenticator code entry screens.
///
/// #### Example
///
/// ```java
/// OtpField otp = new OtpField(6);
/// otp.addCompleteListener(new ActionListener() {
///     public void actionPerformed(ActionEvent evt) {
///         String code = otp.getText();
///         // verify code...
///     }
/// });
/// form.add(otp);
/// ```
///
/// Style the individual boxes with the UIID "OtpDigit"; the field itself uses
/// "OtpField".
public class OtpField extends Container {

    private final int length;
    private final boolean numericOnly;
    private final TextField[] boxes;
    private final ArrayList<ActionListener> completeListeners = new ArrayList<ActionListener>();
    private boolean updating;

    /// Builds a 6-digit numeric OTP field -- the common case.
    public OtpField() {
        this(6, true);
    }

    /// Builds an OTP field of the given length, numeric only.
    ///
    /// #### Parameters
    ///
    /// - `length`: number of digits / characters (e.g. 4, 6, 8)
    public OtpField(int length) {
        this(length, true);
    }

    /// Full constructor.
    ///
    /// #### Parameters
    ///
    /// - `length`: number of digits / characters
    ///
    /// - `numericOnly`: true to restrict input to digits; false to allow any
    ///   character (alphanumeric OTP codes are sometimes used)
    public OtpField(int length, boolean numericOnly) {
        super(BoxLayout.x());
        if (length < 2 || length > 16) {
            throw new IllegalArgumentException("OTP length must be between 2 and 16");
        }
        this.length = length;
        this.numericOnly = numericOnly;
        this.boxes = new TextField[length];
        setUIID("OtpField");
        buildBoxes();
    }

    private void buildBoxes() {
        for (int i = 0; i < length; i++) {
            final int index = i;
            final TextField tf = new TextField();
            tf.setUIID("OtpDigit");
            tf.setColumns(1);
            tf.setMaxSize(1);
            tf.setSingleLineTextArea(true);
            if (numericOnly) {
                tf.setConstraint(TextField.NUMERIC);
            }
            tf.addDataChangedListener(new DataChangedListener() {
                @Override
                public void dataChanged(int type, int idx) {
                    if (updating) {
                        return;
                    }
                    handleChange(index, tf);
                }
            });
            boxes[i] = tf;
            add(tf);
        }
    }

    private void handleChange(int index, TextField source) {
        String text = source.getText();
        if (text == null) {
            text = "";
        }
        // If multiple chars were pasted, distribute across boxes.
        if (text.length() > 1) {
            distributePaste(index, text);
            return;
        }
        if (text.length() == 1) {
            // advance focus to next box if not last
            if (index < length - 1) {
                boxes[index + 1].startEditingAsync();
            } else {
                fireCompleteIfFull();
            }
        } else {
            // empty -- step back to previous box on backspace
            if (index > 0) {
                boxes[index - 1].startEditingAsync();
            }
        }
    }

    private void distributePaste(int startIndex, String text) {
        updating = true;
        try {
            int p = startIndex;
            for (int i = 0; i < text.length() && p < length; i++) {
                char c = text.charAt(i);
                if (numericOnly && (c < '0' || c > '9')) {
                    continue;
                }
                boxes[p].setText(String.valueOf(c));
                p++;
            }
            // clear any remaining cells past where we wrote
            if (p > startIndex) {
                // last cell to focus is the one after the last written, or
                // the last box if we wrote to the end
                int focus = p < length ? p : length - 1;
                boxes[focus].startEditingAsync();
            }
        } finally {
            updating = false;
        }
        fireCompleteIfFull();
    }

    private void fireCompleteIfFull() {
        String code = getText();
        if (code.length() == length) {
            ActionEvent evt = new ActionEvent(this);
            for (ActionListener listener : completeListeners) {
                listener.actionPerformed(evt);
                if (evt.isConsumed()) {
                    break;
                }
            }
        }
    }

    /// Returns the current value, in order from the first box to the last.
    /// Empty boxes are omitted, so a partial entry returns a shorter string.
    public String getText() {
        StringBuilder b = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            String t = boxes[i].getText();
            if (t != null) {
                b.append(t);
            }
        }
        return b.toString();
    }

    /// Sets the value, distributing one character per box. Excess characters
    /// are silently dropped; shorter strings leave the remaining boxes empty.
    public void setText(String code) {
        updating = true;
        try {
            for (int i = 0; i < length; i++) {
                if (code != null && i < code.length()) {
                    boxes[i].setText(String.valueOf(code.charAt(i)));
                } else {
                    boxes[i].setText("");
                }
            }
        } finally {
            updating = false;
        }
    }

    /// Clears all boxes.
    public void clear() {
        setText("");
        boxes[0].startEditingAsync();
    }

    /// Adds a listener fired when the field becomes completely filled. Useful
    /// to trigger automatic verification.
    public void addCompleteListener(ActionListener l) {
        if (l != null) {
            completeListeners.add(l);
        }
    }

    /// Removes a previously-registered listener.
    public void removeCompleteListener(ActionListener l) {
        completeListeners.remove(l);
    }

    /// Returns the underlying [TextField] for the box at `index`. Useful for
    /// custom theming / focus management.
    public TextField getBox(int index) {
        return boxes[index];
    }

    /// Returns the configured length (number of boxes).
    public int getLength() {
        return length;
    }
}
