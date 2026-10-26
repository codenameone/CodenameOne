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
package com.codename1.desktopcompat.com.formdev.flatlaf;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.LookAndFeel;
import com.codename1.desktopcompat.javax.swing.UIDefaults;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.javax.swing.UnsupportedLookAndFeelException;
import com.codename1.desktopcompat.rt.LafTheme;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// The FlatLaf look and feel under its own name, as this layer has it: a
/// choice of palette.
///
/// The widgets are Codename One components drawn with the Codename One
/// theme, so nothing of FlatLaf's own drawing is here. Setting one of
/// its subclasses selects the light or the dark palette, as [#isDark]
/// says, and restyles every open window; the colors, fonts and metrics
/// applications read back from `UIManager` under FlatLaf's keys answer
/// from that palette. See
/// [UIManager][com.codename1.desktopcompat.javax.swing.UIManager].
///
/// What differs from the desktop:
///
///  - Extra defaults ([#setGlobalExtraDefaults], [#setExtraDefaults])
///    are read for plain values: a color as `#rgb`, `#rrggbb` or
///    `#rrggbbaa`, a whole number, `true` or `false`, insets as four
///    numbers. `@accentColor` sets the accent of the palette. A value in
///    FlatLaf's expression language (`lighten(...)`, `$Other.key`) is
///    skipped.
///  - A custom defaults source that is registered is recorded and not
///    read: there are no properties files of a look and feel on a device.
///  - There are no window decorations of the look and feel, no mnemonics
///    to show and no font families to prefer; those calls are recorded.
public abstract class FlatLaf extends LookAndFeel {

    private static Map<String, String> globalExtraDefaults;
    private static boolean useNativeWindowDecorations;
    private static String preferredFontFamily;
    private static String preferredLightFontFamily;
    private static String preferredSemiboldFontFamily;
    private static String preferredMonospacedFontFamily;
    private static boolean sourceLogged;

    private Map<String, String> extraDefaults;

    public FlatLaf() {
    }

    /// Sets the look and feel and answers whether that worked.
    public static boolean setup(LookAndFeel newLookAndFeel) {
        try {
            UIManager.setLookAndFeel(newLookAndFeel);
            return true;
        } catch (UnsupportedLookAndFeelException e) {
            com.codename1.io.Log.p("desktopcompat.laf: " + e.getMessage());
            return false;
        }
    }

    /// The same as [#setup], under its older name.
    public static boolean install(LookAndFeel newLookAndFeel) {
        return setup(newLookAndFeel);
    }

