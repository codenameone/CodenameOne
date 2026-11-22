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
package com.codenameone.examples.bazaar.order;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.CardDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PayLinkDto;
import com.codenameone.examples.bazaar.api.PayLinkRequestDto;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.io.IOException;
import java.util.Map;

/// Checkout links: a draft order or an unpaid invoice sent to its buyer as a
/// URL that pays it on the storefront, with no account needed.
///
/// The token is `id.expiry.signature`, the signature an HMAC of both under
/// the store's secret, so a link cannot be forged or have its expiry moved.
/// The `pay_link` row is what lets a link be replaced (a new link for the
/// same order retires the old one) and marked used once the order is paid.
@Component
public class PayLinks {
    public static final String ACTIVE = "ACTIVE";
    public static final String REPLACED = "REPLACED";
    public static final String PAID = "PAID";
    static final int DEFAULT_DAYS = 7;
    static final int MAX_DAYS = 60;
    private static final long DAY = 86400000L;

    private final DataSource db;
    private final StoreAccess access;
    private final Orders orders;
    private final StoreOrderRepository orderRows;
    private final Campaigns campaigns;
    private final Emails emails;
    private final Payments payments;
    private final SiteUrls urls;

    public PayLinks(DataSource db, StoreAccess access, Orders orders,
            StoreOrderRepository orderRows, Campaigns campaigns, Emails emails,
            Payments payments, SiteUrls urls) {
        this.db = db;
        this.access = access;
        this.orders = orders;
        this.orderRows = orderRows;
        this.campaigns = campaigns;
        this.emails = emails;
        this.payments = payments;
        this.urls = urls;
    }

    /// Makes a link for a draft or an open order awaiting payment, retires
    /// any earlier one, and mails it to the order's address when it has one.
    public PayLinkDto create(String username, String storeId, String orderId,
            PayLinkRequestDto req) throws IOException {
        access.requireArea(username, storeId, Perms.ORDERS, true, StoreAccess.MANAGERS);
        StoreOrder order = orderId == null || orderId.length() > 32 ? null
                : orderRows.findById(orderId);
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        if (!payable(order)) {
            throw new ResponseStatusException(409, "Only a draft or an unpaid order gets a "
                    + "checkout link");
        }
        int days = req == null || req.days() <= 0 ? DEFAULT_DAYS : req.days();
        if (days > MAX_DAYS) {
            throw new ResponseStatusException(400, "A link lasts at most " + MAX_DAYS + " days");
        }
        String[] made = insert(order, days, username);
        String url = urls.absolute(null, path(made[1]));
        boolean emailed = order.getEmail().length() > 0
                && emails.payLink(orderId, url, Long.parseLong(made[2]));
        if (emailed) {
            orders.event(orderId, "PAY_LINK_SENT", "Checkout link sent to " + order.getEmail(),
                    username);
        }
        return new PayLinkDto(orderId, url, Long.parseLong(made[2]), emailed);
    }

