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
package com.codenameone.examples.parley.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.parley.api.ProfileDto;
import com.codenameone.examples.parley.api.UserDto;
import com.codenameone.examples.parley.net.Account;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.net.Session;
import java.util.List;

/// Who the user is to the people they talk to: a name and a line about them.
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

        Ui.section(page, "About you");
        final TextField name = Ui.field(page, "Name", "name", "What your contacts will see");
        name.setText(profile == null ? user.displayName() : profile.name());
        final TextField about = Ui.field(page, "About", "about", "Available");
        about.setText(profile == null ? (user.about() == null ? "" : user.about())
                : (profile.about() == null ? "" : profile.about()));

        final SpanLabel error = Ui.text("", "PlError");
        error.setName("error");
        final Button save = Ui.primary("Save", "submit", null);
        save.addActionListener(e -> {
            save.setEnabled(false);
            error.setText("");
            Account.saveProfile(name.getText().trim(), about.getText().trim(), saved,
                    (status, message) -> {
                        save.setEnabled(true);
                        error.setText(message);
                        Ui.refresh(form);
                    });
        });
        page.add(error).add(save);
        page.add(Ui.text("Your name and photo are seen by the people you talk to. Who sees "
                + "your photo and when you were last online is up to you, in Privacy.",
                "PlMuted"));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
