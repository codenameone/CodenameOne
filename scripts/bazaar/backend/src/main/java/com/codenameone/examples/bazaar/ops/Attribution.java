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
package com.codenameone.examples.bazaar.ops;

import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.cart.Codes;
import java.io.IOException;
import java.util.Map;

/// Where a shopper came from: the first and the last touch, each a source,
/// medium and campaign, carried from the storefront landing to the cart and
/// on to the order.
///
/// A landing page with `utm_source` (and optionally `utm_medium`,
/// `utm_campaign`) is a touch; so is a `Referer` naming another host, as
/// source = that host and medium `referral`. The touch is held in the
/// session -- one is made only when there is a touch, so a plain visit
/// stays cacheable -- the first one kept, the last one replaced. Adding to
/// the cart stamps the cart; placing the order copies the cart's stamp onto
/// the order, where the reports read it.
@Component
public class Attribution {
    static final String FIRST = "bz.touch.first.";
    static final String LAST = "bz.touch.last.";

    private final DataSource db;

    public Attribution(DataSource db) {
        this.db = db;
    }

    private static String param(HttpServer.Request request, String name) {
        try {
            String v = request.param(name);
            return v == null ? "" : clean(v);
        } catch (RuntimeException malformed) {
            return "";
        }
    }

    /// A touch value: printable ASCII, no tabs, at most 100 characters.
    static String clean(String v) {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0; iter < v.length() && sb.length() < 100; iter++) {
            char c = v.charAt(iter);
            if (c > ' ' && c < 127) {
                sb.append(c);
            } else if (c == ' ' && sb.length() > 0) {
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /// The host of an absolute http(s) URL, or "".
    static String host(String url) {
        if (url == null) {
            return "";
        }
        int s = url.indexOf("://");
        if (s < 0 || s > 5) {
            return "";
        }
        int end = s + 3;
        while (end < url.length() && url.charAt(end) != '/' && url.charAt(end) != '?'
                && url.charAt(end) != '#') {
            end++;
        }
        return clean(url.substring(s + 3, end));
    }

    /// The touch a request carries as `source \t medium \t campaign`, or null.
    static String touch(String utmSource, String utmMedium, String utmCampaign, String referer,
            String ownHost) {
        if (utmSource.length() > 0) {
            return utmSource + "\t" + utmMedium + "\t" + utmCampaign;
        }
        String h = host(referer);
        if (h.length() > 0 && !h.equalsIgnoreCase(ownHost == null ? "" : ownHost)) {
            return h + "\treferral\t";
        }
        return null;
    }

    /// Records the landing touch of a storefront page view for store `handle`.
    public void capture(HttpServer.Request request, String handle) {
        String t = touch(param(request, "utm_source"), param(request, "utm_medium"),
                param(request, "utm_campaign"), request.getHeader("Referer"),
                request.getHeader("Host"));
        if (t == null || handle == null) {
            return;
        }
        HttpSession session = request.getSession(true);
        if (session == null) {
            return;
        }
        if (!(session.getAttribute(FIRST + handle) instanceof String)) {
            session.setAttribute(FIRST + handle, t);
        }
        session.setAttribute(LAST + handle, t);
    }

    /// Stamps cart `cartId` with the touches the session holds for `handle`.
    public void stampCart(HttpServer.Request request, String handle, String storeId, String cartId)
            throws IOException {
        HttpSession session = request.getSession(false);
        Object first = session == null ? null : session.getAttribute(FIRST + handle);
        Object last = session == null ? null : session.getAttribute(LAST + handle);
        if (!(first instanceof String) || !(last instanceof String) || cartId == null) {
            return;
        }
        save(storeId, "CART", cartId, (String) first, (String) last);
    }

    /// Copies a cart's stamp onto the order placed from it.
    public void stampOrder(String storeId, String cartId, String orderId) throws IOException {
        Map r = db.queryOne("SELECT first_source, first_medium, first_campaign, last_source, "
                + "last_medium, last_campaign FROM attribution WHERE subject_kind = 'CART' AND "
                + "subject_id = ? AND store_id = ?", new Object[] {cartId, storeId});
        if (r == null) {
            return;
        }
        save(storeId, "ORDER", orderId, Rows.text(r.get("first_source")) + "\t"
                + Rows.text(r.get("first_medium")) + "\t" + Rows.text(r.get("first_campaign")),
                Rows.text(r.get("last_source")) + "\t" + Rows.text(r.get("last_medium")) + "\t"
                + Rows.text(r.get("last_campaign")));
    }

    private static String part(String t, int index) {
        int start = 0;
        for (int iter = 0; iter < index; iter++) {
            int tab = t.indexOf('\t', start);
            if (tab < 0) {
                return "";
            }
            start = tab + 1;
        }
        int end = t.indexOf('\t', start);
        return Codes.cut(t.substring(start, end < 0 ? t.length() : end), 100);
    }

    void save(String storeId, String kind, String subjectId, String first, String last)
            throws IOException {
        int n = db.execute("UPDATE attribution SET last_source = ?, last_medium = ?, last_campaign = ? "
                + "WHERE subject_kind = ? AND subject_id = ? AND store_id = ?", new Object[] {
                    part(last, 0), part(last, 1), part(last, 2), kind, subjectId, storeId});
        if (n == 0) {
            db.execute("INSERT INTO attribution (id, store_id, subject_kind, subject_id, first_source, "
                    + "first_medium, first_campaign, last_source, last_medium, last_campaign, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId, kind,
                        subjectId, part(first, 0), part(first, 1), part(first, 2), part(last, 0),
                        part(last, 1), part(last, 2), Long.valueOf(System.currentTimeMillis())});
        }
    }
}
