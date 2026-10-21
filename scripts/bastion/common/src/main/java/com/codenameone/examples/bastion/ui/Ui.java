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
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
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
}
