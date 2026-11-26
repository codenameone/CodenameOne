/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.bazaar.web;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.security.WebHeaders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The storefront's search, pre-launch and embedding surfaces:
///
/// - `/s/{handle}/suggest?q=` answers predictive search as JSON, for the
///   storefront's search box (`/suggest.js`) and the app's search field.
/// - `/s/{handle}/password` is the pre-launch page of a password-protected
///   store ([#locked]); staff of the store pass without it.
/// - `/s/{handle}/embed/{product}` is a product card made to sit in an
///   `iframe` on another site (a buy button): the one page allowed to be
///   framed by anyone, and it links to the product page in the top window.
@Controller
public class StoreSite {
    static final String UNLOCKED = "bazaar.unlocked.";
    private static final int SUGGESTIONS = 8;

    private final StoreRepository stores;
    private final ProductRepository products;
    private final StoreStaffRepository staff;
    private final Catalog catalog;
    private final Stores storeService;
    private final StoreAccess access;
    private final SiteUrls urls;

    public StoreSite(StoreRepository stores, ProductRepository products,
            StoreStaffRepository staff, Catalog catalog, Stores storeService, StoreAccess access,
            SiteUrls urls) {
        this.stores = stores;
        this.products = products;
        this.staff = staff;
        this.catalog = catalog;
        this.storeService = storeService;
        this.access = access;
        this.urls = urls;
    }

    // ------------------------------------------------------------ password

    /// Whether `store` hides its storefront from this visitor: it has a
    /// password, the visitor has not typed it this session, and the signed
    /// in user (if any) is not on its staff.
    public boolean locked(Store store, HttpServer.Request request) throws IOException {
        if (store.getStorefrontPassword().length() == 0) {
            return false;
        }
        HttpSession session = request == null ? null : request.getSession(false);
        if (session != null && session.getAttribute(UNLOCKED + store.getId()) != null) {
            return false;
        }
        String who = CheckoutPages.signedIn();
        return who == null || staff.findByStoreIdAndUsername(store.getId(), who) == null;
    }

    /// [#locked] by handle; false for a store that does not exist.
    public boolean locked(String handle, HttpServer.Request request) throws IOException {
        Store store = handle == null ? null : stores.findByHandle(handle);
        return store != null && locked(store, request);
    }

