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
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.AudienceMemberDto;
import com.codenameone.examples.bazaar.api.AudiencePreviewDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.CampaignStatsDto;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.ClubMembershipDto;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.DiscountRulesDto;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.JourneyNodeStatDto;
import com.codenameone.examples.bazaar.api.LevelDto;
import com.codenameone.examples.bazaar.api.LoyaltyMemberDto;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import com.codenameone.examples.bazaar.api.PromoEventDto;
import com.codenameone.examples.bazaar.api.RescheduleDto;
import com.codenameone.examples.bazaar.api.SaleEventDto;
import com.codenameone.examples.bazaar.api.SaleItemDto;
import com.codenameone.examples.bazaar.club.Clubs;
import com.codenameone.examples.bazaar.inventory.InventoryEndpoint;
import com.codenameone.examples.bazaar.journey.JourneyCatalog;
import com.codenameone.examples.bazaar.journey.Journeys;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.marketing.Audiences;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.rates.RateSettings;
import com.codenameone.examples.bazaar.sale.SaleEvents;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.table.DataTable;
import com.codenameone.examples.bazaar.web.table.Grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What the marketing pages of the back office show and do: sale events and
/// discount rules, the promotions calendar, loyalty and clubs, segments,
/// campaigns and journeys.
///
/// Every call goes to the service the app's API uses, with the signed-in
/// name and the store from the address; the services check the role at that
/// store (managers for every change, and for most reads) and that each row is
/// the store's, so another store's id is a 404 here as there. Nothing here
/// repeats a business rule: it reads form fields into the services' DTOs,
/// keeps whatever a form does not show from the row it edits, and turns DTOs
/// into rows, forms, a calendar grid and a flowchart.
@Component
public class MarketingData {
    static final String[][] FIELDS = {
        {"totalSpent", "Total spent"}, {"ordersCount", "Orders"},
        {"lastOrderAt", "Last order"}, {"createdAt", "Customer since"}, {"tags", "Tags"},
        {"country", "Country"}, {"tier", "Loyalty tier"}, {"club", "Club"},
        {"productPurchased", "Bought product"}, {"acceptsMarketing", "Accepts marketing"},
    };
    static final String[][] OPS = {
        {"=", "equals"}, {"!=", "is not"}, {">", "more than"}, {">=", "at least"},
        {"<", "less than"}, {"<=", "at most"}, {"withinDays", "within days"},
        {"notWithinDays", "not within days"}, {"has", "has"}, {"lacks", "lacks"},
        {"is", "is"}, {"isNot", "is not (id)"},
    };
    static final int MAX_RULES = 20;
    private static final String[] MONTHS = {"January", "February", "March", "April", "May",
        "June", "July", "August", "September", "October", "November", "December"};

    private final SaleEvents sales;
    private final RateSettings discounts;
    private final Loyalty loyalty;
    private final Clubs clubs;
    private final Audiences audiences;
    private final Campaigns campaigns;
    private final Journeys journeys;
    private final InventoryEndpoint inventory;
    private final AdminData admin;

    public MarketingData(SaleEvents sales, RateSettings discounts, Loyalty loyalty, Clubs clubs,
            Audiences audiences, Campaigns campaigns, Journeys journeys,
            InventoryEndpoint inventory, AdminData admin) {
        this.sales = sales;
        this.discounts = discounts;
        this.loyalty = loyalty;
        this.clubs = clubs;
        this.audiences = audiences;
        this.campaigns = campaigns;
        this.journeys = journeys;
        this.inventory = inventory;
        this.admin = admin;
    }

    private static String me() {
        return Caller.name();
    }

    private static String f(HttpServer.Request request, String name) {
        return AdminForms.field(request, name);
    }

