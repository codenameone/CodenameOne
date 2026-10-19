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
package com.codenameone.examples.savor.merchant;

import com.codename1.maps.LatLng;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.api.PlaceDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.RestaurantEditDto;
import com.codenameone.examples.savor.customer.CustomerForm;
import com.codenameone.examples.savor.customer.Food;
import com.codenameone.examples.savor.customer.PlaceSearchForm;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.net.Session;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Nav;
import com.codenameone.examples.savor.ui.Photos;
import com.codenameone.examples.savor.ui.Ui;

/// Asking for a restaurant to be listed, from the customer's menu: the
/// details an admin reviews, and then where the application stands, with a
/// cover photo it can be given while it waits. Once approved, the account is
/// the restaurant's merchant and opens in the merchant's mode.
public final class RestaurantApplicationForm {
    private static final String PENDING = "PENDING";
    private static final String REJECTED = "REJECTED";

    private RestaurantApplicationForm() {
    }

    /// Shows the latest application, or the form for a first one.
    public static void show(final Form previous) {
        Api.catalog().application(Net.to(found -> {
            if (found == null || REJECTED.equals(found.status())) {
                edit(previous, found);
            } else {
                status(previous, found);
            }
        }, (status, message) -> {
            if (status == 404) {
                edit(previous, null);
            } else {
                Ui.fail(status, message);
            }
        }));
    }

    private static void edit(final Form previous, final RestaurantDto rejected) {
        final Form form = Ui.form(Lang.tr("List a restaurant"), "restaurant-application");
        Ui.back(form, previous);
        Container page = Ui.page();
        if (rejected != null) {
            page.add(Ui.text(Lang.tr("The last application was turned down: {0}",
                    rejected.reason() == null ? "" : rejected.reason()), "SvError"));
        }
        final TextField name = Ui.field(page, "Name", "apply-name", "");
        final String[] cuisine = {Food.CUISINES[0]};
        Ui.section(page, Lang.tr("Cuisine"));
        page.add(Ui.choice("apply-cuisine", Food.CUISINES, Food.CUISINES, cuisine[0],
                value -> cuisine[0] = value, 4));
        final TextField description = Ui.field(page, "Description", "apply-description", "");
        final PlaceDto[] where = {null};
        final Button address = Ui.link("Choose the address", "apply-address", null);
        Ui.icon(address, FontImage.MATERIAL_PLACE, 3f);
        address.addActionListener(e -> {
            LatLng near = CustomerForm.destination();
            PlaceSearchForm.show(form, near, place -> {
                where[0] = place;
                address.setText(place.address() != null && place.address().length() > 0
                        ? place.address() : place.name());
                form.revalidate();
            });
        });
        Ui.section(page, Lang.tr("Address"));
        page.add(address);
        final TextField opens = Ui.field(page, "Opens", "apply-opens", "11:00");
        final TextField closes = Ui.field(page, "Closes", "apply-closes", "22:00");
        final Label error = new Label("", "SvError");
        page.add(error);
        page.add(Ui.primary("Send for review", "apply-send", e -> {
            int opensAt = StoreForm.minutes(opens.getText().length() == 0 ? "11:00"
                    : opens.getText());
            int closesAt = StoreForm.minutes(closes.getText().length() == 0 ? "22:00"
                    : closes.getText());
            if (name.getText().trim().length() == 0 || where[0] == null || opensAt < 0
                    || closesAt < 0) {
                error.setText(Lang.tr("Check the name, the address and the hours (as 11:00)"));
                form.revalidate();
                return;
            }
            PlaceDto place = where[0];
            String line = place.address() != null && place.address().length() > 0
                    ? place.address() : place.name();
            Api.catalog().apply(new RestaurantEditDto(name.getText().trim(), cuisine[0],
                    description.getText().trim(), line, place.lat(), place.lng(),
                    opensAt, closesAt, 15, 0L, 2, ""), Net.to(sent -> status(previous, sent),
                    Ui::fail));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void status(final Form previous, final RestaurantDto application) {
        final Form form = Ui.form(Lang.tr("List a restaurant"), "restaurant-application");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Ui.label(application.name(), "SvRowTitle"));
        if (PENDING.equals(application.status())) {
            page.add(Ui.text(Lang.tr("Waiting for review. Once it is approved, this account "
                    + "runs the restaurant."), "SvMuted"));
            Ui.section(page, Lang.tr("Cover photo"));
            final Label added = new Label(application.photoId() != null
                    && application.photoId().length() > 0 ? Lang.tr("Photo added") : "",
                    "SvMuted");
            page.add(added);
            Container ways = new Container(new GridLayout(1, 2));
            Button camera = Ui.link("Camera", "apply-camera",
                    e -> Photos.camera((jpeg, taken) -> upload(form, added, jpeg)));
            Ui.icon(camera, FontImage.MATERIAL_PHOTO_CAMERA, 3f);
            Button gallery = Ui.link("Gallery", "apply-gallery",
                    e -> Photos.gallery((jpeg, taken) -> upload(form, added, jpeg)));
            Ui.icon(gallery, FontImage.MATERIAL_PHOTO_LIBRARY, 3f);
            ways.add(camera).add(gallery);
            page.add(ways);
        } else {
            // Approved: the merchant's mode is where it is run from.
            page.add(Ui.text(Lang.tr("Approved. This account runs it now."), "SvMuted"));
            page.add(Ui.primary("Open the restaurant", "apply-open",
                    // Explicitly into the merchant's mode: the saved mode is still
                    // the customer's, so a plain refresh would find nothing changed.
                    e -> Session.refreshUser(() -> Nav.switchTo(Nav.MERCHANT), Ui::fail)));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void upload(final Form form, final Label added, byte[] jpeg) {
        Api.catalog().applicationPhoto(new PhotoDto(null, "image/jpeg",
                Base64.encodeNoNewline(jpeg)), Net.to(saved -> {
                    added.setText(Lang.tr("Photo added"));
                    form.revalidate();
                }, Ui::fail));
    }
}
