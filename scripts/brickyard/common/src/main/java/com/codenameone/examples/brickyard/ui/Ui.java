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
package com.codenameone.examples.brickyard.ui;

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
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.engage.Telemetry;
import com.codenameone.examples.brickyard.net.Session;

/// The few building blocks every screen is made of, so each screen reads as
/// what it shows rather than how it is laid out.
public final class Ui {
    /// The sections of the bottom bar, in order.
    static final String[] SECTIONS = {"Home", "Create", "Friends", "Shop", "Me"};
    private static final char[] ICONS = {FontImage.MATERIAL_HOME, FontImage.MATERIAL_CONSTRUCTION,
        FontImage.MATERIAL_GROUP, FontImage.MATERIAL_STOREFRONT, FontImage.MATERIAL_PERSON};

    private Ui() {
    }

    public static Form form(String title, String name) {
        Form f = new Form(title, BoxLayout.y());
        f.setName(name);
        f.setUIID("GameForm");
        // The native theme's content pane is opaque and light; this one lets
        // the form's palette through.
        f.getContentPane().setUIID("GameContent");
        f.getToolbar().setUIID("GameToolbar");
        f.getToolbar().getTitleComponent().setUIID("GameTitle");
        f.setScrollableY(true);
        // Touch scrolling needs no bar; the desktop-style thumb drew a white
        // stripe down the right edge of every screen.
        f.getContentPane().setScrollVisible(false);
        Telemetry.screen(name);
        return f;
    }

    /// A top-level screen: a form with the section bar at the bottom.
    public static Form section(String title, String name, int selected) {
        Form f = form(title, name);
        Container content = new Container(BoxLayout.y());
        content.setScrollableY(true);
        content.setScrollVisible(false);
        f.setLayout(new BorderLayout());
        f.setScrollableY(false);
        f.add(BorderLayout.CENTER, content);
        f.add(BorderLayout.SOUTH, sectionBar(selected));
        f.putClientProperty("content", content);
        return f;
    }

    /// The scrolling body of a form made by [#section].
    public static Container content(Form f) {
        Object c = f.getClientProperty("content");
        return c instanceof Container ? (Container) c : f.getContentPane();
    }

    static Container sectionBar(int selected) {
        Container bar = new Container(new GridLayout(1, SECTIONS.length));
        bar.setUIID("GameToolbar");
        bar.setSafeArea(true);
        for (int i = 0; i < SECTIONS.length; i++) {
            final int index = i;
            Button b = new Button(SECTIONS[i], i == selected ? "SectionTabSelected" : "SectionTab");
            // A 4.5mm glyph over the label, the selected one tinted rather
            // than boxed: a filled pill around one tab outweighed the bar.
            FontImage.setMaterialIcon(b, ICONS[i], 4.5f);
            b.setTextPosition(Component.BOTTOM);
            b.setGap(CN.convertToPixels(0.4f));
            b.setName("section" + SECTIONS[i]);
            b.addActionListener(e -> Nav.section(index));
            bar.add(b);
        }
        return bar;
    }

    public static void back(Form f, final Runnable leave) {
        Command cmd = new Command("") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                leave.run();
            }
        };
        f.getToolbar().setBackCommand(cmd);
        // An arrow and no words: a screen reader (and an agent driving the
        // app) needs the name the picture stands for.
        Button b = f.getToolbar().findCommandComponent(cmd);
        if (b != null) {
            b.setAccessibilityText("Back");
            b.setName("backButton");
        }
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

    public static Button primary(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "PrimaryButton", name, onClick);
    }

    public static Button secondary(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "SecondaryButton", name, onClick);
    }

    public static Button link(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, "LinkButton", name, onClick);
    }

    public static Container panel(Component... children) {
        Container c = BoxLayout.encloseY(children);
        c.setUIID("Panel");
        return c;
    }

    public static TextField field(String hint, int constraint, String name) {
        TextField t = new TextField("", hint, 20, constraint);
        t.setUIID("GameField");
        t.setName(name);
        // The hint is only drawn; screen readers (and the agent tools) read
        // the accessibility text.
        t.setAccessibilityText(hint);
        return t;
    }

    /// The signed-in account's Bricks, or an empty label when signed out.
    public static Label bricks() {
        AccountDto me = Session.user();
        Label l = new Label(me == null ? "" : me.bricks() + " Bricks", "Bricks");
        FontImage.setMaterialIcon(l, FontImage.MATERIAL_VIEW_MODULE);
        l.setName("bricks");
        return l;
    }

    public static void toast(String text) {
        ToastBar.showInfoMessage(text);
    }

    public static void failed(int status, String message) {
        toast(message);
    }

    public static void later(Runnable r) {
        CN.callSerially(r);
    }
}