    private static boolean on(HttpServer.Request request, String name) {
        String v = f(request, name);
        return v.length() > 0 && !"0".equals(v) && !"false".equals(v);
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

    private static String plain(long cents) {
        return Money.plain(cents);
    }

    /// A percentage kept as basis points, written as `12.50`.
    private static String percent(long bps) {
        return Money.plain(bps);
    }

    /// A percentage typed as `12.5` as basis points: the money parser, since
    /// both are hundredths.
    private static int bps(String text, String what) {
        return text.length() == 0 ? 0 : (int) AdminForms.cents(text, what);
    }

    /// The end of a window whose last day is a `yyyy-MM-dd` field: the start
    /// of the day after it, or 0.
    private static long dayEnd(String iso) {
        long s = AdminForms.dayStart(iso);
        return s < 0 ? 0 : s + Days.MILLIS;
    }

    private static long day(String iso, String what) {
        long s = AdminForms.dayStart(iso);
        if (s < 0) {
            throw new ResponseStatusException(400, "Pick " + what);
        }
        return s;
    }

    /// The last day a window covers, for a date field.
    private static String lastDay(long endsAt) {
        return endsAt <= 0 ? "" : Days.iso(endsAt - 1);
    }

    private static DataTable table(DataTable t, String search, String empty) {
        t.csvUrl = "";
        t.searchLabel = search;
        t.empty = empty;
        return t;
    }

    private static int whole(HttpServer.Request request, String name, int fallback, String what) {
        return AdminForms.number(f(request, name), fallback, what);
    }

    // ================================================================ sales

    DataTable sales(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final String cur = admin.currency(storeId);
        List<SaleEventDto> rows = sales.list(me(), storeId);
        final String root = "/admin/" + storeId + "/sales/";
        Grid<SaleEventDto> g = new Grid<SaleEventDto>("sales")
                .col("title", "Sale", s -> s.title()).sortBy(s -> s.title()).link(s -> root + s.id())
                .col("kind", "Kind", s -> AdminData.words(s.kind()))
                .col("off", "Takes off", s -> off(s.mode(), s.amount(), cur))
                .col("starts", "Starts", s -> AdminData.date(s.startsAt()))
                    .sortBy(s -> Long.valueOf(s.startsAt()))
                .col("ends", "Last day", s -> lastDay(s.endsAt()))
                .col("status", "Status", s -> AdminData.words(s.status()))
                    .pill(s -> SaleEvents.RUNNING.equals(s.status()) ? "ok"
                        : AdminData.tone(s.status()))
                .rows(s -> s.id(), s -> root + s.id())
                .defaultSort("starts", true);
        v.links.add(link("Calendar", "/admin/" + storeId + "/calendar"));
        v.links.add(link("Discounts", "/admin/" + storeId + "/discounts"));
        FormView form = new FormView("new-sale", "Schedule a sale", "/admin/" + storeId + "/sales",
                "Schedule sale");
        form.text = "Prices change when it starts and go back when it ends. A flash sale can "
                + "stop after a number of units.";
        form.add("title", "Name", "text", "").required = true;
        form.add("kind", "Kind", "select", SaleEvents.SALE).option(SaleEvents.SALE, "Sale")
                .option(SaleEvents.FLASH, "Flash sale");
        form.add("mode", "Takes off", "select", SaleEvents.PERCENT)
                .option(SaleEvents.PERCENT, "A percentage")
                .option(SaleEvents.AMOUNT_OFF, "An amount")
                .option(SaleEvents.FIXED_PRICE, "Sells at a fixed price");
        form.add("amount", "Percentage or amount", "text", "").hint("20 for 20%, or 5.00");
        long now = System.currentTimeMillis();
        form.add("starts", "First day", "date", Days.iso(now + Days.MILLIS));
        form.add("ends", "Last day", "date", Days.iso(now + 7 * Days.MILLIS));
        form.add("stockCap", "Stop after units (flash sale, 0 for no cap)", "number", "0");
        List<String[]> vs = variants(storeId);
        for (int iter = 0; iter < vs.size(); iter++) {
            form.check("var_" + vs.get(iter)[0], vs.get(iter)[1], false);
        }
        v.forms.add(form);
        return table(g.table(rows, Grid.Query.of(request), "/admin/" + storeId + "/sales"),
                "Search sales", "No sales yet.");
    }

    /// Every variant of the store once: id and "product - variant (sku)".
    private List<String[]> variants(String storeId) throws IOException {
        List<LevelDto> levels;
        try {
            levels = inventory.levels(storeId, null, null, null);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception other) {
            throw new IOException(other);
        }
        List<String[]> out = new ArrayList<String[]>();
        Map<String, Boolean> seen = new HashMap<String, Boolean>();
        for (int iter = 0; iter < levels.size() && out.size() < 200; iter++) {
            LevelDto l = levels.get(iter);
            if (seen.put(l.variantId(), Boolean.TRUE) != null) {
                continue;
            }
            String name = l.productTitle();
            if (l.variantTitle() != null && l.variantTitle().length() > 0) {
                name += " - " + l.variantTitle();
            }
            if (l.sku() != null && l.sku().length() > 0) {
                name += " (" + l.sku() + ")";
            }
            out.add(new String[] {l.variantId(), name});
        }
        return out;
    }

    static String off(String mode, long amount, String cur) {
        if (SaleEvents.PERCENT.equals(mode)) {
            return percent(amount) + "%";
        }
        if (SaleEvents.FIXED_PRICE.equals(mode)) {
            return "Sells at " + Money.format(amount, cur);
        }
        return Money.format(amount, cur);
    }

    String createSale(String storeId, HttpServer.Request request) throws IOException {
        List<String[]> vs = variants(storeId);
        List<SaleItemDto> items = new ArrayList<SaleItemDto>();
        for (int iter = 0; iter < vs.size(); iter++) {
            if (on(request, "var_" + vs.get(iter)[0])) {
                items.add(new SaleItemDto(vs.get(iter)[0], 0, 0, null));
            }
        }
        long amount = AdminForms.cents(f(request, "amount"), "the amount");
        SaleEventDto in = new SaleEventDto(null, f(request, "title"), f(request, "kind"),
                f(request, "mode"), amount, day(f(request, "starts"), "a first day"),
                dayEnd(f(request, "ends")), null, whole(request, "stockCap", 0, "the stock cap"),
                0, null, items);
        return sales.create(me(), storeId, in).id();
    }

    String sale(String storeId, String saleId, MarketingView v) throws IOException {
        String cur = admin.currency(storeId);
        SaleEventDto s = sales.get(me(), storeId, saleId);
        String base = "/admin/" + storeId + "/sales/" + s.id();
        v.back = "/admin/" + storeId + "/sales";
        v.backLabel = "Sales";
        v.heading = s.title();
        v.facts.add(fact("Status", AdminData.words(s.status())));
        v.facts.add(fact("Kind", AdminData.words(s.kind())));
        v.facts.add(fact("Takes off", off(s.mode(), s.amount(), cur)));
        v.facts.add(fact("Runs", AdminData.date(s.startsAt()) + " to " + lastDay(s.endsAt())));
        if (s.stockCap() > 0) {
            v.facts.add(fact("Sold", s.sold() + " of " + s.stockCap()));
        }
        v.rowsTitle = "Items";
        v.headA = "Variant";
        v.headB = "State";
        v.headC = "Was";
        v.headD = "Sale price";
        for (int iter = 0; s.items() != null && iter < s.items().size(); iter++) {
            SaleItemDto i = s.items().get(iter);
            AdminRow r = new AdminRow();
            r.title = i.variantId();
            r.detail = AdminData.words(i.state());
            r.meta = Money.format(i.originalCents(), cur);
            r.amount = Money.format(i.saleCents(), cur);
            v.rows.add(r);
        }
        if (SaleEvents.SCHEDULED.equals(s.status())) {
            v.forms.add(moveForm("move", "Reschedule", base + "/reschedule", s.startsAt(),
                    s.endsAt(), ""));
        }
        if (SaleEvents.SCHEDULED.equals(s.status()) || SaleEvents.RUNNING.equals(s.status())) {
            FormView c = new FormView("cancel", "Cancel the sale", base + "/cancel", "Cancel sale");
            c.text = "A running sale puts every price back now.";
            c.danger = true;
            c.confirm = "Cancel this sale?";
            v.forms.add(c);
        }
        return s.title();
    }

    private static FormView moveForm(String id, String title, String action, long starts,
            long ends, String month) {
        FormView m = new FormView(id, title, action, "Move");
        m.add("starts", "First day", "date", AdminData.date(starts));
        m.add("ends", "Last day", "date", lastDay(ends));
        if (month.length() > 0) {
            m.add("month", "", "hidden", month);
        }
        return m;
    }

    void cancelSale(String storeId, String saleId) throws IOException {
        sales.cancel(me(), storeId, saleId);
    }

    /// Moves a sale, a dated discount or a scheduled campaign: the call the
    /// app's calendar makes when an event is dragged.
    void reschedule(String storeId, String kind, String id, HttpServer.Request request)
            throws IOException {
        sales.reschedule(me(), storeId, new RescheduleDto(kind, id,
                day(f(request, "starts"), "a first day"), dayEnd(f(request, "ends"))));
    }

    // ============================================================= calendar

    /// A month grid, Monday first, of everything [SaleEvents] puts on the
    /// calendar, and a move form per event that can move.
    void calendar(String storeId, String month, MarketingView v) throws IOException {
        long now = System.currentTimeMillis();
        String m = month != null && month.length() == 7 && Days.isIso(month + "-01") ? month
                : Days.iso(now).substring(0, 7);
        long first = AdminForms.dayStart(m + "-01");
        int y = Integer.parseInt(m.substring(0, 4));
        int mo = Integer.parseInt(m.substring(5, 7));
        String next = mo == 12 ? (y + 1) + "-01" : y + "-" + two(mo + 1);
        String prev = mo == 1 ? (y - 1) + "-12" : y + "-" + two(mo - 1);
        long end = AdminForms.dayStart(next + "-01");
        String base = "/admin/" + storeId + "/calendar";
        v.month = MONTHS[mo - 1] + " " + y;
        v.prevUrl = base + "?month=" + prev;
        v.nextUrl = base + "?month=" + next;
        // 1970-01-01 was a Thursday, so a day number plus 3, mod 7, is 0 on Mondays.
        int lead = (int) ((first / Days.MILLIS + 3) % 7);
        long gridStart = first - lead * Days.MILLIS;
        long gridEnd = end;
        while ((gridEnd / Days.MILLIS + 3) % 7 != 0) {
            gridEnd += Days.MILLIS;
        }
        List<PromoEventDto> events = sales.calendar(me(), storeId, Long.toString(gridStart),
                Long.toString(gridEnd));
        String today = Days.iso(now);
        CalendarWeek week = null;
        for (long d = gridStart; d < gridEnd; d += Days.MILLIS) {
            if (week == null || week.days.size() == 7) {
                week = new CalendarWeek();
                v.weeks.add(week);
            }
            CalendarDay cell = new CalendarDay();
            cell.iso = Days.iso(d);
            cell.day = cell.iso.charAt(8) == '0' ? cell.iso.substring(9) : cell.iso.substring(8);
            cell.inMonth = d >= first && d < end;
            cell.today = cell.iso.equals(today);
            for (int iter = 0; iter < events.size(); iter++) {
                PromoEventDto e = events.get(iter);
                if (e.startsAt() < d + Days.MILLIS && e.endsAt() > d) {
                    AdminRow r = new AdminRow();
                    r.title = e.title();
                    r.status = AdminData.words(e.kind());
                    r.url = eventUrl(storeId, e);
                    r.detail = "promo " + kindClass(e.kind());
                    cell.events.add(r);
                }
            }
            week.days.add(cell);
        }
        for (int iter = 0; iter < events.size(); iter++) {
            PromoEventDto e = events.get(iter);
            if (SaleEvents.SALE.equals(e.kind()) && !SaleEvents.SCHEDULED.equals(e.status())) {
                continue;
            }
            FormView f = moveForm("move-" + e.id(), e.title() + " (" + AdminData.words(e.kind())
                    + ")", base + "/move", e.startsAt(), e.endsAt(), m);
            f.add("kind", "", "hidden", e.kind());
            f.add("id", "", "hidden", e.id());
            v.forms.add(f);
        }
        v.links.add(link("Sales", "/admin/" + storeId + "/sales"));
        v.links.add(link("Discounts", "/admin/" + storeId + "/discounts"));
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : Integer.toString(n);
    }

    private static String kindClass(String kind) {
        if (SaleEvents.SALE.equals(kind)) {
            return "sale";
        }
        return "CAMPAIGN".equals(kind) ? "campaign" : "discount";
    }

    private static String eventUrl(String storeId, PromoEventDto e) {
        String base = "/admin/" + storeId;
        if (SaleEvents.SALE.equals(e.kind())) {
            return base + "/sales/" + e.id();
        }
        if ("CAMPAIGN".equals(e.kind())) {
            return base + "/campaigns/" + e.id();
        }
        return base + "/discounts/" + e.id();
    }

    // ============================================================ discounts

    DataTable discounts(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final String cur = admin.currency(storeId);
        final String root = "/admin/" + storeId + "/discounts/";
        Grid<DiscountDto> g = new Grid<DiscountDto>("discounts")
                .col("title", "Discount", d -> name(d)).sortBy(d -> name(d)).link(d -> root + d.id())
                .col("kind", "Kind", d -> AdminData.words(d.kind()))
                .col("amount", "Amount", d -> DiscountEngine.PERCENT.equals(d.kind())
                        ? percent(d.amount()) + "%" : Money.format(d.amount(), cur)).num()
                .col("auto", "Applies", d -> d.automatic() ? "Automatically" : "With the code")
                .col("used", "Used", d -> Integer.toString(d.usedCount())).num()
                .col("status", "Status", d -> d.active() ? "Active" : "Off")
                    .pill(d -> d.active() ? "ok" : "")
                .rows(d -> d.id(), d -> root + d.id())
                .defaultSort("title", false);
        v.links.add(link("New discount", "/admin/" + storeId + "/new-discount"));
        v.links.add(link("Sales", "/admin/" + storeId + "/sales"));
        return table(g.table(discounts.discounts(me(), storeId), Grid.Query.of(request),
                "/admin/" + storeId + "/discounts"), "Search discounts", "No discounts yet.");
    }

    private static String name(DiscountDto d) {
        return d.code() != null && d.code().length() > 0 ? d.code() : d.title();
    }

    private DiscountDto discount(String storeId, String id) throws IOException {
        List<DiscountDto> all = discounts.discounts(me(), storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(id)) {
                return all.get(iter);
            }
        }
        throw new ResponseStatusException(404, "No such discount");
    }

