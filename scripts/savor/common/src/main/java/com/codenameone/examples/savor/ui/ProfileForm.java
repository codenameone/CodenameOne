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
package com.codenameone.examples.savor.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.savor.api.ProfileDto;
import com.codenameone.examples.savor.api.SavedAddressDto;
import com.codenameone.examples.savor.api.UserDto;
import com.codenameone.examples.savor.customer.PlaceSearchForm;
import com.codenameone.examples.savor.map.Locator;
import com.codenameone.examples.savor.net.Account;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.net.Session;
import java.util.List;

/// Who the user is to the service: the name couriers and kitchens see, and the
/// addresses orders go to.
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
        final TextField name = Ui.field(page, "Name", "name",
                "What the kitchen and the courier will call you");
        name.setText(profile == null ? user.displayName() : profile.name());

        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        final Button save = Ui.primary("Save", "submit", null);
        save.addActionListener(e -> {
            save.setEnabled(false);
            error.setText("");
            Account.saveProfile(name.getText().trim(), saved, (status, message) -> {
                save.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            });
        });
        page.add(error).add(save);

        // Saved with the account, so they follow it to the browser and to the
        // next phone; each is saved or removed as it is changed.
        Ui.section(page, "Saved addresses");
        final Container addresses = SettingsForm.group();
        addresses.setName("addresses");
        page.add(addresses);
        page.add(Ui.secondary("Add an address", "addAddress",
                e -> PlaceSearchForm.show(form, Locator.here(), picked ->
                        Api.orders().saveAddress(new SavedAddressDto(null, picked.name(),
                                picked.address(), picked.lat(), picked.lng(), ""),
                                Net.to(added -> load(form, addresses), Ui::fail)))));
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, addresses);
    }

    private static void load(final Form form, final Container addresses) {
        Api.orders().addresses(Net.to(found -> {
            addresses.removeAll();
            List<SavedAddressDto> all = found;
            if (all == null || all.isEmpty()) {
                addresses.add(Ui.text("None yet. Addresses you deliver to can be saved here.",
                        "SvMuted"));
            }
            for (int iter = 0; all != null && iter < all.size(); iter++) {
                final SavedAddressDto address = all.get(iter);
                addresses.add(Ui.row(FontImage.MATERIAL_PLACE, address.label(),
                        address.address(), Ui.flat(FontImage.MATERIAL_DELETE_OUTLINE,
                                "removeAddress-" + iter,
                                e -> Api.orders().removeAddress(address.id(),
                                        Net.to(gone -> load(form, addresses), Ui::fail)))));
            }
            Ui.refresh(form);
        }, Ui::fail));
    }
}
