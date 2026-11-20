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
package com.codenameone.examples.bazaar.store;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;

import java.io.IOException;

/// Who may act for a store. Every merchant call names a store, and the path
/// alone proves nothing: the store's staff list has to name the caller, in one
/// of the roles the call allows.
///
/// The roles, from most to least trusted: OWNER and MANAGER run the store
/// (settings, staff, catalog), FULFILMENT picks, packs and counts stock,
/// CASHIER works a register and may only read the catalog.
///
/// A store that is not found and a store the caller does not work for answer
/// the same 404, so a stranger cannot tell the ids of other stores from
/// nonsense.
@Component
public class StoreAccess {
    public static final String OWNER = "OWNER";
    public static final String MANAGER = "MANAGER";
    public static final String CASHIER = "CASHIER";
    public static final String FULFILMENT = "FULFILMENT";

    /// Running the store: settings, staff and the catalog.
    public static final String[] MANAGERS = {OWNER, MANAGER};
    /// Changing stock.
    public static final String[] STOCK = {OWNER, MANAGER, FULFILMENT};
    /// Anyone on the staff.
    public static final String[] ANYONE = {OWNER, MANAGER, FULFILMENT, CASHIER};

    private final StoreRepository stores;
    private final StoreStaffRepository staff;

    public StoreAccess(StoreRepository stores, StoreStaffRepository staff) {
        this.stores = stores;
        this.staff = staff;
    }

    /// The caller's place on the store's staff. Answers 404 when the store
    /// does not exist or the caller is not on its staff, and 403 when the
    /// caller is, in a role not among `roles`.
    @Transactional(readOnly = true)
    public StoreStaff requireStaff(String username, String storeId, String... roles)
            throws IOException {
        if (username == null || storeId == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        StoreStaff member = staff.findByStoreIdAndUsername(storeId, username);
        if (member == null || stores.findById(storeId) == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        if (!isRole(member.getRole(), roles)) {
            throw new ResponseStatusException(403, "Your role at this store cannot do that");
        }
        return member;
    }

    /// The store, once [#requireStaff] has admitted the caller.
    @Transactional(readOnly = true)
    public Store requireStore(String username, String storeId, String... roles)
            throws IOException {
        requireStaff(username, storeId, roles);
        return stores.findById(storeId);
    }

    /// Whether `role` is one of `roles`; none given means any.
    public static boolean isRole(String role, String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        for (int iter = 0; iter < roles.length; iter++) {
            if (roles[iter].equals(role)) {
                return true;
            }
        }
        return false;
    }

    /// Answers 404 unless something read by id belongs to the store the path
    /// names: an id from another store is treated as no id at all. Pass null
    /// for something that was not found.
    public static void requireIn(String entityStoreId, String storeId, String what) {
        if (entityStoreId == null || !entityStoreId.equals(storeId)) {
            throw new ResponseStatusException(404, "No such " + what);
        }
    }
}
