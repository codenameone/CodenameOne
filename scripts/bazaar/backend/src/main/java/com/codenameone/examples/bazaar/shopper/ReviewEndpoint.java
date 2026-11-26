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
package com.codenameone.examples.bazaar.shopper;

import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.ReviewApiServer;
import com.codenameone.examples.bazaar.api.ReviewDto;
import com.codenameone.examples.bazaar.api.ReviewPageDto;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Product reviews. A shopper writes one per product; it waits as PENDING
/// until the store publishes or rejects it, and only published reviews are
/// shown or counted in the product's rating. A review is `verified` when its
/// author has an order, not cancelled, with that product on it.
@Component
public class ReviewEndpoint implements ReviewApiServer {
    public static final String PENDING = "PENDING";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String REJECTED = "REJECTED";
    static final int MAX_PAGE = 50;

    private final ReviewRepository rows;
    private final ProductRepository products;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final StoreAccess access;
    private final com.codenameone.examples.bazaar.catalog.Schedules schedules;

    public ReviewEndpoint(ReviewRepository rows, ProductRepository products,
            StoreOrderRepository orders, OrderLineRepository lines, StoreAccess access,
            com.codenameone.examples.bazaar.catalog.Schedules schedules) {
        this.schedules = schedules;
        this.rows = rows;
        this.products = products;
        this.orders = orders;
        this.lines = lines;
        this.access = access;
    }

    /// A page of a product's published reviews, newest first, with the
    /// count of each star rating (index 0 is one star) over all of them.
    @Override
    @Transactional(readOnly = true)
    public ReviewPageDto reviews(String productId, int page, int size) throws Exception {
        Product p = productId == null ? null : products.findById(productId);
        // Outside its storefront window a product has no public reviews and
        // takes none, exactly as a draft ([Schedules#onlineNow]).
        if (!schedules.onlineNow(p)) {
            throw new ResponseStatusException(404, "No such product");
        }
        int pageSize = size <= 0 ? 10 : Math.min(size, MAX_PAGE);
        int from = Math.max(page, 0) * pageSize;
        List<Review> all = rows.findByProductIdAndStatusOrderByCreatedAtDesc(productId, PUBLISHED);
        int[] stars = new int[5];
        long total = 0;
        for (int iter = 0; iter < all.size(); iter++) {
            int r = all.get(iter).getRating();
            if (r >= 1 && r <= 5) {
                stars[r - 1]++;
                total += r;
            }
        }
        List<Integer> histogram = new ArrayList<Integer>();
        int counted = 0;
        for (int iter = 0; iter < 5; iter++) {
            histogram.add(Integer.valueOf(stars[iter]));
            counted += stars[iter];
        }
        List<ReviewDto> shown = new ArrayList<ReviewDto>();
        for (int iter = from; iter < all.size() && iter < from + pageSize; iter++) {
            shown.add(dto(all.get(iter)));
        }
        int averageTenths = counted == 0 ? 0 : (int) ((total * 10 + counted / 2) / counted);
        return new ReviewPageDto(productId, counted, averageTenths, histogram, Math.max(page, 0),
                pageSize, shown);
    }

    @Override
    @Transactional
    public ReviewDto submit(String productId, ReviewDto review) throws Exception {
        String user = Caller.name();
        Product p = productId == null ? null : products.findById(productId);
        // Outside its storefront window a product has no public reviews and
        // takes none, exactly as a draft ([Schedules#onlineNow]).
        if (!schedules.onlineNow(p)) {
            throw new ResponseStatusException(404, "No such product");
        }
        if (review == null || review.rating() < 1 || review.rating() > 5) {
            throw new ResponseStatusException(400, "A rating is one to five stars");
        }
        if (rows.findByProductIdAndUsername(productId, user) != null) {
            throw new ResponseStatusException(409, "You already reviewed this product");
        }
        Review r = new Review();
        r.setId(Ids.next());
        r.setStoreId(p.getStoreId());
        r.setProductId(productId);
        r.setUsername(user);
        String author = review.authorName() == null ? "" : review.authorName().trim();
        if (author.length() == 0) {
            int at = user.indexOf('@');
            author = at > 0 ? user.substring(0, at) : user;
        }
        r.setAuthorName(Text.optional(author.length() > 80 ? author.substring(0, 80) : author,
                80, "the name"));
        r.setRating(review.rating());
        r.setTitle(Text.optional(review.title(), 120, "the title"));
        r.setBody(Text.optional(review.body(), 2000, "the review"));
        r.setStatus(PENDING);
        r.setVerified(bought(user, productId));
        r.setCreatedAt(Instant.now());
        rows.insert(r);
        return dto(r);
    }

    private boolean bought(String user, String productId) {
        List<StoreOrder> mine = orders.findByUsernameOrderByCreatedAtDesc(user);
        for (int iter = 0; iter < mine.size(); iter++) {
            StoreOrder o = mine.get(iter);
            if (Orders.CANCELLED.equals(o.getStatus())) {
                continue;
            }
            List<OrderLine> ls = lines.findByOrderId(o.getId());
            for (int l = 0; l < ls.size(); l++) {
                if (productId.equals(ls.get(l).getProductId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> storeReviews(String storeId, String status) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.MANAGERS);
        List<Review> all = rows.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<ReviewDto> out = new ArrayList<ReviewDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Review r = all.get(iter);
            if (status == null || status.length() == 0 || status.equals(r.getStatus())) {
                out.add(dto(r));
            }
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public ReviewDto publish(String storeId, String reviewId) throws Exception {
        Review r = moderated(storeId, reviewId);
        if (!PUBLISHED.equals(r.getStatus())) {
            r.setStatus(PUBLISHED);
            rows.save(r);
            products.addRating(r.getProductId(), r.getRating(), 1);
        }
        return dto(r);
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public ReviewDto reject(String storeId, String reviewId) throws Exception {
        Review r = moderated(storeId, reviewId);
        if (!REJECTED.equals(r.getStatus())) {
            if (PUBLISHED.equals(r.getStatus())) {
                products.addRating(r.getProductId(), -r.getRating(), -1);
            }
            r.setStatus(REJECTED);
            rows.save(r);
        }
        return dto(r);
    }

    private Review moderated(String storeId, String reviewId) throws IOException {
        access.requireStaff(Caller.name(), storeId, StoreAccess.MANAGERS);
        Review r = reviewId == null ? null : rows.findById(reviewId);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such review");
        }
        return r;
    }

    static ReviewDto dto(Review r) {
        return new ReviewDto(r.getId(), r.getProductId(), r.getAuthorName(), r.getRating(),
                r.getTitle(), r.getBody(), r.getStatus(), r.isVerified(),
                r.getCreatedAt().toEpochMilli());
    }
}
