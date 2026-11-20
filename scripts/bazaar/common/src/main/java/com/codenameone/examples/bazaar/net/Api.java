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
import com.codenameone.examples.bazaar.api.GiftCardApi;
import com.codenameone.examples.bazaar.api.InventoryApi;
import com.codenameone.examples.bazaar.api.ShippingApi;
import com.codenameone.examples.bazaar.api.TaxApi;
import com.codenameone.examples.bazaar.api.OrderApi;
import com.codenameone.examples.bazaar.api.PaymentApi;
import com.codenameone.examples.bazaar.api.StoreApi;

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
    private static ShippingApi shipping;

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
        shipping = RestClients.create(ShippingApi.class, baseUrl);
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

    public static TaxApi taxes() {
        return taxes;
    }

    public static ShippingApi shipping() {
        return shipping;
    }
}
