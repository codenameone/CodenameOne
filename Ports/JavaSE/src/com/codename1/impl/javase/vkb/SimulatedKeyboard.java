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
package com.codename1.impl.javase.vkb;

import com.codename1.ui.TextArea;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/// The on-screen keyboard the simulator draws while a text field is being edited.
///
/// It is a model and a painter, nothing more: [KeyboardController] decides when it
/// is shown and what the application sees while it is, and the port routes presses
/// on it here through [#keyAt(int, int)]. Coordinates are relative to the
/// keyboard's own top left corner, in screen pixels.
///
/// Keys are laid out on the EDT and painted on the AWT thread and by the recorder,
/// so every method that reads or rebuilds them is synchronized.
///
/// The layouts follow what each platform shows for a field's constraint: letters
/// for free text, a number pad for numeric and phone input, `@` and `.` beside the
/// space bar for an email address. iOS number pads have no return key, so the
/// simulator shows the accessory bar with Done above them, as the iOS port does.
public final class SimulatedKeyboard {

    /// Which platform's keyboard this is.
    public enum Platform { IOS, ANDROID }

    /// The key set, chosen from the edited field's constraint.
    public enum Layout { TEXT, NUMBER, DECIMAL, PHONE, EMAIL, URL }

    /// What a key does when it is released.
    public enum KeyKind { CHAR, SHIFT, BACKSPACE, RETURN, SPACE, MODE, DONE }

    /// One key, positioned in keyboard coordinates by the last layout pass.
    public static final class Key {
        final String label;
        final String output;
        final KeyKind kind;
        final float weight;
        int x;
        int y;
        int width;
        int height;

        Key(String label, String output, KeyKind kind, float weight) {
            this.label = label;
            this.output = output;
            this.kind = kind;
            this.weight = weight;
        }

        public String getLabel() {
            return label;
        }

        public String getOutput() {
            return output;
        }

        public KeyKind getKind() {
            return kind;
        }

