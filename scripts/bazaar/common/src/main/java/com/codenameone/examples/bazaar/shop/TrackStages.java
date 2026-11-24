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
package com.codenameone.examples.bazaar.shop;

import com.codenameone.examples.bazaar.api.FulfillmentSummaryDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderEventDto;
import java.util.ArrayList;
import java.util.List;

/// The six stages of the order-tracking screen, from placed to delivered,
/// and the in-app banner each new one is announced with. Finer than
/// [OrderSteps], whose four steps label the order lists and pick the badge
/// colours; pure, so the screen only draws what this decides.
public final class TrackStages {
    public static final int PLACED = 0;
    public static final int PAID = 1;
    public static final int PACKED = 2;
    public static final int SHIPPED = 3;
    public static final int OUT_FOR_DELIVERY = 4;
    public static final int DELIVERED = 5;
    /// What [#reached] answers for a cancelled order.
    public static final int CANCELLED = -1;

    public static final String[] LABELS = {"Placed", "Paid", "Packed", "Shipped",
        "Out for delivery", "Delivered"};

    /// The banner title of each stage, reached while the shopper watches.
    private static final String[] BANNERS = {"Order placed", "Payment confirmed",
        "Your order is packed", "Your order has shipped", "Out for delivery today",
        "Delivered!"};

    private TrackStages() {
    }

    /// The stage `order` has reached.
    public static int reached(OrderDto order) {
        if (order == null) {
            return PLACED;
        }
        List<String> parcels = new ArrayList<String>();
        List<FulfillmentSummaryDto> fs = order.fulfillments();
        for (int iter = 0; fs != null && iter < fs.size(); iter++) {
            parcels.add(fs.get(iter).status());
        }
        return reached(order.status(), order.financial(), parcels);
    }

    /// The stage of an order with this status, financial status and parcels:
    /// the furthest any parcel got, Delivered only when every parcel that was
    /// not cancelled arrived.
    public static int reached(String status, String financial, List<String> parcels) {
        if ("CANCELLED".equals(status)) {
            return CANCELLED;
        }
        int at = PLACED;
        if ("AUTHORIZED".equals(financial) || "PAID".equals(financial)
                || "PARTIALLY_REFUNDED".equals(financial) || "REFUNDED".equals(financial)) {
            at = PAID;
        }
        if (parcels == null || parcels.isEmpty()) {
            return at;
        }
        int furthest = at;
        boolean allArrived = true;
        boolean any = false;
        for (int iter = 0; iter < parcels.size(); iter++) {
            String s = parcels.get(iter);
            if ("CANCELLED".equals(s)) {
                continue;
            }
            any = true;
            int p = parcel(s);
            if (p != DELIVERED) {
                allArrived = false;
            }
            furthest = Math.max(furthest, Math.min(p, OUT_FOR_DELIVERY));
        }
        if (!any) {
            return at;
        }
        // A parcel on its way means the order was paid, whatever the
        // financial status says (a card is captured when the goods ship).
        return allArrived ? DELIVERED : Math.max(furthest, PACKED);
    }

    private static int parcel(String s) {
        if ("DELIVERED".equals(s) || "PICKED_UP".equals(s)) {
            return DELIVERED;
        }
        if ("OUT_FOR_DELIVERY".equals(s) || "READY_FOR_PICKUP".equals(s)) {
            return OUT_FOR_DELIVERY;
        }
        if ("SHIPPED".equals(s) || "IN_TRANSIT".equals(s)) {
            return SHIPPED;
        }
        return PACKED;
    }

    /// The share of the way from placed to delivered, in thousandths: what
    /// the progress line and the parcel on the route are drawn at.
    public static int progress(int stage) {
        if (stage <= PLACED) {
            return 0;
        }
        return Math.min(1000, stage * 1000 / DELIVERED);
    }

    /// The banner for moving from `before` to `after`, or null when nothing
    /// worth announcing happened (no change, a step back, a cancellation).
    public static String banner(int before, int after) {
        if (after <= before || after < 0 || after >= BANNERS.length) {
            return null;
        }
        return BANNERS[after];
    }

    /// When each stage was reached, in epoch ms, from the order's timeline:
    /// 0 for a stage not reached. A stage whose own event is missing (a
    /// seeded order, or a parcel that skipped a scan) takes the time of the
    /// stage before it, so every completed step has a time and the times
    /// never run backwards.
    public static long[] times(OrderDto order) {
        long[] out = new long[LABELS.length];
        if (order == null) {
            return out;
        }
        List<OrderEventDto> es = order.events();
        for (int iter = 0; es != null && iter < es.size(); iter++) {
            OrderEventDto e = es.get(iter);
            int stage = eventStage(e.kind());
            if (stage >= 0 && e.createdAt() > 0 && (out[stage] == 0 || e.createdAt() < out[stage])) {
                out[stage] = e.createdAt();
            }
        }
        List<FulfillmentSummaryDto> fs = order.fulfillments();
        for (int iter = 0; fs != null && iter < fs.size(); iter++) {
            long at = fs.get(iter).createdAt();
            if (at > 0 && out[PACKED] == 0) {
                out[PACKED] = at;
            }
        }
        if (out[PLACED] == 0) {
            out[PLACED] = order.createdAt();
        }
        return fill(out, reached(order));
    }

