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

import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.PageMeta;
import com.codenameone.examples.bazaar.web.security.CspSources;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.security.WebHeaders;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// App slots: third-party snippets an owner places at named places of the HTML
/// storefront, and what a page does with them.
///
/// **Editing** is the owner's alone ([StoreAccess#OWNER]): a snippet runs on
/// every visitor's page, so it is as powerful as the store itself. Every
/// create, update and delete writes an [AuditEvent] and bumps the store's
/// slot version in the same transaction.
///
/// **Rendering** ([#decorate]) widens the page's content security policy by
/// exactly the hosts of the snippets that page prints -- never
/// `'unsafe-inline'`, never a wildcard ([CspSources#clean] refuses both) -- and
/// mixes the slot version into the page's ETag, so a cached copy is not
/// revalidated against a policy it was not served with. A widget's HTML never
/// runs in the storefront's origin: it is framed from `/w/{id}`
/// ([SlotPages#widget]) with `sandbox="allow-scripts"`, and that route's own
/// policy carries the same sandbox for a visitor who opens it directly.
///
/// **Consent**: a necessary snippet always loads; an analytics or marketing one
/// only once the visitor accepted that category ([Consent]). The server leaves
/// the rest out of the page entirely, so no script decides it. A page that
/// differs by the consent cookie says `Vary: Cookie`, and a visitor who
/// accepted anything gets a private copy, never the shared one.
///
/// **Where nothing optional loads**: the checkout gets necessary snippets
/// only, and the account area (which has no store) gets none.
@Component
public class Slots {
    /// The places a snippet may go, in the order the editor lists them.
    public static final String[][] PLACES = {
        {"head", "Page head"},
        {"body-end", "End of every page"},
        {"product-below-price", "Product page, below the price"},
        {"cart-below-total", "Cart, below the total"},
        {"checkout-thank-you", "Order confirmation"},
    };
    /// The consent categories, in the order the editor lists them.
    public static final String[][] CATEGORIES = {
        {Consent.NECESSARY, "Necessary (always loads)"},
        {Consent.ANALYTICS, "Analytics (after consent)"},
        {Consent.MARKETING, "Marketing (after consent)"},
    };
    /// The exchange attribute set on a page whose bytes depend on the consent
    /// cookie and whose visitor answered: it must not be cached as shared.
    public static final String PERSONAL = "bazaar.slotsPersonal";

    static final int MAX_URL = 500;
    static final int MAX_HTML = 20000;
    static final int MAX_NAME = 100;
    static final int MAX_ORIGINS = 8;

    private final DataSource db;
    private final StoreAccess access;
    private final AuditEventRepository audits;

    public Slots(DataSource db, StoreAccess access, AuditEventRepository audits) {
        this.db = db;
        this.access = access;
        this.audits = audits;
    }

    // -------------------------------------------------------------- editing

    /// Every slot of `storeId`, for its owner.
    public List<AppSlot> list(String me, String storeId) throws IOException {
        access.requireStaff(me, storeId, StoreAccess.OWNER);
        return rows(storeId);
    }

    /// One slot of `storeId`, for its owner; 404 for an id of another store.
    public AppSlot get(String me, String storeId, String id) throws IOException {
        access.requireStaff(me, storeId, StoreAccess.OWNER);
        AppSlot s = byId(id);
        StoreAccess.requireIn(s == null ? null : s.storeId, storeId, "app");
        return s;
    }

