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
package com.codenameone.examples.bastion.ui;

import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import java.util.Date;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.security.Lock;
import com.codenameone.examples.bastion.security.Protect;

/// The few building blocks every screen is made of; the look of each is a
/// style in `src/main/css/theme.css`.
public final class Ui {
    private Ui() {
    }

    /// A scrolling form of one column.
    public static Form form(String title) {
        Form form = new Form(Lang.tr(title), BoxLayout.y());
        form.setUIID("BnForm");
        form.getContentPane().setUIID("BnPage");
        form.setScrollableY(true);
        return form;
    }

    /// Shows `form`. A `sensitive` one -- anything with a balance, a number
    /// or a code on it -- blocks screenshots while it is up; the form
    /// remembers it, so going back to it ([#reshow]) blocks them again.
    ///
    /// While the app is locked nothing but the lock screen is shown: a form
    /// an answer builds after the lock is kept for the unlock instead
    /// (`Lock.defer`), never painted over the lock.
    public static void show(Form form, boolean sensitive) {
        form.putClientProperty(SENSITIVE, sensitive ? Boolean.TRUE : Boolean.FALSE);
        if (Lock.isLocked()) {
            Lock.defer(form);
            return;
        }
        display(form, sensitive);
    }

    /// Shows a form shown before, with the protection it was shown with.
    public static void reshow(Form form) {
        show(form, isSensitive(form));
    }

    /// Whether `form` was shown as sensitive. A form this class never saw is
    /// treated as sensitive: the safe answer.
    public static boolean isSensitive(Form form) {
        return !Boolean.FALSE.equals(form.getClientProperty(SENSITIVE));
    }

    /// The lock screen itself: the one form shown while locked.
    static void showLockScreen(Form form) {
        form.putClientProperty(SENSITIVE, Boolean.TRUE);
        // No transition in either direction: a slide or fade would paint the
        // form being locked, balances and all, while the lock comes in.
        Form under = CN.getCurrentForm();
        Transition out = under == null ? null : under.getTransitionOutAnimator();
        if (under != null) {
            under.setTransitionOutAnimator(CommonTransitions.createEmpty());
        }
        form.setTransitionInAnimator(CommonTransitions.createEmpty());
        try {
            display(form, true);
        } finally {
            if (under != null) {
                under.setTransitionOutAnimator(out);
            }
        }
    }

    private static void display(Form form, boolean sensitive) {
        Protect.screen(sensitive);
        Lock.watch(form);
        form.show();
    }

    private static final String SENSITIVE = "BnSensitive";

    public static Label title(String text) {
        return new Label(Lang.tr(text), "BnTitle");
    }

    public static SpanLabel text(String text) {
        SpanLabel label = new SpanLabel(text);
        label.setTextUIID("BnText");
        return label;
    }

    public static SpanLabel muted(String text) {
        SpanLabel label = new SpanLabel(text);
        label.setTextUIID("BnMuted");
        return label;
    }

    public static Label error() {
        Label label = new Label("", "BnError");
        label.setVisible(false);
        return label;
    }

