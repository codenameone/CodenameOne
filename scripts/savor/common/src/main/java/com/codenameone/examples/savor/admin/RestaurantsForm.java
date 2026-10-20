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
package com.codenameone.examples.savor.admin;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.savor.api.ReasonDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.SuspendDto;
import com.codenameone.examples.savor.customer.Food;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

import java.util.List;

/// The restaurants on the platform: the ones asking to join, to approve or
/// send back, and the ones selling, to suspend or let back.
final class RestaurantsForm {
    private final Panes panes;
    private final Container list = Ui.list("restaurants");
    private String status = "PENDING";

    private RestaurantsForm(Panes panes) {
        this.panes = panes;
        Container top = new Container(BoxLayout.y());
        top.setUIID("SvPage");
        top.add(Ui.choice("restaurant-status",
                new String[] {"Waiting", "Selling", "Suspended", "Sent back"},
                new String[] {"PENDING", "APPROVED", "SUSPENDED", "REJECTED"}, status, value -> {
                    status = value;
                    load();
                }));
        Ui.pull(list, this::load);
        panes.list("Restaurants", "Restaurants", Panes.listed(top, list), this::load);
    }

    static void show(Form previous) {
        show(Panes.phone(previous));
    }

    static void show(Panes panes) {
        new RestaurantsForm(panes);
    }

    private void load() {
        final String asked = status;
        Api.admin().restaurants(asked, Net.to(found -> {
            if (asked.equals(status)) {
                fill(found);
            }
        }, Ui.failed(list, this::load)));
    }

    private void fill(List<RestaurantDto> found) {
        list.removeAll();
        if (found == null || found.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_STOREFRONT, "Nothing here",
                    "PENDING".equals(status) ? "Every restaurant has had its answer." : null));
        }
        for (int iter = 0; found != null && iter < found.size(); iter++) {
            final RestaurantDto restaurant = found.get(iter);
            Container row = Ui.row((char) 0, restaurant.name(),
                    restaurant.cuisine() + Ui.DOT + restaurant.address(),
                    Ui.badge(word(restaurant.status()), badge(restaurant.status())));
            Label icon = new Label("", "SvRowIcon");
            Ui.icon(icon, Food.icon(restaurant.cuisine()), 4f);
            row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(icon));
            final Container picked = row;
            list.add(Ui.tap(row, "restaurant-" + restaurant.id(), e -> {
                panes.picked(picked);
                review(restaurant);
            }));
        }
        panes.refresh();
    }

    static String word(String status) {
        if ("PENDING".equals(status)) {
            return Lang.tr("Waiting");
        }
        if ("APPROVED".equals(status)) {
            return Lang.tr("Selling");
        }
        if ("SUSPENDED".equals(status)) {
            return Lang.tr("Suspended");
        }
        return Lang.tr("Sent back");
    }

    static String badge(String status) {
        return "APPROVED".equals(status) ? "SvBadgeGood"
                : "PENDING".equals(status) ? "SvBadgeWarn" : "SvBadgeBad";
    }

    private void review(final RestaurantDto restaurant) {
        Container page = Ui.page();
        Label state = Ui.badge(word(restaurant.status()), badge(restaurant.status()));
        state.setName("restaurantStatus");
        page.add(Ui.plain(restaurant.name(), "SvRestaurantTitle"));
        page.add(state);
        if (restaurant.reason() != null && restaurant.reason().length() > 0) {
            page.add(Ui.text(restaurant.reason(), "SvNotice"));
        }
        Ui.section(page, "Restaurant");
        page.add(Ui.fact("Cuisine", restaurant.cuisine()));
        page.add(Ui.fact("Address", restaurant.address()));
        page.add(Ui.fact("Hours", hour(restaurant.opensAt()) + " - " + hour(restaurant.closesAt())));
        page.add(Ui.fact("Time to cook", Lang.tr("{0} min", String.valueOf(restaurant.prepMinutes()))));
        page.add(Ui.fact("Rating", restaurant.ratingCount() == 0 ? Lang.tr("New")
                : Ui.tenths(restaurant.rating()) + " (" + restaurant.ratingCount() + ")"));
        if (restaurant.description() != null && restaurant.description().length() > 0) {
            page.add(Ui.text(restaurant.description(), "SvMuted"));
        }
        Net.Ok<RestaurantDto> done = saved -> {
            Ui.say(Lang.tr("{0}: {1}", saved.name(), word(saved.status())));
            panes.home();
        };
        if ("PENDING".equals(restaurant.status())) {
            page.add(Ui.primary("Approve", "approve-restaurant",
                    e -> Api.admin().approveRestaurant(restaurant.id(), Net.to(done, Ui::fail))));
            page.add(Ui.danger("Send back", "reject-restaurant", e -> panes.reason("Send back",
                    Lang.tr("{0} sees what you write, and can apply again once it is put right.",
                            restaurant.name()), "Send back",
                    why -> Api.admin().rejectRestaurant(restaurant.id(), new ReasonDto(why),
                            Net.to(done, Ui::fail)))));
        } else if ("APPROVED".equals(restaurant.status())) {
            page.add(Ui.danger("Suspend", "suspend-restaurant", e -> panes.reason("Suspend",
                    Lang.tr("{0} stops appearing to customers until it is let back.",
                            restaurant.name()), "Suspend",
                    why -> Api.admin().suspendRestaurant(restaurant.id(), new SuspendDto(true, why),
                            Net.to(done, Ui::fail)))));
        } else if ("SUSPENDED".equals(restaurant.status())) {
            page.add(Ui.primary("Let back", "unsuspend-restaurant",
                    e -> Api.admin().suspendRestaurant(restaurant.id(), new SuspendDto(false, null),
                            Net.to(done, Ui::fail))));
        }
        panes.detail(restaurant.name(), "RestaurantReview", page);
    }

    private static String hour(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        return (hours < 10 ? "0" : "") + hours + ":" + (rest < 10 ? "0" : "") + rest;
    }
}
