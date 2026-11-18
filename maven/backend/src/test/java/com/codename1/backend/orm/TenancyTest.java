/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.orm;

import com.codename1.backend.Database;
import com.codename1.backend.tenancy.TenantContext;
import com.codename1.impl.orm.Attribute;
import com.codename1.impl.orm.BackendSqlAccess;
import com.codename1.impl.orm.EntityModel;
import com.codename1.impl.orm.SessionImpl;
import com.codename1.backend.tenancy.MissingTenantException;
import com.codename1.backend.tenancy.TenantNativeSqlException;
import com.codename1.backend.tenancy.TenantMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Two tenants share one table; every path a session reads or writes through
/// must see only the current tenant's rows.
class TenancyTest {
    static class Doc {
        long id, version, counter;
        String tenant, title;
    }

    static class DocModel extends EntityModel<Doc> {
        private final Attribute[] attrs = {
            new Attribute("id", "id", Attribute.BIGINT, true, true, false, false),
            new Attribute("version", "version", Attribute.BIGINT, false, false, false, true),
            new Attribute("counter", "counter", Attribute.BIGINT, false, false, false, false),
            new Attribute("tenant", "tenant_id", Attribute.TEXT, false, false, false, false),
            new Attribute("title", "title", Attribute.TEXT, false, false, true, false)
        };
        public Class<Doc> type() { return Doc.class; }
        public String table() { return "tenant_doc"; }
        public int tenantIndex() { return 3; }
        public boolean counter(int index) { return index == 2; }
        public Attribute[] attributes() { return attrs; }
        public Doc create() { return new Doc(); }
        public Object get(Doc d, int i) {
            switch (i) {
                case 0: return d.id;
                case 1: return d.version;
                case 2: return d.counter;
                case 3: return d.tenant;
                default: return d.title;
            }
        }
        public void set(Doc d, int i, Object v) {
            switch (i) {
                case 0: d.id = ((Number) v).longValue(); break;
                case 1: d.version = ((Number) v).longValue(); break;
                case 2: d.counter = ((Number) v).longValue(); break;
                case 3: d.tenant = (String) v; break;
                default: d.title = (String) v; break;
            }
        }
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private static SessionImpl open(Database db) {
        Map<String, EntityModel<?>> models = new LinkedHashMap<String, EntityModel<?>>();
        models.put(Doc.class.getName(), new DocModel());
        return new SessionImpl(new BackendSqlAccess(null, db, db.dialect()), models);
    }

    private static long add(SessionImpl s, String title) {
        s.beginTransaction();
        Doc d = new Doc();
        d.title = title;
        s.persist(d);
        s.commitTransaction();
        s.clear();
        return d.id;
    }

    @Test
    void everyPathIsLimitedToTheCurrentTenant() throws Exception {
        Database db = Database.open(":memory:");
        SessionImpl s = open(db);
        try {
            s.createTables();
            TenantContext.set("acme");
            long acme = add(s, "a1");
            add(s, "a2");
            TenantContext.set("globex");
            long globex = add(s, "g1");

            assertEquals("globex", s.find(Doc.class, globex).tenant);
            assertNull(s.find(Doc.class, acme), "another tenant's row by id");
            assertEquals(1, s.query(Doc.class).list().size());
            assertEquals(1, s.createQuery("select d from TenancyTest$Doc d", Doc.class).list().size());
            assertEquals(Long.valueOf(1), s.createQuery("select count(d) from TenancyTest$Doc d", Long.class).first());
            s.beginTransaction();
            assertFalse(s.increment(Doc.class, acme, "counter", 1), "counter on another tenant's row");
            assertEquals(1, s.createQuery("update TenancyTest$Doc d set d.title = 'x'").executeUpdate());
            assertEquals(1, s.createQuery("delete from TenancyTest$Doc d").executeUpdate());
            s.commitTransaction();

            TenantContext.set("acme");
            s.clear();
            assertEquals(2, s.query(Doc.class).list().size(), "acme untouched by globex's bulk writes");
            for (Doc d : s.query(Doc.class).list()) {
                assertEquals("acme", d.tenant);
                assertNotEquals("x", d.title);
            }
            s.clear();
            assertEquals(Integer.valueOf(3), TenantContext.runUnscoped(() -> {
                return Integer.valueOf(s.query(Doc.class).list().size() + 1);
            }));
        } finally {
            s.close();
            db.close();
        }
    }

