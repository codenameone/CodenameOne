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
    /// The sales pipeline board, carts to returns.
    public static final String PIPELINE = "pipeline";
    /// The assistant that answers questions over the store's numbers.
    public static final String ASSISTANT = "assistant";
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
    public static final String COMMISSIONS = "commissions";
    public static final String PERMISSIONS = "permissions";
    public static final String INBOX = "inbox";
    public static final String LISTINGS = "listings";
    public static final String CHANNELS = "channels";
    public static final String APPS = "apps";

    /// The sidebar's order, each group in one run behind its lead (see
    /// [Sections#order]).
    static final String[] ORDER = {
        Sections.DASHBOARD, Sections.ORDERS, PIPELINE, FULFILMENT, INBOX,
        Sections.PRODUCTS, COLLECTIONS, Sections.INVENTORY, WAREHOUSE, GIFT_CARDS, SUBSCRIPTIONS,
        Sections.CUSTOMERS, COMPANIES, QUOTES, REVIEWS, LOYALTY, CLUBS,
        CAMPAIGNS, JOURNEYS, PROMOTIONS, Sections.DISCOUNTS, Sections.AUTOMATIONS,
        Sections.ANALYTICS, REPORTS, FORECAST,
        Sections.POS, LISTINGS, CHANNELS,
        Sections.SETTINGS, SHIPPING, TAXES, PERMISSIONS, COMMISSIONS, APPS, ASSISTANT
    };

    private static boolean installed;

    private MerchantSections() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        String store = Sections.ORDERS_GROUP;
        String products = Sections.PRODUCTS_GROUP;
        String people = Sections.CUSTOMERS_GROUP;
        String grow = Sections.MARKETING_GROUP;
        String numbers = Sections.ANALYTICS_GROUP;
        String channels = Sections.CHANNELS_GROUP;
        String manage = Sections.SETTINGS_GROUP;
        Sections.content(Nav.MERCHANT, Sections.DASHBOARD, DashboardPane::new);
        Sections.content(Nav.MERCHANT, Sections.ORDERS, OrdersPane::new);
        Sections.register(Nav.MERCHANT, new Section(PIPELINE, "Sales pipeline", "",
                FontImage.MATERIAL_VIEW_KANBAN, PipelinePane::new));
        Sections.register(Nav.MERCHANT, new Section(FULFILMENT, "Fulfilment", "",
                FontImage.MATERIAL_LOCAL_SHIPPING,
                FulfilmentPane::new));
        Sections.content(Nav.MERCHANT, Sections.PRODUCTS, ProductsPane::new);
        Sections.register(Nav.MERCHANT, new Section(COLLECTIONS, "Collections", products,
                FontImage.MATERIAL_COLLECTIONS_BOOKMARK, CollectionsPane::new));
        Sections.content(Nav.MERCHANT, Sections.INVENTORY, InventoryPane::new);
        Sections.content(Nav.MERCHANT, Sections.DISCOUNTS, DiscountsPane::new);
        Sections.register(Nav.MERCHANT, new Section(GIFT_CARDS, "Gift cards", products,
                FontImage.MATERIAL_CARD_GIFTCARD, GiftCardsPane::new));
        Sections.content(Nav.MERCHANT, Sections.SETTINGS, SettingsPane::new);
        Sections.content(Nav.MERCHANT, Sections.AUTOMATIONS, AutomationsPane::new);
        Sections.content(Nav.MERCHANT, Sections.ANALYTICS, AnalyticsPane::new);
        Sections.content(Nav.MERCHANT, Sections.CUSTOMERS, CustomersPane::new);
        Sections.register(Nav.MERCHANT, new Section(REVIEWS, "Reviews", people,
                FontImage.MATERIAL_RATE_REVIEW, ReviewsPane::new));
        Sections.register(Nav.MERCHANT, new Section(SHIPPING, "Shipping", manage,
                FontImage.MATERIAL_LOCAL_SHIPPING, ShippingPane::new));
        Sections.register(Nav.MERCHANT, new Section(TAXES, "Taxes", manage,
                FontImage.MATERIAL_RECEIPT, TaxesPane::new));
        Sections.register(Nav.MERCHANT, new Section(LOYALTY, "Loyalty", people,
                FontImage.MATERIAL_LOYALTY, LoyaltyPane::new));
        Sections.register(Nav.MERCHANT, new Section(CLUBS, "Clubs", people,
                FontImage.MATERIAL_GROUPS, ClubsPane::new));
        Sections.register(Nav.MERCHANT, new Section(PROMOTIONS, "Promotions", grow,
                FontImage.MATERIAL_EVENT, PromotionsPane::new));
        Sections.register(Nav.MERCHANT, new Section(CAMPAIGNS, "Campaigns", grow,
                FontImage.MATERIAL_EMAIL, MarketingPane::new));
        Sections.register(Nav.MERCHANT, new Section(JOURNEYS, "Journeys", grow,
                FontImage.MATERIAL_ROUTE, JourneysPane::new));
        Sections.register(Nav.MERCHANT, new Section(SUBSCRIPTIONS, "Subscriptions", products,
                FontImage.MATERIAL_AUTORENEW, SubscriptionsPane::new));
        Sections.register(Nav.MERCHANT, new Section(QUOTES, "Quotes", people,
                FontImage.MATERIAL_REQUEST_QUOTE, QuotesPane::new));
        Sections.register(Nav.MERCHANT, new Section(COMPANIES, "Companies", people,
                FontImage.MATERIAL_BUSINESS, CompaniesPane::new));
        Sections.register(Nav.MERCHANT, new Section(WAREHOUSE, "Warehouse", products,
                FontImage.MATERIAL_WAREHOUSE, WarehousePane::new));
        Sections.register(Nav.MERCHANT, new Section(FORECAST, "Forecast", numbers,
                FontImage.MATERIAL_TRENDING_UP, ForecastPane::new));
        Sections.register(Nav.MERCHANT, new Section(ASSISTANT, "Assistant", manage,
                FontImage.MATERIAL_AUTO_AWESOME, AssistantPane::new));
        Sections.register(Nav.MERCHANT, new Section(REPORTS, "Reports", numbers,
                FontImage.MATERIAL_INSIGHTS, ReportsPane::new));
        Sections.register(Nav.MERCHANT, new Section(COMMISSIONS, "Commissions", manage,
                FontImage.MATERIAL_BADGE, CommissionsPane::new));
        Sections.register(Nav.MERCHANT, new Section(INBOX, "Inbox", store,
                FontImage.MATERIAL_FORUM, InboxPane::new));
        Sections.register(Nav.MERCHANT, new Section(PERMISSIONS, "Permissions", manage,
                FontImage.MATERIAL_ADMIN_PANEL_SETTINGS, PermissionsPane::new));
        Sections.register(Nav.MERCHANT, new Section(LISTINGS, "Listings", channels,
                FontImage.MATERIAL_VIEW_MODULE, ListingsPane::new));
        Sections.register(Nav.MERCHANT, new Section(CHANNELS, "Channels", channels,
                FontImage.MATERIAL_SYNC, ChannelsPane::new));
        Sections.register(Nav.MERCHANT, new Section(APPS, "Apps", manage,
                FontImage.MATERIAL_EXTENSION, AppsPane::new));
        Sections.order(Nav.MERCHANT, ORDER);
    }
}
