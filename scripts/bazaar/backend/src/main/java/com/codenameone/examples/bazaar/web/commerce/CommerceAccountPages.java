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
package com.codenameone.examples.bazaar.web.commerce;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.BulkOrderResultDto;
import com.codenameone.examples.bazaar.api.NetTermDto;
import com.codenameone.examples.bazaar.api.PaymentMethodDto;
import com.codenameone.examples.bazaar.api.SellingPlanDto;
import com.codenameone.examples.bazaar.api.SubscribeDto;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.subscribe.Subscriptions;
import com.codenameone.examples.bazaar.trade.Trade;
import com.codenameone.examples.bazaar.trade.TradeDocumentPages;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.StoreText;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;
import com.codenameone.examples.bazaar.web.table.Grid;
import java.io.IOException;
import java.util.List;

/// The shopper's side of subscriptions and B2B in the account: starting a
/// subscription from a product's selling plan, the subscriptions with skip,
/// pause, resume and cancel, and -- for a company buyer only -- the
/// company's prices, quotes, invoices, a quick-order form and a CSV upload.
///
/// It lives beside the back-office pages because it draws with the same
/// template (`marketing.html`) and form helpers, in the account's shell: the
/// signed-in chain's CSRF, nonce CSP and `no-store` all apply. Another
/// shopper's subscription or another company's quote is the services' 404;
/// somebody who buys for no company gets [Trade#buyer]'s 403 on every
/// company page.
@Controller
public class CommerceAccountPages {
    /// Rows of the quick-order form.
    static final int QUICK_ROWS = 8;

    private final WebPages pages;
    private final WebSessions sessions;
    private final Subscriptions subscriptions;
    private final Trade trade;
    private final Payments payments;
    private final Stores stores;
    private final StoreRepository storeRows;
    private final CustomerRepository customers;
    private final CustomerAddressRepository addresses;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final TradeDocumentPages documents;

    public CommerceAccountPages(WebPages pages, WebSessions sessions, Subscriptions subscriptions,
            Trade trade, Payments payments, Stores stores, StoreRepository storeRows,
            CustomerRepository customers, CustomerAddressRepository addresses,
            ProductRepository products, VariantRepository variants,
            TradeDocumentPages documents) {
        this.pages = pages;
        this.sessions = sessions;
        this.subscriptions = subscriptions;
        this.trade = trade;
        this.payments = payments;
        this.stores = stores;
        this.storeRows = storeRows;
        this.customers = customers;
        this.addresses = addresses;
        this.products = products;
        this.variants = variants;
        this.documents = documents;
    }

