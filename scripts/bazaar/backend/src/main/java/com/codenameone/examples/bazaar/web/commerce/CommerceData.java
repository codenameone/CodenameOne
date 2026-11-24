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
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.api.CompanyDto;
import com.codenameone.examples.bazaar.api.CompanySiteDto;
import com.codenameone.examples.bazaar.api.CsvErrorDto;
import com.codenameone.examples.bazaar.api.NetTermDto;
import com.codenameone.examples.bazaar.api.PriceListDto;
import com.codenameone.examples.bazaar.api.PriceListEntryDto;
import com.codenameone.examples.bazaar.api.RenewalDto;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.api.TradeQuoteLineDto;
import com.codenameone.examples.bazaar.api.VatCheckDto;
import com.codenameone.examples.bazaar.subscribe.Subscriptions;
import com.codenameone.examples.bazaar.trade.Trade;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.table.DataTable;
import com.codenameone.examples.bazaar.web.table.Grid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Builds the subscription and B2B pages of the back office and of the
/// account from [Subscriptions] and [Trade]: every rule (who may move a
/// quote where, what a quantity rule allows, which subscription may be
/// skipped) is the services', and this class only lays their answers out
/// and turns form fields into their DTOs.
@Component
public class CommerceData {
    /// The quote pipeline's columns, in order: status and heading.
    static final String[][] QUOTE_COLUMNS = {
        {Trade.DRAFT, "Draft"},
        {Trade.SENT, "Sent"},
        {Trade.ACCEPTED, "Accepted"},
        {Trade.ORDERED, "Ordered"},
        {Trade.DECLINED, "Declined"},
    };

    private final Subscriptions subscriptions;
    private final Trade trade;
    private final AdminData admin;
    private final MarketingData marketing;

    public CommerceData(Subscriptions subscriptions, Trade trade, AdminData admin,
            MarketingData marketing) {
        this.subscriptions = subscriptions;
        this.trade = trade;
        this.admin = admin;
        this.marketing = marketing;
    }

    private static String me() {
        return Caller.name();
    }

    private static String f(HttpServer.Request request, String name) {
        return AdminForms.field(request, name);
    }

