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
package com.codenameone.examples.bazaar.web.account;

import com.codename1.appkit.server.Text;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.ClubMembershipDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentSummaryDto;
import com.codenameone.examples.bazaar.api.LoyaltyMemberDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderEventDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnRequestDto;
import com.codenameone.examples.bazaar.api.SavedAddressDto;
import com.codenameone.examples.bazaar.api.WishlistItemDto;
import com.codenameone.examples.bazaar.club.Clubs;
import com.codenameone.examples.bazaar.docs.Documents;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.LoyaltyLedger;
import com.codenameone.examples.bazaar.domain.LoyaltyMember;
import com.codenameone.examples.bazaar.domain.LoyaltyProgram;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.domain.StockAlert;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.TrackingEvent;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyLedgerRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyMemberRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyProgramRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.StockAlertRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.TrackingEventRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.shopper.AddressEndpoint;
import com.codenameone.examples.bazaar.shopper.ReviewEndpoint;
import com.codenameone.examples.bazaar.shopper.WishlistEndpoint;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.SiteUrls;
import com.codenameone.examples.bazaar.web.StorefrontPages;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What the account pages show and do, for the signed-in shopper `user`.
///
/// Every read and every change names its owner: an order, address, review or
/// alert of someone else answers 404, the same as one that does not exist, so a
/// page never confirms whose ids are whose. Reads never create anything -- the
/// loyalty page lists the programmes a shopper is in, and joining is a post.
@Component
public class AccountData {
    /// Orders on one page of the list.
    public static final int PAGE = 20;

    private final Orders orders;
    private final Returns returns;
    private final StoreOrderRepository orderRows;
    private final StoreRepository storeRows;
    private final TrackingEventRepository tracking;
    private final Documents documents;
    private final AddressEndpoint addressBook;
    private final WishlistEndpoint wishlist;
    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final CustomerRepository customers;
    private final LoyaltyProgramRepository programs;
    private final LoyaltyMemberRepository members;
    private final LoyaltyLedgerRepository ledger;
    private final Loyalty loyalty;
    private final Clubs clubs;
    private final StockAlertRepository alerts;
    private final Campaigns campaigns;
    private final SiteUrls urls;

    public AccountData(Orders orders, Returns returns, StoreOrderRepository orderRows,
            StoreRepository storeRows, TrackingEventRepository tracking, Documents documents,
            AddressEndpoint addressBook, WishlistEndpoint wishlist, ReviewRepository reviews,
            ProductRepository products, VariantRepository variants, CustomerRepository customers,
            LoyaltyProgramRepository programs, LoyaltyMemberRepository members,
            LoyaltyLedgerRepository ledger, Loyalty loyalty, Clubs clubs,
            StockAlertRepository alerts, Campaigns campaigns, SiteUrls urls) {
        this.orders = orders;
        this.returns = returns;
        this.orderRows = orderRows;
        this.storeRows = storeRows;
        this.tracking = tracking;
        this.documents = documents;
        this.addressBook = addressBook;
        this.wishlist = wishlist;
        this.reviews = reviews;
        this.products = products;
        this.variants = variants;
        this.customers = customers;
        this.programs = programs;
        this.members = members;
        this.ledger = ledger;
        this.loyalty = loyalty;
        this.clubs = clubs;
        this.alerts = alerts;
        this.campaigns = campaigns;
        this.urls = urls;
    }

    private String storeName(Map<String, Store> seen, String storeId) {
        Store s = store(seen, storeId);
        return s == null ? "" : s.getName();
    }

    private Store store(Map<String, Store> seen, String storeId) {
        if (!seen.containsKey(storeId)) {
            seen.put(storeId, storeRows.findById(storeId));
        }
        return seen.get(storeId);
    }

    // --------------------------------------------------------------- orders

