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
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.analytics.AnalyticsEndpoint;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.AdjustDto;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.CustomerDetailDto;
import com.codenameone.examples.bazaar.api.CustomerDto;
import com.codenameone.examples.bazaar.api.CustomerOrderDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.KpiDto;
import com.codenameone.examples.bazaar.api.LevelDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderEventDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.SavedAddressDto;
import com.codenameone.examples.bazaar.api.StoreRoleDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.catalog.CatalogEndpoint;
import com.codenameone.examples.bazaar.customer.CustomerEndpoint;
import com.codenameone.examples.bazaar.fulfil.FulfillmentEndpoint;
import com.codenameone.examples.bazaar.inventory.InventoryEndpoint;
import com.codenameone.examples.bazaar.order.OrderEndpoint;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.store.StoreEndpoint;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.table.DataTable;
import com.codenameone.examples.bazaar.web.table.Grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// What the back-office pages show and do, through the same endpoint beans
/// the app's API answers with. Every call passes the signed-in name and the
/// store from the address, and the services behind them check the caller's
/// role at that store and that each row belongs to it: an order, product or
/// customer of another store is a 404 there, and so here. Nothing here
/// repeats a business rule; it turns form fields into the services' DTOs and
/// DTOs into rows.
@Component
public class AdminData {
    /// The adjust reasons the stock service accepts, as the form offers them.
    static final String[][] REASONS = {
        {"CORRECTION", "Correction"}, {"RESTOCK", "Restock"}, {"DAMAGE", "Damaged"},
        {"RETURN", "Returned"},
    };
    static final String[] PRODUCT_STATUSES = {"ACTIVE", "DRAFT", "ARCHIVED"};

    private final OrderEndpoint orders;
    private final FulfillmentEndpoint fulfillments;
    private final CatalogEndpoint catalog;
    private final InventoryEndpoint inventory;
    private final CustomerEndpoint customers;
    private final AnalyticsEndpoint analytics;
    private final StoreEndpoint storeApi;
    private final Stores stores;

    public AdminData(OrderEndpoint orders, FulfillmentEndpoint fulfillments,
            CatalogEndpoint catalog, InventoryEndpoint inventory, CustomerEndpoint customers,
            AnalyticsEndpoint analytics, StoreEndpoint storeApi, Stores stores) {
        this.orders = orders;
        this.fulfillments = fulfillments;
        this.catalog = catalog;
        this.inventory = inventory;
        this.customers = customers;
        this.analytics = analytics;
        this.storeApi = storeApi;
        this.stores = stores;
    }

    /// The store's currency, from the caller's own store list (a 404 when
    /// they do not work there, as everywhere else).
    String currency(String storeId) throws IOException {
        List<StoreRoleDto> mine = stores.myStores(Caller.name());
        for (int iter = 0; iter < mine.size(); iter++) {
            if (mine.get(iter).store().id().equals(storeId)) {
                return mine.get(iter).store().currency();
            }
        }
        throw new ResponseStatusException(404, "No such store");
    }

    static String date(long millis) {
        return millis <= 0 ? "" : Days.iso(millis);
    }

    static String tone(String status) {
        if (status == null) {
            return "";
        }
        if ("PAID".equals(status) || "FULFILLED".equals(status) || "ACTIVE".equals(status)
                || "CLOSED".equals(status)) {
            return "ok";
        }
        if ("CANCELLED".equals(status) || "REFUNDED".equals(status) || "VOIDED".equals(status)
                || "ARCHIVED".equals(status)) {
            return "bad";
        }
        return "warn";
    }

