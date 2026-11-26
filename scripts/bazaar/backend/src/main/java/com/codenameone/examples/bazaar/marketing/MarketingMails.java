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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.ProductView;
import com.codenameone.examples.bazaar.domain.ReviewRequest;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.notify.ShopperLanguage;
import com.codenameone.examples.bazaar.repository.CartLineRepository;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ProductViewRepository;
import com.codenameone.examples.bazaar.repository.ReviewRequestRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// The mails that go out by themselves: browse abandonment and the review
/// request some days after delivery. (Abandoned carts are [AbandonedCarts];
/// scheduled gift cards are
/// [com.codenameone.examples.bazaar.giftcard.GiftCards#deliverDue].)
///
/// **Browse abandonment.** The storefront and the app record the last
/// product a signed-in shopper looked at in a store ([#viewed]), one row per
/// shopper and store. From an hour after the view, for a day, the sweep
/// mails them about it, but only when all of these hold: they accept
/// marketing from the store and have not unsubscribed from its mail; their
/// cart there is empty (a cart has its own reminder); they have ordered
/// nothing there since the view; and no browse mail went to them in the last
/// [#THROTTLE_MILLIS]. The mail carries a signed unsubscribe link.
///
/// **Review requests.** A delivery schedules one per order
/// ([Emails#scheduleReview]); the sweep mails it when due with a signed link
/// to [ReviewLinks], where the shopper rates each product without signing in.
@Component
public class MarketingMails {
    /// A view becomes a reminder after an hour...
    static final long QUIET_MILLIS = 3600000L;
    /// ...and stops being one after a day.
    static final long STALE_MILLIS = 86400000L;
    /// At most one browse mail a week to a shopper at a store.
    public static final long THROTTLE_MILLIS = 7L * 86400000L;

    private final ProductViewRepository views;
    private final ReviewRequestRepository requests;
    private final CustomerRepository customers;
    private final CartRepository carts;
    private final CartLineRepository cartLines;
    private final StoreOrderRepository orders;
    private final ProductRepository products;
    private final Campaigns campaigns;
    private final Emails emails;
    private final ShopperLanguage language;
    private final SiteUrls urls;

    public MarketingMails(ProductViewRepository views, ReviewRequestRepository requests,
            CustomerRepository customers, CartRepository carts, CartLineRepository cartLines,
            StoreOrderRepository orders, ProductRepository products, Campaigns campaigns,
            Emails emails, ShopperLanguage language, SiteUrls urls) {
        this.views = views;
        this.requests = requests;
        this.customers = customers;
        this.carts = carts;
        this.cartLines = cartLines;
        this.orders = orders;
        this.products = products;
        this.campaigns = campaigns;
        this.emails = emails;
        this.language = language;
        this.urls = urls;
    }