    /// Creates (`id` null) or replaces a slot from what the editor posted;
    /// answers its id. A value that does not pass is a 400 and nothing is
    /// written.
    @Transactional
    public String save(String me, String storeId, String id, AppSlot wanted) throws IOException {
        access.requireStaff(me, storeId, StoreAccess.OWNER);
        AppSlot clean = validate(wanted);
        long now = System.currentTimeMillis();
        String action;
        if (id == null) {
            clean.id = Ids.next();
            db.execute("INSERT INTO app_slot (id, store_id, slot, name, script_url, widget_html, "
                    + "origins, category, enabled, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[] {clean.id, storeId, clean.slot, clean.name, clean.scriptUrl,
                        clean.widgetHtml, clean.origins, clean.category,
                        Integer.valueOf(clean.enabled ? 1 : 0), Long.valueOf(now),
                        Long.valueOf(now)});
            action = "APP_SLOT_CREATED";
        } else {
            AppSlot had = byId(id);
            StoreAccess.requireIn(had == null ? null : had.storeId, storeId, "app");
            clean.id = id;
            db.execute("UPDATE app_slot SET slot = ?, name = ?, script_url = ?, widget_html = ?, "
                    + "origins = ?, category = ?, enabled = ?, updated_at = ? "
                    + "WHERE id = ? AND store_id = ?",
                    new Object[] {clean.slot, clean.name, clean.scriptUrl, clean.widgetHtml,
                        clean.origins, clean.category, Integer.valueOf(clean.enabled ? 1 : 0),
                        Long.valueOf(now), id, storeId});
            action = "APP_SLOT_UPDATED";
        }
        bump(storeId);
        audit(me, storeId, action, clean);
        return clean.id;
    }

    /// Removes a slot of `storeId`.
    @Transactional
    public void delete(String me, String storeId, String id) throws IOException {
        access.requireStaff(me, storeId, StoreAccess.OWNER);
        AppSlot had = byId(id);
        StoreAccess.requireIn(had == null ? null : had.storeId, storeId, "app");
        db.execute("DELETE FROM app_slot WHERE id = ? AND store_id = ?",
                new Object[] {id, storeId});
        bump(storeId);
        audit(me, storeId, "APP_SLOT_DELETED", had);
    }

    private void audit(String me, String storeId, String action, AppSlot s) throws IOException {
        AuditEvent e = new AuditEvent();
        e.setId(Ids.next());
        e.setStoreId(storeId);
        e.setUsername(me);
        e.setAction(action);
        e.setSubject(s.id);
        String detail = s.name + " at " + s.slot + " (" + s.category
                + (s.enabled ? "" : ", disabled") + ")"
                + (s.scriptUrl.length() > 0 ? " script " + s.scriptUrl : "")
                + (s.origins.length() > 0 ? " origins " + s.origins : "");
        e.setDetail(detail.length() > 500 ? detail.substring(0, 500) : detail);
        e.setCreatedAt(Instant.now());
        audits.insert(e);
    }

    private void bump(String storeId) throws IOException {
        int n = db.execute("UPDATE app_slot_version SET version = version + 1 WHERE store_id = ?",
                new Object[] {storeId});
        if (n == 0) {
            db.execute("INSERT INTO app_slot_version (store_id, version) VALUES (?, 1)",
                    new Object[] {storeId});
        }
    }

    /// The store's slot version: 0 before its first change.
    long version(String storeId) throws IOException {
        Map row = db.queryOne("SELECT version FROM app_slot_version WHERE store_id = ?",
                new Object[] {storeId});
        return row == null ? 0L : Rows.number(row.get("version"));
    }

    /// The posted slot made safe, or a 400 naming what is wrong.
    static AppSlot validate(AppSlot in) {
        AppSlot out = new AppSlot();
        out.name = trim(in.name);
        if (out.name.length() == 0 || out.name.length() > MAX_NAME) {
            throw bad("Give the app a name of up to " + MAX_NAME + " characters");
        }
        out.slot = trim(in.slot);
        if (!isPlace(out.slot)) {
            throw bad("Choose where the app goes");
        }
        out.category = trim(in.category);
        if (!Consent.isCategory(out.category)) {
            throw bad("Choose a consent category");
        }
        out.scriptUrl = trim(in.scriptUrl);
        if (out.scriptUrl.length() > 0 && scriptOrigin(out.scriptUrl) == null) {
            throw bad("The script address must be a plain https:// address");
        }
        out.widgetHtml = in.widgetHtml == null ? "" : in.widgetHtml;
        if (out.widgetHtml.trim().length() == 0) {
            out.widgetHtml = "";
        }
        if (out.widgetHtml.length() > MAX_HTML) {
            throw bad("The widget HTML is longer than " + MAX_HTML + " characters");
        }
        if (out.scriptUrl.length() == 0 && out.widgetHtml.length() == 0) {
            throw bad("Give a script address, widget HTML, or both");
        }
        out.origins = origins(in.origins);
        out.enabled = in.enabled;
        return out;
    }

    /// The allowed origins typed (separated by spaces, commas or new lines),
    /// each cleaned, joined by one space; a 400 for any that is not an https
    /// origin -- an owner who typed a host expects it to work, so nothing is
    /// dropped quietly.
    static String origins(String typed) {
        if (typed == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int count = 0;
        int start = 0;
        int n = typed.length();
        while (start < n) {
            while (start < n && separator(typed.charAt(start))) {
                start++;
            }
            int end = start;
            while (end < n && !separator(typed.charAt(end))) {
                end++;
            }
            if (end > start) {
                String one = CspSources.clean(typed.substring(start, end));
                if (one == null) {
                    throw bad("Allowed origins must be https://host addresses, without a path: "
                            + typed.substring(start, Math.min(end, start + 80)));
                }
                if (++count > MAX_ORIGINS) {
                    throw bad("At most " + MAX_ORIGINS + " allowed origins");
                }
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(one);
            }
            start = end;
        }
        return out.toString();
    }

    private static boolean separator(char c) {
        return c == ' ' || c == ',' || c == '\n' || c == '\r' || c == '\t';
    }

    /// The `https://host[:port]` a script address loads from, or null when the
    /// address is not a plain https URL: no user info, no whitespace, quotes,
    /// angle brackets, backslashes or control characters, at most
    /// [#MAX_URL] characters.
    static String scriptOrigin(String url) {
        if (url == null || url.length() > MAX_URL || !url.regionMatches(true, 0, "https://", 0, 8)) {
            return null;
        }
        for (int iter = 0; iter < url.length(); iter++) {
            char c = url.charAt(iter);
            if (c <= ' ' || c >= 127 || c == '"' || c == '\'' || c == '<' || c == '>'
                    || c == '\\' || c == '`') {
                return null;
            }
        }
        int end = 8;
        while (end < url.length() && url.charAt(end) != '/' && url.charAt(end) != '?'
                && url.charAt(end) != '#') {
            end++;
        }
        String authority = url.substring(8, end);
        if (authority.indexOf('@') >= 0) {
            return null;
        }
        return CspSources.clean("https://" + authority);
    }

    static boolean isPlace(String slot) {
        for (int iter = 0; iter < PLACES.length; iter++) {
            if (PLACES[iter][0].equals(slot)) {
                return true;
            }
        }
        return false;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static ResponseStatusException bad(String why) {
        return new ResponseStatusException(400, why);
    }

    // ------------------------------------------------------------ rendering

    /// The enabled widget `id` (HTML set), for the sandbox route; null for
    /// anything else.
    AppSlot widget(String id) throws IOException {
        AppSlot s = byId(id);
        return s == null || !s.enabled || s.widgetHtml.length() == 0 ? null : s;
    }

    /// The enabled slots of `storeId`.
    List<AppSlot> enabled(String storeId) throws IOException {
        List<AppSlot> all = rows(storeId);
        List<AppSlot> out = new ArrayList<AppSlot>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).enabled) {
                out.add(all.get(iter));
            }
        }
        return out;
    }

    /// Fills `page` with what `store`'s slots put on the page being served,
    /// widens its policy by their hosts, and marks how it may be cached. Which
    /// page it is comes from the request's path: `/checkout` is necessary
    /// only, an order's page adds the confirmation place, anything else is the
    /// storefront.
    public void decorate(PageMeta page, Store store, HttpServer.Request request)
            throws IOException {
        List<AppSlot> all = enabled(store.getId());
        page.privacyUrl = "/s/" + store.getHandle() + "/privacy";
        if (all.isEmpty()) {
            return;
        }
        String path = request.getTarget();
        int q = path.indexOf('?');
        page.consentBack = q < 0 ? path : path.substring(0, q);
        if (q >= 0) {
            path = path.substring(0, q);
        }
        boolean checkout = path.indexOf("/checkout") >= 0;
        boolean order = !checkout && path.indexOf("/orders/") >= 0;
        Consent consent = Consent.of(request);
        boolean gated = false;
        CspSources sources = new CspSources();
        for (int iter = 0; iter < all.size(); iter++) {
            AppSlot s = all.get(iter);
            boolean necessary = Consent.NECESSARY.equals(s.category);
            if (!necessary) {
                gated = true;
            }
            if (checkout && !necessary) {
                continue;
            }
            if (!consent.allows(s.category)) {
                continue;
            }
            List<SlotView> target = place(page, s.slot, order);
            if (target == null) {
                continue;
            }
            if (s.scriptUrl.length() > 0) {
                SlotView v = new SlotView();
                v.id = s.id;
                v.kind = "script";
                v.script = true;
                v.src = s.scriptUrl;
                v.title = s.name;
                v.head = "head".equals(s.slot);
                target.add(v);
                sources.add(CspSources.SCRIPT, scriptOrigin(s.scriptUrl));
                allowAll(sources, s.origins);
            }
            if (s.widgetHtml.length() > 0) {
                SlotView v = new SlotView();
                v.id = s.id;
                v.kind = "html";
                v.frame = true;
                v.src = "/w/" + s.id;
                v.title = s.name;
                // A frame cannot sit in the head: a head widget goes to the end.
                ("head".equals(s.slot) ? page.bodySlots : target).add(v);
            }
        }
        if (!sources.isEmpty()) {
            WebHeaders.allow(sources);
        }
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange != null) {
            exchange.setAttribute(PageEtags.SALT, "slots:" + version(store.getId()) + ":"
                    + consent.value() + (consent.answered ? "" : "?"));
            if (gated) {
                exchange.setResponseHeader("Vary", "Cookie");
                if (consent.answered) {
                    exchange.setAttribute(PERSONAL, "1");
                }
            }
        }
        page.consentAsk = gated && !consent.answered && !checkout;
    }

    private static void allowAll(CspSources sources, String origins) {
        int start = 0;
        while (start < origins.length()) {
            int sp = origins.indexOf(' ', start);
            int end = sp < 0 ? origins.length() : sp;
            String one = origins.substring(start, end);
            sources.add(CspSources.SCRIPT, one);
            sources.add(CspSources.CONNECT, one);
            sources.add(CspSources.IMG, one);
            start = end + 1;
        }
    }

    /// The list of `page` that `slot` prints into on this page, or null when
    /// the place is not on it.
    private static List<SlotView> place(PageMeta page, String slot, boolean order) {
        if ("head".equals(slot)) {
            return page.headSlots;
        }
        if ("body-end".equals(slot)) {
            return page.bodySlots;
        }
        if ("product-below-price".equals(slot)) {
            return page.priceSlots;
        }
        if ("cart-below-total".equals(slot)) {
            return page.cartSlots;
        }
        if ("checkout-thank-you".equals(slot)) {
            return order ? page.thanksSlots : null;
        }
        return null;
    }

    // ------------------------------------------------------------------ rows

    private List<AppSlot> rows(String storeId) throws IOException {
        List found = db.query("SELECT id, store_id, slot, name, script_url, widget_html, origins, "
                + "category, enabled, created_at, updated_at FROM app_slot WHERE store_id = ? "
                + "ORDER BY created_at", new Object[] {storeId});
        List<AppSlot> out = new ArrayList<AppSlot>();
        for (int iter = 0; iter < found.size(); iter++) {
            out.add(read((Map) found.get(iter)));
        }
        return out;
    }

    private AppSlot byId(String id) throws IOException {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        Map row = db.queryOne("SELECT id, store_id, slot, name, script_url, widget_html, origins, "
                + "category, enabled, created_at, updated_at FROM app_slot WHERE id = ?",
                new Object[] {id});
        return row == null ? null : read(row);
    }

    private static AppSlot read(Map r) {
        AppSlot s = new AppSlot();
        s.id = Rows.text(r.get("id"));
        s.storeId = Rows.text(r.get("store_id"));
        s.slot = Rows.text(r.get("slot"));
        s.name = Rows.text(r.get("name"));
        s.scriptUrl = Rows.text(r.get("script_url"));
        s.widgetHtml = Rows.text(r.get("widget_html"));
        s.origins = Rows.text(r.get("origins"));
        s.category = Rows.text(r.get("category"));
        s.enabled = Rows.number(r.get("enabled")) != 0;
        s.createdAt = Rows.number(r.get("created_at"));
        s.updatedAt = Rows.number(r.get("updated_at"));
        return s;
    }

    /// The privacy page's model for `store`.
    public PrivacyView privacy(Store store, HttpServer.Request request) throws IOException {
        PrivacyView v = new PrivacyView();
        v.policies = store.getPolicies();
        List<AppSlot> on = enabled(store.getId());
        for (int iter = 0; iter < on.size(); iter++) {
            SlotView a = new SlotView();
            a.title = on.get(iter).name;
            a.kind = on.get(iter).category;
            v.apps.add(a);
        }
        Consent c = Consent.of(request);
        if (c.answered) {
            v.analytics = c.analytics;
            v.marketing = c.marketing;
            v.current = c.analytics || c.marketing
                    ? "You accepted: " + (c.analytics ? "analytics " : "")
                            + (c.marketing ? "marketing" : "")
                    : "You declined optional cookies.";
            SecurityExchange exchange = SecurityExchange.current();
            if (exchange != null) {
                exchange.setAttribute(Slots.PERSONAL, "1");
            }
        }
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange != null) {
            exchange.setResponseHeader("Vary", "Cookie");
        }
        return v;
    }
}
