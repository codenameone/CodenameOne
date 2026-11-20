/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security;

import com.codename1.components.InteractionDialog;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.TextArea;
import com.codename1.ui.VirtualInputDevice;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;

/// An on-screen keyboard drawn by the application itself, for fields a keylogging keyboard must
/// never see.
///
/// On Android the system keyboard is an app the user chose, and it receives every keystroke typed
/// into every other app. A malicious one is a keylogger nothing in the app can switch off: password
/// and [TextArea#SENSITIVE] fields already ask the keyboard not to learn what is typed, but only a
/// well-behaved keyboard honours the request. A field with a `SecureKeyboard` installed is never
/// handed to the system keyboard at all. It is made uneditable, so the platform editor cannot open
/// on it, and a tap shows this keyboard instead, which writes into the field directly.
///
/// ```java
/// TextField pin = new TextField("", "PIN", 6, TextArea.NUMERIC | TextArea.PASSWORD);
/// SecureKeyboard kb = SecureKeyboard.install(pin);
/// kb.setScrambled(true);
/// ```
///
/// A common pattern is to install it only when [DeviceIntegrity#isThirdPartyKeyboardActive(String...)]
/// reports a user-installed keyboard, so most users keep the keyboard they know.
///
/// What it does against the threats a penetration test checks for:
///
/// - **Keylogging keyboards**: no keystroke goes through the platform input method.
/// - **Screen recording and overlays capturing a pressed key**: pressed keys are not enlarged in a
///   popup as system keyboards do. Combine it with [DeviceIntegrity#setSecureScreen(boolean)] to keep
///   the screen out of recordings, and [DeviceIntegrity#setTapjackingProtection(TapjackingPolicy)].
/// - **Tap-coordinate logging**: with [#setScrambled(boolean)] the digit keys move every time the
///   keyboard opens, so recorded touch positions do not map back to digits.
///
/// The layout follows the field's constraint: a number pad for [TextArea#NUMERIC], [TextArea#DECIMAL]
/// and [TextArea#PHONENUMBER], letters with `@` or `/` for [TextArea#EMAILADDR] and [TextArea#URL],
/// and letters otherwise. It is styled by the `SecureKeyboard` UIID for its background, and follows
/// the theme's dark mode.
///
/// It types ASCII only: no accented letters, no other scripts, no dictation, no autofill. That is
/// the price of keeping input away from the platform, and why it suits credentials and codes rather
/// than free text.
public final class SecureKeyboard extends Component {

    /// The key set, chosen from the field's constraint by [#layoutFor(int)].
    public enum Layout {
        /// Letters, with a symbols page.
        TEXT,
        /// Digits only.
        NUMBER,
        /// Digits and a decimal point.
        DECIMAL,
        /// Digits and `+`.
        PHONE,
        /// Letters with `@` and `.` beside the space bar.
        EMAIL,
        /// Letters with `/` and `.` beside the space bar.
        URL
    }

    private static final String CLIENT_PROPERTY = "cn1$secureKeyboard";
    private static final String DIGITS = "1234567890";

    private static final int KIND_CHAR = 0;
    private static final int KIND_SHIFT = 1;
    private static final int KIND_BACKSPACE = 2;
    private static final int KIND_RETURN = 3;
    private static final int KIND_SPACE = 4;
    private static final int KIND_MODE = 5;
    private static final int KIND_BLANK = 6;

    /// One key, positioned in component coordinates by the last layout pass.
    private static final class Key {
        final String label;
        final char icon;
        final int kind;
        final float weight;
        int x;
        int y;
        int width;
        int height;

        Key(String label, char icon, int kind, float weight) {
            this.label = label;
            this.icon = icon;
            this.kind = kind;
            this.weight = weight;
        }

        boolean contains(int px, int py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    private final TextArea field;
    private final boolean wasEditable;
    private final List<Key> keys = new ArrayList<Key>();
    private final com.codename1.ui.events.ActionListener tapListener;
    private Layout layout;
    private boolean shift;
    private boolean symbols;
    private boolean scrambled;
    private String digitOrder = DIGITS;
    private Key pressed;
    private int laidOutWidth = -1;
    private int laidOutHeight = -1;
    private InteractionDialog dialog;
    private Font textFont;
    private Font iconFont;
    private int fontForHeight = -1;

    private SecureKeyboard(TextArea field) {
        this.field = field;
        this.wasEditable = field.isEditable();
        this.layout = layoutFor(field.getConstraint());
        setUIID("SecureKeyboard");
        setFocusable(false);
        tapListener = new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                if (SecureKeyboard.this.field.isEnabled() && !isShowing()) {
                    show();
                }
            }
        };
    }