    @GetMapping("/s/{handle}/password")
    @Transactional(readOnly = true)
    public HttpServer.Response passwordPage(@PathVariable("handle") String handle,
            @RequestParam(value = "wrong", required = false) String wrong,
            HttpServer.Request request) throws IOException {
        Store store = storeService.activeStore(handle);
        StringBuilder b = new StringBuilder(1024);
        b.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<meta name=\"robots\" content=\"noindex\"><title>")
                .append(esc(store.getName())).append("</title></head><body>")
                .append("<main><h1>").append(esc(store.getName())).append("</h1>")
                .append("<p>Opening soon. Enter the store password to look around.</p>");
        if (wrong != null) {
            b.append("<p role=\"alert\">That password is not right.</p>");
        }
        b.append("<form method=\"post\" action=\"/s/").append(esc(handle))
                .append("/password\"><label for=\"pw\">Password</label> ")
                .append("<input id=\"pw\" name=\"password\" type=\"password\" required> ")
                .append("<button type=\"submit\">Enter</button></form></main></body></html>");
        return Conditional.answer(request, "text/html; charset=utf-8",
                b.toString().getBytes("UTF-8"), "no-store", 0);
    }

    @PostMapping("/s/{handle}/password")
    @Transactional(readOnly = true)
    public String unlock(@PathVariable("handle") String handle, HttpServer.Request request)
            throws IOException {
        Store store = storeService.activeStore(handle);
        String typed = request.param("password");
        if (typed != null && same(typed, store.getStorefrontPassword())) {
            request.getSession(true).setAttribute(UNLOCKED + store.getId(), "1");
            return "redirect:" + StorefrontPages.storeUrl(handle);
        }
        return "redirect:" + StorefrontPages.storeUrl(handle) + "/password?wrong=1";
    }

    /// Compares in time independent of where the strings differ.
    static boolean same(String a, String b) {
        if (b.length() == 0) {
            return false;
        }
        int diff = a.length() ^ b.length();
        for (int iter = 0; iter < b.length(); iter++) {
            diff |= (iter < a.length() ? a.charAt(iter) : 0) ^ b.charAt(iter);
        }
        return diff == 0;
    }

    // ------------------------------------------------------------- suggest

    @GetMapping("/s/{handle}/suggest")
    public HttpServer.Response suggest(@PathVariable("handle") String handle,
            @RequestParam(value = "q", required = false) String q, HttpServer.Request request)
            throws IOException {
        List<Object> out = new ArrayList<Object>();
        if (!locked(handle, request)) {
            List<String[]> found = catalog.suggest(handle, q == null ? "" : q, SUGGESTIONS);
            for (int iter = 0; iter < found.size(); iter++) {
                String[] s = found.get(iter);
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("kind", s[0]);
                m.put("title", s[1]);
                m.put("handle", s[2]);
                m.put("url", "product".equals(s[0]) ? StorefrontPages.productUrl(handle, s[2])
                        : StorefrontPages.collectionUrl(handle, s[2]));
                out.add(m);
            }
        }
        return HttpServer.Response.json(200, Json.write(out))
                .header("Cache-Control", "public, max-age=60");
    }

    /// The storefront search box's suggestions, as a file so the page's
    /// script policy (`script-src 'self'`) needs no hash for it: fills the
    /// box's `datalist` from `suggest` as the shopper types.
    @GetMapping("/suggest.js")
    public HttpServer.Response suggestScript(HttpServer.Request request) throws IOException {
        String js = "(function(){var f=document.querySelector('form.search[data-suggest]');"
                + "if(!f){return;}var i=f.querySelector('input[name=q]'),"
                + "l=document.getElementById('q-suggest'),t=null,urls={};"
                + "i.addEventListener('input',function(){clearTimeout(t);var v=i.value;"
                + "if(urls[v]){location.href=urls[v];return;}"
                + "if(v.length<2){return;}t=setTimeout(function(){"
                + "fetch(f.getAttribute('data-suggest')+'?q='+encodeURIComponent(v))"
                + ".then(function(r){return r.json();}).then(function(a){l.innerHTML='';"
                + "urls={};a.forEach(function(s){var o=document.createElement('option');"
                + "o.value=s.title;urls[s.title]=s.url;l.appendChild(o);});});},150);});})();";
        return Conditional.answer(request, "application/javascript; charset=utf-8",
                js.getBytes("UTF-8"), "public, max-age=3600", 0);
    }

    // --------------------------------------------------------------- embed

    @GetMapping("/s/{handle}/embed/{product}")
    @Transactional(readOnly = true)
    public HttpServer.Response embed(@PathVariable("handle") String handle,
            @PathVariable("product") String product, HttpServer.Request request)
            throws IOException {
        Store store = storeService.activeStore(handle);
        Product p = products.findByStoreIdAndHandle(store.getId(), product);
        if (p == null || !Catalog.ACTIVE.equals(p.getStatus()) || locked(store, null)) {
            throw new ResponseStatusException(404, "No such product");
        }
        long[] range = catalog.priceRange(p);
        String link = urls.absolute(request, StorefrontPages.productUrl(handle, p.getHandle()));
        StringBuilder b = new StringBuilder(1024);
        b.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"robots\" content=\"noindex\"><title>")
                .append(esc(p.getTitle())).append("</title></head><body>")
                .append("<div class=\"bazaar-buy\"><strong>").append(esc(p.getTitle()))
                .append("</strong> <span>").append(esc(store.getCurrency())).append(' ')
                .append(Money.plain(range[0], store.getCurrency())).append("</span> <a href=\"")
                .append(esc(link)).append("\" target=\"_top\" rel=\"noopener\">Buy now</a>")
                .append("</div></body></html>");
        // The one page another site may frame: its own policy, no scripts.
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange != null) {
            exchange.setAttribute(WebHeaders.OWN_POLICY, "1");
        }
        return Conditional.answer(request, "text/html; charset=utf-8",
                b.toString().getBytes("UTF-8"), "public, max-age=300", 0).header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; "
                        + "frame-ancestors *; base-uri 'none'");
    }

    /// The snippet the back office offers to copy for a product.
    public static String embedCode(String base, String handle, String productHandle) {
        return "<iframe src=\"" + base + StorefrontPages.storeUrl(handle) + "/embed/"
                + SiteUrls.encode(productHandle) + "\" width=\"320\" height=\"80\" "
                + "style=\"border:0\" loading=\"lazy\" title=\"Buy\"></iframe>";
    }

    // -------------------------------------------------------- back office

    /// Saves the store's search and pre-launch settings: synonym groups, the
    /// storefront password (empty opens the store), and per-product boosts
    /// as `product-handle=N` lines.
    @Transactional
    public void saveSettings(String storeId, String synonyms, String password, String boosts)
            throws IOException {
        saveSettings(storeId, synonyms, password, boosts, null);
    }

    /// As above, plus the store's custom domain (`domain`; null leaves it as
    /// it is, empty removes it). A value that is not a hostname, or that
    /// another store has proved, is refused with a 400.
    @Transactional
    public void saveSettings(String storeId, String synonyms, String password, String boosts,
            String domain) throws IOException {
        access.requireStaff(Caller.name(), storeId, StoreAccess.MANAGERS);
        Store store = stores.findById(storeId);
        if (domain != null) {
            // A new domain gets a new token and is not served until proved.
            DomainVerification.assign(stores, store, domain);
        }
        store.setSearchSynonyms(synonyms == null ? "" : synonyms.trim());
        store.setStorefrontPassword(password == null ? "" : password.trim());
        stores.save(store);
        List<Product> all = products.findByStoreIdOrderByTitleAsc(storeId);
        Map<String, Integer> wanted = boosts(boosts);
        for (int iter = 0; iter < all.size(); iter++) {
            Product p = all.get(iter);
            Integer n = wanted.get(p.getHandle());
            int boost = n == null ? 0 : n.intValue();
            if (boost != p.getSearchBoost()) {
                p.setSearchBoost(boost);
                products.save(p);
            }
        }
    }

    /// The boosted products as `handle=N` lines, for the editor.
    @Transactional(readOnly = true)
    public String boostText(String storeId) throws IOException {
        StringBuilder b = new StringBuilder();
        List<Product> all = products.findByStoreIdOrderByTitleAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).getSearchBoost() != 0) {
                b.append(all.get(iter).getHandle()).append('=')
                        .append(all.get(iter).getSearchBoost()).append('\n');
            }
        }
        return b.toString();
    }

    static Map<String, Integer> boosts(String text) {
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        if (text == null) {
            return out;
        }
        int start = 0;
        for (int iter = 0; iter <= text.length(); iter++) {
            if (iter == text.length() || text.charAt(iter) == '\n') {
                String line = text.substring(start, iter).trim();
                start = iter + 1;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    long n = parse(line.substring(eq + 1).trim());
                    if (n >= -100 && n <= 100) {
                        out.put(line.substring(0, eq).trim(), Integer.valueOf((int) n));
                    }
                }
            }
        }
        return out;
    }

    private static long parse(String s) {
        if (s.length() == 0 || s.length() > 4) {
            return Long.MIN_VALUE;
        }
        long n = 0;
        int at = s.charAt(0) == '-' ? 1 : 0;
        if (at == s.length()) {
            return Long.MIN_VALUE;
        }
        for (int iter = at; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return Long.MIN_VALUE;
            }
            n = n * 10 + (c - '0');
        }
        return at == 1 ? -n : n;
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            switch (c) {
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '&': b.append("&amp;"); break;
                case '"': b.append("&quot;"); break;
                case '\'': b.append("&#39;"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }
}
