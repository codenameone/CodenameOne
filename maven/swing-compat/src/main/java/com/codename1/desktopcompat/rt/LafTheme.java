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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.javax.swing.JList;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.table.JTableHeader;
import com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.Hashtable;

/// What a look and feel is on this layer: a choice between a light and a
/// dark palette.
///
/// The widgets are Codename One components drawn with the Codename One
/// theme, and the colors the Swing `UIManager` answers are read from that
/// theme. So a look and feel is not installed, it is *selected*: the
/// display is told which mode to draw in, and a theme that has a dark
/// variant of its own switches to it. A theme that has none -- its window
/// stays light when dark was asked for, or the reverse -- gets the
/// layer's own palette laid over it, for every widget family the layer
/// uses. Either way there is one theming path: after the selection the
/// theme is dark, and everything that reads it follows.
///
/// Open windows are restyled at once. A color the layer itself gave a
/// list, a table or a tree when it was made is exchanged for its
/// counterpart in the new palette; so is a color an application set that
/// is equal to one of those. Any other color an application set, or
/// read from the defaults and kept, stays as it is.
public final class LafTheme {

    /// The accent of the light palette, and of the dark one.
    private static final int ACCENT_LIGHT = 0x2675bf;
    private static final int ACCENT_DARK = 0x4b6eaf;

    /// Whether the layer's own palette was laid over the theme. Once it
    /// was, it is laid again on every selection: there is no taking theme
    /// properties back.
    private static boolean overlaid;
    /// The accent an application chose, or `null`.
    private static Color accent;
    private static boolean unknownLogged;

    private LafTheme() {
    }

    // ------------------------------------------------------------ state

    private static int luminance(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
    }

    /// Whether the theme is dark now: its window background is.
    public static boolean isDark() {
        return luminance(EventBridge.defaultBackground()) < 128;
    }

    /// Whether the platform asks for dark mode, whatever was selected.
    private static boolean platformDark() {
        Display d = Display.getInstance();
        d.setDarkMode(null);
        Boolean b = d.isDarkMode();
        return b != null && b.booleanValue();
    }

    /// The accent color: the application's, or the palette's.
    public static Color accent() {
        if (accent != null) {
            return accent;
        }
        return new Color(isDark() ? ACCENT_DARK : ACCENT_LIGHT);
    }

    /// Sets the accent color; `null` gives the palette's back. Open
    /// windows are not restyled by this alone.
    public static void setAccent(Color c) {
        accent = c;
    }

    /// The color of lines: borders, separators, the edges of fields.
    public static Color line() {
        return CellTheme.mix(EventBridge.defaultBackground(), EventBridge.defaultForeground(), isDark() ? 0.22f : 0.24f);
    }

    /// The color of text that is disabled.
    public static Color disabledText() {
        return CellTheme.mix(EventBridge.defaultForeground(), EventBridge.defaultBackground(), 0.5f);
    }

    /// The background of a push button.
    public static Color buttonBackground() {
        if (Display.isInitialized()) {
            Style s = UIManager.getInstance().getComponentStyle("Button");
            if (s != null && (s.getBgTransparency() & 0xff) == 255) {
                return new Color(s.getBgColor() & 0xffffff);
            }
        }
        return CellTheme.mix(CellTheme.fieldBackground(), EventBridge.defaultBackground(), isDark() ? 0.4f : 0f);
    }

    /// Says once that a look and feel is not one the layer knows.
    public static void unknown(String name) {
        if (!unknownLogged) {
            unknownLogged = true;
            com.codename1.io.Log.p("desktopcompat.laf: " + name
                    + " is not a look and feel this layer knows as light or dark; the palette is unchanged");
        }
    }

    /// Puts everything back the way it was before any look and feel was
    /// selected: no mode forced on the display, no palette over the theme,
    /// the theme itself emptied. For tests, which share one display.
    public static void reset() {
        accent = null;
        unknownLogged = false;
        if (!Display.isInitialized()) {
            overlaid = false;
            return;
        }
        Display.getInstance().setDarkMode(null);
        if (overlaid) {
            overlaid = false;
            UIManager.getInstance().setThemeProps(new Hashtable<String, Object>());
        }
    }

    // ------------------------------------------------------------ select