    /// Routes a field's input through a secure keyboard instead of the system keyboard.
    ///
    /// The field is made uneditable, which is what keeps the platform editor from ever opening on
    /// it, and a tap on it shows the returned keyboard. Installing twice returns the keyboard
    /// already installed.
    ///
    /// #### Parameters
    ///
    /// - `field`: the field to protect
    ///
    /// #### Returns
    ///
    /// the keyboard now serving the field
    public static SecureKeyboard install(TextArea field) {
        SecureKeyboard existing = getInstalled(field);
        if (existing != null) {
            return existing;
        }
        SecureKeyboard kb = new SecureKeyboard(field);
        field.putClientProperty(CLIENT_PROPERTY, kb);
        field.setEditable(false);
        field.addPointerReleasedListener(kb.tapListener);
        return kb;
    }

    /// Removes a keyboard added by [#install(TextArea)] and restores the field's editability.
    ///
    /// #### Parameters
    ///
    /// - `field`: the protected field
    public static void uninstall(TextArea field) {
        SecureKeyboard kb = getInstalled(field);
        if (kb == null) {
            return;
        }
        kb.hide();
        field.removePointerReleasedListener(kb.tapListener);
        field.putClientProperty(CLIENT_PROPERTY, null);
        field.setEditable(kb.wasEditable);
    }

    /// The keyboard installed on a field, or null.
    ///
    /// #### Parameters
    ///
    /// - `field`: the field to look up
    ///
    /// #### Returns
    ///
    /// its keyboard, or null when none is installed
    public static SecureKeyboard getInstalled(TextArea field) {
        Object o = field.getClientProperty(CLIENT_PROPERTY);
        return o instanceof SecureKeyboard ? (SecureKeyboard) o : null;
    }

    /// Picks the layout a field with this [TextArea] constraint gets.
    ///
    /// #### Parameters
    ///
    /// - `constraint`: a [TextArea] constraint, flags included
    ///
    /// #### Returns
    ///
    /// the matching layout
    public static Layout layoutFor(int constraint) {
        switch (constraint & 0xffff) {
            case TextArea.NUMERIC:
                return Layout.NUMBER;
            case TextArea.DECIMAL:
                return Layout.DECIMAL;
            case TextArea.PHONENUMBER:
                return Layout.PHONE;
            case TextArea.EMAILADDR:
                return Layout.EMAIL;
            case TextArea.URL:
                return Layout.URL;
            default:
                return Layout.TEXT;
        }
    }

    /// The field this keyboard types into.
    ///
    /// #### Returns
    ///
    /// the protected field
    public TextArea getField() {
        return field;
    }

    /// The current layout.
    ///
    /// #### Returns
    ///
    /// the layout in use
    public Layout getLayout() {
        return layout;
    }

    /// Overrides the layout chosen from the field's constraint.
    ///
    /// #### Parameters
    ///
    /// - `layout`: the layout to use; null restores the one the constraint picks
    public void setLayout(Layout layout) {
        this.layout = layout == null ? layoutFor(field.getConstraint()) : layout;
        symbols = false;
        invalidateKeys();
    }

    /// Whether the digit keys are shuffled each time the keyboard opens.
    ///
    /// #### Returns
    ///
    /// true when the digits are scrambled
    public boolean isScrambled() {
        return scrambled;
    }

    /// Shuffles the digit keys every time the keyboard opens, with [SecureRandom], so touch
    /// coordinates recorded by malware do not reveal which digits were typed. Off by default:
    /// it slows typing down, so keep it for PINs and one-time codes.
    ///
    /// #### Parameters
    ///
    /// - `scrambled`: true to shuffle the digits
    public void setScrambled(boolean scrambled) {
        this.scrambled = scrambled;
        digitOrder = scrambled ? shuffledDigits() : DIGITS;
        invalidateKeys();
    }

    /// Whether the keyboard is on screen.
    ///
    /// #### Returns
    ///
    /// true while it is showing
    public boolean isShowing() {
        // Our own state, not the dialog's: InteractionDialog.show defers when called from an
        // event, as a tap on the field is, so the dialog is briefly not showing yet.
        return dialog != null;
    }

