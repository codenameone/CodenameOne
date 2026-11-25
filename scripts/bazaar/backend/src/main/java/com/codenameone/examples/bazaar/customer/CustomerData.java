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

import com.codename1.backend.Base64;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.CustomerDto;
import com.codenameone.examples.bazaar.api.DataExportDto;
import com.codenameone.examples.bazaar.api.DataRequestDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import com.codenameone.examples.bazaar.domain.DataRequest;
import com.codenameone.examples.bazaar.domain.GiftCard;
import com.codenameone.examples.bazaar.domain.LoyaltyMember;
import com.codenameone.examples.bazaar.domain.ProductView;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.domain.ReviewRequest;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Wishlist;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.ClubMemberRepository;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.DataRequestRepository;
import com.codenameone.examples.bazaar.repository.DiscountUseRepository;
import com.codenameone.examples.bazaar.repository.GiftCardRepository;
import com.codenameone.examples.bazaar.repository.LayawayRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyMemberRepository;
import com.codenameone.examples.bazaar.repository.ParkedCartRepository;
import com.codenameone.examples.bazaar.repository.ProductViewRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.ReviewRequestRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.SubscriptionRepository;
import com.codenameone.examples.bazaar.repository.WishlistRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/// A customer's personal data: merging two records of one person, the data
/// export a shopper or a store downloads, and erasure.
///
/// **Merge.** The survivor absorbs the duplicate: its orders, addresses,
/// gift cards, layaways, parked carts, subscriptions, club memberships,
/// store credit grants, loyalty points, and the reviews and wishlist of its
/// account at this store. Counters add up, tags are joined, marketing
/// consent is kept if either record gave it, empty contact fields are filled
/// from the duplicate, and the duplicate is deleted. An audit entry records
/// both ids.
///
/// **Export.** A zip of JSON files: the customer record, orders, addresses,
/// store credit, gift cards, loyalty, reviews, wishlist and the last product
/// viewed. A shopper downloads theirs from every store at once; a store
/// downloads one customer's, to answer a request.
///
/// **Erasure.** The record is anonymised and kept, because orders point at
/// it; the orders keep every amount, tax and refund (the store's books need
/// them) but lose the name, contact details and address. Addresses, reviews,
/// wishlist, the browse record and pending review requests are deleted, and a
/// scheduled gift card's recipient is forgotten. Open erasure requests of the
/// customer are marked done.
@Component
public class CustomerData {
    public static final String EXPORT = "EXPORT";
    public static final String ERASURE = "ERASURE";
    public static final String OPEN = "OPEN";
    public static final String DONE = "DONE";
    /// The name an erased customer is shown under.
    public static final String ERASED = "Erased customer";

    private final CustomerRepository customers;
    private final StoreOrderRepository orders;
    private final CustomerAddressRepository addresses;
    private final GiftCardRepository giftCards;
    private final LayawayRepository layaways;
    private final ParkedCartRepository parked;
    private final SubscriptionRepository subscriptions;
    private final ClubMemberRepository clubs;
    private final DiscountUseRepository discountUses;
    private final LoyaltyMemberRepository members;
    private final ReviewRepository reviews;
    private final WishlistRepository wishlist;
    private final ProductViewRepository views;
    private final ReviewRequestRepository reviewRequests;
    private final DataRequestRepository requests;
    private final AuditEventRepository audits;
    private final StoreRepository stores;
    private final StoreAccess access;
    private final StoreCredit credit;
    private final com.codenameone.examples.bazaar.platform.Apps apps;

    public CustomerData(CustomerRepository customers, StoreOrderRepository orders,
            CustomerAddressRepository addresses, GiftCardRepository giftCards,
            LayawayRepository layaways, ParkedCartRepository parked,
            SubscriptionRepository subscriptions, ClubMemberRepository clubs,
            DiscountUseRepository discountUses, LoyaltyMemberRepository members,
            ReviewRepository reviews, WishlistRepository wishlist, ProductViewRepository views,
            ReviewRequestRepository reviewRequests, DataRequestRepository requests,
            AuditEventRepository audits, StoreRepository stores, StoreAccess access,
            StoreCredit credit, com.codenameone.examples.bazaar.platform.Apps apps) {
        this.customers = customers;
        this.orders = orders;
        this.addresses = addresses;
        this.giftCards = giftCards;
        this.layaways = layaways;
        this.parked = parked;
        this.subscriptions = subscriptions;
        this.clubs = clubs;
        this.discountUses = discountUses;
        this.members = members;
        this.reviews = reviews;
        this.wishlist = wishlist;
        this.views = views;
        this.reviewRequests = reviewRequests;
        this.requests = requests;
        this.audits = audits;
        this.stores = stores;
        this.access = access;
        this.credit = credit;
        this.apps = apps;
    }