    /// Page `page` (from 0) of the shopper's orders, newest first. One more row
    /// than a page is read to know whether there is a next one.
    public void orders(String user, int page, AccountView v) {
        int p = Math.max(0, page);
        List<StoreOrder> rows = orderRows.findByUsernameOrderByCreatedAtDesc(user,
                new PageRequest(p * PAGE, PAGE + 1));
        Map<String, Store> seen = new HashMap<String, Store>();
        for (int iter = 0; iter < rows.size() && iter < PAGE; iter++) {
            StoreOrder o = rows.get(iter);
            if (Orders.isUnconvertedDraft(o)) {
                continue;
            }
            AccountItem i = new AccountItem();
            i.id = o.getId();
            i.title = "Order #" + o.getNumber();
            i.detail = storeName(seen, o.getStoreId());
            i.meta = Days.iso(o.getCreatedAt().toEpochMilli());
            i.status = status(o.getStatus(), o.getFulfilment());
            i.amount = Money.format(o.getTotalCents(), o.getCurrency());
            i.url = "/account/orders/" + o.getId();
            v.items.add(i);
        }
        v.pageLabel = "Page " + (p + 1);
        if (p > 0) {
            v.prevUrl = "/account/orders?p=" + (p - 1);
        }
        if (rows.size() > PAGE) {
            v.nextUrl = "/account/orders?p=" + (p + 1);
        }
    }

    static String status(String status, String fulfilment) {
        if (Orders.CANCELLED.equals(status)) {
            return "Cancelled";
        }
        if (Orders.FULFILLED.equals(fulfilment)) {
            return "Shipped";
        }
        if (Orders.PARTIAL.equals(fulfilment)) {
            return "Partly shipped";
        }
        if (Orders.CLOSED.equals(status)) {
            return "Completed";
        }
        return "Processing";
    }

    /// One of the shopper's orders: lines, timeline, shipments with their
    /// tracking events, returns, and what may still be done with it.
    public void order(String user, String orderId, AccountView v) throws IOException {
        OrderDto o = orders.mineDetail(user, orderId);
        String c = o.currency();
        Map<String, Store> seen = new HashMap<String, Store>();
        v.order.id = o.id();
        v.order.title = "Order #" + o.number();
        v.order.detail = storeName(seen, o.storeId());
        v.order.meta = Days.iso(o.createdAt());
        v.order.status = status(o.status(), o.fulfilment());
        v.order.amount = Money.format(o.totalCents(), c);
        AddressDto a = o.address();
        if (a != null) {
            StringBuilder to = new StringBuilder();
            append(to, a.name());
            append(to, a.line1());
            append(to, a.line2());
            append(to, (a.city() + " " + a.region() + " " + a.postal()).trim());
            append(to, a.country());
            v.shipTo = to.toString();
        }
        boolean shipped = false;
        for (int iter = 0; iter < o.lines().size(); iter++) {
            OrderLineDto l = o.lines().get(iter);
            AccountItem i = new AccountItem();
            i.id = l.id();
            i.title = l.title();
            i.detail = l.variantTitle() == null ? "" : l.variantTitle();
            i.quantity = Integer.toString(l.quantity());
            int returnable = Math.max(0, l.fulfilledQty() - l.refundedQty());
            i.max = Integer.toString(returnable);
            i.flag = returnable > 0;
            shipped |= returnable > 0;
            i.amount = Money.format(l.priceCents() * l.quantity(), c);
            v.lines.add(i);
        }
        if (o.events() != null) {
            for (int iter = 0; iter < o.events().size(); iter++) {
                OrderEventDto e = o.events().get(iter);
                AccountItem i = new AccountItem();
                i.title = e.message();
                i.status = e.kind();
                i.meta = Days.iso(e.createdAt());
                v.events.add(i);
            }
        }
        if (o.fulfillments() != null) {
            for (int iter = 0; iter < o.fulfillments().size(); iter++) {
                FulfillmentSummaryDto f = o.fulfillments().get(iter);
                List<TrackingEvent> evs = tracking.findByFulfillmentIdOrderByCreatedAtAsc(f.id());
                String number = f.trackingNumber() == null ? "" : f.trackingNumber();
                for (int e = 0; e < evs.size(); e++) {
                    TrackingEvent t = evs.get(e);
                    AccountItem i = new AccountItem();
                    i.title = t.getMessage();
                    i.status = t.getStatus();
                    i.detail = t.getPlace();
                    i.meta = Days.iso(t.getCreatedAt().toEpochMilli());
                    i.extra = (f.carrier() == null ? "" : f.carrier()) + " " + number;
                    i.url = number.length() == 0 ? "" : "/track/" + SiteUrls.encode(number);
                    v.tracking.add(i);
                }
                if (evs.isEmpty() && number.length() > 0) {
                    AccountItem i = new AccountItem();
                    i.title = "Shipped";
                    i.status = f.status();
                    i.extra = (f.carrier() == null ? "" : f.carrier()) + " " + number;
                    i.url = "/track/" + SiteUrls.encode(number);
                    v.tracking.add(i);
                }
            }
        }
        List<ReturnDto> rs = returns.mine(user, orderId);
        for (int iter = 0; iter < rs.size(); iter++) {
            ReturnDto r = rs.get(iter);
            AccountItem i = new AccountItem();
            i.id = r.id();
            i.title = r.reason();
            i.status = r.status();
            i.meta = Days.iso(r.createdAt());
            int n = 0;
            for (int l = 0; l < r.lines().size(); l++) {
                n += r.lines().get(l).quantity();
            }
            i.quantity = Integer.toString(n);
            i.amount = r.refundedCents() > 0 ? Money.format(r.refundedCents(), c) : "";
            v.returns.add(i);
        }
        String base = "/account/orders/" + o.id();
        v.invoiceUrl = base + "/invoice.pdf";
        v.cancelAction = base + "/cancel";
        v.returnAction = base + "/return";
        v.canCancel = Orders.OPEN.equals(o.status()) && Orders.UNFULFILLED.equals(o.fulfilment());
        v.canReturn = shipped && !Orders.CANCELLED.equals(o.status());
    }