    /// `{id, token, expiresAt}` of a fresh link; the order's older ones retired.
    @Transactional(rollbackFor = IOException.class)
    private String[] insert(StoreOrder order, int days, String username) throws IOException {
        long now = System.currentTimeMillis();
        long expires = now + days * DAY;
        String id = Ids.next();
        String token = id + "." + expires + "." + Tokens.sign(campaigns.secret(order.getStoreId()),
                text(id, expires));
        db.execute("UPDATE pay_link SET status = ? WHERE order_id = ? AND status = ?",
                new Object[] {REPLACED, order.getId(), ACTIVE});
        db.execute("INSERT INTO pay_link (id, store_id, order_id, expires_at, status, token, "
                + "created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {id,
                    order.getStoreId(), order.getId(), Long.valueOf(expires), ACTIVE, token,
                    username, Long.valueOf(now)});
        return new String[] {id, token, Long.toString(expires)};
    }

    /// The storefront path a token opens.
    public static String path(String token) {
        return "/pay/" + token;
    }

    /// The order a token pays, or 404 when the token is forged, expired,
    /// replaced or used, or its order no longer awaits payment. One answer
    /// for all of them, so a guess learns nothing.
    @Transactional(readOnly = true)
    public StoreOrder resolve(String token) throws IOException {
        Map row = row(token);
        StoreOrder order = row == null ? null : orderRows.findById(Rows.text(row.get("order_id")));
        if (order == null || !payable(order)) {
            throw new ResponseStatusException(404, "This link has expired or was already used");
        }
        return order;
    }

    /// Pays the order a token opens with a typed card. `payer` is the buyer's
    /// account when signed in, else the order's own.
    public OrderDto pay(String token, String payer, CardDto card) throws IOException {
        Map row = row(token);
        StoreOrder order = resolve(token);
        String who = payer == null || payer.length() == 0 ? order.getUsername() : payer;
        if (who == null || who.length() == 0) {
            who = "guest-" + order.getId();
        }
        Payments.OneOffCard checked = payments.checkCard(who, card);
        if (db.execute("UPDATE pay_link SET status = ? WHERE id = ? AND status = ?",
                new Object[] {PAID, Rows.text(row.get("id")), ACTIVE}) != 1) {
            throw new ResponseStatusException(404, "This link has expired or was already used");
        }
        try {
            return orders.payOnline(order.getId(), who, checked);
        } catch (IOException | RuntimeException failed) {
            db.execute("UPDATE pay_link SET status = ? WHERE id = ? AND status = ?",
                    new Object[] {ACTIVE, Rows.text(row.get("id")), PAID});
            throw failed;
        }
    }

    /// The active link of an order, for the invoice PDF; null when it has none.
    @Transactional(readOnly = true)
    public String activeUrl(String orderId) throws IOException {
        Map r = db.queryOne("SELECT token FROM pay_link WHERE order_id = ? AND status = ? AND "
                + "expires_at > ?", new Object[] {orderId, ACTIVE,
                    Long.valueOf(System.currentTimeMillis())});
        return r == null ? null : urls.absolute(null, path(Rows.text(r.get("token"))));
    }

    /// The link's row when the token is well formed, signed, unexpired and
    /// still active; null otherwise.
    private Map row(String token) throws IOException {
        if (token == null || token.length() > 200) {
            return null;
        }
        int a = token.indexOf('.');
        int b = a < 0 ? -1 : token.indexOf('.', a + 1);
        if (a <= 0 || b <= a + 1 || b == token.length() - 1) {
            return null;
        }
        String id = token.substring(0, a);
        long expires = digits(token.substring(a + 1, b));
        if (expires <= System.currentTimeMillis()) {
            return null;
        }
        Map r = db.queryOne("SELECT id, store_id, order_id, status, expires_at FROM pay_link "
                + "WHERE id = ?", new Object[] {id});
        if (r == null || !ACTIVE.equals(Rows.text(r.get("status")))
                || Rows.number(r.get("expires_at")) != expires) {
            return null;
        }
        if (!Tokens.verify(campaigns.secret(Rows.text(r.get("store_id"))), text(id, expires),
                token.substring(b + 1))) {
            return null;
        }
        return r;
    }

    private static String text(String id, long expires) {
        return "pay:" + id + ":" + expires;
    }

    /// A draft not yet turned into an order, or an open one awaiting payment.
    static boolean payable(StoreOrder o) {
        if (!Orders.OPEN.equals(o.getStatus())) {
            return false;
        }
        return Orders.isUnconvertedDraft(o) || Orders.PENDING.equals(o.getFinancial());
    }

    /// The value of a run of decimal digits; -1 for anything else.
    private static long digits(String s) {
        if (s.length() == 0 || s.length() > 18) {
            return -1;
        }
        long out = 0;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }
}
