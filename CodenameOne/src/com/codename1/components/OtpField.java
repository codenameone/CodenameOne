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
import com.codename1.ui.EditField;
import com.codename1.ui.Graphics;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;

import java.util.ArrayList;

/// Segmented one-time-code input -- one box per digit, with a caret that walks
/// from box to box as the code is typed. The standard entry screen for an SMS
/// or authenticator code, and the second half of phone number verification.
///
/// #### Example
///
/// ```java
/// OtpField otp = new OtpField(6);
/// otp.addCompleteListener(e -> verify(otp.getText()));
/// form.add(otp);
/// ```
///
/// #### Receiving the code from the SMS
///
/// The field carries `TextArea#ONE_TIME_CODE`, so the platform offers the code
/// from the incoming message by itself: on iOS the keyboard's suggestion bar
/// shows it above the keys, on Android the autofill service offers it on the
/// field. Accepting it fills every box at once. The application reads no
/// messages and asks for no messaging permission -- it only says what the field
/// is for, and the platform does the rest. A platform that cannot offer the
/// code is unaffected, and the code is typed.
///
/// This is why the boxes are drawn rather than being separate editors. A code
/// arrives as one value, and a row of one-character fields can only receive one
/// character of it. Behind the boxes is a single field holding the whole code,
/// so an offered code, a paste and a keyboard all land the same way.
///
/// #### Styling
///
/// Each box uses the UIID "OtpDigit" and the field itself uses "OtpField".
public class OtpField extends Container {

    private final int length;
    private final boolean numericOnly;
    private final TextField[] boxes;
    private final OtpInput input;
    private final ArrayList<ActionListener> completeListeners = new ArrayList<ActionListener>();
    private boolean complete;

    /// Builds a 6-digit numeric field -- the common case.
    public OtpField() {
        this(6, true);
    }

