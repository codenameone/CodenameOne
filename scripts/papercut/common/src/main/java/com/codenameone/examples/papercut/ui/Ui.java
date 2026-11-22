/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.papercut.ui;

import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codenameone.examples.papercut.api.PlayerDto;
import com.codenameone.examples.papercut.net.Session;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/// The few building blocks every screen is made of. None of them draws
/// anything itself: each one only names a UIID, and `theme.css` decides what a
/// `PrimaryButton`, a `Panel` or a `Banner` looks like. Re-skinning the game is
/// editing that file, not this one.
// tag::papercut-ui[]
public final class Ui {
    /// The widest a column of fields and buttons grows, in millimetres.
    public static final int MAX_COLUMN_MM = 90;

    private Ui() {
    }

    /// A screen: a scrolling column on the game's backdrop, its name reported
    /// to analytics and given to the form so tests can find it.
    public static Form form(String title, String name) {
        Form f = new Form(title, BoxLayout.y());
        f.setName(name);
        f.setUIID("Form");
        f.getToolbar().setUIID("Toolbar");
        f.getToolbar().getTitleComponent().setUIID("Title");
        f.setTitle(caps(title));
        f.setScrollableY(true);
        return f;
    }

    /// On a desktop, lifts every theme font to the floor `Type` keeps for the
    /// HUD. The stylesheet sizes its small print in millimetres, and a monitor
    /// reports so few pixels to the millimetre that a 2mm caption -- a menu
    /// item's subtitle, a store row's tag -- came out at seven pixels. Call it
    /// once, before the first form shows. Does nothing on a phone or tablet.
    public static void desktopFonts() {
        if (!CN.isDesktop()) {
            return;
        }
        float perMm = CN.convertToPixels(100f) / 100f;
        if (perMm <= 0 || perMm >= 5.5f) {
            return;
        }
        float scale = 5.5f / perMm;
        try {
            Resources r = Resources.openLayered("/theme");
            String[] names = r.getThemeResourceNames();
            if (names == null || names.length == 0) {
                return;
            }
            Hashtable theme = r.getTheme(names[0]);
            Hashtable lifted = new Hashtable();
            Enumeration keys = theme.keys();
            while (keys.hasMoreElements()) {
                Object k = keys.nextElement();
                Object v = theme.get(k);
                if (v instanceof Font && ((Font) v).isTTFNativeFont()) {
                    Font f = (Font) v;
                    v = f.derive(f.getPixelSize() * scale, f.getStyle());
                }
                lifted.put(k, v);
            }
            UIManager.getInstance().setThemeProps(lifted);
        } catch (IOException e) {
            Log.e(e);
        }
    }

    /// The widest a list screen reads on a desktop, in pixels: a row with its
    /// label at one edge of a monitor and its button at the other is two
    /// things, not one.
    static final int DESKTOP_PAGE_PX = 1000;

    /// `mm` millimetres in pixels, with the same desktop floor `desktopFonts`
    /// gives the type. A monitor reports so few pixels to the millimetre that
    /// a panel sized for a phone came out as a phone-sized box in the middle
    /// of a large window; sized through this it grows with its text.
    public static int mm(float mm) {
        int px = CN.convertToPixels(mm);
        if (!CN.isDesktop()) {
            return px;
        }
        float perMm = CN.convertToPixels(100f) / 100f;
        return perMm > 0 && perMm < 5.5f ? (int) (mm * 5.5f) : px;
    }

    /// The column a desktop list screen keeps to: wide enough to fill a large
    /// window with something, never so wide that a row splits in two.
    static int desktopPage(int width) {
        return Math.max(DESKTOP_PAGE_PX, width * 3 / 5);
    }

    /// On a desktop, keeps a list screen to a column in the middle of the
    /// window, however wide the window is. Does nothing on a phone or tablet.
    public static void desktopColumn(final Form f) {
        if (!CN.isDesktop()) {
            return;
        }
        final Container c = f.getContentPane();
        final Runnable fit = () -> {
            int side = Math.max(0, (f.getWidth() - desktopPage(f.getWidth())) / 2);
            Style s = c.getUnselectedStyle();
            s.setPaddingUnitLeft(Style.UNIT_TYPE_PIXELS);
            s.setPaddingUnitRight(Style.UNIT_TYPE_PIXELS);
            s.setPadding(Component.LEFT, side);
            s.setPadding(Component.RIGHT, side);
        };
        f.addShowListener(e -> {
            fit.run();
            f.revalidate();
        });
        f.addSizeChangedListener(e -> {
            fit.run();
            f.revalidateLater();
        });
    }

