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
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.savor.Prefs;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.PlaceDto;
import com.codenameone.examples.savor.api.RestaurantSummaryDto;
import com.codenameone.examples.savor.api.UserDto;
import com.codenameone.examples.savor.live.LiveChannel;
import com.codenameone.examples.savor.map.Locator;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.net.Session;
import com.codenameone.examples.savor.pay.ReceiptForm;
import com.codenameone.examples.savor.pay.WalletForm;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Menu;
import com.codenameone.examples.savor.ui.Nav;
import com.codenameone.examples.savor.ui.Skeleton;
import com.codenameone.examples.savor.ui.ProfileForm;
import com.codenameone.examples.savor.ui.Ui;

import java.util.List;

/// The customer's home: where the food is coming to, what is near, and what
/// is already on its way.
///
/// Four tabs share the screen -- the restaurants near the address, a search,
/// the orders, and the favorites -- under one "Deliver to" header, with the
/// cart's button over all of them while something is in it.
public final class CustomerForm extends LiveChannel.Adapter implements Nav.Home, Cart.Listener {
    private static final Net.Failed QUIET = (status, message) -> { };
    /// How often an order on its way is asked about while the live channel is
    /// down. With it up, a frame says when to ask.
    private static final int POLL_MILLIS = 15000;
    private static final String[] SORTS = {"distance", "rating", "fee", "time"};
    private static final String[] SORT_NAMES = {"Nearest", "Rating", "Fee", "Fastest"};

    private final Form form = Ui.home("Customer");
    private final Menu menu = new Menu(form, Nav.CUSTOMER);
    private final Container body = new Container(new BorderLayout());
    private final Container tabs = new Container(new GridLayout(1, 4));
    private final Button cart = new Button("", "SvCartButton");
    private final Label deliverTo = new Label("", "SvDeliverTo");
    private String cuisine;
    private String sort = SORTS[0];
    private String query = "";
    private int tab;
    private Container restaurants;
    private Container active;

