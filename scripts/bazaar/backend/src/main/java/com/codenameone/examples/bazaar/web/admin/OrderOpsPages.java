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
import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.BulkActionDto;
import com.codenameone.examples.bazaar.api.BulkActionResultDto;
import com.codenameone.examples.bazaar.api.InventoryStateDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.api.PayLinkDto;
import com.codenameone.examples.bazaar.api.PayLinkRequestDto;
import com.codenameone.examples.bazaar.api.SerialDto;
import com.codenameone.examples.bazaar.api.TemplateDto;
import com.codenameone.examples.bazaar.api.TrackedReceiptDto;
import com.codenameone.examples.bazaar.api.TrackingDto;
import com.codenameone.examples.bazaar.api.UnavailableDto;
import com.codenameone.examples.bazaar.docs.DocumentPages;
import com.codenameone.examples.bazaar.inventory.InventoryStates;
import com.codenameone.examples.bazaar.inventory.Tracking;
import com.codenameone.examples.bazaar.notify.Templates;
import com.codenameone.examples.bazaar.order.BulkOrders;
import com.codenameone.examples.bazaar.order.OrderSettings;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.order.PayLinks;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The back office of the order and stock tools: stock states per variant
/// and location, serial and lot receiving with a serial lookup, label
/// sheets, the order settings (auto-archive and return rules), the
/// notification template editor, archived orders, bulk order actions with a
/// result per order, archiving one order and sending a pay link.
///
/// Every page renders [MarketingView] into the shared `marketing` template
/// and every action is a form post answered with a redirect and a toast. The
/// services check the role; [WebPages#admin] answers 404 first for anyone
/// not on the store's staff.
@Controller
public class OrderOpsPages {
    /// The languages the storefront and its mail speak.
    static final String[][] LANGUAGES = {
        {"en", "English"}, {"es", "Spanish"}, {"fr", "French"}, {"de", "German"}, {"he", "Hebrew"}};

    private final WebPages pages;
    private final DataSource db;
    private final Orders orders;
    private final BulkOrders bulk;
    private final PayLinks payLinks;
    private final OrderSettings settings;
    private final Templates templates;
    private final InventoryStates states;
    private final Tracking tracking;
    private final Stores stores;
    private final DocumentPages documents;

    public OrderOpsPages(WebPages pages, DataSource db, Orders orders, BulkOrders bulk,
            PayLinks payLinks, OrderSettings settings, Templates templates,
            InventoryStates states, Tracking tracking, Stores stores, DocumentPages documents) {
        this.pages = pages;
        this.db = db;
        this.orders = orders;
        this.bulk = bulk;
        this.payLinks = payLinks;
        this.settings = settings;
        this.templates = templates;
        this.states = states;
        this.tracking = tracking;
        this.stores = stores;
        this.documents = documents;
    }

