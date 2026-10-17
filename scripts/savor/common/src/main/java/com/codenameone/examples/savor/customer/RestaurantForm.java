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

import com.codename1.maps.LatLng;
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
import com.codenameone.examples.savor.api.MenuSectionDto;
import com.codenameone.examples.savor.api.OrderLineDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.RestaurantSummaryDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Skeleton;
import com.codenameone.examples.savor.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/// A restaurant: its photo, what it costs to have it come, and its menu,
/// section by section, with the cart's button at the foot.
public final class RestaurantForm implements Cart.Listener {
    private final Form form;
    private final Form previous;
    private final Container page = Ui.page();
    private final Button cart = new Button("", "SvCartButton");
    private RestaurantDto restaurant;

    private RestaurantForm(Form previous) {
        this.previous = previous;
        form = Ui.form("", "restaurant");
        Ui.back(form, previous);
        page.add(new Skeleton(5));
        form.add(BorderLayout.CENTER, page);
        cart.setName("restaurant-cart");
        cart.addActionListener(e -> CheckoutForm.show(form));
        form.add(BorderLayout.SOUTH, cart);
        form.addShowListener(e -> {
            Cart.listen(this);
            changed();
        });
    }

    /// Opens the restaurant `id` over `previous`. With `reorder`, the lines of
    /// an earlier order go into the cart as soon as the menu is read: those
    /// dishes still on it, with the options still offered.
    public static void show(Form previous, String id, final List<OrderLineDto> reorder) {
        final RestaurantForm shown = new RestaurantForm(previous);
        shown.form.show();
        LatLng to = CustomerForm.destination();
        Api.catalog().restaurant(id, to.getLatitude(), to.getLongitude(), Net.to(found -> {
            shown.fill(found);
            if (reorder != null) {
                shown.reorder(reorder);
            }
        }, (status, message) -> {
            Ui.fail(status, message);
            previous.showBack();
        }));
    }

    private void fill(RestaurantDto found) {
        restaurant = found;
        form.setTitle(found.name());
        page.removeAll();
        page.add(Food.photo(found.photoId(), found.cuisine(), "SvHeroPhoto", 100f, 42f));
        Container title = new Container(new BorderLayout());
        title.add(BorderLayout.CENTER, Ui.plain(found.name(), "SvRestaurantTitle"));
        title.add(BorderLayout.EAST, favorite(found.favorite()));
        page.add(title);
        page.add(Ui.plain(found.cuisine() + Ui.DOT + Food.priceLevel(found.priceLevel())
                + Ui.DOT + found.address(), "SvMutedSmall"));
        page.add(Ui.plain(Food.meta(found.rating(), found.ratingCount(), found.etaMinutes(),
                found.deliveryFeeCents(), null), "SvMetaLine"));
        if (found.description() != null && found.description().length() > 0) {
            page.add(Ui.text(found.description(), "SvMuted"));
        }
        if (!found.open() || found.paused()) {
            page.add(Ui.banner(FontImage.MATERIAL_SCHEDULE, Lang.tr("Not taking orders now"),
                    Lang.tr("Open {0} to {1}", hour(found.opensAt()), hour(found.closesAt())),
                    "SvBannerIcon"));
        } else if (found.minimumCents() > 0) {
            page.add(Ui.plain(Lang.tr("Orders under {0} pay a small-order fee",
                    Ui.money(found.minimumCents(), null)), "SvMutedSmall"));
        }
        Container chips = new Container(BoxLayout.x());
        chips.setScrollableX(true);
        chips.setScrollVisible(false);
        chips.setUIID("SvSectionChips");
        page.add(chips);
        for (MenuSectionDto section : found.menu()) {
            if (section.dishes().isEmpty()) {
                continue;
            }
            final Label heading = Ui.plain(section.name(), "SvMenuSection");
            Button chip = new Button(section.name(), "SvSectionChip");
            chip.setName("section-" + section.id());
            chip.addActionListener(e -> page.scrollComponentToVisible(heading));
            chips.add(chip);
            page.add(heading);
            for (DishDto dish : section.dishes()) {
                page.add(dishRow(dish));
            }
        }
        form.revalidate();
    }

    private static String hour(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        return (hours < 10 ? "0" : "") + hours + ":" + (rest < 10 ? "0" : "") + rest;
    }