    static String words(String status) {
        if (status == null || status.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(status.length());
        for (int iter = 0; iter < status.length(); iter++) {
            char c = status.charAt(iter);
            if (c == '_') {
                b.append(' ');
            } else if (iter > 0 && c >= 'A' && c <= 'Z') {
                b.append((char) (c + ('a' - 'A')));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    // ------------------------------------------------------------ dashboard

    void dashboard(String storeId, AdminView v) throws Exception {
        long now = System.currentTimeMillis();
        long to = Days.start(now) + Days.MILLIS;
        long from = to - 30 * Days.MILLIS;
        AnalyticsDto a;
        try {
            a = analytics.dashboard(storeId, Long.toString(from), Long.toString(to));
        } catch (ResponseStatusException e) {
            if (e.getStatus() != 403) {
                throw e;
            }
            // Fulfilment and cashier staff see the store's work, not its money.
            v.figures = false;
            return;
        }
        v.figures = true;
        String cur = a.currency();
        KpiDto c = a.current();
        KpiDto p = a.previous();
        v.kpis.add(new Kpi("Net sales", Money.format(c.netSalesCents(), cur),
                c.netSalesCents(), p.netSalesCents()));
        v.kpis.add(new Kpi("Orders", Integer.toString(c.orders()), c.orders(), p.orders()));
        v.kpis.add(new Kpi("Average order", Money.format(c.averageOrderCents(), cur),
                c.averageOrderCents(), p.averageOrderCents()));
        v.kpis.add(new Kpi("Customers", Integer.toString(c.customers()), c.customers(),
                p.customers()));
        AdminCharts.sales(v, a.daily(), cur);
        AdminCharts.top(v, a.topProducts(), cur);
    }

    // --------------------------------------------------------------- orders

    /// The orders table. `status` and `since` (days) filter in the service's
    /// own query; search, sort and paging are the grid's.
    Tabled orders(String storeId, HttpServer.Request request, String base, boolean csv)
            throws Exception {
        final String cur = currency(storeId);
        String status = AdminForms.field(request, "status");
        if (!"OPEN".equals(status) && !"CLOSED".equals(status) && !"CANCELLED".equals(status)) {
            status = "";
        }
        String since = AdminForms.field(request, "since");
        if (!"7".equals(since) && !"30".equals(since) && !"90".equals(since)
                && !"365".equals(since)) {
            since = "";
        }
        String from = since.length() == 0 ? null : Long.toString(System.currentTimeMillis()
                - Long.parseLong(since) * Days.MILLIS);
        List<OrderDto> rows = orders.list(storeId, status.length() == 0 ? null : status, null,
                null, null, from, null, null);
        final String root = "/admin/" + storeId + "/orders/";
        Grid<OrderDto> g = new Grid<OrderDto>("orders")
                .col("number", "Order", o -> "#" + o.number()).sortBy(o -> Integer.valueOf(o.number()))
                    .link(o -> root + o.id())
                .col("date", "Date", o -> date(o.createdAt())).sortBy(o -> Long.valueOf(o.createdAt()))
                .col("customer", "Customer", o -> o.email() == null ? "" : o.email())
                    .sortBy(o -> o.email() == null ? "" : o.email())
                .col("payment", "Payment", o -> words(o.financial())).pill(o -> tone(o.financial()))
                .col("fulfilment", "Fulfilment", o -> words(o.fulfilment()))
                    .pill(o -> tone(o.fulfilment()))
                .col("status", "Status", o -> words(o.status())).pill(o -> tone(o.status()))
                .col("total", "Total", o -> Money.format(o.totalCents(), cur)).num()
                    .sortBy(o -> Long.valueOf(o.totalCents()))
                .rows(o -> o.id(), o -> root + o.id())
                .defaultSort("date", true);
        Tabled out = done(g, rows, request, base, storeId, "orders", csv,
                filterParam("status", status), filterParam("since", since));
        if (csv) {
            return out;
        }
        DataTable t = out.table;
        t.searchLabel = "Search orders";
        t.empty = "No orders match.";
        t.filters.add(filter("status", "Status", status, new String[][] {
            {"", "Any status"}, {"OPEN", "Open"}, {"CLOSED", "Closed"}, {"CANCELLED", "Cancelled"}}));
        t.filters.add(filter("since", "Placed", since, new String[][] {
            {"", "Any time"}, {"7", "Last 7 days"}, {"30", "Last 30 days"}, {"90", "Last 90 days"},
            {"365", "Last year"}}));
        return out;
    }

    /// A built table, or the CSV of the same rows when the export was asked for.
    static final class Tabled {
        DataTable table;
        byte[] csv;
    }

    /// Draws `rows` through `g` for the page at `base`, or writes them as CSV.
    /// The export link points at `/admin/{store}/export/{name}` with the same
    /// filters, search and sort, since a page answers HTML only.
    static <T> Tabled done(Grid<T> g, List<T> rows, HttpServer.Request request, String base,
            String storeId, String name, boolean csv, DataTable.Param... keep) {
        Tabled out = new Tabled();
        Grid.Query query = Grid.Query.of(request);
        if (csv) {
            out.csv = g.csv(rows, query);
            return out;
        }
        out.table = g.table(rows, query, base, keep);
        String link = out.table.csvUrl;
        int q = link.indexOf('?');
        out.table.csvUrl = "/admin/" + storeId + "/export/" + name + (q < 0 ? "" : link.substring(q));
        return out;
    }

    static DataTable.Param filterParam(String name, String value) {
        DataTable.Param p = new DataTable.Param(name, value);
        p.hidden = false;
        return p;
    }

    static DataTable.Filter filter(String name, String label, String chosen, String[][] options) {
        DataTable.Filter f = new DataTable.Filter();
        f.name = name;
        f.label = label;
        for (int iter = 0; iter < options.length; iter++) {
            f.options.add(new DataTable.Option(options[iter][0], options[iter][1],
                    options[iter][0].equals(chosen)));
        }
        return f;
    }

    /// The CSV of the same rows the table would show, every page.
    static HttpServer.Response csv(byte[] body, String name) {
        return new HttpServer.Response(200, "text/csv; charset=utf-8", body)
                .header("Content-Disposition", "attachment; filename=\"" + name + ".csv\"");
    }

    void order(String storeId, String orderId, AdminView v) throws Exception {
        OrderDto o = orders.detail(storeId, orderId);
        String cur = o.currency();
        String base = "/admin/" + storeId + "/orders/" + o.id();
        v.order.id = o.id();
        v.order.title = "Order #" + o.number();
        v.order.status = words(o.status());
        v.order.detail = words(o.financial()) + ", " + words(o.fulfilment());
        v.order.meta = date(o.createdAt());
        v.order.amount = Money.format(o.totalCents(), cur);
        v.customer = o.email() == null ? "" : o.email();
        v.shipTo = address(o.address());
        v.fulfilAction = base + "/fulfil";
        v.refundAction = base + "/refund";
        v.cancelAction = base + "/cancel";
        boolean open = Orders.OPEN.equals(o.status());
        boolean shippedAny = false;
        boolean anyOpen = false;
        boolean anyRefundable = false;
        List<OrderLineDto> ls = o.lines() == null ? new ArrayList<OrderLineDto>() : o.lines();
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLineDto l = ls.get(iter);
            int toSend = Math.max(0, l.quantity() - l.fulfilledQty() - l.refundedQty());
            int refundable = Math.max(0, l.quantity() - l.refundedQty());
            AdminRow r = new AdminRow();
            r.id = l.id();
            r.title = l.title();
            r.detail = l.variantTitle() == null ? "" : l.variantTitle();
            r.sku = l.sku() == null ? "" : l.sku();
            r.quantity = Integer.toString(l.quantity());
            r.meta = l.fulfilledQty() + " sent, " + l.refundedQty() + " refunded";
            r.price = Money.format(l.priceCents(), cur);
            r.amount = Money.format(l.priceCents() * l.quantity() - l.discountCents(), cur);
            r.field = "qty_" + l.id();
            r.field2 = "refund_" + l.id();
            r.max = Integer.toString(toSend);
            r.status = Integer.toString(refundable);
            v.lines.add(r);
            shippedAny |= l.fulfilledQty() > 0;
            anyOpen |= toSend > 0;
            anyRefundable |= refundable > 0;
        }
        total(v, "Subtotal", o.subtotalCents(), cur);
        if (o.discountCents() != 0) {
            total(v, "Discount", -o.discountCents(), cur);
        }
        total(v, "Shipping", o.shippingCents(), cur);
        total(v, "Tax", o.taxCents(), cur);
        total(v, "Total", o.totalCents(), cur);
        if (o.refundedCents() != 0) {
            total(v, "Refunded", -o.refundedCents(), cur);
        }
        List<OrderEventDto> ev = o.events() == null ? new ArrayList<OrderEventDto>() : o.events();
        for (int iter = 0; iter < ev.size(); iter++) {
            AdminRow r = new AdminRow();
            r.meta = date(ev.get(iter).createdAt());
            r.title = ev.get(iter).message() == null ? words(ev.get(iter).kind())
                    : ev.get(iter).message();
            r.detail = ev.get(iter).username() == null ? "" : ev.get(iter).username();
            v.events.add(r);
        }
        boolean paid = "PAID".equals(o.financial()) || "PARTIALLY_REFUNDED".equals(o.financial());
        v.canFulfil = open && anyOpen && !"PENDING".equals(o.financial());
        v.canRefund = paid && anyRefundable;
        v.canCancel = open && !shippedAny;
        if (v.canFulfil) {
            List<LocationDto> places = storeApi.locations(storeId);
            for (int iter = 0; iter < places.size(); iter++) {
                if (places.get(iter).active()) {
                    AdminRow r = new AdminRow();
                    r.id = places.get(iter).id();
                    r.title = places.get(iter).name();
                    v.locations.add(r);
                }
            }
        }
    }

    private static void total(AdminView v, String label, long cents, String cur) {
        AdminRow r = new AdminRow();
        r.title = label;
        r.amount = Money.format(cents, cur);
        v.totals.add(r);
    }

    static String address(AddressDto a) {
        if (a == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        String[] parts = {a.name(), a.line1(), a.line2(), a.city(), a.region(), a.postal(),
            a.country()};
        for (int iter = 0; iter < parts.length; iter++) {
            if (parts[iter] != null && parts[iter].length() > 0) {
                b.append(b.length() == 0 ? "" : ", ").append(parts[iter]);
            }
        }
        return b.toString();
    }

    /// Opens a fulfilment for the quantities typed against each line, from one location.
    void fulfil(String storeId, String orderId, HttpServer.Request request) throws Exception {
        OrderDto o = orders.detail(storeId, orderId);
        List<FulfilLineDto> chosen = new ArrayList<FulfilLineDto>();
        for (int iter = 0; iter < o.lines().size(); iter++) {
            OrderLineDto l = o.lines().get(iter);
            int q = AdminForms.number(AdminForms.field(request, "qty_" + l.id()), 0, "a quantity");
            if (q > 0) {
                chosen.add(new FulfilLineDto(l.id(), l.title(), l.sku(), q));
            }
        }
        String location = AdminForms.field(request, "location");
        fulfillments.create(storeId, orderId, new NewFulfillmentDto(
                location.length() == 0 ? null : location, "SHIP", chosen));
    }

    /// Refunds the quantities typed against each line, and any shipping amount.
    void refund(String storeId, String orderId, HttpServer.Request request) throws Exception {
        OrderDto o = orders.detail(storeId, orderId);
        List<RefundLineDto> chosen = new ArrayList<RefundLineDto>();
        for (int iter = 0; iter < o.lines().size(); iter++) {
            OrderLineDto l = o.lines().get(iter);
            int q = AdminForms.number(AdminForms.field(request, "refund_" + l.id()), 0,
                    "a quantity");
            if (q < 0) {
                throw new ResponseStatusException(400, "A quantity is not negative");
            }
            if (q > 0) {
                chosen.add(new RefundLineDto(l.id(), q));
            }
        }
        String ship = AdminForms.field(request, "shipping");
        long shipping = ship.length() == 0 ? 0 : AdminForms.cents(ship, "the shipping refund");
        boolean restock = "on".equals(AdminForms.field(request, "restock"));
        String reason = AdminForms.field(request, "reason");
        orders.refund(storeId, orderId, new RefundRequestDto(chosen, shipping, restock,
                reason.length() > 200 ? reason.substring(0, 200) : reason));
    }

    void cancel(String storeId, String orderId) throws Exception {
        orders.cancel(storeId, orderId);
    }

    // ------------------------------------------------------------- products

    /// Every product of the store, in pages of the service's largest size.
    private List<ProductCardDto> allProducts(String storeId, String status) throws Exception {
        List<ProductCardDto> out = new ArrayList<ProductCardDto>();
        for (int page = 0; page < 100; page++) {
            ProductPageDto p = catalog.merchantProducts(storeId,
                    status.length() == 0 ? null : status, null, null, null, null, null, null,
                    Integer.toString(page), "100");
            out.addAll(p.items());
            if (p.items().size() < 100 || out.size() >= p.total()) {
                break;
            }
        }
        return out;
    }

    Tabled products(String storeId, HttpServer.Request request, String base, boolean csv)
            throws Exception {
        final String cur = currency(storeId);
        String status = AdminForms.field(request, "status");
        if (!"ACTIVE".equals(status) && !"DRAFT".equals(status) && !"ARCHIVED".equals(status)) {
            status = "";
        }
        final String root = "/admin/" + storeId + "/products/";
        final Map<String, Integer> margins = catalog.margins(storeId);
        Grid<ProductCardDto> g = new Grid<ProductCardDto>("products")
                .col("title", "Product", p -> p.title()).sortBy(p -> p.title())
                    .link(p -> root + p.id())
                .col("status", "Status", p -> words(p.status())).pill(p -> tone(p.status()))
                    .sortBy(p -> p.status())
                .col("variants", "Variants", p -> Integer.toString(p.variantCount())).num()
                .col("available", "In stock", p -> Integer.toString(p.available())).num()
                    .sortBy(p -> Integer.valueOf(p.available()))
                .col("price", "Price", p -> p.minPriceCents() == p.maxPriceCents()
                        ? Money.format(p.minPriceCents(), cur)
                        : Money.format(p.minPriceCents(), cur) + " - "
                            + Money.format(p.maxPriceCents(), cur)).num()
                    .sortBy(p -> Long.valueOf(p.minPriceCents()))
                .col("margin", "Margin", p -> com.codenameone.examples.bazaar.ops.Margins.percent(
                        margin(margins, p.id()))).num()
                    .sortBy(p -> Integer.valueOf(margin(margins, p.id())))
                .rows(p -> p.id(), p -> root + p.id())
                .defaultSort("title", false);
        Tabled out = done(g, allProducts(storeId, status), request, base, storeId, "products",
                csv, filterParam("status", status));
        if (csv) {
            return out;
        }
        DataTable t = out.table;
        t.searchLabel = "Search products";
        t.empty = "No products match.";
        t.filters.add(filter("status", "Status", status, new String[][] {
            {"", "Any status"}, {"ACTIVE", "Active"}, {"DRAFT", "Draft"}, {"ARCHIVED", "Archived"}}));
        return out;
    }

    private static int margin(Map<String, Integer> margins, String productId) {
        Integer m = margins.get(productId);
        return m == null ? com.codenameone.examples.bazaar.ops.Margins.UNKNOWN : m.intValue();
    }

    void product(String storeId, String productId, AdminView v) throws Exception {
        ProductDto p = catalog.merchantProduct(storeId, productId);
        String cur = currency(storeId);
        String base = "/admin/" + storeId + "/products/" + p.id();
        v.saveAction = base;
        v.adjustAction = base + "/adjust";
        v.title = p.title() == null ? "" : p.title();
        v.description = p.description() == null ? "" : p.description();
        v.status = p.status();
        for (int iter = 0; iter < PRODUCT_STATUSES.length; iter++) {
            AdminRow r = new AdminRow();
            r.id = PRODUCT_STATUSES[iter];
            r.title = words(PRODUCT_STATUSES[iter]);
            r.flag = PRODUCT_STATUSES[iter].equals(p.status());
            v.statuses.add(r);
        }
        List<VariantDto> vs = p.variants() == null ? new ArrayList<VariantDto>() : p.variants();
        for (int iter = 0; iter < vs.size(); iter++) {
            VariantDto x = vs.get(iter);
            AdminRow r = new AdminRow();
            r.id = x.id();
            r.title = x.title() == null ? "" : x.title();
            r.sku = x.sku() == null ? "" : x.sku();
            r.price = Money.plain(x.priceCents());
            r.amount = Money.format(x.priceCents(), cur);
            r.quantity = Integer.toString(x.available());
            r.field = "v" + iter + "_title";
            r.field2 = "v" + iter + "_sku";
            r.field3 = "v" + iter + "_price";
            r.field4 = "v" + iter + "_cost";
            r.cost = x.costCents() > 0 ? Money.plain(x.costCents()) : "";
            int margin = com.codenameone.examples.bazaar.ops.Margins.marginBps(x.priceCents(),
                    x.costCents());
            r.margin = margin == com.codenameone.examples.bazaar.ops.Margins.UNKNOWN ? "-"
                    : com.codenameone.examples.bazaar.ops.Margins.percent(margin) + " margin, "
                        + com.codenameone.examples.bazaar.ops.Margins.percent(
                            com.codenameone.examples.bazaar.ops.Margins.markupBps(
                                x.priceCents(), x.costCents())) + " markup";
            v.variants.add(r);
        }
        List<LevelDto> levels = inventory.levels(storeId, null, p.id(), null);
        for (int iter = 0; iter < levels.size(); iter++) {
            LevelDto l = levels.get(iter);
            AdminRow r = new AdminRow();
            r.id = l.variantId();
            r.title = l.variantTitle() == null ? "" : l.variantTitle();
            r.sku = l.sku() == null ? "" : l.sku();
            r.detail = l.locationName() == null ? "" : l.locationName();
            r.field = l.locationId();
            r.quantity = Integer.toString(l.onHand());
            r.meta = Integer.toString(l.committed());
            r.max = Integer.toString(l.available());
            v.levels.add(r);
        }
        for (int iter = 0; iter < REASONS.length; iter++) {
            AdminRow r = new AdminRow();
            r.id = REASONS[iter][0];
            r.title = REASONS[iter][1];
            v.reasons.add(r);
        }
    }

    /// Saves the editor's fields over the product as it is now: every field
    /// the form does not show (options, photos, collections, each variant's
    /// barcode, cost, weight and flags) goes back exactly as it was, and every
    /// variant is sent with its id, so a save can never drop one -- the
    /// service deletes a variant missing from the list, with its stock.
    void saveProduct(String storeId, String productId, HttpServer.Request request)
            throws Exception {
        ProductDto p = catalog.merchantProduct(storeId, productId);
        String title = AdminForms.field(request, "title");
        String description = AdminForms.field(request, "description");
        String status = AdminForms.field(request, "status");
        boolean known = false;
        for (int iter = 0; iter < PRODUCT_STATUSES.length; iter++) {
            known |= PRODUCT_STATUSES[iter].equals(status);
        }
        if (!known) {
            throw new ResponseStatusException(400, "Choose a status");
        }
        List<VariantDto> vs = new ArrayList<VariantDto>();
        List<VariantDto> was = p.variants() == null ? new ArrayList<VariantDto>() : p.variants();
        for (int iter = 0; iter < was.size(); iter++) {
            VariantDto x = was.get(iter);
            String price = AdminForms.field(request, "v" + iter + "_price");
            String cost = AdminForms.field(request, "v" + iter + "_cost");
            String sku = AdminForms.field(request, "v" + iter + "_sku");
            String vt = AdminForms.field(request, "v" + iter + "_title");
            String id = AdminForms.field(request, "v" + iter + "_id");
            if (id.length() > 0 && !id.equals(x.id())) {
                // The product changed under the form (a variant added or removed).
                throw new ResponseStatusException(409, "The product changed since the page "
                        + "was opened; reload it and try again");
            }
            vs.add(new VariantDto(x.id(), x.productId(), vt.length() == 0 ? x.title() : vt,
                    x.option1(), x.option2(), x.option3(), sku, x.barcode(),
                    price.length() == 0 ? x.priceCents() : AdminForms.cents(price, "the price"),
                    x.compareAtCents(), cost.length() == 0 ? x.costCents()
                        : AdminForms.cents(cost, "the cost"), x.weightGrams(), x.taxable(),
                    x.trackInventory(), x.allowBackorder(), x.position(), x.available(), null));
        }
        catalog.updateProduct(storeId, productId, new ProductDto(p.id(), p.storeId(), p.handle(),
                title, description, p.vendor(), p.productType(), p.tags(), status, p.seoTitle(),
                p.seoDescription(), p.photoId(), p.rating(), p.ratingCount(), null, vs, null,
                null));
    }

    /// Adds or takes away units of one variant at one location.
    void adjust(String storeId, String productId, HttpServer.Request request) throws Exception {
        // Reading the product first makes another store's product a 404 here too.
        ProductDto p = catalog.merchantProduct(storeId, productId);
        String variant = AdminForms.field(request, "variant");
        boolean ours = false;
        for (int iter = 0; p.variants() != null && iter < p.variants().size(); iter++) {
            ours |= p.variants().get(iter).id().equals(variant);
        }
        if (!ours) {
            throw new ResponseStatusException(404, "No such variant");
        }
        int delta = AdminForms.number(AdminForms.field(request, "delta"), 0, "the change");
        String reason = AdminForms.field(request, "reason");
        String note = AdminForms.field(request, "note");
        inventory.adjust(storeId, new AdjustDto(variant, AdminForms.field(request, "location"),
                delta, 0, reason, note.length() > 64 ? note.substring(0, 64) : note));
    }

    // ------------------------------------------------------------ inventory

    Tabled inventory(String storeId, HttpServer.Request request, String base, boolean csv)
            throws Exception {
        String location = AdminForms.field(request, "location");
        List<LocationDto> places = storeApi.locations(storeId);
        boolean known = false;
        String[][] options = new String[places.size() + 1][];
        options[0] = new String[] {"", "Every location"};
        for (int iter = 0; iter < places.size(); iter++) {
            options[iter + 1] = new String[] {places.get(iter).id(), places.get(iter).name()};
            known |= places.get(iter).id().equals(location);
        }
        if (!known) {
            location = "";
        }
        String low = "1".equals(AdminForms.field(request, "low")) ? "1" : "";
        List<LevelDto> rows = inventory.levels(storeId, location.length() == 0 ? null : location,
                null, low.length() > 0 ? "true" : null);
        final String root = "/admin/" + storeId + "/products/";
        Grid<LevelDto> g = new Grid<LevelDto>("inventory")
                .col("product", "Product", l -> l.productTitle()).sortBy(l -> l.productTitle())
                    .link(l -> root + l.productId())
                .col("variant", "Variant", l -> l.variantTitle() == null ? "" : l.variantTitle())
                .col("sku", "SKU", l -> l.sku() == null ? "" : l.sku()).sortBy(l -> l.sku())
                .col("location", "Location", l -> l.locationName()).sortBy(l -> l.locationName())
                .col("onHand", "On hand", l -> Integer.toString(l.onHand())).num()
                    .sortBy(l -> Integer.valueOf(l.onHand()))
                .col("committed", "Committed", l -> Integer.toString(l.committed())).num()
                .col("available", "Available", l -> Integer.toString(l.available())).num()
                    .sortBy(l -> Integer.valueOf(l.available()))
                .rows(l -> l.id(), l -> root + l.productId())
                .defaultSort("product", false);
        Tabled out = done(g, rows, request, base, storeId, "inventory", csv,
                filterParam("location", location), filterParam("low", low));
        if (csv) {
            return out;
        }
        DataTable t = out.table;
        t.searchLabel = "Search stock";
        t.empty = "No stock levels match.";
        t.filters.add(filter("location", "Location", location, options));
        t.filters.add(filter("low", "Level", low, new String[][] {
            {"", "Any level"}, {"1", "At or below reorder point"}}));
        return out;
    }

    // ------------------------------------------------------------ customers

    Tabled customers(String storeId, HttpServer.Request request, String base, boolean csv)
            throws Exception {
        final String cur = currency(storeId);
        List<CustomerDto> rows = customers.list(storeId, null, null, null, null, null, null, null);
        final String root = "/admin/" + storeId + "/customers/";
        Grid<CustomerDto> g = new Grid<CustomerDto>("customers")
                .col("name", "Name", c -> name(c)).sortBy(c -> name(c)).link(c -> root + c.id())
                .col("email", "Email", c -> c.email() == null ? "" : c.email())
                    .sortBy(c -> c.email() == null ? "" : c.email())
                .col("orders", "Orders", c -> Integer.toString(c.ordersCount())).num()
                    .sortBy(c -> Integer.valueOf(c.ordersCount()))
                .col("spent", "Spent", c -> Money.format(c.spentCents(), cur)).num()
                    .sortBy(c -> Long.valueOf(c.spentCents()))
                .col("last", "Last order", c -> date(c.lastOrderAt()))
                    .sortBy(c -> Long.valueOf(c.lastOrderAt()))
                .rows(c -> c.id(), c -> root + c.id())
                .defaultSort("spent", true);
        Tabled out = done(g, rows, request, base, storeId, "customers", csv);
        if (csv) {
            return out;
        }
        DataTable t = out.table;
        t.searchLabel = "Search customers";
        t.empty = "No customers yet.";
        return out;
    }

    static String name(CustomerDto c) {
        String first = c.firstName() == null ? "" : c.firstName();
        String last = c.lastName() == null ? "" : c.lastName();
        String n = (first + " " + last).trim();
        if (n.length() > 0) {
            return n;
        }
        return c.email() != null && c.email().length() > 0 ? c.email()
                : c.username() == null ? "" : c.username();
    }

    /// The customer's page; answers their display name for the title.
    String customer(String storeId, String customerId, AdminView v) throws Exception {
        CustomerDetailDto d = customers.detail(storeId, customerId);
        CustomerDto c = d.customer();
        String cur = d.currency();
        fact(v, "Email", c.email());
        fact(v, "Phone", c.phone());
        fact(v, "Orders", Integer.toString(c.ordersCount()));
        fact(v, "Lifetime value", Money.format(d.lifetimeValueCents(), cur));
        fact(v, "Average order", Money.format(d.averageOrderCents(), cur));
        fact(v, "Store credit", Money.format(c.storeCreditCents(), cur));
        fact(v, "Marketing", c.acceptsMarketing() ? "Subscribed" : "Not subscribed");
        fact(v, "Customer since", date(c.createdAt()));
        fact(v, "Note", c.note());
        String root = "/admin/" + storeId + "/orders/";
        List<CustomerOrderDto> os = d.orders() == null ? new ArrayList<CustomerOrderDto>()
                : d.orders();
        for (int iter = 0; iter < os.size(); iter++) {
            CustomerOrderDto o = os.get(iter);
            AdminRow r = new AdminRow();
            r.title = "#" + o.number();
            r.url = root + o.orderId();
            r.meta = date(o.createdAt());
            r.status = words(o.status());
            r.detail = words(o.financial()) + ", " + words(o.fulfilment());
            r.amount = Money.format(o.totalCents(), o.currency());
            v.orders.add(r);
        }
        List<SavedAddressDto> as = d.addresses() == null ? new ArrayList<SavedAddressDto>()
                : d.addresses();
        for (int iter = 0; iter < as.size(); iter++) {
            AdminRow r = new AdminRow();
            r.title = address(as.get(iter).address());
            r.flag = as.get(iter).isDefault();
            v.addresses.add(r);
        }
        return name(c);
    }

    private static void fact(AdminView v, String label, String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        AdminRow r = new AdminRow();
        r.title = label;
        r.detail = value;
        v.facts.add(r);
    }
}