    // ------------------------------------------------------------- plumbing

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            MarketingView view) throws IOException {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        view.base = "/admin/" + storeId;
        if (view.heading.length() == 0) {
            view.heading = title;
        }
        model.addAttribute("view", view);
        model.addAttribute("table", new DataTable());
        return WebPages.view(model, shell, "marketing");
    }

    private static MarketingView view(String page) {
        MarketingView v = new MarketingView();
        v.page = page;
        return v;
    }

    private void member(HttpServer.Request request, String storeId) throws IOException {
        pages.admin(request, storeId, "");
    }

    private interface Change {
        void run() throws Exception;
    }

    private static String change(HttpServer.Request request, String back, String done, Change c)
            throws Exception {
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

    private static String f(HttpServer.Request request, String name) {
        return AdminForms.field(request, name);
    }

    /// A whole number typed in a form, or `fallback` when blank or not one.
    static int whole(String text, int fallback) {
        String t = text == null ? "" : text.trim();
        boolean minus = t.startsWith("-");
        if (minus) {
            t = t.substring(1);
        }
        if (t.length() == 0 || t.length() > 9) {
            return fallback;
        }
        int n = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9') {
                return fallback;
            }
            n = n * 10 + (c - '0');
        }
        return minus ? -n : n;
    }

    private static AdminRow row(String title, String detail, String meta, String amount) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.detail = detail;
        r.meta = meta;
        r.amount = amount;
        return r;
    }

    private static AdminRow fact(String title, String detail) {
        return row(title, detail, "", "");
    }

    /// The store's variants, product title first: id, "Product - Variant (SKU)".
    private List<String[]> variants(String storeId) throws IOException {
        List rows = db.query("SELECT v.id AS id, v.title AS vt, v.sku AS sku, p.title AS pt "
                + "FROM variant v JOIN product p ON p.id = v.product_id WHERE v.store_id = ? "
                + "ORDER BY p.title, v.position LIMIT 500", new Object[] {storeId});
        List<String[]> out = new ArrayList<String[]>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String sku = Rows.text(r.get("sku"));
            out.add(new String[] {Rows.text(r.get("id")), Rows.text(r.get("pt")) + " - "
                    + Rows.text(r.get("vt")) + (sku.length() == 0 ? "" : " (" + sku + ")")});
        }
        return out;
    }

    private FormField variantField(FormView form, String storeId) throws IOException {
        FormField fld = form.add("variant", "Variant", "select", "");
        List<String[]> vs = variants(storeId);
        for (int iter = 0; iter < vs.size(); iter++) {
            fld.option(vs.get(iter)[0], vs.get(iter)[1]);
        }
        return fld;
    }

    private FormField locationField(FormView form, String storeId) throws IOException {
        FormField fld = form.add("location", "Location", "select", "");
        List<LocationDto> ls = stores.locations(Caller.name(), storeId);
        for (int iter = 0; iter < ls.size(); iter++) {
            fld.option(ls.get(iter).id(), ls.get(iter).name());
        }
        return fld;
    }

    // ---------------------------------------------------------- stock states

    /// Each variant at each location: on hand, committed to open orders,
    /// incoming on purchase orders, and the units held back as unavailable
    /// with their reason. A form moves units into or out of a reason.
    @GetMapping("/admin/{storeId}/stock-states")
    public String stockStates(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        MarketingView v = view("stock-states");
        v.intro = "Available is on hand less what open orders have committed. Damaged, quality "
                + "control and safety stock units are held out of on hand until moved back.";
        String location = f(request, "location");
        List<InventoryStateDto> rows = states.states(Caller.name(), storeId,
                location.length() == 0 ? null : location);
        v.rowsTitle = "Stock states";
        v.rowsEmpty = "No stocked variants yet.";
        v.headA = "Variant";
        v.headB = "Location";
        v.headC = "Committed, incoming, unavailable";
        v.headD = "On hand / available";
        for (int iter = 0; iter < rows.size(); iter++) {
            InventoryStateDto s = rows.get(iter);
            StringBuilder held = new StringBuilder();
            held.append("committed ").append(s.committed()).append(", incoming ")
                    .append(s.incoming());
            if (s.damaged() > 0) {
                held.append(", damaged ").append(s.damaged());
            }
            if (s.qualityControl() > 0) {
                held.append(", quality control ").append(s.qualityControl());
            }
            if (s.safetyStock() > 0) {
                held.append(", safety stock ").append(s.safetyStock());
            }
            v.rows.add(row(s.productTitle() + " - " + s.variantTitle(), s.locationName(),
                    held.toString(), s.onHand() + " / " + s.available()));
        }
        FormView move = new FormView("unavailable", "Adjust unavailable",
                "/admin/" + storeId + "/stock-states/move", "Move units");
        move.text = "A positive quantity takes units out of on hand into the reason; a negative "
                + "one puts them back.";
        variantField(move, storeId);
        locationField(move, storeId);
        move.add("reason", "Reason", "select", "")
                .option(InventoryStates.DAMAGED, "Damaged")
                .option(InventoryStates.QUALITY_CONTROL, "Quality control")
                .option(InventoryStates.SAFETY_STOCK, "Safety stock");
        move.add("quantity", "Quantity", "number", "1");
        v.forms.add(move);
        return render(model, request, storeId, "Stock states", v);
    }

    @PostMapping("/admin/{storeId}/stock-states/move")
    public String moveUnavailable(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        final UnavailableDto m = new UnavailableDto(f(request, "variant"), f(request, "location"),
                f(request, "reason"), whole(f(request, "quantity"), 0));
        return change(request, "/admin/" + storeId + "/stock-states", "Stock moved",
                () -> states.move(Caller.name(), storeId, m));
    }

    // -------------------------------------------------------- serials, lots

    /// Tracking mode per variant, receiving serials or a lot into a
    /// location, and finding a serial: where it is, or which order took it.
    @GetMapping("/admin/{storeId}/serials")
    public String serials(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        MarketingView v = view("serials");
        String base = "/admin/" + storeId + "/serials";
        v.intro = "Serial numbers are one per unit; a lot is a batch with a code and an "
                + "optional expiry. Received units are added to on hand.";
        List<TrackingDto> tracked = tracking.tracked(Caller.name(), storeId);
        for (int iter = 0; iter < tracked.size(); iter++) {
            TrackingDto t = tracked.get(iter);
            if (!Tracking.NONE.equals(t.mode())) {
                v.facts.add(fact(t.productTitle() + " - " + t.variantTitle(),
                        Tracking.SERIAL.equals(t.mode()) ? "Serial numbers" : "Lots"));
            }
        }
        FormView find = new FormView("find", "Find a serial", base + "/find", "Find");
        String q = f(request, "q");
        find.add("q", "Serial number", "text", q);
        v.forms.add(find);
        if (q.length() > 0) {
            List<SerialDto> found = tracking.find(Caller.name(), storeId, q);
            v.rowsTitle = "Serials matching " + q;
            v.rowsEmpty = "No serial matches.";
            v.headA = "Serial";
            v.headB = "Variant";
            v.headC = "Status";
            v.headD = "Order";
            for (int iter = 0; iter < found.size(); iter++) {
                SerialDto s = found.get(iter);
                v.rows.add(row(s.serial(), s.productTitle() + " - " + s.variantTitle(),
                        (Tracking.SOLD.equals(s.status()) ? "Sold " + Days.iso(s.soldAt())
                                : "In stock since " + Days.iso(s.receivedAt())),
                        s.orderNumber() > 0 ? "#" + s.orderNumber() : ""));
            }
        }
        FormView receive = new FormView("receive", "Receive serials or a lot", base + "/receive",
                "Receive");
        receive.text = "For a serial-tracked variant, list one serial per line. For a lot, give "
                + "the lot code, the quantity and, if it has one, the expiry date.";
        variantField(receive, storeId);
        locationField(receive, storeId);
        receive.add("reference", "Reference (purchase order or note)", "text", "");
        receive.add("serials", "Serial numbers, one per line", "textarea", "");
        receive.add("lot", "Lot code", "text", "");
        receive.add("expires", "Lot expiry (YYYY-MM-DD)", "text", "");
        receive.add("quantity", "Lot quantity", "number", "");
        v.forms.add(receive);
        FormView mode = new FormView("mode", "Tracking mode", base + "/mode", "Save mode");
        variantField(mode, storeId);
        mode.add("mode", "Track by", "select", "")
                .option(Tracking.NONE, "Nothing").option(Tracking.SERIAL, "Serial number")
                .option(Tracking.LOT, "Lot");
        v.forms.add(mode);
        return render(model, request, storeId, "Serials and lots", v);
    }

    @PostMapping("/admin/{storeId}/serials/find")
    public String findSerial(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        String q = f(request, "q");
        return "redirect:/admin/" + storeId + "/serials"
                + (q.length() == 0 ? "" : "?q=" + com.codenameone.examples.bazaar.web.SiteUrls
                        .encode(q.length() > 64 ? q.substring(0, 64) : q));
    }

    @PostMapping("/admin/{storeId}/serials/mode")
    public String trackingMode(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        final TrackingDto t = new TrackingDto(f(request, "variant"), null, null, null,
                f(request, "mode"));
        return change(request, "/admin/" + storeId + "/serials", "Tracking saved",
                () -> tracking.setMode(Caller.name(), storeId, t));
    }

    @PostMapping("/admin/{storeId}/serials/receive")
    public String receive(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        List<String> serials = lines(f(request, "serials"));
        String expires = f(request, "expires");
        final TrackedReceiptDto r = new TrackedReceiptDto(f(request, "variant"),
                f(request, "location"), f(request, "reference"), serials, f(request, "lot"),
                expires.length() == 0 ? 0L : AdminForms.dayStart(expires),
                whole(f(request, "quantity"), serials.size()));
        return change(request, "/admin/" + storeId + "/serials", "Received",
                () -> tracking.receive(Caller.name(), storeId, r));
    }

    /// The non-blank lines of a text area, trimmed; at most 500.
    static List<String> lines(String text) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int iter = 0; iter <= text.length() && out.size() < 500; iter++) {
            if (iter == text.length() || text.charAt(iter) == '\n' || text.charAt(iter) == ',') {
                String s = text.substring(start, iter).trim();
                if (s.length() > 0) {
                    out.add(s);
                }
                start = iter + 1;
            }
        }
        return out;
    }

    // ---------------------------------------------------------- label sheets

    /// Barcode label sheets: any quantity of each variant, or every unit a
    /// received purchase order brought in.
    @GetMapping("/admin/{storeId}/labels")
    public String labels(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        MarketingView v = view("labels");
        String base = "/admin/" + storeId + "/labels";
        v.intro = "Each label carries the product, the variant, the price and the barcode (or "
                + "the SKU when there is no barcode).";
        FormView sheet = new FormView("sheet", "Print labels", base + ".pdf", "Print PDF");
        sheet.text = "Type how many labels each variant needs; blank prints none.";
        layoutField(sheet);
        List<String[]> vs = variants(storeId);
        for (int iter = 0; iter < vs.size() && iter < 200; iter++) {
            sheet.add("qty-" + vs.get(iter)[0], vs.get(iter)[1], "number", "");
        }
        v.forms.add(sheet);
        List rows = db.query("SELECT id, number, status FROM purchase_order WHERE store_id = ? "
                + "AND status IN (?, ?) ORDER BY number DESC LIMIT 50", new Object[] {storeId,
                    com.codenameone.examples.bazaar.ops.Timelines.RECEIVED,
                    com.codenameone.examples.bazaar.ops.Timelines.PARTIAL});
        v.cardsTitle = "Received purchase orders";
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            ActionCard k = new ActionCard();
            k.title = "PO #" + Rows.text(r.get("number"));
            k.status = words(Rows.text(r.get("status")));
            k.button("Print labels for what arrived", "/admin/" + storeId + "/purchase-orders/"
                    + Rows.text(r.get("id")) + "/labels.pdf", false);
            v.cards.add(k);
        }
        return render(model, request, storeId, "Label sheets", v);
    }

    private static void layoutField(FormView form) {
        form.add("layout", "Layout", "select", "AVERY_30")
                .option("AVERY_30", "Letter sheet, 30 labels").option("THERMAL_2X1",
                        "Thermal roll, 2 x 1 inch");
    }

    @PostMapping("/admin/{storeId}/labels.pdf")
    public HttpServer.Response printLabels(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        List<String[]> vs = variants(storeId);
        StringBuilder items = new StringBuilder();
        for (int iter = 0; iter < vs.size(); iter++) {
            int n = whole(f(request, "qty-" + vs.get(iter)[0]), 0);
            if (n > 0) {
                items.append(items.length() == 0 ? "" : ",").append(vs.get(iter)[0]).append(':')
                        .append(Math.min(n, 1000));
            }
        }
        if (items.length() == 0) {
            WebPages.toast(request, "Type a quantity for at least one variant");
            return HttpServer.Response.text(303, "").header("Location",
                    "/admin/" + storeId + "/labels");
        }
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        return documents.labelSheet(storeId, items.toString(), f(request, "layout"));
    }

    @PostMapping("/admin/{storeId}/purchase-orders/{poId}/labels.pdf")
    public HttpServer.Response purchaseOrderLabels(@PathVariable("storeId") String storeId,
            @PathVariable("poId") String poId, HttpServer.Request request) throws Exception {
        member(request, storeId);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        return documents.purchaseOrderLabels(storeId, poId, f(request, "layout"));
    }

    // -------------------------------------------------------- order settings

    /// Auto-archive and the return rules the storefront shows and applies.
    @GetMapping("/admin/{storeId}/order-settings")
    public String orderSettings(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        MarketingView v = view("order-settings");
        OrderSettingsDto s = settings.read(Caller.name(), storeId);
        FormView archive = new FormView("archive", "Archiving",
                "/admin/" + storeId + "/order-settings", "Save");
        archive.check("autoArchive", "Archive orders once they are paid and fulfilled",
                s.autoArchive());
        archive.add("window", "Return window (days after delivery)", "number",
                Integer.toString(s.returnWindowDays()));
        archive.add("restocking", "Restocking fee (percent of the goods)", "text",
                percent(s.restockingFeeBp()));
        archive.add("returnShipping", "Return shipping fee", "text",
                s.returnShippingCents() <= 0 ? "" : Money.plain(s.returnShippingCents()));
        archive.add("finalSaleProducts", "Final sale product ids (comma separated)", "text",
                s.finalSaleProducts() == null ? "" : s.finalSaleProducts())
                .hint("Final sale products and collections cannot be returned.");
        archive.add("finalSaleCollections", "Final sale collection ids (comma separated)",
                "text", s.finalSaleCollections() == null ? "" : s.finalSaleCollections());
        archive.title = "Archiving and return rules";
        v.forms.add(archive);
        return render(model, request, storeId, "Order settings", v);
    }

    @PostMapping("/admin/{storeId}/order-settings")
    public String saveOrderSettings(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        String back = "/admin/" + storeId + "/order-settings";
        final OrderSettingsDto s;
        try {
            String ship = f(request, "returnShipping");
            s = new OrderSettingsDto("1".equals(f(request, "autoArchive")),
                    whole(f(request, "window"), OrderSettings.WINDOW_DAYS),
                    basisPoints(f(request, "restocking")),
                    ship.length() == 0 ? 0 : AdminForms.cents(ship, "The return shipping fee"),
                    f(request, "finalSaleProducts"), f(request, "finalSaleCollections"));
        } catch (ResponseStatusException bad) {
            WebPages.toast(request, bad.getReason());
            return "redirect:" + back;
        }
        return change(request, back, "Order settings saved",
                () -> settings.save(Caller.name(), storeId, s));
    }

    /// `15`, `12.5` or `12.50` percent as basis points; blank is none.
    static int basisPoints(String text) {
        String t = text == null ? "" : text.trim();
        if (t.endsWith("%")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        if (t.length() == 0) {
            return 0;
        }
        int dot = t.indexOf('.');
        String w = dot < 0 ? t : t.substring(0, dot);
        String frac = dot < 0 ? "" : t.substring(dot + 1);
        if (frac.length() > 2) {
            throw new ResponseStatusException(400, "The restocking fee takes two decimals at most");
        }
        int a = whole(w.length() == 0 ? "0" : w, -1);
        int b = whole(frac.length() == 0 ? "0" : (frac.length() == 1 ? frac + "0" : frac), -1);
        if (a < 0 || b < 0) {
            throw new ResponseStatusException(400, "The restocking fee is a percentage");
        }
        return a * 100 + b;
    }

    static String percent(int bp) {
        if (bp <= 0) {
            return "";
        }
        int rest = bp % 100;
        return bp / 100 + (rest == 0 ? "" : "." + (rest < 10 ? "0" : "") + rest);
    }

    // ------------------------------------------------ notification templates

    /// Every customer message per language: the text the store sends, which
    /// placeholders it takes, a preview of it filled in and a reset to the
    /// default.
    @GetMapping("/admin/{storeId}/notifications")
    public String notifications(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        MarketingView v = view("notifications");
        String lang = language(f(request, "lang"));
        String base = "/admin/" + storeId + "/notifications";
        for (int iter = 0; iter < LANGUAGES.length; iter++) {
            AdminRow l = new AdminRow();
            l.title = LANGUAGES[iter][1] + (LANGUAGES[iter][0].equals(lang) ? " (shown)" : "");
            l.url = base + "?lang=" + LANGUAGES[iter][0];
            v.links.add(l);
        }
        v.intro = "Placeholders such as {0} are filled in when the message is sent; each "
                + "template lists the ones it takes. A template must keep every placeholder.";
        String previewKey = f(request, "preview");
        List<TemplateDto> list = templates.list(Caller.name(), storeId, lang);
        for (int iter = 0; iter < list.size(); iter++) {
            TemplateDto t = list.get(iter);
            if (t.key().equals(previewKey)) {
                TemplateDto shown = templates.preview(Caller.name(), storeId, new TemplateDto(
                        t.key(), null, null, lang, t.body(), null, null, false));
                v.facts.add(fact("Preview of " + t.key(), shown.body()));
            }
            FormView fm = new FormView("t-" + iter, t.key() + (t.custom() ? " (edited)" : ""),
                    base + "/save", "Save");
            fm.text = (isSms(t) ? "Text message. " : "E-mail. ") + "Placeholders: "
                    + (t.placeholders() == null || t.placeholders().length() == 0 ? "none"
                            : t.placeholders());
            fm.add("key", "", "hidden", t.key());
            fm.add("lang", "", "hidden", lang);
            fm.add("body", "Text", "textarea", t.body()).hint("Default: " + t.defaultBody());
            v.forms.add(fm);
            ActionCard k = new ActionCard();
            k.title = t.key();
            k.status = t.custom() ? "Edited" : "Default";
            k.extraUrl = base + "?lang=" + lang + "&preview=" + t.key();
            k.extraLabel = "Preview";
            if (t.custom()) {
                k.button("Reset to default", base + "/reset?lang=" + lang + "&key=" + t.key(),
                        true);
            }
            v.cards.add(k);
        }
        v.cardsTitle = "Templates";
        return render(model, request, storeId, "Notifications", v);
    }

    private static boolean isSms(TemplateDto t) {
        return Templates.SMS.equals(t.channel());
    }

    static String language(String lang) {
        for (int iter = 0; iter < LANGUAGES.length; iter++) {
            if (LANGUAGES[iter][0].equals(lang)) {
                return lang;
            }
        }
        return "en";
    }

    @PostMapping("/admin/{storeId}/notifications/save")
    public String saveTemplate(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        final String lang = language(f(request, "lang"));
        final TemplateDto t = new TemplateDto(f(request, "key"), null, null, lang,
                request.param("body") == null ? "" : request.param("body"), null, null, false);
        return change(request, "/admin/" + storeId + "/notifications?lang=" + lang,
                "Template saved", () -> templates.save(Caller.name(), storeId, t));
    }

    @PostMapping("/admin/{storeId}/notifications/reset")
    public String resetTemplate(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        final String lang = language(f(request, "lang"));
        final TemplateDto t = new TemplateDto(f(request, "key"), null, null, lang, null, null,
                null, false);
        return change(request, "/admin/" + storeId + "/notifications?lang=" + lang,
                "Template reset", () -> templates.reset(Caller.name(), storeId, t));
    }

    // ---------------------------------------------------------------- orders

    /// The "placed within" choices of the archived list, in days, as the
    /// orders table offers them.
    static final String[] SINCE_DAYS = {"7", "30", "90", "365"};

    /// Archived orders, newest first, each with a button to bring it back.
    /// `q` keeps those whose number or e-mail contains it (ignoring case) and
    /// `since` those placed within that many days, as on the orders table.
    @GetMapping("/admin/{storeId}/archived-orders")
    public String archived(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        MarketingView v = view("archived");
        AdminRow back = new AdminRow();
        back.title = "Open orders";
        back.url = "/admin/" + storeId + "/orders";
        v.links.add(back);
        v.cardsTitle = "Archived orders";
        String q = f(request, "q").trim();
        String since = knownSince(f(request, "since"));
        v.searchAction = "/admin/" + storeId + "/archived-orders";
        v.q = q;
        AdminRow any = new AdminRow();
        any.id = "";
        any.title = "Any time";
        any.flag = since.length() == 0;
        v.sinceOptions.add(any);
        for (int iter = 0; iter < SINCE_DAYS.length; iter++) {
            AdminRow o = new AdminRow();
            o.id = SINCE_DAYS[iter];
            o.title = "Last " + SINCE_DAYS[iter] + " days";
            o.flag = SINCE_DAYS[iter].equals(since);
            v.sinceOptions.add(o);
        }
        long from = since.length() == 0 ? Long.MIN_VALUE
                : System.currentTimeMillis() - Long.parseLong(since) * Days.MILLIS;
        List<OrderDto> list = orders.archived(Caller.name(), storeId);
        int shown = 0;
        for (int iter = 0; iter < list.size(); iter++) {
            OrderDto o = list.get(iter);
            if (o.createdAt() < from || !matches(o, q)) {
                continue;
            }
            shown++;
            ActionCard k = new ActionCard();
            k.title = "#" + o.number();
            k.url = "/admin/" + storeId + "/orders/" + o.id();
            k.detail = o.email() == null ? "" : o.email();
            k.meta = Days.iso(o.createdAt());
            k.amount = Money.format(o.totalCents(), o.currency());
            k.button("Unarchive", "/admin/" + storeId + "/orders/" + o.id() + "/unarchive", false);
            v.cards.add(k);
        }
        if (list.isEmpty()) {
            v.intro = "No archived orders.";
        } else if (shown == 0) {
            v.intro = "No archived orders match.";
        }
        return render(model, request, storeId, "Archived orders", v);
    }

    /// `since` when it is one of [#SINCE_DAYS], else empty.
    static String knownSince(String since) {
        for (int iter = 0; iter < SINCE_DAYS.length; iter++) {
            if (SINCE_DAYS[iter].equals(since)) {
                return since;
            }
        }
        return "";
    }

    /// The query string that carries a search and a date filter to the
    /// archived list; empty when there is neither.
    static String archivedQuery(String q, String since) {
        StringBuilder b = new StringBuilder();
        String words = q == null ? "" : q.trim();
        if (words.length() > 0) {
            b.append("?q=").append(java.net.URLEncoder.encode(words,
                    java.nio.charset.StandardCharsets.UTF_8));
        }
        String days = knownSince(since);
        if (days.length() > 0) {
            b.append(b.length() == 0 ? '?' : '&').append("since=").append(days);
        }
        return b.toString();
    }

    /// Whether the order's number, with or without its `#`, or its e-mail
    /// contains `q`, ignoring case (character by character, so the device
    /// locale cannot fold it).
    static boolean matches(OrderDto o, String q) {
        if (q.length() == 0) {
            return true;
        }
        String words = q.charAt(0) == '#' ? q.substring(1) : q;
        return contains(Long.toString(o.number()), words)
                || o.email() != null && contains(o.email(), words);
    }

    private static boolean contains(String text, String part) {
        for (int at = 0; at + part.length() <= text.length(); at++) {
            if (text.regionMatches(true, at, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }

    @PostMapping("/admin/{storeId}/orders/{orderId}/archive")
    public String archive(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Archived",
                () -> orders.archive(Caller.name(), storeId, orderId, true));
    }

    @PostMapping("/admin/{storeId}/orders/{orderId}/unarchive")
    public String unarchive(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Back in orders",
                () -> orders.archive(Caller.name(), storeId, orderId, false));
    }

    /// Mails the customer a checkout link for a draft order or an unpaid
    /// invoice, replacing any earlier link. `back=invoices` returns to the
    /// invoices page.
    @PostMapping("/admin/{storeId}/orders/{orderId}/pay-link")
    public String payLink(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request) throws Exception {
        member(request, storeId);
        String back = "invoices".equals(f(request, "back")) ? "/admin/" + storeId + "/invoices"
                : "/admin/" + storeId + "/orders/" + orderId;
        final PayLinkDto[] made = new PayLinkDto[1];
        String to = change(request, back, "Pay link sent", () -> made[0] = payLinks.create(
                Caller.name(), storeId, orderId, new PayLinkRequestDto(7)));
        if (made[0] != null && !made[0].emailed()) {
            WebPages.toast(request, "Pay link made, but the order has no e-mail address: "
                    + made[0].url());
        }
        return to;
    }

    /// The bulk form of the orders table: one action over the ticked orders,
    /// then a page with what happened to each.
    @PostMapping("/admin/{storeId}/orders/bulk")
    public String bulkAction(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        List<String> ids = request.params("ids");
        if (ids == null || ids.isEmpty()) {
            WebPages.toast(request, "Tick at least one order");
            return "redirect:/admin/" + storeId + "/orders";
        }
        String action = f(request, "action");
        if (BulkOrders.PACKING_SLIPS.equals(action)) {
            // One PDF, a slip per order, opened by the browser.
            StringBuilder csv = new StringBuilder();
            for (int iter = 0; iter < ids.size() && iter < BulkOrders.MAX; iter++) {
                csv.append(iter == 0 ? "" : ",").append(
                        com.codenameone.examples.bazaar.web.SiteUrls.encode(ids.get(iter)));
            }
            return "redirect:/admin/" + storeId + "/packing-slips.pdf?orders=" + csv;
        }
        String tag = f(request, "tag");
        List<BulkActionResultDto> results = bulk.run(Caller.name(), storeId,
                new BulkActionDto(ids, action, tag.length() == 0 ? null : tag));
        MarketingView v = view("bulk");
        AdminRow back = new AdminRow();
        back.title = "Back to orders";
        back.url = "/admin/" + storeId + "/orders";
        v.links.add(back);
        v.rowsTitle = words(action) + ": a result per order";
        v.headA = "Order";
        v.headB = "Result";
        v.headC = "Detail";
        v.headD = "";
        int ok = 0;
        for (int iter = 0; iter < results.size(); iter++) {
            BulkActionResultDto r = results.get(iter);
            if (r.ok()) {
                ok++;
            }
            v.rows.add(row("#" + r.number(), r.ok() ? "Done" : "Not done",
                    r.message() == null ? "" : r.message(), ""));
        }
        v.intro = ok + " of " + results.size() + " done.";
        return render(model, request, storeId, "Bulk action", v);
    }

    /// Packing slips for several orders in one PDF.
    @GetMapping("/admin/{storeId}/packing-slips.pdf")
    public HttpServer.Response packingSlips(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        return documents.packingSlips(storeId, f(request, "orders"));
    }

    static String words(String constant) {
        String s = constant == null ? "" : constant.replace('_', ' ');
        if (s.length() == 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        b.append(s.charAt(0));
        for (int iter = 1; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            b.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return b.toString();
    }
}