        boolean contains(int px, int py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    private Platform platform = Platform.IOS;
    private Layout layout = Layout.TEXT;
    private boolean symbols;
    private boolean shift;
    private boolean tablet;
    private boolean dark;
    private String returnLabel;
    private Key pressed;
    private int laidOutWidth = -1;
    private int laidOutHeight = -1;
    private final List<Key> keys = new ArrayList<Key>();

    public synchronized Platform getPlatform() {
        return platform;
    }

    public synchronized void setPlatform(Platform platform) {
        if (this.platform != platform) {
            this.platform = platform;
            invalidate();
        }
    }

    public synchronized Layout getLayout() {
        return layout;
    }

    public synchronized void setLayout(Layout layout) {
        if (this.layout != layout) {
            this.layout = layout;
            symbols = false;
            invalidate();
        }
    }

    public synchronized void setTablet(boolean tablet) {
        if (this.tablet != tablet) {
            this.tablet = tablet;
            invalidate();
        }
    }

    public synchronized void setDark(boolean dark) {
        this.dark = dark;
    }

    /// The label of the return key: "Next" while another field follows, "Done" for
    /// the last one, as both ports label it. Null means the platform's default.
    public synchronized void setReturnLabel(String returnLabel) {
        this.returnLabel = returnLabel;
        invalidate();
    }

    public synchronized String getReturnLabel() {
        if (returnLabel != null) {
            return returnLabel;
        }
        return platform == Platform.IOS ? "return" : "\u21b5";
    }

    public synchronized boolean isShift() {
        return shift;
    }

    public synchronized void setShift(boolean shift) {
        if (this.shift != shift) {
            this.shift = shift;
            invalidate();
        }
    }

    public synchronized boolean isSymbols() {
        return symbols;
    }

    public synchronized void setSymbols(boolean symbols) {
        if (this.symbols != symbols) {
            this.symbols = symbols;
            invalidate();
        }
    }

    /// Picks the layout a field with this [TextArea] constraint gets.
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

    /// True when the layout carries the accessory bar with Done above the keys.
    public synchronized boolean hasAccessoryBar() {
        return platform == Platform.IOS && isPad();
    }

    private boolean isPad() {
        return layout == Layout.NUMBER || layout == Layout.DECIMAL || layout == Layout.PHONE;
    }

    /// The keyboard's height on a screen of this size, accessory bar included.
    /// Proportions are those of the system keyboards on current phones: about three
    /// quarters of the width in portrait, a little over half the height in
    /// landscape, and much less of a tablet.
    public synchronized int preferredHeight(int screenWidth, int screenHeight) {
        boolean portrait = screenHeight >= screenWidth;
        float ratio;
        int h;
        if (portrait) {
            if (tablet) {
                ratio = platform == Platform.IOS ? 0.42f : 0.40f;
            } else {
                ratio = platform == Platform.IOS ? 0.745f : 0.72f;
            }
            h = Math.round(screenWidth * ratio);
        } else {
            h = Math.round(screenHeight * (tablet ? 0.42f : 0.55f));
        }
        if (hasAccessoryBar()) {
            h += accessoryBarHeight(screenWidth, screenHeight);
        }
        return Math.min(h, screenHeight * 2 / 3);
    }

    static int accessoryBarHeight(int screenWidth, int screenHeight) {
        return Math.max(24, Math.round(Math.min(screenWidth, screenHeight) * 0.115f));
    }

    private void invalidate() {
        laidOutWidth = -1;
        pressed = null;
    }

    /// The key under a point in keyboard coordinates, or null.
    public synchronized Key keyAt(int px, int py) {
        for (Key k : keys) {
            if (k.contains(px, py)) {
                return k;
            }
        }
        return null;
    }

    /// The key whose output, or whose kind for the special keys, matches. Used to
    /// animate a key while text arrives from the physical keyboard or an agent.
    public synchronized Key findKey(String output) {
        if (output == null) {
            return null;
        }
        for (Key k : keys) {
            if (k.kind == KeyKind.CHAR && output.equalsIgnoreCase(k.output)) {
                return k;
            }
        }
        if (" ".equals(output)) {
            return findKind(KeyKind.SPACE);
        }
        return null;
    }

    public synchronized Key findKind(KeyKind kind) {
        for (Key k : keys) {
            if (k.kind == kind) {
                return k;
            }
        }
        return null;
    }

    public synchronized void setPressed(Key key) {
        pressed = key;
    }

    public synchronized Key getPressed() {
        return pressed;
    }

    /// Positions every key for a keyboard of this size. Called by [#paint] and
    /// before hit testing, and cheap when the size has not changed.
    public synchronized void layout(int width, int height) {
        if (width == laidOutWidth && height == laidOutHeight) {
            return;
        }
        laidOutWidth = width;
        laidOutHeight = height;
        keys.clear();
        List<List<Key>> rows = rows();
        int top = 0;
        if (hasAccessoryBar()) {
            int bar = Math.min(height / 4, accessoryBarHeight(width, Math.max(width, height * 2)));
            Key done = new Key("Done", null, KeyKind.DONE, 1);
            done.width = width / 4;
            done.x = width - done.width;
            done.y = 0;
            done.height = bar;
            keys.add(done);
            top = bar;
        }
        int bottomGap = Math.round(height * (platform == Platform.IOS ? 0.12f : 0.04f));
        int areaH = height - top - bottomGap;
        int pad = Math.max(2, width / 130);
        int rowH = areaH / rows.size();
        for (int r = 0; r < rows.size(); r++) {
            List<Key> row = rows.get(r);
            float total = 0;
            for (Key k : row) {
                total += k.weight;
            }
            float unitsInRow = isPad() ? total : Math.max(total, 10f);
            float unit = (width - pad) / unitsInRow;
            float x = (width - total * unit) / 2f + pad / 2f;
            int y = top + r * rowH;
            for (Key k : row) {
                k.x = Math.round(x + pad / 2f);
                k.width = Math.max(1, Math.round(k.weight * unit) - pad);
                k.y = y + pad;
                k.height = rowH - pad * 2;
                x += k.weight * unit;
                keys.add(k);
            }
        }
    }

    private List<List<Key>> rows() {
        List<List<Key>> rows = new ArrayList<List<Key>>();
        if (isPad()) {
            rows.add(chars("1", "2", "3"));
            rows.add(chars("4", "5", "6"));
            rows.add(chars("7", "8", "9"));
            List<Key> last = new ArrayList<Key>();
            if (layout == Layout.DECIMAL) {
                last.add(new Key(".", ".", KeyKind.CHAR, 1));
            } else if (layout == Layout.PHONE) {
                last.add(new Key("+*#", "+", KeyKind.CHAR, 1));
            } else {
                last.add(new Key("", null, KeyKind.MODE, 1));
            }
            last.add(new Key("0", "0", KeyKind.CHAR, 1));
            last.add(new Key("\u232b", null, KeyKind.BACKSPACE, 1));
            rows.add(last);
            if (platform == Platform.ANDROID) {
                // Gboard's number pad carries its own action key in a column of
                // its own; folding it into the last row keeps the grid readable.
                last.set(0, new Key(getReturnLabel(), null, KeyKind.RETURN, 1));
            }
            return rows;
        }
        if (symbols) {
            rows.add(chars("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"));
            rows.add(chars("-", "/", ":", ";", "(", ")", "$", "&", "@", "\""));
            List<Key> third = new ArrayList<Key>();
            third.add(new Key("#+=", null, KeyKind.MODE, 1.5f));
            third.addAll(chars(".", ",", "?", "!", "'"));
            third.add(new Key("\u232b", null, KeyKind.BACKSPACE, 1.5f));
            rows.add(third);
        } else {
            rows.add(letters("qwertyuiop"));
            rows.add(letters("asdfghjkl"));
            List<Key> third = new ArrayList<Key>();
            third.add(new Key("\u21e7", null, KeyKind.SHIFT, 1.5f));
            third.addAll(letters("zxcvbnm"));
            third.add(new Key("\u232b", null, KeyKind.BACKSPACE, 1.5f));
            rows.add(third);
        }
        List<Key> bottom = new ArrayList<Key>();
        bottom.add(new Key(symbols ? "ABC" : (platform == Platform.IOS ? "123" : "?123"), null, KeyKind.MODE, platform == Platform.IOS ? 2.5f : 1.5f));
        if (layout == Layout.EMAIL) {
            bottom.add(new Key("@", "@", KeyKind.CHAR, 1));
        } else if (layout == Layout.URL) {
            bottom.add(new Key("/", "/", KeyKind.CHAR, 1));
        } else if (platform == Platform.ANDROID) {
            bottom.add(new Key(",", ",", KeyKind.CHAR, 1));
        }
        bottom.add(new Key(platform == Platform.IOS ? "space" : "", " ", KeyKind.SPACE, layout == Layout.TEXT && platform == Platform.IOS ? 5f : 4f));
        if (layout == Layout.EMAIL || layout == Layout.URL || platform == Platform.ANDROID) {
            bottom.add(new Key(".", ".", KeyKind.CHAR, 1));
        }
        bottom.add(new Key(getReturnLabel(), null, KeyKind.RETURN, platform == Platform.IOS ? 2.5f : 1.5f));
        rows.add(bottom);
        return rows;
    }

    private List<Key> letters(String s) {
        List<Key> out = new ArrayList<Key>();
        for (int i = 0; i < s.length(); i++) {
            String c = String.valueOf(s.charAt(i));
            String shown = shift ? asciiUpper(c) : c;
            out.add(new Key(shown, shown, KeyKind.CHAR, 1));
        }
        return out;
    }

    private static List<Key> chars(String... labels) {
        List<Key> out = new ArrayList<Key>();
        for (String l : labels) {
            out.add(new Key(l, l, KeyKind.CHAR, 1));
        }
        return out;
    }

    private static String asciiUpper(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'a' && c[i] <= 'z') {
                c[i] = (char) (c[i] - 'a' + 'A');
            }
        }
        return new String(c);
    }