    // ---------------------------------------------------------------- merge

    /// Merges `duplicateId` into `survivorId`; answers the survivor as it now
    /// is. Both must be customers of the store.
    @Transactional(rollbackFor = IOException.class)
    public CustomerDto merge(String username, String storeId, String survivorId,
            String duplicateId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Customer a = mine(storeId, survivorId);
        Customer b = mine(storeId, duplicateId);
        if (a.getId().equals(b.getId())) {
            throw new ResponseStatusException(400, "Choose another customer to merge");
        }
        String from = b.getId();
        String to = a.getId();
        orders.moveCustomer(from, to);
        addresses.moveCustomer(from, to);
        giftCards.moveCustomer(from, to);
        layaways.moveCustomer(from, to);
        parked.moveCustomer(from, to);
        subscriptions.moveCustomer(from, to);
        clubs.moveCustomer(from, to);
        discountUses.moveCustomer(from, to);
        requests.moveCustomer(from, to);
        credit.moveAll(from, to, 0);
        mergeLoyalty(storeId, a, b);
        String bUser = b.getUsername() == null ? "" : b.getUsername();
        String aUser = a.getUsername() == null ? "" : a.getUsername();
        String keepUser = aUser.length() > 0 ? aUser : bUser;
        if (bUser.length() > 0 && keepUser.length() > 0 && !bUser.equals(keepUser)) {
            moveAccountRows(storeId, bUser, keepUser);
        }
        // Read again: the bulk updates above cleared what the session held.
        a = customers.findById(to);
        b = customers.findById(from);
        a.setUsername(keepUser);
        a.setEmail(a.getEmail().length() > 0 ? a.getEmail() : b.getEmail());
        a.setPhone(a.getPhone().length() > 0 ? a.getPhone() : b.getPhone());
        a.setFirstName(a.getFirstName().length() > 0 ? a.getFirstName() : b.getFirstName());
        a.setLastName(a.getLastName().length() > 0 ? a.getLastName() : b.getLastName());
        a.setTags(Codes.cut(joinTags(a.getTags(), b.getTags()), 500));
        a.setNote(Codes.cut(a.getNote().length() == 0 ? b.getNote() : b.getNote().length() == 0
                ? a.getNote() : a.getNote() + "\n" + b.getNote(), 2000));
        a.setAcceptsMarketing(a.isAcceptsMarketing() || b.isAcceptsMarketing());
        a.setOrdersCount(a.getOrdersCount() + b.getOrdersCount());
        a.setSpentCents(a.getSpentCents() + b.getSpentCents());
        a.setStoreCreditCents(a.getStoreCreditCents() + b.getStoreCreditCents());
        if (b.getCreatedAt() != null && a.getCreatedAt() != null
                && b.getCreatedAt().isBefore(a.getCreatedAt())) {
            a.setCreatedAt(b.getCreatedAt());
        }
        a.setUpdatedAt(Instant.now());
        customers.delete(b);
        customers.save(a);
        audit(storeId, username, "CUSTOMER_MERGED", to, "Merged " + from + " ("
                + Codes.cut(name(b), 120) + ") into " + to);
        return Customers.dto(a, 0);
    }

    /// Tags of both, each once, in order.
    public static String joinTags(String a, String b) {
        List<String> out = new ArrayList<String>();
        addTags(out, a);
        addTags(out, b);
        StringBuilder s = new StringBuilder();
        for (int iter = 0; iter < out.size(); iter++) {
            if (iter > 0) {
                s.append(',');
            }
            s.append(out.get(iter));
        }
        return s.toString();
    }

