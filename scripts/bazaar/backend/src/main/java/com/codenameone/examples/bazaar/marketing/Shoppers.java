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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;

import java.io.IOException;
import java.time.Instant;

/// The customer row a signed-in shopper has in a store, made on first use:
/// joining a club or the points programme comes before any order.
@Component
public class Shoppers {
    private final CustomerRepository customers;
    private final StoreRepository stores;

    public Shoppers(CustomerRepository customers, StoreRepository stores) {
        this.customers = customers;
        this.stores = stores;
    }

    /// The store, which must exist and be open; 404 otherwise.
    @Transactional(readOnly = true)
    public Store store(String storeId) {
        Store s = storeId == null ? null : stores.findById(storeId);
        if (s == null || !"ACTIVE".equals(s.getStatus())) {
            throw new ResponseStatusException(404, "No such store");
        }
        return s;
    }

    /// `username`'s customer in the store; 401 for a guest.
    @Transactional(rollbackFor = IOException.class)
    public Customer customer(String storeId, String username) throws IOException {
        if (username == null || username.length() == 0) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        Customer c = customers.findByStoreIdAndUsername(storeId, username);
        if (c != null) {
            return c;
        }
        c = new Customer();
        c.setId(Ids.next());
        c.setStoreId(storeId);
        c.setUsername(username);
        c.setEmail(username.indexOf('@') > 0 ? username : "");
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        customers.insert(c);
        return c;
    }
}