    /// Adds a back command that leaves through `leave`.
    public static void back(Form f, final Runnable leave) {
        Command back = new Command("") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                leave.run();
            }
        };
        // The toolbar draws its arrow in millimetres, which on a monitor came
        // out as a speck beside the title: there it gets the same desktop
        // floor as the fonts.
        float mm = 3f;
        float perMm = CN.convertToPixels(100f) / 100f;
        if (CN.isDesktop() && perMm > 0 && perMm < 5.5f) {
            mm = mm * 5.5f / perMm;
        }
        f.getToolbar().setBackCommand(back, Toolbar.BackCommandPolicy.AS_ARROW, mm);
    }

    public static Label label(String text, String uiid) {
        return new Label(text, uiid);
    }

    public static SpanLabel text(String text, String uiid) {
        SpanLabel l = new SpanLabel(text);
        l.setUIID("Container");
        l.setTextUIID(uiid);
        return l;
    }

    public static Button button(String text, String uiid, String name, ActionListener<ActionEvent> onClick) {
        Button b = new Button(text, uiid);
        b.setName(name);
        if (onClick != null) {
            b.addActionListener(onClick);
        }
        return b;
    }

    /// The bright, rounded call to action of a screen.
    public static Button primary(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "Button", name, onClick);
    }

    /// The outlined button for everything secondary.
    public static Button secondary(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "Plain", name, onClick);
    }

    /// Sets a material icon drawn on nothing, so a label with a translucent
    /// background doesn't get a darker square behind its icon.
    public static void glyph(com.codename1.ui.Label l, char icon, float mm) {
        com.codename1.ui.plaf.Style st = new com.codename1.ui.plaf.Style(l.getUnselectedStyle());
        st.setBgTransparency(0);
        st.setBorder(null);
        l.setIcon(com.codename1.ui.FontImage.createMaterial(icon, st, mm));
    }

    public static Container panel(Component... children) {
        Container c = BoxLayout.encloseY(children);
        c.setUIID("Card");
        return c;
    }

    /// Equal cells side by side, with a gap between them and none at the
    /// outer edges, so a row lines up with the full-width buttons above it.
    public static Container row(Component... children) {
        Container c = new Container(new GridLayout(1, children.length));
        for (int i = 0; i < children.length; i++) {
            Style s = children[i].getAllStyles();
            s.setMarginUnitLeft(Style.UNIT_TYPE_DIPS);
            s.setMarginUnitRight(Style.UNIT_TYPE_DIPS);
            s.setMarginLeft(i == 0 ? 0f : 0.8f);
            s.setMarginRight(i == children.length - 1 ? 0f : 0.8f);
            c.add(children[i]);
        }
        return c;
    }

    /// A column no wider than a phone held upright, centred on wider screens,
    /// with the same gutter on both sides. Fields and buttons inside it all
    /// fill its width, so they line up.
    public static Container column(Component... children) {
        Container col = new Container(BoxLayout.y()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension d = super.calcPreferredSize();
                int gutter = CN.convertToPixels(4) * 2;
                d.setWidth(Math.min(CN.convertToPixels(MAX_COLUMN_MM), CN.getDisplayWidth() - gutter));
                return d;
            }
        };
        for (Component child : children) {
            col.add(child);
        }
        return col;
    }

    /// `column` centred in the space it is given.
    public static Container centeredColumn(Container column) {
        return BorderLayout.centerCenter(column);
    }

    /// A rounded bar filled to `value` of `max`, for experience and quests.
    public static Component bar(final int value, final int max, final int color) {
        return new Component() {
            @Override
            public void paint(Graphics g) {
                // Segmented and square, the way a shooter shows a stat.
                int h = getHeight();
                int w = getWidth();
                int segs = 10;
                int gap = Math.max(1, CN.convertToPixels(0.3f));
                int sw = Math.max(1, (w - gap * (segs - 1)) / segs);
                int lit = max <= 0 ? 0 : (int) ((long) Math.max(0, Math.min(value, max)) * segs * 2 / max + 1) / 2;
                for (int i = 0; i < segs; i++) {
                    g.setColor(i < lit ? color : 0x2a3038);
                    g.fillRect(getX() + i * (sw + gap), getY(), sw, h);
                }
            }

            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(CN.convertToPixels(26), CN.convertToPixels(1.4f));
            }
        };
    }

    public static Container centered(Component... children) {
        return FlowLayout.encloseCenter(children);
    }

    /// The ink coins at the top of the menus.
    public static Label wallet(int coins) {
        Label l = new Label(String.valueOf(coins), "Accent");
        l.setName("Coins");
        l.setMaterialIcon(FontImage.MATERIAL_WATER_DROP);
        return l;
    }

    /// Menu labels are set as written: paper and handwriting, not capitals.
    public static String caps(String s) {
        return s == null ? "" : s;
    }

    public static void toast(String text) {
        ToastBar.showMessage(text, FontImage.MATERIAL_INFO, 2500);
    }

    public static void failed(int status, String message) {
        toast(message == null || message.length() == 0 ? "Something went wrong (" + status + ")" : message);
    }

    public static PlayerDto me() {
        return Session.user();
    }

    /// True once the player has bought ink coins with real money: a paying
    /// player is not shown interstitials.
    public static boolean adFree() {
        return com.codename1.io.Preferences.get("papercut.paid", false);
    }

    public static void later(Runnable r) {
        CN.callSerially(r);
    }
}
// end::papercut-ui[]
