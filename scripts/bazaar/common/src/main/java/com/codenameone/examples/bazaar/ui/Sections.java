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
package com.codenameone.examples.bazaar.ui;

import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/// The places of each mode, in the order they are listed.
///
/// Every mode starts with placeholders. A feature screen takes its place by
/// registering content under the same id, before the home screen is built:
///
/// ```java
/// Sections.content(Nav.MERCHANT, Sections.PRODUCTS, ProductsPane::new);
/// ```
///
/// [Shell] reads the list when it builds a home screen: a bottom tab bar on a
/// phone, a sidebar on a wide screen.
public final class Sections {
    public static final String HOME = "home";
    public static final String SEARCH = "search";
    public static final String CART = "cart";
    public static final String ORDERS = "orders";
    public static final String ACCOUNT = "account";
    public static final String DASHBOARD = "dashboard";
    public static final String PRODUCTS = "products";
    public static final String INVENTORY = "inventory";
    public static final String CUSTOMERS = "customers";
    public static final String DISCOUNTS = "discounts";
    public static final String ANALYTICS = "analytics";
    public static final String AUTOMATIONS = "automations";
    public static final String SETTINGS = "settings";
    public static final String POS = "pos";
    public static final String REGISTER = "register";
    public static final String OVERVIEW = "overview";
    public static final String STORES = "stores";
    public static final String USERS = "users";
    public static final String PAYMENTS = "payments";

    /// Sidebar headings of the merchant's sections.
    public static final String ORDERS_GROUP = "Orders";
    public static final String PRODUCTS_GROUP = "Products";
    public static final String CUSTOMERS_GROUP = "Customers";
    public static final String MARKETING_GROUP = "Marketing";
    public static final String ANALYTICS_GROUP = "Analytics";
    public static final String CHANNELS_GROUP = "Sales channels";
    public static final String SETTINGS_GROUP = "Settings";

    private static final Map<String, List<Section>> BY_MODE = new HashMap<String, List<Section>>();

    static {
        add(Nav.SHOP, HOME, "Home", "", FontImage.MATERIAL_STOREFRONT);
        add(Nav.SHOP, SEARCH, "Search", "", FontImage.MATERIAL_SEARCH);
        add(Nav.SHOP, CART, "Cart", "", FontImage.MATERIAL_SHOPPING_CART);
        add(Nav.SHOP, ORDERS, "Orders", "", FontImage.MATERIAL_RECEIPT_LONG);
        add(Nav.SHOP, ACCOUNT, "Account", "", FontImage.MATERIAL_PERSON);

        // The merchant's sidebar groups, in Shopify's order: what happens
        // every day first, then catalog, people, growth, numbers, channels
        // and setup (MerchantSections.ORDER). The first of a group leads it.
        add(Nav.MERCHANT, DASHBOARD, "Home", "", FontImage.MATERIAL_HOME);
        add(Nav.MERCHANT, ORDERS, "Orders", ORDERS_GROUP, FontImage.MATERIAL_RECEIPT_LONG);
        add(Nav.MERCHANT, PRODUCTS, "Products", PRODUCTS_GROUP, FontImage.MATERIAL_SELL);
        add(Nav.MERCHANT, INVENTORY, "Inventory", PRODUCTS_GROUP, FontImage.MATERIAL_INVENTORY_2);
        add(Nav.MERCHANT, CUSTOMERS, "Customers", CUSTOMERS_GROUP, FontImage.MATERIAL_GROUP);
        add(Nav.MERCHANT, DISCOUNTS, "Discounts", MARKETING_GROUP, FontImage.MATERIAL_LOCAL_OFFER);
        add(Nav.MERCHANT, ANALYTICS, "Analytics", ANALYTICS_GROUP, FontImage.MATERIAL_BAR_CHART);
        add(Nav.MERCHANT, AUTOMATIONS, "Automations", MARKETING_GROUP,
                FontImage.MATERIAL_ACCOUNT_TREE);
        add(Nav.MERCHANT, SETTINGS, "Store settings", SETTINGS_GROUP, FontImage.MATERIAL_TUNE);
        add(Nav.MERCHANT, POS, "Point of sale", CHANNELS_GROUP, FontImage.MATERIAL_POINT_OF_SALE);

        add(Nav.POS, REGISTER, "Register", "", FontImage.MATERIAL_POINT_OF_SALE);

        add(Nav.ADMIN, OVERVIEW, "Overview", "", FontImage.MATERIAL_INSIGHTS);
        add(Nav.ADMIN, STORES, "Stores", "", FontImage.MATERIAL_STORE);
        add(Nav.ADMIN, USERS, "Users", "", FontImage.MATERIAL_GROUP);
        add(Nav.ADMIN, PAYMENTS, "Payments", "", FontImage.MATERIAL_PAYMENTS);
    }

    private Sections() {
    }

    private static void add(String mode, String id, final String title, String group,
            final char icon) {
        register(mode, new Section(id, title, group, icon, () -> placeholder(icon, title)));
    }

    /// Adds `section` to `mode`, or puts it in the place of the section with
    /// the same id.
    public static void register(String mode, Section section) {
        List<Section> list = BY_MODE.get(mode);
        if (list == null) {
            list = new ArrayList<Section>();
            BY_MODE.put(mode, list);
        }
        for (int iter = 0; iter < list.size(); iter++) {
            if (list.get(iter).id().equals(section.id())) {
                list.set(iter, section);
                return;
            }
        }
        list.add(section);
    }

    /// Gives the existing section `id` of `mode` other content, keeping its
    /// title, icon and place in the list. Does nothing for an unknown id.
    public static void content(String mode, String id, Supplier<Component> content) {
        Section found = find(mode, id);
        if (found != null) {
            register(mode, found.with(content));
        }
    }

    /// The sections of `mode`, in order: a copy, empty for an unknown mode.
    /// Puts the sections of `mode` in the order of `ids`; any not named keep
    /// their order after them. A heading is drawn where a group starts, so
    /// the order is what keeps each heading to one appearance.
    public static void order(String mode, String[] ids) {
        List<Section> list = BY_MODE.get(mode);
        if (list == null) {
            return;
        }
        List<Section> sorted = new ArrayList<Section>();
        for (int iter = 0; iter < ids.length; iter++) {
            Section found = find(mode, ids[iter]);
            if (found != null && !sorted.contains(found)) {
                sorted.add(found);
            }
        }
        for (int iter = 0; iter < list.size(); iter++) {
            if (!sorted.contains(list.get(iter))) {
                sorted.add(list.get(iter));
            }
        }
        list.clear();
        list.addAll(sorted);
    }

    public static List<Section> of(String mode) {
        List<Section> list = BY_MODE.get(mode);
        return list == null ? new ArrayList<Section>() : new ArrayList<Section>(list);
    }

    public static Section find(String mode, String id) {
        List<Section> list = BY_MODE.get(mode);
        for (int iter = 0; list != null && iter < list.size(); iter++) {
            if (list.get(iter).id().equals(id)) {
                return list.get(iter);
            }
        }
        return null;
    }

    /// What a section shows until its feature screen is written.
    public static Component placeholder(char icon, String title) {
        Component empty = Ui.empty(icon, title, "This part of Bazaar is coming soon.");
        empty.setName("placeholder");
        return empty;
    }
}
