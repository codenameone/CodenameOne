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
package com.codenameone.examples.bazaar.staff;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.StaffSecurityDto;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A store's sign-in rule for its staff: whether every member must have an
/// authenticator app. [StoreAccess#requireStaff] enforces it on every staff
/// route, for the app and the web pages alike; this reads and changes it.
///
/// The owner turns it on, and only once enrolled -- otherwise the first
/// thing the rule would do is lock the owner out of the setting. Members
/// still without an app are listed, so the owner can see who will be
/// refused. Nobody can turn their own app off while a store they work at
/// requires one ([#requiredBy]).
@Component
public class StaffSecurity {
    private final DataSource db;
    private final StoreAccess access;

    public StaffSecurity(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public StaffSecurityDto settings(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return describe(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public StaffSecurityDto save(String username, String storeId, StaffSecurityDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        boolean on = wanted != null && wanted.requireTwoFactor();
        if (on && !access.hasTwoFactor(username)) {
            throw new ResponseStatusException(409, "Turn on your own authenticator app first");
        }
        long now = System.currentTimeMillis();
        int changed = db.execute("UPDATE staff_security SET require_two_factor = ?, updated_at = ? "
                + "WHERE store_id = ?", new Object[] {Integer.valueOf(on ? 1 : 0),
                    Long.valueOf(now), storeId});
        if (changed == 0) {
            db.execute("INSERT INTO staff_security (store_id, require_two_factor, updated_at) "
                    + "VALUES (?, ?, ?)", new Object[] {storeId, Integer.valueOf(on ? 1 : 0),
                        Long.valueOf(now)});
        }
        return describe(storeId);
    }

    /// The names of the stores `username` works at that require an
    /// authenticator app; empty when none does.
    public List<String> requiredBy(String username) throws IOException {
        List rows = db.query("SELECT s.name AS name FROM store_staff m JOIN store s ON "
                + "s.id = m.store_id JOIN staff_security r ON r.store_id = m.store_id "
                + "WHERE m.username = ? AND r.require_two_factor <> 0 ORDER BY s.name",
                new Object[] {username});
        List<String> out = new ArrayList<String>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            out.add(Rows.text(((Map) rows.get(iter)).get("name")));
        }
        return out;
    }

    private StaffSecurityDto describe(String storeId) throws IOException {
        List rows = db.query("SELECT username AS username FROM store_staff WHERE store_id = ? "
                + "ORDER BY username", new Object[] {storeId});
        List<String> unenrolled = new ArrayList<String>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            String who = Rows.text(((Map) rows.get(iter)).get("username"));
            if (!access.hasTwoFactor(who)) {
                unenrolled.add(who);
            }
        }
        return new StaffSecurityDto(access.requiresTwoFactor(storeId), unenrolled);
    }
}
