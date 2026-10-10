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
package com.codenameone.examples.wayline.ui;

import com.codename1.components.SpanLabel;
import com.codename1.components.Switch;
import com.codename1.components.ToastBar;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.Prefs;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.RideStates;

import java.util.Date;

/// The pieces every screen is made of. Each one is a component with a style
/// name from `theme.css`; nothing here, or in any screen, sets a colour or a
/// font in code.
public final class Ui {
    /// What goes between two short facts on one line: a raised dot.
    public static final String DOT = "  \u00b7  ";

    private Ui() {
    }

    /// A screen with a title bar.
    ///
    /// @param name what a test waits for; the title is for people and changes
    public static Form form(String title, String name) {
        Form form = new Form(title, new BorderLayout());
        form.setUIID("WlForm");
        form.setName(name);
        watch(form);
        clearOfTheStatusBar(form);
        form.addSizeChangedListener(e -> clearOfTheStatusBar(form));
        return form;
    }

    /// Keeps the title bar of `form` under what the device draws over the top
    /// of the screen -- the clock, a notch -- the same way the map screens
    /// keep their floating bar under it: by the inset the device reports. The
    /// strip a form reserves for the status bar is as tall as the theme makes
    /// it, which says nothing of a notch; the title bar is padded by whatever
    /// that strip leaves uncovered.
    private static void clearOfTheStatusBar(Form form) {
        Container bar = form.getToolbar();
        if (bar == null) {
            return;
        }
        Rectangle safe = Display.getInstance().getDisplaySafeArea(new Rectangle());
        int covered = Math.max(0, safe.getY());
        Container area = bar.getParent();
        int kept = 0;
        if (area != null && area.getLayout() instanceof BorderLayout) {
            Component strip = ((BorderLayout) area.getLayout()).getNorth();
            if (strip != null && strip != bar) {
                kept = strip.getPreferredH();
            }
        }
        int styled = UIManager.getInstance().getComponentStyle("Toolbar").getPaddingTop();
        int wanted = styled + Math.max(0, covered - kept);
        if (bar.getUnselectedStyle().getPaddingTop() != wanted) {
            bar.getAllStyles().setPaddingUnitTop(Style.UNIT_TYPE_PIXELS);
            bar.getAllStyles().setPaddingTop(wanted);
        }
    }

    /// Gives `label` a material icon `size` millimetres tall, and the space
    /// between it and the label's words: without one the two touch.
    public static void icon(Label label, char icon, float size) {
        FontImage.setMaterialIcon(label, icon, size);
        label.setGap(CN.convertToPixels(ICON_GAP));
    }

    /// The space between an icon and the words beside it, in millimetres.
    private static final float ICON_GAP = 1.4f;

    /// Has `form` counted as a screen each time it is shown, under its name.
    /// Every screen of the app is made by this class or handed to this method,
    /// so this is the one place a screen view is reported from.
    public static void watch(final Form form) {
        form.addShowListener(e -> viewed(form.getName()));
    }

    /// Counts something that takes the place of a screen without being a
    /// form: a pane of the admin's console on a wide window.
    public static void viewed(String name) {
        Telemetry.screen(name);
    }

    /// The first screen of a mode: no title bar, and layers, so that a map can
    /// fill the screen with the controls floating over it and the menu sliding
    /// in over both.
    public static Form home(String name) {
        Form form = new Form("", new LayeredLayout());
        form.setUIID("WlForm");
        form.setName(name);
        watch(form);
        form.getToolbar().hideToolbar();
        // What is on the screen scrolls, where it needs to. The screen itself
        // does not, or it would offer to scroll the map away.
        form.setScrollable(false);
        return form;
    }

    /// Gives `form` a back arrow that returns to `previous`.
    public static void back(Form form, final Form previous) {
        back(form, () -> previous.showBack());
    }