    String discountEditor(String storeId, String id, MarketingView v) throws IOException {
        String base = "/admin/" + storeId + "/discounts";
        v.back = base;
        v.backLabel = "Discounts";
        DiscountDto d = id == null ? new DiscountDto(null, "", "", DiscountEngine.PERCENT, 1000, 0,
                0, 0, DiscountEngine.ALL, "", false, 0, 0, false, false, 0, 0, true)
                : discount(storeId, id);
        FormView form = new FormView("discount", "Discount", id == null ? base : base + "/" + id,
                "Save discount");
        form.add("code", "Code (empty for an automatic discount)", "text", d.code());
        form.add("title", "Name", "text", d.title());
        form.add("kind", "Kind", "select", d.kind()).option(DiscountEngine.PERCENT, "Percentage")
                .option(DiscountEngine.FIXED, "Amount off")
                .option(DiscountEngine.FREE_SHIPPING, "Free shipping")
                .option(DiscountEngine.BUY_X_GET_Y, "Buy X get Y");
        form.add("amount", "Percentage or amount", "text", plain(d.amount()))
                .hint("10 for 10%, or 5.00");
        form.add("minSubtotal", "Minimum order", "text", plain(d.minSubtotalCents()));
        form.add("buyQty", "Buy (units)", "number", Integer.toString(d.buyQty()));
        form.add("getQty", "Get (units)", "number", Integer.toString(d.getQty()));
        form.add("usageLimit", "Uses in all (0 for no limit)", "number",
                Integer.toString(d.usageLimit()));
        form.add("starts", "First day (optional)", "date", AdminData.date(d.startsAt()));
        form.add("ends", "Last day (optional)", "date", lastDay(d.endsAt()));
        form.check("automatic", "Applies by itself, no code", d.automatic());
        form.check("oncePerCustomer", "Once per customer", d.oncePerCustomer());
        form.check("combinable", "Combines with other discounts", d.combinable());
        form.check("active", "Active", d.active());
        v.forms.add(form);
        if (id == null) {
            v.heading = "New discount";
            return v.heading;
        }
        DiscountRulesDto r = sales.rules(me(), storeId, id);
        FormView rules = new FormView("rules", "Stacking and rules", base + "/" + id + "/rules",
                "Save rules");
        rules.text = "Which classes of discount this one combines with, who may use it, and the "
                + "tiers of a tiered, volume or bundle discount (JSON).";
        rules.add("discountClass", "Class", "select", r.discountClass()).option("", "From its kind")
                .option(DiscountEngine.CLASS_PRODUCT, "Product")
                .option(DiscountEngine.CLASS_ORDER, "Order")
                .option(DiscountEngine.CLASS_SHIPPING, "Shipping");
        rules.check("combinesProduct", "Combines with product discounts", r.combinesProduct());
        rules.check("combinesOrder", "Combines with order discounts", r.combinesOrder());
        rules.check("combinesShipping", "Combines with shipping discounts", r.combinesShipping());
        rules.add("eligibility", "Only for (TIER:<id> or CLUB:<id>, empty for everyone)", "text",
                r.eligibility());
        rules.add("rules", "Rules (JSON)", "textarea", r.rules());
        v.forms.add(rules);
        FormView del = new FormView("delete", "Delete", base + "/" + id + "/delete",
                "Delete discount");
        del.danger = true;
        del.confirm = "Delete this discount?";
        v.forms.add(del);
        v.heading = name(d);
        return v.heading;
    }

