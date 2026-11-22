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
import com.codenameone.examples.bazaar.api.ApiKeyDto;
import com.codenameone.examples.bazaar.api.BulkJobDto;
import com.codenameone.examples.bazaar.api.SsoProviderDto;
import com.codenameone.examples.bazaar.api.StaffSecurityDto;
import com.codenameone.examples.bazaar.api.TaxOverrideDto;
import com.codenameone.examples.bazaar.api.TaxSettingsDto;
import com.codenameone.examples.bazaar.platform.BulkJobs;
import com.codenameone.examples.bazaar.platform.StoreApiKeys;
import com.codenameone.examples.bazaar.staff.SsoProviders;
import com.codenameone.examples.bazaar.staff.StaffSecurity;
import com.codenameone.examples.bazaar.taxes.OssReportCsv;
import com.codenameone.examples.bazaar.taxes.TaxRules;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;

import java.util.ArrayList;
import java.util.List;

/// The back office's staff security and developer page (two-factor
/// enforcement, single sign-on providers, API keys, bulk jobs) and its tax
/// rules page (EU VAT, the US provider, overrides, the OSS report), drawn by
/// `templates/marketing.html` like [GlobalPages]. The services decide roles;
/// another store is a 404 from them.
@Controller
public class PlatformPages {
    private final WebPages pages;
    private final AdminData data;
    private final WebSessions sessions;
    private final StaffSecurity security;
    private final SsoProviders sso;
    private final StoreApiKeys keys;
    private final BulkJobs bulk;
    private final TaxRules tax;