    /// How a step's time reads: whole seconds since `at` when that is under
    /// an hour, or -1 when the step is old enough to show its clock time. A
    /// fast order moves through several steps in one minute, and the same
    /// clock time on each of them reads as a glitch; "40 sec ago", "12 sec
    /// ago" and "just now" read as the order moving.
    public static int secondsAgo(long at, long now) {
        long age = now - at;
        if (age < 0) {
            return 0;
        }
        if (age >= 3600000L) {
            return -1;
        }
        return (int) (age / 1000L);
    }

    /// [#times] once the events are sorted into stages: the stages past
    /// `now` cleared, the gaps up to it filled forward, never backwards.
    public static long[] fill(long[] at, int now) {
        long[] out = new long[at.length];
        long last = 0;
        for (int iter = 0; iter < at.length && iter <= now; iter++) {
            last = Math.max(last, at[iter]);
            out[iter] = last;
        }
        return out;
    }

    /// The stage an order event marks, or -1.
    public static int eventStage(String kind) {
        if (kind == null) {
            return -1;
        }
        switch (kind) {
            case "PLACED":
                return PLACED;
            case "AUTHORIZED":
            case "CAPTURED":
            case "PAID":
                return PAID;
            case "FULFILMENT":
            case "FULFILMENT_PACKED":
                return PACKED;
            case "LABEL":
            case "FULFILMENT_SHIPPED":
            case "FULFILMENT_IN_TRANSIT":
                return SHIPPED;
            case "FULFILMENT_OUT_FOR_DELIVERY":
                return OUT_FOR_DELIVERY;
            case "DELIVERED":
            case "FULFILMENT_DELIVERED":
                return DELIVERED;
            default:
                return -1;
        }
    }

    /// The word on an order's status chip: the tracking stage, so the chip
    /// and the tracker never disagree; Cancelled and Refunded win.
    public static String chip(OrderDto order) {
        if (order == null) {
            return "";
        }
        if ("CANCELLED".equals(order.status())) {
            return "Cancelled";
        }
        if ("REFUNDED".equals(order.financial())) {
            return "Refunded";
        }
        return LABELS[Math.max(0, reached(order))];
    }

    /// The name a shopper sees for a carrier code: the demo's simulated
    /// carrier is "Bazaar Express", a known code its proper name, anything
    /// else as the server sent it.
    public static String carrierName(String code) {
        if (code == null || code.length() == 0) {
            return "";
        }
        if ("simulated".equalsIgnoreCase(code) || "sim".equalsIgnoreCase(code)) {
            return "Bazaar Express";
        }
        if ("fedex".equalsIgnoreCase(code)) {
            return "FedEx";
        }
        // Folded by hand: toUpperCase is locale sensitive.
        String[] upper = {"USPS", "UPS", "DHL"};
        for (String u : upper) {
            if (u.equalsIgnoreCase(code)) {
                return u;
            }
        }
        return code;
    }

    /// A tracking number in groups of four, the way a label prints it:
    /// `SIM255202074779` reads `SIM 2552 0207 4779` -- the letters as their
    /// own group, the digits in fours from the end so the last is whole.
    public static String trackingNumber(String n) {
        if (n == null) {
            return "";
        }
        int at = 0;
        while (at < n.length() && !Character.isDigit(n.charAt(at))) {
            at++;
        }
        int digits = n.length() - at;
        if (digits <= 4) {
            return n;
        }
        StringBuilder out = new StringBuilder(n.length() + 6);
        out.append(n.substring(0, at));
        int first = digits % 4 == 0 ? 4 : digits % 4;
        for (int iter = 0; iter < digits; iter++) {
            if ((iter == 0 && at > 0) || (iter > 0 && (iter - first) % 4 == 0 && iter >= first)) {
                out.append(' ');
            }
            out.append(n.charAt(at + iter));
        }
        return out.toString();
    }

    /// The delivery window for an order at stage `now` when the hour of the
    /// day is `hourNow`: `{from hour, to hour, days away}` (0 today, 1
    /// tomorrow); null once delivered or cancelled.
    public static int[] window(int now, int hourNow) {
        if (now < PLACED || now >= DELIVERED) {
            return null;
        }
        if (now == OUT_FOR_DELIVERY) {
            int from = Math.max(hourNow + 1, 10);
            from += from % 2;
            if (from > 18) {
                return new int[] {9, 12, 1};
            }
            return new int[] {from, from + 2, 0};
        }
        // Not out yet: a day for each stage still ahead of the van.
        return new int[] {10, 14, Math.min(2, OUT_FOR_DELIVERY - now)};
    }

    /// A span of hours: `4-6 pm` when both ends share am or pm, `11 am-1 pm`
    /// when they do not.
    public static String span(int from, int to) {
        if (from < 12 == to < 12) {
            return (from % 12 == 0 ? 12 : from % 12) + "-" + hour(to);
        }
        return hour(from) + "-" + hour(to);
    }

    /// An hour of the day as a shopper reads it: `4 pm`, `11 am`, `12 pm`.
    public static String hour(int h) {
        int x = h % 12 == 0 ? 12 : h % 12;
        return x + (h < 12 ? " am" : " pm");
    }
}