    String saveDiscount(String storeId, String id, HttpServer.Request request) throws IOException {
        DiscountDto old = id == null ? null : discount(storeId, id);
        String kind = f(request, "kind");
        String amountText = f(request, "amount");
        long amount = amountText.length() == 0 && DiscountEngine.FREE_SHIPPING.equals(kind) ? 0
                : AdminForms.cents(amountText, "the amount");
        String min = f(request, "minSubtotal");
        long starts = AdminForms.dayStart(f(request, "starts"));
        DiscountDto in = new DiscountDto(id, f(request, "code"), f(request, "title"), kind, amount,
                min.length() == 0 ? 0 : AdminForms.cents(min, "the minimum order"),
                whole(request, "buyQty", 0, "buy units"), whole(request, "getQty", 0, "get units"),
                old == null ? DiscountEngine.ALL : old.appliesTo(),
                old == null ? "" : old.targetId(), on(request, "automatic"),
                whole(request, "usageLimit", 0, "the uses"), old == null ? 0 : old.usedCount(),
                on(request, "oncePerCustomer"), on(request, "combinable"),
                starts < 0 ? 0 : starts, dayEnd(f(request, "ends")), on(request, "active"));
        return discounts.saveDiscount(me(), storeId, in).id();
    }

    void saveRules(String storeId, String id, HttpServer.Request request) throws IOException {
        sales.saveRules(me(), storeId, id, new DiscountRulesDto(f(request, "discountClass"),
                on(request, "combinesProduct"), on(request, "combinesOrder"),
                on(request, "combinesShipping"), f(request, "eligibility"), f(request, "rules")));
    }

    void deleteDiscount(String storeId, String id) throws IOException {
        discounts.deleteDiscount(me(), storeId, id);
    }

    // ============================================================== loyalty

    void loyalty(String storeId, MarketingView v) throws IOException {
        LoyaltyProgramDto p = loyalty.program(me(), storeId);
        String cur = admin.currency(storeId);
        FormView form = new FormView("programme", "Programme", "/admin/" + storeId + "/loyalty",
                "Save programme");
        form.check("active", "The programme is running", p.active());
        form.add("pointsPerUnit", "Points earned", "number", Integer.toString(p.pointsPerUnit()));
        form.add("unitCents", "for every (amount spent)", "text", plain(p.unitCents()));
        form.add("rounding", "Rounding", "select", p.rounding()).option("DOWN", "Down")
                .option("UP", "Up").option("NEAREST", "To the nearest");
        form.add("pointsPerReward", "Points per reward", "number",
                Integer.toString(p.pointsPerReward()));
        form.add("rewardCents", "A reward is worth", "text", plain(p.rewardCents()));
        form.add("minRedeemPoints", "Fewest points to redeem", "number",
                Integer.toString(p.minRedeemPoints()));
        form.add("expiryDays", "Points expire after days (0 for never)", "number",
                Integer.toString(p.expiryDays()));
        form.check("earnOnPos", "Earn on in-store sales", p.earnOnPos());
        form.add("tierBasis", "Tiers by", "select", p.tierBasis())
                .option(Loyalty.SPEND, "Spend (in cents)").option(Loyalty.POINTS, "Points earned");
        form.add("referrerPoints", "Points for a referral", "number",
                Integer.toString(p.referrerPoints()));
        form.add("refereePoints", "Points for the friend referred", "number",
                Integer.toString(p.refereePoints()));
        List<LoyaltyTierDto> tiers = p.tiers() == null ? new ArrayList<LoyaltyTierDto>() : p.tiers();
        for (int iter = 0; iter <= tiers.size(); iter++) {
            LoyaltyTierDto t = iter < tiers.size() ? tiers.get(iter) : null;
            String n = "t" + iter + "_";
            String label = t == null ? "New tier" : "Tier " + (iter + 1);
            form.add(n + "id", "", "hidden", t == null ? "" : t.id());
            form.add(n + "name", label + ": name (empty removes it)", "text",
                    t == null ? "" : t.name());
            form.add(n + "threshold", label + ": from", "number",
                    t == null ? "" : Long.toString(t.threshold()));
            form.add(n + "bps", label + ": member discount %", "text",
                    t == null ? "" : percent(t.memberBps()));
            form.add(n + "early", label + ": early access hours", "number",
                    t == null ? "" : Integer.toString(t.earlyAccessHours()));
            form.check(n + "ship", label + ": free shipping", t != null && t.freeShipping());
        }
        form.add("tiers", "", "hidden", Integer.toString(tiers.size() + 1));
        v.forms.add(form);
        v.rowsTitle = "Tiers";
        v.rowsEmpty = "No tiers yet.";
        v.headA = "Tier";
        v.headB = "From";
        v.headC = "Perks";
        v.headD = "Member price";
        for (int iter = 0; iter < tiers.size(); iter++) {
            LoyaltyTierDto t = tiers.get(iter);
            AdminRow r = new AdminRow();
            r.title = t.name();
            r.detail = Loyalty.SPEND.equals(p.tierBasis()) ? Money.format(t.threshold(), cur)
                    : t.threshold() + " points";
            StringBuilder perks = new StringBuilder();
            if (t.freeShipping()) {
                perks.append("Free shipping");
            }
            if (t.earlyAccessHours() > 0) {
                perks.append(perks.length() > 0 ? ", " : "").append(t.earlyAccessHours())
                        .append("h early access");
            }
            r.meta = perks.toString();
            r.amount = percent(t.memberBps()) + "% off";
            v.rows.add(r);
        }
    }

    DataTable members(String storeId, HttpServer.Request request) throws IOException {
        List<LoyaltyMemberDto> rows = loyalty.members(me(), storeId, null);
        Grid<LoyaltyMemberDto> g = new Grid<LoyaltyMemberDto>("members")
                .col("who", "Member", m -> m.email() != null && m.email().length() > 0 ? m.email()
                        : m.username()).sortBy(m -> m.username())
                .col("tier", "Tier", m -> m.tierName() == null ? "" : m.tierName())
                .col("balance", "Points", m -> Long.toString(m.balance())).num()
                    .sortBy(m -> Long.valueOf(m.balance()))
                .col("lifetime", "Earned in all", m -> Long.toString(m.lifetime())).num()
                    .sortBy(m -> Long.valueOf(m.lifetime()))
                .col("since", "Since", m -> AdminData.date(m.createdAt()))
                .defaultSort("lifetime", true);
        return table(g.table(rows, Grid.Query.of(request), "/admin/" + storeId + "/loyalty"),
                "Search members", "No members yet.");
    }

    void saveLoyalty(String storeId, HttpServer.Request request) throws IOException {
        LoyaltyProgramDto old = loyalty.program(me(), storeId);
        int count = Math.min(whole(request, "tiers", 0, "tiers"), 20);
        List<LoyaltyTierDto> tiers = new ArrayList<LoyaltyTierDto>();
        for (int iter = 0; iter < count; iter++) {
            String n = "t" + iter + "_";
            String name = f(request, n + "name");
            if (name.length() == 0) {
                continue;
            }
            String id = f(request, n + "id");
            String colour = "";
            for (int k = 0; old.tiers() != null && k < old.tiers().size(); k++) {
                if (old.tiers().get(k).id().equals(id)) {
                    colour = old.tiers().get(k).colour();
                }
            }
            String threshold = f(request, n + "threshold");
            long th;
            try {
                th = threshold.length() == 0 ? 0 : Long.parseLong(threshold);
            } catch (NumberFormatException bad) {
                throw new ResponseStatusException(400, "Write the tier's threshold as a whole number");
            }
            tiers.add(new LoyaltyTierDto(id.length() == 0 ? null : id, name, th,
                    bps(f(request, n + "bps"), "the member discount"), on(request, n + "ship"),
                    whole(request, n + "early", 0, "the early access hours"), colour,
                    tiers.size()));
        }
        String unit = f(request, "unitCents");
        String reward = f(request, "rewardCents");
        loyalty.saveProgram(me(), storeId, new LoyaltyProgramDto(on(request, "active"),
                whole(request, "pointsPerUnit", old.pointsPerUnit(), "the points"),
                unit.length() == 0 ? old.unitCents() : AdminForms.cents(unit, "the amount spent"),
                f(request, "rounding"),
                whole(request, "pointsPerReward", old.pointsPerReward(), "points per reward"),
                reward.length() == 0 ? old.rewardCents() : AdminForms.cents(reward, "the reward"),
                whole(request, "minRedeemPoints", old.minRedeemPoints(), "the fewest points"),
                whole(request, "expiryDays", old.expiryDays(), "the expiry"),
                on(request, "earnOnPos"), f(request, "tierBasis"),
                whole(request, "referrerPoints", old.referrerPoints(), "the referral points"),
                whole(request, "refereePoints", old.refereePoints(), "the friend's points"),
                tiers));
    }

