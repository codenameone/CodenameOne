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
package com.codenameone.examples.bazaar.web.slots;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.admin.AdminRow;
import com.codenameone.examples.bazaar.web.admin.AdminView;
import com.codenameone.examples.bazaar.web.security.CspSources;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;
import com.codenameone.examples.bazaar.web.table.Grid;

import java.io.IOException;
import java.util.List;

/// The routes of app slots ([Slots]):
///
/// - `/admin/{store}/apps`: the owner's list (a [DataTable]) and the editor,
///   create, update and delete as CSRF-checked form posts answered with 303.
///   They are in the signed-in chain, like the rest of the back office.
/// - `/w/{id}`: a widget's HTML, for the sandboxed frame a page prints. Its
///   own chain keeps it stateless (no session is read or made, no cookie is
///   set), and its policy carries `sandbox allow-scripts`, so even opened as
///   a page of its own it runs in an opaque origin with no access to the
///   storefront's cookies or storage.
/// - `POST /consent`: records the visitor's cookie choice and goes back to
///   the page the banner was on. Works with no script at all.
@Controller
public class SlotPages {
    private final Slots slots;
    private final WebPages pages;
    private final WebSessions sessions;
    private final boolean secureCookies;

    public SlotPages(Slots slots, WebPages pages, WebSessions sessions,
            @Value("${bazaar.web.hsts:true}") boolean secureCookies) {
        this.slots = slots;
        this.pages = pages;
        this.sessions = sessions;
        this.secureCookies = secureCookies;
    }

