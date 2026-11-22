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
package com.codenameone.examples.bazaar.customer;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CreditAdjustDto;
import com.codenameone.examples.bazaar.api.CustomerDetailDto;
import com.codenameone.examples.bazaar.api.CustomerDto;
import com.codenameone.examples.bazaar.api.CustomerEditDto;
import com.codenameone.examples.bazaar.api.CustomerOrderDto;
import com.codenameone.examples.bazaar.api.ExportDto;
import com.codenameone.examples.bazaar.api.ExportRowDto;
import com.codenameone.examples.bazaar.api.SavedAddressDto;
import com.codenameone.examples.bazaar.api.SegmentDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A store's customers for its staff: finding them, one customer's orders
/// and addresses, notes, tags and store credit, the built-in segments and an
/// export.
///
/// `Customer.ordersCount` and `spentCents` are kept by the code that places
/// an order (online and at a register) and the code that cancels one; the
/// last order date is read from the orders.
@Component
public class Customers {
    public static final String VIP = "vip";
    public static final String LAPSED = "lapsed";
    public static final String SUBSCRIBERS = "subscribers";
    public static final String NEW = "new";
    /// Over this lifetime spend a customer is a VIP: $500.
    public static final long VIP_CENTS = 50000L;
    static final long LAPSED_MILLIS = 90L * 86400000L;
    static final long NEW_MILLIS = 30L * 86400000L;

    private final CustomerRepository customers;
    private final CustomerAddressRepository addresses;
    private final StoreOrderRepository orders;
    private final StoreRepository stores;
    private final AuditEventRepository audits;
    private final StoreAccess access;
    private final StoreCredit credit;

    public Customers(CustomerRepository customers, CustomerAddressRepository addresses,
            StoreOrderRepository orders, StoreRepository stores, AuditEventRepository audits,
            StoreAccess access, StoreCredit credit) {
        this.credit = credit;
        this.customers = customers;
        this.addresses = addresses;
        this.orders = orders;
        this.stores = stores;
        this.audits = audits;
        this.access = access;
    }

    /// The query a customer list takes; every field may be empty.
    public static final class Filter {
        public String q = "";
        public String tag = "";
        public long minSpent;
        public int minOrders;
        public String marketing = "";
        public String segment = "";
        public String sort = "";
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> list(String username, String storeId, Filter f) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return search(storeId, f, System.currentTimeMillis());
    }

