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
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
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
/// and letters otherwise. It draws its own keys in the style of the platform keyboards, light or
/// dark following the theme's dark mode, under a strip with a lock icon that tells the user this is
/// not their usual keyboard.
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
    private boolean landed;
    private Font textFont;
    private Font iconFont;
    private int fontForHeight = -1;
    private Font headerFont;
    private Font modeFont;
    private int laidOutHeader;
    private int bottomInset;
    private boolean showDeferred;
    private boolean pressAccepted;
    private Form hostForm;
    private Form slideForm;
    private int fontForHeader = -1;
    /// Rotation or a window resize: every size show() docked by is stale, so dock again.
    private final com.codename1.ui.events.ActionListener sizeListener = new com.codename1.ui.events.ActionListener() {
        @Override
        public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
            if (dialog != null) {
                dismiss(false);
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        show();
                    }
                });
            }
        }
    };
    private Motion slide;

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
        if (field.isEditing()) {
            // Installed mid-edit: the platform editor would stay open on what is now a
            // protected field, so end that edit first.
            field.stopEditing();
        }
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
        // Not an edit the user finished, so no action event: apps submit PINs on it.
        kb.dismiss(false);
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

    /// Puts the digit keys in a fixed order, for documentation screenshots and tests that need the
    /// same picture every time. A fixed order is exactly what scrambling exists to avoid, so do not
    /// use this in place of [#setScrambled(boolean)]; it also turns scrambling off, since the next
    /// [#show()] would otherwise replace the order.
    ///
    /// #### Parameters
    ///
    /// - `order`: the ten digits, each exactly once, in the order the keys show them
    public void setDigitOrder(String order) {
        if (order == null || order.length() != 10) {
            throw new IllegalArgumentException("order must hold the ten digits");
        }
        for (int i = 0; i < 10; i++) {
            if (order.indexOf((char) ('0' + i)) < 0) {
                throw new IllegalArgumentException("order must hold each digit once: " + order);
            }
        }
        scrambled = false;
        digitOrder = order;
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
        InteractionDialog dlg = dialog;
        if (dlg == null) {
            return false;
        }
        if (landed && dlg.getParent() == null) {
            // Removed behind our back (the form's layered pane cleared or replaced) while an
            // animation defers the deinitialize below. Forget it, or the field could never open
            // its keyboard again.
            forget();
            return false;
        }
        return true;
    }

    /// Retries a show that came before layout. One retry was not enough: when the form's layout
    /// lands a serial call later than the show (a transition, a slower host), the single retry
    /// still saw a height of zero and the show was lost for good. It keeps trying while the field
    /// is still on that form, and gives up after a bounded number of serial calls so a form that
    /// is never laid out does not spin the EDT.
    private void deferShow(final Form f, final int attempt) {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                if (field.getComponentForm() != f) {
                    showDeferred = false;
                    return;
                }
                if (f.getContentPane().getHeight() > 0) {
                    showDeferred = false;
                    show();
                } else if (attempt < 50) {
                    deferShow(f, attempt + 1);
                } else {
                    showDeferred = false;
                }
            }
        });
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
        if (f.getContentPane().getHeight() == 0) {
            // Not laid out yet (show() from a show listener): every size below would be zero and
            // the keyboard would dock at the top. Try again once the form has a size.
            if (!showDeferred) {
                showDeferred = true;
                deferShow(f, 0);
            }
            return;
        }
        if (scrambled) {
            digitOrder = shuffledDigits();
        }
        shift = false;
        symbols = false;
        final InteractionDialog dlg = new InteractionDialog(new BorderLayout());
        // Docked to the bottom of the form like the system keyboard it replaces, never a window.
        dlg.setNativeWindowMode(false);
        // The dialog's own animation grows it out of the middle of the screen, which squashes
        // every key while it runs. The keyboard slides up from the bottom edge instead (animate()).
        dlg.setAnimateShow(false);
        dlg.setUIID("Container");
        dlg.getContentPane().setUIID("Container");
        dlg.getTitleComponent().setHidden(true);
        // Edge to edge, as a system keyboard is: no theme padding or border around the tray.
        flatten(dlg.getAllStyles());
        flatten(dlg.getContentPane().getAllStyles());
        flatten(getAllStyles());
        if (getParent() != null) {
            // Still inside a dialog that was removed behind our back.
            remove();
        }
        dlg.add(BorderLayout.CENTER, this);
        dialog = dlg;
        landed = false;
        Rectangle safe = f.getSafeArea();
        // Keys stay above a home indicator; the tray runs on underneath it, as system keyboards do.
        bottomInset = Math.max(0, f.getHeight() - (safe.getY() + safe.getHeight()));
        int h = keyboardHeight(f.getWidth(), f.getHeight());
        setPreferredH(h);
        // After the inset: the key rectangles depend on it as well as on the size.
        invalidateKeys();
        hostForm = f;
        f.addSizeChangedListener(sizeListener);
        try {
            f.setCurrentInputDevice(new VirtualInputDevice() { //NOPMD CloseResource - managed by Form#setCurrentInputDevice
                @Override
                public void close() {
                    if (dialog == dlg) { //NOPMD CompareObjectsWithEquals
                        // The form moved on; the user did not finish the edit.
                        dismiss(false);
                    }
                }
            });
        } catch (Exception ex) {
            // The previous input device would not give up control; show anyway, as Picker does.
            Log.e(ex);
        }
        // The dialog lives in the layered pane over the content pane, below the title bar, so the
        // margin is measured from the content pane's height and not the form's.
        dlg.show(Math.max(0, f.getContentPane().getHeight() - h), 0, 0, 0);
        f.scrollComponentToVisible(field);
    }

    /// Hides the keyboard and reports the edit as complete, as the platform editor does when it
    /// closes: the field's action listeners fire.
    public void hide() {
        dismiss(true);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        // On screen at last: from here a dialog with no parent was removed, not still pending.
        landed = true;
        Form f = getComponentForm();
        if (f != null && dialog != null) {
            int from = getPreferredH();
            slide = Motion.createEaseOutMotion(from, 0, 180);
            slide.start();
            slideForm = f;
            f.registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        super.deinitialize();
        // Removed behind our back. No action event: the user did not finish the edit.
        forget();
    }

    private void forget() {
        stopSlide();
        if (hostForm != null) {
            hostForm.removeSizeChangedListener(sizeListener);
            hostForm = null;
        }
        // Recomputed by the next show; a fixed height would outlive a rotation.
        setPreferredSize(null);
        pressAccepted = false;
        dialog = null;
        landed = false;
        pressed = null;
    }

    private void dismiss(boolean complete) {
        InteractionDialog dlg = dialog;
        if (dlg == null) {
            return;
        }
        forget();
        // Unconditionally: a show deferred by an event may not have landed yet, and dispose()
        // on a dialog that is not up only marks it disposed.
        dlg.dispose();
        if (complete) {
            Display.getInstance().onEditingComplete(field, field.getText());
        }
    }

    private static void flatten(com.codename1.ui.plaf.Style s) {
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        s.setBorder(null);
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

    /// Height in the physical proportions of the platform keyboards: rows about 10mm apart under
    /// a slim strip, whatever the density or the tablet flag. Never more than about half the screen,
    /// so the field stays visible in landscape.
    int keyboardHeight(int width, int height) {
        Display d = Display.getInstance();
        int rows = rows().size();
        int rowMm = isPad() ? 95 : 105;
        int h = d.convertToPixels(rows * rowMm / 10f) + d.convertToPixels(rowMm / 20f);
        int cap = height >= width ? height * 55 / 100 : height / 2;
        return Math.max(1, Math.min(h, cap)) + bottomInset;
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

    /// Height of the strip above the keys that shows the lock.
    private static int headerHeight(int height, int rowCount) {
        return height / (rowCount * 2 + 1);
    }

    void layoutKeys(int width, int height) {
        if (width == laidOutWidth && height == laidOutHeight) {
            return;
        }
        laidOutWidth = width;
        laidOutHeight = height;
        height = Math.max(1, height - bottomInset);
        keys.clear();
        List<List<Key>> rows = rows();
        int header = headerHeight(height, rows.size());
        laidOutHeader = header;
        // Gaps in the proportions of the platform keyboards: narrow between keys, wider between
        // rows, so each key reads as its own cap.
        int hgap = Math.max(2, width / 68);
        int rowH = (height - header - hgap) / rows.size();
        int vgap = Math.max(2, rowH / 6);
        for (int r = 0; r < rows.size(); r++) {
            List<Key> row = rows.get(r);
            float total = 0;
            for (Key k : row) {
                total += k.weight;
            }
            float units = isPad() ? total : Math.max(total, 10f);
            float unit = (width - hgap) / units;
            float x = (width - total * unit) / 2f + hgap / 2f;
            int y = header + r * rowH;
            for (Key k : row) {
                k.x = Math.round(x + hgap / 2f);
                k.width = Math.max(1, Math.round(k.weight * unit) - hgap);
                k.y = y + vgap / 2;
                k.height = Math.max(1, rowH - vgap);
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
                last.add(new Key(null, FontImage.MATERIAL_CHECK, KIND_RETURN, 1));
            }
            last.add(new Key(d.substring(9), '\0', KIND_CHAR, 1));
            last.add(new Key(null, FontImage.MATERIAL_BACKSPACE, KIND_BACKSPACE, 1));
            rows.add(last);
            if (layout != Layout.NUMBER) {
                List<Key> action = new ArrayList<Key>();
                action.add(new Key(null, FontImage.MATERIAL_CHECK, KIND_RETURN, 1));
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
        bottom.add(new Key(null, FontImage.MATERIAL_CHECK, KIND_RETURN, 2f));
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
        pressAccepted = slide == null;
        if (!pressAccepted) {
            // Keys are still moving under the finger.
            return;
        }
        layoutKeys(getWidth(), getHeight());
        pressed = keyAt(x - getAbsoluteX(), y - getAbsoluteY());
        repaint();
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!pressAccepted) {
            // A press that began during the slide must not pick a key the finger never saw.
            return;
        }
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
        if (!pressAccepted) {
            return;
        }
        pressAccepted = false;
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

    private void stopSlide() {
        if (slide != null) {
            slide = null;
            // The form it was registered with, not getComponentForm(): on removal the parent
            // chain may already be gone, which would leave this in the form's animation list.
            if (slideForm != null) {
                slideForm.deregisterAnimated(this);
                slideForm = null;
            }
        }
    }

    @Override
    public boolean animate() {
        if (slide == null) {
            return false;
        }
        if (slide.isFinished()) {
            stopSlide();
        }
        return true;
    }

    private void updateFonts(int keyHeight, int header) {
        if (keyHeight == fontForHeight && header == fontForHeader) {
            return;
        }
        fontForHeight = keyHeight;
        fontForHeader = header;
        int px = Math.max(8, keyHeight * 48 / 100);
        Font base = Font.isTrueTypeFileSupported() ? Font.createTrueTypeFont("native:MainRegular") : null;
        textFont = base == null ? Font.getDefaultFont() : base.derive(px, Font.STYLE_PLAIN);
        Font material = FontImage.getMaterialDesignFont();
        modeFont = base == null ? textFont : base.derive(px * 0.72f, Font.STYLE_PLAIN);
        iconFont = material == null ? textFont : material.derive(px * 1.1f, Font.STYLE_PLAIN);
        headerFont = material == null ? textFont : material.derive(Math.max(8, header * 55 / 100), Font.STYLE_PLAIN);
    }

    /// The palette, modelled on the platform keyboards: white caps on a grey tray in light mode,
    /// grey caps on near black in dark mode, darker caps for the function keys.
    private static final int[] LIGHT = {0xe4e6eb, 0xffffff, 0xc5cad3, 0xa9aeb8, 0x9aa0a8, 0x1f1f1f, 0x5f6368};
    private static final int[] DARK = {0x1b1c1e, 0x46474b, 0x2f3033, 0x5e6066, 0x0e0e0f, 0xf1f1f1, 0x9aa0a6};
    private static final int TRAY = 0;
    private static final int CAP = 1;
    private static final int FUNCTION = 2;
    private static final int PRESSED = 3;
    private static final int SHADOW = 4;
    private static final int LABEL = 5;
    private static final int HINT = 6;
    private static final int ACTION = 0x0b57d0;

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        layoutKeys(w, h);
        int[] p = Boolean.TRUE.equals(CN.isDarkMode()) ? DARK : LIGHT;
        int oy = slide == null ? 0 : Math.max(0, Math.min(h, slide.getValue()));
        int x0 = getX();
        int y0 = getY() + oy;
        g.setColor(p[TRAY]);
        g.fillRect(x0, y0, w, h - oy);
        int keyH = keys.isEmpty() ? 20 : keys.get(0).height;
        int header = laidOutHeader;
        updateFonts(keyH, header);
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        if (header > 0) {
            g.setFont(headerFont);
            g.setColor(p[HINT]);
            String lock = String.valueOf(FontImage.MATERIAL_LOCK);
            g.drawString(lock, x0 + (w - headerFont.stringWidth(lock)) / 2,
                    y0 + (header - headerFont.getHeight()) / 2 + header / 8);
        }
        int arc = Math.max(4, keyH / 4);
        int drop = Math.max(1, keyH / 40);
        for (Key k : keys) {
            if (k.kind == KIND_BLANK) {
                continue;
            }
            boolean function = k.kind != KIND_CHAR && k.kind != KIND_SPACE;
            boolean down = k == pressed; //NOPMD CompareObjectsWithEquals
            int c;
            if (down) {
                c = p[PRESSED];
            } else if (k.kind == KIND_RETURN) {
                c = ACTION;
            } else if (k.kind == KIND_SHIFT && shift) {
                c = p[CAP];
            } else {
                c = function ? p[FUNCTION] : p[CAP];
            }
            int kx = x0 + k.x;
            int ky = y0 + k.y;
            g.setColor(p[SHADOW]);
            g.fillRoundRect(kx, ky + drop, k.width, k.height, arc, arc);
            g.setColor(c);
            g.fillRoundRect(kx, ky, k.width, k.height, arc, arc);
            g.setColor(k.kind == KIND_RETURN && !down ? 0xffffff : p[LABEL]);
            String s;
            Font f;
            if (k.icon != '\0') {
                f = iconFont;
                s = String.valueOf(k.icon);
            } else {
                f = k.kind == KIND_MODE ? modeFont : textFont;
                s = k.label;
            }
            g.setFont(f);
            g.drawString(s, kx + (k.width - f.stringWidth(s)) / 2, ky + (k.height - f.getHeight()) / 2);
        }
        g.setAntiAliased(aa);
    }
}
