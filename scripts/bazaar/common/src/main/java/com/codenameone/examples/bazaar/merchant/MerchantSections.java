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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.ui.FontImage;
import com.codenameone.examples.bazaar.ui.Nav;
import com.codenameone.examples.bazaar.ui.Section;
import com.codenameone.examples.bazaar.ui.Sections;

/// Puts the merchant app's screens into the merchant mode's sections.
///
/// The fulfilment section is the Kanban board of `FulfilmentPane`.
public final class MerchantSections {
    /// The order fulfilment board.
    public static final String FULFILMENT = "fulfilment";
    public static final String COLLECTIONS = "collections";
    public static final String SHIPPING = "shipping";
    public static final String TAXES = "taxes";
    public static final String GIFT_CARDS = "giftcards";
    /// Review moderation.
    public static final String REVIEWS = "reviews";

    public static final String LOYALTY = "loyalty";
    public static final String CLUBS = "clubs";
    public static final String PROMOTIONS = "promotions";
    public static final String CAMPAIGNS = "campaigns";
    public static final String JOURNEYS = "journeys";
    public static final String SUBSCRIPTIONS = "subscriptions";
    public static final String QUOTES = "quotes";
    public static final String COMPANIES = "companies";
    public static final String WAREHOUSE = "warehouse";
    public static final String FORECAST = "forecast";
    public static final String REPORTS = "reports";
    public static final String PERMISSIONS = "permissions";
    public static final String INBOX = "inbox";
    public static final String LISTINGS = "listings";

    private static boolean installed;

    private MerchantSections() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        String store = "Store";
        String grow = "Marketing";
        String manage = "Manage";
        Sections.content(Nav.MERCHANT, Sections.DASHBOARD, DashboardPane::new);
        Sections.content(Nav.MERCHANT, Sections.ORDERS, OrdersPane::new);
        Sections.register(Nav.MERCHANT, new Section(FULFILMENT, "Fulfilment", store,
                FontImage.MATERIAL_LOCAL_SHIPPING,
                FulfilmentPane::new));
        Sections.content(Nav.MERCHANT, Sections.PRODUCTS, ProductsPane::new);
        Sections.register(Nav.MERCHANT, new Section(COLLECTIONS, "Collections", store,
                FontImage.MATERIAL_COLLECTIONS_BOOKMARK, CollectionsPane::new));
        Sections.content(Nav.MERCHANT, Sections.INVENTORY, InventoryPane::new);
        Sections.content(Nav.MERCHANT, Sections.DISCOUNTS, DiscountsPane::new);
        Sections.register(Nav.MERCHANT, new Section(GIFT_CARDS, "Gift cards", grow,
                FontImage.MATERIAL_CARD_GIFTCARD, GiftCardsPane::new));
        Sections.content(Nav.MERCHANT, Sections.SETTINGS, SettingsPane::new);
        Sections.content(Nav.MERCHANT, Sections.AUTOMATIONS, AutomationsPane::new);
        Sections.content(Nav.MERCHANT, Sections.ANALYTICS, AnalyticsPane::new);
        Sections.content(Nav.MERCHANT, Sections.CUSTOMERS, CustomersPane::new);
        Sections.register(Nav.MERCHANT, new Section(REVIEWS, "Reviews", store,
                FontImage.MATERIAL_RATE_REVIEW, ReviewsPane::new));
        Sections.register(Nav.MERCHANT, new Section(SHIPPING, "Shipping", manage,
                FontImage.MATERIAL_LOCAL_SHIPPING, ShippingPane::new));
        Sections.register(Nav.MERCHANT, new Section(TAXES, "Taxes", manage,
                FontImage.MATERIAL_RECEIPT, TaxesPane::new));
        Sections.register(Nav.MERCHANT, new Section(LOYALTY, "Loyalty", grow,
                FontImage.MATERIAL_LOYALTY, LoyaltyPane::new));
        Sections.register(Nav.MERCHANT, new Section(CLUBS, "Clubs", grow,
                FontImage.MATERIAL_GROUPS, ClubsPane::new));
        Sections.register(Nav.MERCHANT, new Section(PROMOTIONS, "Promotions", grow,
                FontImage.MATERIAL_EVENT, PromotionsPane::new));
        Sections.register(Nav.MERCHANT, new Section(CAMPAIGNS, "Campaigns", grow,
                FontImage.MATERIAL_EMAIL, MarketingPane::new));
        Sections.register(Nav.MERCHANT, new Section(JOURNEYS, "Journeys", grow,
                FontImage.MATERIAL_ROUTE, JourneysPane::new));
        Sections.register(Nav.MERCHANT, new Section(SUBSCRIPTIONS, "Subscriptions", store,
                FontImage.MATERIAL_AUTORENEW, SubscriptionsPane::new));
        Sections.register(Nav.MERCHANT, new Section(QUOTES, "Quotes", store,
                FontImage.MATERIAL_REQUEST_QUOTE, QuotesPane::new));
        Sections.register(Nav.MERCHANT, new Section(COMPANIES, "Companies", manage,
                FontImage.MATERIAL_BUSINESS, CompaniesPane::new));
        Sections.register(Nav.MERCHANT, new Section(WAREHOUSE, "Warehouse", store,
                FontImage.MATERIAL_WAREHOUSE, WarehousePane::new));
        Sections.register(Nav.MERCHANT, new Section(FORECAST, "Forecast", manage,
                FontImage.MATERIAL_TRENDING_UP, ForecastPane::new));
        Sections.register(Nav.MERCHANT, new Section(REPORTS, "Reports", manage,
                FontImage.MATERIAL_INSIGHTS, ReportsPane::new));
        Sections.register(Nav.MERCHANT, new Section(INBOX, "Inbox", store,
                FontImage.MATERIAL_FORUM, InboxPane::new));
        Sections.register(Nav.MERCHANT, new Section(PERMISSIONS, "Permissions", manage,
                FontImage.MATERIAL_ADMIN_PANEL_SETTINGS, PermissionsPane::new));
        Sections.register(Nav.MERCHANT, new Section(LISTINGS, "Listings", store,
                FontImage.MATERIAL_VIEW_MODULE, ListingsPane::new));
    }
}