    public HttpServer.Response invoice(String user, String orderId) throws IOException {
        OrderDto o = orders.mineDetail(user, orderId);
        return new HttpServer.Response(200, "application/pdf", documents.invoice(o.storeId(), o.id()))
                .header("Content-Disposition", "inline; filename=\"invoice-" + o.number() + ".pdf\"")
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff");
    }

    public void cancel(String user, String orderId) throws IOException {
        orders.cancelMine(user, orderId);
    }

    /// Asks for a return of the quantities posted as `qty-<line id>`.
    public void requestReturn(String user, String orderId, HttpServer.Request request)
            throws IOException {
        OrderDto o = orders.mineDetail(user, orderId);
        List<FulfilLineDto> wanted = new ArrayList<FulfilLineDto>();
        for (int iter = 0; iter < o.lines().size(); iter++) {
            OrderLineDto l = o.lines().get(iter);
            String q = request.param("qty-" + l.id());
            int n = number(q);
            if (n > 0) {
                wanted.add(new FulfilLineDto(l.id(), l.title(), l.sku(), n));
            }
        }
        if (wanted.isEmpty()) {
            throw new ResponseStatusException(400, "Choose at least one item to return");
        }
        String reason = request.param("reason");
        returns.request(user, orderId, new ReturnRequestDto(
                reason == null || reason.trim().length() == 0 ? "OTHER" : reason.trim(),
                request.param("note") == null ? "" : request.param("note"), wanted));
    }

