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
package com.codenameone.examples.bazaar.risk;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.OrderRiskDto;
import com.codenameone.examples.bazaar.domain.OrderRisk;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CheckoutFailureRepository;
import com.codenameone.examples.bazaar.repository.OrderRiskRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Rules-based fraud scoring of online orders.
///
/// Every order the checkout places is scored once, after its payment is
/// held. The rules add points ([#score]):
///
/// - **High value**: 25 from 500.00 in the store currency, 40 from 2,000.00.
/// - **Velocity**: 25 when the same account placed 3 or more other orders at
///   the store in the last hour.
/// - **Failed attempts**: 15 for one or two refused payments by the account
///   at the store in the last hour, 30 for three or more ([RiskHolds]).
/// - **Address mismatch**: 20 when the account's earlier orders all went to
///   other countries than this one.
/// - **IP country mismatch**: 25 when the country the client connected from
///   differs from the delivery country. The country comes from a header a
///   trusted proxy or CDN sets (`bazaar.risk.country-header`, Cloudflare's
///   `CF-IPCountry` by default); there is no IP database in the server, and
///   with no header -- or no request at all, as for an order placed from a
///   job or a test -- the rule adds nothing.
///
/// 60 points or more is HIGH and the order is HELD: fulfilment refuses it
/// ([RiskHolds#requireReleased]) until a manager approves it or cancels it
/// (the order's own cancel, which voids the hold on the card and releases the
/// stock). From 30 it is MEDIUM, shown but not held. A single rule never
/// holds an order by itself.
@Component
public class Fraud {
    public static final String LOW = "LOW";
    public static final String MEDIUM = "MEDIUM";
    public static final String HIGH = "HIGH";
    public static final String CLEAR = "CLEAR";
    public static final String HELD = "HELD";
    public static final String APPROVED = "APPROVED";
    public static final String CANCELLED = "CANCELLED";
    static final int MEDIUM_FROM = 30;
    static final int HIGH_FROM = 60;
    static final long HOUR = 3600L * 1000L;

    private final OrderRiskRepository risks;
    private final CheckoutFailureRepository failures;
    private final StoreOrderRepository orders;
    private final Orders orderService;
    private final StoreAccess access;
    private final String countryHeader;

    public Fraud(OrderRiskRepository risks, CheckoutFailureRepository failures,
            StoreOrderRepository orders, Orders orderService, StoreAccess access, Config config)
            throws IOException {
        this.risks = risks;
        this.failures = failures;
        this.orders = orders;
        this.orderService = orderService;
        this.access = access;
        this.countryHeader = config.get("bazaar.risk.country-header", "CF-IPCountry");
    }

    /// What the rules found about one order: points and their reasons.
    public static final class Score {
        public int points;
        public final List<String> reasons = new ArrayList<String>();

        void add(int p, String reason) {
            points += p;
            reasons.add(reason);
        }

        public String level() {
            return points >= HIGH_FROM ? HIGH : points >= MEDIUM_FROM ? MEDIUM : LOW;
        }
    }

    /// The rules of the class comment, pure: the order, the account's other
    /// orders at the store, its refused payments in the last hour, and the
    /// client's country (or null).
    public static Score score(StoreOrder order, List<StoreOrder> history, long failedLastHour,
            String clientCountry, long now) {
        Score s = new Score();
        long total = order.getTotalCents();
        if (total >= 200000) {
            s.add(40, "Very high value");
        } else if (total >= 50000) {
            s.add(25, "High value");
        }
        int recent = 0;
        boolean sameCountryBefore = false;
        int earlier = 0;
        for (int iter = 0; iter < history.size(); iter++) {
            StoreOrder o = history.get(iter);
            if (o.getId().equals(order.getId())) {
                continue;
            }
            if (now - o.getCreatedAt().toEpochMilli() < HOUR) {
                recent++;
            }
            if (o.getShipCountry() != null && o.getShipCountry().length() == 2) {
                earlier++;
                sameCountryBefore |= o.getShipCountry().equalsIgnoreCase(order.getShipCountry());
            }
        }
        if (recent >= 3) {
            s.add(25, recent + " orders in the last hour");
        }
        if (failedLastHour >= 3) {
            s.add(30, failedLastHour + " refused payments in the last hour");
        } else if (failedLastHour > 0) {
            s.add(15, failedLastHour + " refused payment" + (failedLastHour == 1 ? "" : "s")
                    + " in the last hour");
        }
        if (earlier > 0 && !sameCountryBefore && order.getShipCountry() != null
                && order.getShipCountry().length() == 2) {
            s.add(20, "Ships to a country this customer never used");
        }
        if (clientCountry != null && clientCountry.length() == 2
                && order.getShipCountry() != null && order.getShipCountry().length() == 2
                && !clientCountry.equalsIgnoreCase(order.getShipCountry())) {
            s.add(25, "Connected from " + clientCountry + ", ships to "
                    + order.getShipCountry());
        }
        return s;
    }