    private String render(Model model, HttpServer.Request request, String title,
            MarketingView view, DataTable table) throws IOException {
        Shell shell = pages.shell(request, "account", title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        if (view.heading.length() == 0) {
            view.heading = title;
        }
        view.hasTable = table != null;
        localize(view, table, shell.t);
        model.addAttribute("view", view);
        model.addAttribute("table", table == null ? new DataTable() : table);
        return WebPages.view(model, shell, "marketing");
    }

    /// The shopper's interface text ([StoreText#forRequest]).
    private static java.util.Map<String, String> t(HttpServer.Request request) {
        return StoreText.table(StoreText.forRequest(request, false));
    }

    /// The fixed English these pages wrote -- headings, labels, buttons,
    /// statuses, messages -- in the shopper's language ([StoreText#translate]
    /// leaves anything that is not one of the table's texts, such as a
    /// product or company name, as it is). The back office draws the same
    /// template and stays English.
    static void localize(MarketingView v, DataTable table, java.util.Map<String, String> t) {
        v.heading = StoreText.translate(t, v.heading);
        v.intro = StoreText.translate(t, v.intro);
        v.backLabel = StoreText.translate(t, v.backLabel);
        v.cardsTitle = StoreText.translate(t, v.cardsTitle);
        v.rowsTitle = StoreText.translate(t, v.rowsTitle);
        v.rowsEmpty = StoreText.translate(t, v.rowsEmpty);
        rows(v.links, t);
        rows(v.facts, t);
        rows(v.problems, t);
        rows(v.rows, t);
        for (int iter = 0; iter < v.forms.size(); iter++) {
            FormView f = v.forms.get(iter);
            f.title = StoreText.translate(t, f.title);
            f.text = StoreText.translate(t, f.text);
            f.submit = StoreText.translate(t, f.submit);
            f.confirm = StoreText.translate(t, f.confirm);
            for (int at = 0; at < f.fields.size(); at++) {
                f.fields.get(at).label = StoreText.translate(t, f.fields.get(at).label);
                f.fields.get(at).hint = StoreText.translate(t, f.fields.get(at).hint);
            }
        }
        for (int iter = 0; iter < v.cards.size(); iter++) {
            ActionCard k = v.cards.get(iter);
            k.title = StoreText.translate(t, k.title);
            k.status = StoreText.translate(t, k.status);
            k.detail = StoreText.translate(t, k.detail);
            k.extraLabel = StoreText.translate(t, k.extraLabel);
            rows(k.buttons, t);
        }
        if (table != null) {
            table.searchLabel = StoreText.translate(t, table.searchLabel);
            table.empty = StoreText.translate(t, table.empty);
            for (int iter = 0; iter < table.columns.size(); iter++) {
                table.columns.get(iter).label = StoreText.translate(t,
                        table.columns.get(iter).label);
            }
        }
    }

    private static void rows(List<AdminRow> rows, java.util.Map<String, String> t) {
        for (int iter = 0; iter < rows.size(); iter++) {
            AdminRow r = rows.get(iter);
            r.title = StoreText.translate(t, r.title);
            r.status = StoreText.translate(t, r.status);
        }
    }

    private static MarketingView view(String page) {
        MarketingView v = new MarketingView();
        v.page = page;
        return v;
    }

    private String me(HttpServer.Request request) throws IOException {
        String me = Caller.name();
        sessions.touch(request, me);
        return me;
    }

    private static String f(HttpServer.Request request, String name) {
        return AdminForms.field(request, name);
    }

    private interface Change {
        void run() throws Exception;
    }

    /// As in the account's other pages: a refusal the shopper can act on is
    /// the message; 401, 403 and 404 answer their status.
    private static String change(HttpServer.Request request, String back, String done,
            Change c) throws Exception {
        try {
            c.run();
            WebPages.toast(request, done);
        } catch (ResponseStatusException e) {
            int s = e.getStatus();
            if (s == 401 || s == 403 || s == 404) {
                throw e;
            }
            WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
        }
        return "redirect:" + back;
    }

    private static AdminRow fact(String title, String detail) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.detail = detail;
        return r;
    }