    /// Records that `username` looked at `productId` in `storeId`. A guest
    /// (null) is not recorded: there is nobody to write to. Never throws; a
    /// product page must not fail over it.
    public void viewed(String storeId, String username, String productId) {
        if (username == null || username.length() == 0 || storeId == null || productId == null) {
            return;
        }
        try {
            record(storeId, username, productId, Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("[browse] the view was not recorded: " + failed);
        }
    }

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.repository.StoreRepository stores;

    /// [#viewed] for the storefront's product page, which knows the store and
    /// product by their handles.
    public void viewedByHandle(String storeHandle, String productHandle, String username) {
        if (username == null || storeHandle == null || productHandle == null) {
            return;
        }
        try {
            com.codenameone.examples.bazaar.domain.Store s = stores.findByHandle(storeHandle);
            Product p = s == null ? null : products.findByStoreIdAndHandle(s.getId(), productHandle);
            if (p != null) {
                viewed(s.getId(), username, p.getId());
            }
        } catch (RuntimeException failed) {
            System.err.println("[browse] the view was not recorded: " + failed);
        }
    }

    /// [#viewed] at a chosen time; public so that tests can set the clock.
    @Transactional(rollbackFor = IOException.class)
    public void record(String storeId, String username, String productId, Instant at)
            throws IOException {
        ProductView v = views.findByStoreIdAndUsername(storeId, username);
        if (v == null) {
            v = new ProductView();
            v.setId(Ids.next());
            v.setStoreId(storeId);
            v.setUsername(username);
            v.setProductId(productId);
            v.setViewedAt(at);
            views.insert(v);
        } else {
            v.setProductId(productId);
            v.setViewedAt(at);
            views.save(v);
        }
    }

    @Scheduled(fixedDelay = 600000, initialDelay = 120000, lock = "bazaar-browse-abandonment")
    public void browseJob() throws IOException {
        sweepBrowse(System.currentTimeMillis());
    }

    /// Mails every shopper whose view qualifies at `now` (see the class
    /// comment); answers how many mails went.
    public int sweepBrowse(long now) throws IOException {
        List<ProductView> due = views.findByViewedAtLessThan(Instant.ofEpochMilli(now - QUIET_MILLIS));
        int sent = 0;
        for (int iter = 0; iter < due.size(); iter++) {
            ProductView v = due.get(iter);
            if (shouldMail(v.getViewedAt().toEpochMilli(), v.getMailedAt().toEpochMilli(), now)
                    && browseOne(v, now)) {
                sent++;
            }
        }
        return sent;
    }

    /// The timing half of the rule: the view is between an hour and a day
    /// old, and the last browse mail is older than the throttle and older
    /// than the view (the same view is never mailed twice).
    public static boolean shouldMail(long viewedAt, long mailedAt, long now) {
        long age = now - viewedAt;
        if (age < QUIET_MILLIS || age > STALE_MILLIS) {
            return false;
        }
        return mailedAt <= 0 || (now - mailedAt >= THROTTLE_MILLIS && mailedAt < viewedAt);
    }

    private boolean browseOne(ProductView v, long now) throws IOException {
        Customer c = customers.findByStoreIdAndUsername(v.getStoreId(), v.getUsername());
        if (c == null || !c.isAcceptsMarketing()
                || !com.codename1.backend.mail.Mail.isAddress(c.getEmail())
                || !campaigns.consented(v.getStoreId(), c.getEmail(), Campaigns.EMAIL)) {
            return false;
        }
        Cart cart = carts.findByStoreIdAndUsername(v.getStoreId(), v.getUsername());
        if (cart != null && !cartLines.findByCartId(cart.getId()).isEmpty()) {
            return false;
        }
        List<StoreOrder> mine = orders.findByUsernameOrderByCreatedAtDesc(v.getUsername());
        for (int iter = 0; iter < mine.size(); iter++) {
            StoreOrder o = mine.get(iter);
            if (v.getStoreId().equals(o.getStoreId()) && o.getCreatedAt() != null
                    && !o.getCreatedAt().isBefore(v.getViewedAt())) {
                return false;
            }
        }
        Product p = products.findById(v.getProductId());
        if (p == null || !"ACTIVE".equals(p.getStatus())) {
            return false;
        }
        if (views.claimMail(v.getId(), v.getMailedAt(), Instant.ofEpochMilli(now)) != 1) {
            return false;
        }
        String unsubscribe = urls.absolute(null, "/api/shop/m/b/" + v.getId() + "?s="
                + Tokens.sign(campaigns.secret(v.getStoreId()), "b:" + v.getId()));
        return emails.browsed(v.getStoreId(), c.getEmail(),
                language.shopper(v.getStoreId(), c.getUsername(), c.getEmail()), p, unsubscribe);
    }

    /// The browse mail's unsubscribe link: opts the shopper's address out of
    /// the store's mail and their customer record out of marketing. Answers
    /// whether the signature was good.
    @Transactional(rollbackFor = IOException.class)
    public boolean unsubscribeBrowse(String viewId, String signature) throws IOException {
        ProductView v = viewId == null || viewId.length() > 32 ? null : views.findById(viewId);
        if (v == null || !Tokens.verify(campaigns.secret(v.getStoreId()), "b:" + viewId, signature)) {
            return false;
        }
        Customer c = customers.findByStoreIdAndUsername(v.getStoreId(), v.getUsername());
        if (c != null) {
            if (com.codename1.backend.mail.Mail.isAddress(c.getEmail())) {
                campaigns.setConsent(v.getStoreId(), c.getEmail(), Campaigns.EMAIL,
                        Campaigns.UNSUBSCRIBED, "browse");
            }
            c = customers.findById(c.getId());
            if (c.isAcceptsMarketing()) {
                c.setAcceptsMarketing(false);
                c.setUpdatedAt(Instant.now());
                customers.save(c);
            }
        }
        return true;
    }

    @Scheduled(fixedDelay = 900000, initialDelay = 180000, lock = "bazaar-review-requests")
    public void reviewJob() throws IOException {
        sweepReviews(System.currentTimeMillis());
    }

    /// Mails every review request due by `now`; answers how many went.
    public int sweepReviews(long now) throws IOException {
        List<ReviewRequest> due = requests.findBySentAtAndDueAtLessThan(Instant.EPOCH,
                Instant.ofEpochMilli(now + 1));
        int sent = 0;
        for (int iter = 0; iter < due.size(); iter++) {
            ReviewRequest r = due.get(iter);
            if (requests.claim(r.getId(), Instant.EPOCH, Instant.ofEpochMilli(now)) != 1) {
                continue;
            }
            if (emails.reviewRequest(r, reviewLink(r))) {
                sent++;
            }
        }
        return sent;
    }

    /// The signed one-click link of a review request.
    public String reviewLink(ReviewRequest r) throws IOException {
        return urls.absolute(null, ReviewLinks.PATH + r.getId() + "?s="
                + Tokens.sign(campaigns.secret(r.getStoreId()), "r:" + r.getId()));
    }

    /// The review request `requestId`, when `signature` is its link's.
    public ReviewRequest verified(String requestId, String signature) throws IOException {
        ReviewRequest r = requestId == null || requestId.length() > 32 ? null
                : requests.findById(requestId);
        if (r == null || !Tokens.verify(campaigns.secret(r.getStoreId()), "r:" + requestId,
                signature)) {
            return null;
        }
        return r;
    }
}