    private static AdminRow link(String title, String url) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.url = url;
        return r;
    }

    private static AdminRow fact(String title, String detail) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.detail = detail;
        return r;
    }

    private static AdminRow row(String a, String b, String c, String d) {
        AdminRow r = new AdminRow();
        r.title = a;
        r.detail = b;
        r.meta = c;
        r.amount = d;
        return r;
    }

    /// Basis points as a percentage: 1000 is "10", 1250 is "12.5".
    public static String percent(int bps) {
        String whole = Integer.toString(bps / 100);
        int rest = bps % 100;
        if (rest == 0) {
            return whole;
        }
        return whole + "." + (rest % 10 == 0 ? Integer.toString(rest / 10)
                : (rest < 10 ? "0" : "") + rest);
    }

    /// "12.5" as basis points; refused past two decimals or 100.
    static int bps(String text, String what) {
        long cents = AdminForms.cents(text.length() == 0 ? "0" : text, what);
        if (cents > 10000) {
            throw new com.codename1.backend.ResponseStatusException(400, "Enter " + what
                    + " of at most 100");
        }
        return (int) cents;
    }

    /// "every month", "every 2 weeks".
    static String cadence(String unit, int n) {
        return com.codenameone.examples.bazaar.web.StorefrontPages.planWords(unit, n);
    }

    private Map<String, String> variantNames(String storeId) throws IOException {
        List<String[]> vs = marketing.variants(storeId);
        Map<String, String> out = new HashMap<String, String>();
        for (int iter = 0; iter < vs.size(); iter++) {
            out.put(vs.get(iter)[0], vs.get(iter)[1]);
        }
        return out;
    }

    private static String named(Map<String, String> names, String id) {
        String n = names.get(id);
        return n == null ? "Item" : n;
    }

    // ======================================================== subscriptions

    DataTable subscriptions(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final Map<String, String> names = variantNames(storeId);
        final String root = "/admin/" + storeId + "/subscriptions/";
        List<SubscriptionDto> rows = subscriptions.list(me(), storeId);
        Grid<SubscriptionDto> g = new Grid<SubscriptionDto>("subscriptions")
                .col("customer", "Customer", s -> s.username()).sortBy(s -> s.username())
                    .link(s -> root + s.id())
                .col("item", "Item", s -> named(names, s.variantId()) + " x " + s.quantity())
                .col("every", "Delivers", s -> cadence(s.unit(), s.cadence()))
                .col("off", "Saves", s -> percent(s.discountBps()) + "%")
                .col("next", "Next renewal", s -> Subscriptions.CANCELLED.equals(s.status())
                        ? "" : AdminData.date(s.dueAt())).sortBy(s -> Long.valueOf(s.dueAt()))
                .col("status", "Status", s -> AdminData.words(s.status()))
                    .pill(s -> AdminData.tone(s.status()))
                .rows(s -> s.id(), s -> root + s.id())
                .defaultSort("next", false);
        v.links.add(link("Renewal calendar", "/admin/" + storeId + "/renewals"));
        DataTable t = g.table(rows, Grid.Query.of(request), "/admin/" + storeId
                + "/subscriptions");
        t.csvUrl = "";
        t.searchLabel = "Search subscriptions";
        t.empty = "No subscriptions yet. A shopper subscribes from a product with a selling plan.";
        return t;
    }

    String subscription(String storeId, String id, MarketingView v) throws IOException {
        SubscriptionDto s = subscriptions.detail(me(), storeId, id);
        Map<String, String> names = variantNames(storeId);
        String base = "/admin/" + storeId + "/subscriptions/" + s.id();
        v.back = "/admin/" + storeId + "/subscriptions";
        v.backLabel = "Subscriptions";
        v.facts.add(fact("Customer", s.username()));
        v.facts.add(fact("Item", named(names, s.variantId()) + " x " + s.quantity()));
        v.facts.add(fact("Delivers", cadence(s.unit(), s.cadence())));
        v.facts.add(fact("Saves", percent(s.discountBps()) + "%"));
        v.facts.add(fact("Status", AdminData.words(s.status())));
        if (!Subscriptions.CANCELLED.equals(s.status())) {
            v.facts.add(fact("Next renewal", AdminData.date(s.dueAt())));
        }
        if (s.failures() > 0) {
            v.facts.add(fact("Failed attempts", s.failures() + (s.lastError() == null ? ""
                    : " (" + s.lastError() + ")")));
        }
        ActionCard c = actions(s, base);
        if (!c.buttons.isEmpty()) {
            c.title = "Change it";
            v.cards.add(c);
        }
        return "Subscription of " + s.username();
    }

    /// The buttons a subscription's status allows; the service still decides.
    static ActionCard actions(SubscriptionDto s, String base) {
        ActionCard c = new ActionCard();
        if (Subscriptions.ACTIVE.equals(s.status())) {
            c.button("Skip next delivery", base + "/skip", false);
            c.button("Pause", base + "/pause", false);
        }
        if (Subscriptions.PAUSED.equals(s.status())) {
            c.button("Resume", base + "/resume", false);
        }
        if (!Subscriptions.CANCELLED.equals(s.status())) {
            c.button("Cancel", base + "/cancel", true);
        }
        return c;
    }

    void changeSubscription(String storeId, String id, String action) throws IOException {
        subscriptions.change(me(), storeId, id, action);
    }

    /// A month of renewals: every cycle of every active subscription due in
    /// the grid, from [Subscriptions#calendar].
    void renewals(String storeId, String month, MarketingView v) throws IOException {
        String cur = admin.currency(storeId);
        String base = "/admin/" + storeId + "/renewals";
        long[] grid = MarketingData.monthGrid(month, base, v);
        List<RenewalDto> due = subscriptions.calendar(me(), storeId, Long.toString(grid[0]),
                Long.toString(grid[1]));
        long total = 0;
        for (int w = 0; w < v.weeks.size(); w++) {
            List<CalendarDay> days = v.weeks.get(w).days;
            for (int d = 0; d < days.size(); d++) {
                CalendarDay cell = days.get(d);
                for (int iter = 0; iter < due.size(); iter++) {
                    RenewalDto r = due.get(iter);
                    if (cell.iso.equals(Days.iso(r.dueAt()))) {
                        AdminRow e = new AdminRow();
                        e.title = r.username() + " x" + r.quantity();
                        e.status = Money.format(r.amountCents(), cur);
                        e.url = "/admin/" + storeId + "/subscriptions/" + r.subscriptionId();
                        e.detail = "promo";
                        cell.events.add(e);
                        if (cell.inMonth) {
                            total += r.amountCents();
                        }
                    }
                }
            }
        }
        v.intro = "Renewals expected this month: " + Money.format(total, cur) + ".";
        v.links.add(link("Subscriptions", "/admin/" + storeId + "/subscriptions"));
    }

    // ============================================================ companies

    DataTable companies(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final Map<String, String> lists = listNames(storeId);
        final String root = "/admin/" + storeId + "/companies/";
        List<CompanyDto> rows = trade.companies(me(), storeId);
        Grid<CompanyDto> g = new Grid<CompanyDto>("companies")
                .col("name", "Company", c -> c.name()).sortBy(c -> c.name()).link(c -> root + c.id())
                .col("vat", "VAT ID", c -> c.vatId() == null ? "" : c.vatId())
                .col("terms", "Terms", c -> terms(c.termsDays()))
                .col("list", "Price list", c -> c.priceListId() == null
                        || c.priceListId().length() == 0 ? "List prices" : named(lists,
                        c.priceListId()))
                .rows(c -> c.id(), c -> root + c.id())
                .defaultSort("name", false);
        v.links.add(link("Price lists", "/admin/" + storeId + "/price-lists"));
        v.links.add(link("Quotes", "/admin/" + storeId + "/quotes"));
        v.links.add(link("Invoices", "/admin/" + storeId + "/invoices"));
        v.forms.add(companyForm("new-company", "Add a company", "/admin/" + storeId
                + "/companies", "Add company", null, trade.priceLists(me(), storeId)));
        DataTable t = g.table(rows, Grid.Query.of(request), "/admin/" + storeId + "/companies");
        t.csvUrl = "";
        t.searchLabel = "Search companies";
        t.empty = "No companies yet.";
        return t;
    }

    static String terms(int days) {
        return days <= 0 ? "Pays by card" : "Net " + days;
    }

    private Map<String, String> listNames(String storeId) throws IOException {
        List<PriceListDto> ls = trade.priceLists(me(), storeId);
        Map<String, String> out = new HashMap<String, String>();
        for (int iter = 0; iter < ls.size(); iter++) {
            out.put(ls.get(iter).id(), ls.get(iter).name());
        }
        return out;
    }

    private static FormView companyForm(String id, String title, String action, String submit,
            CompanyDto c, List<PriceListDto> lists) {
        FormView form = new FormView(id, title, action, submit);
        form.add("name", "Name", "text", c == null ? "" : c.name()).required = true;
        form.add("vatId", "VAT ID", "text", c == null || c.vatId() == null ? "" : c.vatId())
                .hint("Checked for its country's format when saved");
        String t = c == null ? "0" : Integer.toString(c.termsDays());
        form.add("termsDays", "Payment terms", "select", t).option("0", "Pays by card")
                .option("15", "Net 15").option("30", "Net 30").option("60", "Net 60");
        FormField list = form.add("priceListId", "Price list", "select",
                c == null || c.priceListId() == null ? "" : c.priceListId());
        list.option("", "List prices");
        for (int iter = 0; iter < lists.size(); iter++) {
            list.option(lists.get(iter).id(), lists.get(iter).name());
        }
        selectChosen(form);
        return form;
    }

    /// Marks each select's option whose id is the field's value.
    static void selectChosen(FormView form) {
        for (int iter = 0; iter < form.fields.size(); iter++) {
            FormField ff = form.fields.get(iter);
            for (int o = 0; o < ff.options.size(); o++) {
                ff.options.get(o).flag = ff.options.get(o).id.equals(ff.value);
            }
        }
    }

    private static int termsField(HttpServer.Request request) {
        String t = f(request, "termsDays");
        return "15".equals(t) ? 15 : "30".equals(t) ? 30 : "60".equals(t) ? 60 : 0;
    }

    String saveCompany(String storeId, String companyId, HttpServer.Request request)
            throws IOException {
        CompanyDto in = new CompanyDto(companyId, f(request, "name"), f(request, "vatId"),
                termsField(request), f(request, "priceListId"));
        return trade.saveCompany(me(), storeId, companyId, in).id();
    }

    String company(String storeId, String companyId, MarketingView v) throws IOException {
        CompanyDto c = null;
        List<CompanyDto> all = trade.companies(me(), storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(companyId)) {
                c = all.get(iter);
            }
        }
        if (c == null) {
            throw new com.codename1.backend.ResponseStatusException(404, "No such company");
        }
        String base = "/admin/" + storeId + "/companies/" + c.id();
        v.back = "/admin/" + storeId + "/companies";
        v.backLabel = "Companies";
        v.facts.add(fact("Terms", terms(c.termsDays())));
        if (c.vatId() != null && c.vatId().length() > 0) {
            VatCheckDto vat = trade.storedVat(me(), storeId, c.id());
            v.facts.add(fact("VAT check", vat.message() + (vat.name().length() > 0
                    ? " (" + vat.name() + ")" : "") + (vat.checkedAt() > 0
                    ? ", " + com.codenameone.examples.bazaar.Days.iso(vat.checkedAt()) : "")));
        }
        v.forms.add(companyForm("company", "Company", base, "Save", c, trade.priceLists(me(), storeId)));
        List<CompanySiteDto> sites = trade.sites(me(), storeId, c.id());
        Map<String, String> siteNames = new HashMap<String, String>();
        v.rowsTitle = "Locations";
        v.rowsEmpty = "No locations yet.";
        v.headA = "Name";
        v.headB = "Address";
        v.headC = "Country";
        v.headD = "";
        for (int iter = 0; iter < sites.size(); iter++) {
            CompanySiteDto s = sites.get(iter);
            siteNames.put(s.id(), s.name());
            v.rows.add(row(s.name(), s.line1() + ", " + s.postal() + " " + s.city(), s.country(),
                    ""));
        }
        FormView site = new FormView("site", "Add a location", base + "/sites", "Add location");
        site.add("name", "Name", "text", "").required = true;
        site.add("line1", "Street address", "text", "");
        site.add("city", "City", "text", "");
        site.add("postal", "Postal code", "text", "");
        site.add("country", "Country (two letters)", "text", "");
        v.forms.add(site);
        List<CompanyBuyerDto> bs = trade.buyers(me(), storeId, c.id());
        v.cardsTitle = "Buyers";
        for (int iter = 0; iter < bs.size(); iter++) {
            CompanyBuyerDto b = bs.get(iter);
            ActionCard k = new ActionCard();
            k.title = b.username();
            k.detail = b.siteId() == null || b.siteId().length() == 0 ? "" : "Ships to "
                    + named(siteNames, b.siteId());
            k.button("Remove", base + "/buyers/" + b.id() + "/remove", true);
            v.cards.add(k);
        }
        FormView buyer = new FormView("buyer", "Add a buyer", base + "/buyers", "Add buyer");
        buyer.text = "A shopper account that orders for the company at its prices and terms.";
        buyer.add("username", "Account (email)", "text", "").required = true;
        FormField at = buyer.add("siteId", "Ships to", "select", "");
        at.option("", "No default location");
        for (int iter = 0; iter < sites.size(); iter++) {
            at.option(sites.get(iter).id(), sites.get(iter).name());
        }
        v.forms.add(buyer);
        return c.name();
    }

    void addSite(String storeId, String companyId, HttpServer.Request request)
            throws IOException {
        trade.addSite(me(), storeId, companyId, new CompanySiteDto(null, f(request, "name"),
                f(request, "line1"), f(request, "city"), f(request, "postal"),
                f(request, "country")));
    }

    void addBuyer(String storeId, String companyId, HttpServer.Request request)
            throws IOException {
        trade.addBuyer(me(), storeId, companyId, new CompanyBuyerDto(null,
                f(request, "username"), f(request, "siteId")));
    }

    void removeBuyer(String storeId, String companyId, String buyerId) throws IOException {
        trade.removeBuyer(me(), storeId, companyId, buyerId);
    }

    // ========================================================== price lists

    DataTable priceLists(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final String root = "/admin/" + storeId + "/price-lists/";
        List<PriceListDto> rows = trade.priceLists(me(), storeId);
        Grid<PriceListDto> g = new Grid<PriceListDto>("price-lists")
                .col("name", "Price list", p -> p.name()).sortBy(p -> p.name())
                    .link(p -> root + p.id())
                .col("off", "Off list", p -> percent(p.percentOffBps()) + "%")
                .rows(p -> p.id(), p -> root + p.id())
                .defaultSort("name", false);
        v.links.add(link("Companies", "/admin/" + storeId + "/companies"));
        v.forms.add(listForm("new-list", "Add a price list", "/admin/" + storeId + "/price-lists",
                "Add price list", null));
        DataTable t = g.table(rows, Grid.Query.of(request), "/admin/" + storeId
                + "/price-lists");
        t.csvUrl = "";
        t.searchLabel = "Search price lists";
        t.empty = "No price lists yet.";
        return t;
    }

    private static FormView listForm(String id, String title, String action, String submit,
            PriceListDto p) {
        FormView form = new FormView(id, title, action, submit);
        form.text = "Every item sells at the percentage off list, unless the list has a "
                + "fixed price for it.";
        form.add("name", "Name", "text", p == null ? "" : p.name()).required = true;
        form.add("percentOff", "Percentage off list", "text", p == null ? "0"
                : percent(p.percentOffBps())).hint("10 for 10%");
        return form;
    }

    String savePriceList(String storeId, String listId, HttpServer.Request request)
            throws IOException {
        PriceListDto in = new PriceListDto(listId, f(request, "name"),
                bps(f(request, "percentOff"), "a percentage"));
        return trade.savePriceList(me(), storeId, listId, in).id();
    }

    String priceList(String storeId, String listId, MarketingView v) throws IOException {
        PriceListDto p = null;
        List<PriceListDto> all = trade.priceLists(me(), storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(listId)) {
                p = all.get(iter);
            }
        }
        if (p == null) {
            throw new com.codename1.backend.ResponseStatusException(404, "No such price list");
        }
        String cur = admin.currency(storeId);
        String base = "/admin/" + storeId + "/price-lists/" + p.id();
        v.back = "/admin/" + storeId + "/price-lists";
        v.backLabel = "Price lists";
        v.forms.add(listForm("list", "Price list", base, "Save", p));
        Map<String, String> names = variantNames(storeId);
        List<PriceListEntryDto> es = trade.entries(me(), storeId, p.id());
        v.cardsTitle = "Prices and quantity rules";
        for (int iter = 0; iter < es.size(); iter++) {
            PriceListEntryDto e = es.get(iter);
            ActionCard k = new ActionCard();
            k.title = named(names, e.variantId());
            k.amount = e.fixedCents() > 0 ? Money.format(e.fixedCents(), cur)
                    : percent(p.percentOffBps()) + "% off";
            k.detail = rule(e.minQty(), e.maxQty(), e.stepQty());
            k.button("Remove", base + "/entries/" + e.id() + "/remove", true);
            v.cards.add(k);
        }
        FormView entry = new FormView("entry", "Set a price or quantity rule", base + "/entries",
                "Save rule");
        entry.text = "A fixed price replaces the percentage for that item. Minimum, maximum and "
                + "step hold every quote and net-terms order line; 0 means no rule.";
        FormField item = entry.add("variantId", "Item", "select", "");
        List<String[]> vs = marketing.variants(storeId);
        for (int iter = 0; iter < vs.size(); iter++) {
            item.option(vs.get(iter)[0], vs.get(iter)[1]);
        }
        entry.add("fixed", "Fixed price (empty for the percentage)", "text", "");
        entry.add("minQty", "Minimum", "number", "0");
        entry.add("maxQty", "Maximum", "number", "0");
        entry.add("stepQty", "In multiples of", "number", "0");
        v.forms.add(entry);
        return p.name();
    }

    static String rule(int min, int max, int step) {
        String r = Trade.ruleText(min, max, step);
        return r.length() == 0 ? "No quantity rule" : r;
    }

    void putEntry(String storeId, String listId, HttpServer.Request request) throws IOException {
        String fixed = f(request, "fixed");
        PriceListEntryDto in = new PriceListEntryDto(null, f(request, "variantId"),
                fixed.length() == 0 ? 0 : AdminForms.cents(fixed, "the fixed price"),
                AdminForms.number(f(request, "minQty"), 0, "the minimum"),
                AdminForms.number(f(request, "maxQty"), 0, "the maximum"),
                AdminForms.number(f(request, "stepQty"), 0, "the step"));
        trade.putEntry(me(), storeId, listId, in);
    }

    void removeEntry(String storeId, String listId, String entryId) throws IOException {
        trade.removeEntry(me(), storeId, listId, entryId);
    }

    // =============================================================== quotes

    /// The quote pipeline as a board. Each card's move form offers every
    /// column, the next one the merchant may move it to chosen; a move the
    /// pipeline refuses comes back as the service's 409 message.
    void quotes(String storeId, MarketingView v) throws IOException {
        List<TradeQuoteDto> rows = trade.quotes(me(), storeId);
        for (int c = 0; c < QUOTE_COLUMNS.length; c++) {
            BoardColumn col = new BoardColumn();
            col.id = QUOTE_COLUMNS[c][0];
            col.title = QUOTE_COLUMNS[c][1];
            col.next = col.id;
            for (int n = 0; n < QUOTE_COLUMNS.length; n++) {
                if (Trade.allowed(col.id, QUOTE_COLUMNS[n][0], true)
                        && !Trade.DECLINED.equals(QUOTE_COLUMNS[n][0])) {
                    col.next = QUOTE_COLUMNS[n][0];
                    break;
                }
            }
            for (int iter = 0; iter < rows.size(); iter++) {
                TradeQuoteDto q = rows.get(iter);
                if (!col.id.equals(q.status())) {
                    continue;
                }
                AdminRow r = new AdminRow();
                r.id = q.id();
                r.title = "Quote " + shortId(q.id());
                r.detail = q.companyName();
                r.amount = Money.format(q.totalCents(), q.currency());
                r.url = "/admin/" + storeId + "/quotes/" + q.id();
                col.cards.add(r);
            }
            col.count = Integer.toString(col.cards.size());
            v.columns.add(col);
        }
        v.links.add(link("Companies", "/admin/" + storeId + "/companies"));
        v.links.add(link("Invoices", "/admin/" + storeId + "/invoices"));
    }

    static String shortId(String id) {
        return id.length() > 8 ? id.substring(id.length() - 8) : id;
    }

    void moveQuote(String storeId, String quoteId, String to) throws IOException {
        trade.moveQuote(me(), storeId, quoteId, to);
    }

    String quote(String storeId, String quoteId, MarketingView v) throws IOException {
        TradeQuoteDto q = trade.quote(me(), storeId, quoteId);
        v.back = "/admin/" + storeId + "/quotes";
        v.backLabel = "Quotes";
        quoteFacts(q, v);
        return "Quote " + shortId(q.id());
    }

    /// A quote's facts and lines, the same in the back office and the account.
    static void quoteFacts(TradeQuoteDto q, MarketingView v) {
        v.facts.add(fact("Company", q.companyName()));
        v.facts.add(fact("Status", AdminData.words(q.status())));
        if (q.buyerUsername() != null && q.buyerUsername().length() > 0) {
            v.facts.add(fact("Asked for by", q.buyerUsername()));
        }
        if (q.note() != null && q.note().length() > 0) {
            v.facts.add(fact("Note", q.note()));
        }
        v.facts.add(fact("Subtotal at list", Money.format(q.subtotalCents(), q.currency())));
        v.facts.add(fact("Company discount", Money.format(q.discountCents(), q.currency())));
        v.facts.add(fact("Total", Money.format(q.totalCents(), q.currency())));
        v.facts.add(fact("Updated", AdminData.date(q.updatedAt())));
        v.rowsTitle = "Lines";
        v.headA = "Item";
        v.headB = "SKU";
        v.headC = "Quantity";
        v.headD = "Line total";
        List<TradeQuoteLineDto> ls = q.lines();
        for (int iter = 0; ls != null && iter < ls.size(); iter++) {
            TradeQuoteLineDto l = ls.get(iter);
            v.rows.add(row(l.title(), l.sku(), Integer.toString(l.quantity()),
                    Money.format(l.unitCents() * l.quantity(), q.currency())));
        }
    }

    // ============================================================= invoices

    void invoices(String storeId, MarketingView v) throws IOException {
        String cur = admin.currency(storeId);
        List<NetTermDto> rows = trade.netTerms(me(), storeId);
        Map<String, String> companies = new HashMap<String, String>();
        List<CompanyDto> cs = trade.companies(me(), storeId);
        for (int iter = 0; iter < cs.size(); iter++) {
            companies.put(cs.get(iter).id(), cs.get(iter).name());
        }
        String base = "/admin/" + storeId + "/invoices/";
        v.cardsTitle = "Net-terms invoices";
        for (int iter = 0; iter < rows.size(); iter++) {
            NetTermDto t = rows.get(iter);
            ActionCard k = invoiceCard(t, cur, base + t.orderId() + "/invoice.pdf");
            k.detail = named(companies, t.companyId()) + ", " + k.detail;
            k.url = "/admin/" + storeId + "/orders/" + t.orderId();
            if (!Trade.PAID.equals(t.status())) {
                k.button("Mark paid", base + t.orderId() + "/paid", false);
                k.button("Send pay link", "/admin/" + storeId + "/orders/" + t.orderId()
                        + "/pay-link?back=invoices", false);
            }
            v.cards.add(k);
        }
        if (rows.isEmpty()) {
            v.intro = "No orders on net terms yet. An order a company on terms places, or a "
                    + "quote it orders, shows here until it is paid.";
        }
        v.links.add(link("Companies", "/admin/" + storeId + "/companies"));
    }

    static ActionCard invoiceCard(NetTermDto t, String cur, String pdf) {
        ActionCard k = new ActionCard();
        k.title = "Order #" + t.orderNumber();
        k.status = Trade.PAID.equals(t.status()) ? "Paid" : t.overdue() ? "Overdue" : "Open";
        k.detail = "net " + t.termsDays();
        k.meta = Trade.PAID.equals(t.status()) ? "paid " + AdminData.date(t.paidAt())
                : "due " + AdminData.date(t.dueAt());
        k.amount = Money.format(t.totalCents(), cur);
        k.extraUrl = pdf;
        k.extraLabel = "Invoice (PDF)";
        return k;
    }

    void markPaid(String storeId, String orderId) throws IOException {
        trade.markPaid(me(), storeId, orderId);
    }

    /// The per-line refusals of a CSV or quick order, as the page's table.
    static void csvErrors(List<CsvErrorDto> errors, MarketingView v) {
        v.rowsTitle = "These lines were not accepted";
        v.headA = "Line";
        v.headB = "Problem";
        v.headC = "";
        v.headD = "";
        for (int iter = 0; iter < errors.size(); iter++) {
            CsvErrorDto e = errors.get(iter);
            v.rows.add(row(e.line() == 0 ? "-" : Integer.toString(e.line()), e.message(), "",
                    ""));
        }
    }
}
