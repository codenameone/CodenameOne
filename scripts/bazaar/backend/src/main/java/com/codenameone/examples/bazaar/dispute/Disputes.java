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
package com.codenameone.examples.bazaar.dispute;

import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.DisputeDto;
import com.codenameone.examples.bazaar.api.DisputeEvidenceDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Dispute;
import com.codenameone.examples.bazaar.domain.DisputeEvidence;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.DisputeEvidenceRepository;
import com.codenameone.examples.bazaar.repository.DisputeRepository;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.Money;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Payment disputes (chargebacks): what the processor reports, the evidence
/// the merchant answers with, and the money held meanwhile.
///
/// **From the processor.** [#webhook] takes the processor's event, signed
/// with HMAC-SHA256 over the raw body under `bazaar.payments.webhook-secret`
/// (refused outright while that is empty) and idempotent on the event id: a
/// redelivery changes nothing. `dispute.created` opens one on the order its
/// metadata names, NEEDS_RESPONSE with the processor's deadline (seven days
/// when it gives none); `dispute.closed` settles it WON or LOST. The
/// simulated processor's events are the same JSON as the test's.
///
/// **The pipeline.** NEEDS_RESPONSE -> UNDER_REVIEW when the merchant submits
/// evidence (refused past the deadline: the processor decides for the
/// buyer then), or -> ACCEPTED when the merchant concedes; UNDER_REVIEW ->
/// WON or LOST by the processor. Every move is a conditional update on the
/// status it starts from.
///
/// **Funds held.** The disputed amount is held back from the store until the
/// dispute closes ([#heldCents]); a LOST or ACCEPTED one is gone for good, a
/// WON one is released.
@Component
public class Disputes {
    public static final String NEEDS_RESPONSE = "NEEDS_RESPONSE";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String WON = "WON";
    public static final String LOST = "LOST";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String TEXT = "TEXT";
    public static final String ORDER = "ORDER";
    static final int MAX_EVIDENCE = 20;
    static final int MAX_TEXT = 4000;

    private final DisputeRepository disputes;
    private final DisputeEvidenceRepository evidence;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final FulfillmentRepository fulfillments;
    private final Orders orderService;
    private final StoreAccess access;
    private final String secret;

    public Disputes(DisputeRepository disputes, DisputeEvidenceRepository evidence,
            StoreOrderRepository orders, OrderLineRepository lines,
            FulfillmentRepository fulfillments, Orders orderService, StoreAccess access,
            Config config) throws IOException {
        this.disputes = disputes;
        this.evidence = evidence;
        this.orders = orders;
        this.lines = lines;
        this.fulfillments = fulfillments;
        this.orderService = orderService;
        this.access = access;
        this.secret = config.get("bazaar.payments.webhook-secret", "");
    }

    // -------------------------------------------------------------- webhook