    /// Gives `form` a back arrow that runs `leave`.
    ///
    /// On a desktop whose window owns the title bar there is no title bar in
    /// the app to carry an arrow. The command is then named, so that it is in
    /// the window's menu, and the screen gets a line of its own to go back
    /// from. A screen that has put something of its own at its top keeps it,
    /// under that line: a screen with no way back is one nobody can leave
    /// without a device's back button, and a desktop has none.
    public static void back(final Form form, final Runnable leave) {
        boolean window = CN.isDesktop() && form.getToolbar().getParent() == null;
        Command back = form.getToolbar().setBackCommand(window ? Lang.tr("Back") : "",
                e -> leave.run());
        if (!window) {
            return;
        }
        back.setDesktopMenu(Command.DESKTOP_MENU_VIEW);
        back.setDesktopShortcut('[');
        final boolean[] added = new boolean[1];
        form.addShowListener(e -> {
            if (added[0] || form.getToolbar().getParent() != null
                    || !(form.getContentPane().getLayout() instanceof BorderLayout)) {
                return;
            }
            added[0] = true;
            Container bar = new Container(new BorderLayout());
            bar.setUIID("WlPaneBar");
            Button arrow = button("Back", "back-bar", "WlLink", e2 -> leave.run());
            icon(arrow, FontImage.MATERIAL_ARROW_BACK, 3.4f);
            bar.add(BorderLayout.WEST, arrow);
            Component top = ((BorderLayout) form.getContentPane().getLayout()).getNorth();
            if (top == null) {
                form.add(BorderLayout.NORTH, bar);
            } else {
                top.remove();
                Container both = new Container(BoxLayout.y());
                both.add(bar).add(top);
                form.add(BorderLayout.NORTH, both);
            }
            Layouts.dress(bar);
            form.revalidate();
        });
    }

    /// A column of content with the page's margins, that scrolls when it has to.
    public static Container page() {
        Container page = new Column();
        page.setUIID("WlPage");
        page.setScrollableY(true);
        page.setScrollVisible(false);
        page.setSafeArea(true);
        return page;
    }

    public static Container card() {
        Container card = new Container(BoxLayout.y());
        card.setUIID("WlCard");
        return card;
    }

    public static Label label(String text, String uiid) {
        return new Label(text == null ? "" : text, uiid);
    }

    /// Text that wraps.
    public static SpanLabel text(String text, String uiid) {
        SpanLabel label = new SpanLabel(text == null ? "" : text);
        label.setUIID("Container");
        label.setTextUIID(uiid);
        return label;
    }

