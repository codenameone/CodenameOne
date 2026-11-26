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
import com.codenameone.examples.bazaar.api.DiscoverApiServer;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.StoreFollow;
import com.codenameone.examples.bazaar.repository.StoreFollowRepository;
import com.codenameone.examples.bazaar.store.Stores;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The server's half of `DiscoverApi`: the feed across every active store,
/// which reads no caller, and the follow list, which is the caller's own.
@Component
public class DiscoverEndpoint implements DiscoverApiServer {
    static final int MAX_FOLLOWS = 500;
    private static final int DEFAULT_SIZE = 24;
    private static final int MAX_SIZE = 60;

    private final Catalog catalog;
    private final Stores stores;
    private final StoreFollowRepository follows;

    public DiscoverEndpoint(Catalog catalog, Stores stores, StoreFollowRepository follows) {
        this.catalog = catalog;
        this.stores = stores;
        this.follows = follows;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPageDto feed(String q, String sort, String size) throws Exception {
        return mix(stores.publicStores(), q, sort, size(size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreDto> following() throws Exception {
        return followed(Caller.name());
    }

    @Override
    @Transactional
    public List<StoreDto> follow(String storeId) throws Exception {
        String user = Caller.name();
        if (active(storeId) == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        if (follows.findByUsernameAndStoreId(user, storeId) == null) {
            if (follows.findByUsernameOrderByCreatedAtDesc(user).size() >= MAX_FOLLOWS) {
                throw new ResponseStatusException(409, "You follow too many stores");
            }
            StoreFollow f = new StoreFollow();
            f.setId(Ids.next());
            f.setUsername(user);
            f.setStoreId(storeId);
            f.setCreatedAt(Instant.now());
            follows.insert(f);
        }
        return followed(user);
    }

    @Override
    @Transactional
    public List<StoreDto> unfollow(String storeId) throws Exception {
        String user = Caller.name();
        StoreFollow f = follows.findByUsernameAndStoreId(user, storeId);
        if (f != null) {
            follows.delete(f);
        }
        return followed(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPageDto followingFeed(String size) throws Exception {
        return mix(followed(Caller.name()), null, "newest", size(size));
    }

    private StoreDto active(String storeId) throws Exception {
        List<StoreDto> all = stores.publicStores();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(storeId)) {
                return all.get(iter);
            }
        }
        return null;
    }

    /// The followed stores that are still open, newest follow first. A store
    /// that closed since stays in the table and reappears if it reopens.
    private List<StoreDto> followed(String user) throws Exception {
        Map<String, StoreDto> open = new HashMap<String, StoreDto>();
        List<StoreDto> all = stores.publicStores();
        for (int iter = 0; iter < all.size(); iter++) {
            open.put(all.get(iter).id(), all.get(iter));
        }
        List<StoreDto> out = new ArrayList<StoreDto>();
        List<StoreFollow> rows = follows.findByUsernameOrderByCreatedAtDesc(user);
        for (int iter = 0; iter < rows.size(); iter++) {
            StoreDto s = open.get(rows.get(iter).getStoreId());
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    private static int size(String size) {
        int n = DEFAULT_SIZE;
        if (size != null) {
            try {
                n = Integer.parseInt(size.trim());
            } catch (NumberFormatException bad) {
                throw new ResponseStatusException(400, "size must be a number");
            }
        }
        return Math.max(1, Math.min(MAX_SIZE, n));
    }

    /// One page from each store, taken a product of each in turn; for
    /// `popular`, ordered by rating after that.
    private ProductPageDto mix(List<StoreDto> from, String q, String sort, int size)
            throws Exception {
        boolean popular = "popular".equals(sort);
        boolean words = q != null && q.trim().length() > 0;
        String storeSort = popular ? "rating" : words && sort == null ? "relevance" : "newest";
        List<List<ProductCardDto>> pages = new ArrayList<List<ProductCardDto>>();
        int total = 0;
        for (int iter = 0; iter < from.size(); iter++) {
            ProductPageDto p = catalog.browse(from.get(iter).handle(), null, words ? q : null,
                    storeSort, 0, size);
            total += p.total();
            pages.add(p.items() == null ? new ArrayList<ProductCardDto>() : p.items());
        }
        List<ProductCardDto> out = new ArrayList<ProductCardDto>();
        for (int round = 0; out.size() < size; round++) {
            boolean any = false;
            for (int iter = 0; iter < pages.size() && out.size() < size; iter++) {
                if (round < pages.get(iter).size()) {
                    out.add(pages.get(iter).get(round));
                    any = true;
                }
            }
            if (!any) {
                break;
            }
        }
        if (popular) {
            out.sort((a, b) -> a.rating() != b.rating() ? b.rating() - a.rating()
                    : b.ratingCount() - a.ratingCount());
        }
        return new ProductPageDto(out, total, 0, size);
    }
}