    static int number(String s) {
        if (s == null) {
            return 0;
        }
        String t = s.trim();
        if (t.length() == 0 || t.length() > 6) {
            return 0;
        }
        int n = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char ch = t.charAt(iter);
            if (ch < '0' || ch > '9') {
                return 0;
            }
            n = n * 10 + (ch - '0');
        }
        return n;
    }

    private static void append(StringBuilder to, String part) {
        if (part != null && part.trim().length() > 0) {
            if (to.length() > 0) {
                to.append(", ");
            }
            to.append(part.trim());
        }
    }

    // ------------------------------------------------------------ addresses

    public void addresses(AccountView v) throws Exception {
        List<SavedAddressDto> all = addressBook.list();
        for (int iter = 0; iter < all.size(); iter++) {
            SavedAddressDto s = all.get(iter);
            AddressDto a = s.address();
            AddressRow r = new AddressRow();
            r.id = s.id();
            r.name = text(a.name());
            r.line1 = text(a.line1());
            r.line2 = text(a.line2());
            r.city = text(a.city());
            r.region = text(a.region());
            r.postal = text(a.postal());
            r.country = text(a.country());
            r.phone = text(a.phone());
            StringBuilder to = new StringBuilder();
            append(to, a.line1());
            append(to, a.line2());
            append(to, (text(a.city()) + " " + text(a.region()) + " " + text(a.postal())).trim());
            append(to, a.country());
            r.summary = to.toString();
            r.isDefault = s.isDefault();
            r.action = "/account/addresses/" + s.id();
            v.addresses.add(r);
        }
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    /// The address a form posted.
    public static AddressDto address(HttpServer.Request request) {
        String name = field(request, "name", 80, true, "the name");
        String line1 = field(request, "line1", 120, true, "the street address");
        String city = field(request, "city", 80, true, "the city");
        String country = field(request, "country", 2, true, "the country");
        return new AddressDto(name, line1, field(request, "line2", 120, false, ""), city,
                field(request, "region", 80, false, ""), field(request, "postal", 20, false, ""),
                upper(country),
                field(request, "phone", 30, false, ""), 0, 0);
    }

    private static String upper(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char ch = s.charAt(iter);
            b.append(ch >= 'a' && ch <= 'z' ? (char) (ch - 32) : ch);
        }
        return b.toString();
    }

    private static String field(HttpServer.Request request, String name, int max,
            boolean required, String label) {
        String v = request.param(name);
        v = v == null ? "" : v.trim();
        if (required && v.length() == 0) {
            throw new ResponseStatusException(400, "Please enter " + label);
        }
        if (v.length() > max) {
            throw new ResponseStatusException(400, "That is too long for " + label);
        }
        return v;
    }

    public void addAddress(HttpServer.Request request) throws Exception {
        addressBook.add(new SavedAddressDto("", address(request),
                "on".equals(request.param("isDefault"))));
    }

    public void updateAddress(String id, HttpServer.Request request) throws Exception {
        addressBook.update(id, new SavedAddressDto(id, address(request),
                "on".equals(request.param("isDefault"))));
    }

    public void removeAddress(String id) throws Exception {
        addressBook.remove(id);
    }

    public void defaultAddress(String id) throws Exception {
        addressBook.makeDefault(id);
    }

    // ------------------------------------------------------------- wishlist

    public void wishlist(AccountView v) throws Exception {
        List<WishlistItemDto> all = wishlist.list();
        Map<String, Store> seen = new HashMap<String, Store>();
        for (int iter = 0; iter < all.size(); iter++) {
            WishlistItemDto w = all.get(iter);
            Store s = store(seen, w.storeId());
            AccountItem i = new AccountItem();
            i.id = w.productId();
            i.title = w.title();
            i.detail = s == null ? "" : s.getName();
            i.amount = Money.format(w.priceCents(), s == null ? "USD" : s.getCurrency());
            i.flag = w.available();
            i.action = "/account/wishlist/" + w.productId() + "/remove";
            if (s != null) {
                i.url = StorefrontPages.storeUrl(s.getHandle()) + "/products/" + SiteUrls.encode(w.handle());
                List<Variant> vs = variants.findByProductIdOrderByPositionAsc(w.productId());
                if (!vs.isEmpty() && w.available()) {
                    i.extra = StorefrontPages.storeUrl(s.getHandle()) + "/cart/add";
                    i.meta = vs.get(0).getId();
                }
            }
            v.items.add(i);
        }
    }

    public void unwish(String productId) throws Exception {
        wishlist.remove(productId);
    }

    // -------------------------------------------------------------- reviews

    public void reviews(String user, AccountView v) {
        List<Review> all = reviews.findByUsernameOrderByCreatedAtDesc(user);
        for (int iter = 0; iter < all.size() && iter < 200; iter++) {
            Review r = all.get(iter);
            Product p = products.findById(r.getProductId());
            AccountItem i = new AccountItem();
            i.id = r.getId();
            i.title = p == null ? "" : p.getTitle();
            i.detail = r.getTitle();
            i.meta = r.getBody();
            i.status = r.getStatus();
            i.rating = Integer.toString(r.getRating());
            i.flag = ReviewEndpoint.PENDING.equals(r.getStatus());
            i.action = "/account/reviews/" + r.getId();
            v.items.add(i);
        }
    }

    private Review pendingReview(String user, String id) {
        Review r = id == null ? null : reviews.findById(id);
        if (r == null || !user.equals(r.getUsername())) {
            throw new ResponseStatusException(404, "No such review");
        }
        if (!ReviewEndpoint.PENDING.equals(r.getStatus())) {
            throw new ResponseStatusException(409, "Only a review awaiting moderation can change");
        }
        return r;
    }

    public void editReview(String user, String id, HttpServer.Request request) {
        Review r = pendingReview(user, id);
        int rating = number(request.param("rating"));
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(400, "A rating is one to five stars");
        }
        r.setRating(rating);
        r.setTitle(Text.optional(request.param("title"), 120, "the title"));
        r.setBody(Text.optional(request.param("body"), 2000, "the review"));
        reviews.save(r);
    }

    public void deleteReview(String user, String id) {
        reviews.delete(pendingReview(user, id));
    }

    // -------------------------------------------------------------- loyalty

    /// The programmes of the stores the shopper is a customer of.
    public void loyalty(String user, HttpServer.Request request, AccountView v) throws IOException {
        List<Customer> mine = customers.findByUsername(user);
        for (int iter = 0; iter < mine.size(); iter++) {
            Customer c = mine.get(iter);
            LoyaltyProgram p = programs.findByStoreId(c.getStoreId());
            Store s = storeRows.findById(c.getStoreId());
            if (p == null || !p.isActive() || s == null) {
                continue;
            }
            LoyaltyCard card = new LoyaltyCard();
            card.storeId = s.getId();
            card.storeName = s.getName();
            card.joinAction = "/account/loyalty/" + s.getId() + "/join";
            LoyaltyMember m = members.findByStoreIdAndCustomerId(s.getId(), c.getId());
            if (m != null) {
                LoyaltyMemberDto d = loyalty.dto(m);
                card.member = true;
                card.balance = Long.toString(d.balance());
                card.tierName = d.tierName();
                card.nextTierName = d.nextTierName();
                if (d.nextTierName() != null && d.nextTierName().length() > 0) {
                    card.toNext = Long.toString(d.toNextTier());
                    card.progress = Long.toString(d.qualifying());
                    card.progressMax = Long.toString(d.qualifying() + d.toNextTier());
                } else {
                    card.progress = "1";
                    card.progressMax = "1";
                }
                card.referralCode = d.referralCode();
                card.shareUrl = urls.absolute(request, StorefrontPages.storeUrl(s.getHandle())
                        + "?ref=" + SiteUrls.encode(d.referralCode()));
                card.qrPath = QrSvg.path(d.cardPayload());
                card.qrBox = QrSvg.box(d.cardPayload());
                List<LoyaltyLedger> h = ledger.findByMemberIdOrderByCreatedAtDesc(m.getId());
                for (int e = 0; e < h.size() && e < 50; e++) {
                    LoyaltyLedger l = h.get(e);
                    AccountItem i = new AccountItem();
                    i.title = l.getNote();
                    i.status = l.getKind();
                    i.amount = (l.getPoints() > 0 ? "+" : "") + l.getPoints();
                    i.meta = Days.iso(l.getCreatedAt().toEpochMilli());
                    card.history.add(i);
                }
            }
            v.cards.add(card);
        }
    }

    /// Joins a store's programme; only one the shopper is already a customer of.
    public void join(String user, String storeId) throws IOException {
        Customer c = storeId == null ? null : customers.findByStoreIdAndUsername(storeId, user);
        LoyaltyProgram p = c == null ? null : programs.findByStoreId(storeId);
        if (p == null || !p.isActive()) {
            throw new ResponseStatusException(404, "No such programme");
        }
        loyalty.me(user, storeId);
    }

    // ---------------------------------------------------------------- clubs

    public void clubs(String user, AccountView v) throws IOException {
        List<Customer> mine = customers.findByUsername(user);
        for (int iter = 0; iter < mine.size(); iter++) {
            Customer c = mine.get(iter);
            Store s = storeRows.findById(c.getStoreId());
            if (s == null) {
                continue;
            }
            List<ClubMembershipDto> ms = clubs.mine(user, s.getId());
            for (int m = 0; m < ms.size(); m++) {
                ClubMembershipDto d = ms.get(m);
                AccountItem i = new AccountItem();
                i.id = d.id();
                i.title = d.clubName();
                i.detail = s.getName();
                i.status = d.status();
                i.meta = "Joined " + Days.iso(d.joinedAt());
                i.extra = d.renewsAt() > 0 ? Days.iso(d.renewsAt()) : "";
                v.items.add(i);
            }
        }
    }

    // --------------------------------------------------------- stock alerts

    public void alerts(String user, AccountView v) {
        List<StockAlert> all = alerts.findByUsernameOrderByCreatedAtDesc(user);
        Map<String, Store> seen = new HashMap<String, Store>();
        for (int iter = 0; iter < all.size() && iter < 200; iter++) {
            StockAlert a = all.get(iter);
            Variant var = variants.findById(a.getVariantId());
            Product p = var == null ? null : products.findById(var.getProductId());
            AccountItem i = new AccountItem();
            i.id = a.getId();
            i.title = p == null ? "" : p.getTitle();
            i.detail = var == null ? "" : var.getTitle();
            i.extra = storeName(seen, a.getStoreId());
            i.status = a.getStatus();
            i.meta = Days.iso(a.getCreatedAt().toEpochMilli());
            i.flag = "PENDING".equals(a.getStatus());
            i.action = "/account/alerts/" + a.getId() + "/delete";
            v.items.add(i);
        }
    }

    public void deleteAlert(String user, String id) {
        StockAlert a = id == null ? null : alerts.findById(id);
        if (a == null || !user.equals(a.getUsername())) {
            throw new ResponseStatusException(404, "No such alert");
        }
        alerts.delete(a);
    }

    // ------------------------------------------------------------ marketing

    /// Each store's e-mail and SMS consent, as sending reads it: a campaign goes
    /// to a customer who accepts marketing and has not opted out of the channel.
    public void marketing(String user, AccountView v) {
        List<Customer> mine = customers.findByUsername(user);
        for (int iter = 0; iter < mine.size(); iter++) {
            Customer c = mine.get(iter);
            Store s = storeRows.findById(c.getStoreId());
            if (s == null) {
                continue;
            }
            AccountItem i = new AccountItem();
            i.id = s.getId();
            i.title = s.getName();
            String email = c.getEmail() == null || c.getEmail().length() == 0 ? user : c.getEmail();
            String phone = c.getPhone() == null ? "" : c.getPhone();
            i.detail = email;
            i.meta = phone;
            i.flag = c.isAcceptsMarketing() && campaigns.consented(s.getId(), email, Campaigns.EMAIL);
            i.disabled = phone.length() == 0;
            i.flag2 = !i.disabled && c.isAcceptsMarketing()
                    && campaigns.consented(s.getId(), phone, Campaigns.SMS);
            i.action = "/account/marketing/" + s.getId();
            v.items.add(i);
        }
    }

    public void saveMarketing(String user, String storeId, boolean email, boolean sms)
            throws IOException {
        Customer c = storeId == null ? null : customers.findByStoreIdAndUsername(storeId, user);
        if (c == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        String address = c.getEmail() == null || c.getEmail().length() == 0 ? user : c.getEmail();
        String phone = c.getPhone() == null ? "" : c.getPhone();
        boolean smsOn = sms && phone.length() > 0;
        campaigns.setConsent(storeId, address, Campaigns.EMAIL,
                email ? Campaigns.SUBSCRIBED : Campaigns.UNSUBSCRIBED, "account");
        if (phone.length() > 0) {
            campaigns.setConsent(storeId, phone, Campaigns.SMS,
                    smsOn ? Campaigns.SUBSCRIBED : Campaigns.UNSUBSCRIBED, "account");
        }
        c.setAcceptsMarketing(email || smsOn);
        c.setUpdatedAt(Instant.now());
        customers.save(c);
    }
}
