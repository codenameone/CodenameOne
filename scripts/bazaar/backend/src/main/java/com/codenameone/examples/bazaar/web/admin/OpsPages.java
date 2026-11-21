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
import com.codename1.backend.mvc.Model;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AttributionDto;
import com.codenameone.examples.bazaar.api.BinDto;
import com.codenameone.examples.bazaar.api.CannedReplyDto;
import com.codenameone.examples.bazaar.api.ChatConversationDto;
import com.codenameone.examples.bazaar.api.ChatMessageDto;
import com.codenameone.examples.bazaar.api.CohortRowDto;
import com.codenameone.examples.bazaar.api.CombinedListingDto;
import com.codenameone.examples.bazaar.api.CsvErrorDto;
import com.codenameone.examples.bazaar.api.ForecastDto;
import com.codenameone.examples.bazaar.api.ImportDto;
import com.codenameone.examples.bazaar.api.ImportResultDto;
import com.codenameone.examples.bazaar.api.ListingMemberDto;
import com.codenameone.examples.bazaar.api.PermissionSetDto;
import com.codenameone.examples.bazaar.api.PickLineDto;
import com.codenameone.examples.bazaar.api.ReportDto;
import com.codenameone.examples.bazaar.api.ReportSpecDto;
import com.codenameone.examples.bazaar.api.SavedReportDto;
import com.codenameone.examples.bazaar.api.StaffPermissionDto;
import com.codenameone.examples.bazaar.api.SuggestionDto;
import com.codenameone.examples.bazaar.api.TimelineEventDto;
import com.codenameone.examples.bazaar.api.WarehouseMoveDto;
import com.codenameone.examples.bazaar.api.WarehouseTaskDto;
import com.codenameone.examples.bazaar.ops.ChatInbox;
import com.codenameone.examples.bazaar.ops.Forecasts;
import com.codenameone.examples.bazaar.ops.Listings;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.ops.Permissions;
import com.codenameone.examples.bazaar.ops.Reports;
import com.codenameone.examples.bazaar.ops.Timelines;
import com.codenameone.examples.bazaar.ops.Warehouse;
import com.codenameone.examples.bazaar.ops.WarehouseFlow;
import com.codenameone.examples.bazaar.web.SiteUrls;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The back office of the operations features: the warehouse board, the
/// transfer and purchase order timelines, forecasts, the report builder,
/// cohorts and attribution, permission sets, the chat inbox and combined
/// listings with the marketplace import.
///
/// Every page renders [MarketingView] into the shared `marketing` template;
/// charts are drawn on the server as SVG marks. Every action is a plain
/// form post answered with a redirect and a toast, which htmx swaps in
/// place where it is loaded. The services check access; [WebPages#admin]
/// answers 404 first for anyone not on the store's staff.
@Controller
public class OpsPages {
    private final WebPages pages;
    private final WebSessions sessions;
    private final DataSource db;
    private final Warehouse warehouse;
    private final Timelines timelines;
    private final Forecasts forecasts;
    private final Reports reports;
    private final Permissions permissions;
    private final ChatInbox chat;
    private final Listings listings;

    public OpsPages(WebPages pages, WebSessions sessions, DataSource db, Warehouse warehouse,
            Timelines timelines, Forecasts forecasts, Reports reports, Permissions permissions,
            ChatInbox chat, Listings listings) {
        this.pages = pages;
        this.sessions = sessions;
        this.db = db;
        this.warehouse = warehouse;
        this.timelines = timelines;
        this.forecasts = forecasts;
        this.reports = reports;
        this.permissions = permissions;
        this.chat = chat;
        this.listings = listings;
    }