    /// The client's country from the trusted header of the request this
    /// thread serves, or null.
    String clientCountry() {
        SecurityExchange ex = SecurityExchange.current();
        HttpServer.Request request = ex == null ? null : ex.getRequest();
        if (request == null) {
            return null;
        }
        String c = request.getHeader(countryHeader);
        if (c == null || c.trim().length() != 2) {
            return null;
        }
        c = c.trim();
        // XX and T1 are what Cloudflare sends for unknown and Tor.
        return "XX".equalsIgnoreCase(c) || "T1".equalsIgnoreCase(c) ? null : c;
    }

    /// Scores a just-placed order and records the result; a HIGH one is held.
    @Transactional(rollbackFor = IOException.class)
    public OrderRisk assess(String orderId, String username) throws IOException {
        StoreOrder order = orders.findById(orderId);
        if (order == null || risks.findByOrderId(orderId) != null) {
            return null;
        }
        long now = System.currentTimeMillis();
        List<StoreOrder> history = new ArrayList<StoreOrder>();
        if (username != null) {
            List<StoreOrder> mine = orders.findByUsernameOrderByCreatedAtDesc(username);
            for (int iter = 0; iter < mine.size() && history.size() < 200; iter++) {
                if (order.getStoreId().equals(mine.get(iter).getStoreId())) {
                    history.add(mine.get(iter));
                }
            }
        }
        long failed = username == null ? 0 : failures
                .countByStoreIdAndUsernameAndCreatedAtGreaterThanEqual(order.getStoreId(),
                        username, Instant.ofEpochMilli(now - HOUR));
        Score s = score(order, history, failed, clientCountry(), now);
        OrderRisk r = new OrderRisk();
        r.setId(Ids.next());
        r.setStoreId(order.getStoreId());
        r.setOrderId(orderId);
        r.setScore(s.points);
        r.setLevel(s.level());
        r.setReasons(join(s.reasons));
        r.setStatus(HIGH.equals(s.level()) ? HELD : CLEAR);
        r.setCreatedAt(Instant.ofEpochMilli(now));
        r.setUpdatedAt(r.getCreatedAt());
        risks.insert(r);
        if (HELD.equals(r.getStatus())) {
            orderService.event(orderId, "RISK_HELD", "Held for a fraud review (score "
                    + s.points + ")", "risk");
        }
        return r;
    }

    static String join(List<String> reasons) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < reasons.size(); iter++) {
            String r = reasons.get(iter);
            if (out.length() + r.length() + 1 > 1000) {
                break;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(r);
        }
        return out.toString();
    }

    static List<String> lines(String text) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (start < text.length()) {
            int nl = text.indexOf('\n', start);
            int end = nl < 0 ? text.length() : nl;
            if (end > start) {
                out.add(text.substring(start, end));
            }
            start = end + 1;
        }
        return out;
    }

    /// Every scored order of the store, newest first; `heldOnly` keeps the
    /// ones waiting for a review.
    @Transactional(readOnly = true)
    public List<OrderRiskDto> list(String username, String storeId, boolean heldOnly)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<OrderRisk> rows = risks.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<OrderRiskDto> out = new ArrayList<OrderRiskDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderRisk r = rows.get(iter);
            if (!heldOnly || HELD.equals(r.getStatus())) {
                out.add(dto(r, orders.findById(r.getOrderId())));
            }
        }
        return out;
    }

    static OrderRiskDto dto(OrderRisk r, StoreOrder o) {
        return new OrderRiskDto(r.getOrderId(), o == null ? 0 : o.getNumber(),
                o == null ? 0 : o.getTotalCents(), r.getScore(), r.getLevel(),
                lines(r.getReasons()), r.getStatus(), r.getCreatedAt().toEpochMilli());
    }

    @Transactional(readOnly = true)
    public OrderRiskDto detail(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        OrderRisk r = risk(storeId, orderId);
        return dto(r, orders.findById(orderId));
    }

    private OrderRisk risk(String storeId, String orderId) throws IOException {
        OrderRisk r = orderId == null ? null : risks.findByOrderId(orderId);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No risk assessment for that order");
        }
        return r;
    }

    /// Releases a held order for fulfilment.
    public OrderRiskDto approve(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        OrderRisk r = risk(storeId, orderId);
        if (risks.move(r.getId(), HELD, APPROVED, username, Instant.now()) != 1) {
            throw new ResponseStatusException(409, "The order is not waiting for a review");
        }
        orderService.event(orderId, "RISK_APPROVED", "Fraud review: approved", username);
        return detail(username, storeId, orderId);
    }

    /// Cancels a held order: the order's own cancel, then the review closed.
    public OrderRiskDto cancel(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        OrderRisk r = risk(storeId, orderId);
        if (!HELD.equals(r.getStatus())) {
            throw new ResponseStatusException(409, "The order is not waiting for a review");
        }
        orderService.cancel(username, storeId, orderId);
        risks.move(r.getId(), HELD, CANCELLED, username, Instant.now());
        return detail(username, storeId, orderId);
    }
}