    /// Shows the keyboard at the bottom of the field's form. Called by a tap on the field; call it
    /// yourself to open the keyboard when a screen appears.
    public void show() {
        if (isShowing()) {
            return;
        }
        final Form f = field.getComponentForm();
        if (f == null) {
            return;
        }
        if (scrambled) {
            digitOrder = shuffledDigits();
        }
        shift = false;
        symbols = false;
        invalidateKeys();
        final InteractionDialog dlg = new InteractionDialog(new BorderLayout());
        // Docked to the bottom of the form like the system keyboard it replaces, never a window.
        dlg.setNativeWindowMode(false);
        dlg.setUIID("Container");
        dlg.getContentPane().setUIID("Container");
        dlg.getTitleComponent().setHidden(true);
        dlg.add(BorderLayout.CENTER, this);
        dialog = dlg;
        int h = keyboardHeight(f.getWidth(), f.getHeight());
        try {
            f.setCurrentInputDevice(new VirtualInputDevice() { //NOPMD CloseResource - managed by Form#setCurrentInputDevice
                @Override
                public void close() {
                    if (dialog == dlg) { //NOPMD CompareObjectsWithEquals
                        hide();
                    }
                }
            });
        } catch (Exception ex) {
            // The previous input device would not give up control; show anyway, as Picker does.
            Log.e(ex);
        }
        dlg.show(Math.max(0, f.getHeight() - h), 0, 0, 0);
        f.scrollComponentToVisible(field);
    }

    /// Hides the keyboard and reports the edit as complete, as the platform editor does when it
    /// closes: the field's action listeners fire.
    public void hide() {
        InteractionDialog dlg = dialog;
        if (dlg == null) {
            return;
        }
        dialog = null;
        pressed = null;
        // Unconditionally: a show deferred by an event may not have landed yet, and dispose()
        // on a dialog that is not up only marks it disposed.
        dlg.dispose();
        Display.getInstance().onEditingComplete(field, field.getText());
    }

    private static String shuffledDigits() {
        StringBuilder sb = new StringBuilder(DIGITS);
        for (int i = sb.length() - 1; i > 0; i--) {
            int j = SecureRandom.intBelow(i + 1);
            char c = sb.charAt(i);
            sb.setCharAt(i, sb.charAt(j));
            sb.setCharAt(j, c);
        }
        return sb.toString();
    }

    private boolean isPad() {
        return layout == Layout.NUMBER || layout == Layout.DECIMAL || layout == Layout.PHONE;
    }

    /// Proportions of the system keyboards on current phones: about three quarters of the width in
    /// portrait for letters, less for a number pad, a little over half the height in landscape.
    int keyboardHeight(int width, int height) {
        int h;
        if (height >= width) {
            h = Math.round(width * (isPad() ? 0.6f : 0.72f));
        } else {
            h = Math.round(height * 0.55f);
        }
        if (Display.getInstance().isTablet()) {
            h = h * 6 / 10;
        }
        return Math.min(h, height * 2 / 3);
    }

    @Override
    protected Dimension calcPreferredSize() {
        Display d = Display.getInstance();
        return new Dimension(d.getDisplayWidth(), keyboardHeight(d.getDisplayWidth(), d.getDisplayHeight()));
    }

    private void invalidateKeys() {
        laidOutWidth = -1;
        pressed = null;
        repaint();
    }

    void layoutKeys(int width, int height) {
        if (width == laidOutWidth && height == laidOutHeight) {
            return;
        }
        laidOutWidth = width;
        laidOutHeight = height;
        keys.clear();
        List<List<Key>> rows = rows();
        int pad = Math.max(2, width / 130);
        int rowH = (height - pad) / rows.size();
        for (int r = 0; r < rows.size(); r++) {
            List<Key> row = rows.get(r);
            float total = 0;
            for (Key k : row) {
                total += k.weight;
            }
            float units = isPad() ? total : Math.max(total, 10f);
            float unit = (width - pad) / units;
            float x = (width - total * unit) / 2f + pad / 2f;
            int y = r * rowH + pad / 2;
            for (Key k : row) {
                k.x = Math.round(x + pad / 2f);
                k.width = Math.max(1, Math.round(k.weight * unit) - pad);
                k.y = y + pad / 2;
                k.height = Math.max(1, rowH - pad);
                x += k.weight * unit;
                keys.add(k);
            }
        }
    }