    // ================================================================ clubs

    DataTable clubs(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final String cur = admin.currency(storeId);
        final String root = "/admin/" + storeId + "/clubs/";
        Grid<ClubDto> g = new Grid<ClubDto>("clubs")
                .col("name", "Club", c -> c.name()).sortBy(c -> c.name()).link(c -> root + c.id())
                .col("fee", "Fee", c -> c.feeCents() == 0 ? "Free"
                        : Money.format(c.feeCents(), cur) + " / " + c.periodDays() + " days")
                .col("perk", "Member price", c -> percent(c.memberBps()) + "% off")
                .col("members", "Members", c -> Long.toString(c.members())).num()
                    .sortBy(c -> Long.valueOf(c.members()))
                .col("status", "Status", c -> c.active() ? "Open" : "Closed")
                    .pill(c -> c.active() ? "ok" : "")
                .rows(c -> c.id(), c -> root + c.id())
                .defaultSort("name", false);
        v.forms.add(clubForm(new ClubDto(null, "", "", 0, 30, 0, 0, "", true, 0),
                "/admin/" + storeId + "/clubs", "Start a club", "Create club"));
        return table(g.table(clubs.list(me(), storeId), Grid.Query.of(request),
                "/admin/" + storeId + "/clubs"), "Search clubs", "No clubs yet.");
    }

    private static FormView clubForm(ClubDto c, String action, String title, String submit) {
        FormView form = new FormView("club", title, action, submit);
        form.add("name", "Name", "text", c.name()).required = true;
        form.add("description", "Description", "textarea", c.description());
        form.add("fee", "Fee (0 for free)", "text", plain(c.feeCents()));
        form.add("periodDays", "Renews every (days)", "number", Integer.toString(c.periodDays()));
        form.add("bps", "Member discount %", "text", percent(c.memberBps()));
        form.add("early", "Early access hours", "number", Integer.toString(c.earlyAccessHours()));
        form.check("active", "Open to join", c.active());
        return form;
    }