    private List<CustomerDto> search(String storeId, Filter f, long now) {
        List<Customer> all = customers.findByStoreIdOrderByLastNameAsc(storeId);
        Map<String, Long> last = lastOrders(storeId);
        List<CustomerDto> out = new ArrayList<CustomerDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Customer c = all.get(iter);
            long lastAt = last.containsKey(c.getId()) ? last.get(c.getId()).longValue() : 0;
            if (matches(c, lastAt, f, now)) {
                out.add(dto(c, lastAt));
            }
        }
        sort(out, f.sort);
        return out;
    }

    static boolean matches(Customer c, long lastAt, Filter f, long now) {
        String q = f.q == null ? "" : f.q.trim();
        if (q.length() > 0 && !contains(c.getEmail(), q) && !contains(c.getFirstName(), q)
                && !contains(c.getLastName(), q) && !contains(c.getUsername(), q)
                && !contains(c.getPhone(), q)
                && !contains(c.getFirstName() + " " + c.getLastName(), q)) {
            return false;
        }
        if (f.tag != null && f.tag.trim().length() > 0 && !hasTag(c.getTags(), f.tag)) {
            return false;
        }
        if (c.getSpentCents() < f.minSpent || c.getOrdersCount() < f.minOrders) {
            return false;
        }
        if ("true".equalsIgnoreCase(f.marketing) && !c.isAcceptsMarketing()
                || "false".equalsIgnoreCase(f.marketing) && c.isAcceptsMarketing()) {
            return false;
        }
        String s = f.segment == null ? "" : f.segment.trim();
        return s.length() == 0 || inSegment(c, lastAt, s, now);
    }

    /// Whether a customer belongs to one of the built-in segments.
    public static boolean inSegment(Customer c, long lastOrderAt, String segment, long now) {
        if (VIP.equalsIgnoreCase(segment)) {
            return c.getSpentCents() > VIP_CENTS;
        }
        if (LAPSED.equalsIgnoreCase(segment)) {
            return c.getOrdersCount() > 0 && lastOrderAt > 0 && now - lastOrderAt > LAPSED_MILLIS;
        }
        if (SUBSCRIBERS.equalsIgnoreCase(segment)) {
            return c.isAcceptsMarketing();
        }
        if (NEW.equalsIgnoreCase(segment)) {
            return now - c.getCreatedAt().toEpochMilli() <= NEW_MILLIS;
        }
        throw new ResponseStatusException(400, "No such segment");
    }

    private static boolean contains(String text, String q) {
        if (text == null || q.length() > text.length()) {
            return false;
        }
        for (int iter = 0; iter + q.length() <= text.length(); iter++) {
            if (text.regionMatches(true, iter, q, 0, q.length())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTag(String tags, String tag) {
        List<String> all = Catalog.tags(tags);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).equalsIgnoreCase(tag.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void sort(List<CustomerDto> out, String sort) {
        final String s = sort == null ? "" : sort;
        if (s.length() == 0 || "name".equalsIgnoreCase(s)) {
            return;
        }
        Collections.sort(out, new Comparator<CustomerDto>() {
            @Override
            public int compare(CustomerDto a, CustomerDto b) {
                if ("orders".equalsIgnoreCase(s)) {
                    return Integer.compare(b.ordersCount(), a.ordersCount());
                }
                if ("recent".equalsIgnoreCase(s)) {
                    return Long.compare(b.lastOrderAt(), a.lastOrderAt());
                }
                if ("newest".equalsIgnoreCase(s)) {
                    return Long.compare(b.createdAt(), a.createdAt());
                }
                return Long.compare(b.spentCents(), a.spentCents());
            }
        });
    }

    /// Each customer's newest counted order.
    private Map<String, Long> lastOrders(String storeId) {
        List<StoreOrder> all = orders.findByStoreIdAndCreatedAtGreaterThanEqual(storeId,
                Instant.EPOCH);
        Map<String, Long> last = new HashMap<String, Long>();
        for (int iter = 0; iter < all.size(); iter++) {
            StoreOrder o = all.get(iter);
            if (o.getCustomerId().length() == 0 || !counts(o)) {
                continue;
            }
            Long seen = last.get(o.getCustomerId());
            long at = o.getCreatedAt().toEpochMilli();
            if (seen == null || seen.longValue() < at) {
                last.put(o.getCustomerId(), Long.valueOf(at));
            }
        }
        return last;
    }

    static boolean counts(StoreOrder o) {
        return !"CANCELLED".equals(o.getStatus()) && !Orders.isUnconvertedDraft(o);
    }

    @Transactional(readOnly = true)
    public CustomerDetailDto detail(String username, String storeId, String customerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Customer c = mine(storeId, customerId);
        List<StoreOrder> all = orders.findByStoreIdAndCreatedAtGreaterThanEqual(storeId,
                Instant.EPOCH);
        List<StoreOrder> theirs = new ArrayList<StoreOrder>();
        for (int iter = 0; iter < all.size(); iter++) {
            StoreOrder o = all.get(iter);
            if (customerId.equals(o.getCustomerId()) && !Orders.isUnconvertedDraft(o)) {
                theirs.add(o);
            }
        }
        Collections.sort(theirs, new Comparator<StoreOrder>() {
            @Override
            public int compare(StoreOrder a, StoreOrder b) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
        });
        long lifetime = 0;
        int counted = 0;
        long lastAt = 0;
        List<CustomerOrderDto> rows = new ArrayList<CustomerOrderDto>();
        for (int iter = 0; iter < theirs.size(); iter++) {
            StoreOrder o = theirs.get(iter);
            rows.add(new CustomerOrderDto(o.getId(), o.getNumber(), o.getChannel(), o.getStatus(),
                    o.getFinancial(), o.getFulfilment(), o.getTotalCents(), o.getRefundedCents(),
                    o.getCurrency(), o.getCreatedAt().toEpochMilli()));
            if (counts(o)) {
                lifetime += o.getTotalCents() - o.getRefundedCents();
                counted++;
                lastAt = Math.max(lastAt, o.getCreatedAt().toEpochMilli());
            }
        }
        List<SavedAddressDto> saved = new ArrayList<SavedAddressDto>();
        List<CustomerAddress> book = addresses.findByCustomerId(customerId);
        for (int iter = 0; iter < book.size(); iter++) {
            CustomerAddress a = book.get(iter);
            saved.add(new SavedAddressDto(a.getId(), new AddressDto(a.getName(), a.getLine1(),
                    a.getLine2(), a.getCity(), a.getRegion(), a.getPostal(), a.getCountry(),
                    a.getPhone(), a.getLat(), a.getLng()), a.isIsDefault()));
        }
        Store store = stores.findById(storeId);
        return new CustomerDetailDto(dto(c, lastAt), rows, saved, lifetime,
                counted == 0 ? 0 : lifetime / counted, store == null ? "" : store.getCurrency(),
                credit.grantsOf(c.getId()));
    }

    @Transactional(rollbackFor = IOException.class)
    public CustomerDto edit(String username, String storeId, String customerId,
            CustomerEditDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Customer c = mine(storeId, customerId);
        String note = wanted.note() == null ? "" : wanted.note().trim();
        if (note.length() > 2000) {
            throw new ResponseStatusException(400, "A note is at most 2000 characters");
        }
        StringBuilder tags = new StringBuilder();
        List<String> given = wanted.tags() == null ? new ArrayList<String>() : wanted.tags();
        List<String> kept = new ArrayList<String>();
        for (int iter = 0; iter < given.size(); iter++) {
            String t = given.get(iter) == null ? "" : given.get(iter).trim();
            if (t.length() == 0 || t.indexOf(',') >= 0 || t.length() > 40 || hasTag(join(kept), t)) {
                continue;
            }
            kept.add(t);
        }
        tags.append(join(kept));
        if (tags.length() > 500) {
            throw new ResponseStatusException(400, "Too many tags");
        }
        c.setNote(note);
        c.setTags(tags.toString());
        c.setAcceptsMarketing(wanted.acceptsMarketing());
        c.setUpdatedAt(Instant.now());
        customers.save(c);
        audit(storeId, username, "CUSTOMER_EDITED", customerId, "Notes and tags");
        return dto(c, lastOrders(storeId).containsKey(customerId)
                ? lastOrders(storeId).get(customerId).longValue() : 0);
    }

    private static String join(List<String> tags) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < tags.size(); iter++) {
            out.append(iter == 0 ? "" : ",").append(tags.get(iter));
        }
        return out.toString();
    }

    /// Adds to (or, negative, takes from) a customer's store credit; it never
    /// goes below zero. Added credit lapses at `adjust.expiresAt()` (0 for
    /// never); taken credit comes off the grants that lapse first.
    @Transactional(rollbackFor = IOException.class)
    public CustomerDto adjustCredit(String username, String storeId, String customerId,
            CreditAdjustDto adjust) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (adjust == null || adjust.amountCents() == 0
                || Math.abs(adjust.amountCents()) > 10000000L) {
            throw new ResponseStatusException(400, "An adjustment is a non-zero amount up to 100000");
        }
        String reason = adjust.reason() == null ? "" : adjust.reason().trim();
        if (reason.length() == 0) {
            throw new ResponseStatusException(400, "Say why the credit changes");
        }
        Customer c = mine(storeId, customerId);
        if (adjust.amountCents() > 0) {
            credit.grant(storeId, customerId, adjust.amountCents(),
                    adjust.expiresAt() <= 0 ? Instant.EPOCH : Instant.ofEpochMilli(adjust.expiresAt()),
                    reason);
        } else if (!credit.take(customerId, -adjust.amountCents(), "")) {
            throw new ResponseStatusException(409, "The customer has only "
                    + credit.available(c) + " cents of credit");
        }
        audit(storeId, username, "STORE_CREDIT", customerId, Codes.cut(adjust.amountCents()
                + " cents" + (adjust.amountCents() > 0 && adjust.expiresAt() > 0
                    ? " until " + Instant.ofEpochMilli(adjust.expiresAt()) : "")
                + ": " + reason, 500));
        return dto(customers.findById(customerId), 0);
    }

    @Transactional(readOnly = true)
    public List<SegmentDto> segments(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        long now = System.currentTimeMillis();
        List<Customer> all = customers.findByStoreIdOrderByLastNameAsc(storeId);
        Map<String, Long> last = lastOrders(storeId);
        String[][] defs = {
            {VIP, "VIP", "Spent over $500"},
            {LAPSED, "Lapsed", "No order in 90 days"},
            {SUBSCRIBERS, "Subscribers", "Accept marketing email"},
            {NEW, "New", "Joined in the last 30 days"},
        };
        List<SegmentDto> out = new ArrayList<SegmentDto>();
        for (int d = 0; d < defs.length; d++) {
            int count = 0;
            for (int iter = 0; iter < all.size(); iter++) {
                Customer c = all.get(iter);
                Long at = last.get(c.getId());
                if (inSegment(c, at == null ? 0 : at.longValue(), defs[d][0], now)) {
                    count++;
                }
            }
            out.add(new SegmentDto(defs[d][0], defs[d][1], defs[d][2], count));
        }
        return out;
    }

    /// The customers of a segment (all of them when it is empty) as rows of
    /// text, ready to write as CSV.
    @Transactional(readOnly = true)
    public ExportDto export(String username, String storeId, String segment) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Filter f = new Filter();
        f.segment = segment == null ? "" : segment;
        List<CustomerDto> all = search(storeId, f, System.currentTimeMillis());
        List<ExportRowDto> rows = new ArrayList<ExportRowDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            CustomerDto c = all.get(iter);
            rows.add(new ExportRowDto(Arrays.asList(c.id(), c.email(), c.firstName(),
                    c.lastName(), c.phone(), join(c.tags()),
                    Boolean.toString(c.acceptsMarketing()), Integer.toString(c.ordersCount()),
                    Long.toString(c.spentCents()), Long.toString(c.storeCreditCents()),
                    c.lastOrderAt() == 0 ? "" : Instant.ofEpochMilli(c.lastOrderAt()).toString(),
                    Instant.ofEpochMilli(c.createdAt()).toString())));
        }
        return new ExportDto(Arrays.asList("id", "email", "first_name", "last_name", "phone",
                "tags", "accepts_marketing", "orders", "spent_cents", "store_credit_cents",
                "last_order_at", "created_at"), rows);
    }

    private Customer mine(String storeId, String customerId) {
        Customer c = customerId == null ? null : customers.findById(customerId);
        if (c == null || !storeId.equals(c.getStoreId())) {
            throw new ResponseStatusException(404, "No such customer");
        }
        return c;
    }

    private void audit(String storeId, String username, String action, String subject,
            String detail) throws IOException {
        AuditEvent e = new AuditEvent();
        e.setId(Ids.next());
        e.setStoreId(storeId);
        e.setUsername(username);
        e.setAction(action);
        e.setSubject(subject);
        e.setDetail(Codes.cut(detail, 500));
        e.setCreatedAt(Instant.now());
        audits.insert(e);
    }

    static CustomerDto dto(Customer c, long lastOrderAt) {
        return new CustomerDto(c.getId(), c.getUsername(), c.getEmail(), c.getPhone(),
                c.getFirstName(), c.getLastName(), Catalog.tags(c.getTags()),
                c.isAcceptsMarketing(), c.getNote(), c.getOrdersCount(), c.getSpentCents(),
                c.getStoreCreditCents(), lastOrderAt, c.getCreatedAt().toEpochMilli());
    }
}