    @Test
    void failsClosedWithoutATenantAndRefusesAForeignOne() throws Exception {
        Database db = Database.open(":memory:");
        SessionImpl s = open(db);
        try {
            s.createTables();
            assertThrows(MissingTenantException.class, () -> s.query(Doc.class).list());
            TenantContext.set("acme");
            s.beginTransaction();
            Doc foreign = new Doc();
            foreign.tenant = "globex";
            s.persist(foreign);
            assertThrows(TenantMismatchException.class, s::flush);
        } finally {
            s.close();
            db.close();
        }
    }

    @Test
    void aSessionStaysWithItsTenantAndNativeSqlNeedsAnOptIn() throws Exception {
        Database db = Database.open(":memory:");
        SessionImpl s = open(db);
        try {
            s.createTables();
            TenantContext.set("acme");
            long acme = add(s, "a1");
            s.find(Doc.class, acme);
            TenantContext.set("globex");
            // The identity map holds acme's row; serving globex from it would leak.
            assertThrows(TenantMismatchException.class, () -> s.find(Doc.class, acme));
            assertThrows(TenantMismatchException.class,
                    () -> TenantContext.runUnscoped(() -> s.query(Doc.class).list()));
            s.clear();
            assertNull(s.find(Doc.class, acme));

            assertThrows(TenantNativeSqlException.class,
                    () -> s.createNativeQuery("SELECT COUNT(*) FROM tenant_doc", Long.class));
            assertEquals(Long.valueOf(0), TenantContext.allowNativeSql(() -> s.createNativeQuery(
                    "SELECT COUNT(*) FROM tenant_doc WHERE tenant_id = :t", Long.class)
                    .setParameter("t", TenantContext.get()).first()));
            s.clear();
            assertEquals(Long.valueOf(1), TenantContext.runUnscoped(() -> s.createNativeQuery(
                    "SELECT COUNT(*) FROM tenant_doc", Long.class).first()));
        } finally {
            s.close();
            db.close();
        }
    }

    @Test
    void aRowCannotBeMovedToAnotherTenant() throws Exception {
        Database db = Database.open(":memory:");
        SessionImpl s = open(db);
        try {
            s.createTables();
            TenantContext.set("acme");
            long id = add(s, "a1");
            s.beginTransaction();
            Doc d = s.find(Doc.class, id);
            d.tenant = "globex";
            d.title = "planted";
            assertThrows(TenantMismatchException.class, s::flush);
            s.rollbackTransaction();
            s.clear();

            s.beginTransaction();
            assertThrows(RuntimeException.class,
                    () -> s.createQuery("update TenancyTest$Doc d set d.tenant = 'globex'").executeUpdate());
            s.rollbackTransaction();
            s.clear();

            Doc detached = new Doc();
            detached.id = id;
            detached.tenant = "globex";
            detached.title = "merged";
            s.beginTransaction();
            assertThrows(TenantMismatchException.class, () -> { s.merge(detached); s.flush(); });
            s.rollbackTransaction();
            s.clear();
            TenantContext.set("globex");
            assertTrue(s.query(Doc.class).list().isEmpty(), "globex received no rows");
        } finally {
            s.close();
            db.close();
        }
    }

    @Test
    void aNativeQueryCreatedInsideTheOptInCannotRunOutsideIt() throws Exception {
        Database db = Database.open(":memory:");
        SessionImpl s = open(db);
        try {
            s.createTables();
            TenantContext.set("acme");
            com.codename1.orm.session.JpqlQuery<?> q = (com.codename1.orm.session.JpqlQuery<?>)
                    TenantContext.allowNativeSql(() -> s.createNativeQuery("SELECT title FROM tenant_doc", String.class));
            assertThrows(TenantNativeSqlException.class, q::list);
        } finally {
            s.close();
            db.close();
        }
    }

    @Test
    void decoratorCarriesTheTenantAndRejectsBadIds() throws Exception {
        TenantContext.set("acme");
        final String[] seen = new String[1];
        Runnable task = TenantContext.decorator().decorate(() -> seen[0] = TenantContext.get());
        TenantContext.clear();
        Thread t = new Thread(task);
        t.start();
        t.join();
        assertEquals("acme", seen[0]);
        assertThrows(IllegalArgumentException.class, () -> TenantContext.set("a'; drop table x"));
    }
}