    private List<List<Key>> rows() {
        List<List<Key>> rows = new ArrayList<List<Key>>();
        String d = digitOrder;
        if (isPad()) {
            rows.add(chars(d.substring(0, 3)));
            rows.add(chars(d.substring(3, 6)));
            rows.add(chars(d.substring(6, 9)));
            List<Key> last = new ArrayList<Key>();
            if (layout == Layout.DECIMAL) {
                last.add(new Key(".", '\0', KIND_CHAR, 1));
            } else if (layout == Layout.PHONE) {
                last.add(new Key("+", '\0', KIND_CHAR, 1));
            } else {
                last.add(new Key(null, FontImage.MATERIAL_KEYBOARD_HIDE, KIND_RETURN, 1));
            }
            last.add(new Key(d.substring(9), '\0', KIND_CHAR, 1));
            last.add(new Key(null, FontImage.MATERIAL_BACKSPACE, KIND_BACKSPACE, 1));
            rows.add(last);
            if (layout != Layout.NUMBER) {
                List<Key> action = new ArrayList<Key>();
                action.add(new Key(null, FontImage.MATERIAL_KEYBOARD_HIDE, KIND_RETURN, 1));
                rows.add(action);
            }
            return rows;
        }
        if (symbols) {
            rows.add(chars(d));
            rows.add(chars("-/:;()$&@\""));
            List<Key> third = new ArrayList<Key>();
            third.add(new Key(null, '\0', KIND_BLANK, 1.5f));
            third.addAll(chars(".,?!'#*_"));
            third.add(new Key(null, FontImage.MATERIAL_BACKSPACE, KIND_BACKSPACE, 1.5f));
            rows.add(third);
        } else {
            rows.add(letters("qwertyuiop"));
            rows.add(letters("asdfghjkl"));
            List<Key> third = new ArrayList<Key>();
            third.add(new Key(null, FontImage.MATERIAL_KEYBOARD_CAPSLOCK, KIND_SHIFT, 1.5f));
            third.addAll(letters("zxcvbnm"));
            third.add(new Key(null, FontImage.MATERIAL_BACKSPACE, KIND_BACKSPACE, 1.5f));
            rows.add(third);
        }
        List<Key> bottom = new ArrayList<Key>();
        bottom.add(new Key(symbols ? "ABC" : "?123", '\0', KIND_MODE, 2f));
        if (layout == Layout.EMAIL) {
            bottom.add(new Key("@", '\0', KIND_CHAR, 1));
        } else if (layout == Layout.URL) {
            bottom.add(new Key("/", '\0', KIND_CHAR, 1));
        }
        bottom.add(new Key(" ", '\0', KIND_SPACE, layout == Layout.TEXT ? 5f : 4f));
        bottom.add(new Key(".", '\0', KIND_CHAR, 1));
        bottom.add(new Key(null, FontImage.MATERIAL_KEYBOARD_HIDE, KIND_RETURN, 2f));
        rows.add(bottom);
        return rows;
    }

