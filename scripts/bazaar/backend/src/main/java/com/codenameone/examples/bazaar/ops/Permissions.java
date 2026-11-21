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
package com.codenameone.examples.bazaar.ops;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.PermissionSetDto;
import com.codenameone.examples.bazaar.api.StaffPermissionDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The permission set editor: named sets of per-area grants ([Perms]) and
/// their assignment to staff members, per location or for every location.
/// Only the OWNER and MANAGERs edit them (the settings area); an empty set
/// id in an assignment removes it, and the member falls back to the role.
/// A set cannot be assigned to the OWNER, who is never restricted.
@Component
public class Permissions {
    static final int MAX_SETS = 50;

    private final DataSource db;
    private final StoreAccess access;

    public Permissions(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<PermissionSetDto> sets(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, false, StoreAccess.MANAGERS);
        List rows = db.query("SELECT id, name, grants FROM permission_set WHERE store_id = ? "
                + "ORDER BY name", new Object[] {storeId});
        List<PermissionSetDto> out = new ArrayList<PermissionSetDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new PermissionSetDto(Rows.text(r.get("id")), Rows.text(r.get("name")),
                    Rows.text(r.get("grants"))));
        }
        return out;
    }

    private static String name(PermissionSetDto s) {
        String n = s == null || s.name() == null ? "" : s.name().trim();
        if (n.length() == 0 || n.length() > 100) {
            throw new ResponseStatusException(400, "A permission set has a name of up to 100 characters");
        }
        return n;
    }

    @Transactional(rollbackFor = IOException.class)
    public PermissionSetDto create(String username, String storeId, PermissionSetDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        String n = name(wanted);
        Map count = db.queryOne("SELECT COUNT(*) AS n FROM permission_set WHERE store_id = ?",
                new Object[] {storeId});
        if (Rows.number(count.get("n")) >= MAX_SETS) {
            throw new ResponseStatusException(409, "A store has at most " + MAX_SETS + " permission sets");
        }
        String id = Ids.next();
        String grants = Perms.normalize(wanted.grants());
        db.execute("INSERT INTO permission_set (id, store_id, name, grants, created_at) VALUES "
                + "(?, ?, ?, ?, ?)", new Object[] {id, storeId, n, grants,
                    Long.valueOf(System.currentTimeMillis())});
        return new PermissionSetDto(id, n, grants);
    }

    @Transactional(rollbackFor = IOException.class)
    public PermissionSetDto update(String username, String storeId, String setId,
            PermissionSetDto wanted) throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        set(storeId, setId);
        String n = name(wanted);
        String grants = Perms.normalize(wanted.grants());
        db.execute("UPDATE permission_set SET name = ?, grants = ? WHERE id = ? AND store_id = ?",
                new Object[] {n, grants, setId, storeId});
        return new PermissionSetDto(setId, n, grants);
    }

    private Map set(String storeId, String setId) throws IOException {
        Map r = setId == null || setId.length() > 32 ? null
                : db.queryOne("SELECT id, store_id, name FROM permission_set WHERE id = ?",
                        new Object[] {setId});
        StoreAccess.requireIn(r == null ? null : Rows.text(r.get("store_id")), storeId,
                "permission set");
        return r;
    }

    @Transactional(readOnly = true)
    public List<StaffPermissionDto> assignments(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, false, StoreAccess.MANAGERS);
        List rows = db.query("SELECT p.id, p.staff_id, s.username AS username, s.role AS role, "
                + "p.location_id, p.set_id, ps.name AS set_name FROM staff_permission p JOIN store_staff s "
                + "ON s.id = p.staff_id JOIN permission_set ps ON ps.id = p.set_id WHERE p.store_id = ? "
                + "ORDER BY s.username, p.location_id", new Object[] {storeId});
        List<StaffPermissionDto> out = new ArrayList<StaffPermissionDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new StaffPermissionDto(Rows.text(r.get("id")), Rows.text(r.get("staff_id")),
                    Rows.text(r.get("username")), Rows.text(r.get("role")),
                    Rows.text(r.get("location_id")), Rows.text(r.get("set_id")),
                    Rows.text(r.get("set_name"))));
        }
        return out;
    }

    /// Assigns (or, with no set id, removes) a set for a staff member at a
    /// location ("" for every location).
    @Transactional(rollbackFor = IOException.class)
    public List<StaffPermissionDto> assign(String username, String storeId, StaffPermissionDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        Map staff = wanted == null || wanted.staffId() == null || wanted.staffId().length() > 32 ? null
                : db.queryOne("SELECT id, store_id, role FROM store_staff WHERE id = ?",
                        new Object[] {wanted.staffId()});
        if (staff == null || !storeId.equals(Rows.text(staff.get("store_id")))) {
            throw new ResponseStatusException(400, "No such staff member");
        }
        if (StoreAccess.OWNER.equals(Rows.text(staff.get("role")))) {
            throw new ResponseStatusException(400, "The owner is never restricted");
        }
        String location = wanted.locationId() == null ? "" : wanted.locationId();
        if (location.length() > 0) {
            Map l = location.length() > 32 ? null
                    : db.queryOne("SELECT store_id FROM location WHERE id = ?", new Object[] {location});
            if (l == null || !storeId.equals(Rows.text(l.get("store_id")))) {
                throw new ResponseStatusException(400, "No such location");
            }
        }
        db.execute("DELETE FROM staff_permission WHERE staff_id = ? AND location_id = ? AND store_id = ?",
                new Object[] {wanted.staffId(), location, storeId});
        if (wanted.setId() != null && wanted.setId().length() > 0) {
            Map s = wanted.setId().length() > 32 ? null : db.queryOne(
                    "SELECT store_id FROM permission_set WHERE id = ?", new Object[] {wanted.setId()});
            if (s == null || !storeId.equals(Rows.text(s.get("store_id")))) {
                throw new ResponseStatusException(400, "No such permission set");
            }
            db.execute("INSERT INTO staff_permission (id, store_id, staff_id, location_id, set_id, "
                    + "created_at) VALUES (?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId,
                        wanted.staffId(), Codes.cut(location, 32), wanted.setId(),
                        Long.valueOf(System.currentTimeMillis())});
        }
        return assignments(username, storeId);
    }
}
