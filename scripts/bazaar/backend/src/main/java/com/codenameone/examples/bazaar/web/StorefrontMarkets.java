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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.market.Locales;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.market.Translations;
import com.codenameone.examples.bazaar.web.slots.Slots;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Which market and language a storefront page is drawn for.
///
/// The market comes from `?market=<code>` -- an address of its own, so a
/// shared cache keeps one copy per market and search engines see each as an
/// alternate (`hreflang`, in Link headers since compiled views take only
/// static hrefs) -- or else from the `market` cookie a shopper's earlier pick
/// left, which makes that copy private ([Slots#PERSONAL]). With neither the
/// store's own prices show, and the page is the shared default.
///
/// The language is the market's locale, or else the best of the visitor's
/// `Accept-Language` that the store has translations for; a page translated
/// that way says `Vary: Accept-Language`.
public final class StorefrontMarkets {
    public static final String COOKIE = "market";
    /// The exchange attribute holding the extra Link header values.
    public static final String LINKS = "bazaar.market.links";
    /// The exchange attribute naming the request header the page varies on.
    public static final String VARY = "bazaar.market.vary";

    private StorefrontMarkets() {
    }

    /// The market code the query names, or null.
    public static String query(HttpServer.Request request) {
        String q;
        try {
            q = request.param(COOKIE);
        } catch (RuntimeException malformed) {
            q = null;
        }
        return code(q);
    }

    /// The market code of the visitor's cookie, or null.
    public static String cookie(HttpServer.Request request) {
        return request == null ? null : code(request.getCookie(COOKIE));
    }

    static String code(String c) {
        if (c == null || c.length() == 0 || c.length() > 16) {
            return null;
        }
        for (int iter = 0; iter < c.length(); iter++) {
            char ch = c.charAt(iter);
            if (!(ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '-')) {
                return null;
            }
        }
        return c;
    }

    static final String CHOICE = "bazaar.market.choice";

    /// What a product page needs: the pricer (or null), the locale (or null).
    public static final class Choice {
        String storeId;
        public Markets.Pricer pricer;
        public String locale;
        /// The interface language ([StoreText#LANGUAGES]).
        public String ui = "en";
        public final List<LinkView> picker = new ArrayList<LinkView>();
        /// The market the visitor's country suggests ([GeoCountry]), offered in a
        /// banner and never applied; null when there is none.
        public LinkView suggested;
        /// Whether the shopper picked a market (query or cookie).
        boolean chose;
        /// Whether a country lookup has been asked for this request.
        boolean geoAsked;
    }

    /// Chooses for `path` (no query) at `store`, and records the headers.
    public static Choice choose(Store store, String path, HttpServer.Request request,
            Markets markets, Translations translations, SiteUrls urls) throws IOException {
        return choose(store, path, request, markets, translations, urls, null);
    }