    /// The colors the layer hands to widgets when they are made, in a
    /// fixed order, as the theme has them now.
    private static Color[] roles() {
        return new Color[]{
            CellTheme.background(null), CellTheme.foreground(null), CellTheme.selectionBackground(null),
            CellTheme.selectionForeground(null), CellTheme.grid(null), CellTheme.headerBackground(null),
            CellTheme.headerForeground(null), EventBridge.defaultBackground(), EventBridge.defaultForeground(),
            CellTheme.fieldBackground()};
    }

    /// Selects the palette: dark, light, or with `null` whichever the
    /// platform is in. Open windows are restyled. Does nothing before
    /// there is a display.
    public static void select(Boolean dark) {
        if (!Display.isInitialized()) {
            return;
        }
        Color[] before = roles();
        boolean want = dark != null ? dark.booleanValue() : platformDark();
        Display.getInstance().setDarkMode(dark);
        UIManager m = UIManager.getInstance();
        m.refreshTheme();
        if (overlaid || isDark() != want) {
            m.addThemeProps(overlay(want));
            overlaid = true;
        }
        Color[] after = roles();
        Window[] all = Window.getWindows();
        for (int i = 0; i < all.length; i++) {
            exchange(all[i], before, after);
            restyle(all[i]);
        }
    }

    /// Restyles every open window from the theme as it is now.
    public static void restyleAll() {
        if (!Display.isInitialized()) {
            return;
        }
        Window[] all = Window.getWindows();
        for (int i = 0; i < all.length; i++) {
            restyle(all[i]);
        }
    }

    /// Restyles the window `c` is in, or `c` itself when it is in none:
    /// the Codename One widgets take their styles from the theme again,
    /// and everything is laid out and painted.
    public static void restyle(Component c) {
        if (c == null || !Display.isInitialized()) {
            return;
        }
        Component top = c;
        while (!(top instanceof Window) && top.getParent() != null) {
            top = top.getParent();
        }
        if (top instanceof Window) {
            Form f = ((Window) top).cn1Form();
            if (f != null) {
                f.refreshTheme(false);
                f.revalidate();
            }
        } else {
            com.codename1.ui.Component p = top.cn1PeerOrNull();
            if (p != null) {
                p.refreshTheme(false);
            }
        }
        top.invalidate();
        top.validate();
        top.repaint();
    }

    /// The roles a background may have, a foreground, and so on.
    private static final int[] BACK = {0, 5, 7, 9};
    private static final int[] FORE = {1, 6, 8};
    private static final int[] SEL_BACK = {2};
    private static final int[] SEL_FORE = {3};
    private static final int[] GRID = {4};

    private static Color swap(Color c, Color[] before, Color[] after, int[] roles) {
        if (c == null) {
            return null;
        }
        for (int i = 0; i < roles.length; i++) {
            int r = roles[i];
            if (before[r].equals(c)) {
                return after[r].equals(c) ? null : after[r];
            }
        }
        return null;
    }

    /// Exchanges the colors of the old palette for those of the new one
    /// in `c` and everything in it.
    private static void exchange(Component c, Color[] before, Color[] after) {
        if (c.isBackgroundSet()) {
            Color n = swap(c.getBackground(), before, after, BACK);
            if (n != null) {
                c.setBackground(n);
            }
        }
        if (c.isForegroundSet()) {
            Color n = swap(c.getForeground(), before, after, FORE);
            if (n != null) {
                c.setForeground(n);
            }
        }
        Object o = c;
        if (o instanceof JTable) {
            JTable t = (JTable) o;
            Color n = swap(t.getSelectionBackground(), before, after, SEL_BACK);
            if (n != null) {
                t.setSelectionBackground(n);
            }
            n = swap(t.getSelectionForeground(), before, after, SEL_FORE);
            if (n != null) {
                t.setSelectionForeground(n);
            }
            n = swap(t.getGridColor(), before, after, GRID);
            if (n != null) {
                t.setGridColor(n);
            }
            JTableHeader h = t.getTableHeader();
            if (h != null && h.getParent() == null) {
                exchange(h, before, after);
            }
        } else if (o instanceof JList) {
            JList<?> l = (JList<?>) o;
            Color n = swap(l.getSelectionBackground(), before, after, SEL_BACK);
            if (n != null) {
                l.setSelectionBackground(n);
            }
            n = swap(l.getSelectionForeground(), before, after, SEL_FORE);
            if (n != null) {
                l.setSelectionForeground(n);
            }
        } else if (o instanceof JTree) {
            TreeCellRenderer r = ((JTree) o).getCellRenderer();
            if (r instanceof DefaultTreeCellRenderer) {
                DefaultTreeCellRenderer d = (DefaultTreeCellRenderer) r;
                Color n = swap(d.getTextSelectionColor(), before, after, SEL_FORE);
                if (n != null) {
                    d.setTextSelectionColor(n);
                }
                n = swap(d.getTextNonSelectionColor(), before, after, FORE);
                if (n != null) {
                    d.setTextNonSelectionColor(n);
                }
                n = swap(d.getBackgroundSelectionColor(), before, after, SEL_BACK);
                if (n != null) {
                    d.setBackgroundSelectionColor(n);
                }
                n = swap(d.getBackgroundNonSelectionColor(), before, after, BACK);
                if (n != null) {
                    d.setBackgroundNonSelectionColor(n);
                }
                n = swap(d.getBorderSelectionColor(), before, after, SEL_BACK);
                if (n != null) {
                    d.setBorderSelectionColor(n);
                }
            }
        }
        if (o instanceof Container) {
            Component[] kids = ((Container) o).getComponents();
            for (int i = 0; i < kids.length; i++) {
                exchange(kids[i], before, after);
            }
        }
    }