    // ------------------------------------------------------------- plumbing

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            MarketingView view) throws java.io.IOException {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        view.base = base(storeId);
        if (view.heading.length() == 0) {
            view.heading = title;
        }
        view.hasTable = false;
        model.addAttribute("view", view);
        model.addAttribute("table", new DataTable());
        return WebPages.view(model, shell, "marketing");
    }

    private static MarketingView view(String page) {
        MarketingView v = new MarketingView();
        v.page = page;
        return v;
    }

    private void member(HttpServer.Request request, String storeId) throws Exception {
        pages.admin(request, storeId, "");
        sessions.touch(request, Caller.name());
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

    private static String base(String storeId) {
        return "/admin/" + storeId;
    }

    static String param(HttpServer.Request request, String name) {
        try {
            String v = request.param(name);
            return v == null ? "" : v.trim();
        } catch (RuntimeException malformed) {
            return "";
        }
    }

    static int number(String text) {
        int n = 0;
        for (int iter = 0; iter < text.length() && iter < 9; iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return text.length() == 0 ? -1 : n;
    }

    private static AdminRow row(String title, String detail, String meta, String amount) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.detail = detail;
        r.meta = meta;
        r.amount = amount;
        return r;
    }

    private static AdminRow option(String id, String title, boolean selected) {
        AdminRow r = new AdminRow();
        r.id = id;
        r.title = title;
        r.flag = selected;
        return r;
    }

    private static FormField field(String name, String label, String type, String value) {
        return new FormField(name, label, type, value);
    }

    private List<Map> locations(String storeId) throws java.io.IOException {
        List rows = db.query("SELECT id, name FROM location WHERE store_id = ? ORDER BY position, id",
                new Object[] {storeId});
        List<Map> out = new ArrayList<Map>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add((Map) rows.get(iter));
        }
        return out;
    }

    // ------------------------------------------------------------ warehouse

    @GetMapping("/admin/{storeId}/warehouse")
    public String warehouse(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Warehouse");
        MarketingView v = view("warehouse");
        List<Map> locs = locations(storeId);
        String location = param(request, "location");
        if (location.length() == 0 && !locs.isEmpty()) {
            location = Rows.text(locs.get(0).get("id"));
        }
        v.intro = "Receiving to put-away for inbound purchase orders; pick, pack and dispatch for "
                + "open orders. Sync adds new work; a card moves one step at a time.";
        for (int iter = 0; iter < locs.size(); iter++) {
            AdminRow l = row(Rows.text(locs.get(iter).get("name")), "", "", "");
            l.url = base(storeId) + "/warehouse?location=" + SiteUrls.encode(Rows.text(locs.get(iter).get("id")));
            v.links.add(l);
        }
        if (location.length() > 0) {
            List<WarehouseTaskDto> tasks = warehouse.board(Caller.name(), storeId, location);
            for (int c = 0; c < WarehouseFlow.COLUMNS.length; c++) {
                BoardColumn col = new BoardColumn();
                col.id = WarehouseFlow.COLUMNS[c];
                col.title = stageTitle(col.id);
                for (int iter = 0; iter < tasks.size(); iter++) {
                    WarehouseTaskDto t = tasks.get(iter);
                    if (col.id.equals(t.stage())) {
                        AdminRow card = row(t.label(), WarehouseFlow.INBOUND.equals(t.kind())
                                ? "Inbound" : "Outbound", "", "");
                        card.id = t.id();
                        card.field = t.locationId();
                        String next = WarehouseFlow.next(t.kind(), t.stage());
                        card.status = next == null ? t.stage() : next;
                        col.cards.add(card);
                    }
                }
                col.count = String.valueOf(col.cards.size());
                v.columns.add(col);
            }
            FormView sync = new FormView("sync", "New work", base(storeId) + "/warehouse/sync", "Sync");
            sync.fields.add(field("location", "", "hidden", location));
            v.forms.add(sync);
            FormView bin = new FormView("bin", "Bin", base(storeId) + "/warehouse/bins", "Save bin");
            bin.text = "Aisle, shelf and position give the walk order of the pick list.";
            bin.fields.add(field("location", "", "hidden", location));
            bin.fields.add(field("code", "Code", "text", ""));
            bin.fields.add(field("aisle", "Aisle", "number", "1"));
            bin.fields.add(field("shelf", "Shelf", "number", "1"));
            bin.fields.add(field("position", "Position", "number", "1"));
            bin.fields.add(field("sku", "SKU stored here", "text", ""));
            v.forms.add(bin);
            List<PickLineDto> picks = warehouse.pickList(Caller.name(), storeId, location);
            v.rowsTitle = "Pick list, in walking order";
            v.rowsEmpty = "Nothing to pick.";
            v.headA = "Bin";
            v.headB = "Item";
            v.headC = "Orders";
            v.headD = "Quantity";
            for (int iter = 0; iter < picks.size(); iter++) {
                PickLineDto p = picks.get(iter);
                v.rows.add(row(p.binCode(), p.title() + " (" + p.sku() + ")", String.valueOf(p.orders()),
                        String.valueOf(p.quantity())));
            }
            List<BinDto> bins = warehouse.bins(Caller.name(), storeId, location);
            for (int iter = 0; iter < bins.size(); iter++) {
                BinDto b = bins.get(iter);
                v.facts.add(row(b.code(), b.sku() == null || b.sku().length() == 0 ? "empty" : b.sku(),
                        "", ""));
            }
        }
        return render(model, request, storeId, "Warehouse", v);
    }

    /// ASCII lower case of a constant's name, locale independent.
    static String lower(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.toString();
    }

    static String stageTitle(String stage) {
        if (WarehouseFlow.PUTAWAY.equals(stage)) {
            return "Put-away";
        }
        if (stage.length() == 0) {
            return stage;
        }
        return stage.charAt(0) + lower(stage.substring(1).replace('_', ' '));
    }

    @PostMapping("/admin/{storeId}/warehouse/sync")
    public String syncWarehouse(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        String location = param(request, "location");
        return change(request, base(storeId) + "/warehouse?location=" + SiteUrls.encode(location),
                "Board synced", () -> warehouse.sync(Caller.name(), storeId, location));
    }

    @PostMapping("/admin/{storeId}/warehouse/move")
    public String moveWarehouse(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        String location = param(request, "location");
        return change(request, base(storeId) + "/warehouse?location=" + SiteUrls.encode(location),
                "Moved", () -> warehouse.move(Caller.name(), storeId,
                        new WarehouseMoveDto(param(request, "task"), param(request, "to"))));
    }

    @PostMapping("/admin/{storeId}/warehouse/bins")
    public String saveBin(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        String location = param(request, "location");
        return change(request, base(storeId) + "/warehouse?location=" + SiteUrls.encode(location),
                "Bin saved", () -> {
                    String sku = param(request, "sku");
                    String variant = "";
                    if (sku.length() > 0) {
                        Map r = db.queryOne("SELECT v.id FROM variant v JOIN product p ON p.id = "
                                + "v.product_id WHERE p.store_id = ? AND v.sku = ?",
                                new Object[] {storeId, sku});
                        if (r == null) {
                            throw new ResponseStatusException(400, "No variant has that SKU");
                        }
                        variant = Rows.text(r.get("id"));
                    }
                    warehouse.saveBin(Caller.name(), storeId, location, new BinDto(null, location,
                            param(request, "code"), number(param(request, "aisle")),
                            number(param(request, "shelf")), number(param(request, "position")),
                            variant, sku));
                });
    }

    // ------------------------------------------------------------ timelines

    @GetMapping("/admin/{storeId}/transfers/{id}/timeline")
    public String transferTimeline(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Transfer timeline");
        return timeline(model, request, storeId, "Transfer timeline",
                timelines.transfer(Caller.name(), storeId, id), "/inventory");
    }

    @GetMapping("/admin/{storeId}/purchase-orders/{id}/timeline")
    public String purchaseTimeline(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Purchase order timeline");
        return timeline(model, request, storeId, "Purchase order timeline",
                timelines.purchase(Caller.name(), storeId, id), "/inventory");
    }

    private String timeline(Model model, HttpServer.Request request, String storeId, String title,
            List<TimelineEventDto> events, String back) throws Exception {
        MarketingView v = view("timeline");
        v.back = base(storeId) + back;
        v.backLabel = "Inventory";
        timelineMarks(v, events);
        v.rowsTitle = "Events";
        v.headA = "Event";
        v.headB = "Note";
        v.headC = "By";
        v.headD = "When";
        for (int iter = 0; iter < events.size(); iter++) {
            TimelineEventDto e = events.get(iter);
            v.rows.add(row(eventTitle(e.kind()), e.message(), e.username(), Days.iso(e.at())));
        }
        return render(model, request, storeId, title, v);
    }

    /// A vertical line of stops, one per event, oldest at the top.
    static void timelineMarks(MarketingView v, List<TimelineEventDto> events) {
        int step = 56;
        int h = Math.max(1, events.size()) * step + 20;
        v.chartTitle = "Timeline";
        v.chartBox = "0 0 420 " + h;
        if (events.size() > 1) {
            FlowEdge spine = new FlowEdge();
            spine.path = "M24 30 L24 " + (30 + (events.size() - 1) * step);
            v.lines.add(spine);
        }
        for (int iter = 0; iter < events.size(); iter++) {
            TimelineEventDto e = events.get(iter);
            ChartMark m = new ChartMark();
            m.x = 24;
            m.y = 30 + iter * step;
            m.textX = 44;
            m.valueX = 44;
            m.textY = m.y + 5;
            m.label = eventTitle(e.kind());
            m.value = Days.iso(e.at());
            m.detail = Days.iso(e.at()) + (e.username() == null || e.username().length() == 0 ? ""
                    : " - " + e.username());
            m.opacity = Timelines.CLOSED.equals(e.kind()) ? "0.4" : "1";
            v.marks.add(m);
        }
    }

    static String eventTitle(String kind) {
        if (Timelines.PARTIAL.equals(kind)) {
            return "Partially received";
        }
        return kind == null || kind.length() == 0 ? "" : kind.charAt(0)
                + lower(kind.substring(1).replace('_', ' '));
    }

    // ------------------------------------------------------------- forecast

    @GetMapping("/admin/{storeId}/forecast")
    public String forecast(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Forecast");
        MarketingView v = view("forecast");
        v.intro = "Demand is the last four weeks' average, shaped by weekday. Cover is how many "
                + "days the available stock lasts at that pace.";
        List<ForecastDto> rows = forecasts.forecast(Caller.name(), storeId);
        v.rowsTitle = "Cover by location";
        v.headA = "Item";
        v.headB = "Location";
        v.headC = "Per day / cover";
        v.headD = "Reorder";
        for (int iter = 0; iter < rows.size(); iter++) {
            ForecastDto f = rows.get(iter);
            v.rows.add(row(f.title() + " (" + f.sku() + ")", f.locationName() + ": " + f.available()
                    + " available, " + f.incoming() + " incoming", hundredths(f.dailyX100()) + " / "
                    + (f.daysOfCover() >= 9999 ? "no demand" : f.daysOfCover() + " days"),
                    String.valueOf(f.reorderQuantity())));
        }
        List<SuggestionDto> s = forecasts.suggestions(Caller.name(), storeId);
        v.cardsTitle = "Suggestions";
        for (int iter = 0; iter < s.size(); iter++) {
            SuggestionDto d = s.get(iter);
            ActionCard c = new ActionCard();
            c.title = d.title();
            c.status = d.kind();
            c.detail = d.reason();
            c.amount = String.valueOf(d.quantity());
            v.cards.add(c);
        }
        v.forms.add(new FormView("reorder", "Draft purchase orders", base(storeId) + "/forecast/reorder",
                "Draft purchase orders"));
        v.forms.add(new FormView("rebalance", "Draft transfers", base(storeId) + "/forecast/rebalance",
                "Draft transfers"));
        return render(model, request, storeId, "Forecast", v);
    }

    static String hundredths(long x100) {
        long whole = x100 / 100;
        long frac = Math.abs(x100 % 100);
        return whole + "." + (frac < 10 ? "0" : "") + frac;
    }

    @PostMapping("/admin/{storeId}/forecast/reorder")
    public String draftReorders(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/forecast", "Draft purchase orders made",
                () -> forecasts.draftReorders(Caller.name(), storeId, param(request, "location")));
    }

    @PostMapping("/admin/{storeId}/forecast/rebalance")
    public String draftTransfers(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/forecast", "Draft transfers made",
                () -> forecasts.draftTransfers(Caller.name(), storeId, param(request, "location")));
    }

    // -------------------------------------------------------------- reports

    @GetMapping("/admin/{storeId}/reports")
    public String reportsPage(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Reports");
        return report(model, request, storeId, null, null);
    }

    /// Runs the builder's form; with a name, saves it too.
    @PostMapping("/admin/{storeId}/reports")
    public String runReport(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        ReportSpecDto spec = new ReportSpecDto(param(request, "name"), param(request, "dimension"),
                param(request, "dimension2"), metrics(request), param(request, "channel"),
                param(request, "source"), param(request, "from"), param(request, "to"));
        try {
            ReportDto r = reports.run(Caller.name(), storeId, spec);
            if (spec.name().length() > 0) {
                reports.save(Caller.name(), storeId, spec);
                WebPages.toast(request, "Report saved");
            }
            return report(model, request, storeId, spec, r);
        } catch (ResponseStatusException e) {
            if (e.getStatus() == 401 || e.getStatus() == 403 || e.getStatus() == 404) {
                throw e;
            }
            WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
            return "redirect:" + base(storeId) + "/reports";
        }
    }

    private static String metrics(HttpServer.Request request) {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0; iter < Reports.METRICS.length; iter++) {
            String id = Reports.METRICS[iter][0];
            if ("1".equals(param(request, "m_" + id))) {
                sb.append(sb.length() == 0 ? "" : ",").append(id);
            }
        }
        return sb.toString();
    }

    @GetMapping("/admin/{storeId}/reports/{id}")
    public String savedReport(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Report");
        ReportSpecDto spec = reports.spec(Caller.name(), storeId, id);
        return report(model, request, storeId, spec, reports.runSaved(Caller.name(), storeId, id));
    }

    /// A saved report as a CSV download.
    @GetMapping("/admin/{storeId}/reports/{id}/csv")
    public HttpServer.Response reportCsv(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Report");
        ReportDto r = reports.runSaved(Caller.name(), storeId, id);
        return new HttpServer.Response(200, "text/csv; charset=utf-8",
                Reports.csv(r).getBytes(StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=\"report.csv\"");
    }

    private String report(Model model, HttpServer.Request request, String storeId, ReportSpecDto spec,
            ReportDto r) throws Exception {
        MarketingView v = view("report");
        v.intro = "Pick what to group by and what to measure. Sources are the last place a "
                + "shopper came from before ordering.";
        FormView f = new FormView("report", "Build a report", base(storeId) + "/reports", "Run");
        FormField d1 = field("dimension", "Group by", "select", spec == null ? "day" : spec.dimension());
        FormField d2 = field("dimension2", "Then by", "select", spec == null ? "" : spec.dimension2());
        d2.options.add(option("", "Nothing", spec == null || spec.dimension2().length() == 0));
        for (int iter = 0; iter < Reports.DIMENSIONS.length; iter++) {
            String id = Reports.DIMENSIONS[iter][0];
            d1.options.add(option(id, Reports.DIMENSIONS[iter][1], id.equals(d1.value)));
            d2.options.add(option(id, Reports.DIMENSIONS[iter][1], id.equals(d2.value)));
        }
        f.fields.add(d1);
        f.fields.add(d2);
        String m = spec == null ? "sales,orders" : spec.metrics();
        for (int iter = 0; iter < Reports.METRICS.length; iter++) {
            FormField c = field("m_" + Reports.METRICS[iter][0], Reports.METRICS[iter][1], "checkbox", "1");
            c.checked = (","  + m + ",").indexOf("," + Reports.METRICS[iter][0] + ",") >= 0;
            f.fields.add(c);
        }
        f.fields.add(field("channel", "Channel (ONLINE, POS...)", "text", spec == null ? "" : spec.channel()));
        f.fields.add(field("source", "Source", "text", spec == null ? "" : spec.source()));
        f.fields.add(field("from", "From (yyyy-MM-dd)", "text", spec == null ? "" : spec.from()));
        f.fields.add(field("to", "To (yyyy-MM-dd)", "text", spec == null ? "" : spec.to()));
        FormField name = field("name", "Save as (optional)", "text", "");
        f.fields.add(name);
        v.forms.add(f);
        List<SavedReportDto> saved = reports.saved(Caller.name(), storeId);
        for (int iter = 0; iter < saved.size(); iter++) {
            AdminRow l = row(saved.get(iter).name(), "", "", "");
            l.url = base(storeId) + "/reports/" + saved.get(iter).id();
            v.links.add(l);
            AdminRow csv = row(saved.get(iter).name() + " (CSV)", "", "", "");
            csv.url = l.url + "/csv";
            v.links.add(csv);
        }
        if (r != null) {
            v.rowsTitle = (spec.name().length() > 0 ? spec.name() + ": " : "") + spec.from() + " to "
                    + spec.to();
            String[] heads = new String[4];
            for (int iter = 0; iter < 4; iter++) {
                heads[iter] = iter < r.columns().size() ? r.columns().get(iter) : "";
            }
            v.headA = heads[0];
            v.headB = heads[1];
            v.headC = heads[2];
            v.headD = heads[3];
            for (int iter = 0; iter < r.rows().size(); iter++) {
                List<String> c = r.rows().get(iter).cells();
                v.rows.add(row(cell(c, 0), cell(c, 1), cell(c, 2), cell(c, 3)));
            }
            bars(v, r, r.columns().get(spec.dimension2().length() > 0 ? 2 : 1));
        }
        return render(model, request, storeId, "Reports", v);
    }

    private static String cell(List<String> c, int index) {
        return index < c.size() ? c.get(index) : "";
    }

    /// Horizontal bars of each row's first metric, longest the full width.
    static void bars(MarketingView v, ReportDto r, String metric) {
        int n = Math.min(r.rows().size(), 30);
        long max = 1;
        for (int iter = 0; iter < n; iter++) {
            max = Math.max(max, r.rows().get(iter).value());
        }
        v.chartTitle = metric;
        v.chartBox = "0 0 600 " + (n * 24 + 10);
        for (int iter = 0; iter < n; iter++) {
            long value = Math.max(0, r.rows().get(iter).value());
            ChartMark m = new ChartMark();
            m.x = 200;
            m.y = 6 + iter * 24;
            m.height = 16;
            m.width = (int) Math.max(1, value * 380 / max);
            m.textX = 4;
            m.textY = m.y + 13;
            m.valueX = 600;
            m.label = cell(r.rows().get(iter).cells(), 0);
            m.value = String.valueOf(value);
            v.marks.add(m);
        }
    }

    // ------------------------------------------------- cohorts, attribution

    @GetMapping("/admin/{storeId}/cohorts")
    public String cohorts(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Cohorts");
        MarketingView v = view("cohorts");
        v.intro = "Each row is the customers whose first order fell in that month; each cell the "
                + "share of them who ordered again that many months later.";
        List<CohortRowDto> rows = reports.cohorts(Caller.name(), storeId);
        cohortMarks(v, rows);
        v.rowsTitle = "Cohorts";
        v.headA = "First order";
        v.headB = "Customers";
        v.headC = "Month 1";
        v.headD = "Month 3";
        for (int iter = 0; iter < rows.size(); iter++) {
            CohortRowDto r = rows.get(iter);
            v.rows.add(row(r.month(), String.valueOf(r.size()), percent(r.retainedBp(), 1),
                    percent(r.retainedBp(), 3)));
        }
        return render(model, request, storeId, "Cohorts", v);
    }

    static String percent(List<Integer> bp, int index) {
        if (index >= bp.size()) {
            return "";
        }
        int b = bp.get(index).intValue();
        return b / 100 + "." + b % 100 / 10 + "%";
    }

    /// The retention grid as a heat map: darker is more retained.
    static void cohortMarks(MarketingView v, List<CohortRowDto> rows) {
        int cell = 40;
        int cols = 1;
        for (int iter = 0; iter < rows.size(); iter++) {
            cols = Math.max(cols, rows.get(iter).retainedBp().size());
        }
        v.chartTitle = "Retention by first-order month";
        v.chartBox = "0 0 " + (90 + cols * cell) + " " + (rows.size() * cell + 10);
        for (int r = 0; r < rows.size(); r++) {
            List<Integer> bp = rows.get(r).retainedBp();
            for (int c = 0; c < bp.size(); c++) {
                int b = bp.get(c).intValue();
                ChartMark m = new ChartMark();
                m.x = 90 + c * cell;
                m.y = 5 + r * cell;
                m.width = cell - 2;
                m.height = cell - 2;
                m.textX = c == 0 ? 0 : m.x + 4;
                m.valueX = m.x + 2;
                m.textY = m.y + 22;
                m.label = c == 0 ? rows.get(r).month() : "";
                m.detail = "";
                m.value = percent(bp, c);
                int o = Math.max(5, Math.min(100, b / 100));
                m.opacity = o >= 100 ? "1" : "0." + (o < 10 ? "0" : "") + o;
                v.marks.add(m);
            }
        }
    }

    @GetMapping("/admin/{storeId}/attribution")
    public String attribution(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Attribution");
        MarketingView v = view("attribution");
        v.intro = "Where customers came from: the first page that brought them, and the last one "
                + "before they ordered (utm tags or the referring site).";
        String from = param(request, "from");
        String to = param(request, "to");
        List<AttributionDto> rows = reports.attribution(Caller.name(), storeId, from, to);
        v.rowsTitle = "Sales by source";
        v.headA = "Touch";
        v.headB = "Source / medium";
        v.headC = "Orders";
        v.headD = "Sales";
        Map cur = db.queryOne("SELECT currency FROM store WHERE id = ?", new Object[] {storeId});
        String currency = cur == null ? "USD" : Rows.text(cur.get("currency"));
        long max = 1;
        for (int iter = 0; iter < rows.size(); iter++) {
            max = Math.max(max, rows.get(iter).salesCents());
        }
        int n = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            AttributionDto a = rows.get(iter);
            v.rows.add(row(a.touch(), a.source() + " / " + a.medium(), String.valueOf(a.orders()),
                    com.codenameone.examples.bazaar.web.Money.format(a.salesCents(), currency)));
            if ("LAST".equals(a.touch()) && n < 20) {
                ChartMark m = new ChartMark();
                m.x = 200;
                m.y = 6 + n * 24;
                m.height = 16;
                m.width = (int) Math.max(1, Math.max(0, a.salesCents()) * 380 / max);
                m.textX = 4;
                m.textY = m.y + 13;
                m.label = a.source() + " / " + a.medium();
                m.value = String.valueOf(a.salesCents());
                v.marks.add(m);
                n++;
            }
        }
        v.chartTitle = "Last-touch sales";
        v.chartBox = "0 0 600 " + (n * 24 + 10);
        return render(model, request, storeId, "Attribution", v);
    }

    // ---------------------------------------------------------- permissions

    @GetMapping("/admin/{storeId}/permissions")
    public String permissionsPage(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Permissions");
        MarketingView v = view("permissions");
        v.intro = "A permission set grants read or write per area. Assigned to a staff member "
                + "(everywhere or at one location) it replaces what their role allows. Without one, "
                + "the role decides as before.";
        List<PermissionSetDto> sets = permissions.sets(Caller.name(), storeId);
        v.cardsTitle = "Permission sets";
        for (int iter = 0; iter < sets.size(); iter++) {
            ActionCard c = new ActionCard();
            c.title = sets.get(iter).name();
            c.detail = sets.get(iter).grants();
            v.cards.add(c);
        }
        FormView create = new FormView("set", "New permission set", base(storeId) + "/permissions",
                "Create");
        create.fields.add(field("name", "Name", "text", ""));
        for (int iter = 0; iter < Perms.AREAS.length; iter++) {
            FormField a = field("a_" + Perms.AREAS[iter], Perms.AREAS[iter], "select", "N");
            a.options.add(option("N", "None", true));
            a.options.add(option("R", "Read", false));
            a.options.add(option("W", "Write", false));
            create.fields.add(a);
        }
        v.forms.add(create);
        FormView assign = new FormView("assign", "Assign a set", base(storeId) + "/permissions/assign",
                "Assign");
        FormField staff = field("staff", "Staff member", "select", "");
        List st = db.query("SELECT id, username, role FROM store_staff WHERE store_id = ? AND role <> ? "
                + "ORDER BY username", new Object[] {storeId, "OWNER"});
        for (int iter = 0; iter < st.size(); iter++) {
            Map r = (Map) st.get(iter);
            staff.options.add(option(Rows.text(r.get("id")), Rows.text(r.get("username")) + " ("
                    + Rows.text(r.get("role")) + ")", iter == 0));
        }
        assign.fields.add(staff);
        FormField loc = field("location", "Where", "select", "");
        loc.options.add(option("", "Every location", true));
        List<Map> locs = locations(storeId);
        for (int iter = 0; iter < locs.size(); iter++) {
            loc.options.add(option(Rows.text(locs.get(iter).get("id")),
                    Rows.text(locs.get(iter).get("name")), false));
        }
        assign.fields.add(loc);
        FormField set = field("set", "Set", "select", "");
        set.options.add(option("", "None (use the role)", true));
        for (int iter = 0; iter < sets.size(); iter++) {
            set.options.add(option(sets.get(iter).id(), sets.get(iter).name(), false));
        }
        assign.fields.add(set);
        v.forms.add(assign);
        List<StaffPermissionDto> as = permissions.assignments(Caller.name(), storeId);
        v.rowsTitle = "Assignments";
        v.headA = "Staff";
        v.headB = "Role";
        v.headC = "Where";
        v.headD = "Set";
        for (int iter = 0; iter < as.size(); iter++) {
            StaffPermissionDto a = as.get(iter);
            String where = "Every location";
            for (int k = 0; k < locs.size(); k++) {
                if (Rows.text(locs.get(k).get("id")).equals(a.locationId())) {
                    where = Rows.text(locs.get(k).get("name"));
                }
            }
            v.rows.add(row(a.username(), a.role(), where, a.setName()));
        }
        return render(model, request, storeId, "Permissions", v);
    }

    static String grants(HttpServer.Request request) {
        char[] levels = new char[Perms.AREAS.length];
        for (int iter = 0; iter < levels.length; iter++) {
            String l = param(request, "a_" + Perms.AREAS[iter]);
            levels[iter] = "W".equals(l) ? 'W' : "R".equals(l) ? 'R' : 'N';
        }
        return Perms.format(levels);
    }

    @PostMapping("/admin/{storeId}/permissions")
    public String createSet(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/permissions", "Permission set created",
                () -> permissions.create(Caller.name(), storeId,
                        new PermissionSetDto(null, param(request, "name"), grants(request))));
    }

    @PostMapping("/admin/{storeId}/permissions/assign")
    public String assignSet(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/permissions", "Assignment saved",
                () -> permissions.assign(Caller.name(), storeId, new StaffPermissionDto(null,
                        param(request, "staff"), null, null, param(request, "location"),
                        param(request, "set"), null)));
    }

    // ---------------------------------------------------------------- inbox

    @GetMapping("/admin/{storeId}/inbox")
    public String inbox(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Inbox");
        MarketingView v = view("inbox");
        v.intro = "Messages from the storefront chat. A reply is emailed to the customer.";
        List<ChatConversationDto> list = chat.list(Caller.name(), storeId);
        v.cardsTitle = "Conversations";
        for (int iter = 0; iter < list.size(); iter++) {
            ChatConversationDto c = list.get(iter);
            ActionCard a = new ActionCard();
            a.title = c.name() + " <" + c.email() + ">";
            a.url = base(storeId) + "/inbox/" + c.id();
            a.status = c.status();
            a.detail = c.subject();
            a.meta = (c.assignee().length() == 0 ? "unassigned" : c.assignee()) + ", " + c.messages()
                    + " messages";
            v.cards.add(a);
        }
        v.rowsTitle = "Canned replies";
        v.headA = "Title";
        v.headB = "Reply";
        List<CannedReplyDto> canned = chat.canned(Caller.name(), storeId);
        for (int iter = 0; iter < canned.size(); iter++) {
            v.rows.add(row(canned.get(iter).title(), canned.get(iter).body(), "", ""));
        }
        FormView f = new FormView("canned", "New canned reply", base(storeId) + "/inbox/canned", "Save");
        f.fields.add(field("title", "Title", "text", ""));
        f.fields.add(field("body", "Reply", "textarea", ""));
        v.forms.add(f);
        return render(model, request, storeId, "Inbox", v);
    }

    @GetMapping("/admin/{storeId}/inbox/{id}")
    public String conversation(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Conversation");
        List<ChatMessageDto> messages = chat.messages(Caller.name(), storeId, id);
        MarketingView v = view("conversation");
        v.back = base(storeId) + "/inbox";
        v.backLabel = "Inbox";
        v.rowsTitle = "Messages";
        v.headA = "From";
        v.headB = "Message";
        v.headC = "";
        v.headD = "When";
        for (int iter = 0; iter < messages.size(); iter++) {
            ChatMessageDto m = messages.get(iter);
            v.rows.add(row(m.author() + (m.staff() ? " (staff)" : ""), m.body(), "", Days.iso(m.at())));
        }
        String path = base(storeId) + "/inbox/" + id;
        FormView reply = new FormView("reply", "Reply", path + "/reply", "Send");
        FormField canned = field("canned", "Canned reply", "select", "");
        canned.options.add(option("", "Write my own", true));
        List<CannedReplyDto> cs = chat.canned(Caller.name(), storeId);
        for (int iter = 0; iter < cs.size(); iter++) {
            canned.options.add(option(cs.get(iter).id(), cs.get(iter).title(), false));
        }
        reply.fields.add(canned);
        reply.fields.add(field("body", "Message", "textarea", ""));
        v.forms.add(reply);
        FormView assign = new FormView("assign", "Assign", path + "/assign", "Assign");
        FormField who = field("to", "Staff member", "select", "");
        who.options.add(option("", "Nobody", false));
        List st = db.query("SELECT username FROM store_staff WHERE store_id = ? ORDER BY username",
                new Object[] {storeId});
        for (int iter = 0; iter < st.size(); iter++) {
            String u = Rows.text(((Map) st.get(iter)).get("username"));
            who.options.add(option(u, u, u.equals(Caller.name())));
        }
        assign.fields.add(who);
        v.forms.add(assign);
        FormView close = new FormView("close", "Close", path + "/close", "Close conversation");
        v.forms.add(close);
        return render(model, request, storeId, "Conversation", v);
    }

    @PostMapping("/admin/{storeId}/inbox/{id}/reply")
    public String reply(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/inbox/" + id, "Reply sent", () -> {
            String body = param(request, "body");
            String canned = param(request, "canned");
            if (body.length() == 0 && canned.length() > 0) {
                List<CannedReplyDto> cs = chat.canned(Caller.name(), storeId);
                for (int iter = 0; iter < cs.size(); iter++) {
                    if (cs.get(iter).id().equals(canned)) {
                        body = cs.get(iter).body();
                    }
                }
            }
            chat.reply(Caller.name(), storeId, id, body);
        });
    }

    @PostMapping("/admin/{storeId}/inbox/{id}/assign")
    public String assign(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/inbox/" + id, "Assigned",
                () -> chat.assign(Caller.name(), storeId, id, param(request, "to")));
    }

    @PostMapping("/admin/{storeId}/inbox/{id}/close")
    public String close(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/inbox", "Conversation closed",
                () -> chat.close(Caller.name(), storeId, id));
    }

    @PostMapping("/admin/{storeId}/inbox/canned")
    public String saveCanned(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/inbox", "Canned reply saved",
                () -> chat.saveCanned(Caller.name(), storeId,
                        new CannedReplyDto(null, param(request, "title"), param(request, "body"))));
    }

    // ------------------------------------------------------------- listings

    @GetMapping("/admin/{storeId}/listings")
    public String listingsPage(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Listings");
        return listings(model, request, storeId, null);
    }

    private String listings(Model model, HttpServer.Request request, String storeId,
            ImportResultDto result) throws Exception {
        MarketingView v = view("listings");
        v.intro = "A combined listing shows separate products as the colours of one. The import "
                + "reads a marketplace CSV (listing_id, title, description, brand, category, color, "
                + "size, sku, price, quantity) and previews it before anything is made.";
        List<CombinedListingDto> list = listings.list(Caller.name(), storeId);
        v.cardsTitle = "Combined listings";
        for (int iter = 0; iter < list.size(); iter++) {
            CombinedListingDto l = list.get(iter);
            ActionCard c = new ActionCard();
            c.title = l.title();
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < l.members().size(); k++) {
                ListingMemberDto m = l.members().get(k);
                sb.append(k == 0 ? "" : ", ").append(m.color()).append(": ").append(m.title());
            }
            c.detail = sb.toString();
            v.cards.add(c);
        }
        FormView combine = new FormView("combine", "Combine products", base(storeId) + "/listings",
                "Combine");
        combine.fields.add(field("title", "Title", "text", ""));
        FormField members = field("members", "One product per line: handle=colour", "textarea", "");
        combine.fields.add(members);
        v.forms.add(combine);
        FormView imp = new FormView("import", "Marketplace import", base(storeId) + "/listings/import",
                "Preview");
        imp.fields.add(field("csv", "CSV", "textarea", ""));
        FormField commit = field("commit", "Create the products when nothing is wrong", "checkbox", "1");
        imp.fields.add(commit);
        v.forms.add(imp);
        if (result != null) {
            v.rowsTitle = (result.committed() ? "Imported: " : "Preview: ") + result.rows() + " rows, "
                    + result.products() + " products, " + result.variants() + " variants";
            v.rowsEmpty = "No problems.";
            v.headA = "Line";
            v.headB = "Problem";
            for (int iter = 0; iter < result.errors().size(); iter++) {
                CsvErrorDto e = result.errors().get(iter);
                v.rows.add(row(String.valueOf(e.line()), e.message(), "", ""));
            }
            for (int iter = 0; iter < result.preview().size(); iter++) {
                v.facts.add(row(result.preview().get(iter), "", "", ""));
            }
        }
        return render(model, request, storeId, "Listings", v);
    }

    @PostMapping("/admin/{storeId}/listings")
    public String combine(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/listings", "Combined listing saved", () -> {
            List<ListingMemberDto> members = new ArrayList<ListingMemberDto>();
            String text = param(request, "members");
            int start = 0;
            while (start < text.length()) {
                int nl = text.indexOf('\n', start);
                String line = text.substring(start, nl < 0 ? text.length() : nl).trim();
                start = nl < 0 ? text.length() : nl + 1;
                if (line.length() == 0) {
                    continue;
                }
                int eq = line.indexOf('=');
                String handle = (eq < 0 ? line : line.substring(0, eq)).trim();
                Map p = db.queryOne("SELECT id FROM product WHERE store_id = ? AND handle = ?",
                        new Object[] {storeId, handle});
                if (p == null) {
                    throw new ResponseStatusException(400, "No product has the handle " + handle);
                }
                members.add(new ListingMemberDto(Rows.text(p.get("id")), null,
                        eq < 0 ? "" : line.substring(eq + 1).trim()));
            }
            listings.save(Caller.name(), storeId,
                    new CombinedListingDto(null, param(request, "title"), members));
        });
    }

    /// The import renders its preview (or result) in place: there is nothing
    /// to redirect to until the products exist.
    @PostMapping("/admin/{storeId}/listings/import")
    public String importCsv(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        ImportResultDto r = listings.importCsv(Caller.name(), storeId,
                new ImportDto(param(request, "csv"), "1".equals(param(request, "commit"))));
        return listings(model, request, storeId, r);
    }
}
