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
        band.add(new Label(Lang.tr(heading), "BnBrandTitle"));
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

    /// Buttons side by side, sharing the width.
    public static Container across(Component... parts) {
        Container row = new Container(new GridLayout(1, parts.length));
        for (Component part : parts) {
            if (part instanceof Label) {
                // Three buttons share a phone's width: they give up most of
                // their side padding, and a longer label (or a longer
                // translation) shrinks a little rather than end in "...".
                Label label = (Label) part;
                label.getAllStyles().setPaddingLeft(1f);
                label.getAllStyles().setPaddingRight(1f);
                label.setAutoSizeMode(true);
                label.setMaxAutoSize(2.9f);
                label.setMinAutoSize(2.2f);
            }
        }
        row.addAll(parts);
        return row;
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