    public static Button primary(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, name, "WlPrimary", onClick);
    }

    public static Button secondary(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, name, "WlSecondary", onClick);
    }

    public static Button danger(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, name, "WlDanger", onClick);
    }

    public static Button link(String text, String name, ActionListener<ActionEvent> onClick) {
        return button(text, name, "WlLink", onClick);
    }

    /// A round button that floats over the map.
    public static Button round(char icon, String name, ActionListener<ActionEvent> onClick) {
        Button button = button("", name, "WlMapButton", onClick);
        icon(button, icon, 4f);
        return button;
    }

    /// The same button for a bar that is a surface of its own: flat, since a
    /// shadow says "this floats", and on a bar nothing does.
    public static Button flat(char icon, String name, ActionListener<ActionEvent> onClick) {
        Button button = button("", name, "WlBarButton", onClick);
        icon(button, icon, 4f);
        return button;
    }

    /// One of a row of things to do: an icon with a word under it.
    public static Button action(char icon, String text, String name,
            ActionListener<ActionEvent> onClick) {
        Button button = button(text, name, "WlAction", onClick);
        icon(button, icon, 4f);
        button.setTextPosition(Component.BOTTOM);
        button.setGap(CN.convertToPixels(0.6f));
        return button;
    }

    /// A short fact in a pill: how far, how long. The icon may be 0.
    public static Label chip(String text, char icon) {
        Label chip = new Label(text == null ? "" : text, "WlChip");
        if (icon != 0) {
            icon(chip, icon, 2.7f);
        }
        return chip;
    }

    /// A person, as the initials of their name in a circle.
    public static Label avatar(String name) {
        Label avatar = new Label("", "WlAvatar");
        // Initials are not words: looking them up would translate "OK".
        avatar.setShouldLocalize(false);
        avatar.setText(initials(name));
        return avatar;
    }

    /// The first letters of the first and last words of `name`.
    public static String initials(String name) {
        String text = name == null ? "" : name.trim();
        if (text.length() == 0) {
            return "?";
        }
        int space = text.lastIndexOf(' ');
        String first = text.substring(0, 1);
        return space < 0 || space + 1 >= text.length() ? first
                : first + text.substring(space + 1, space + 2);
    }

    public interface Flipped {
        void flipped(boolean on);
    }

    /// A switch that reports each change.
    public static Switch toggle(String name, boolean value, final Flipped flipped) {
        final Switch toggle = new Switch();
        toggle.setName(name);
        toggle.setValue(value);
        toggle.addChangeListener(e -> flipped.flipped(toggle.isValue()));
        return toggle;
    }

    public interface Chosen {
        void chosen(String value);
    }

    /// A few options side by side, one of them selected. Each button is named
    /// `name-value`.
    ///
    /// @param labels what each option is called, for people
    /// @param values what each option is, for the code
    public static Container choice(String name, String[] labels, final String[] values,
            String current, final Chosen chosen) {
        return choice(name, labels, values, current, chosen, values.length);
    }

    /// The same, in rows of `columns`, for options that do not fit in one.
    public static Container choice(String name, String[] labels, final String[] values,
            String current, final Chosen chosen, int columns) {
        Container row = new Container(new GridLayout((values.length + columns - 1) / columns,
                columns));
        row.setUIID("WlSegments");
        ButtonGroup group = new ButtonGroup();
        for (int iter = 0; iter < values.length; iter++) {
            final String value = values[iter];
            RadioButton option = RadioButton.createToggle(labels[iter], group);
            option.setUIID("WlSegment");
            option.setName(name + "-" + value);
            option.setSelected(value.equals(current));
            option.addActionListener(e -> chosen.chosen(value));
            row.add(option);
        }
        return row;
    }

    /// The longest address that is still shown on one line.
    private static final int LONG_ADDRESS = 26;

    /// Who a screen is about: their initials, their name, the address they sign
    /// in with, and under those `value` when there is something to say about
    /// them. The address wraps rather than being cut off; it is what tells
    /// two people with the same name apart.
    public static Container person(String name, String username, Component value) {
        Container who = new Container(new BorderLayout());
        who.setUIID("WlPanel");
        Container names = new Container(BoxLayout.y());
        Label called = plain(name, "WlHeading");
        shrink(called, 3f);
        String signsIn = username == null ? "" : username;
        Component address;
        if (signsIn.length() > LONG_ADDRESS) {
            // An address has no spaces to break at: a long one is broken
            // where the line ends, which keeps all of it readable.
            SpanLabel wrapped = text("", "WlMuted");
            wrapped.setShouldLocalize(false);
            wrapped.setText(signsIn);
            address = wrapped;
        } else {
            Label line = plain(signsIn, "WlMuted");
            shrink(line, 2f);
            address = line;
        }
        names.add(called).add(address);
        if (value != null) {
            names.add(FlowLayout.encloseIn(value));
        }
        who.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(avatar(name)));
        who.add(BorderLayout.CENTER, names);
        return who;
    }

    /// Lets `label` draw its text smaller, down to `smallest` millimetres,
    /// when it would otherwise be cut off -- and never larger than its style
    /// says, which is what fitting text to its space does when left alone.
    public static void shrink(Label label, float smallest) {
        float styled = label.getUnselectedStyle().getFont().getPixelSize()
                / (float) CN.convertToPixels(1f);
        label.setAutoSizeMode(true);
        label.setMaxAutoSize(styled);
        label.setMinAutoSize(Math.min(smallest, styled));
    }

    /// Lays `form` out again after its content changed while it was showing.
    ///
    /// Twice, the second time after the first has been drawn: text that wraps
    /// learns its width from the first pass, and only then knows how many
    /// lines it is.
    public static void refresh(final Form form) {
        form.revalidate();
        CN.callSerially(() -> form.revalidate());
    }

    /// The heading over a group of settings or facts.
    public static void section(Container page, String title) {
        page.add(label(title, "WlSection"));
    }

    /// A label for text that is a name or a number, not a word to translate.
    public static Label plain(String text, String uiid) {
        Label label = new Label("", uiid);
        label.setShouldLocalize(false);
        label.setText(text == null ? "" : text);
        return label;
    }

    /// One line of a receipt or a record: what it is on one side, and its
    /// value, which is never translated, on the other.
    public static Container fact(String caption, String value) {
        return fact(caption, value, "WlFactValue");
    }

    public static Container fact(String caption, String value, String valueUiid) {
        Container fact = new Container(new BorderLayout());
        fact.setUIID("WlFact");
        fact.add(BorderLayout.CENTER, label(caption, "WlFactLabel"));
        fact.add(BorderLayout.EAST, plain(value, valueUiid));
        return fact;
    }

    /// What a screen shows instead of a list with nothing in it, and at the top
    /// of a screen that is about one thing: a large icon, a line, and a few
    /// words under it.
    public static Container banner(char icon, String title, String text, String iconUiid) {
        Container banner = new Container(BoxLayout.y());
        banner.setUIID("WlBanner");
        Label image = new Label("", iconUiid);
        icon(image, icon, 6f);
        banner.add(FlowLayout.encloseCenter(image));
        SpanLabel heading = text(title, "WlBannerTitle");
        heading.getTextComponent().setAlignment(Component.CENTER);
        banner.add(heading);
        if (text != null && text.length() > 0) {
            SpanLabel words = text(text, "WlBannerText");
            words.getTextComponent().setAlignment(Component.CENTER);
            banner.add(words);
        }
        return banner;
    }

    public static Container empty(char icon, String title, String text) {
        return banner(icon, title, text, "WlBannerIcon");
    }

    /// A list that fills a screen: rows from edge to edge, scrolling.
    public static Container list(String name) {
        Container list = new Column();
        list.setUIID("WlList");
        list.setScrollableY(true);
        list.setScrollVisible(false);
        list.setName(name);
        // Until it is filled: every list is emptied by what fills it.
        list.add(new Skeleton(5));
        return list;
    }

    /// Has a pull down on `list` fetch what is in it again, with `load`. A
    /// window on a desktop is not pulled, and gets nothing.
    public static void pull(Container list, final Runnable load) {
        if (!CN.isDesktop()) {
            list.addPullToRefresh(load);
        }
    }

    /// A column that stays a column: on a window wider than a column of text
    /// should be, it keeps its content in the middle rather than stretching
    /// every row and every button from one edge to the other.
    private static final class Column extends Container {
        private String styled;
        private int margin;

        Column() {
            super(BoxLayout.y());
        }

        @Override
        public void layoutContainer() {
            // The margin is the style's own, read once for each style the
            // column is given: what is set below replaces it.
            if (!getUIID().equals(styled)) {
                styled = getUIID();
                margin = getUnselectedStyle().getPaddingLeftNoRTL();
            }
            Layouts.centre(this, margin);
            super.layoutContainer();
        }
    }

    public interface Reason {
        void given(String reason);
    }

    /// Asks why, before something an admin does to someone else's account or
    /// ride. The reason is kept with what was done, so it is not optional.
    ///
    /// @param action what the button says, which is what will be done
    public static void reason(final Form previous, String title, String text, String action,
            final Reason reason) {
        final Form form = form(title, "Reason");
        back(form, previous);
        form.add(BorderLayout.CENTER, reasonPage(text, action, reason));
        form.show();
    }

    /// What [#reason] asks with, for a caller that has its own place to put
    /// it: the admin console shows it beside the list on a wide screen.
    public static Container reasonPage(String text, String action, final Reason reason) {
        final Container page = page();
        page.add(text(text, "WlText"));
        final TextField why = field(page, "Reason", "reason", "A sentence the record will keep");
        final SpanLabel error = text("", "WlError");
        error.setName("error");
        page.add(error);
        page.add(danger(action, "confirm", e -> {
            String given = why.getText().trim();
            if (given.length() == 0) {
                error.setText(Lang.tr("Say why, in a few words."));
                page.getComponentForm().revalidate();
                return;
            }
            reason.given(given);
        }));
        return page;
    }

    private static Button button(String text, String name, String uiid,
            ActionListener<ActionEvent> onClick) {
        Button button = new Button(text, uiid);
        // The name is what a test finds the button by.
        button.setName(name);
        if (onClick != null) {
            button.addActionListener(onClick);
        }
        return button;
    }

    /// Adds a caption and a text field under it to `to`, and returns the field.
    public static TextField field(Container to, String caption, String name, String hint,
            int constraint) {
        TextField field = new TextField("", hint, 24, constraint);
        field.setUIID("WlField");
        field.getHintLabel().setUIID("WlFieldHint");
        field.setName(name);
        to.add(label(caption, "WlLabel")).add(field);
        return field;
    }

    public static TextField field(Container to, String caption, String name, String hint) {
        return field(to, caption, name, hint, TextArea.ANY);
    }

    /// A row of a list: an icon, two lines of text, and something at the end.
    /// The icon may be 0, and the last two null.
    public static Container row(char icon, String title, String detail, Component value) {
        Container row = new Container(new BorderLayout());
        row.setUIID("WlRow");
        if (icon != 0) {
            Label image = new Label("", "WlRowIcon");
            icon(image, icon, 4f);
            row.add(BorderLayout.WEST, image);
        }
        Container lines = new Container(BoxLayout.y());
        lines.add(label(title, "WlRowTitle"));
        if (detail != null && detail.length() > 0) {
            lines.add(text(detail, "WlRowDetail"));
        }
        row.add(BorderLayout.CENTER, lines);
        if (value != null) {
            // Its own height, in the middle of the row: straight into EAST it
            // would be stretched to the height of the row.
            row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(value));
        }
        return row;
    }

    /// Makes the whole of `row` one button. The name is the button's, for a
    /// test to click.
    public static Container tap(Container row, String name, ActionListener<ActionEvent> onClick) {
        Button lead = new Button();
        lead.setName(name);
        lead.setHidden(true);
        lead.addActionListener(onClick);
        row.add(BorderLayout.SOUTH, lead);
        row.setLeadComponent(lead);
        return row;
    }

    /// A day, without the time.
    public static String day(long millis) {
        return new SimpleDateFormat("MMM d, yyyy").format(new Date(millis));
    }

    /// The product a ride was asked for as, in words.
    public static String product(String product) {
        return "comfort".equals(product) ? "Comfort" : "xl".equals(product) ? "XL" : "Standard";
    }

    /// A moment, as a short date and time.
    public static String when(long millis) {
        return new SimpleDateFormat("MMM d, HH:mm").format(new Date(millis));
    }

    public static Label badge(String text, String uiid) {
        return new Label(text, uiid);
    }

    /// A row of numbers, each with a caption under it, `columns` across:
    /// add them with [StatRow#stat]. More than `columns` of them run onto a
    /// further row.
    public static StatRow stats(int columns) {
        return new StatRow(columns);
    }

    /// Tells the user something failed. The signature is `Net.Failed`'s, so a
    /// call can end `Ui::fail`.
    public static void fail(int status, String message) {
        // What the server says is the server's wording; what the app says of
        // its own is translated. Looking both up costs nothing.
        ToastBar.showErrorMessage(Lang.tr(message));
    }

    /// What a list's load ends with when it fails: the user is told, as with
    /// [#fail], and a list still showing the outline of rows that are not
    /// coming shows what went wrong instead, with a way to ask again. A list
    /// that already has rows keeps them.
    ///
    /// @param load fetches the list again, or null when there is no asking again
    public static Net.Failed failed(final Container list, final Runnable load) {
        return (status, message) -> {
            fail(status, message);
            if (list.getComponentCount() != 1 || !(list.getComponentAt(0) instanceof Skeleton)) {
                return;
            }
            list.removeAll();
            list.add(empty(FontImage.MATERIAL_CLOUD_OFF, message, null));
            if (load != null) {
                list.add(FlowLayout.encloseCenter(link("Try again", "retry", e -> {
                    list.removeAll();
                    list.add(new Skeleton(5));
                    list.revalidate();
                    load.run();
                })));
            }
            list.revalidate();
        };
    }

    public static void say(String message) {
        ToastBar.showInfoMessage(Lang.tr(message));
    }

    /// An amount of money. Whole cents in and text out: `$12.40` for dollars,
    /// and the amount followed by the currency's code for anything else.
    public static String money(long cents, String currency) {
        long whole = cents / 100;
        long part = Math.abs(cents % 100);
        String amount = whole + "." + (part < 10 ? "0" : "") + part;
        if (currency == null || currency.length() == 0 || "USD".equals(currency)) {
            return "$" + amount;
        }
        return amount + " " + currency;
    }

    /// A distance, in the units chosen in Settings.
    public static String distance(double meters) {
        if (Prefs.UNITS_MILES.equals(Prefs.units())) {
            double feet = meters * 3.28084;
            if (feet < 900) {
                return Lang.tr("{0} ft", String.valueOf(Math.round(feet / 50d) * 50));
            }
            return Lang.tr("{0} mi", tenths(meters / 1609.344));
        }
        if (meters < 950) {
            return Lang.tr("{0} m", String.valueOf(Math.round(meters / 50d) * 50));
        }
        return Lang.tr("{0} km", tenths(meters / 1000d));
    }

    /// `value` with one digit after the point.
    public static String tenths(double value) {
        long tenths = Math.round(value * 10d);
        return (tenths / 10) + "." + (tenths % 10);
    }

    public static String minutes(double seconds) {
        return Lang.tr("{0} min", String.valueOf(Math.max(1L, Math.round(seconds / 60d))));
    }

    /// A ride's state in words.
    public static String state(String state) {
        if (RideStates.REQUESTED.equals(state) || RideStates.OFFERED.equals(state)) {
            return "Finding a driver";
        }
        if (RideStates.ACCEPTED.equals(state)) {
            return "Driver on the way";
        }
        if (RideStates.ARRIVED.equals(state)) {
            return "Driver at pickup";
        }
        if (RideStates.IN_PROGRESS.equals(state)) {
            return "On the trip";
        }
        if (RideStates.COMPLETED.equals(state)) {
            return "Completed";
        }
        if (RideStates.CANCELLED_BY_RIDER.equals(state)) {
            return "Cancelled by rider";
        }
        if (RideStates.CANCELLED_BY_DRIVER.equals(state)) {
            return "Cancelled by driver";
        }
        if (RideStates.CANCELLED_BY_ADMIN.equals(state)) {
            return "Cancelled by Wayline";
        }
        if (RideStates.NO_DRIVERS.equals(state)) {
            return "No driver found";
        }
        return "";
    }

    /// The style of the badge a state is shown in.
    public static String stateBadge(String state) {
        if (RideStates.COMPLETED.equals(state)) {
            return "WlBadgeGood";
        }
        if (RideStates.isActive(state)) {
            return "WlBadgeWarn";
        }
        return "WlBadgeBad";
    }
}
