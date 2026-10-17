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

import com.codename1.components.Switch;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.savor.api.DishDto;
import com.codenameone.examples.savor.api.DishOptionDto;
import com.codenameone.examples.savor.api.MenuSectionDto;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.customer.Food;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Photos;
import com.codenameone.examples.savor.ui.Skeleton;
import com.codenameone.examples.savor.ui.Ui;

import java.util.ArrayList;

/// The menu, as the kitchen keeps it: sections of dishes, each of which can
/// be sold out for the evening without being taken off, edited, or given a
/// photo.
final class MenuEditor {
    private final Form form;
    private final Container page = Ui.page();
    private RestaurantDto restaurant;

    MenuEditor(Form form) {
        this.form = form;
        page.setName("menu-editor");
        page.add(new Skeleton(5));
        load();
    }

    Container page() {
        return page;
    }

    private void load() {
        Api.merchant().restaurant(Net.to(this::fill, Ui.failed(page, this::load)));
    }

    private void fill(RestaurantDto found) {
        restaurant = found;
        page.removeAll();
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, Ui.plain(Lang.tr("Menu"), "SvRestaurantTitle"));
        top.add(BorderLayout.EAST, Ui.link("Add section", "add-section", e -> section(null)));
        page.add(top);
        if (found.menu().isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_RESTAURANT_MENU, Lang.tr("No dishes yet"),
                    Lang.tr("Add a section, then the dishes in it.")));
        }
        for (final MenuSectionDto section : found.menu()) {
            Container heading = new Container(new BorderLayout());
            heading.add(BorderLayout.CENTER, Ui.plain(section.name(), "SvMenuSection"));
            Container actions = new Container(BoxLayout.x());
            actions.add(Ui.flat(FontImage.MATERIAL_EDIT, "edit-section-" + section.id(),
                    e -> section(section)));
            actions.add(Ui.flat(FontImage.MATERIAL_ADD, "add-dish-" + section.id(),
                    e -> DishEditor.show(form, restaurant, section.id(), null, this::load)));
            heading.add(BorderLayout.EAST, actions);
            page.add(heading);
            for (final DishDto dish : section.dishes()) {
                Switch on = Ui.toggle("available-" + dish.id(), dish.available(),
                        value -> Api.merchant().available(dish.id(), value, Net.to(saved -> {
                            Ui.say(value ? "Back on the menu" : "Sold out for now");
                        }, (status, message) -> {
                            Ui.fail(status, message);
                            load();
                        })));
                Container row = Ui.row((char) 0, dish.name(), Ui.money(dish.priceCents(), null)
                        + (dish.available() ? "" : Ui.DOT + Lang.tr("Sold out")), on);
                page.add(Ui.tap(row, "dish-" + dish.id(),
                        e -> DishEditor.show(form, restaurant, section.id(), dish, this::load)));
            }
        }
        page.revalidate();
    }

    /// Asks for a section's name: a new one, or `section` renamed or removed.
    private void section(final MenuSectionDto section) {
        TextField name = new TextField(section == null ? "" : section.name(),
                Lang.tr("Section name"));
        name.setName("section-name");
        Command save = new Command(Lang.tr(section == null ? "Add" : "Save"));
        Command remove = new Command(Lang.tr("Remove"));
        Command cancel = new Command(Lang.tr("Cancel"));
        Command chosen = section == null
                ? Dialog.show(Lang.tr("New section"), name, save, cancel)
                : Dialog.show(Lang.tr("Section"), name, save, remove, cancel);
        String text = name.getText().trim();
        if (chosen == save && text.length() > 0) {
            if (section == null) {
                Api.merchant().addSection(new MenuSectionDto(null, text,
                        new ArrayList<DishDto>()), Net.to(saved -> load(), Ui::fail));
            } else {
                Api.merchant().saveSection(section.id(), new MenuSectionDto(section.id(), text,
                        section.dishes()), Net.to(saved -> load(), Ui::fail));
            }
        } else if (chosen == remove) {
            Api.merchant().removeSection(section.id(), Net.to(gone -> load(), Ui::fail));
        }
    }

    /// One dish, edited or new.
    static final class DishEditor {
        private DishEditor() {
        }

        static void show(final Form previous, final RestaurantDto restaurant,
                final String sectionId, final DishDto dish, final Runnable saved) {
            final Form form = Ui.form(dish == null ? Lang.tr("New dish") : dish.name(),
                    "dish-editor");
            Ui.back(form, previous);
            Container page = Ui.page();
            final String[] photoId = {dish == null ? null : dish.photoId()};
            final Container photo = new Container(new BorderLayout());
            photo.add(BorderLayout.CENTER, Food.photo(photoId[0], restaurant.cuisine(),
                    "SvHeroPhoto", 100f, 36f));
            page.add(photo);
            Container pick = new Container(BoxLayout.x());
            Photos.Taken taken = (jpeg, picture) -> Api.merchant().upload(new PhotoDto(null,
                    "image/jpeg", Base64.encodeNoNewline(jpeg)), Net.to(stored -> {
                        photoId[0] = stored.id();
                        photo.removeAll();
                        photo.add(BorderLayout.CENTER, Food.photo(stored.id(), restaurant.cuisine(),
                                "SvHeroPhoto", 100f, 36f));
                        form.revalidate();
                    }, Ui::fail));
            pick.add(Ui.link("Take photo", "dish-camera", e -> Photos.camera(taken)));
            pick.add(Ui.link("Choose photo", "dish-gallery", e -> Photos.gallery(taken)));
            page.add(pick);
            final TextField name = Ui.field(page, "Name", "dish-name", "Margherita");
            final TextField price = Ui.field(page, "Price", "dish-price", "12.50");
            price.setConstraint(TextArea.DECIMAL);
            final TextField description = Ui.field(page, "Description", "dish-description",
                    "Tomato, mozzarella, basil");
            final boolean[] popular = {dish != null && dish.popular()};
            page.add(Ui.row(FontImage.MATERIAL_STAR, Lang.tr("Popular"),
                    Lang.tr("Marked on the menu"), Ui.toggle("dish-popular", popular[0],
                            value -> popular[0] = value)));
            if (dish != null) {
                name.setText(dish.name());
                price.setText(amount(dish.priceCents()));
                description.setText(dish.description() == null ? "" : dish.description());
            }
            final Label error = new Label("", "SvError");
            page.add(error);
            page.add(Ui.primary("Save", "save-dish", e -> {
                long cents = cents(price.getText());
                if (name.getText().trim().length() == 0 || cents <= 0) {
                    error.setText(Lang.tr("A dish needs a name and a price"));
                    form.revalidate();
                    return;
                }
                DishDto edited = new DishDto(dish == null ? null : dish.id(), sectionId,
                        name.getText().trim(), description.getText().trim(), cents, photoId[0],
                        dish == null || dish.available(), popular[0],
                        dish == null ? new ArrayList<DishOptionDto>() : dish.options());
                Net.Ok<DishDto> done = stored -> {
                    saved.run();
                    previous.showBack();
                };
                if (dish == null) {
                    Api.merchant().addDish(edited, Net.to(done, Ui::fail));
                } else {
                    Api.merchant().saveDish(dish.id(), edited, Net.to(done, Ui::fail));
                }
            }));
            if (dish != null) {
                page.add(Ui.danger("Remove from menu", "remove-dish", e -> {
                    if (Dialog.show(Lang.tr("Remove {0}?", dish.name()), null,
                            Lang.tr("Remove"), Lang.tr("Keep it"))) {
                        Api.merchant().removeDish(dish.id(), Net.to(gone -> {
                            saved.run();
                            previous.showBack();
                        }, Ui::fail));
                    }
                }));
            }
            form.add(BorderLayout.CENTER, page);
            form.show();
        }

        /// 1250 cents as `12.50`, the way the price field takes it back.
        static String amount(long cents) {
            long part = cents % 100;
            return cents / 100 + "." + (part < 10 ? "0" : "") + part;
        }

        /// `12.5` or `12,50` as 1250 cents; 0 for anything else.
        static long cents(String text) {
            String clean = text.trim().replace(',', '.');
            int dot = clean.indexOf('.');
            try {
                if (dot < 0) {
                    return Long.parseLong(clean) * 100;
                }
                String part = (clean.substring(dot + 1) + "00").substring(0, 2);
                return Long.parseLong(clean.substring(0, dot).length() == 0 ? "0"
                        : clean.substring(0, dot)) * 100 + Long.parseLong(part);
            } catch (NumberFormatException wrong) {
                return 0;
            }
        }
    }
}