    // ------------------------------------------------------------ overlay

    private static String hex(int rgb) {
        return Integer.toHexString(rgb & 0xffffff);
    }

    private static int mix(int a, int b, float t) {
        return CellTheme.mix(new Color(a), new Color(b), t).getRGB() & 0xffffff;
    }

    /// One widget style in every state: background, text and whether the
    /// background is filled.
    private static void ui(Hashtable<String, Object> h, String id, int bg, int fg, int disabled, boolean filled) {
        String[] states = {"", "sel#", "press#", "dis#"};
        for (int i = 0; i < states.length; i++) {
            String k = id + "." + states[i];
            h.put(k + "bgColor", hex(bg));
            h.put(k + "fgColor", hex(i == 3 ? disabled : fg));
            h.put(k + "transparency", filled ? "255" : "0");
        }
    }

    private static void edge(Hashtable<String, Object> h, String id, int line, int focus) {
        h.put(id + ".border", Border.createLineBorder(1, line));
        h.put(id + ".sel#border", Border.createLineBorder(1, focus));
        h.put(id + ".press#border", Border.createLineBorder(1, focus));
        h.put(id + ".dis#border", Border.createLineBorder(1, line));
    }

    /// The layer's palette as Codename One theme properties.
    private static Hashtable<String, Object> overlay(boolean dark) {
        int win = dark ? 0x3c3f41 : 0xf2f2f2;
        int field = dark ? 0x46494b : 0xffffff;
        int ink = dark ? 0xdddddd : 0x000000;
        int button = dark ? 0x4e5052 : 0xffffff;
        int sel = accent != null ? accent.getRGB() & 0xffffff : dark ? ACCENT_DARK : ACCENT_LIGHT;
        int selInk = 0xffffff;
        int off = dark ? 0x8c8c8c : 0x8c8c8c;
        int line = dark ? 0x616365 : 0xc4c4c4;
        int head = mix(field, ink, 0.08f);
        Hashtable<String, Object> h = new Hashtable<String, Object>();
        String[] window = {"Form", "ContentPane", "Dialog", "DialogBody", "DialogContentPane", "DialogTitle",
            "PopupDialog", "PopupContentPane", "Title", "TitleArea", "Toolbar", "MenuBar", "Menu", "TabsContainer",
            "TabbedPane", "SideNavigationPanel", "StatusBar", "CommandList", "Sheet"};
        for (int i = 0; i < window.length; i++) {
            ui(h, window[i], win, ink, off, true);
        }
        String[] clear = {"Label", "Container", "CheckBox", "RadioButton", "Command", "TitleCommand", "BackCommand",
            "DialogCommandArea", "Slider", "SpanLabel", "SpanLabelText", "MultiLine1", "MultiLine2", "Separator",
            "Scroll", "ScrollThumb", "HorizontalScroll", "HorizontalScrollThumb", "SpinnerRenderer", "Spinner",
            "Calendar", "CalendarTitle", "CalendarDay", "CalendarDate", "PickerButton", "Picker"};
        for (int i = 0; i < clear.length; i++) {
            ui(h, clear[i], win, ink, off, false);
        }
        String[] fields = {"TextField", "TextArea", "ComboBox", "ComboBoxPopup", "ComboBoxList", "List", "TextHint"};
        for (int i = 0; i < fields.length; i++) {
            ui(h, fields[i], field, ink, off, true);
            if (i < 3) {
                edge(h, fields[i], line, sel);
            }
        }
        h.put("TextHint.fgColor", hex(off));
        h.put("TextHint.transparency", "0");
        String[] buttons = {"Button", "RaisedButton", "DialogButton", "DialogCommand", "ToolTip", "Tooltip"};
        for (int i = 0; i < buttons.length; i++) {
            ui(h, buttons[i], button, ink, off, true);
            h.put(buttons[i] + ".press#bgColor", hex(mix(button, ink, 0.15f)));
            h.put(buttons[i] + ".border", Border.createLineBorder(1, line));
            h.put(buttons[i] + ".sel#border", Border.createLineBorder(1, sel));
            h.put(buttons[i] + ".press#border", Border.createLineBorder(1, sel));
            h.put(buttons[i] + ".dis#border", Border.createLineBorder(1, line));
        }
        String[] rows = {"TableCell", "ListRenderer", "ComboBoxItem", "MenuItem"};
        for (int i = 0; i < rows.length; i++) {
            ui(h, rows[i], field, ink, off, true);
            h.put(rows[i] + ".sel#bgColor", hex(sel));
            h.put(rows[i] + ".sel#fgColor", hex(selInk));
            h.put(rows[i] + ".press#bgColor", hex(sel));
            h.put(rows[i] + ".press#fgColor", hex(selInk));
        }
        ui(h, "ListRendererFocus", sel, selInk, off, true);
        ui(h, "ComboBoxFocus", sel, selInk, off, true);
        ui(h, "TableHeader", head, ink, off, true);
        ui(h, "Tab", win, ink, off, true);
        ui(h, "UnselectedTab", win, ink, off, true);
        ui(h, "SelectedTab", win, ink, off, true);
        h.put("Tab.press#fgColor", hex(dark ? 0xffffff : sel));
        h.put("Tab.sel#fgColor", hex(dark ? 0xffffff : sel));
        ui(h, "SliderFull", sel, selInk, off, true);
        h.put("Slider.bgColor", hex(mix(win, ink, 0.22f)));
        h.put("Scroll.fgColor", hex(mix(win, ink, 0.1f)));
        h.put("ScrollThumb.fgColor", hex(mix(win, ink, 0.4f)));
        h.put("ScrollThumb.bgColor", hex(mix(win, ink, 0.4f)));
        h.put("Separator.fgColor", hex(line));
        return h;
    }

