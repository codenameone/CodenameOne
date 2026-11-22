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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.WishlistApiServer;
import com.codenameone.examples.bazaar.api.WishlistItemDto;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.domain.Wishlist;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.repository.WishlistRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// A shopper's wishlist: products saved for later, across stores. A product
/// archived since it was saved stays listed, marked unavailable; one whose
/// storefront window is closed ([Schedules]) is left out until it opens,
/// so a scheduled launch is not shown early through a wishlist.
@Component
public class WishlistEndpoint implements WishlistApiServer {
    static final int MAX_ITEMS = 500;
    private final WishlistRepository rows;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final com.codenameone.examples.bazaar.catalog.Schedules schedules;

    public WishlistEndpoint(WishlistRepository rows, ProductRepository products,
            VariantRepository variants,
            com.codenameone.examples.bazaar.catalog.Schedules schedules) {
        this.schedules = schedules;
        this.rows = rows;
        this.products = products;
        this.variants = variants;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> list() throws Exception {
        return listOf(Caller.name());
    }

    @Override
    @Transactional
    public WishlistItemDto add(String productId) throws Exception {
        String user = Caller.name();
        Product p = productId == null ? null : products.findById(productId);
        if (!schedules.onlineNow(p)) {
            throw new ResponseStatusException(404, "No such product");
        }
        Wishlist w = rows.findByUsernameAndProductId(user, productId);
        if (w == null) {
            if (rows.findByUsernameOrderByCreatedAtDesc(user).size() >= MAX_ITEMS) {
                throw new ResponseStatusException(409, "The wishlist is full");
            }
            w = new Wishlist();
            w.setId(Ids.next());
            w.setUsername(user);
            w.setProductId(productId);
            w.setStoreId(p.getStoreId());
            w.setCreatedAt(Instant.now());
            rows.insert(w);
        }
        return dto(w, p);
    }

    @Override
    @Transactional
    public List<WishlistItemDto> remove(String productId) throws Exception {
        String user = Caller.name();
        Wishlist w = rows.findByUsernameAndProductId(user, productId);
        if (w != null) {
            rows.delete(w);
        }
        return listOf(user);
    }

    private List<WishlistItemDto> listOf(String user) throws java.io.IOException {
        List<Wishlist> saved = rows.findByUsernameOrderByCreatedAtDesc(user);
        List<WishlistItemDto> out = new ArrayList<WishlistItemDto>();
        for (int iter = 0; iter < saved.size(); iter++) {
            Wishlist w = saved.get(iter);
            Product p = products.findById(w.getProductId());
            if (p != null && "ACTIVE".equals(p.getStatus())
                    && schedules.hiddenNow(p.getId(),
                        com.codenameone.examples.bazaar.catalog.Schedules.ONLINE)) {
                continue;
            }
            out.add(dto(w, p));
        }
        return out;
    }

    private WishlistItemDto dto(Wishlist w, Product p) {
        long price = 0;
        if (p != null) {
            List<Variant> vs = variants.findByProductIdOrderByPositionAsc(p.getId());
            for (int iter = 0; iter < vs.size(); iter++) {
                long c = vs.get(iter).getPriceCents();
                if (iter == 0 || c < price) {
                    price = c;
                }
            }
        }
        return new WishlistItemDto(w.getId(), w.getProductId(), w.getStoreId(),
                p == null ? "" : p.getHandle(), p == null ? "" : p.getTitle(),
                p == null ? "" : p.getPhotoId(), price,
                p != null && "ACTIVE".equals(p.getStatus()), w.getCreatedAt().toEpochMilli());
    }
}