    private List<Key> letters(String s) {
        List<Key> out = new ArrayList<Key>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (shift) {
                // ASCII fold by hand: toUpperCase is locale sensitive (Turkish dotless i).
                c = (char) (c - 'a' + 'A');
            }
            out.add(new Key(String.valueOf(c), '\0', KIND_CHAR, 1));
        }
        return out;
    }

    private static List<Key> chars(String s) {
        List<Key> out = new ArrayList<Key>();
        for (int i = 0; i < s.length(); i++) {
            out.add(new Key(String.valueOf(s.charAt(i)), '\0', KIND_CHAR, 1));
        }
        return out;
    }

    private Key keyAt(int px, int py) {
        for (Key k : keys) {
            if (k.contains(px, py)) {
                return k;
            }
        }
        return null;
    }

    @Override
    public void pointerPressed(int x, int y) {
        layoutKeys(getWidth(), getHeight());
        pressed = keyAt(x - getAbsoluteX(), y - getAbsoluteY());
        repaint();
    }

    @Override
    public void pointerDragged(int x, int y) {
        Key k = keyAt(x - getAbsoluteX(), y - getAbsoluteY());
        if (k != pressed) { //NOPMD CompareObjectsWithEquals
            pressed = k;
            repaint();
        }
    }

    @Override
    public void pointerReleased(int x, int y) {
        Key k = keyAt(x - getAbsoluteX(), y - getAbsoluteY());
        Key p = pressed;
        pressed = null;
        repaint();
        if (k != null && k == p) { //NOPMD CompareObjectsWithEquals
            press(k);
        }
    }

    /// Applies a key to the field. Package private so tests can type without pointer coordinates.
    void press(Key k) {
        switch (k.kind) {
            case KIND_CHAR:
            case KIND_SPACE:
                type(k.label);
                if (shift && k.kind == KIND_CHAR) {
                    shift = false;
                    invalidateKeys();
                }
                break;
            case KIND_BACKSPACE:
                String t = field.getText();
                if (t != null && t.length() > 0) {
                    field.setText(t.substring(0, t.length() - 1));
                }
                break;
            case KIND_SHIFT:
                shift = !shift;
                invalidateKeys();
                break;
            case KIND_MODE:
                symbols = !symbols;
                invalidateKeys();
                break;
            case KIND_RETURN:
                hide();
                field.fireDoneEvent();
                break;
            default:
                break;
        }
    }

    /// Types one key's text, as the key with that label would. Package private for tests.
    boolean typeKey(String label) {
        layoutKeys(Math.max(getWidth(), 320), Math.max(getHeight(), 240));
        for (Key k : keys) {
            if (k.label != null && k.label.equals(label)) {
                press(k);
                return true;
            }
        }
        return false;
    }

    /// The labels of the keys in the current layout, in row order. Package private for tests.
    String keyLabels() {
        layoutKeys(Math.max(getWidth(), 320), Math.max(getHeight(), 240));
        StringBuilder sb = new StringBuilder();
        for (Key k : keys) {
            if (k.label != null) {
                sb.append(k.label);
            }
        }
        return sb.toString();
    }

    private void type(String s) {
        String t = field.getText();
        if (t == null) {
            t = "";
        }
        if (t.length() + s.length() > field.getMaxSize()) {
            return;
        }
        field.setText(t + s);
    }

    private void updateFonts(int keyHeight) {
        if (keyHeight == fontForHeight) {
            return;
        }
        fontForHeight = keyHeight;
        int px = Math.max(8, keyHeight * 45 / 100);
        if (Font.isTrueTypeFileSupported()) {
            textFont = Font.createTrueTypeFont("native:MainRegular").derive(px, Font.STYLE_PLAIN);
        } else {
            textFont = Font.getDefaultFont();
        }
        Font material = FontImage.getMaterialDesignFont();
        iconFont = material == null ? textFont : material.derive(px * 1.2f, Font.STYLE_PLAIN);
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        layoutKeys(w, h);
        boolean dark = Boolean.TRUE.equals(CN.isDarkMode());
        int keyBg = dark ? 0x3c3c3c : 0xffffff;
        int specialBg = dark ? 0x2e2e2e : 0xd6dbe6;
        int pressedBg = dark ? 0x5a5a5a : 0xb8bfcc;
        int actionBg = 0x0b57d0;
        int fg = dark ? 0xffffff : 0x000000;
        if (getStyle().getBgTransparency() == 0) {
            g.setColor(dark ? 0x1f1f1f : 0xeef0f6);
            g.fillRect(getX(), getY(), w, h);
        }
        int keyH = keys.isEmpty() ? 20 : keys.get(0).height;
        updateFonts(keyH);
        int arc = Math.max(4, keyH / 5);
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        for (Key k : keys) {
            if (k.kind == KIND_BLANK) {
                continue;
            }
            boolean special = k.kind != KIND_CHAR && k.kind != KIND_SPACE;
            int c;
            if (k == pressed) { //NOPMD CompareObjectsWithEquals
                c = pressedBg;
            } else if (k.kind == KIND_RETURN) {
                c = actionBg;
            } else if (k.kind == KIND_SHIFT && shift) {
                c = keyBg;
            } else {
                c = special ? specialBg : keyBg;
            }
            int kx = getX() + k.x;
            int ky = getY() + k.y;
            g.setColor(c);
            g.fillRoundRect(kx, ky, k.width, k.height, arc, arc);
            g.setColor(k.kind == KIND_RETURN && k != pressed ? 0xffffff : fg); //NOPMD CompareObjectsWithEquals
            String s;
            if (k.icon != '\0') {
                g.setFont(iconFont);
                s = String.valueOf(k.icon);
            } else {
                g.setFont(textFont);
                s = k.label;
            }
            Font f = g.getFont();
            g.drawString(s, kx + (k.width - f.stringWidth(s)) / 2, ky + (k.height - f.getHeight()) / 2);
        }
        g.setAntiAliased(aa);
    }
}
