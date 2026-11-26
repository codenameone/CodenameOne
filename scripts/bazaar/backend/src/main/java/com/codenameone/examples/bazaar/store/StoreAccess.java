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

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;

import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codename1.backend.security.mfa.TotpService;
import java.io.IOException;
import java.util.Map;

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
    private final DataSource db;
    private final TotpService totp;

    public StoreAccess(StoreRepository stores, StoreStaffRepository staff, DataSource db,
            TotpService totp) {
        this.stores = stores;
        this.staff = staff;
        this.db = db;
        this.totp = totp;
    }

    /// The reason a member of a store that requires an authenticator app is
    /// refused until they enrol one; the app and the web pages look for it to
    /// offer enrolment.
    public static final String TWO_FACTOR_REQUIRED = "This store requires two-step sign-in. "
            + "Turn on an authenticator app in your account's security settings.";

    /// Whether the store requires every member of its staff to have an
    /// authenticator app.
    public boolean requiresTwoFactor(String storeId) throws IOException {
        Map row = db.queryOne("SELECT require_two_factor AS r FROM staff_security "
                + "WHERE store_id = ?", new Object[] {storeId});
        return row != null && Rows.number(row.get("r")) != 0;
    }

    /// Whether `username` has a confirmed authenticator app.
    public boolean hasTwoFactor(String username) {
        return totp.isEnabled(username);
    }

    /// The caller's place on the staff, judged by permission area: 404 as
    /// [#requireStaff] for a stranger, then 403 unless allowed.
    ///
    /// A member with a permission set assigned (for `locationId`, or for every
    /// location) is judged by the set alone; one with none is judged by
    /// `legacyRoles`, exactly as before sets existed, so assigning nothing
    /// changes nothing. The OWNER is never restricted: a store must keep
    /// someone who can undo a mistaken set. `locationId` may be null for a
    /// call about no one location, which reads the every-location set.
    @Transactional(readOnly = true)
    public StoreStaff requireArea(String username, String storeId, String area, boolean write,
            String locationId, String... legacyRoles) throws IOException {
        StoreStaff member = requireStaff(username, storeId);
        if (OWNER.equals(member.getRole())) {
            return member;
        }
        String grants = grantsOf(member, locationId);
        boolean ok = grants == null ? isRole(member.getRole(), legacyRoles)
                : Perms.allows(grants, area, write);
        if (!ok) {
            throw new ResponseStatusException(403, "Your permissions at this store cannot do that");
        }
        return member;
    }

    /// [#requireArea] for a call about no one location.
    @Transactional(readOnly = true)
    public StoreStaff requireArea(String username, String storeId, String area, boolean write,
            String[] legacyRoles) throws IOException {
        return requireArea(username, storeId, area, write, null, legacyRoles);
    }

    /// The grants assigned to `member` at `locationId` (falling back to the
    /// every-location assignment), or null when none is.
    public String grantsOf(StoreStaff member, String locationId) throws IOException {
        Map row = null;
        if (locationId != null && locationId.length() > 0) {
            row = db.queryOne("SELECT s.grants AS grants FROM staff_permission p JOIN permission_set s "
                    + "ON s.id = p.set_id WHERE p.staff_id = ? AND p.location_id = ? AND s.store_id = ?",
                    new Object[] {member.getId(), locationId, member.getStoreId()});
        }
        if (row == null) {
            row = db.queryOne("SELECT s.grants AS grants FROM staff_permission p JOIN permission_set s "
                    + "ON s.id = p.set_id WHERE p.staff_id = ? AND p.location_id = '' AND s.store_id = ?",
                    new Object[] {member.getId(), member.getStoreId()});
        }
        return row == null ? null : Rows.text(row.get("grants"));
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
        // Membership first, so a stranger still learns nothing but 404; then
        // the store's sign-in rule, for every route and every role, the
        // owner's included. Enrolling is under /api/account, which names no
        // store, so a refused member can still put it right.
        if (requiresTwoFactor(storeId) && !totp.isEnabled(username)) {
            throw new ResponseStatusException(403, TWO_FACTOR_REQUIRED);
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
