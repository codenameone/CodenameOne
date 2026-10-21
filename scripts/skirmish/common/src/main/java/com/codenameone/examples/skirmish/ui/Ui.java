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
package com.codenameone.examples.skirmish.ui;

import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.skirmish.api.PlayerDto;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Session;

/// The few building blocks every screen is made of. None of them draws
/// anything itself: each one only names a UIID, and `theme.css` decides what a
/// `WoodButton`, a `Panel` or a `Banner` looks like. Re-skinning the game is
/// editing that file, not this one.
// tag::skirmish-ui[]
public final class Ui {
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

    /// The big carved button of a menu.
    public static Button wood(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "WoodButton", name, onClick);
    }

    /// The smaller grey button for everything secondary.
    public static Button stone(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "StoneButton", name, onClick);
    }

    public static Container panel(Component... children) {
        Container c = BoxLayout.encloseY(children);
        c.setUIID("Panel");
        return c;
    }

    public static Container row(Component... children) {
        Container c = new Container(new GridLayout(1, children.length));
        for (Component child : children) {
            c.add(child);
        }
        return c;
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
// end::skirmish-ui[]