    private Button favorite(final boolean on) {
        final boolean[] liked = {on};
        final Button[] heart = new Button[1];
        heart[0] = Ui.flat(on ? FontImage.MATERIAL_FAVORITE : FontImage.MATERIAL_FAVORITE_BORDER,
                "favorite", e -> {
                    Net.Ok<RestaurantSummaryDto> done = summary -> {
                        liked[0] = summary.favorite();
                        FontImage.setMaterialIcon(heart[0], liked[0] ? FontImage.MATERIAL_FAVORITE
                                : FontImage.MATERIAL_FAVORITE_BORDER);
                        heart[0].repaint();
                    };
                    if (liked[0]) {
                        Api.orders().unfavorite(restaurant.id(), Net.to(done, Ui::fail));
                    } else {
                        Api.orders().favorite(restaurant.id(), Net.to(done, Ui::fail));
                    }
                });
        return heart[0];
    }

    private Container dishRow(final DishDto dish) {
        Container row = new Container(new BorderLayout());
        row.setUIID(dish.available() ? "SvDishRow" : "SvDishRowOff");
        Container words = new Container(BoxLayout.y());
        Container name = new Container(BoxLayout.x());
        name.add(Ui.plain(dish.name(), "SvDishName"));
        if (dish.popular()) {
            name.add(Ui.badge(Lang.tr("Popular"), "SvPopularBadge"));
        }
        words.add(name);
        if (dish.description() != null && dish.description().length() > 0) {
            words.add(Ui.text(dish.description(), "SvMutedSmall"));
        }
        words.add(Ui.plain(dish.available() ? Ui.money(dish.priceCents(), null)
                : Lang.tr("Sold out"), "SvDishPrice"));
        row.add(BorderLayout.CENTER, words);
        if (dish.photoId() != null) {
            row.add(BorderLayout.EAST, Food.photo(dish.photoId(), restaurant.cuisine(),
                    "SvDishPhoto", 20f, 20f));
        }
        if (!dish.available() || !restaurant.open() || restaurant.paused()) {
            return row;
        }
        return Ui.tap(row, "dish-" + dish.id(), e -> DishForm.show(form, restaurant, dish));
    }

    /// Puts an earlier order's lines into the cart, after asking whether a
    /// cart from another restaurant may go.
    private void reorder(final List<OrderLineDto> lines) {
        if (!restaurant.open() || restaurant.paused()) {
            return;
        }
        if (Cart.fromElsewhere(restaurant) && !Dialog.show(Lang.tr("Start a new cart?"),
                Lang.tr("Your cart has food from {0}.", Cart.restaurant().name()),
                Lang.tr("New cart"), Lang.tr("Keep it"))) {
            return;
        }
        int added = 0;
        for (OrderLineDto line : lines) {
            DishDto dish = find(line.dishId());
            if (dish == null || !dish.available()) {
                continue;
            }
            Cart.add(restaurant, dish, options(dish, line.options()), line.quantity());
            added++;
        }
        Ui.say(added == lines.size() ? "Added to your cart"
                : "Some of it is no longer on the menu");
    }

    private DishDto find(String id) {
        for (MenuSectionDto section : restaurant.menu()) {
            for (DishDto dish : section.dishes()) {
                if (dish.id().equals(id)) {
                    return dish;
                }
            }
        }
        return null;
    }

    /// The options of `dish` named in `names`, the order's ", " joined list.
    private static List<DishOptionDto> options(DishDto dish, String names) {
        List<DishOptionDto> picked = new ArrayList<DishOptionDto>();
        if (names == null || names.length() == 0 || dish.options() == null) {
            return picked;
        }
        String joined = ", " + names + ", ";
        for (DishOptionDto option : dish.options()) {
            if (joined.indexOf(", " + option.name() + ", ") >= 0) {
                picked.add(option);
            }
        }
        return picked;
    }

    @Override
    public void changed() {
        int count = Cart.count();
        boolean here = count > 0 && restaurant != null && !Cart.fromElsewhere(restaurant);
        cart.setHidden(!here);
        cart.setVisible(here);
        if (here) {
            cart.setText(Lang.tr("View cart ({0})", String.valueOf(count)) + Ui.DOT
                    + Ui.money(Cart.subtotalCents(), null));
        }
        form.revalidateLater();
    }
}
