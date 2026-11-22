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
package com.codenameone.examples.bazaar.web.account;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.web.NotFoundPage;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.StoreText;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.security.PageEtags;

/// The signed-in shopper's account: orders, addresses, wishlist, reviews,
/// loyalty, clubs, stock alerts and marketing preferences. The security page is
/// [AccountSecurityPages].
///
/// Plain HTML first: every link is a page and every change a form post answered
/// with 303 to the page it came from and a one-line message, so the area works
/// with scripts off. The shell boosts links and forms with htmx when scripts
/// run. These paths are in the web app's signed-in chain (`WebSecurity`): CSRF
/// on every post, a nonce CSP, and nothing here may be stored by a cache
/// (`no-store`, see [#render]). Another shopper's order, address, review or
/// alert answers the 404 page ([AccountData]).
@Controller
public class AccountPages {
    private final WebPages pages;
    private final AccountData data;
    private final WebSessions sessions;

    public AccountPages(WebPages pages, AccountData data, WebSessions sessions) {
        this.pages = pages;
        this.data = data;
        this.sessions = sessions;
    }

    /// Draws `view` in the shell; marks the response `no-store`, since every
    /// account page carries personal data.
    static String render(WebPages pages, Model model, HttpServer.Request request, String title,
            AccountView view) throws java.io.IOException {
        Shell shell = pages.shell(request, "account", title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        localize(view, shell.t);
        model.addAttribute("view", view);
        return WebPages.view(model, shell, "account");
    }

    /// The words [AccountData] wrote in English -- statuses, "Order #12",
    /// "Page 2", timeline events -- in the shopper's language. Names, dates
    /// and amounts are left as they are.
    static void localize(AccountView v, java.util.Map<String, String> t) {
        java.util.List<java.util.List<AccountItem>> lists =
                new java.util.ArrayList<java.util.List<AccountItem>>();
        lists.add(v.items);
        lists.add(v.events);
        lists.add(v.tracking);
        lists.add(v.returns);
        lists.add(v.sessions);
        lists.add(java.util.Collections.singletonList(v.order));
        for (int l = 0; l < lists.size(); l++) {
            java.util.List<AccountItem> items = lists.get(l);
            for (int iter = 0; iter < items.size(); iter++) {
                AccountItem i = items.get(iter);
                i.title = StoreText.translate(t, i.title);
                i.status = status(t, i.status);
            }
        }
        v.pageLabel = StoreText.translate(t, v.pageLabel);
    }

    /// A status as a word: "Processing" as written, or a service's
    /// `REQUESTED` read as "Requested" -- kept as it was when the table has
    /// no other language's word for it.
    static String status(java.util.Map<String, String> t, String s) {
        String done = StoreText.translate(t, s);
        if (!done.equals(s) || s == null || s.length() < 2) {
            return done;
        }
        StringBuilder word = new StringBuilder();
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c == '_') {
                word.append(' ');
            } else if (iter > 0 && c >= 'A' && c <= 'Z') {
                word.append((char) (c - 'A' + 'a'));
            } else {
                word.append(c);
            }
        }
        String english = word.toString();
        String other = StoreText.translate(t, english);
        return other.equals(english) ? s : other;
    }

    private static AccountView view(String page) {
        AccountView v = new AccountView();
        v.page = page;
        return v;
    }

    /// The signed-in name, after checking this session was not signed out from
    /// elsewhere (a post renders no shell, so it checks here).
    private String me(HttpServer.Request request) throws java.io.IOException {
        String me = Caller.name();
        sessions.touch(request, me);
        return me;
    }

    /// A change that went through, or one the service refused for a reason the
    /// shopper can act on; a 404 still answers the 404 page.
    private interface Change {
        void run() throws Exception;
    }

    private static String change(HttpServer.Request request, String back, String done,
            Change c) throws Exception {
        try {
            c.run();
            WebPages.toast(request, done);
        } catch (ResponseStatusException e) {
            if (e.getStatus() == 404 || e.getStatus() == 401) {
                throw e;
            }
            WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
        }
        return "redirect:" + back;
    }

    @GetMapping("/account")
    public String home(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("home");
        data.orders(Caller.name(), 0, v);
        while (v.items.size() > 3) {
            v.items.remove(v.items.size() - 1);
        }
        return render(pages, model, request, "Your account", v);
    }

    // --------------------------------------------------------------- orders

    @GetMapping("/account/orders")
    public String orders(@RequestParam(value = "p", required = false) String p, Model model,
            HttpServer.Request request) throws Exception {
        AccountView v = view("orders");
        data.orders(Caller.name(), AccountData.number(p), v);
        return render(pages, model, request, "Orders", v);
    }

    @GetMapping("/account/orders/{id}")
    public String order(@PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        AccountView v = view("order");
        data.order(Caller.name(), id, v);
        return render(pages, model, request, v.order.title, v);
    }

    @GetMapping("/account/orders/{id}/invoice.pdf")
    public HttpServer.Response invoice(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        return data.invoice(me(request), id);
    }

    @PostMapping("/account/orders/{id}/cancel")
    public String cancel(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        final String me = me(request);
        return change(request, "/account/orders/" + id, "Order cancelled", () -> data.cancel(me, id));
    }

    @PostMapping("/account/orders/{id}/return")
    public String requestReturn(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        final String me = me(request);
        return change(request, "/account/orders/" + id, "Return requested",
                () -> data.requestReturn(me, id, request));
    }

    /// What the return posted would come to -- the goods' value, the fees the
    /// store's rules take, an exchange's price and the difference -- shown on
    /// the order page with the form as posted, before anything is asked for.
    @PostMapping("/account/orders/{id}/return/quote")
    public String quoteReturn(@PathVariable("id") String id, Model model,
            HttpServer.Request request) throws Exception {
        String me = me(request);
        AccountView v = view("order");
        data.order(me, id, v);
        data.quoteReturn(me, id, request, v);
        for (int iter = 0; iter < v.lines.size(); iter++) {
            AccountItem l = v.lines.get(iter);
            l.chosen = Integer.toString(AccountData.number(request.param("qty-" + l.id)));
        }
        return render(pages, model, request, v.order.title, v);
    }

    // ------------------------------------------------------------ addresses

    @GetMapping("/account/addresses")
    public String addresses(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("addresses");
        data.addresses(v);
        return render(pages, model, request, "Addresses", v);
    }

    @PostMapping("/account/addresses")
    public String addAddress(HttpServer.Request request) throws Exception {
        me(request);
        return change(request, "/account/addresses", "Address saved",
                () -> data.addAddress(request));
    }

    @PostMapping("/account/addresses/{id}")
    public String updateAddress(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        me(request);
        return change(request, "/account/addresses", "Address saved",
                () -> data.updateAddress(id, request));
    }

    @PostMapping("/account/addresses/{id}/delete")
    public String removeAddress(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        me(request);
        return change(request, "/account/addresses", "Address removed",
                () -> data.removeAddress(id));
    }

    @PostMapping("/account/addresses/{id}/default")
    public String defaultAddress(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        me(request);
        return change(request, "/account/addresses", "Default address changed",
                () -> data.defaultAddress(id));
    }

    // ------------------------------------------------------------- wishlist

    @GetMapping("/account/wishlist")
    public String wishlist(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("wishlist");
        data.wishlist(v);
        return render(pages, model, request, "Wishlist", v);
    }

    @PostMapping("/account/wishlist/{product}/remove")
    public String unwish(@PathVariable("product") String product, HttpServer.Request request)
            throws Exception {
        me(request);
        return change(request, "/account/wishlist", "Removed from your wishlist",
                () -> data.unwish(product));
    }

    // -------------------------------------------------------------- reviews

    @GetMapping("/account/reviews")
    public String reviews(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("reviews");
        data.reviews(Caller.name(), v);
        return render(pages, model, request, "Your reviews", v);
    }

    @PostMapping("/account/reviews/{id}")
    public String editReview(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        final String me = me(request);
        return change(request, "/account/reviews", "Review saved",
                () -> data.editReview(me, id, request));
    }

    @PostMapping("/account/reviews/{id}/delete")
    public String deleteReview(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        final String me = me(request);
        return change(request, "/account/reviews", "Review deleted",
                () -> data.deleteReview(me, id));
    }

    // -------------------------------------------------------------- loyalty

    @GetMapping("/account/loyalty")
    public String loyalty(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("loyalty");
        data.loyalty(Caller.name(), request, v);
        return render(pages, model, request, "Loyalty", v);
    }

    @PostMapping("/account/loyalty/{store}/join")
    public String join(@PathVariable("store") String store, HttpServer.Request request)
            throws Exception {
        final String me = me(request);
        return change(request, "/account/loyalty", "Welcome to the programme",
                () -> data.join(me, store));
    }

    // ---------------------------------------------------------------- clubs

    @GetMapping("/account/clubs")
    public String clubs(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("clubs");
        data.clubs(Caller.name(), v);
        return render(pages, model, request, "Clubs", v);
    }

    // --------------------------------------------------------- stock alerts

    @GetMapping("/account/alerts")
    public String alerts(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("alerts");
        data.alerts(Caller.name(), v);
        return render(pages, model, request, "Stock alerts", v);
    }

    @PostMapping("/account/alerts/{id}/delete")
    public String deleteAlert(@PathVariable("id") String id, HttpServer.Request request)
            throws Exception {
        final String me = me(request);
        return change(request, "/account/alerts", "Alert removed", () -> data.deleteAlert(me, id));
    }

    // ------------------------------------------------------------ marketing

    @GetMapping("/account/marketing")
    public String marketing(Model model, HttpServer.Request request) throws Exception {
        AccountView v = view("marketing");
        data.marketing(Caller.name(), v);
        return render(pages, model, request, "Marketing preferences", v);
    }

    @PostMapping("/account/marketing/{store}")
    public String saveMarketing(@PathVariable("store") String store, HttpServer.Request request)
            throws Exception {
        final String me = me(request);
        final boolean email = "on".equals(request.param("email"));
        final boolean sms = "on".equals(request.param("sms"));
        return change(request, "/account/marketing", "Preferences saved",
                () -> data.saveMarketing(me, store, email, sms));
    }

    /// A missing (or someone else's) order, address, review or alert answers the
    /// 404 page; anything else its own status.
    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws java.io.IOException {
        return refusal(e);
    }

    static HttpServer.Response refusal(ResponseStatusException e) throws java.io.IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        String reason = e.getReason() == null ? "" : e.getReason();
        return HttpServer.Response.text(e.getStatus(), reason).header("Cache-Control", "no-store");
    }
}