    private static AdminRow link(String title, String url) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.url = url;
        return r;
    }

    private String storeName(String storeId) {
        Store s = storeRows.findById(storeId);
        return s == null ? "" : s.getName();
    }

    private String itemName(String variantId) {
        Variant v = variantId == null ? null : variants.findById(variantId);
        if (v == null) {
            return "Item";
        }
        Product p = products.findById(v.getProductId());
        String t = p == null ? "Item" : p.getTitle();
        return v.getTitle().length() > 0 ? t + " - " + v.getTitle() : t;
    }

    // ======================================================== subscriptions

    @GetMapping("/account/subscriptions")
    public String subscriptions(Model model, HttpServer.Request request) throws Exception {
        String me = me(request);
        java.util.Map<String, String> words = t(request);
        MarketingView v = view("subscriptions");
        v.cardsTitle = "Your subscriptions";
        List<Customer> mine = customers.findByUsername(me);
        for (int iter = 0; iter < mine.size(); iter++) {
            String storeId = mine.get(iter).getStoreId();
            List<SubscriptionDto> subs = subscriptions.mine(me, storeId);
            for (int s = 0; s < subs.size(); s++) {
                SubscriptionDto d = subs.get(s);
                ActionCard k = CommerceData.actions(d, "/account/subscriptions/" + storeId + "/"
                        + d.id());
                k.title = itemName(d.variantId()) + " x " + d.quantity();
                k.status = AdminData.words(d.status());
                k.detail = StoreText.format(words, "a.subDetail", storeName(storeId),
                        CommerceData.cadence(d.unit(), d.cadence()),
                        CommerceData.percent(d.discountBps()));
                k.meta = Subscriptions.CANCELLED.equals(d.status()) ? ""
                        : Subscriptions.PAUSED.equals(d.status()) ? words.get("a.paused")
                        : StoreText.format(words, "a.nextDelivery", AdminData.date(d.dueAt()));
                v.cards.add(k);
            }
        }
        if (v.cards.isEmpty()) {
            v.intro = "No subscriptions yet. Choose \"Subscribe\" on a product that offers it.";
        }
        return render(model, request, "Subscriptions", v, null);
    }

    @PostMapping("/account/subscriptions/{store}/{id}/{action}")
    public String changeSubscription(@PathVariable("store") String storeId,
            @PathVariable("id") String id, @PathVariable("action") String action,
            HttpServer.Request request) throws Exception {
        String me = me(request);
        return change(request, "/account/subscriptions", "Subscription updated",
                () -> subscriptions.changeMine(me, storeId, id, action));
    }

    /// The subscription checkout: the plan, item and quantity the product
    /// page chose, with a saved card and address to pick. The first delivery
    /// is ordered and charged when it is confirmed.
    @GetMapping("/account/subscriptions/start")
    public String start(Model model, HttpServer.Request request) throws Exception {
        String me = me(request);
        Store store = stores.activeStore(f(request, "shop"));
        String planId = f(request, "plan");
        SellingPlanDto plan = null;
        Variant variant = variants.findById(f(request, "variant"));
        if (variant != null) {
            List<SellingPlanDto> offered = subscriptions.productPlans(store.getId(),
                    variant.getProductId());
            for (int iter = 0; iter < offered.size(); iter++) {
                if (offered.get(iter).id().equals(planId)) {
                    plan = offered.get(iter);
                }
            }
        }
        if (plan == null || !store.getId().equals(variant.getStoreId())) {
            throw new ResponseStatusException(404, "No such plan");
        }
        int qty = AdminForms.number(f(request, "quantity"), 1, "the quantity");
        MarketingView v = view("subscribe");
        long unit = variant.getPriceCents() - variant.getPriceCents() * plan.discountBps() / 10000;
        v.back = "/s/" + store.getHandle();
        v.backLabel = store.getName();
        v.facts.add(fact("Item", itemName(variant.getId()) + " x " + qty));
        v.facts.add(fact("Delivers", CommerceData.cadence(plan.unit(), plan.cadence())));
        v.facts.add(fact("Each delivery", Money.format(unit * qty, store.getCurrency())
                + " (saves " + CommerceData.percent(plan.discountBps()) + "%)"));
        FormView form = new FormView("subscribe", "Subscribe", "/account/subscriptions/start",
                "Subscribe and pay the first delivery");
        form.text = "Skip, pause or cancel any time from Subscriptions.";
        form.add("shop", "", "hidden", store.getHandle());
        form.add("plan", "", "hidden", plan.id());
        form.add("variant", "", "hidden", variant.getId());
        form.add("quantity", "", "hidden", Integer.toString(qty));
        List<PaymentMethodDto> cards = payments.methods(me);
        FormField card = form.add("card", "Card", "select", "");
        for (int iter = 0; iter < cards.size(); iter++) {
            card.option(cards.get(iter).id(), cards.get(iter).label());
        }
        if (cards.isEmpty()) {
            v.intro = "A subscription is paid by a saved card, and this account has none yet. "
                    + "Save one at checkout, then come back.";
        }
        List<CustomerAddress> saved = addresses.findByUsername(me);
        FormField to = form.add("address", "Deliver to", "select", "");
        for (int iter = 0; iter < saved.size(); iter++) {
            CustomerAddress a = saved.get(iter);
            to.option(a.getId(), a.getName() + ", " + a.getLine1() + ", " + a.getCity());
            if (a.isIsDefault()) {
                to.value = a.getId();
            }
        }
        CommerceData.selectChosen(form);
        if (!cards.isEmpty()) {
            v.forms.add(form);
        }
        return render(model, request, "Subscribe", v, null);
    }

    @PostMapping("/account/subscriptions/start")
    public String subscribe(HttpServer.Request request) throws Exception {
        String me = me(request);
        Store store = stores.activeStore(f(request, "shop"));
        AddressDto address = null;
        List<CustomerAddress> saved = addresses.findByUsername(me);
        String chosen = f(request, "address");
        for (int iter = 0; iter < saved.size(); iter++) {
            CustomerAddress a = saved.get(iter);
            if (a.getId().equals(chosen)) {
                address = new AddressDto(a.getName(), a.getLine1(), a.getLine2(), a.getCity(),
                        a.getRegion(), a.getPostal(), a.getCountry(), a.getPhone(), a.getLat(),
                        a.getLng());
            }
        }
        final SubscribeDto in = new SubscribeDto(f(request, "plan"), f(request, "variant"),
                AdminForms.number(f(request, "quantity"), 1, "the quantity"),
                f(request, "card").length() == 0 ? null : f(request, "card"), address);
        try {
            subscriptions.subscribe(me, store.getId(), in);
        } catch (ResponseStatusException e) {
            if (e.getStatus() == 401 || e.getStatus() == 403 || e.getStatus() == 404) {
                throw e;
            }
            WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
            return "redirect:/account/subscriptions/start?shop=" + store.getHandle() + "&plan="
                    + enc(in.planId()) + "&variant=" + enc(in.variantId()) + "&quantity="
                    + in.quantity();
        }
        WebPages.toast(request, "Subscribed. The first delivery is on its way.");
        return "redirect:/account/subscriptions";
    }

    private static String enc(String s) {
        return com.codenameone.examples.bazaar.web.SiteUrls.encode(s == null ? "" : s);
    }

    // ============================================================== company

    /// The stores the shopper buys in for a company; straight to the one
    /// when there is only one. 403 for somebody who buys for none.
    @GetMapping("/account/company")
    public String companies(Model model, HttpServer.Request request) throws Exception {
        String me = me(request);
        List<String> in = trade.buyerStores(me);
        if (in.isEmpty()) {
            throw new ResponseStatusException(403, "You do not buy for a company");
        }
        if (in.size() == 1) {
            return "redirect:/account/company/" + in.get(0);
        }
        MarketingView v = view("companies");
        for (int iter = 0; iter < in.size(); iter++) {
            v.links.add(link(storeName(in.get(iter)), "/account/company/" + in.get(iter)));
        }
        v.intro = "You buy for a company in these stores.";
        return render(model, request, "Company", v, null);
    }

    @GetMapping("/account/company/{store}")
    public String company(@PathVariable("store") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("company");
        DataTable t = overview(me(request), storeId, request, v);
        return render(model, request, v.heading, v, t);
    }

    /// The company's terms, its price sheet, the quick-order form and the
    /// CSV upload.
    private DataTable overview(String me, String storeId, HttpServer.Request request,
            MarketingView v) throws IOException {
        Trade.BuyerTerms terms = trade.myTerms(me, storeId);
        final Store store = storeRows.findById(storeId);
        final String cur = store.getCurrency();
        String base = "/account/company/" + storeId;
        v.heading = terms.companyName;
        v.intro = StoreText.format(t(request), "a.buyingAt", store.getName(), terms.companyName);
        v.links.add(link("Quotes", base + "/quotes"));
        v.links.add(link("Invoices", base + "/invoices"));
        v.facts.add(fact("Payment terms", CommerceData.terms(terms.termsDays)));
        v.facts.add(fact("Price list", terms.priceListName.length() == 0 ? "List prices"
                : terms.priceListName));
        if (terms.percentOffBps > 0) {
            v.facts.add(fact("Everything else", StoreText.format(t(request), "a.pctOff",
                    CommerceData.percent(terms.percentOffBps))));
        }
        FormView quick = new FormView("quick", "Quick order", base + "/quick-order",
                "Request a quote");
        quick.text = "Type SKUs and quantities. The store confirms the quote at your prices; "
                + "every line is checked against your quantity rules.";
        for (int iter = 1; iter <= QUICK_ROWS; iter++) {
            quick.add("sku" + iter, "SKU " + iter, "text", f(request, "sku" + iter));
            quick.add("qty" + iter, "Quantity " + iter, "number", f(request, "qty" + iter));
        }
        v.forms.add(quick);
        FormView csv = new FormView("csv", "Bulk order from CSV", base + "/upload",
                "Upload order");
        csv.text = "One sku,quantity row per line; a first row of sku,quantity is a header.";
        csv.add("csv", "CSV rows", "textarea", f(request, "csv"));
        v.forms.add(csv);
        Grid<Trade.BuyerPrice> g = new Grid<Trade.BuyerPrice>("prices")
                .col("item", "Item", p -> p.title).sortBy(p -> p.title)
                .col("sku", "SKU", p -> p.sku)
                .col("list", "List price", p -> Money.format(p.listCents, cur)).num()
                .col("yours", "Your price", p -> Money.format(p.unitCents, cur)).num()
                    .sortBy(p -> Long.valueOf(p.unitCents))
                .col("rule", "Quantity rule", p -> p.rule)
                .rows(p -> p.variantId, p -> "")
                .defaultSort("item", false);
        DataTable t = g.table(terms.prices, Grid.Query.of(request), base);
        t.csvUrl = "";
        t.searchLabel = "Search your prices";
        t.empty = "No item has a price of its own on your list.";
        return t;
    }

    @PostMapping("/account/company/{store}/quick-order")
    public String quickOrder(@PathVariable("store") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        String me = me(request);
        StringBuilder csv = new StringBuilder();
        String typo = null;
        for (int iter = 1; iter <= QUICK_ROWS; iter++) {
            String sku = f(request, "sku" + iter);
            String qty = f(request, "qty" + iter);
            if (sku.length() == 0 && qty.length() == 0) {
                continue;
            }
            // The rows become CSV for Trade#bulkOrder, so a separator typed
            // into a cell would split it; no SKU has one.
            if (sku.indexOf(',') >= 0 || sku.indexOf('"') >= 0 || qty.indexOf(',') >= 0) {
                typo = "Row " + iter + ": a SKU or quantity cannot hold a comma or a quote";
                break;
            }
            csv.append(sku).append(',').append(qty).append('\n');
        }
        if (typo == null && csv.length() == 0) {
            typo = "Type at least one SKU and quantity";
        }
        if (typo != null) {
            MarketingView v = view("company");
            DataTable t = overview(me, storeId, request, v);
            v.problems.add(fact(typo, ""));
            return render(model, request, v.heading, v, t);
        }
        return bulk(me, storeId, csv.toString(), model, request);
    }

    @PostMapping("/account/company/{store}/upload")
    public String upload(@PathVariable("store") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        String me = me(request);
        String csv;
        try {
            csv = request.param("csv");
        } catch (RuntimeException malformed) {
            csv = null;
        }
        return bulk(me, storeId, csv == null ? "" : csv, model, request);
    }

    /// Runs rows through [Trade#bulkOrder]: a quote when every line passed,
    /// otherwise the page again with a table of every refused line.
    private String bulk(String me, String storeId, String csv, Model model,
            HttpServer.Request request) throws Exception {
        BulkOrderResultDto r = trade.bulkOrder(me, storeId, csv);
        if (r.errors() != null && !r.errors().isEmpty()) {
            MarketingView v = view("company");
            DataTable t = overview(me, storeId, request, v);
            CommerceData.csvErrors(r.errors(), v);
            v.problems.add(fact("Nothing was ordered: " + r.errors().size()
                    + (r.errors().size() == 1 ? " line needs" : " lines need")
                    + " fixing.", ""));
            return render(model, request, v.heading, v, t);
        }
        WebPages.toast(request, "Quote requested for " + r.accepted()
                + (r.accepted() == 1 ? " line" : " lines"));
        return "redirect:/account/company/" + storeId + "/quotes/" + r.quote().id();
    }

    @GetMapping("/account/company/{store}/quotes")
    public String quotes(@PathVariable("store") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        String me = me(request);
        List<TradeQuoteDto> rows = trade.myQuotes(me, storeId);
        MarketingView v = view("my-quotes");
        String base = "/account/company/" + storeId;
        v.back = base;
        v.backLabel = "Company";
        v.cardsTitle = "Quotes";
        for (int iter = 0; iter < rows.size(); iter++) {
            TradeQuoteDto q = rows.get(iter);
            ActionCard k = quoteCard(q, base + "/quotes/" + q.id());
            v.cards.add(k);
        }
        if (rows.isEmpty()) {
            v.intro = "No quotes yet. Use the quick order or a CSV upload to ask for one.";
        }
        return render(model, request, "Quotes", v, null);
    }

    /// A quote with the moves the pipeline allows a buyer
    /// ([Trade#allowed]): accept or decline what was sent, order what was
    /// accepted.
    private static ActionCard quoteCard(TradeQuoteDto q, String url) {
        ActionCard k = new ActionCard();
        k.title = "Quote " + CommerceData.shortId(q.id());
        k.url = url;
        k.status = AdminData.words(q.status());
        k.amount = Money.format(q.totalCents(), q.currency());
        k.meta = AdminData.date(q.updatedAt());
        k.detail = q.lines() == null ? "" : q.lines().size()
                + (q.lines().size() == 1 ? " line" : " lines");
        String[][] moves = {{Trade.ACCEPTED, "Accept"}, {Trade.ORDERED, "Place the order"},
            {Trade.DECLINED, "Decline"}};
        for (int iter = 0; iter < moves.length; iter++) {
            if (Trade.allowed(q.status(), moves[iter][0], false)) {
                k.button(moves[iter][1], url + "/" + moves[iter][0], Trade.DECLINED.equals(
                        moves[iter][0]));
            }
        }
        return k;
    }

    @GetMapping("/account/company/{store}/quotes/{id}")
    public String quote(@PathVariable("store") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        String me = me(request);
        TradeQuoteDto q = null;
        List<TradeQuoteDto> rows = trade.myQuotes(me, storeId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).id().equals(id)) {
                q = rows.get(iter);
            }
        }
        if (q == null) {
            throw new ResponseStatusException(404, "No such quote");
        }
        String base = "/account/company/" + storeId;
        MarketingView v = view("my-quote");
        v.back = base + "/quotes";
        v.backLabel = "Quotes";
        CommerceData.quoteFacts(q, v);
        ActionCard k = quoteCard(q, base + "/quotes/" + q.id());
        if (!k.buttons.isEmpty()) {
            k.url = "";
            k.title = "Your answer";
            v.cards.add(k);
        }
        return render(model, request, StoreText.format(t(request), "a.quoteN",
                CommerceData.shortId(q.id())), v, null);
    }

    @PostMapping("/account/company/{store}/quotes/{id}/{to}")
    public String moveQuote(@PathVariable("store") String storeId, @PathVariable("id") String id,
            @PathVariable("to") String to, HttpServer.Request request) throws Exception {
        String me = me(request);
        return change(request, "/account/company/" + storeId + "/quotes/" + id, "Quote updated",
                () -> trade.moveMyQuote(me, storeId, id, to));
    }

    @GetMapping("/account/company/{store}/invoices")
    public String invoices(@PathVariable("store") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        String me = me(request);
        List<NetTermDto> rows = trade.myNetTerms(me, storeId);
        Store store = storeRows.findById(storeId);
        String base = "/account/company/" + storeId;
        MarketingView v = view("my-invoices");
        v.back = base;
        v.backLabel = "Company";
        v.cardsTitle = "Invoices";
        for (int iter = 0; iter < rows.size(); iter++) {
            NetTermDto t = rows.get(iter);
            v.cards.add(CommerceData.invoiceCard(t, store.getCurrency(), base + "/invoices/"
                    + t.orderId() + "/invoice.pdf"));
        }
        if (rows.isEmpty()) {
            v.intro = "No invoices yet. Orders on net terms show here until they are paid.";
        }
        return render(model, request, "Invoices", v, null);
    }

    @GetMapping("/account/company/{store}/invoices/{orderId}/invoice.pdf")
    public HttpServer.Response invoice(@PathVariable("store") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request)
            throws Exception {
        me(request);
        return documents.buyerInvoice(storeId, orderId);
    }
}
