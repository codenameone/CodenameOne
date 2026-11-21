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
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.api.BoardMoveDto;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;

/// The back office's fulfilment board and its marketing: sales, the
/// promotions calendar, discounts, loyalty, clubs, segments, campaigns and
/// journeys, drawn by `templates/marketing.html`.
///
/// The same shape as [AdminPages]: every change a form post answered with 303
/// and a one-line message, so it all works with scripts off. A store the
/// caller does not work at is a 404 from [WebPages#admin] (pages) or from
/// [#member] (posts), and the services decide the role: a fulfilment clerk
/// may move the board but not open a campaign.
@Controller
public class MarketingPages {
    private final WebPages pages;
    private final AdminData data;
    private final MarketingData marketing;
    private final Parcels parcels;
    private final Fulfillments fulfillments;
    private final WebSessions sessions;

    public MarketingPages(WebPages pages, AdminData data, MarketingData marketing,
            Parcels parcels, Fulfillments fulfillments, WebSessions sessions) {
        this.pages = pages;
        this.data = data;
        this.marketing = marketing;
        this.parcels = parcels;
        this.fulfillments = fulfillments;
        this.sessions = sessions;
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

    /// As in [AdminPages]: a refusal the merchant can act on becomes the
    /// message; 401, 403 and 404 answer their status.
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

    /// A create or save that lands on the saved row's page, or back on the
    /// form with the reason it was refused.
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

    // ============================================================ fulfilment

    /// Orders by fulfilment status; each card is a button that moves it one
    /// column on. With htmx the form swaps only the board.
    @GetMapping("/admin/{storeId}/fulfilment")
    public String board(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("board");
        v.intro = "Each card moves one step: pack it, ship it, mark it delivered.";
        parcels.board(storeId, v);
        return render(model, request, storeId, "Fulfilment", v, null);
    }

    @PostMapping("/admin/{storeId}/fulfilment/move")
    public String move(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        String order = AdminForms.field(request, "order");
        String to = AdminForms.field(request, "to");
        return change(request, base(storeId) + "/fulfilment", "Order moved",
                () -> fulfillments.moveCard(Caller.name(), storeId, new BoardMoveDto(order, to)));
    }

    // ================================================================= sales

    @GetMapping("/admin/{storeId}/sales")
    public String sales(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("sales");
        DataTable t = marketing.sales(storeId, request, v);
        return render(model, request, storeId, "Sales", v, t);
    }

    @PostMapping("/admin/{storeId}/sales")
    public String createSale(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/sales/", base(storeId) + "/sales",
                "Sale scheduled", () -> marketing.createSale(storeId, request));
    }

    @GetMapping("/admin/{storeId}/sales/{id}")
    public String sale(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("sale");
        String title = marketing.sale(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/sales/{id}/reschedule")
    public String rescheduleSale(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/sales/" + id, "Sale moved",
                () -> marketing.reschedule(storeId, "SALE", id, request));
    }

    @PostMapping("/admin/{storeId}/sales/{id}/cancel")
    public String cancelSale(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/sales/" + id, "Sale cancelled",
                () -> marketing.cancelSale(storeId, id));
    }

    // ============================================================== calendar

    @GetMapping("/admin/{storeId}/calendar")
    public String calendar(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("calendar");
        marketing.calendar(storeId, AdminForms.field(request, "month"), v);
        return render(model, request, storeId, "Calendar", v, null);
    }

    /// Moves any event on the calendar: a sale, a dated discount, a campaign.
    @PostMapping("/admin/{storeId}/calendar/move")
    public String calendarMove(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        String month = AdminForms.field(request, "month");
        String back = base(storeId) + "/calendar" + (month.length() == 7 && month.charAt(4) == '-'
                ? "?month=" + month : "");
        return change(request, back, "Moved", () -> marketing.reschedule(storeId,
                AdminForms.field(request, "kind"), AdminForms.field(request, "id"), request));
    }

    // ============================================================= discounts

    @GetMapping("/admin/{storeId}/discounts")
    public String discounts(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("discounts");
        DataTable t = marketing.discounts(storeId, request, v);
        return render(model, request, storeId, "Discounts", v, t);
    }

