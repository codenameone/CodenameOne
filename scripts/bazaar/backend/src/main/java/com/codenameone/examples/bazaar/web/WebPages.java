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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.Roles;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.WebHeaders;

import java.io.IOException;
import java.util.List;

/// Builds the [Shell] of a signed-in page and carries the one-line message a
/// form post leaves for the page it redirects to.
///
/// The sections of each area are tables here, and the same tables drive the
/// sidebar, the `g`+letter shortcuts and the test that walks every back-office
/// route as a member of another store (`WebAdminTest`), so a section added to
/// the sidebar is a section that test covers.
@Component
public class WebPages {
    /// Account sections: label, path, shortcut.
    public static final String[][] ACCOUNT = {
        {"Overview", "/account", "h"},
        {"Orders", "/account/orders", "o"},
        {"Subscriptions", "/account/subscriptions", "u"},
        {"Addresses", "/account/addresses", "a"},
        {"Wishlist", "/account/wishlist", "w"},
        {"Reviews", "/account/reviews", "r"},
        {"Loyalty", "/account/loyalty", "l"},
        {"Clubs", "/account/clubs", "c"},
        {"Stock alerts", "/account/alerts", "k"},
        {"Marketing", "/account/marketing", "m"},
        {"Security", "/account/security", "s"},
    };
    /// The account section only a company buyer sees: label, path, shortcut.
    public static final String[] BUYER = {"Company", "/account/company", "n"};
    static final String TOAST = "web.toast";

    private final Accounts accounts;
    private final Stores stores;
    private final WebSessions sessions;
    private final com.codenameone.examples.bazaar.trade.Trade trade;

    public WebPages(Accounts accounts, Stores stores, WebSessions sessions,
            com.codenameone.examples.bazaar.trade.Trade trade) {
        this.trade = trade;
        this.accounts = accounts;
        this.stores = stores;
        this.sessions = sessions;
    }

    /// A shell for a page nobody needs to be signed in to see (signing in).
    public static Shell anonymous(HttpServer.Request request, String title) {
        Shell s = new Shell();
        s.area = "auth";
        // The visitor's language (these pages are never shared-cached: each
        // carries its own form token).
        language(s, request);
        String heading = StoreText.translate(s.t, title);
        s.title = heading + " - Bazaar";
        s.heading = heading;
        s.nonce = WebHeaders.currentNonce();
        s.toast = takeToast(request);
        return s;
    }

    /// A shell for `area` with the caller's name and the area's sidebar.
    public Shell shell(HttpServer.Request request, String area, String title) throws IOException {
        Shell s = new Shell();
        s.area = area;
        s.title = title + " - Bazaar";
        s.heading = title;
        s.nonce = WebHeaders.currentNonce();
        String me = Caller.name();
        // Every signed-in page records its session, and refuses one that was
        // signed out from the security page.
        sessions.touch(request, me);
        s.signedIn = true;
        s.user = accounts.describe(me).displayName();
        s.merchant = Caller.hasRole(Roles.MERCHANT);
        s.admin = Caller.hasRole(Roles.ADMIN);
        String path = path(request);
        String[][] table = ACCOUNT;
        {
            for (int iter = 0; iter < table.length; iter++) {
                NavItem n = new NavItem(table[iter][0], table[iter][1], table[iter][2]);
                // The overview matches only itself, not every page under it.
                if (iter == 0) {
                    n.active = path.equals(n.url);
                    n.current = n.active ? "page" : "false";
                } else {
                    n.activeIf(path);
                }
                s.nav.add(n);
            }
            if ("account".equals(area) && !trade.buyerStores(me).isEmpty()) {
                NavItem n = new NavItem(BUYER[0], BUYER[1], BUYER[2]);
                n.activeIf(path);
                s.nav.add(n);
            }
            s.home = table[0][1];
        }
        s.toast = takeToast(request);
        if ("account".equals(area)) {
            // The reader's language, picked as the storefront picks it
            // (StoreText#forRequest); account pages are
            // no-store, so nothing about caching depends on it.
            language(s, request);
            s.title = StoreText.translate(s.t, title) + " - Bazaar";
            s.heading = StoreText.translate(s.t, title);
            s.toast = StoreText.translate(s.t, s.toast);
            for (int iter = 0; iter < s.nav.size(); iter++) {
                s.nav.get(iter).label = StoreText.translate(s.t, s.nav.get(iter).label);
            }
        }
        return s;
    }

    /// Sets the language of `s` from the shopper's choice, else their
    /// `Accept-Language`, and fills its picker.
    static void language(Shell s, HttpServer.Request request) {
        s.lang = StoreText.forRequest(request, false);
        s.dir = StoreText.rtl(s.lang) ? "rtl" : "ltr";
        s.t = StoreText.table(s.lang);
        s.langs = StoreText.options(s.lang);
        s.langBack = StoreText.back(request.getTarget());
    }

    /// Leaves `message` for the next page this session renders.
    public static void toast(HttpServer.Request request, String message) {
        HttpSession session = request.getSession(true);
        session.setAttribute(TOAST, message);
    }

    static String takeToast(HttpServer.Request request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "";
        }
        Object t = session.getAttribute(TOAST);
        if (t == null) {
            return "";
        }
        session.removeAttribute(TOAST);
        return t.toString();
    }

    static String path(HttpServer.Request request) {
        String t = request.getTarget();
        int q = t.indexOf('?');
        return q < 0 ? t : t.substring(0, q);
    }

    /// Puts `shell` in `model` and answers the view: the whole page, or only
    /// its `main` fragment for an htmx request that targets it.
    public static String view(Model model, Shell shell, String template) {
        model.addAttribute("shell", shell);
        return template;
    }
}