    // ---------------------------------------------------------- back office

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            AdminView view, DataTable table) throws IOException {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        view.base = "/admin/" + storeId;
        model.addAttribute("view", view);
        model.addAttribute("table", table == null ? new DataTable() : table);
        return WebPages.view(model, shell, "admin");
    }

    @GetMapping("/admin/{storeId}/apps")
    public String list(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        List<AppSlot> rows = slots.list(Caller.name(), storeId);
        final String root = "/admin/" + storeId + "/apps/";
        Grid<AppSlot> g = new Grid<AppSlot>("apps")
                .col("name", "Name", s -> s.name).sortBy(s -> s.name).link(s -> root + s.id)
                .col("slot", "Place", s -> label(Slots.PLACES, s.slot)).sortBy(s -> s.slot)
                .col("category", "Consent", s -> s.category).sortBy(s -> s.category)
                .col("kind", "Kind", s -> kind(s))
                .col("status", "Status", s -> s.enabled ? "Enabled" : "Disabled")
                    .pill(s -> s.enabled ? "ok" : "")
                .rows(s -> s.id, s -> root + s.id)
                .defaultSort("name", false);
        DataTable t = g.table(rows, Grid.Query.of(request), "/admin/" + storeId + "/apps");
        t.csvUrl = "";
        t.searchLabel = "Search apps";
        t.empty = "No apps yet.";
        AdminView v = new AdminView();
        v.page = "apps";
        return render(model, request, storeId, "Apps", v, t);
    }

    @GetMapping("/admin/{storeId}/new-app")
    public String create(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        slots.list(Caller.name(), storeId);
        AppSlot s = new AppSlot();
        s.slot = "body-end";
        s.category = Consent.ANALYTICS;
        s.enabled = true;
        return editor(model, request, storeId, s, true);
    }

    @GetMapping("/admin/{storeId}/apps/{id}")
    public String edit(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        AppSlot s = slots.get(Caller.name(), storeId, id);
        return editor(model, request, storeId, s, false);
    }

    private String editor(Model model, HttpServer.Request request, String storeId, AppSlot s,
            boolean fresh) throws IOException {
        AdminView v = new AdminView();
        v.page = "app";
        v.app = s;
        v.appNew = fresh;
        String base = "/admin/" + storeId + "/apps";
        v.appAction = fresh ? base : base + "/" + s.id;
        v.deleteAction = base + "/" + s.id + "/delete";
        options(v.places, Slots.PLACES, s.slot);
        options(v.categories, Slots.CATEGORIES, s.category);
        return render(model, request, storeId, fresh ? "New app" : s.name, v, null);
    }

    @PostMapping("/admin/{storeId}/apps")
    public String insert(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        String me = Caller.name();
        sessions.touch(request, me);
        String base = "/admin/" + storeId + "/apps";
        try {
            String id = slots.save(me, storeId, null, posted(request));
            WebPages.toast(request, "App added");
            return "redirect:" + base + "/" + id;
        } catch (ResponseStatusException e) {
            return refused(request, e, "/admin/" + storeId + "/new-app");
        }
    }

    @PostMapping("/admin/{storeId}/apps/{id}")
    public String update(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        String me = Caller.name();
        sessions.touch(request, me);
        String back = "/admin/" + storeId + "/apps/" + id;
        try {
            slots.save(me, storeId, id, posted(request));
            WebPages.toast(request, "App saved");
            return "redirect:" + back;
        } catch (ResponseStatusException e) {
            return refused(request, e, back);
        }
    }

    @PostMapping("/admin/{storeId}/apps/{id}/delete")
    public String delete(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        String me = Caller.name();
        sessions.touch(request, me);
        slots.delete(me, storeId, id);
        WebPages.toast(request, "App deleted");
        return "redirect:/admin/" + storeId + "/apps";
    }

    /// A value the owner can fix is a message on the page they came from; a
    /// store or app that is not theirs, or a role that may not edit, answers
    /// its status.
    private static String refused(HttpServer.Request request, ResponseStatusException e,
            String back) {
        if (e.getStatus() != 400) {
            throw e;
        }
        WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
        return "redirect:" + back;
    }

    private static AppSlot posted(HttpServer.Request request) {
        AppSlot s = new AppSlot();
        s.name = param(request, "name");
        s.slot = param(request, "slot");
        s.scriptUrl = param(request, "scriptUrl");
        s.widgetHtml = param(request, "widgetHtml");
        s.origins = param(request, "origins");
        s.category = param(request, "category");
        s.enabled = "1".equals(param(request, "enabled"));
        return s;
    }

    private static String param(HttpServer.Request request, String name) {
        String v = request.param(name);
        return v == null ? "" : v;
    }

    private static void options(List<AdminRow> out, String[][] table, String chosen) {
        for (int iter = 0; iter < table.length; iter++) {
            AdminRow r = new AdminRow();
            r.id = table[iter][0];
            r.title = table[iter][1];
            r.flag = table[iter][0].equals(chosen);
            out.add(r);
        }
    }

    private static String label(String[][] table, String key) {
        for (int iter = 0; iter < table.length; iter++) {
            if (table[iter][0].equals(key)) {
                return table[iter][1];
            }
        }
        return key;
    }

    private static String kind(AppSlot s) {
        if (s.scriptUrl.length() > 0 && s.widgetHtml.length() > 0) {
            return "Script and widget";
        }
        return s.scriptUrl.length() > 0 ? "Script" : "Widget";
    }

    // --------------------------------------------------------------- widget

    /// A widget's HTML as a document of its own, for `<iframe sandbox>`.
    @GetMapping("/w/{id}")
    public HttpServer.Response widget(@PathVariable("id") String id) throws IOException {
        AppSlot s = slots.widget(id);
        if (s == null) {
            return new HttpServer.Response(404, "text/plain; charset=utf-8",
                    "Not found".getBytes("UTF-8")).header("Cache-Control", "no-store");
        }
        String doc = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"robots\" content=\"noindex\"></head><body>" + s.widgetHtml
                + "</body></html>";
        return new HttpServer.Response(200, "text/html; charset=utf-8", doc.getBytes("UTF-8"))
                .header("Content-Security-Policy", widgetPolicy(s.origins))
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .header("Cache-Control", "public, max-age=60");
    }

    /// The policy of a widget document. The `sandbox` directive is what makes
    /// it safe: the document runs in an opaque origin whether it is framed
    /// with the `sandbox` attribute or opened directly, so its inline script
    /// (the point of a widget) can reach neither the storefront's cookies nor
    /// its storage nor its pages. Beyond its inline code it may load only from
    /// the hosts its owner allowed, may be framed only by this site's pages,
    /// and may not submit forms or set a `<base>`.
    static String widgetPolicy(String origins) {
        StringBuilder hosts = new StringBuilder();
        int start = 0;
        while (start < origins.length()) {
            int sp = origins.indexOf(' ', start);
            int end = sp < 0 ? origins.length() : sp;
            String one = CspSources.clean(origins.substring(start, end));
            if (one != null) {
                hosts.append(' ').append(one);
            }
            start = end + 1;
        }
        String h = hosts.toString();
        return "sandbox allow-scripts; default-src 'none'; script-src 'unsafe-inline'" + h
                + "; style-src 'unsafe-inline'" + h + "; img-src data:" + h + "; connect-src"
                + (h.length() == 0 ? " 'none'" : h) + "; font-src" + (h.length() == 0 ? " 'none'" : h)
                + "; frame-ancestors 'self'; base-uri 'none'; form-action 'none'";
    }

    // -------------------------------------------------------------- consent

    /// Records the visitor's choice: `choice` is `all`, `none` or `custom`
    /// (with `analytics` and `marketing` checkboxes), and `back` the page to
    /// return to -- a path on this site, or the front page.
    @PostMapping("/consent")
    public HttpServer.Response consent(HttpServer.Request request) {
        com.codenameone.examples.bazaar.web.Storefront.sameSite(request);
        String choice = param(request, "choice");
        boolean a;
        boolean m;
        if ("all".equals(choice)) {
            a = true;
            m = true;
        } else if ("custom".equals(choice)) {
            a = "1".equals(param(request, "analytics"));
            m = "1".equals(param(request, "marketing"));
        } else {
            a = false;
            m = false;
        }
        Consent c = new Consent(true, a, m);
        return HttpServer.Response.text(303, "")
                .header("Location", back(param(request, "back")))
                .header("Set-Cookie", c.setCookie(secureCookies))
                .header("Cache-Control", "no-store");
    }

    /// `back` when it is a path on this site; `/` for anything else, so the
    /// form is not an open redirect.
    static String back(String back) {
        if (back == null || back.length() == 0 || back.length() > 500 || back.charAt(0) != '/'
                || (back.length() > 1 && (back.charAt(1) == '/' || back.charAt(1) == '\\'))) {
            return "/";
        }
        for (int iter = 0; iter < back.length(); iter++) {
            char c = back.charAt(iter);
            if (c <= ' ' || c >= 127 || c == '\\') {
                return "/";
            }
        }
        return back;
    }
}
