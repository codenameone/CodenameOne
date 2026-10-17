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
package com.codenameone.examples.savor.customer;

import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.savor.api.DishDto;
import com.codenameone.examples.savor.api.DishOptionDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/// One dish: what comes with it, how many, and the bar that adds it to the
/// order at the price it comes to.
public final class DishForm {
    private final DishDto dish;
    private final List<DishOptionDto> picked = new ArrayList<DishOptionDto>();
    private final Label count = new Label("1", "SvQuantity");
    private final Button add;
    private int quantity = 1;

    private DishForm(final Form previous, final RestaurantDto restaurant, DishDto dish) {
        this.dish = dish;
        final Form form = Ui.form(dish.name(), "dish");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Food.photo(dish.photoId(), restaurant.cuisine(), "SvHeroPhoto", 100f, 42f));
        page.add(Ui.plain(dish.name(), "SvRestaurantTitle"));
        page.add(Ui.plain(Ui.money(dish.priceCents(), null), "SvDishPrice"));
        if (dish.description() != null && dish.description().length() > 0) {
            page.add(Ui.text(dish.description(), "SvMuted"));
        }
        if (dish.options() != null && !dish.options().isEmpty()) {
            Ui.section(page, Lang.tr("Add extras"));
            for (final DishOptionDto option : dish.options()) {
                Switch on = Ui.toggle("option-" + option.id(), false, value -> {
                    if (value) {
                        picked.add(option);
                    } else {
                        picked.remove(option);
                    }
                    price();
                });
                page.add(Ui.row(FontImage.MATERIAL_ADD_CIRCLE_OUTLINE, option.name(),
                        option.priceCents() == 0 ? null
                                : "+" + Ui.money(option.priceCents(), null), on));
            }
        }
        form.add(BorderLayout.CENTER, page);

        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvAddBar");
        bar.setSafeArea(true);
        Container stepper = new Container(BoxLayout.x());
        stepper.setUIID("SvStepper");
        stepper.add(Ui.flat(FontImage.MATERIAL_REMOVE, "less", e -> step(-1)));
        stepper.add(count);
        stepper.add(Ui.flat(FontImage.MATERIAL_ADD, "more", e -> step(1)));
        bar.add(BorderLayout.WEST, stepper);
        add = Ui.primary("", "add-to-order", e -> {
            if (Cart.fromElsewhere(restaurant) && !Dialog.show(Lang.tr("Start a new cart?"),
                    Lang.tr("Your cart has food from {0}.", Cart.restaurant().name()),
                    Lang.tr("New cart"), Lang.tr("Keep it"))) {
                return;
            }
            Cart.add(restaurant, this.dish, new ArrayList<DishOptionDto>(picked), quantity);
            previous.showBack();
        });
        bar.add(BorderLayout.CENTER, add);
        form.add(BorderLayout.SOUTH, bar);
        price();
        form.show();
    }

    /// Opens `dish` of `restaurant` over `previous`.
    public static void show(Form previous, RestaurantDto restaurant, DishDto dish) {
        new DishForm(previous, restaurant, dish);
    }

    private void step(int by) {
        quantity = Math.max(1, Math.min(20, quantity + by));
        count.setText(String.valueOf(quantity));
        price();
    }

    private void price() {
        long unit = dish.priceCents();
        for (int iter = 0; iter < picked.size(); iter++) {
            unit += picked.get(iter).priceCents();
        }
        add.setText(Lang.tr("Add to order") + Ui.DOT + Ui.money(unit * quantity, null));
        Ui.relayout(add.getParent());
    }
}