    /// Whether `signature` (`sha256=<hex>`) signs `body`.
    boolean signed(byte[] body, String signature) throws IOException {
        if (secret.length() == 0 || signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        return Tokens.verify(secret, new String(body, "UTF-8"), signature.substring(7));
    }

    /// Applies a processor event; answers the dispute it touched, or null
    /// for an event of another type or one already applied.
    @Transactional(rollbackFor = IOException.class)
    public DisputeDto webhook(byte[] body, String signature) throws IOException {
        if (body == null || body.length > 65536 || !signed(body, signature)) {
            throw new ResponseStatusException(401, "Bad signature");
        }
        Map event;
        try {
            event = Json.parseObject(new String(body, "UTF-8"));
        } catch (IOException | RuntimeException bad) {
            throw new ResponseStatusException(400, "Not JSON");
        }
        String id = text(event, "id", 64);
        String type = text(event, "type", 40);
        if (id.length() == 0) {
            throw new ResponseStatusException(400, "The event has no id");
        }
        if ("dispute.created".equals(type)) {
            if (disputes.findByEventId(id) != null) {
                return null;
            }
            StoreOrder o = orders.findById(text(event, "orderId", 32));
            if (o == null) {
                throw new ResponseStatusException(404, "No such order");
            }
            long amount = number(event, "amountCents");
            if (amount <= 0 || amount > o.getTotalCents()) {
                amount = o.getTotalCents();
            }
            long due = number(event, "dueAt");
            Instant now = Instant.now();
            Dispute d = new Dispute();
            d.setId(Ids.next());
            d.setStoreId(o.getStoreId());
            d.setOrderId(o.getId());
            d.setEventId(id);
            String reason = text(event, "reason", 40);
            d.setReason(reason.length() == 0 ? "general" : reason);
            d.setAmountCents(amount);
            d.setCurrency(o.getCurrency());
            d.setStatus(NEEDS_RESPONSE);
            d.setDueAt(Instant.ofEpochMilli(due > now.toEpochMilli() ? due
                    : now.toEpochMilli() + 7 * Days.MILLIS));
            d.setCreatedAt(now);
            d.setUpdatedAt(now);
            disputes.insert(d);
            orderService.event(o.getId(), "DISPUTE", "Payment disputed: "
                    + Money.format(amount, o.getCurrency()) + " held", "processor");
            return dto(d, o);
        }
        if ("dispute.closed".equals(type)) {
            Dispute d = disputes.findByEventId(text(event, "disputeEventId", 64));
            if (d == null) {
                throw new ResponseStatusException(404, "No such dispute");
            }
            String outcome = "won".equalsIgnoreCase(text(event, "outcome", 8)) ? WON : LOST;
            if (WON.equals(d.getStatus()) || LOST.equals(d.getStatus())
                    || ACCEPTED.equals(d.getStatus())) {
                return null;
            }
            if (disputes.move(d.getId(), d.getStatus(), outcome, Instant.now()) != 1) {
                throw new ResponseStatusException(409, "The dispute is changing; retry");
            }
            orderService.event(d.getOrderId(), "DISPUTE_" + outcome, WON.equals(outcome)
                    ? "Dispute won: funds released" : "Dispute lost", "processor");
            Dispute after = disputes.findById(d.getId());
            return dto(after, orders.findById(after.getOrderId()));
        }
        return null;
    }

    private static String text(Map m, String key, int max) {
        Object v = m.get(key);
        return v instanceof String ? Codes.cut((String) v, max) : "";
    }

    private static long number(Map m, String key) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0;
    }