    public CustomerForm() {
        menu.item(FontImage.MATERIAL_HOME, "Restaurants", "home", () -> show(0));
        menu.item(FontImage.MATERIAL_RECEIPT_LONG, "Your orders", "history", () -> show(2));
        menu.item(FontImage.MATERIAL_FAVORITE, "Favorites", "favorites", () -> show(3));
        menu.item(FontImage.MATERIAL_ACCOUNT_BALANCE_WALLET, "Wallet", "wallet",
                () -> WalletForm.show(form));
        menu.item(FontImage.MATERIAL_PERSON, "Profile and addresses", "profile",
                () -> ProfileForm.show(form, () -> form.showBack()));
        UserDto user = Session.user();
        if (user != null && !user.courier()) {
            menu.item(FontImage.MATERIAL_TWO_WHEELER, "Deliver with Savor", "deliver",
                    () -> Nav.switchTo(Nav.COURIER));
        }
        // The header and the tab bar each reach into the strip the device
        // keeps clear at their edge, padded by it, so the strip wears their
        // colour; padding the whole screen left both strips to whatever lay
        // behind it, and the list showed through under the tabs.
        Container screen = new Container(new BorderLayout());
        Container top = header();
        top.setSafeArea(true);
        screen.add(BorderLayout.NORTH, top);
        screen.add(BorderLayout.CENTER, body);
        Container south = new Container(BoxLayout.y());
        cart.setName("cart");
        cart.addActionListener(e -> CheckoutForm.show(form));
        south.add(cart);
        tabs.setUIID("SvTabBar");
        tab(FontImage.MATERIAL_HOME, "Home", "tab-home", 0);
        tab(FontImage.MATERIAL_SEARCH, "Search", "tab-search", 1);
        tab(FontImage.MATERIAL_RECEIPT_LONG, "Orders", "tab-orders", 2);
        tab(FontImage.MATERIAL_FAVORITE_BORDER, "Favorites", "tab-favorites", 3);
        tabs.setSafeArea(true);
        south.add(tabs);
        screen.add(BorderLayout.SOUTH, south);
        form.add(screen);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            Cart.listen(this);
            changed();
            where();
            show(tab);
        });
        UITimer.timer(POLL_MILLIS, true, form, () -> {
            if (!LiveChannel.connected() && tab == 0) {
                loadActive();
            }
        });
    }

    @Override
    public void start() {
        if (Prefs.deliverTo() != null) {
            return;
        }
        // No address chosen yet: the one the phone is at, in words, once the
        // server has put a name on it.
        final LatLng here = Locator.here();
        Api.geo().reverse(here.getLatitude(), here.getLongitude(), Net.to(place -> {
            if (place != null && Prefs.deliverTo() == null) {
                Prefs.setDeliverTo(new PlaceDto(place.name(), place.address(),
                        here.getLatitude(), here.getLongitude()));
                where();
            }
        }, QUIET));
    }

    @Override
    public Form form() {
        return form;
    }

    /// Where the food goes: the chosen address, or where the phone is.
    public static LatLng destination() {
        PlaceDto to = Prefs.deliverTo();
        return to == null ? Locator.here() : new LatLng(to.lat(), to.lng());
    }

    /// The chosen address in words, or null with none chosen.
    public static String destinationName() {
        PlaceDto to = Prefs.deliverTo();
        return to == null ? null : to.address() != null && to.address().length() > 0
                ? to.address() : to.name();
    }

    private Container header() {
        Container header = new Container(new BorderLayout());
        header.setUIID("SvHomeHeader");
        header.add(BorderLayout.WEST, menu.flatButton());
        Container words = new Container(BoxLayout.y());
        words.add(Ui.plain(Lang.tr("Deliver to"), "SvMutedSmall"));
        Ui.icon(deliverTo, FontImage.MATERIAL_EXPAND_MORE, 3.2f);
        deliverTo.setTextPosition(Component.LEFT);
        words.add(deliverTo);
        header.add(BorderLayout.CENTER, Ui.tap(words, "deliver-to",
                e -> PlaceSearchForm.show(form, destination(), picked -> {
                    Prefs.setDeliverTo(picked);
                    form.showBack();
                })));
        return header;
    }

    private void where() {
        String name = destinationName();
        deliverTo.setText(name == null ? Lang.tr("Current location") : name);
        Ui.relayout(deliverTo.getParent());
    }

    private void tab(char icon, String text, String name, final int index) {
        Button button = new Button(Lang.tr(text), "SvTab");
        button.setName(name);
        FontImage.setMaterialIcon(button, icon, 3.6f);
        button.setTextPosition(Component.BOTTOM);
        button.addActionListener(e -> show(index));
        tabs.add(button);
    }

    private void show(int index) {
        tab = index;
        for (int iter = 0; iter < tabs.getComponentCount(); iter++) {
            tabs.getComponentAt(iter).setUIID(iter == index ? "SvTabSelected" : "SvTab");
        }
        body.removeAll();
        if (index == 0) {
            body.add(BorderLayout.CENTER, home());
        } else if (index == 1) {
            body.add(BorderLayout.CENTER, search());
        } else if (index == 2) {
            body.add(BorderLayout.CENTER, orders());
        } else {
            body.add(BorderLayout.CENTER, favorites());
        }
        form.revalidate();
    }

    /// Good morning, afternoon or evening, by the phone's clock.
    static String greeting(String name) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String part = hour < 5 ? "Good evening" : hour < 12 ? "Good morning"
                : hour < 18 ? "Good afternoon" : "Good evening";
        return name == null || name.length() == 0 ? Lang.tr(part)
                : Lang.tr(part) + ", " + firstName(name);
    }

    private static String firstName(String name) {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    private Container home() {
        Container page = Ui.page();
        page.setName("restaurants");
        UserDto user = Session.user();
        page.add(Ui.plain(greeting(user == null ? null : user.displayName()), "SvGreeting"));
        page.add(Ui.plain(Lang.tr("What would you like to eat?"), "SvMuted"));
        active = new Container(BoxLayout.y());
        page.add(active);
        page.add(cuisines());
        page.add(Ui.choice("sort", tr(SORT_NAMES), SORTS, sort, picked -> {
            sort = picked;
            loadRestaurants();
        }));
        restaurants = new Container(BoxLayout.y());
        restaurants.add(new Skeleton(3));
        page.add(restaurants);
        Ui.pull(page, () -> {
            loadActive();
            loadRestaurants();
        });
        loadActive();
        loadRestaurants();
        return page;
    }

    private static String[] tr(String[] texts) {
        String[] done = new String[texts.length];
        for (int iter = 0; iter < texts.length; iter++) {
            done[iter] = Lang.tr(texts[iter]);
        }
        return done;
    }

    private Container cuisines() {
        Container row = new Container(BoxLayout.x());
        row.setUIID("SvCuisineRow");
        row.setScrollableX(true);
        row.setScrollVisible(false);
        row.setName("cuisines");
        for (int iter = 0; iter < Food.CUISINES.length; iter++) {
            final String name = Food.CUISINES[iter];
            boolean picked = name.equals(cuisine);
            Button chip = new Button(Lang.tr(name), picked ? "SvCuisineSelected" : "SvCuisine");
            chip.setName("cuisine-" + name);
            FontImage.setMaterialIcon(chip, Food.icon(name), 5f);
            chip.setTextPosition(Component.BOTTOM);
            chip.addActionListener(e -> {
                cuisine = name.equals(cuisine) ? null : name;
                show(0);
            });
            row.add(chip);
        }
        return row;
    }

    private void loadActive() {
        if (active == null) {
            return;
        }
        final Container into = active;
        Api.orders().active(Net.to(found -> {
            into.removeAll();
            for (final OrderDto order : found) {
                Container banner = Ui.banner(FontImage.MATERIAL_DELIVERY_DINING,
                        order.restaurantName(), Food.state(order.state()), "SvBannerIcon");
                banner.setUIID("SvActiveOrder");
                into.add(Ui.tap(banner, "active-" + order.id(),
                        e -> TrackForm.show(form, order.id())));
            }
            form.revalidateLater();
        }, QUIET));
    }

    private void loadRestaurants() {
        final Container into = restaurants;
        LatLng to = destination();
        Api.catalog().restaurants(to.getLatitude(), to.getLongitude(), cuisine, null, sort,
                Net.to(found -> fill(into, found, cuisine == null
                        ? Lang.tr("Nothing delivers here yet")
                        : Lang.tr("No {0} near you", Lang.tr(cuisine))),
                        Ui.failed(into, this::loadRestaurants)));
    }

    private void fill(Container into, List<RestaurantSummaryDto> found, String none) {
        into.removeAll();
        if (found.isEmpty()) {
            into.add(Ui.empty(FontImage.MATERIAL_STOREFRONT, none, null));
        }
        for (final RestaurantSummaryDto restaurant : found) {
            into.add(Food.card(restaurant, null, "restaurant-" + restaurant.id(),
                    e -> RestaurantForm.show(form, restaurant.id(), null)));
        }
        Ui.refresh(form);
    }

    private Container search() {
        Container screen = new Container(new BorderLayout());
        final TextField field = new TextField(query, Lang.tr("Restaurants, dishes, cuisines"));
        field.setName("search");
        field.setUIID("SvSearchField");
        FontImage.setMaterialIcon(field.getHintLabel(), FontImage.MATERIAL_SEARCH);
        final Container results = Ui.list("search-results");
        results.removeAll();
        results.add(Ui.empty(FontImage.MATERIAL_SEARCH, Lang.tr("Find something to eat"), null));
        Runnable run = () -> {
            query = field.getText().trim();
            if (query.length() < 2) {
                return;
            }
            LatLng to = destination();
            results.removeAll();
            results.add(new Skeleton(3));
            results.revalidate();
            // An answer for an earlier term that arrives late is dropped: the
            // results are always for what is in the field.
            final String asked = query;
            Api.catalog().restaurants(to.getLatitude(), to.getLongitude(), null, asked, sort,
                    Net.to(found -> {
                        if (asked.equals(query)) {
                            fill(results, found, Lang.tr("Nothing matches \"{0}\"", asked));
                        }
                    }, (status, message) -> {
                        if (asked.equals(query)) {
                            Ui.failed(results, null).failed(status, message);
                        }
                    }));
        };
        field.setDoneListener(e -> run.run());
        field.addDataChangedListener((type, index) -> UITimer.timer(400, false, form, () -> {
            if (!field.getText().trim().equals(query)) {
                run.run();
            }
        }));
        Container top = new Container(new BorderLayout());
        top.setUIID("SvSearchBar");
        top.add(BorderLayout.CENTER, field);
        screen.add(BorderLayout.NORTH, top);
        screen.add(BorderLayout.CENTER, results);
        if (query.length() >= 2) {
            run.run();
        }
        return screen;
    }

    private Container orders() {
        final Container list = Ui.list("orders");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.orders().history(Net.to(found -> {
            list.removeAll();
            if (found.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, Lang.tr("No orders yet"),
                        Lang.tr("What you order shows here.")));
            }
            for (final OrderDto order : found) {
                list.add(orderRow(order));
            }
            Ui.refresh(form);
        }, Ui.failed(list, () -> load[0].run())));
        Ui.pull(list, load[0]);
        load[0].run();
        return list;
    }

    private Container orderRow(final OrderDto order) {
        Container row = new Container(new BorderLayout());
        row.setUIID("SvOrderRow");
        row.add(BorderLayout.WEST, Food.photo(null, null, "SvOrderThumb", 12f, 12f));
        Container words = new Container(BoxLayout.y());
        words.add(Ui.plain(order.restaurantName(), "SvRowTitle"));
        words.add(Ui.plain(Ui.money(order.totalCents(), order.currency()) + Ui.DOT
                + Ui.day(order.placedAt()), "SvMutedSmall"));
        words.add(Ui.plain(Food.state(order.state()), "SvMutedSmall"));
        row.add(BorderLayout.CENTER, words);
        boolean coming = Food.active(order.state());
        if (coming) {
            return Ui.tap(row, "order-" + order.id(), e -> TrackForm.show(form, order.id()));
        }
        Button again = Ui.link("Reorder", "reorder-" + order.id(),
                e -> RestaurantForm.show(form, order.restaurantId(), order.lines()));
        row.add(BorderLayout.EAST, again);
        return Ui.tap(row, "order-" + order.id(), e -> {
            if (order.state() == com.codenameone.examples.savor.api.OrderState.DELIVERED) {
                ReceiptForm.open(form, order.id());
            } else {
                TrackForm.show(form, order.id());
            }
        });
    }

    private Container favorites() {
        final Container list = Ui.list("favorites");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> {
            LatLng to = destination();
            Api.orders().favorites(to.getLatitude(), to.getLongitude(), Net.to(found -> fill(list,
                    found, Lang.tr("Tap the heart on a restaurant to keep it here")),
                    Ui.failed(list, () -> load[0].run())));
        };
        Ui.pull(list, load[0]);
        load[0].run();
        return list;
    }

    @Override
    public void changed() {
        int count = Cart.count();
        cart.setHidden(count == 0);
        cart.setVisible(count > 0);
        if (count > 0) {
            cart.setText(Lang.tr("View cart ({0})", String.valueOf(count)) + Ui.DOT
                    + Ui.money(Cart.subtotalCents(), null));
        }
        form.revalidateLater();
    }

    @Override
    public void orderChanged(String id, String state) {
        if (CN.getCurrentForm() == form && tab == 0) {
            loadActive();
        }
    }
}