    // ------------------------------------------------------------ defaults

    /// The font of a FlatLaf style class or typography key -- `h1`,
    /// `small`, `monospaced` and so on -- derived from the default font;
    /// `null` for a name that is none.
    public static Font styleFont(String name) {
        Font base = Fonts.defaultFont();
        float size = base.getSize2D();
        if ("h00".equals(name)) {
            return base.deriveFont(Font.BOLD, size + 24);
        } else if ("h0".equals(name)) {
            return base.deriveFont(Font.BOLD, size + 18);
        } else if ("h1".equals(name)) {
            return base.deriveFont(Font.BOLD, size + 12);
        } else if ("h2".equals(name)) {
            return base.deriveFont(Font.BOLD, size + 6);
        } else if ("h3".equals(name)) {
            return base.deriveFont(Font.BOLD, size + 3);
        } else if ("h4".equals(name) || "semibold".equals(name)) {
            return base.deriveFont(Font.BOLD, size);
        } else if ("h1.regular".equals(name)) {
            return base.deriveFont(Font.PLAIN, size + 12);
        } else if ("h2.regular".equals(name)) {
            return base.deriveFont(Font.PLAIN, size + 6);
        } else if ("h3.regular".equals(name)) {
            return base.deriveFont(Font.PLAIN, size + 3);
        } else if ("large".equals(name)) {
            return base.deriveFont(Font.PLAIN, size + 2);
        } else if ("medium".equals(name)) {
            return base.deriveFont(Font.PLAIN, Math.max(6, size - 1));
        } else if ("small".equals(name)) {
            return base.deriveFont(Font.PLAIN, Math.max(6, size - 2));
        } else if ("mini".equals(name)) {
            return base.deriveFont(Font.PLAIN, Math.max(6, size - 3));
        } else if ("light".equals(name) || "default".equals(name)) {
            return base;
        } else if ("monospaced".equals(name)) {
            return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
        }
        return null;
    }

