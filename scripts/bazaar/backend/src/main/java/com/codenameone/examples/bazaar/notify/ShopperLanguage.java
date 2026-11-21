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
package com.codenameone.examples.bazaar.notify;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Market;
import com.codenameone.examples.bazaar.domain.Profile;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.market.Locales;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.MarketRepository;
import com.codenameone.examples.bazaar.repository.ProfileRepository;
import com.codenameone.examples.bazaar.web.StoreText;
import com.codenameone.examples.bazaar.web.StorefrontMarkets;

import java.util.List;

/// The language a message to a shopper goes out in.
///
/// What is recorded: the order and the cart keep the language they were
/// placed or filled in, the store's customer record the language of the
/// shopper's last order, and the account ([Profile#getLanguage]) the one the
/// shopper signed up in or picked in the app's settings. A message is written
/// in the first of those that is known -- order, then customer, then account
/// -- else the language of the store's first market, else English.
@Component
public class ShopperLanguage {
    private final CustomerRepository customers;
    private final ProfileRepository profiles;
    private final MarketRepository markets;

    public ShopperLanguage(CustomerRepository customers, ProfileRepository profiles,
            MarketRepository markets) {
        this.customers = customers;
        this.profiles = profiles;
        this.markets = markets;
    }

    /// The language of the request being served, or empty when it says none:
    /// the language a storefront page resolved for it, else the shopper's
    /// explicit choice (`?lang=`, the cookie), else the best supported entry
    /// of `Accept-Language` (what the app and other API clients send).
    public static String fromRequest() {
        String ui = StorefrontMarkets.chosenUi();
        if (StoreText.supported(ui)) {
            return ui;
        }
        SecurityExchange ex = SecurityExchange.current();
        HttpServer.Request request = ex == null ? null : ex.getRequest();
        if (request == null) {
            return "";
        }
        String chosen = StoreText.chosen(request);
        if (chosen != null) {
            return chosen;
        }
        List<String> accepted = Locales.accepted(request.getHeader("Accept-Language"));
        for (int iter = 0; accepted != null && iter < accepted.size(); iter++) {
            String l = Locales.language(accepted.get(iter));
            if (StoreText.supported(l)) {
                return l;
            }
        }
        return "";
    }

    /// The language of mail about `o`.
    public String of(StoreOrder o) {
        if (o == null) {
            return "en";
        }
        if (StoreText.supported(o.getLanguage())) {
            return o.getLanguage();
        }
        return shopper(o.getStoreId(), o.getUsername(), o.getEmail());
    }

    /// The language of a reminder about `cart`.
    public String of(Cart cart) {
        if (StoreText.supported(cart.getLanguage())) {
            return cart.getLanguage();
        }
        return shopper(cart.getStoreId(), cart.getUsername(), cart.getEmail());
    }

    /// The language of mail to a shopper of `storeId` known by account name
    /// and/or address (either may be null or empty).
    public String shopper(String storeId, String username, String email) {
        Customer c = null;
        if (username != null && username.length() > 0) {
            c = customers.findByStoreIdAndUsername(storeId, username);
        }
        if (c == null && email != null && email.length() > 0) {
            c = customers.findByStoreIdAndEmail(storeId, email.trim());
        }
        if (c != null && StoreText.supported(c.getLanguage())) {
            return c.getLanguage();
        }
        String account = c != null && c.getUsername().length() > 0 ? c.getUsername() : username;
        Profile p = account == null || account.length() == 0 ? null : profiles.findById(account);
        if (p != null && StoreText.supported(p.getLanguage())) {
            return p.getLanguage();
        }
        return store(storeId);
    }

    /// The language of mail to a member of a store's staff: the one their
    /// account is set to ([Profile#getLanguage]), else English -- the staff's
    /// working language in this sample, never the store's market.
    public String staff(String username) {
        Profile p = username == null || username.length() == 0 ? null : profiles.findById(username);
        return p != null && StoreText.supported(p.getLanguage()) ? p.getLanguage() : "en";
    }

    /// The store's own language: that of its first market with text for it,
    /// else English.
    public String store(String storeId) {
        List<Market> ms = storeId == null ? null : markets.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; ms != null && iter < ms.size(); iter++) {
            String l = Locales.language(ms.get(iter).getLocale());
            if (ms.get(iter).isActive() && StoreText.supported(l)) {
                return l;
            }
        }
        return "en";
    }
}