    private ClubDto club(String storeId, String id) throws IOException {
        List<ClubDto> all = clubs.list(me(), storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(id)) {
                return all.get(iter);
            }
        }
        throw new ResponseStatusException(404, "No such club");
    }

    String club(String storeId, String id, MarketingView v) throws IOException {
        ClubDto c = club(storeId, id);
        v.back = "/admin/" + storeId + "/clubs";
        v.backLabel = "Clubs";
        v.heading = c.name();
        v.forms.add(clubForm(c, v.back + "/" + id, "Club", "Save club"));
        v.rowsTitle = "Members";
        v.rowsEmpty = "No members yet.";
        v.headA = "Member";
        v.headB = "Status";
        v.headC = "Joined";
        v.headD = "Renews";
        List<ClubMembershipDto> ms = clubs.members(me(), storeId, id);
        for (int iter = 0; iter < ms.size(); iter++) {
            ClubMembershipDto m = ms.get(iter);
            AdminRow r = new AdminRow();
            r.title = m.username();
            r.detail = AdminData.words(m.status());
            r.meta = AdminData.date(m.joinedAt());
            r.amount = AdminData.date(m.renewsAt());
            v.rows.add(r);
        }
        return c.name();
    }

    String saveClub(String storeId, String id, HttpServer.Request request) throws IOException {
        ClubDto old = id == null ? null : club(storeId, id);
        String fee = f(request, "fee");
        return clubs.save(me(), storeId, id, new ClubDto(id, f(request, "name"),
                f(request, "description"), fee.length() == 0 ? 0 : AdminForms.cents(fee, "the fee"),
                whole(request, "periodDays", 30, "the period"),
                bps(f(request, "bps"), "the member discount"),
                whole(request, "early", 0, "the early access hours"),
                old == null ? "" : old.colour(), on(request, "active"), 0)).id();
    }

    // ============================================================= segments

    DataTable segments(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final String root = "/admin/" + storeId + "/segments/";
        Grid<AudienceDto> g = new Grid<AudienceDto>("segments")
                .col("name", "Segment", a -> a.name()).sortBy(a -> a.name()).link(a -> root + a.id())
                .col("count", "Customers", a -> Long.toString(a.count())).num()
                    .sortBy(a -> Long.valueOf(a.count()))
                .rows(a -> a.id(), a -> root + a.id())
                .defaultSort("name", false);
        v.links.add(link("New segment", "/admin/" + storeId + "/new-segment"));
        v.links.add(link("Campaigns", "/admin/" + storeId + "/campaigns"));
        return table(g.table(audiences.list(me(), storeId), Grid.Query.of(request),
                "/admin/" + storeId + "/segments"), "Search segments", "No segments yet.");
    }

    private AudienceDto segment(String storeId, String id) throws IOException {
        List<AudienceDto> all = audiences.list(me(), storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(id)) {
                return all.get(iter);
            }
        }
        throw new ResponseStatusException(404, "No such segment");
    }

    /// The rule builder. The rows come from the request when the page was
    /// asked for with them -- the add-row and count buttons post the form to
    /// a route that only draws this page again (a GET override cannot carry
    /// the CSRF token) -- else from the saved rules. Groups nested inside the top
    /// level (the app's editor makes them) ride along untouched in a hidden
    /// field. The count is the service's preview of the rows on screen.
    String segmentEditor(String storeId, String id, HttpServer.Request request, MarketingView v)
            throws IOException {
        String base = "/admin/" + storeId + "/segments";
        AudienceDto a = id == null ? null : segment(storeId, id);
        v.back = base;
        v.backLabel = "Segments";
        v.segNew = id == null;
        v.segAction = id == null ? base : base + "/" + id;
        v.segUrl = id == null ? "/admin/" + storeId + "/new-segment" : base + "/" + id + "/edit";
        List<String[]> rows = new ArrayList<String[]>();
        List<Object> keep = new ArrayList<Object>();
        String op = "AND";
        String name = a == null ? "" : a.name();
        if (f(request, "rows").length() > 0) {
            name = f(request, "name");
            op = "OR".equals(f(request, "op")) ? "OR" : "AND";
            rows = posted(request);
            keep = kept(f(request, "keep"));
        } else if (a != null && a.rules() != null && a.rules().trim().length() > 0) {
            Map tree;
            try {
                tree = Json.parseObject(a.rules());
            } catch (IOException | RuntimeException unreadable) {
                tree = null;
            }
            if (tree != null) {
                op = "OR".equals(String.valueOf(tree.get("op"))) ? "OR" : "AND";
                Object list = tree.get("rules");
                if (list instanceof List) {
                    List items = (List) list;
                    for (int iter = 0; iter < items.size(); iter++) {
                        Object o = items.get(iter);
                        if (o instanceof Map && ((Map) o).get("field") != null) {
                            Map c = (Map) o;
                            rows.add(new String[] {String.valueOf(c.get("field")),
                                String.valueOf(c.get("op")), shown(c)});
                        } else {
                            keep.add(o);
                        }
                    }
                }
            }
        }
        if (on(request, "add") || rows.isEmpty()) {
            rows.add(new String[] {"totalSpent", ">=", ""});
        }
        v.segName = name;
        v.segOp = op;
        v.segKeep = keep.isEmpty() ? "" : Json.write(keep);
        int shown = Math.min(rows.size(), MAX_RULES);
        v.segRows = Integer.toString(shown);
        for (int iter = 0; iter < shown; iter++) {
            RuleRow r = new RuleRow();
            r.label = "Condition " + (iter + 1);
            r.fieldName = "f" + iter;
            r.opName = "o" + iter;
            r.valueName = "v" + iter;
            r.removeName = "x" + iter;
            r.value = rows.get(iter)[2];
            for (int k = 0; k < FIELDS.length; k++) {
                r.fields.add(choice(FIELDS[k][0], FIELDS[k][1], rows.get(iter)[0]));
            }
            for (int k = 0; k < OPS.length; k++) {
                r.ops.add(choice(OPS[k][0], OPS[k][1], rows.get(iter)[1]));
            }
            v.rules.add(r);
        }
        // An unfinished last row (no value yet) is left out of the count.
        List<String[]> counted = new ArrayList<String[]>();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter)[2].length() > 0) {
                counted.add(rows.get(iter));
            }
        }
        try {
            AudiencePreviewDto p = audiences.preview(me(), storeId, new AudienceDto(id, name,
                    tree(op, counted, keep), 0));
            v.segCount = p.count() == 1 ? "1 customer matches" : p.count() + " customers match";
            for (int iter = 0; p.sample() != null && iter < p.sample().size(); iter++) {
                AudienceMemberDto m = p.sample().get(iter);
                v.sample.add(fact(m.name() == null || m.name().length() == 0 ? m.email() : m.name(),
                        m.email()));
            }
        } catch (ResponseStatusException bad) {
            if (bad.getStatus() != 400) {
                throw bad;
            }
            v.segError = bad.getReason() == null ? "Those conditions do not work" : bad.getReason();
        }
        if (id != null) {
            FormView del = new FormView("delete", "Delete", base + "/" + id + "/delete",
                    "Delete segment");
            del.danger = true;
            del.confirm = "Delete this segment?";
            v.forms.add(del);
        }
        v.heading = id == null ? "New segment" : a.name();
        return v.heading;
    }

    private static AdminRow choice(String id, String title, String selected) {
        AdminRow r = new AdminRow();
        r.id = id;
        r.title = title;
        r.flag = id.equals(selected);
        return r;
    }

    /// A condition's value as the form shows it: spend as money, whole
    /// numbers without a fraction, the rest as written.
    private static String shown(Map c) {
        Object v = c.get("value");
        if (v == null) {
            return "";
        }
        if (v instanceof Number) {
            long n = ((Number) v).longValue();
            return "totalSpent".equals(String.valueOf(c.get("field"))) ? plain(n)
                    : Long.toString(n);
        }
        return String.valueOf(v);
    }

    /// The rows of a submitted builder; a row whose remove box is ticked
    /// (`x<n>`) is dropped.
    private static List<String[]> posted(HttpServer.Request request) {
        int n = Math.min(whole(request, "rows", 0, "rows"), MAX_RULES);
        List<String[]> out = new ArrayList<String[]>();
        for (int iter = 0; iter < n; iter++) {
            String field = f(request, "f" + iter);
            if (field.length() == 0 || on(request, "x" + iter)) {
                continue;
            }
            out.add(new String[] {field, f(request, "o" + iter), f(request, "v" + iter)});
        }
        return out;
    }

    private static List<Object> kept(String json) {
        List<Object> out = new ArrayList<Object>();
        if (json.length() == 0) {
            return out;
        }
        try {
            Object o = Json.parse(json);
            if (o instanceof List) {
                out.addAll((List) o);
            }
        } catch (IOException | RuntimeException unreadable) {
            // A hidden field that does not read keeps nothing, and the
            // service checks whatever is saved.
            out.clear();
        }
        return out;
    }

    /// The rule tree the segment compiler reads: numbers for the numeric and
    /// day fields, a boolean for marketing consent, text for the rest. A value
    /// that is not of its field's kind goes on as text, and the compiler's 400
    /// says why.
    static String tree(String op, List<String[]> rows, List<Object> keep) {
        List<Object> rules = new ArrayList<Object>();
        for (int iter = 0; iter < rows.size(); iter++) {
            String[] r = rows.get(iter);
            Map<String, Object> c = new LinkedHashMap<String, Object>();
            c.put("field", r[0]);
            c.put("op", r[1]);
            c.put("value", value(r[0], r[2]));
            rules.add(c);
        }
        rules.addAll(keep);
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("op", op);
        root.put("rules", rules);
        return Json.write(root);
    }

    private static Object value(String field, String text) {
        if ("acceptsMarketing".equals(field)) {
            return Boolean.valueOf("true".equals(text) || "yes".equals(text) || "1".equals(text));
        }
        if ("totalSpent".equals(field)) {
            try {
                return Long.valueOf(AdminForms.cents(text, "the amount spent"));
            } catch (ResponseStatusException notMoney) {
                return text;
            }
        }
        if ("ordersCount".equals(field) || "lastOrderAt".equals(field)
                || "createdAt".equals(field)) {
            try {
                return Long.valueOf(Long.parseLong(text.trim()));
            } catch (NumberFormatException notANumber) {
                return text;
            }
        }
        return text;
    }

    String saveSegment(String storeId, String id, HttpServer.Request request) throws IOException {
        String op = "OR".equals(f(request, "op")) ? "OR" : "AND";
        String rules = tree(op, posted(request), kept(f(request, "keep")));
        return audiences.save(me(), storeId, id, new AudienceDto(id, f(request, "name"), rules, 0))
                .id();
    }

    void deleteSegment(String storeId, String id) throws IOException {
        audiences.remove(me(), storeId, id);
    }

    // ============================================================ campaigns

    DataTable campaigns(String storeId, HttpServer.Request request, MarketingView v)
            throws IOException {
        final String root = "/admin/" + storeId + "/campaigns/";
        Grid<CampaignDto> g = new Grid<CampaignDto>("campaigns")
                .col("name", "Campaign", c -> c.name()).sortBy(c -> c.name()).link(c -> root + c.id())
                .col("channel", "Channel", c -> AdminData.words(c.channel()))
                .col("when", "Scheduled", c -> Campaigns.SCHEDULED.equals(c.status())
                        ? AdminData.date(c.scheduledAt()) : "")
                    .sortBy(c -> Long.valueOf(c.scheduledAt()))
                .col("status", "Status", c -> AdminData.words(c.status()))
                    .pill(c -> Campaigns.SENT.equals(c.status()) ? "ok"
                        : Campaigns.CANCELLED.equals(c.status()) ? "bad" : "warn")
                .rows(c -> c.id(), c -> root + c.id())
                .defaultSort("name", false);
        v.links.add(link("New campaign", "/admin/" + storeId + "/new-campaign"));
        v.links.add(link("Segments", "/admin/" + storeId + "/segments"));
        return table(g.table(campaigns.list(me(), storeId), Grid.Query.of(request),
                "/admin/" + storeId + "/campaigns"), "Search campaigns", "No campaigns yet.");
    }

    private CampaignDto campaign(String storeId, String id) throws IOException {
        List<CampaignDto> all = campaigns.list(me(), storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(id)) {
                return all.get(iter);
            }
        }
        throw new ResponseStatusException(404, "No such campaign");
    }

    String campaignEditor(String storeId, String id, MarketingView v) throws IOException {
        String base = "/admin/" + storeId + "/campaigns";
        v.back = base;
        v.backLabel = "Campaigns";
        List<AudienceDto> segs = audiences.list(me(), storeId);
        CampaignDto c = id == null ? new CampaignDto(null, "", Campaigns.EMAIL,
                segs.isEmpty() ? "" : segs.get(0).id(), "", "", "", 10, 60, Campaigns.DRAFT, 0, "",
                "", new ArrayList<String>()) : campaign(storeId, id);
        boolean editable = Campaigns.DRAFT.equals(c.status())
                || Campaigns.SCHEDULED.equals(c.status());
        if (editable) {
            FormView form = new FormView("campaign", "Message", id == null ? base : base + "/" + id,
                    "Save campaign");
            form.add("name", "Name", "text", c.name()).required = true;
            form.add("channel", "Channel", "select", c.channel()).option(Campaigns.EMAIL, "E-mail")
                    .option(Campaigns.SMS, "Text message");
            FormField aud = form.add("audienceId", "Send to", "select", c.audienceId());
            for (int iter = 0; iter < segs.size(); iter++) {
                aud.option(segs.get(iter).id(), segs.get(iter).name() + " ("
                        + segs.get(iter).count() + ")");
            }
            form.add("subjectA", "Subject", "text", c.subjectA());
            form.add("subjectB", "Second subject to test against (optional)", "text",
                    c.subjectB());
            form.add("abPercent", "Test on percent of the audience", "number",
                    Integer.toString(c.abPercent()));
            form.add("winnerAfterMinutes", "Pick the winner after (minutes)", "number",
                    Integer.toString(c.winnerAfterMinutes()));
            form.add("body", "Message", "textarea", c.body());
            StringBuilder links = new StringBuilder();
            for (int iter = 0; c.links() != null && iter < c.links().size(); iter++) {
                links.append(c.links().get(iter)).append('\n');
            }
            form.add("links", "Links, one per line (tracked)", "textarea", links.toString());
            v.forms.add(form);
        }
        if (id == null) {
            v.heading = "New campaign";
            return v.heading;
        }
        v.heading = c.name();
        v.facts.add(fact("Status", AdminData.words(c.status())));
        if (Campaigns.SCHEDULED.equals(c.status())) {
            v.facts.add(fact("Goes out", AdminData.date(c.scheduledAt())));
        }
        CampaignStatsDto s = campaigns.stats(me(), storeId, id);
        v.facts.add(fact("Audience", Long.toString(s.audience())));
        v.facts.add(fact("Sent", Long.toString(s.sent())));
        v.facts.add(fact("Opened", Long.toString(s.opened())));
        v.facts.add(fact("Clicked", Long.toString(s.clicked())));
        v.facts.add(fact("Orders", s.orders() + ", " + Money.format(s.revenueCents(),
                admin.currency(storeId))));
        if (s.winner() != null && s.winner().length() > 0) {
            v.facts.add(fact("Winning subject", s.winner()));
        }
        if (Campaigns.EMAIL.equals(c.channel())) {
            FormView test = new FormView("test", "Send a test", base + "/" + id + "/test",
                    "Send test");
            test.text = "One copy, marked as a test. It counts in no statistics.";
            test.add("to", "Send to", "email", "").required = true;
            v.forms.add(test);
        }
        if (editable) {
            FormView when = new FormView("schedule", "Schedule", base + "/" + id + "/schedule",
                    "Schedule");
            when.text = "Pick a day, or leave it empty to send now.";
            when.add("day", "Day", "date", Campaigns.SCHEDULED.equals(c.status())
                    ? AdminData.date(c.scheduledAt()) : "");
            v.forms.add(when);
            FormView cancel = new FormView("cancel", "Cancel", base + "/" + id + "/cancel",
                    "Cancel campaign");
            cancel.danger = true;
            cancel.confirm = "Cancel this campaign?";
            v.forms.add(cancel);
        }
        return c.name();
    }

    String saveCampaign(String storeId, String id, HttpServer.Request request) throws IOException {
        CampaignDto old = id == null ? null : campaign(storeId, id);
        String text = f(request, "links");
        List<String> links = new ArrayList<String>();
        int from = 0;
        while (from <= text.length()) {
            int nl = text.indexOf('\n', from);
            String line = (nl < 0 ? text.substring(from) : text.substring(from, nl)).trim();
            if (line.length() > 0) {
                links.add(line);
            }
            if (nl < 0) {
                break;
            }
            from = nl + 1;
        }
        return campaigns.save(me(), storeId, id, new CampaignDto(id, f(request, "name"),
                f(request, "channel"), f(request, "audienceId"), f(request, "subjectA"),
                f(request, "subjectB"), f(request, "body"),
                whole(request, "abPercent", 0, "the test percentage"),
                whole(request, "winnerAfterMinutes", 0, "the minutes"),
                null, 0, null, old == null ? "" : old.colour(), links)).id();
    }

    void sendTest(String storeId, String id, HttpServer.Request request) throws IOException {
        campaigns.sendTest(me(), storeId, id, f(request, "to"));
    }

    void schedule(String storeId, String id, HttpServer.Request request) throws IOException {
        String d = f(request, "day");
        campaigns.schedule(me(), storeId, id, d.length() == 0 ? 0 : day(d, "a day"));
    }

    void cancelCampaign(String storeId, String id) throws IOException {
        campaigns.cancel(me(), storeId, id);
    }

    // ============================================================= journeys

    DataTable journeys(String storeId, HttpServer.Request request) throws IOException {
        final String root = "/admin/" + storeId + "/journeys/";
        Grid<JourneyDto> g = new Grid<JourneyDto>("journeys")
                .col("name", "Journey", j -> j.name()).sortBy(j -> j.name()).link(j -> root + j.id())
                .col("status", "Status", j -> AdminData.words(j.status()))
                    .pill(j -> AdminData.tone(j.status()))
                .col("problems", "Problems", j -> j.problems() == null || j.problems().isEmpty()
                        ? "None" : Integer.toString(j.problems().size()))
                .col("created", "Created", j -> AdminData.date(j.createdAt()))
                    .sortBy(j -> Long.valueOf(j.createdAt()))
                .rows(j -> j.id(), j -> root + j.id())
                .defaultSort("name", false);
        return table(g.table(journeys.list(me(), storeId), Grid.Query.of(request),
                "/admin/" + storeId + "/journeys"), "Search journeys", "No journeys yet.");
    }

    static final int NODE_W = 190;
    static final int NODE_H = 70;
    static final int GAP_X = 70;
    static final int GAP_Y = 26;
    static final int PAD = 20;

    /// A read-only flowchart of the journey graph, laid out by the server:
    /// each step in the column of its longest path from the entry, edges as
    /// curves from an output port to an input port, labelled with the port
    /// (`yes`, `no`, `a`, `b`) of a split, and each step with the counts
    /// [Journeys] keeps. The graph is the JSON the app's NodeGraph editor
    /// writes: `nodes` with `id` and `data.type` (or `style`), `edges` with
    /// `from`, `fromPort` and `to`.
    String journey(String storeId, String id, MarketingView v) throws IOException {
        JourneyDto j = journeys.get(me(), storeId, id);
        List<JourneyNodeStatDto> stats = journeys.stats(me(), storeId, id);
        v.back = "/admin/" + storeId + "/journeys";
        v.backLabel = "Journeys";
        v.heading = j.name();
        v.intro = "Steps are edited in the app; this is the journey as it runs, with how many "
                + "customers passed each step and how many wait on it now.";
        v.facts.add(fact("Status", AdminData.words(j.status())));
        for (int iter = 0; j.problems() != null && iter < j.problems().size(); iter++) {
            v.problems.add(fact(j.problems().get(iter), ""));
        }
        Map root;
        try {
            root = Json.parseObject(j.graphJson());
        } catch (IOException | RuntimeException unreadable) {
            root = null;
        }
        if (root == null) {
            return j.name();
        }
        List<String> ids = new ArrayList<String>();
        Map<String, String> types = new HashMap<String, String>();
        Object nl = root.get("nodes");
        if (nl instanceof List) {
            List ns = (List) nl;
            for (int iter = 0; iter < ns.size() && ids.size() < 200; iter++) {
                if (!(ns.get(iter) instanceof Map)) {
                    continue;
                }
                Map n = (Map) ns.get(iter);
                String nid = text(n.get("id"));
                if (nid.length() == 0 || types.containsKey(nid)) {
                    continue;
                }
                String type = "";
                Object data = n.get("data");
                if (data instanceof Map) {
                    type = text(((Map) data).get("type"));
                }
                if (type.length() == 0) {
                    type = text(n.get("style"));
                }
                if (type.length() == 0) {
                    type = text(n.get("type"));
                }
                ids.add(nid);
                types.put(nid, type);
            }
        }
        List<String[]> edges = new ArrayList<String[]>();
        Object el = root.get("edges");
        if (el instanceof List) {
            List es = (List) el;
            for (int iter = 0; iter < es.size() && edges.size() < 400; iter++) {
                if (!(es.get(iter) instanceof Map)) {
                    continue;
                }
                Map e = (Map) es.get(iter);
                String from = text(e.get("from"));
                String to = text(e.get("to"));
                if (types.containsKey(from) && types.containsKey(to)) {
                    edges.add(new String[] {from, text(e.get("fromPort")), to});
                }
            }
        }
        layout(ids, types, edges, stats, v);
        return j.name();
    }

    private static void layout(List<String> ids, Map<String, String> types, List<String[]> edges,
            List<JourneyNodeStatDto> stats, MarketingView v) {
        // Longest-path depth, at most one relaxation pass per node: a loop
        // (which the service refuses to activate) stops growing, never spins.
        Map<String, Integer> depth = new HashMap<String, Integer>();
        for (int iter = 0; iter < ids.size(); iter++) {
            depth.put(ids.get(iter), Integer.valueOf(0));
        }
        for (int pass = 0; pass < ids.size(); pass++) {
            boolean moved = false;
            for (int iter = 0; iter < edges.size(); iter++) {
                int d = depth.get(edges.get(iter)[0]).intValue() + 1;
                if (d > depth.get(edges.get(iter)[2]).intValue() && d < ids.size()) {
                    depth.put(edges.get(iter)[2], Integer.valueOf(d));
                    moved = true;
                }
            }
            if (!moved) {
                break;
            }
        }
        Map<String, FlowNode> placed = new HashMap<String, FlowNode>();
        List<Integer> perColumn = new ArrayList<Integer>();
        int maxX = 0;
        int maxY = 0;
        for (int iter = 0; iter < ids.size(); iter++) {
            String nid = ids.get(iter);
            int col = depth.get(nid).intValue();
            while (perColumn.size() <= col) {
                perColumn.add(Integer.valueOf(0));
            }
            int row = perColumn.get(col).intValue();
            perColumn.set(col, Integer.valueOf(row + 1));
            FlowNode n = new FlowNode();
            n.id = nid;
            NodeTypeDto t = JourneyCatalog.find(types.get(nid));
            n.title = t == null ? types.get(nid) : t.label();
            n.kind = t == null ? "step" : kindOf(t.kind());
            n.detail = t == null ? "Not a journey step" : Character.toUpperCase(n.kind.charAt(0))
                    + n.kind.substring(1);
            long passed = 0;
            long current = 0;
            for (int k = 0; k < stats.size(); k++) {
                if (stats.get(k).nodeId().equals(nid)) {
                    passed = stats.get(k).passed();
                    current = stats.get(k).current();
                }
            }
            n.counts = passed + " passed, " + current + " here now";
            n.x = PAD + col * (NODE_W + GAP_X);
            n.y = PAD + row * (NODE_H + GAP_Y);
            n.width = NODE_W;
            n.height = NODE_H;
            n.textX = n.x + 12;
            n.titleY = n.y + 22;
            n.detailY = n.y + 40;
            n.countsY = n.y + 58;
            n.inX = n.x;
            n.outX = n.x + NODE_W;
            n.portY = n.y + NODE_H / 2;
            n.hasIn = !"entry".equals(n.kind);
            n.hasOut = !"end".equals(n.kind);
            placed.put(nid, n);
            v.nodes.add(n);
            maxX = Math.max(maxX, n.x + NODE_W);
            maxY = Math.max(maxY, n.y + NODE_H);
            AdminRow r = new AdminRow();
            r.title = n.title;
            r.detail = nid;
            r.meta = Long.toString(passed);
            r.amount = Long.toString(current);
            v.rows.add(r);
        }
        for (int iter = 0; iter < edges.size(); iter++) {
            FlowNode a = placed.get(edges.get(iter)[0]);
            FlowNode b = placed.get(edges.get(iter)[2]);
            FlowEdge e = new FlowEdge();
            int x1 = a.outX;
            int y1 = a.portY;
            int x2 = b.inX;
            int y2 = b.portY;
            int bend = Math.max(30, Math.abs(x2 - x1) / 2);
            e.path = "M" + x1 + " " + y1 + " C" + (x1 + bend) + " " + y1 + " " + (x2 - bend) + " "
                    + y2 + " " + x2 + " " + y2;
            String port = edges.get(iter)[1];
            e.label = JourneyCatalog.YES.equals(port) || JourneyCatalog.NO.equals(port)
                    || JourneyCatalog.A.equals(port) || JourneyCatalog.B.equals(port) ? port : "";
            e.labelX = x1 + 6;
            e.labelY = y1 - 6;
            v.edges.add(e);
        }
        v.flowBox = "0 0 " + (maxX + PAD) + " " + (maxY + PAD);
        v.rowsTitle = "Steps";
        v.headA = "Step";
        v.headB = "Id";
        v.headC = "Passed";
        v.headD = "Here now";
    }

    private static String kindOf(String kind) {
        if (JourneyCatalog.ENTRY.equals(kind)) {
            return "entry";
        }
        if (JourneyCatalog.SPLIT.equals(kind)) {
            return "split";
        }
        return JourneyCatalog.END.equals(kind) ? "end" : "step";
    }

    private static String text(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