    public static void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(message != null && message.length() > 0);
        Form form = label.getComponentForm();
        if (form != null) {
            form.revalidate();
        }
    }

    public static Button primary(String text) {
        return new Button(Lang.tr(text), "BnPrimary");
    }

    public static Button secondary(String text) {
        return new Button(Lang.tr(text), "BnSecondary");
    }

    public static TextField field(String hint, int constraint) {
        TextField field = new TextField("", Lang.tr(hint), 20, constraint);
        field.setUIID("BnField");
        return field;
    }

    public static Component spacer() {
        return new Label(" ", "BnSpacer");
    }

    /// A screen of the signed-out app -- sign in, the code step, the lock --
    /// with no title bar: the brand band across the top is its header, as
    /// the front door of a bank app usually is. Answers the column the
    /// screen's fields go in; the form is its `getComponentForm()`.
    public static Container landing(String title, String heading, String subtitle) {
        Form form = new Form(Lang.tr(title), new BorderLayout());
        form.setUIID("BnForm");
        form.getToolbar().hideToolbar();
        Container band = new Container(BoxLayout.y(), "BnBrandBand");
        band.setSafeArea(true);
        Label logo = new Label("", "BnBrandLogo");
        FontImage.setMaterialIcon(logo, FontImage.MATERIAL_SHIELD, 9f);
        band.add(logo);
        // Wraps rather than ending in "...": a longer translation, or a
        // narrow phone, gets a second line.
        SpanLabel headline = new SpanLabel(Lang.tr(heading));
        headline.setTextUIID("BnBrandTitle");
        band.add(headline);
        if (subtitle != null) {
            SpanLabel line = new SpanLabel(Lang.tr(subtitle));
            line.setTextUIID("BnBrandText");
            band.add(line);
        }
        Container column = new Container(BoxLayout.y(), "BnPage");
        column.setScrollableY(true);
        form.add(BorderLayout.NORTH, band);
        form.add(BorderLayout.CENTER, column);
        return column;
    }

    /// A form with a back arrow that runs `back`. Whether it also has the
    /// menu is [Nav#form]'s question; the answer there is "never both".
    public static Form form(String title, final Runnable back) {
        Form form = form(title);
        if (back != null) {
            form.getToolbar().setBackCommand("", Toolbar.BackCommandPolicy.AS_ARROW, evt -> back.run());
        }
        return form;
    }

    /// A small heading over a group of rows.
    public static Label section(String text) {
        return new Label(Lang.tr(text), "BnLabel");
    }

    /// A row: a title and a line under it on the left, `value` on the right
    /// in `valueUiid`. Tapping it runs `tap`, when there is one.
    public static Container row(String title, String detail, String value, String valueUiid,
            final Runnable tap) {
        Container words = new Container(BoxLayout.y());
        words.add(new Label(title, "BnRowTitle"));
        if (detail != null && detail.length() > 0) {
            SpanLabel line = new SpanLabel(detail);
            line.setTextUIID("BnRowDetail");
            words.add(line);
        }
        Container row = new Container(new BorderLayout(), "BnRow");
        row.add(BorderLayout.CENTER, words);
        if (value != null) {
            row.add(BorderLayout.EAST, new Label(value, valueUiid == null ? "BnRowValue" : valueUiid));
        }
        if (tap != null) {
            row.setLeadComponent(lead(row, tap));
        }
        return row;
    }

    /// An invisible button that makes a whole row one tap target.
    private static Button lead(Container row, final Runnable tap) {
        Button lead = new Button();
        lead.setUIID("BnRowLead");
        lead.addActionListener(evt -> tap.run());
        row.add(BorderLayout.WEST, lead);
        return lead;
    }

    /// A line of quiet text for a list with nothing in it yet.
    public static SpanLabel empty(String text) {
        SpanLabel label = new SpanLabel(Lang.tr(text));
        label.setTextUIID("BnEmpty");
        return label;
    }

    /// Buttons side by side, sharing the width, with the theme's gap
    /// (`bnButtonGapMm`) between each two. The label size is the theme's and
    /// the same for every button: nothing here shrinks one label to fit.
    ///
    /// When the buttons do not fit one row at that size -- three on a phone,
    /// or a long translation -- the first, the screen's main action, takes a
    /// row of its own and the rest share the row under it; two that still do
    /// not fit are stacked. Wrapping beats a label ending in "...".
    public static Container across(Component... parts) {
        int gap = CN.convertToPixels(buttonGapMm());
        // A grid gives every cell the same width, so the widest button
        // decides whether the row fits.
        int widest = 0;
        for (Component part : parts) {
            widest = Math.max(widest, part.getPreferredW());
        }
        int needed = widest * parts.length + gap * (parts.length - 1);
        // The page's own side padding is about 4mm each side.
        int room = CN.getDisplayWidth() - CN.convertToPixels(8f);
        if (needed <= room || parts.length < 2) {
            return gridRow(gap, parts);
        }
        Container column = new Container(BoxLayout.y());
        if (parts.length > 2) {
            Component[] rest = new Component[parts.length - 1];
            System.arraycopy(parts, 1, rest, 0, rest.length);
            column.add(parts[0]);
            column.add(across(rest));
            return column;
        }
        column.addAll(parts);
        return column;
    }

    /// One grid row of `parts`, `gap` pixels apart. A grid has no gap of its
    /// own, so each cell gives up half of it on each inner side; the margins
    /// are start and end ones, which the RTL layout mirrors with the grid.
    private static Container gridRow(int gap, Component... parts) {
        Container row = new Container(new GridLayout(1, parts.length));
        int half = gap / 2;
        for (int iter = 0; iter < parts.length; iter++) {
            com.codename1.ui.plaf.Style style = parts[iter].getAllStyles();
            style.setMarginUnitLeft(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            style.setMarginUnitRight(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            style.setMarginLeft(iter == 0 ? 0 : half);
            style.setMarginRight(iter == parts.length - 1 ? 0 : gap - half);
        }
        row.addAll(parts);
        return row;
    }

    private static float buttonGapMm() {
        String value = com.codename1.ui.plaf.UIManager.getInstance().getThemeConstant(
                "bnButtonGapMm", "2");
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 2f;
        }
    }

    /// A key or code shown to be copied by eye: its groups of four (split at
    /// the spaces of `grouped`), each in a monospaced box, wrapping between
    /// groups and never inside one -- so it is always shown whole.
    public static Container keyGroups(String grouped) {
        Container box = new Container(new com.codename1.ui.layouts.FlowLayout(Component.CENTER));
        Font mono = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);
        int start = 0;
        while (start < grouped.length()) {
            int end = grouped.indexOf(' ', start);
            if (end < 0) {
                end = grouped.length();
            }
            if (end > start) {
                Label group = new Label(grouped.substring(start, end), "BnKeyGroup");
                group.getAllStyles().setFont(mono);
                box.add(group);
            }
            start = end + 1;
        }
        return box;
    }

    /// A fact whose value may be long -- a reference, an address: the label
    /// on a line of its own and the value wrapping in full under it, never
    /// cut off.
    public static Container fact(String title, String value) {
        Container row = new Container(BoxLayout.y(), "BnRow");
        row.add(new Label(title, "BnRowTitle"));
        SpanLabel text = new SpanLabel(value);
        text.setTextUIID("BnRowValueWrap");
        row.add(text);
        return row;
    }

    /// A screen at the end of a flow -- a receipt: no menu and no back arrow
    /// into the flow just finished, but a close button that runs `close`
    /// (the device's back button does the same).
    public static Form finished(String title, final Runnable close) {
        Form form = form(title);
        com.codename1.ui.Command done = com.codename1.ui.Command.createMaterial("",
                FontImage.MATERIAL_CLOSE, evt -> close.run());
        form.getToolbar().addCommandToLeftBar(done);
        form.setBackCommand(done);
        return form;
    }

    /// A choice among `labels`, shown as segments; `chosen` is told the index
    /// of the one tapped, once at the start for `initial`.
    public static Container segments(final String[] labels, int initial, final Chosen chosen) {
        final Container row = new Container(new GridLayout(1, labels.length), "BnSegments");
        for (int iter = 0; iter < labels.length; iter++) {
            final int index = iter;
            Button one = new Button(Lang.tr(labels[iter]), "BnSegment");
            one.addActionListener(evt -> {
                select(row, index);
                chosen.chose(index);
            });
            row.add(one);
        }
        select(row, initial);
        chosen.chose(initial);
        return row;
    }

    public interface Chosen {
        void chose(int index);
    }

    private static void select(Container row, int index) {
        for (int iter = 0; iter < row.getComponentCount(); iter++) {
            row.getComponentAt(iter).setUIID(iter == index ? "BnSegmentOn" : "BnSegment");
        }
        row.repaint();
    }

    /// A picker of the things named by `ids`, shown as `labels` (one per id),
    /// starting at `initialId` (the first when it is null or absent). Read
    /// the choice with [#pickedId]: it answers the id, never the label, so
    /// two payees with one nickname are still two payees.
    ///
    /// A string picker only knows its strings, so labels that repeat are made
    /// distinct with a counter (`Rent`, `Rent (2)`) before it sees them.
    public static Picker picker(String[] ids, String[] labels, String initialId) {
        String[] shown = distinct(labels);
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        // setStrings localizes the array in place, so `shown` stays the
        // picker's own strings and their positions stay those of `ids`.
        picker.setStrings(shown);
        picker.putClientProperty(IDS, ids.clone());
        if (shown.length > 0) {
            picker.setSelectedStringIndex(Math.max(0, indexOf(ids, initialId)));
        }
        picker.setUIID("BnField");
        return picker;
    }

    /// The id of what `picker` is on, or null when it is on nothing.
    public static String pickedId(Picker picker) {
        String[] ids = (String[]) picker.getClientProperty(IDS);
        int index = picker.getSelectedStringIndex();
        return ids == null || index < 0 || index >= ids.length ? null : ids[index];
    }

    /// Where `id` is in `ids`, or -1.
    public static int indexOf(String[] ids, String id) {
        for (int iter = 0; id != null && iter < ids.length; iter++) {
            if (id.equals(ids[iter])) {
                return iter;
            }
        }
        return -1;
    }

    private static final String IDS = "BnPickerIds";

    static String[] distinct(String[] labels) {
        String[] out = new String[labels.length];
        for (int iter = 0; iter < labels.length; iter++) {
            String label = labels[iter];
            int n = 1;
            while (indexOf(out, label) >= 0) {
                n++;
                label = labels[iter] + " (" + n + ")";
            }
            out[iter] = label;
        }
        return out;
    }

    /// Asks a yes-or-no question about money that is about to move; `yes`
    /// is the button that does it.
    public static boolean confirm(String title, String body, String yes) {
        return Dialog.show(Lang.tr(title), body, Lang.tr(yes), Lang.tr("Cancel"));
    }

    /// A short message that goes away by itself.
    public static void toast(String text) {
        ToastBar.showInfoMessage(Lang.tr(text));
    }

    /// When something happened, the way the lists show it.
    public static String when(long millis) {
        if (millis <= 0) {
            return "";
        }
        return new SimpleDateFormat("MMM d, HH:mm").format(new Date(millis));
    }

    /// A day, the way a statement's period shows it.
    public static String day(long millis) {
        return new SimpleDateFormat("MMM d").format(new Date(millis));
    }

    /// Adds a quiet line of detail, wrapping, under a row's title.
    public static void addDetail(Container to, String text) {
        SpanLabel line = new SpanLabel(text);
        line.setTextUIID("BnRowDetail");
        to.add(line);
    }

    /// A rebuildable form: `content` is emptied and refilled on every load.
    /// While the first answer is on its way there is a line saying so; a
    /// failure shows the reason and a way to try again.
    public static void loading(Container content) {
        content.removeAll();
        content.add(muted(Lang.tr("Loading...")));
        content.revalidate();
    }

    /// "Enter an amount, like 25.00.", with the example written the way this
    /// locale writes an amount (25,00 in much of Europe).
    public static String amountHint() {
        return Lang.tr("Enter an amount, like {0}.", Amounts.format(2500L, 2, 2));
    }

    public static void failed(final Container content, String message, final Runnable retry) {
        content.removeAll();
        Label error = error();
        content.add(error);
        showError(error, Lang.tr(message));
        Button again = secondary("Try again");
        again.addActionListener(evt -> retry.run());
        content.add(again);
        content.revalidate();
    }
}