    /// Paints the keyboard into a rectangle of this size whose top left is the
    /// graphics origin.
    public synchronized void paint(Graphics2D g, int width, int height) {
        layout(width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean ios = platform == Platform.IOS;
        Color bg = ios ? (dark ? new Color(0x2b2b2d) : new Color(0xd1d3d9))
                : (dark ? new Color(0x1f1f1f) : new Color(0xeef0f6));
        Color keyBg = ios ? (dark ? new Color(0x6b6b6d) : Color.WHITE)
                : (dark ? new Color(0x3c3c3c) : Color.WHITE);
        Color specialBg = ios ? (dark ? new Color(0x464648) : new Color(0xabb0ba))
                : (dark ? new Color(0x2e2e2e) : new Color(0xd6dbe6));
        Color actionBg = ios ? new Color(0x0a84ff) : new Color(0x0b57d0);
        Color fg = dark ? Color.WHITE : Color.BLACK;
        g.setColor(bg);
        g.fillRect(0, 0, width, height);
        if (hasAccessoryBar()) {
            Key done = findKind(KeyKind.DONE);
            g.setColor(dark ? new Color(0x3a3a3c) : new Color(0xf6f6f7));
            g.fillRect(0, 0, width, done.height);
        }
        int fontSize = Math.max(9, laidOutKeyHeight() * (isPad() ? 45 : 48) / 100);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
        Font smallFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, fontSize * 2 / 3));
        float arc = ios ? Math.max(4, laidOutKeyHeight() / 7f) : Math.max(4, laidOutKeyHeight() / 5f);
        for (Key k : keys) {
            boolean special = k.kind != KeyKind.CHAR && k.kind != KeyKind.SPACE;
            if (k.kind == KeyKind.DONE) {
                g.setFont(smallFont.deriveFont(Font.BOLD));
                g.setColor(ios ? new Color(0x0a84ff) : actionBg);
                drawCentered(g, k.label, k.x, k.y, k.width, k.height);
                continue;
            }
            Color c;
            if (k == pressed) {
                c = special ? keyBg : specialBg;
            } else if (k.kind == KeyKind.RETURN && returnLabel != null) {
                c = actionBg;
            } else if (k.kind == KeyKind.SHIFT && shift) {
                c = keyBg;
            } else {
                c = special ? specialBg : keyBg;
            }
            if (ios) {
                g.setColor(dark ? new Color(0x1c1c1e) : new Color(0x898a8d));
                g.fill(new RoundRectangle2D.Float(k.x, k.y + 1, k.width, k.height, arc, arc));
            }
            g.setColor(c);
            g.fill(new RoundRectangle2D.Float(k.x, k.y, k.width, k.height, arc, arc));
            boolean onAction = k.kind == KeyKind.RETURN && returnLabel != null && k != pressed;
            g.setColor(onAction ? Color.WHITE : fg);
            g.setFont(k.kind == KeyKind.CHAR && k.label.length() == 1 ? font : smallFont);
            drawCentered(g, k.label, k.x, k.y, k.width, k.height);
            if (k == pressed && ios && k.kind == KeyKind.CHAR && k.label.length() == 1) {
                paintIosPopup(g, k, keyBg, fg, font, arc);
            }
        }
    }

    private int laidOutKeyHeight() {
        for (Key k : keys) {
            if (k.kind != KeyKind.DONE) {
                return k.height;
            }
        }
        return 20;
    }

    /// iOS shows the pressed letter enlarged above the key; it is what makes typing
    /// readable in a recording.
    private static void paintIosPopup(Graphics2D g, Key k, Color keyBg, Color fg, Font font, float arc) {
        int w = Math.round(k.width * 1.5f);
        int h = Math.round(k.height * 1.15f);
        int x = k.x + k.width / 2 - w / 2;
        int y = k.y - h + k.height / 3;
        g.setColor(keyBg);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, arc * 1.5f, arc * 1.5f));
        g.setColor(fg);
        g.setFont(font.deriveFont(font.getSize2D() * 1.4f));
        drawCentered(g, k.label, x, y, w, h);
    }

    private static void drawCentered(Graphics2D g, String s, int x, int y, int w, int h) {
        if (s == null || s.length() == 0) {
            return;
        }
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(s);
        g.drawString(s, x + (w - sw) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }
}
