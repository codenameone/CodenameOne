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
package com.codenameone.examples.reel.admin;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.api.BrandingDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;

/// The service's name, tagline and colors, which every client picks up from
/// the configuration on its next start.
final class BrandingForm {
    private BrandingForm() {
    }

    static void show(final Form previous) {
        final Form form = Ui.form("Branding", "Branding");
        Ui.back(form, previous);
        final Container page = Ui.page();
        form.add(BorderLayout.CENTER, page);
        form.show();
        Api.admin().config(Net.to(c -> {
            BrandingDto b = c.branding();
            final TextField name = Ui.field(page, "Service name", "serviceName", "Reel");
            final TextField tagline = Ui.field(page, "Tagline", "tagline", "Watch and learn");
            final TextField primary = Ui.field(page, "Primary color", "primaryColor", "#E50914");
            final TextField accent = Ui.field(page, "Accent color", "accentColor", "#FFFFFF");
            final TextField logo = Ui.field(page, "Logo URL", "logoUrl", "Optional");
            if (b != null) {
                name.setText(b.serviceName());
                tagline.setText(b.tagline());
                primary.setText(b.primaryColor());
                accent.setText(b.accentColor());
                logo.setText(b.logoUrl());
            }
            final SpanLabel error = Ui.text("", "SvError");
            error.setName("error");
            page.add(error);
            page.add(Ui.primary("Save", "saveBranding", e -> {
                if (!color(primary.getText().trim()) || !color(accent.getText().trim())) {
                    error.setText(Lang.tr("Colors are written #RRGGBB"));
                    return;
                }
                Api.admin().setBranding(new BrandingDto(name.getText().trim(), tagline.getText().trim(),
                        primary.getText().trim(), accent.getText().trim(), logo.getText().trim()),
                        Net.to(x -> {
                            Ui.say("Saved");
                            previous.showBack();
                        }, (code, message) -> error.setText(message)));
            }));
            page.revalidate();
        }, Ui.failed(page, () -> show(previous))));
    }

    static boolean color(String s) {
        if (s.length() != 7 || s.charAt(0) != '#') {
            return false;
        }
        for (int iter = 1; iter < 7; iter++) {
            char ch = s.charAt(iter);
            if (!(ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F')) {
                return false;
            }
        }
        return true;
    }
}
