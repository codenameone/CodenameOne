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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import java.util.Date;
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
    /// or a code on it -- blocks screenshots while it is up.
    public static void show(Form form, boolean sensitive) {
        Protect.screen(sensitive);
        Lock.watch(form);
        form.show();
    }

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

    /// A form with a back arrow that runs `back`, and the app's menu.
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

    /// A picker of `labels`, starting at `initial`.
    public static Picker picker(String[] labels, int initial) {
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings(labels);
        if (labels.length > 0) {
            picker.setSelectedString(labels[Math.max(0, Math.min(initial, labels.length - 1))]);
        }
        picker.setUIID("BnField");
        return picker;
    }

    /// The index `picker` is on, among the `labels` it was made with.
    public static int picked(Picker picker, String[] labels) {
        String selected = picker.getSelectedString();
        for (int iter = 0; iter < labels.length; iter++) {
            if (labels[iter].equals(selected)) {
                return iter;
            }
        }
        return -1;
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