    public PlatformPages(WebPages pages, AdminData data, WebSessions sessions,
            StaffSecurity security, SsoProviders sso, StoreApiKeys keys, BulkJobs bulk,
            TaxRules tax) {
        this.pages = pages;
        this.data = data;
        this.sessions = sessions;
        this.security = security;
        this.sso = sso;
        this.keys = keys;
        this.bulk = bulk;
        this.tax = tax;
    }

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            MarketingView view) throws java.io.IOException {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        view.base = "/admin/" + storeId;
        view.heading = title;
        view.hasTable = false;
        model.addAttribute("view", view);
        model.addAttribute("table", new DataTable());
        return WebPages.view(model, shell, "marketing");
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
            if (done != null) {
                WebPages.toast(request, done);
            }
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

    private static boolean on(HttpServer.Request request, String name) {
        return "1".equals(f(request, name));
    }

    private static AdminRow row(String a, String b, String c, String d) {
        AdminRow r = new AdminRow();
        r.title = a;
        r.detail = b;
        r.meta = c;
        r.amount = d;
        return r;
    }

    private static String when(long millis) {
        return millis <= 0 ? "never" : com.codenameone.examples.bazaar.Days.iso(millis);
    }

    // ============================================================= security

    @GetMapping("/admin/{storeId}/security")
    public String securityPage(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Security and API");
        String me = Caller.name();
        String base = "/admin/" + storeId + "/security";
        MarketingView v = new MarketingView();
        v.page = "security";
        v.intro = "How staff sign in, and the keys and bulk jobs other systems use.";

        StaffSecurityDto s = security.settings(me, storeId);
        FormView two = new FormView("two-factor", "Two-factor sign-in", base + "/two-factor",
                "Save");
        two.text = s.unenrolled().isEmpty() ? "Every staff member has an authenticator app."
                : "Not yet enrolled: " + join(s.unenrolled());
        two.check("require", "Require it of every staff member", s.requireTwoFactor());
        v.forms.add(two);

        List<SsoProviderDto> providers = sso.list(me, storeId);
        v.cardsTitle = "Single sign-on, API keys and bulk jobs";
        for (int iter = 0; iter < providers.size(); iter++) {
            SsoProviderDto p = providers.get(iter);
            ActionCard k = new ActionCard();
            k.title = "Sign-on: " + p.name() + (p.enabled() ? "" : " (off)");
            k.detail = p.issuer() + ", " + p.domainRoles() + ", sign in at " + p.signInPath();
            k.button("Remove", base + "/sso/" + p.id() + "/remove", true);
            v.cards.add(k);
        }
        FormView add = new FormView("sso", "Add a single sign-on provider", base + "/sso",
                "Add provider");
        add.text = "Any OpenID Connect provider. Staff whose verified email is at a mapped "
                + "domain join with that role.";
        add.add("name", "Name", "text", "").required = true;
        add.add("issuer", "Issuer URL", "text", "").required = true;
        add.add("clientId", "Client id", "text", "").required = true;
        add.add("clientSecret", "Client secret", "password", "").required = true;
        add.add("domainRoles", "Domains and roles", "text", "")
                .hint("e.g. example.com=MANAGER, ops.example.com=FULFILMENT").required = true;
        v.forms.add(add);

        List<ApiKeyDto> all = keys.list(me, storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            ApiKeyDto k = all.get(iter);
            ActionCard c = new ActionCard();
            c.title = "Key: " + k.name() + " (" + k.display() + ")";
            c.detail = join(k.scopes()) + ", last used " + when(k.lastUsedAt())
                    + (k.revokedAt() > 0 ? ", revoked" : "");
            if (k.revokedAt() <= 0) {
                c.button("Revoke", base + "/api-keys/" + k.id() + "/revoke", true);
            }
            v.cards.add(c);
        }
        FormView key = new FormView("api-key", "Create an API key", base + "/api-keys",
                "Create key");
        key.text = "The key is shown once, when it is made. Send it as a bearer token.";
        key.add("name", "Name", "text", "").required = true;
        for (int iter = 0; iter < StoreApiKeys.SCOPES.size(); iter++) {
            String scope = StoreApiKeys.SCOPES.get(iter);
            key.check("scope_" + scope, scope, false);
        }
        v.forms.add(key);

        FormView job = new FormView("bulk", "Start a bulk export", base + "/bulk", "Start");
        job.add("kind", "Kind", "select", "EXPORT_PRODUCTS")
                .option("EXPORT_PRODUCTS", "Export products")
                .option("EXPORT_ORDERS", "Export orders");
        v.forms.add(job);
        List<BulkJobDto> jobs = bulk.list(me, storeId);
        v.rowsTitle = "Bulk jobs";
        v.rowsEmpty = "No bulk jobs yet.";
        v.headA = "Job";
        v.headB = "Status";
        v.headC = "Lines";
        v.headD = "Errors";
        for (int iter = 0; iter < jobs.size(); iter++) {
            BulkJobDto j = jobs.get(iter);
            AdminRow r = row(j.kind(), j.status(), String.valueOf(j.lineCount()),
                    String.valueOf(j.errorCount()));
            if ("DONE".equals(j.status()) && j.kind().startsWith("EXPORT")) {
                r.url = base + "/bulk/" + j.id() + "/result.jsonl";
            }
            v.rows.add(r);
        }
        return render(model, request, storeId, "Security and API", v);
    }

    @PostMapping("/admin/{storeId}/security/two-factor")
    public String saveTwoFactor(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/security", "Saved", () ->
                security.save(Caller.name(), storeId, new StaffSecurityDto(on(request, "require"),
                        null)));
    }

    @PostMapping("/admin/{storeId}/security/sso")
    public String addSso(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/security", "Provider added", () ->
                sso.save(Caller.name(), storeId, new SsoProviderDto(null, f(request, "name"),
                        f(request, "issuer"), f(request, "clientId"),
                        f(request, "clientSecret"), f(request, "domainRoles"), true, null)));
    }

    @PostMapping("/admin/{storeId}/security/sso/{id}/remove")
    public String removeSso(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/security", "Provider removed",
                () -> sso.remove(Caller.name(), storeId, id));
    }

    @PostMapping("/admin/{storeId}/security/api-keys")
    public String addKey(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        final List<String> scopes = new ArrayList<String>();
        for (int iter = 0; iter < StoreApiKeys.SCOPES.size(); iter++) {
            if (on(request, "scope_" + StoreApiKeys.SCOPES.get(iter))) {
                scopes.add(StoreApiKeys.SCOPES.get(iter));
            }
        }
        return change(request, "/admin/" + storeId + "/security", null, () -> {
            ApiKeyDto made = keys.create(Caller.name(), storeId, new ApiKeyDto(null,
                    f(request, "name"), scopes, null, null, 0, 0, 0, null));
            // Shown this once; only its hash is kept.
            WebPages.toast(request, "Key created. Copy it now, it is not shown again: "
                    + made.secret());
        });
    }

    @PostMapping("/admin/{storeId}/security/api-keys/{id}/revoke")
    public String revokeKey(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/security", "Key revoked",
                () -> keys.revoke(Caller.name(), storeId, id));
    }

    @PostMapping("/admin/{storeId}/security/bulk")
    public String startBulk(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/security", "Job started", () ->
                bulk.startAsStaff(Caller.name(), storeId, new BulkJobDto(null,
                        f(request, "kind"), null, null, 0, 0, null, 0, 0)));
    }

    @GetMapping("/admin/{storeId}/security/bulk/{id}/result.jsonl")
    public HttpServer.Response bulkResult(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Security and API");
        return new HttpServer.Response(200, "application/x-ndjson; charset=utf-8",
                bulk.result(Caller.name(), storeId, id).getBytes("UTF-8"))
                .header("Cache-Control", "no-store")
                .header("Content-Disposition", "attachment; filename=\"bulk-" + id + ".jsonl\"");
    }

    // ================================================================= tax

    @GetMapping("/admin/{storeId}/tax-rules")
    public String taxPage(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Tax rules");
        String me = Caller.name();
        String base = "/admin/" + storeId + "/tax-rules";
        MarketingView v = new MarketingView();
        v.page = "tax-rules";
        v.intro = "EU VAT by product tax category, the US sales-tax provider, and rate "
                + "overrides for a product or a collection.";
        TaxSettingsDto s = tax.settings(me, storeId);
        FormView settings = new FormView("settings", "Tax settings", base + "/settings", "Save");
        settings.check("euVat", "Charge EU VAT from the rate table", s.euVat());
        settings.add("homeCountry", "Home country", "text", s.homeCountry())
                .hint("Two letters, e.g. DE");
        settings.add("usProvider", "US sales-tax provider", "select", s.usProvider())
                .option("", "None (the store's own rates)")
                .option("simulated", "Simulated");
        v.forms.add(settings);

        List<TaxOverrideDto> overrides = tax.overrides(me, storeId);
        v.cardsTitle = "Overrides";
        for (int iter = 0; iter < overrides.size(); iter++) {
            TaxOverrideDto o = overrides.get(iter);
            ActionCard k = new ActionCard();
            k.title = o.targetTitle();
            k.detail = o.country() + (o.region().length() > 0 ? "-" + o.region() : "") + ": "
                    + percent(o.basisPoints());
            k.button("Remove", base + "/overrides/" + o.id() + "/remove", true);
            v.cards.add(k);
        }
        FormView add = new FormView("override", "Add an override", base + "/overrides",
                "Add override");
        add.add("targetKind", "For", "select", TaxRules.PRODUCT)
                .option(TaxRules.PRODUCT, "Product").option(TaxRules.COLLECTION, "Collection");
        add.add("targetId", "Product or collection id", "text", "").required = true;
        add.add("country", "Country", "text", "").required = true;
        add.add("region", "Region", "text", "").hint("Empty for the whole country");
        add.add("percent", "Rate (%)", "text", "").required = true;
        v.forms.add(add);
        v.links.add(link("Download the OSS VAT report (CSV)", base + "/oss.csv"));
        return render(model, request, storeId, "Tax rules", v);
    }

    @PostMapping("/admin/{storeId}/tax-rules/settings")
    public String saveTax(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/tax-rules", "Saved", () ->
                tax.saveSettings(Caller.name(), storeId, new TaxSettingsDto(on(request, "euVat"),
                        f(request, "homeCountry"), f(request, "usProvider"))));
    }

    @PostMapping("/admin/{storeId}/tax-rules/overrides")
    public String addOverride(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/tax-rules", "Override added", () ->
                tax.saveOverride(Caller.name(), storeId, new TaxOverrideDto(null,
                        f(request, "targetKind"), f(request, "targetId"), null,
                        f(request, "country"), f(request, "region"),
                        (int) AdminForms.cents(f(request, "percent"), "Rate"))));
    }

    @PostMapping("/admin/{storeId}/tax-rules/overrides/{id}/remove")
    public String removeOverride(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/tax-rules", "Override removed",
                () -> tax.removeOverride(Caller.name(), storeId, id));
    }

    @GetMapping("/admin/{storeId}/tax-rules/oss.csv")
    public HttpServer.Response ossCsv(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "Tax rules");
        return OssReportCsv.response(TaxRules.ossCsv(tax.ossReport(Caller.name(), storeId, 0,
                Long.MAX_VALUE)));
    }

    private static AdminRow link(String title, String url) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.url = url;
        return r;
    }

    private static String percent(int bp) {
        String frac = String.valueOf(100 + bp % 100).substring(1);
        return bp / 100 + "." + frac + "%";
    }

    private static String join(List<String> items) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; items != null && iter < items.size(); iter++) {
            b.append(iter == 0 ? "" : ", ").append(items.get(iter));
        }
        return b.toString();
    }
}