    // ------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public List<DisputeDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<Dispute> rows = disputes.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<DisputeDto> out = new ArrayList<DisputeDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter), orders.findById(rows.get(iter).getOrderId())));
        }
        return out;
    }

    /// What open disputes hold back from the store now.
    @Transactional(readOnly = true)
    public long heldCents(String storeId) throws IOException {
        List<Dispute> rows = disputes.findByStoreIdOrderByCreatedAtDesc(storeId);
        long held = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            String s = rows.get(iter).getStatus();
            if (NEEDS_RESPONSE.equals(s) || UNDER_REVIEW.equals(s)) {
                held += rows.get(iter).getAmountCents();
            }
        }
        return held;
    }

    static DisputeDto dto(Dispute d, StoreOrder o) {
        return new DisputeDto(d.getId(), d.getOrderId(), o == null ? 0 : o.getNumber(),
                d.getReason(), d.getAmountCents(), d.getCurrency(), d.getStatus(),
                d.getDueAt().toEpochMilli(), d.getCreatedAt().toEpochMilli());
    }

    Dispute dispute(String storeId, String disputeId) throws IOException {
        Dispute d = disputeId == null ? null : disputes.findById(disputeId);
        if (d == null || !storeId.equals(d.getStoreId())) {
            throw new ResponseStatusException(404, "No such dispute");
        }
        return d;
    }

    @Transactional(readOnly = true)
    public DisputeDto detail(String username, String storeId, String disputeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Dispute d = dispute(storeId, disputeId);
        return dto(d, orders.findById(d.getOrderId()));
    }

    @Transactional(readOnly = true)
    public List<DisputeEvidenceDto> evidence(String username, String storeId, String disputeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Dispute d = dispute(storeId, disputeId);
        List<DisputeEvidence> rows = evidence.findByDisputeIdOrderByCreatedAtAsc(d.getId());
        List<DisputeEvidenceDto> out = new ArrayList<DisputeEvidenceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            DisputeEvidence e = rows.get(iter);
            out.add(new DisputeEvidenceDto(e.getId(), e.getKind(), e.getBody(), e.getCreatedBy(),
                    e.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    /// Adds evidence while the dispute awaits a response: `TEXT` with the
    /// merchant's words, or `ORDER`, the order's own record (lines, totals,
    /// delivery and tracking) written by the server, not typed.
    @Transactional(rollbackFor = IOException.class)
    public List<DisputeEvidenceDto> addEvidence(String username, String storeId,
            String disputeId, DisputeEvidenceDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Dispute d = dispute(storeId, disputeId);
        if (!NEEDS_RESPONSE.equals(d.getStatus())) {
            throw new ResponseStatusException(409, "Evidence can only be added before it is "
                    + "submitted");
        }
        if (evidence.findByDisputeIdOrderByCreatedAtAsc(d.getId()).size() >= MAX_EVIDENCE) {
            throw new ResponseStatusException(409, "A dispute takes at most " + MAX_EVIDENCE
                    + " pieces of evidence");
        }
        String kind = in == null || in.kind() == null ? TEXT : in.kind();
        String body;
        if (ORDER.equals(kind)) {
            body = orderRecord(d.getOrderId());
        } else if (TEXT.equals(kind)) {
            body = in.body() == null ? "" : in.body().trim();
            if (body.length() == 0 || body.length() > MAX_TEXT) {
                throw new ResponseStatusException(400, "Write 1 to " + MAX_TEXT + " characters");
            }
        } else {
            throw new ResponseStatusException(400, "Evidence is TEXT or ORDER");
        }
        DisputeEvidence e = new DisputeEvidence();
        e.setId(Ids.next());
        e.setStoreId(storeId);
        e.setDisputeId(d.getId());
        e.setKind(kind);
        e.setBody(body);
        e.setCreatedBy(username);
        e.setCreatedAt(Instant.now());
        evidence.insert(e);
        return evidence(username, storeId, disputeId);
    }

    /// The order as evidence: number, date, lines, totals, where it went and
    /// how it was sent.
    String orderRecord(String orderId) throws IOException {
        StoreOrder o = orders.findById(orderId);
        StringBuilder out = new StringBuilder();
        out.append("Order #").append(o.getNumber()).append(", placed ")
                .append(Days.iso(o.getCreatedAt().toEpochMilli())).append(" by ")
                .append(o.getEmail()).append('\n');
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine l = ls.get(iter);
            out.append(l.getQuantity()).append(" x ").append(l.getTitle())
                    .append(l.getVariantTitle().length() > 0 ? " (" + l.getVariantTitle() + ")" : "")
                    .append(" at ").append(Money.format(l.getPriceCents(), o.getCurrency()))
                    .append('\n');
        }
        out.append("Total ").append(Money.format(o.getTotalCents(), o.getCurrency()))
                .append(", payment ").append(Tokens.fold(o.getFinancial()))
                .append('\n');
        out.append("Delivered to ").append(o.getShipName()).append(", ").append(o.getShipCity())
                .append(' ').append(o.getShipCountry()).append('\n');
        List<Fulfillment> fs = fulfillments.findByOrderId(orderId);
        for (int iter = 0; iter < fs.size(); iter++) {
            Fulfillment f = fs.get(iter);
            out.append("Fulfilment ").append(Tokens.fold(f.getStatus()));
            if (f.getTrackingNumber() != null && f.getTrackingNumber().length() > 0) {
                out.append(", tracking ").append(f.getTrackingNumber());
            }
            out.append('\n');
        }
        return Codes.cut(out.toString(), MAX_TEXT);
    }

    /// Sends the evidence to the processor: NEEDS_RESPONSE -> UNDER_REVIEW.
    public DisputeDto submit(String username, String storeId, String disputeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Dispute d = dispute(storeId, disputeId);
        if (NEEDS_RESPONSE.equals(d.getStatus())
                && d.getDueAt().toEpochMilli() < System.currentTimeMillis()) {
            throw new ResponseStatusException(409, "The deadline to respond has passed");
        }
        if (evidence.findByDisputeIdOrderByCreatedAtAsc(d.getId()).isEmpty()) {
            throw new ResponseStatusException(400, "Add evidence before submitting");
        }
        if (disputes.move(d.getId(), NEEDS_RESPONSE, UNDER_REVIEW, Instant.now()) != 1) {
            throw new ResponseStatusException(409, "The dispute is not awaiting a response");
        }
        orderService.event(d.getOrderId(), "DISPUTE_SUBMITTED", "Dispute evidence submitted",
                username);
        return detail(username, storeId, disputeId);
    }

    /// Concedes: NEEDS_RESPONSE -> ACCEPTED, the money stays with the buyer.
    public DisputeDto accept(String username, String storeId, String disputeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Dispute d = dispute(storeId, disputeId);
        if (disputes.move(d.getId(), NEEDS_RESPONSE, ACCEPTED, Instant.now()) != 1) {
            throw new ResponseStatusException(409, "The dispute is not awaiting a response");
        }
        orderService.event(d.getOrderId(), "DISPUTE_ACCEPTED", "Dispute accepted", username);
        return detail(username, storeId, disputeId);
    }
}
