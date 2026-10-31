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
package com.codenameone.examples.dash.ui;

import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.net.Session;

/// The few building blocks every screen is made of. None of them draws
/// anything itself: each one only names a UIID, and `theme.css` decides what a
/// `PrimaryButton`, a `Panel` or a `Banner` looks like. Re-skinning the game is
/// editing that file, not this one.
// tag::dash-ui[]
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
        f.setUIID("GameForm");
        f.getToolbar().setUIID("GameToolbar");
        f.getToolbar().getTitleComponent().setUIID("GameTitle");
        f.setScrollableY(true);
        Telemetry.screen(name);
        return f;
    }

    /// Adds a back command that leaves through `leave`.
    public static void back(Form f, final Runnable leave) {
        Command back = new Command("") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                leave.run();
            }
        };
        f.getToolbar().setBackCommand(back);
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
        return button(text, "PrimaryButton", name, onClick);
    }

    /// The outlined button for everything secondary.
    public static Button secondary(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "GhostButton", name, onClick);
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
        c.setUIID("Panel");
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
                int h = getHeight();
                int w = getWidth();
                g.setAntiAliased(true);
                g.setColor(0x0e1224);
                g.fillRoundRect(getX(), getY(), w, h, h, h);
                int fill = max <= 0 ? 0 : (int) ((long) Math.max(0, Math.min(value, max)) * (w - 2) / max);
                if (fill > 0) {
                    g.setColor(color);
                    g.fillRoundRect(getX() + 1, getY() + 1, Math.max(h - 2, fill), h - 2, h - 2, h - 2);
                    g.setColor(0xffffff);
                    g.setAlpha(70);
                    g.fillRoundRect(getX() + 2, getY() + 1, Math.max(h - 4, fill - 2), (h - 2) / 2, h / 2, h / 2);
                    g.setAlpha(255);
                }
            }

            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(CN.convertToPixels(20), CN.convertToPixels(2.2f));
            }
        };
    }

    public static Container centered(Component... children) {
        return FlowLayout.encloseCenter(children);
    }

    /// The trophies, coins and gems strip at the top of the menus.
    public static Container wallet(PlayerDto p) {
        Container c = new Container(new GridLayout(1, 3));
        c.setUIID("WalletBar");
        c.add(stat(FontImage.MATERIAL_EMOJI_EVENTS, p.trophies(), "Trophies"));
        c.add(stat(FontImage.MATERIAL_MONETIZATION_ON, p.coins(), "Coins"));
        c.add(stat(FontImage.MATERIAL_DIAMOND, p.gems(), "Gems"));
        return c;
    }

    private static Label stat(char icon, int value, String uiid) {
        Label l = new Label(String.valueOf(value), uiid);
        l.setName(uiid);
        l.setMaterialIcon(icon);
        return l;
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

    public static boolean adFree() {
        PlayerDto p = Session.user();
        return p != null && p.owned() != null && p.owned().contains("royal-pass");
    }

    public static void later(Runnable r) {
        CN.callSerially(r);
    }
}
// end::dash-ui[]