    /// Builds a field of the given length, numeric only.
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
    ///   character (alphanumeric codes are sometimes used)
    public OtpField(int length, boolean numericOnly) {
        super(new LayeredLayout());
        if (length < 2 || length > 16) {
            throw new IllegalArgumentException("OTP length must be between 2 and 16");
        }
        this.length = length;
        this.numericOnly = numericOnly;
        this.boxes = new TextField[length];
        setUIID("OtpField");
        Container row = new DigitRow();
        row.setUIID("Container");
        for (int i = 0; i < length; i++) {
            TextField tf = new TextField();
            tf.setUIID("OtpDigit");
            tf.setColumns(1);
            tf.setMaxSize(1);
            tf.setSingleLineTextArea(true);
            // display only: the value lives in the field underneath, and a box that
            // took focus would tear the single editing session into one per box
            tf.setEditable(false);
            tf.setFocusable(false);
            tf.getAllStyles().setAlignment(CENTER);
            boxes[i] = tf;
            row.add(tf);
        }
        add(FlowLayout.encloseCenter(row));
        input = new OtpInput(this);
        add(input);
        input.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                valueChanged();
            }
        });
    }

    /// Keeps the boxes showing the value and fires completion on the edit that
    /// fills the last one.
    private void valueChanged() {
        String text = input.getText();
        for (int i = 0; i < length; i++) {
            boxes[i].setText(i < text.length() ? text.substring(i, i + 1) : "");
        }
        boolean full = text.length() == length;
        if (full && !complete) {
            complete = true;
            ActionEvent evt = new ActionEvent(this);
            for (ActionListener listener : new ArrayList<ActionListener>(completeListeners)) {
                listener.actionPerformed(evt);
                if (evt.isConsumed()) {
                    break;
                }
            }
            return;
        }
        complete = full;
    }

    /// Drops everything this field will not accept: characters outside the
    /// allowed set, and anything past the last box. Applied to typing, paste,
    /// dictation and a code offered by the platform alike, because all four
    /// arrive through the same path.
    String accept(String text, int room) {
        if (text == null || room <= 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(text.length());
        for (int i = 0; i < text.length() && b.length() < room; i++) {
            char c = text.charAt(i);
            if (numericOnly && (c < '0' || c > '9')) {
                continue;
            }
            if (c == '\n' || c == '\r' || c == '\t') {
                continue;
            }
            b.append(c);
        }
        return b.toString();
    }

    /// The box the caret sits in, clamped to the last box once the code is full.
    TextField caretBox(int caretOffset) {
        int i = caretOffset;
        if (i >= length) {
            i = length - 1;
        }
        if (i < 0) {
            i = 0;
        }
        return boxes[i];
    }

    /// The row of boxes, pinned left to right.
    ///
    /// A code is a sequence of digits, and digits read left to right in every
    /// locale -- the platforms' own code fields do not mirror them. `BoxLayout`
    /// lays its children out in reverse when its parent is right to left, which
    /// would put the first digit on the right and show the whole code backwards
    /// on a Hebrew or Arabic form, so this row opts out of that. Everything that
    /// walks the boxes by position -- the caret, the hit test -- can then take
    /// their order as given.
    ///
    /// Re-applied after `initLaf`, which is where the look and feel assigns the
    /// flag: setting it once in the constructor would not survive being added to
    /// a form, let alone a theme change.
    private static final class DigitRow extends Container {
        DigitRow() {
            super(BoxLayout.x());
            setRTL(false);
        }

        @Override
        protected void initLaf(com.codename1.ui.plaf.UIManager uim) {
            super.initLaf(uim);
            setRTL(false);
        }
    }

    /// The box a tap at this absolute x landed on, or the count of boxes when it
    /// landed past the last one. Absolute rather than local because pointer
    /// coordinates arrive in form space.
    ///
    /// The boxes are in left-to-right order whatever the form's direction, which
    /// is what `DigitRow` is for, so this walks them in order.
    ///
    /// #### Parameters
    ///
    /// - `absX`: the absolute x of the pointer
    ///
    /// #### Returns
    ///
    /// the box index, from 0, or the length when the tap was past the last box
    int boxIndexAt(int absX) {
        for (int i = 0; i < length; i++) {
            if (absX < boxes[i].getAbsoluteX() + boxes[i].getWidth()) {
                return i;
            }
        }
        return length;
    }

    /// True once the caret has run past the last box, i.e. the code is full and
    /// the caret belongs at the trailing edge rather than in front of a digit.
    boolean caretPastEnd(int caretOffset) {
        return caretOffset >= length;
    }

    /// Returns the current value, in order from the first box to the last. A
    /// partial entry returns a shorter string.
    public String getText() {
        return input.getText();
    }

    /// Sets the value, one character per box. Excess characters are dropped, as
    /// are characters this field does not accept; a shorter string leaves the
    /// remaining boxes empty.
    ///
    /// #### Parameters
    ///
    /// - `code`: the value, or null to clear
    public void setText(String code) {
        String accepted = accept(code, length);
        input.setText(accepted);
        // setText resets the caret to the start; entry continues after the last
        // character that was set, which is where the next one belongs
        input.moveCaret(accepted.length(), false);
        valueChanged();
    }

    /// Clears every box and puts the caret back in the first one, ready for a
    /// fresh code.
    public void clear() {
        setText("");
        startEditing();
    }

    /// Focuses the field and opens the keyboard, so a verification screen can
    /// put the user straight into the code without a tap.
    public void startEditing() {
        input.requestFocus();
    }

    /// True when every box holds a character.
    public boolean isComplete() {
        return input.getText().length() == length;
    }

    /// Adds a listener fired on the edit that fills the last box. Useful to
    /// verify the code without a submit button.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addCompleteListener(ActionListener l) {
        if (l != null) {
            completeListeners.add(l);
        }
    }

    /// Removes a previously-registered listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeCompleteListener(ActionListener l) {
        completeListeners.remove(l);
    }

    /// Adds a listener fired on every change to the value, not only on the one
    /// that completes it.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addDataChangedListener(DataChangedListener l) {
        input.addDataChangedListener(l);
    }

    /// Removes a previously-registered listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeDataChangedListener(DataChangedListener l) {
        input.removeDataChangedListener(l);
    }

    /// Returns the box at `index`, which displays the character at that
    /// position. Useful for theming an individual box; the value itself is read
    /// and written through `#getText()` / `#setText(String)`, since a code is
    /// entered into the field as a whole rather than box by box.
    ///
    /// #### Parameters
    ///
    /// - `index`: the box position, from 0
    ///
    /// #### Returns
    ///
    /// the box at that position
    public TextField getBox(int index) {
        return boxes[index];
    }

    /// The field that actually holds the code and carries the one-time-code
    /// hint. It spans the boxes and draws only the caret. Exposed for the cases
    /// the boxes cannot serve: adding a done listener, or reading the caret.
    public EditField getInputField() {
        return input;
    }

    /// Returns the configured length (number of boxes).
    public int getLength() {
        return length;
    }

    /// True when the field accepts digits only.
    public boolean isNumericOnly() {
        return numericOnly;
    }

    /// The field that actually holds the code. It spans the boxes, draws
    /// nothing but the caret, and carries the one-time-code hint that lets the
    /// platform offer the code from the incoming message.
    private static final class OtpInput extends EditField {

        private final OtpField owner;
        private boolean caretOn = true;
        private long lastBlink;

        OtpInput(OtpField owner) {
            super("");
            this.owner = owner;
            setUIID("OtpFieldInput");
            // no pixels of its own: the boxes underneath are the field's appearance
            getAllStyles().setBgTransparency(0);
            getAllStyles().setBorder(Border.createEmpty());
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
            setConstraint((owner.numericOnly ? TextArea.NUMERIC : TextArea.ANY) | TextArea.ONE_TIME_CODE);
        }

        /// Correction and capitalization off, whatever the field's other settings.
        ///
        /// A code is not language. The platform would otherwise capitalize the first
        /// letter of an alphanumeric code and offer to correct the rest, and it does that
        /// BEFORE the value reaches this field -- so the user types the code they were
        /// sent, the keyboard changes it, and the server rejects a code that was correct
        /// when it left their hands. A digits-only field is safe from this by virtue of
        /// its keyboard; one that accepts letters is not, and the flags cost nothing
        /// either way.
        @Override
        public TextInputConfig getConfig() {
            return super.getConfig().setAutoCorrect(false).setAutoCapitalize(false);
        }

        @Override
        protected boolean handleTypedText(String text) {
            String accepted = limit(text);
            if (accepted.equals(text)) {
                return false;
            }
            if (accepted.length() > 0) {
                insertText(accepted);
            }
            return true;
        }

        /// Text arriving from a platform input source, which is the path a committed
        /// autocorrection, a pasted value, dictation and an offered code all take.
        ///
        /// Filtered here as well as in `#handleTypedText(String)` because the two do not
        /// meet: a commit that finalizes an IME composition replaces the composed range
        /// directly and never reaches the typed-text hook. An unfiltered value there would
        /// leave the field holding something it would refuse from the keyboard -- letters in
        /// a numeric code, or more characters than there are boxes -- which shows as a code
        /// that can never be complete and a verification that never fires.
        @Override
        public void commitText(String text) {
            super.commitText(limit(text));
        }

        /// The in-progress composition an IME, handwriting or dictation builds before it
        /// commits. It writes to the document directly, so it needs the same limit; without
        /// it the field can hold an unacceptable value for as long as the composition lasts,
        /// and dictation into a code field is composition from the first syllable.
        @Override
        public void setComposingText(String text, int relativeCaret) {
            super.setComposingText(limit(text), relativeCaret);
        }

        /// Where a tap puts the caret. The inherited hit test measures the field's own
        /// text, laid out from its left edge -- a rendering that exists in the metrics
        /// and nowhere on the screen, because this component draws boxes instead and
        /// this layer draws nothing. Answering from it puts the caret nowhere near the
        /// box the user aimed at, so a correction lands on the wrong digit.
        ///
        /// Clamped to the text, because a tap on an empty box means the end of what has
        /// been entered rather than an offset past it -- and the caller assigns this
        /// offset to the caret without clamping it itself.
        @Override
        public int offsetAtPoint(int absX, int absY) {
            return Math.min(owner.boxIndexAt(absX), getText().length());
        }

        /// Drops what this field will not take: characters outside the allowed set, and
        /// anything beyond the last box once the range this text replaces is accounted for.
        private String limit(String text) {
            TextInputState state = getEditingState();
            int replacedStart = state.getComposingStart();
            int replacedEnd = state.getComposingEnd();
            if (replacedStart < 0 || replacedEnd <= replacedStart) {
                replacedStart = getSelectionStart();
                replacedEnd = getSelectionEnd();
            }
            int used = getText().length() - (replacedEnd - replacedStart);
            return owner.accept(text, owner.length - used);
        }

        @Override
        protected Dimension calcPreferredSize() {
            // the boxes decide how big the field is; this layer only overlays them
            return new Dimension(0, 0);
        }

        @Override
        public void paint(Graphics g) {
            if (!caretOn || !hasFocus() || !isEditableState()) {
                return;
            }
            int caret = getCaretOffset();
            TextField box = owner.caretBox(caret);
            // The caret belongs to a box that is a cousin rather than a child, so its position
            // has to cross coordinate spaces. Painting happens with the ancestors' offsets
            // already applied to the Graphics -- which is why every component here draws at its
            // own getX() rather than its absolute position -- so an absolute coordinate would
            // add those offsets a second time and land the caret somewhere else, or outside the
            // clip and nowhere at all. The difference between this component's absolute and
            // local origin is exactly the translation in force, so subtracting it puts the box
            // in the space this Graphics is drawing in.
            int dx = getAbsoluteX() - getX();
            int dy = getAbsoluteY() - getY();
            int h = box.getHeight() / 2;
            int w = Math.max(1, box.getWidth() / 16);
            int boxX = box.getAbsoluteX() - dx;
            int x = owner.caretPastEnd(caret)
                    ? boxX + box.getWidth() - box.getStyle().getPaddingRight(isRTL()) - w
                    : boxX + (box.getWidth() - w) / 2;
            g.setColor(box.getStyle().getFgColor());
            g.fillRect(x, box.getAbsoluteY() - dy + (box.getHeight() - h) / 2, w, h);
        }

        @Override
        public boolean animate() {
            boolean sup = super.animate();
            if (hasFocus()) {
                long now = System.currentTimeMillis();
                if (now - lastBlink >= 500) {
                    caretOn = !caretOn;
                    lastBlink = now;
                    return true;
                }
            }
            return sup;
        }
    }
}