    /// [#choose] that also suggests a market from the visitor's country when
    /// `geo` knows it, the shopper chose nothing (no query, no cookie) and the
    /// visitor is not a crawler. A page with a suggestion is private
    /// ([Slots#PERSONAL]); its prices, address and canonical are unchanged.
    public static Choice choose(Store store, String path, HttpServer.Request request,
            Markets markets, Translations translations, SiteUrls urls, GeoCountry geo)
            throws IOException {
        SecurityExchange ex = SecurityExchange.current();
        // Once per request: the page body and the menus every page carries
        // ask the same question, and the headers are recorded once.
        Object made = ex == null ? null : ex.getAttribute(CHOICE);
        if (made instanceof Choice && store.getId().equals(((Choice) made).storeId)) {
            Choice prior = (Choice) made;
            // An earlier caller (the page shell) may have chosen without a
            // country lookup; the product page asks with one, so suggest now.
            if (geo != null && !prior.geoAsked) {
                prior.geoAsked = true;
                if (!prior.chose && request != null
                        && !GeoCountry.crawler(request.getHeader("User-Agent"))) {
                    prior.suggested = suggest(markets.active(store.getId()),
                            geo.country(request), path);
                    if (prior.suggested != null) {
                        ex.setAttribute(Slots.PERSONAL, "1");
                    }
                }
            }
            return prior;
        }
        Choice c = new Choice();
        c.storeId = store.getId();
        if (ex != null) {
            ex.setAttribute(CHOICE, c);
        }
        String code = query(request);
        boolean fromCookie = false;
        if (code == null) {
            code = cookie(request);
            fromCookie = code != null;
        }
        c.pricer = markets.pricer(store, code);
        if (c.pricer != null && fromCookie && ex != null) {
            ex.setAttribute(Slots.PERSONAL, "1");
        }
        List<MarketDto> all = markets.active(store.getId());
        boolean chose = code != null;
        c.chose = chose;
        c.geoAsked = geo != null;
        StringBuilder links = new StringBuilder();
        String current = c.pricer == null ? null : c.pricer.market().getCode();
        c.picker.add(new LinkView(store.getCurrency() + " (store prices)", path, current == null));
        for (int iter = 0; iter < all.size(); iter++) {
            MarketDto m = all.get(iter);
            String href = path + "?market=" + SiteUrls.encode(m.code());
            c.picker.add(new LinkView(m.name() + " (" + m.currency() + ")", href,
                    m.code().equals(current)));
            String lang = Locales.orNull(m.locale());
            if (lang != null) {
                if (links.length() > 0) {
                    links.append(", ");
                }
                links.append('<').append(urls.absolute(request, href))
                        .append(">; rel=\"alternate\"; hreflang=\"").append(lang).append('"');
            }
        }
        if (geo != null && !chose && request != null
                && !GeoCountry.crawler(request.getHeader("User-Agent"))) {
            c.suggested = suggest(all, geo.country(request), path);
            if (c.suggested != null && ex != null) {
                ex.setAttribute(Slots.PERSONAL, "1");
            }
        }
        if (links.length() > 0) {
            links.append(", <").append(urls.absolute(request, path))
                    .append(">; rel=\"alternate\"; hreflang=\"x-default\"");
            if (ex != null) {
                ex.setAttribute(LINKS, links.toString());
            }
        }
        if (c.pricer != null) {
            c.locale = Locales.orNull(c.pricer.market().getLocale());
        }
        if (c.locale == null) {
            List<String> wanted = Locales.accepted(request.getHeader("Accept-Language"));
            c.locale = wanted.isEmpty() ? null : translations.pick(store.getId(), wanted);
            if (ex != null) {
                ex.setAttribute(VARY, "Accept-Language");
            }
        }
        // The interface text: the content language when the app ships text
        // for it, else the visitor's best (StoreText). A market whose locale
        // has no interface text falls back on the header too, so that page
        // varies by it as well.
        String spoken = c.locale == null ? null : Locales.language(c.locale);
        if (spoken != null && StoreText.supported(spoken)) {
            c.ui = spoken;
        } else {
            c.ui = StoreText.pick(null, Locales.accepted(request.getHeader("Accept-Language")));
            if (ex != null) {
                ex.setAttribute(VARY, "Accept-Language");
            }
        }
        c.picker.set(0, new LinkView(store.getCurrency() + " ("
                + StoreText.table(c.ui).get("storePrices") + ")", path, current == null));
        return c;
    }

    /// The first active market selling to `country`, as a link, or null.
    static LinkView suggest(List<MarketDto> all, String country, String path) {
        if (country == null) {
            return null;
        }
        for (int iter = 0; iter < all.size(); iter++) {
            MarketDto m = all.get(iter);
            List<String> cs = m.countries();
            for (int at = 0; cs != null && at < cs.size(); at++) {
                if (country.equalsIgnoreCase(cs.get(at))) {
                    return new LinkView("Shopping from " + country + "? See prices for "
                            + m.name() + " (" + m.currency() + ")",
                            path + "?market=" + SiteUrls.encode(m.code()), false);
                }
            }
        }
        return null;
    }

    /// The translated value of `field`, or `original`.
    public static String text(Map<String, String> fields, String field, String original) {
        String v = fields == null ? null : fields.get(field);
        return v == null || v.length() == 0 ? original : v;
    }
}
