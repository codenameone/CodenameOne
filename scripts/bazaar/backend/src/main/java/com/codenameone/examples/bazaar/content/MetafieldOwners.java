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
package com.codenameone.examples.bazaar.content;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.domain.Collection;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import java.io.IOException;

/// Whether a record a metafield value is written against belongs to the store.
@Component
public class MetafieldOwners {
    private final ProductRepository products;
    private final VariantRepository variants;
    private final CollectionRepository collections;
    private final CustomerRepository customers;
    private final StoreOrderRepository orders;

    public MetafieldOwners(ProductRepository products, VariantRepository variants,
            CollectionRepository collections, CustomerRepository customers,
            StoreOrderRepository orders) {
        this.products = products;
        this.variants = variants;
        this.collections = collections;
        this.customers = customers;
        this.orders = orders;
    }

    /// True when `ownerId` of `kind` is a record of `storeId`.
    public boolean owns(String storeId, String kind, String ownerId) throws IOException {
        String owner = null;
        if ("PRODUCT".equals(kind)) {
            Product p = products.findById(ownerId);
            owner = p == null ? null : p.getStoreId();
        } else if ("VARIANT".equals(kind)) {
            Variant v = variants.findById(ownerId);
            owner = v == null ? null : v.getStoreId();
        } else if ("COLLECTION".equals(kind)) {
            Collection c = collections.findById(ownerId);
            owner = c == null ? null : c.getStoreId();
        } else if ("CUSTOMER".equals(kind)) {
            Customer c = customers.findById(ownerId);
            owner = c == null ? null : c.getStoreId();
        } else if ("ORDER".equals(kind)) {
            StoreOrder o = orders.findById(ownerId);
            owner = o == null ? null : o.getStoreId();
        }
        return storeId.equals(owner);
    }
}
