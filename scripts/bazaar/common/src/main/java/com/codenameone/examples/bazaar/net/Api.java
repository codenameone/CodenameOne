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
package com.codenameone.examples.bazaar.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.bazaar.api.AccountApi;
import com.codenameone.examples.bazaar.api.AdminApi;
import com.codenameone.examples.bazaar.api.CartApi;
import com.codenameone.examples.bazaar.api.CatalogApi;
import com.codenameone.examples.bazaar.api.CheckoutApi;
import com.codenameone.examples.bazaar.api.DiscountApi;
import com.codenameone.examples.bazaar.api.FulfillmentApi;
import com.codenameone.examples.bazaar.api.PosApi;
import com.codenameone.examples.bazaar.api.PosCounterApi;
import com.codenameone.examples.bazaar.api.PosToolsApi;
import com.codenameone.examples.bazaar.api.LayawayApi;
import com.codenameone.examples.bazaar.api.OrderEditApi;
import com.codenameone.examples.bazaar.api.ReturnApi;
import com.codenameone.examples.bazaar.api.GiftCardApi;
import com.codenameone.examples.bazaar.api.InventoryApi;
import com.codenameone.examples.bazaar.api.RoutingApi;
import com.codenameone.examples.bazaar.api.ShippingApi;
import com.codenameone.examples.bazaar.api.OrderToolsApi;
import com.codenameone.examples.bazaar.api.StockStatesApi;
import com.codenameone.examples.bazaar.api.TaxApi;
import com.codenameone.examples.bazaar.api.OrderApi;
import com.codenameone.examples.bazaar.api.PaymentApi;
import com.codenameone.examples.bazaar.api.StoreApi;
import com.codenameone.examples.bazaar.api.AutomationApi;
import com.codenameone.examples.bazaar.api.AnalyticsApi;
import com.codenameone.examples.bazaar.api.PipelineApi;
import com.codenameone.examples.bazaar.api.CustomerApi;
import com.codenameone.examples.bazaar.api.WebhookApi;
import com.codenameone.examples.bazaar.api.StoreMediaApi;
import com.codenameone.examples.bazaar.api.PlatformApi;
import com.codenameone.examples.bazaar.api.ReviewApi;
import com.codenameone.examples.bazaar.api.WishlistApi;
import com.codenameone.examples.bazaar.api.DiscoverApi;
import com.codenameone.examples.bazaar.api.AddressApi;
import com.codenameone.examples.bazaar.api.TradeApi;
import com.codenameone.examples.bazaar.api.SubscriptionApi;
import com.codenameone.examples.bazaar.api.JourneyApi;
import com.codenameone.examples.bazaar.api.MarketApi;
import com.codenameone.examples.bazaar.api.MarketingApi;
import com.codenameone.examples.bazaar.api.SaleApi;
import com.codenameone.examples.bazaar.api.ClubApi;
import com.codenameone.examples.bazaar.api.ShopperDataApi;
import com.codenameone.examples.bazaar.api.LoyaltyApi;
import com.codenameone.examples.bazaar.api.OpsApi;
import com.codenameone.examples.bazaar.api.ChannelsApi;
import com.codenameone.examples.bazaar.api.DigitalApi;
import com.codenameone.examples.bazaar.api.TwoFactorApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static AdminApi admin;
    private static PaymentApi payments;
    private static StoreApi stores;
    private static CatalogApi catalog;
    private static CartApi cart;
    private static CheckoutApi checkout;
    private static OrderApi orders;
    private static GiftCardApi giftCards;
    private static InventoryApi inventory;
    private static DiscountApi discounts;
    private static TaxApi taxes;
    private static OrderToolsApi orderTools;
    private static StockStatesApi stockStates;
    private static ShippingApi shipping;
    private static RoutingApi routing;
    private static FulfillmentApi fulfilment;
    private static ReturnApi returns;
    private static PosApi pos;
    private static PosToolsApi posTools;
    private static PosCounterApi posCounter;
    private static LayawayApi layaways;
    private static OrderEditApi orderEdits;
    private static AutomationApi automations;
    private static AnalyticsApi analytics;
    private static PipelineApi pipeline;
    private static CustomerApi customers;
    private static WebhookApi webhooks;
    private static StoreMediaApi storeMedia;
    private static PlatformApi platform;
    private static ReviewApi reviews;
    private static WishlistApi wishlist;
    private static DiscoverApi discover;
    private static AddressApi addresses;
    private static TradeApi trade;
    private static SubscriptionApi subscriptions;
    private static JourneyApi journeys;
    private static MarketingApi marketing;
    private static MarketApi markets;
    private static SaleApi sales;
    private static ClubApi clubs;
    private static ShopperDataApi shopperData;
    private static LoyaltyApi loyalty;
    private static OpsApi ops;
    private static ChannelsApi channels;
    private static DigitalApi digital;
    private static TwoFactorApi twoFactor;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        payments = RestClients.create(PaymentApi.class, baseUrl);
        stores = RestClients.create(StoreApi.class, baseUrl);
        catalog = RestClients.create(CatalogApi.class, baseUrl);
        cart = RestClients.create(CartApi.class, baseUrl);
        checkout = RestClients.create(CheckoutApi.class, baseUrl);
        orders = RestClients.create(OrderApi.class, baseUrl);
        giftCards = RestClients.create(GiftCardApi.class, baseUrl);
        inventory = RestClients.create(InventoryApi.class, baseUrl);
        discounts = RestClients.create(DiscountApi.class, baseUrl);
        taxes = RestClients.create(TaxApi.class, baseUrl);
        orderTools = RestClients.create(OrderToolsApi.class, baseUrl);
        stockStates = RestClients.create(StockStatesApi.class, baseUrl);
        shipping = RestClients.create(ShippingApi.class, baseUrl);
        routing = RestClients.create(RoutingApi.class, baseUrl);
        fulfilment = RestClients.create(FulfillmentApi.class, baseUrl);
        returns = RestClients.create(ReturnApi.class, baseUrl);
        pos = RestClients.create(PosApi.class, baseUrl);
        posTools = RestClients.create(PosToolsApi.class, baseUrl);
        posCounter = RestClients.create(PosCounterApi.class, baseUrl);
        layaways = RestClients.create(LayawayApi.class, baseUrl);
        orderEdits = RestClients.create(OrderEditApi.class, baseUrl);
        automations = RestClients.create(AutomationApi.class, baseUrl);
        analytics = RestClients.create(AnalyticsApi.class, baseUrl);
        pipeline = RestClients.create(PipelineApi.class, baseUrl);
        customers = RestClients.create(CustomerApi.class, baseUrl);
        webhooks = RestClients.create(WebhookApi.class, baseUrl);
        storeMedia = RestClients.create(StoreMediaApi.class, baseUrl);
        platform = RestClients.create(PlatformApi.class, baseUrl);
        reviews = RestClients.create(ReviewApi.class, baseUrl);
        wishlist = RestClients.create(WishlistApi.class, baseUrl);
        discover = RestClients.create(DiscoverApi.class, baseUrl);
        addresses = RestClients.create(AddressApi.class, baseUrl);
        trade = RestClients.create(TradeApi.class, baseUrl);
        subscriptions = RestClients.create(SubscriptionApi.class, baseUrl);
        journeys = RestClients.create(JourneyApi.class, baseUrl);
        marketing = RestClients.create(MarketingApi.class, baseUrl);
        markets = RestClients.create(MarketApi.class, baseUrl);
        sales = RestClients.create(SaleApi.class, baseUrl);
        clubs = RestClients.create(ClubApi.class, baseUrl);
        shopperData = RestClients.create(ShopperDataApi.class, baseUrl);
        loyalty = RestClients.create(LoyaltyApi.class, baseUrl);
        ops = RestClients.create(OpsApi.class, baseUrl);
        channels = RestClients.create(ChannelsApi.class, baseUrl);
        digital = RestClients.create(DigitalApi.class, baseUrl);
        twoFactor = RestClients.create(TwoFactorApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static AdminApi admin() {
        return admin;
    }

    public static PaymentApi payments() {
        return payments;
    }

    public static StoreApi stores() {
        return stores;
    }

    public static CatalogApi catalog() {
        return catalog;
    }

    public static CartApi cart() {
        return cart;
    }

    public static CheckoutApi checkout() {
        return checkout;
    }

    public static OrderApi orders() {
        return orders;
    }

    public static GiftCardApi giftCards() {
        return giftCards;
    }

    public static InventoryApi inventory() {
        return inventory;
    }

    public static DiscountApi discounts() {
        return discounts;
    }

    public static OrderToolsApi orderTools() {
        return orderTools;
    }

    public static StockStatesApi stockStates() {
        return stockStates;
    }

    public static TaxApi taxes() {
        return taxes;
    }

    public static ShippingApi shipping() {
        return shipping;
    }

    public static RoutingApi routing() {
        return routing;
    }

    public static FulfillmentApi fulfilment() {
        return fulfilment;
    }

    public static ReturnApi returns() {
        return returns;
    }

    public static PosApi pos() {
        return pos;
    }

    /// Policy, approvals, parked carts and the smart grid.
    /// Card readers, the pickup queue and sales shipped from the counter.
    public static PosCounterApi posCounter() {
        return posCounter;
    }

    public static PosToolsApi posTools() {
        return posTools;
    }

    /// Layaways.
    public static LayawayApi layaways() {
        return layaways;
    }

    /// Edits of placed orders.
    public static OrderEditApi orderEdits() {
        return orderEdits;
    }

    public static AutomationApi automations() {
        return automations;
    }

    public static AnalyticsApi analytics() {
        return analytics;
    }

    /// The sales pipeline board.
    public static PipelineApi pipeline() {
        return pipeline;
    }

    public static CustomerApi customers() {
        return customers;
    }

    public static WebhookApi webhooks() {
        return webhooks;
    }

    public static StoreMediaApi storeMedia() {
        return storeMedia;
    }

    public static PlatformApi platform() {
        return platform;
    }

    public static ReviewApi reviews() {
        return reviews;
    }

    /// The feed across stores, and the stores the shopper follows.
    public static DiscoverApi discover() {
        return discover;
    }

    public static WishlistApi wishlist() {
        return wishlist;
    }

    public static AddressApi addresses() {
        return addresses;
    }

    public static TradeApi trade() {
        return trade;
    }

    public static SubscriptionApi subscriptions() {
        return subscriptions;
    }

    public static JourneyApi journeys() {
        return journeys;
    }

    public static MarketingApi marketing() {
        return marketing;
    }

    /// Markets and translations; the app uses it for a campaign message's
    /// translations.
    public static MarketApi markets() {
        return markets;
    }

    public static SaleApi sales() {
        return sales;
    }

    /// The signed-in shopper's own data: the export and the requests to stores.
    public static ShopperDataApi shopperData() {
        return shopperData;
    }

    public static ClubApi clubs() {
        return clubs;
    }

    /// Warehouse, forecasts, reports, permissions, chat and listings.
    public static OpsApi ops() {
        return ops;
    }

    /// Marketplace channels and their sync.
    public static ChannelsApi channels() {
        return channels;
    }

    /// Digital downloads, licence keys and product video and models.
    public static DigitalApi digital() {
        return digital;
    }

    public static TwoFactorApi twoFactor() {
        return twoFactor;
    }

    public static LoyaltyApi loyalty() {
        return loyalty;
    }
}
