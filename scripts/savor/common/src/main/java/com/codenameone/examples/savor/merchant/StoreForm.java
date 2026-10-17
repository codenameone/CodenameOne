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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.RestaurantEditDto;
import com.codenameone.examples.savor.customer.Food;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

/// What the restaurant tells its customers about itself: its name, its
/// hours, how long it usually takes, and the smallest order it makes without
/// a surcharge.
final class StoreForm {
    interface Saved {
        void saved(RestaurantDto restaurant);
    }

    private StoreForm() {
    }

    static void show(final Form previous, final RestaurantDto restaurant, final Saved saved) {
        if (restaurant == null) {
            return;
        }
        final Form form = Ui.form(Lang.tr("Store details"), "store-details");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "store-name", "");
        name.setText(restaurant.name());
        final String[] cuisine = {restaurant.cuisine()};
        Ui.section(page, Lang.tr("Cuisine"));
        page.add(Ui.choice("store-cuisine", Food.CUISINES, Food.CUISINES, cuisine[0],
                value -> cuisine[0] = value, 4));
        final TextField description = Ui.field(page, "Description", "store-description", "");
        description.setText(restaurant.description() == null ? "" : restaurant.description());
        final TextField opens = Ui.field(page, "Opens", "store-opens", "11:00");
        opens.setText(hour(restaurant.opensAt()));
        final TextField closes = Ui.field(page, "Closes", "store-closes", "22:00");
        closes.setText(hour(restaurant.closesAt()));
        final TextField prep = Ui.field(page, "Usual time to cook, in minutes", "store-prep", "15");
        prep.setConstraint(TextArea.NUMERIC);
        prep.setText(String.valueOf(restaurant.prepMinutes()));
        final TextField minimum = Ui.field(page, "Smallest order without a surcharge",
                "store-minimum", "10.00");
        minimum.setConstraint(TextArea.DECIMAL);
        minimum.setText(MenuEditor.DishEditor.amount(restaurant.minimumCents()));
        final Label error = new Label("", "SvError");
        page.add(error);
        page.add(Ui.primary("Save", "save-store", e -> {
            int opensAt = minutes(opens.getText());
            int closesAt = minutes(closes.getText());
            int prepMinutes = number(prep.getText());
            if (name.getText().trim().length() == 0 || opensAt < 0 || closesAt < 0
                    || prepMinutes <= 0) {
                error.setText(Lang.tr("Check the name, the hours (as 11:00) and the time to cook"));
                form.revalidate();
                return;
            }
            Api.merchant().save(new RestaurantEditDto(name.getText().trim(), cuisine[0],
                    description.getText().trim(), restaurant.address(), restaurant.lat(),
                    restaurant.lng(), opensAt, closesAt, prepMinutes,
                    MenuEditor.DishEditor.cents(minimum.getText()), restaurant.priceLevel(),
                    restaurant.photoId()), Net.to(stored -> {
                        saved.saved(stored);
                        Ui.say("Saved");
                        previous.showBack();
                    }, Ui::fail));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    static String hour(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        return (hours < 10 ? "0" : "") + hours + ":" + (rest < 10 ? "0" : "") + rest;
    }

    /// `11:30` as 690; -1 for anything that is not a time of day.
    static int minutes(String text) {
        String clean = text.trim();
        int colon = clean.indexOf(':');
        int hours = number(colon < 0 ? clean : clean.substring(0, colon));
        int rest = colon < 0 ? 0 : number(clean.substring(colon + 1));
        if (hours < 0 || hours > 24 || rest < 0 || rest > 59 || hours == 24 && rest > 0) {
            return -1;
        }
        return hours * 60 + rest;
    }

    private static int number(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException wrong) {
            return -1;
        }
    }
}