    /// Adds the look and feel to those `UIManager` lists as installed.
    public static void installLafInfo(String lafName, Class<? extends LookAndFeel> lafClass) {
        UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo(lafName, lafClass.getName()));
    }

    @Override
    public String getID() {
        return "FlatLaf - " + getName();
    }

    /// Whether this look and feel selects the dark palette.
    public abstract boolean isDark();

    /// Whether the look and feel that is set now is a dark FlatLaf.
    public static boolean isLafDark() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        return laf instanceof FlatLaf && ((FlatLaf) laf).isDark();
    }

    @Override
    public boolean getSupportsWindowDecorations() {
        return false;
    }

    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void uninitialize() {
    }

    /// The extra defaults, the global ones and then this look's own, as
    /// far as their values are plain; everything else of the palette is
    /// answered by `UIManager` from the theme.
    @Override
    public UIDefaults getDefaults() {
        UIDefaults d = new UIDefaults();
        LafTheme.setAccent(null);
        read(globalExtraDefaults, d);
        read(extraDefaults, d);
        return d;
    }

    private static void read(Map<String, String> from, UIDefaults into) {
        if (from == null) {
            return;
        }
        Iterator<Map.Entry<String, String>> it = from.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            String key = e.getKey();
            Object v = plain(e.getValue());
            if (key == null || v == null) {
                continue;
            }
            if ("@accentColor".equals(key)) {
                if (v instanceof Color) {
                    LafTheme.setAccent((Color) v);
                }
            } else if (!key.startsWith("@")) {
                into.put(key, v);
            }
        }
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static Color color(String s) {
        int n = s.length() - 1;
        if (n != 3 && n != 4 && n != 6 && n != 8) {
            return null;
        }
        int[] d = new int[8];
        boolean brief = n <= 4;
        int count = 0;
        for (int i = 1; i < s.length(); i++) {
            int h = hexDigit(s.charAt(i));
            if (h < 0) {
                return null;
            }
            d[count++] = h;
            if (brief) {
                d[count++] = h;
            }
        }
        int r = d[0] * 16 + d[1];
        int g = d[2] * 16 + d[3];
        int b = d[4] * 16 + d[5];
        return count == 8 ? new Color(r, g, b, d[6] * 16 + d[7]) : new Color(r, g, b);
    }

    private static Integer whole(String s) {
        if (s.length() == 0 || s.length() > 9) {
            return null;
        }
        int v = 0;
        int start = s.charAt(0) == '-' ? 1 : 0;
        if (start == s.length()) {
            return null;
        }
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
            v = v * 10 + (c - '0');
        }
        return Integer.valueOf(start == 1 ? -v : v);
    }

    /// A value written plainly, or `null` for anything else.
    private static Object plain(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.length() == 0) {
            return null;
        }
        if (s.charAt(0) == '#') {
            return color(s);
        }
        if ("true".equals(s)) {
            return Boolean.TRUE;
        } else if ("false".equals(s)) {
            return Boolean.FALSE;
        }
        Integer n = whole(s);
        if (n != null) {
            return n;
        }
        if (s.indexOf(',') > 0) {
            int[] parts = new int[4];
            int count = 0;
            int from = 0;
            while (from <= s.length()) {
                int comma = s.indexOf(',', from);
                int to = comma < 0 ? s.length() : comma;
                Integer part = whole(s.substring(from, to).trim());
                if (part == null || count == 4) {
                    return null;
                }
                parts[count++] = part.intValue();
                from = to + 1;
            }
            return count == 4 ? new Insets(parts[0], parts[1], parts[2], parts[3]) : null;
        }
        return null;
    }

    /// The value a defaults file would give `value` when it is plain: a
    /// color, a whole number, a boolean or insets; a string otherwise.
    /// `valueType` narrows nothing here.
    public static Object parseDefaultsValue(String key, String value, Class<?> valueType)
            throws IllegalArgumentException {
        Object v = plain(value);
        return v != null ? v : value;
    }

    private static void sourceIgnored(String what) {
        if (!sourceLogged) {
            sourceLogged = true;
            com.codename1.io.Log.p("desktopcompat.laf: custom defaults source " + what
                    + " is recorded and not read; put the values with FlatLaf.setGlobalExtraDefaults or UIManager.put");
        }
    }

    /// Recorded and not read; see the class description.
    public static void registerCustomDefaultsSource(String packageName) {
        sourceIgnored(packageName);
    }

    public static void unregisterCustomDefaultsSource(String packageName) {
    }

    /// The extra defaults of every FlatLaf look, or `null`.
    public static Map<String, String> getGlobalExtraDefaults() {
        return globalExtraDefaults;
    }

    /// Sets defaults that every FlatLaf look gets on top of its own. They
    /// take effect when a look and feel is next set.
    public static void setGlobalExtraDefaults(Map<String, String> globalExtraDefaults) {
        FlatLaf.globalExtraDefaults = globalExtraDefaults == null ? null
                : new HashMap<String, String>(globalExtraDefaults);
    }

    public Map<String, String> getExtraDefaults() {
        return extraDefaults;
    }

    /// Sets defaults this look gets on top of its own, when it is next
    /// set.
    public void setExtraDefaults(Map<String, String> extraDefaults) {
        this.extraDefaults = extraDefaults == null ? null : new HashMap<String, String>(extraDefaults);
    }

    /// `[light]` or `[dark]`, the prefix of a key for one palette only.
    public static String getUIKeyLightOrDarkPrefix(boolean dark) {
        return dark ? "[dark]" : "[light]";
    }

    /// Restyles every open window from the look and feel that is set.
    public static void updateUI() {
        LafTheme.restyleAll();
    }

    /// The same as [#updateUI], after the events that are waiting.
    public static void updateUILater() {
        com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                LafTheme.restyleAll();
            }
        });
    }

    /// False: the windows of this layer are Codename One forms.
    public static boolean supportsNativeWindowDecorations() {
        return false;
    }

    public static boolean isUseNativeWindowDecorations() {
        return useNativeWindowDecorations;
    }

    /// Recorded only.
    public static void setUseNativeWindowDecorations(boolean enabled) {
        useNativeWindowDecorations = enabled;
    }

    public static void revalidateAndRepaintAllFramesAndDialogs() {
        LafTheme.restyleAll();
    }

    public static void repaintAllFramesAndDialogs() {
        LafTheme.restyleAll();
    }

    /// False: there are no mnemonics to underline.
    public static boolean isShowMnemonics() {
        return false;
    }

    /// Does nothing.
    public static void showMnemonics(Component c) {
    }

    /// Does nothing.
    public static void hideMnemonics() {
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    public static String getPreferredFontFamily() {
        return preferredFontFamily;
    }

    /// Recorded only: the font of the layer is the theme's.
    public static void setPreferredFontFamily(String preferredFontFamily) {
        FlatLaf.preferredFontFamily = preferredFontFamily;
    }

    public static String getPreferredLightFontFamily() {
        return preferredLightFontFamily;
    }

    /// Recorded only.
    public static void setPreferredLightFontFamily(String preferredLightFontFamily) {
        FlatLaf.preferredLightFontFamily = preferredLightFontFamily;
    }

    public static String getPreferredSemiboldFontFamily() {
        return preferredSemiboldFontFamily;
    }

    /// Recorded only.
    public static void setPreferredSemiboldFontFamily(String preferredSemiboldFontFamily) {
        FlatLaf.preferredSemiboldFontFamily = preferredSemiboldFontFamily;
    }

    public static String getPreferredMonospacedFontFamily() {
        return preferredMonospacedFontFamily;
    }

    /// Recorded only.
    public static void setPreferredMonospacedFontFamily(String preferredMonospacedFontFamily) {
        FlatLaf.preferredMonospacedFontFamily = preferredMonospacedFontFamily;
    }
}
