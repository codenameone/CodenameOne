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
package com.codenameone.examples.fieldline.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.fieldline.api.ProfileDto;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.net.Account;
import com.codenameone.examples.fieldline.net.Session;

/// The name a customer sees on the sign-off, and the number dispatch calls.
public final class ProfileForm {
    private ProfileForm() {
    }

    /// @param saved where to go once the profile is saved: a screen that shows
    ///     the name is built again there
    public static void show(final Form previous, final Runnable saved) {
        final Form form = Ui.form("Profile", "Profile");
        Ui.back(form, previous);
        UserDto user = Session.user();
        ProfileDto profile = Account.profile();
        Container page = Ui.page();
        page.add(Ui.person(user.displayName(), user.username(), null));
        final TextField name = Ui.field(page, "Name", "name", "As the customer will see it");
        name.setText(profile == null ? user.displayName() : profile.name());
        final TextField phone = Ui.field(page, "Mobile number", "phone", "+1 555 010 0000",
                TextArea.PHONENUMBER);
        phone.setText(profile == null ? user.phone() : profile.phone());
        page.add(Ui.text("Dispatch calls this number when a job changes while you are "
                + "on the road.", "WlMuted"));

        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button save = Ui.primary("Save", "submit", null);
        save.addActionListener(e -> {
            save.setEnabled(false);
            error.setText("");
            Account.saveProfile(name.getText().trim(), phone.getText().trim(), saved,
                    (status, message) -> {
                        save.setEnabled(true);
                        error.setText(message);
                        Ui.refresh(form);
                    });
        });
        page.add(error).add(save);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