    private static Color action(String name, boolean dark) {
        if ("Red".equals(name)) {
            return new Color(dark ? 0xc75450 : 0xdb5860);
        } else if ("Yellow".equals(name)) {
            return new Color(dark ? 0xf0a732 : 0xeda200);
        } else if ("Green".equals(name)) {
            return new Color(dark ? 0x499c54 : 0x59a869);
        } else if ("Blue".equals(name)) {
            return new Color(dark ? 0x3592c4 : 0x389fd6);
        } else if ("Grey".equals(name)) {
            return new Color(dark ? 0xafb1b3 : 0x6e6e6e);
        } else if ("GreyInline".equals(name)) {
            return new Color(0x7f8b91);
        } else if ("Purple".equals(name) || "Pink".equals(name)) {
            return new Color("Pink".equals(name) ? 0xf98b9e : 0xb99bf8);
        } else if ("BlackText".equals(name)) {
            return new Color(0x231f20);
        } else if ("RedStatus".equals(name)) {
            return new Color(0xe05555);
        } else if ("GreenAndroid".equals(name)) {
            return new Color(0xa4c639);
        } else if ("YellowDark".equals(name)) {
            return new Color(0xd9a343);
        }
        return null;
    }

    /// What the palette has for a key of the Swing defaults that the
    /// theme itself has no direct answer to: the keys of the desktop look
    /// and feels and of FlatLaf that applications read. `null` for a key
    /// that is not known.
    public static Object value(String k) {
        boolean dark = isDark();
        Color win = EventBridge.defaultBackground();
        Color ink = EventBridge.defaultForeground();
        if ("laf.dark".equals(k)) {
            return Boolean.valueOf(dark);
        }
        if (k.startsWith("Actions.")) {
            return action(k.substring(8), dark);
        }
        if (k.startsWith("Objects.")) {
            return action(k.substring(8), dark);
        }
        if ("Component.accentColor".equals(k) || "@accentColor".equals(k) || "Component.focusedBorderColor".equals(k)
                || "ProgressBar.foreground".equals(k) || "TabbedPane.underlineColor".equals(k)
                || "Button.default.background".equals(k) || "Slider.thumbColor".equals(k)
                || "Slider.trackValueColor".equals(k) || "CheckBox.icon.selectedBackground".equals(k)) {
            return accent();
        }
        if ("Component.focusColor".equals(k) || "Button.default.focusColor".equals(k)) {
            return CellTheme.mix(accent(), win, 0.45f);
        }
        if ("Component.linkColor".equals(k) || "Hyperlink.linkColor".equals(k)) {
            return accent != null ? accent : new Color(dark ? 0x589df6 : 0x2470b3);
        }
        if ("Button.default.foreground".equals(k)) {
            return Color.WHITE;
        }
        if (k.startsWith("Component.error.") || "Component.errorColor".equals(k)) {
            return new Color(dark ? 0x8b3c3c : 0xe53e4d);
        }
        if (k.startsWith("Component.warning.")) {
            return new Color(dark ? 0xac7920 : 0xe2a53a);
        }
        if (k.startsWith("Component.success.")) {
            return new Color(dark ? 0x499c54 : 0x59a869);
        }
        if ("Component.borderColor".equals(k) || "Separator.foreground".equals(k) || "controlShadow".equals(k)
                || k.endsWith(".borderColor") || k.endsWith(".separatorColor") || "SplitPane.dividerColor".equals(k)
                || "SplitPaneDivider.gripColor".equals(k) || "ToolBar.separatorColor".equals(k)
                || k.endsWith(".shadow") || "TabbedPane.contentAreaColor".equals(k)) {
            return line();
        }
        if ("controlDkShadow".equals(k) || k.endsWith(".darkShadow")) {
            return CellTheme.mix(win, ink, 0.45f);
        }
        if ("controlHighlight".equals(k) || "controlLtHighlight".equals(k) || k.endsWith(".highlight")) {
            return CellTheme.mix(win, dark ? ink : Color.WHITE, dark ? 0.12f : 0.8f);
        }
        if (k.endsWith(".disabledForeground") || k.endsWith(".inactiveForeground") || k.endsWith(".disabledText")
                || "textInactiveText".equals(k) || k.endsWith(".placeholderForeground")
                || k.endsWith(".disabledSelectedForeground") || "inactiveCaptionText".equals(k)) {
            return disabledText();
        }
        if (k.endsWith(".inactiveBackground") || k.endsWith(".disabledBackground") || "inactiveCaption".equals(k)) {
            return win;
        }
        if ("Button.background".equals(k) || "ToggleButton.background".equals(k) || "ComboBox.buttonBackground".equals(k)) {
            return buttonBackground();
        }
        if (k.endsWith(".hoverBackground") || k.endsWith(".selectionInactiveBackground")
                || k.endsWith(".alternateRowColor")) {
            return CellTheme.mix(k.startsWith("Button.") ? buttonBackground() : CellTheme.fieldBackground(), ink, 0.08f);
        }
        if (k.endsWith(".pressedBackground")) {
            return CellTheme.mix(k.startsWith("Button.") ? buttonBackground() : win, ink, 0.16f);
        }
        if (k.endsWith(".selectionInactiveForeground") || k.endsWith(".textForeground") || k.endsWith(".caretForeground")
                || k.endsWith(".caretColor") || "infoText".equals(k)) {
            return ink;
        }
        if ("info".equals(k) || "ToolTip.background".equals(k)) {
            return CellTheme.mix(win, ink, dark ? 0.12f : 0.03f);
        }
        if ("defaultFont".equals(k)) {
            return Fonts.defaultFont();
        }
        if (k.endsWith(".font")) {
            return styleFont(k.substring(0, k.length() - 5));
        }
        if (k.endsWith(".border")) {
            return border(k.substring(0, k.length() - 7));
        }
        if ("Button.margin".equals(k) || "ToggleButton.margin".equals(k)) {
            return new Insets(2, 14, 2, 14);
        }
        if ("TextField.margin".equals(k) || "TextArea.margin".equals(k) || "PasswordField.margin".equals(k)
                || "FormattedTextField.margin".equals(k) || "ComboBox.padding".equals(k) || "Table.cellMargins".equals(k)
                || "EditorPane.margin".equals(k) || "TextPane.margin".equals(k)) {
            return new Insets(2, 6, 2, 6);
        }
        if ("TabbedPane.tabInsets".equals(k)) {
            return new Insets(4, 12, 4, 12);
        }
        if ("MenuItem.margin".equals(k) || "Menu.margin".equals(k)) {
            return new Insets(3, 6, 3, 6);
        }
        if ("ToolBar.buttonMargins".equals(k)) {
            return new Insets(3, 3, 3, 3);
        }
        if ("CheckBox.margin".equals(k) || "RadioButton.margin".equals(k)) {
            return new Insets(2, 2, 2, 2);
        }
        if ("Component.arc".equals(k) || "CheckBox.arc".equals(k) || "ProgressBar.arc".equals(k)) {
            return Integer.valueOf(4);
        }
        if ("Button.arc".equals(k)) {
            return Integer.valueOf(6);
        }
        if ("TextComponent.arc".equals(k) || "Component.innerFocusWidth".equals(k)) {
            return Integer.valueOf(0);
        }
        if ("Component.focusWidth".equals(k)) {
            return Integer.valueOf(2);
        }
        if ("Component.borderWidth".equals(k)) {
            return Integer.valueOf(1);
        }
        if ("Table.rowHeight".equals(k) || "Tree.rowHeight".equals(k) || "List.rowHeight".equals(k)) {
            return Integer.valueOf(CellTheme.rowHeight(null, 20, 32));
        }
        if ("TabbedPane.tabHeight".equals(k)) {
            return Integer.valueOf(CellTheme.touch() ? 40 : 32);
        }
        if ("SplitPane.dividerSize".equals(k)) {
            return Integer.valueOf(CellTheme.touch() ? 12 : 5);
        }
        return null;
    }

    private static Object border(String owner) {
        if ("TextField".equals(owner) || "FormattedTextField".equals(owner) || "PasswordField".equals(owner)
                || "ComboBox".equals(owner) || "Spinner".equals(owner)) {
            return new LafBorder(2, 6, 2, 6);
        }
        if ("Button".equals(owner) || "ToggleButton".equals(owner)) {
            return new LafBorder(2, 14, 2, 14);
        }
        if ("ScrollPane".equals(owner) || "TitledBorder".equals(owner)
                || "PopupMenu".equals(owner) || "ToolTip".equals(owner) || "Table".equals(owner)
                || "TableHeader.cell".equals(owner)) {
            return new LafBorder(0, 0, 0, 0);
        }
        return null;
    }
}