    @GetMapping("/admin/{storeId}/new-discount")
    public String newDiscount(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("discount");
        String title = marketing.discountEditor(storeId, null, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/discounts")
    public String createDiscount(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/discounts/", base(storeId) + "/new-discount",
                "Discount saved", () -> marketing.saveDiscount(storeId, null, request));
    }

    @GetMapping("/admin/{storeId}/discounts/{id}")
    public String discount(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        MarketingView v = view("discount");
        String title = marketing.discountEditor(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/discounts/{id}")
    public String saveDiscount(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/discounts/", base(storeId) + "/discounts/" + id,
                "Discount saved", () -> marketing.saveDiscount(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/discounts/{id}/rules")
    public String saveRules(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/discounts/" + id, "Rules saved",
                () -> marketing.saveRules(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/discounts/{id}/delete")
    public String deleteDiscount(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/discounts", "Discount deleted",
                () -> marketing.deleteDiscount(storeId, id));
    }

    // =============================================================== loyalty

    @GetMapping("/admin/{storeId}/loyalty")
    public String loyalty(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("loyalty");
        marketing.loyalty(storeId, v);
        DataTable t = marketing.members(storeId, request);
        return render(model, request, storeId, "Loyalty", v, t);
    }

    @PostMapping("/admin/{storeId}/loyalty")
    public String saveLoyalty(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/loyalty", "Programme saved",
                () -> marketing.saveLoyalty(storeId, request));
    }

    // ================================================================= clubs

    @GetMapping("/admin/{storeId}/clubs")
    public String clubs(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("clubs");
        DataTable t = marketing.clubs(storeId, request, v);
        return render(model, request, storeId, "Clubs", v, t);
    }

    @PostMapping("/admin/{storeId}/clubs")
    public String createClub(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/clubs/", base(storeId) + "/clubs",
                "Club created", () -> marketing.saveClub(storeId, null, request));
    }

    @GetMapping("/admin/{storeId}/clubs/{id}")
    public String club(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("club");
        String title = marketing.club(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/clubs/{id}")
    public String saveClub(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/clubs/", base(storeId) + "/clubs/" + id,
                "Club saved", () -> marketing.saveClub(storeId, id, request));
    }

    // ============================================================== segments

    @GetMapping("/admin/{storeId}/segments")
    public String segments(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("segments");
        DataTable t = marketing.segments(storeId, request, v);
        return render(model, request, storeId, "Segments", v, t);
    }

    @GetMapping("/admin/{storeId}/new-segment")
    public String newSegment(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("segment");
        String title = marketing.segmentEditor(storeId, null, request, v);
        return render(model, request, storeId, title, v, null);
    }

    /// The builder's add-row and count buttons: draws the editor again from
    /// the rows posted, saving nothing.
    @PostMapping("/admin/{storeId}/new-segment")
    public String redrawNewSegment(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        return newSegment(storeId, model, request);
    }

    @PostMapping("/admin/{storeId}/segments/{id}/edit")
    public String redrawSegment(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        return segment(storeId, id, model, request);
    }

    @PostMapping("/admin/{storeId}/segments")
    public String createSegment(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/segments/", base(storeId) + "/new-segment",
                "Segment saved", () -> marketing.saveSegment(storeId, null, request));
    }

    @GetMapping("/admin/{storeId}/segments/{id}")
    public String segment(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        MarketingView v = view("segment");
        String title = marketing.segmentEditor(storeId, id, request, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/segments/{id}")
    public String saveSegment(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/segments/", base(storeId) + "/segments/" + id,
                "Segment saved", () -> marketing.saveSegment(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/segments/{id}/delete")
    public String deleteSegment(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/segments", "Segment deleted",
                () -> marketing.deleteSegment(storeId, id));
    }

    // ============================================================= campaigns

    @GetMapping("/admin/{storeId}/campaigns")
    public String campaigns(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("campaigns");
        DataTable t = marketing.campaigns(storeId, request, v);
        return render(model, request, storeId, "Campaigns", v, t);
    }

    @GetMapping("/admin/{storeId}/new-campaign")
    public String newCampaign(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("campaign");
        String title = marketing.campaignEditor(storeId, null, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/campaigns")
    public String createCampaign(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/campaigns/", base(storeId) + "/new-campaign",
                "Campaign saved", () -> marketing.saveCampaign(storeId, null, request));
    }

    @GetMapping("/admin/{storeId}/campaigns/{id}")
    public String campaign(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        MarketingView v = view("campaign");
        String title = marketing.campaignEditor(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }

    @PostMapping("/admin/{storeId}/campaigns/{id}")
    public String saveCampaign(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return create(request, base(storeId) + "/campaigns/", base(storeId) + "/campaigns/" + id,
                "Campaign saved", () -> marketing.saveCampaign(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/campaigns/{id}/test")
    public String testCampaign(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/campaigns/" + id, "Test sent",
                () -> marketing.sendTest(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/campaigns/{id}/schedule")
    public String scheduleCampaign(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/campaigns/" + id, "Campaign scheduled",
                () -> marketing.schedule(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/campaigns/{id}/cancel")
    public String cancelCampaign(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/campaigns/" + id, "Campaign cancelled",
                () -> marketing.cancelCampaign(storeId, id));
    }

    // ============================================================== journeys

    @GetMapping("/admin/{storeId}/journeys")
    public String journeys(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("journeys");
        DataTable t = marketing.journeys(storeId, request);
        return render(model, request, storeId, "Journeys", v, t);
    }

    @GetMapping("/admin/{storeId}/journeys/{id}")
    public String journey(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, Model model, HttpServer.Request request)
            throws Exception {
        MarketingView v = view("journey");
        String title = marketing.journey(storeId, id, v);
        return render(model, request, storeId, title, v, null);
    }
}
