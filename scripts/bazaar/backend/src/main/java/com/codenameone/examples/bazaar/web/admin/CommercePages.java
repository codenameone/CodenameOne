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
package com.codenameone.examples.bazaar.web.admin;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.trade.TradeDocumentPages;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;

/// The back office's subscriptions (list, actions, renewal calendar) and
/// B2B (companies with locations and buyers, price lists with quantity
/// rules, the quote board and net-terms invoices), drawn by
/// `templates/marketing.html` like [MarketingPages] and in the same shape:
/// every change a form post answered with 303 and a one-line message. A store
/// the caller does not work at is a 404 ([WebPages#admin] for pages,
/// [#member] for posts); the services decide the role, and a move the quote
/// pipeline does not allow comes back as its 409 message.
@Controller
public class CommercePages {
    private final WebPages pages;
    private final AdminData data;
    private final CommerceData commerce;
    private final WebSessions sessions;
    private final TradeDocumentPages documents;

    public CommercePages(WebPages pages, AdminData data, CommerceData commerce,
            WebSessions sessions, TradeDocumentPages documents) {
        this.pages = pages;
        this.data = data;
        this.commerce = commerce;
        this.sessions = sessions;
        this.documents = documents;
    }

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            MarketingView view, DataTable table) throws java.io.IOException {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        view.base = "/admin/" + storeId;
        if (view.heading.length() == 0) {
            view.heading = title;
        }
        view.hasTable = table != null;
        model.addAttribute("view", view);
        model.addAttribute("table", table == null ? new DataTable() : table);
        return WebPages.view(model, shell, "marketing");
    }

    private static MarketingView view(String page) {
        MarketingView v = new MarketingView();
        v.page = page;
        return v;
    }

    private void member(HttpServer.Request request, String storeId) throws Exception {
        sessions.touch(request, Caller.name());
        data.currency(storeId);
    }

    private interface Change {
        void run() throws Exception;
    }

    private interface Create {
        String run() throws Exception;
    }

    private static String change(HttpServer.Request request, String back, String done,
            Change c) throws Exception {
        try {
            c.run();
            WebPages.toast(request, done);
        } catch (ResponseStatusException e) {
            rethrow(request, e);
        }
        return "redirect:" + back;
    }

    private static String create(HttpServer.Request request, String rowBase, String back,
            String done, Create c) throws Exception {
        try {
            String id = c.run();
            WebPages.toast(request, done);
            return "redirect:" + rowBase + id;
        } catch (ResponseStatusException e) {
            rethrow(request, e);
            return "redirect:" + back;
        }
    }

    private static void rethrow(HttpServer.Request request, ResponseStatusException e) {
        int s = e.getStatus();
        if (s == 401 || s == 403 || s == 404) {
            throw e;
        }
        WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
    }

    private static String base(String storeId) {
        return "/admin/" + storeId;
    }

    // ======================================================== subscriptions

    @GetMapping("/admin/{storeId}/subscriptions")
    public String subscriptions(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("subscriptions");
        v.intro = "Every subscription a shopper started from a selling plan.";
        DataTable t = commerce.subscriptions(storeId, request, v);
        return render(model, request, storeId, "Subscriptions", v, t);
    }

    @GetMapping("/admin/{storeId}/subscriptions/{id}")
    public String subscription(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        MarketingView v = view("subscription");
        String title = commerce.subscription(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/subscriptions/{id}/{action}")
    public String changeSubscription(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, @PathVariable("action") String action,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/subscriptions/" + id, "Subscription updated",
                () -> commerce.changeSubscription(storeId, id, action));
    }

    @GetMapping("/admin/{storeId}/renewals")
    public String renewals(@PathVariable("storeId") String storeId,
            @RequestParam(value = "month", required = false) String month, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("renewals");
        commerce.renewals(storeId, month, v);
        return render(model, request, storeId, "Renewal calendar", v, null);
    }

    // ============================================================ companies

    @GetMapping("/admin/{storeId}/companies")
    public String companies(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("companies");
        v.intro = "Business customers: their prices, payment terms, locations and buyers.";
        DataTable t = commerce.companies(storeId, request, v);
        return render(model, request, storeId, "Companies", v, t);
    }

    @PostMapping("/admin/{storeId}/companies")
    public String createCompany(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/companies/", base(storeId) + "/companies",
                "Company added", () -> commerce.saveCompany(storeId, null, request));
    }

    @GetMapping("/admin/{storeId}/companies/{id}")
    public String company(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("company");
        String title = commerce.company(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/companies/{id}")
    public String saveCompany(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/companies/", base(storeId) + "/companies/" + id,
                "Company saved", () -> commerce.saveCompany(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/companies/{id}/sites")
    public String addSite(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/companies/" + id, "Location added",
                () -> commerce.addSite(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/companies/{id}/buyers")
    public String addBuyer(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/companies/" + id, "Buyer added",
                () -> commerce.addBuyer(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/companies/{id}/buyers/{buyer}/remove")
    public String removeBuyer(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, @PathVariable("buyer") String buyer,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/companies/" + id, "Buyer removed",
                () -> commerce.removeBuyer(storeId, id, buyer));
    }

    // ========================================================== price lists

    @GetMapping("/admin/{storeId}/price-lists")
    public String priceLists(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("price-lists");
        v.intro = "Company prices: a percentage off list, fixed prices and quantity rules.";
        DataTable t = commerce.priceLists(storeId, request, v);
        return render(model, request, storeId, "Price lists", v, t);
    }

    @PostMapping("/admin/{storeId}/price-lists")
    public String createPriceList(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/price-lists/", base(storeId) + "/price-lists",
                "Price list added", () -> commerce.savePriceList(storeId, null, request));
    }

    @GetMapping("/admin/{storeId}/price-lists/{id}")
    public String priceList(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        MarketingView v = view("price-list");
        String title = commerce.priceList(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/price-lists/{id}")
    public String savePriceList(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/price-lists/", base(storeId) + "/price-lists/"
                + id, "Price list saved", () -> commerce.savePriceList(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/price-lists/{id}/entries")
    public String putEntry(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/price-lists/" + id, "Rule saved",
                () -> commerce.putEntry(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/price-lists/{id}/entries/{entry}/remove")
    public String removeEntry(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, @PathVariable("entry") String entry,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/price-lists/" + id, "Rule removed",
                () -> commerce.removeEntry(storeId, id, entry));
    }

    // =============================================================== quotes

    @GetMapping("/admin/{storeId}/quotes")
    public String quotes(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("quotes");
        v.intro = "Send a draft, take a sent one back, order an accepted one. A buyer accepts "
                + "or declines what was sent.";
        commerce.quotes(storeId, v);
        return render(model, request, storeId, "Quotes", v, null);
    }

    @PostMapping("/admin/{storeId}/quotes/move")
    public String moveQuote(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        String quote = AdminForms.field(request, "quote");
        String to = AdminForms.field(request, "to");
        return change(request, base(storeId) + "/quotes", "Quote moved",
                () -> commerce.moveQuote(storeId, quote, to));
    }

    @GetMapping("/admin/{storeId}/quotes/{id}")
    public String quote(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("quote");
        String title = commerce.quote(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    // ============================================================= invoices

    @GetMapping("/admin/{storeId}/invoices")
    public String invoices(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("invoices");
        commerce.invoices(storeId, v);
        return render(model, request, storeId, "Invoices", v, null);
    }

    @PostMapping("/admin/{storeId}/invoices/{orderId}/paid")
    public String markPaid(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/invoices", "Marked paid",
                () -> commerce.markPaid(storeId, orderId));
    }

    /// The invoice of a net-terms order, with its due line: the same document
    /// the app's API serves, behind the back office's session.
    @GetMapping("/admin/{storeId}/invoices/{orderId}/invoice.pdf")
    public HttpServer.Response invoice(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return documents.invoice(storeId, orderId);
    }
}
