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
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.SiteUrls;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;

/// The back office's markets, translations, tax, fraud review and disputes
/// pages, drawn by `templates/marketing.html` in the shape of
/// [CommercePages]: every change a form post answered with 303 and a
/// one-line message; another store is a 404 and the services decide roles.
@Controller
public class GlobalPages {
    private final WebPages pages;
    private final AdminData data;
    private final GlobalData global;
    private final WebSessions sessions;

    public GlobalPages(WebPages pages, AdminData data, GlobalData global, WebSessions sessions) {
        this.pages = pages;
        this.data = data;
        this.global = global;
        this.sessions = sessions;
    }

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            MarketingView view) throws java.io.IOException {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        view.base = "/admin/" + storeId;
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
        sessions.touch(request, Caller.name());
        data.currency(storeId);
    }

    private interface Change {
        void run() throws Exception;
    }

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

    private static String base(String storeId) {
        return "/admin/" + storeId;
    }

    private static String q(String s) {
        return s == null ? "" : SiteUrls.encode(s);
    }

    // ============================================================== markets

    @GetMapping("/admin/{storeId}/markets")
    public String markets(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("markets");
        v.intro = "Sell abroad: each market has its countries, currency, rounding and price "
                + "adjustment. Shoppers pick one on the storefront.";
        pages.admin(request, storeId, "Markets");
        global.markets(storeId, v);
        return render(model, request, storeId, "Markets", v);
    }

    @PostMapping("/admin/{storeId}/markets")
    public String createMarket(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets", "Market added",
                () -> global.saveMarket(storeId, null, request));
    }

    @GetMapping("/admin/{storeId}/markets/{id}")
    public String market(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("market");
        pages.admin(request, storeId, "Market");
        String title = global.market(storeId, id, v);
        return render(model, request, storeId, title, v);
    }

    @PostMapping("/admin/{storeId}/markets/{id}")
    public String saveMarket(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets/" + id, "Market saved",
                () -> global.saveMarket(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/markets/{id}/prices")
    public String marketPrice(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets/" + id, "Price saved",
                () -> global.putPrice(storeId, id, request));
    }

    @PostMapping("/admin/{storeId}/markets/{id}/remove")
    public String removeMarket(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets", "Market removed",
                () -> global.removeMarket(storeId, id));
    }

    @PostMapping("/admin/{storeId}/fx-rates")
    public String fxRate(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets", "Rate saved",
                () -> global.putRate(storeId, request));
    }

    @PostMapping("/admin/{storeId}/duty-rates")
    public String dutyRate(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets", "Duty rate added",
                () -> global.addDuty(storeId, request));
    }

    @PostMapping("/admin/{storeId}/duty-rates/{id}/remove")
    public String removeDutyRate(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets", "Duty rate removed",
                () -> global.removeDuty(storeId, id));
    }

    @PostMapping("/admin/{storeId}/customs-codes")
    public String customsCode(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/markets", "HS code saved",
                () -> global.putCustomsCode(storeId, request));
    }

    // ========================================================= translations

    @GetMapping("/admin/{storeId}/translations")
    public String translations(@PathVariable("storeId") String storeId,
            @RequestParam(value = "kind", required = false) String kind,
            @RequestParam(value = "refId", required = false) String refId,
            @RequestParam(value = "locale", required = false) String locale, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("translations");
        v.intro = "Product, collection and page text in other languages, beside the original. "
                + "A storefront shows a market's language, or the shopper's browser language.";
        pages.admin(request, storeId, "Translations");
        global.translations(storeId, kind, refId, locale, v);
        return render(model, request, storeId, "Translations", v);
    }

    @PostMapping("/admin/{storeId}/translations/open")
    public String openTranslation(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return "redirect:" + base(storeId) + "/translations?kind="
                + q(AdminForms.field(request, "kind")) + "&refId="
                + q(AdminForms.field(request, "refId")) + "&locale="
                + q(AdminForms.field(request, "locale"));
    }

    @PostMapping("/admin/{storeId}/translations")
    public String saveTranslation(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        String back = base(storeId) + "/translations?kind=" + q(AdminForms.field(request, "kind"))
                + "&refId=" + q(AdminForms.field(request, "refId")) + "&locale="
                + q(AdminForms.field(request, "locale"));
        return change(request, back, "Translation saved",
                () -> global.putTranslations(storeId, request));
    }

    /// Every product's and collection's text with one language's
    /// translation and status, for a translator's spreadsheet.
    @GetMapping("/admin/{storeId}/translations/export.csv")
    public HttpServer.Response exportTranslations(@PathVariable("storeId") String storeId,
            @RequestParam(value = "locale", required = false) String locale,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Translations");
        return new HttpServer.Response(200, "text/csv; charset=utf-8",
                global.exportTranslations(storeId, locale).getBytes("UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"translations.csv\"");
    }

    /// The spreadsheet back: each row with a changed translation is saved,
    /// and the toast counts what was saved and names refused rows.
    @PostMapping("/admin/{storeId}/translations/import")
    public String importTranslations(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        String[] said = {null};
        String done = change(request, base(storeId) + "/translations", "Imported",
                () -> said[0] = global.importTranslations(storeId, request));
        if (said[0] != null) {
            WebPages.toast(request, said[0]);
        }
        return done;
    }

    // ================================================================= taxes

    @GetMapping("/admin/{storeId}/tax")
    public String tax(@PathVariable("storeId") String storeId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("tax");
        v.intro = "Customer tax exemptions, and the tax collected by region for a period.";
        pages.admin(request, storeId, "Tax");
        global.exemptions(storeId, from, to, v);
        return render(model, request, storeId, "Tax", v);
    }

    @PostMapping("/admin/{storeId}/tax/period")
    public String taxPeriod(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return "redirect:" + base(storeId) + "/tax?from=" + q(AdminForms.field(request, "from"))
                + "&to=" + q(AdminForms.field(request, "to"));
    }

    @GetMapping("/admin/{storeId}/tax/report.csv")
    public HttpServer.Response taxCsv(@PathVariable("storeId") String storeId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Tax");
        return new HttpServer.Response(200, "text/csv; charset=utf-8",
                global.reportCsv(storeId, from, to).getBytes("UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"tax-report.csv\"");
    }

    @GetMapping("/admin/{storeId}/tax/profit-loss.csv")
    public HttpServer.Response profitLossCsv(@PathVariable("storeId") String storeId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Tax");
        return new HttpServer.Response(200, "text/csv; charset=utf-8",
                global.profitLossCsv(storeId, from, to).getBytes("UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"profit-loss.csv\"");
    }

    @PostMapping("/admin/{storeId}/tax/exemptions")
    public String addExemption(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/tax", "Exemption added",
                () -> global.addExemption(storeId, request));
    }

    @PostMapping("/admin/{storeId}/tax/exemptions/{id}/remove")
    public String removeExemption(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/tax", "Exemption removed",
                () -> global.removeExemption(storeId, id));
    }

    // ================================================================== risk

    @GetMapping("/admin/{storeId}/risk")
    public String risk(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("risk");
        v.intro = "Every order's fraud score. A high one is held: it cannot be fulfilled "
                + "until it is approved, or cancelled.";
        pages.admin(request, storeId, "Fraud review");
        global.risks(storeId, v);
        return render(model, request, storeId, "Fraud review", v);
    }

    @PostMapping("/admin/{storeId}/risk/{orderId}/approve")
    public String approve(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/risk", "Order approved for fulfilment",
                () -> global.approve(storeId, orderId));
    }

    @PostMapping("/admin/{storeId}/risk/{orderId}/cancel")
    public String cancel(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/risk", "Order cancelled",
                () -> global.cancel(storeId, orderId));
    }

    // ============================================================== disputes

    @GetMapping("/admin/{storeId}/disputes")
    public String disputes(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("disputes");
        v.intro = "Chargebacks the payment processor reports, with their deadlines.";
        pages.admin(request, storeId, "Disputes");
        global.disputes(storeId, v);
        return render(model, request, storeId, "Disputes", v);
    }

    @GetMapping("/admin/{storeId}/disputes/{id}")
    public String dispute(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("dispute");
        pages.admin(request, storeId, "Dispute");
        String title = global.dispute(storeId, id, v);
        return render(model, request, storeId, title, v);
    }

    @PostMapping("/admin/{storeId}/disputes/{id}/evidence")
    public String evidence(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/disputes/" + id, "Evidence added",
                () -> global.addEvidence(storeId, id, "TEXT", request));
    }

    @PostMapping("/admin/{storeId}/disputes/{id}/evidence/order")
    public String orderEvidence(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/disputes/" + id, "Order record attached",
                () -> global.addEvidence(storeId, id, "ORDER", null));
    }

    @PostMapping("/admin/{storeId}/disputes/{id}/submit")
    public String submit(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/disputes/" + id, "Evidence submitted",
                () -> global.submit(storeId, id));
    }

    @PostMapping("/admin/{storeId}/disputes/{id}/accept")
    public String accept(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, base(storeId) + "/disputes/" + id, "Dispute accepted",
                () -> global.accept(storeId, id));
    }
}