    private static void addTags(List<String> out, String tags) {
        if (tags == null) {
            return;
        }
        int start = 0;
        while (start <= tags.length()) {
            int comma = tags.indexOf(',', start);
            int end = comma < 0 ? tags.length() : comma;
            String t = tags.substring(start, end).trim();
            boolean seen = false;
            for (int iter = 0; iter < out.size() && !seen; iter++) {
                seen = out.get(iter).equalsIgnoreCase(t);
            }
            if (t.length() > 0 && !seen) {
                out.add(t);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
    }

    private void mergeLoyalty(String storeId, Customer a, Customer b) {
        LoyaltyMember mb = members.findByStoreIdAndCustomerId(storeId, b.getId());
        if (mb == null) {
            return;
        }
        LoyaltyMember ma = members.findByStoreIdAndCustomerId(storeId, a.getId());
        if (ma == null) {
            mb.setCustomerId(a.getId());
            members.save(mb);
            return;
        }
        ma.setBalance(ma.getBalance() + mb.getBalance());
        ma.setLifetime(ma.getLifetime() + mb.getLifetime());
        members.delete(mb);
        members.save(ma);
    }

    /// Reviews, wishlist and the browse record of account `from` at the store
    /// become `to`'s; where `to` already has one for the product, `from`'s goes.
    private void moveAccountRows(String storeId, String from, String to) {
        List<Review> rs = reviews.findByUsernameOrderByCreatedAtDesc(from);
        for (int iter = 0; iter < rs.size(); iter++) {
            Review r = rs.get(iter);
            if (!storeId.equals(r.getStoreId())) {
                continue;
            }
            if (reviews.findByProductIdAndUsername(r.getProductId(), to) != null) {
                reviews.delete(r);
            } else {
                r.setUsername(to);
                reviews.save(r);
            }
        }
        List<Wishlist> ws = wishlist.findByUsernameOrderByCreatedAtDesc(from);
        for (int iter = 0; iter < ws.size(); iter++) {
            Wishlist w = ws.get(iter);
            if (!storeId.equals(w.getStoreId())) {
                continue;
            }
            if (wishlist.findByUsernameAndProductId(to, w.getProductId()) != null) {
                wishlist.delete(w);
            } else {
                w.setUsername(to);
                wishlist.save(w);
            }
        }
        views.deleteByStoreIdAndUsername(storeId, from);
    }

    // --------------------------------------------------------------- export

    /// One customer's data at the store, for the store to send on.
    @Transactional(rollbackFor = IOException.class)
    public DataExportDto exportFor(String username, String storeId, String customerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Customer c = mine(storeId, customerId);
        audit(storeId, username, "CUSTOMER_EXPORTED", c.getId(), "Data export downloaded");
        List<Customer> one = new ArrayList<Customer>();
        one.add(c);
        return zip("customer-" + c.getId(), one, c.getUsername());
    }

    /// The signed-in shopper's data at every store they are a customer of.
    @Transactional(readOnly = true)
    public DataExportDto exportMine(String me) throws IOException {
        if (me == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        return zip("my-data", customers.findByUsername(me), me);
    }

    private DataExportDto zip(String name, List<Customer> rows, String account) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        List<Object> all = new ArrayList<Object>();
        for (int iter = 0; iter < rows.size(); iter++) {
            all.add(describe(rows.get(iter)));
        }
        entry(zip, "customers.json", all);
        if (account != null && account.length() > 0) {
            Map<String, Object> mine = new LinkedHashMap<String, Object>();
            List<Object> adds = new ArrayList<Object>();
            List<CustomerAddress> as = addresses.findByUsername(account);
            for (int iter = 0; iter < as.size(); iter++) {
                adds.add(address(as.get(iter)));
            }
            mine.put("account", account);
            mine.put("addresses", adds);
            List<Object> rs = new ArrayList<Object>();
            List<Review> reviewRows = reviews.findByUsernameOrderByCreatedAtDesc(account);
            for (int iter = 0; iter < reviewRows.size(); iter++) {
                if (inStores(rows, reviewRows.get(iter).getStoreId())) {
                    rs.add(review(reviewRows.get(iter)));
                }
            }
            mine.put("reviews", rs);
            List<Object> ws = new ArrayList<Object>();
            List<Wishlist> wishRows = wishlist.findByUsernameOrderByCreatedAtDesc(account);
            for (int iter = 0; iter < wishRows.size(); iter++) {
                Wishlist w = wishRows.get(iter);
                if (inStores(rows, w.getStoreId())) {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("storeId", w.getStoreId());
                    m.put("productId", w.getProductId());
                    m.put("addedAt", w.getCreatedAt() == null ? "" : w.getCreatedAt().toString());
                    ws.add(m);
                }
            }
            mine.put("wishlist", ws);
            List<Object> vs = new ArrayList<Object>();
            List<ProductView> viewRows = views.findByUsername(account);
            for (int iter = 0; iter < viewRows.size(); iter++) {
                ProductView v = viewRows.get(iter);
                if (inStores(rows, v.getStoreId())) {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("storeId", v.getStoreId());
                    m.put("productId", v.getProductId());
                    m.put("viewedAt", v.getViewedAt().toString());
                    vs.add(m);
                }
            }
            mine.put("productViews", vs);
            entry(zip, "account.json", mine);
        }
        zip.close();
        return new DataExportDto(name + ".zip", Base64.encode(bytes.toByteArray()));
    }

    private static boolean inStores(List<Customer> rows, String storeId) {
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).getStoreId().equals(storeId)) {
                return true;
            }
        }
        return false;
    }

    private static void entry(ZipOutputStream zip, String name, Object value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(Json.write(value).getBytes("UTF-8"));
        zip.closeEntry();
    }

    private Map<String, Object> describe(Customer c) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        Store s = stores.findById(c.getStoreId());
        m.put("store", s == null ? c.getStoreId() : s.getName());
        m.put("storeId", c.getStoreId());
        m.put("customerId", c.getId());
        m.put("firstName", c.getFirstName());
        m.put("lastName", c.getLastName());
        m.put("email", c.getEmail());
        m.put("phone", c.getPhone());
        m.put("acceptsMarketing", Boolean.valueOf(c.isAcceptsMarketing()));
        m.put("language", c.getLanguage());
        m.put("tags", c.getTags());
        m.put("ordersCount", Integer.valueOf(c.getOrdersCount()));
        m.put("spentCents", Long.valueOf(c.getSpentCents()));
        m.put("storeCreditCents", Long.valueOf(c.getStoreCreditCents()));
        m.put("createdAt", c.getCreatedAt() == null ? "" : c.getCreatedAt().toString());
        List<Object> os = new ArrayList<Object>();
        List<StoreOrder> orderRows = orders.findByCustomerIdOrderByCreatedAtDesc(c.getId());
        for (int iter = 0; iter < orderRows.size(); iter++) {
            os.add(order(orderRows.get(iter)));
        }
        m.put("orders", os);
        List<Object> as = new ArrayList<Object>();
        List<CustomerAddress> addressRows = addresses.findByCustomerId(c.getId());
        for (int iter = 0; iter < addressRows.size(); iter++) {
            as.add(address(addressRows.get(iter)));
        }
        m.put("addresses", as);
        m.put("storeCredit", credit.grantsOf(c.getId()));
        List<Object> gs = new ArrayList<Object>();
        List<GiftCard> cards = giftCards.findByCustomerId(c.getId());
        for (int iter = 0; iter < cards.size(); iter++) {
            GiftCard g = cards.get(iter);
            Map<String, Object> gm = new LinkedHashMap<String, Object>();
            gm.put("lastFour", g.getLastFour());
            gm.put("balanceCents", Long.valueOf(g.getBalanceCents()));
            gm.put("recipientEmail", g.getRecipientEmail());
            gm.put("message", g.getMessage());
            gs.add(gm);
        }
        m.put("giftCards", gs);
        LoyaltyMember lm = members.findByStoreIdAndCustomerId(c.getStoreId(), c.getId());
        if (lm != null) {
            Map<String, Object> l = new LinkedHashMap<String, Object>();
            l.put("points", Long.valueOf(lm.getBalance()));
            l.put("lifetime", Long.valueOf(lm.getLifetime()));
            l.put("referralCode", lm.getReferralCode());
            m.put("loyalty", l);
        }
        return m;
    }

    private static Map<String, Object> order(StoreOrder o) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("number", Integer.valueOf(o.getNumber()));
        m.put("status", o.getStatus());
        m.put("totalCents", Long.valueOf(o.getTotalCents()));
        m.put("refundedCents", Long.valueOf(o.getRefundedCents()));
        m.put("currency", o.getCurrency());
        m.put("email", o.getEmail());
        m.put("phone", o.getPhone());
        m.put("shipName", o.getShipName());
        m.put("shipLine1", o.getShipLine1());
        m.put("shipCity", o.getShipCity());
        m.put("shipPostal", o.getShipPostal());
        m.put("shipCountry", o.getShipCountry());
        m.put("note", o.getNote());
        m.put("createdAt", o.getCreatedAt() == null ? "" : o.getCreatedAt().toString());
        return m;
    }

    private static Map<String, Object> address(CustomerAddress a) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", a.getName());
        m.put("line1", a.getLine1());
        m.put("line2", a.getLine2());
        m.put("city", a.getCity());
        m.put("region", a.getRegion());
        m.put("postal", a.getPostal());
        m.put("country", a.getCountry());
        m.put("phone", a.getPhone());
        return m;
    }

    private static Map<String, Object> review(Review r) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("storeId", r.getStoreId());
        m.put("productId", r.getProductId());
        m.put("rating", Integer.valueOf(r.getRating()));
        m.put("title", r.getTitle());
        m.put("body", r.getBody());
        m.put("status", r.getStatus());
        return m;
    }

    // -------------------------------------------------------------- erasure

    /// Erases one customer's personal data at the store (see the class
    /// comment). OWNER only.
    @Transactional(rollbackFor = IOException.class)
    public CustomerDto erase(String username, String storeId, String customerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        Customer c = mine(storeId, customerId);
        String account = c.getUsername() == null ? "" : c.getUsername();
        List<StoreOrder> os = orders.findByCustomerIdOrderByCreatedAtDesc(c.getId());
        for (int iter = 0; iter < os.size(); iter++) {
            StoreOrder o = os.get(iter);
            o.setEmail("");
            o.setPhone("");
            o.setUsername("");
            o.setShipName("");
            o.setShipLine1("");
            o.setShipLine2("");
            o.setShipCity("");
            o.setShipPostal("");
            o.setShipPhone("");
            o.setShipLat(0);
            o.setShipLng(0);
            o.setNote("");
            orders.save(o);
            ReviewRequest rr = reviewRequests.findByOrderId(o.getId());
            if (rr != null) {
                reviewRequests.delete(rr);
            }
        }
        List<CustomerAddress> as = addresses.findByCustomerId(c.getId());
        for (int iter = 0; iter < as.size(); iter++) {
            addresses.delete(as.get(iter));
        }
        List<GiftCard> cards = giftCards.findByCustomerId(c.getId());
        for (int iter = 0; iter < cards.size(); iter++) {
            GiftCard g = cards.get(iter);
            g.setRecipientEmail("");
            g.setRecipientName("");
            g.setMessage("");
            giftCards.save(g);
        }
        if (account.length() > 0) {
            List<Review> rs = reviews.findByUsernameOrderByCreatedAtDesc(account);
            for (int iter = 0; iter < rs.size(); iter++) {
                if (storeId.equals(rs.get(iter).getStoreId())) {
                    reviews.delete(rs.get(iter));
                }
            }
            List<Wishlist> ws = wishlist.findByUsernameOrderByCreatedAtDesc(account);
            for (int iter = 0; iter < ws.size(); iter++) {
                if (storeId.equals(ws.get(iter).getStoreId())) {
                    wishlist.delete(ws.get(iter));
                }
            }
            views.deleteByStoreIdAndUsername(storeId, account);
        }
        LoyaltyMember lm = members.findByStoreIdAndCustomerId(storeId, c.getId());
        if (lm != null) {
            lm.setUsername("");
            lm.setEmail("");
            lm.setPhone("");
            members.save(lm);
        }
        c = customers.findById(c.getId());
        c.setUsername("");
        c.setEmail("");
        c.setPhone("");
        c.setFirstName(ERASED);
        c.setLastName("");
        c.setNote("");
        c.setTags("");
        c.setAcceptsMarketing(false);
        c.setUpdatedAt(Instant.now());
        customers.save(c);
        List<DataRequest> open = requests.findByCustomerId(c.getId());
        for (int iter = 0; iter < open.size(); iter++) {
            DataRequest d = open.get(iter);
            if (OPEN.equals(d.getStatus())) {
                close(d, username);
            }
            // The request outlives the data, under no name.
            d.setUsername("");
            requests.save(d);
        }
        audit(storeId, username, "CUSTOMER_ERASED", c.getId(), "Personal data erased");
        // Every installed app holds its own copy; each is told to erase it.
        apps.customerRedact(storeId, c.getId());
        return Customers.dto(c, 0);
    }

    // ------------------------------------------------------------- requests

    /// A shopper asks a store for their data (`EXPORT`) or its erasure
    /// (`ERASURE`). An open request of the same kind is answered instead of a
    /// second one.
    @Transactional(rollbackFor = IOException.class)
    public DataRequestDto request(String me, String storeId, String kind) throws IOException {
        if (me == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        if (!EXPORT.equals(kind) && !ERASURE.equals(kind)) {
            throw new ResponseStatusException(400, "Ask for EXPORT or ERASURE");
        }
        Customer c = storeId == null ? null : customers.findByStoreIdAndUsername(storeId, me);
        if (c == null) {
            throw new ResponseStatusException(404, "That store holds no customer record for you");
        }
        DataRequest d = requests.findByStoreIdAndUsernameAndKindAndStatus(storeId, me, kind, OPEN);
        if (d == null) {
            d = new DataRequest();
            d.setId(Ids.next());
            d.setStoreId(storeId);
            d.setCustomerId(c.getId());
            d.setUsername(me);
            d.setKind(kind);
            d.setStatus(OPEN);
            d.setCreatedAt(Instant.now());
            requests.insert(d);
            if (EXPORT.equals(kind)) {
                // Installed apps answer for what they hold of the customer.
                apps.customerDataRequest(storeId, c.getId(), d.getId());
            }
        }
        return dto(d, c);
    }

    @Transactional(readOnly = true)
    public List<DataRequestDto> mine(String me) {
        if (me == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        List<DataRequest> rows = requests.findByUsernameOrderByCreatedAtDesc(me);
        List<DataRequestDto> out = new ArrayList<DataRequestDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter), customers.findById(rows.get(iter).getCustomerId())));
        }
        return out;
    }

    /// The store's requests, open ones first.
    @Transactional(readOnly = true)
    public List<DataRequestDto> forStore(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<DataRequest> rows = requests.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<DataRequestDto> open = new ArrayList<DataRequestDto>();
        List<DataRequestDto> done = new ArrayList<DataRequestDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            DataRequest d = rows.get(iter);
            DataRequestDto dto = dto(d, customers.findById(d.getCustomerId()));
            (OPEN.equals(d.getStatus()) ? open : done).add(dto);
        }
        open.addAll(done);
        return open;
    }

    /// Marks an export request answered. An erasure is completed by erasing.
    @Transactional(rollbackFor = IOException.class)
    public DataRequestDto complete(String username, String storeId, String requestId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        DataRequest d = requestId == null ? null : requests.findById(requestId);
        if (d == null || !storeId.equals(d.getStoreId())) {
            throw new ResponseStatusException(404, "No such request");
        }
        if (ERASURE.equals(d.getKind()) && OPEN.equals(d.getStatus())) {
            throw new ResponseStatusException(409, "Erase the customer to complete this request");
        }
        if (OPEN.equals(d.getStatus())) {
            close(d, username);
            requests.save(d);
            audit(storeId, username, "DATA_REQUEST_DONE", d.getCustomerId(), d.getKind());
        }
        return dto(d, customers.findById(d.getCustomerId()));
    }

    private static void close(DataRequest d, String by) {
        d.setStatus(DONE);
        d.setDoneAt(Instant.now());
        d.setDoneBy(Codes.cut(by, 190));
    }

    private DataRequestDto dto(DataRequest d, Customer c) {
        Store s = stores.findById(d.getStoreId());
        return new DataRequestDto(d.getId(), d.getStoreId(), s == null ? "" : s.getName(),
                d.getCustomerId(), c == null ? "" : name(c), d.getKind(), d.getStatus(),
                d.getCreatedAt().toEpochMilli(), d.getDoneAt().toEpochMilli());
    }

    static String name(Customer c) {
        String n = (c.getFirstName() + " " + c.getLastName()).trim();
        return n.length() > 0 ? n : c.getEmail();
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
}
