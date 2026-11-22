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

package com.codenameone.examples.bazaar.analytics;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.ProductViewEvent;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ProductViewEventRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.web.StorefrontPages;

import java.io.IOException;
import java.time.Instant;

/// Records every opening of a product page, for the funnel's first step and
/// the purchase-intent signals of the sales pipeline.
///
/// The visitor is the browser's cart token when it has a cart, else the
/// signed-in account, else blank: a guest who never added anything is still
/// counted as a view, but cannot be followed from one view to the next.
/// Recording never fails a page.
@Component
public class Views {
    private final ProductViewEventRepository views;
    private final StoreRepository stores;
    private final ProductRepository products;

    public Views(ProductViewEventRepository views, StoreRepository stores,
            ProductRepository products) {
        this.views = views;
        this.stores = stores;
        this.products = products;
    }

    /// A product page of the app or the API; `username` may be null.
    public void viewed(String storeId, String productId, String username) {
        if (storeId == null || productId == null) {
            return;
        }
        try {
            record(storeId, productId, username == null ? "" : username, Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("[views] the view was not recorded: " + failed);
        }
    }

    /// A product page of the storefront, which knows the store and product
    /// by their handles.
    public void viewedByHandle(String storeHandle, String productHandle,
            HttpServer.Request request, String username) {
        if (storeHandle == null || productHandle == null) {
            return;
        }
        try {
            Store s = stores.findByHandle(storeHandle);
            Product p = s == null ? null : products.findByStoreIdAndHandle(s.getId(),
                    productHandle);
            if (p == null) {
                return;
            }
            String token = StorefrontPages.cartToken(request, s.getId());
            String visitor = token != null ? token : username == null ? "" : username;
            record(s.getId(), p.getId(), visitor, Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("[views] the view was not recorded: " + failed);
        }
    }

    /// One view at a chosen time; public so that tests can set the clock.
    @Transactional(rollbackFor = IOException.class)
    public void record(String storeId, String productId, String visitor, Instant at)
            throws IOException {
        ProductViewEvent v = new ProductViewEvent();
        v.setId(Ids.next());
        v.setStoreId(storeId);
        v.setProductId(productId);
        v.setVisitor(visitor.length() > 190 ? visitor.substring(0, 190) : visitor);
        v.setViewedAt(at);
        views.insert(v);
    }
}
