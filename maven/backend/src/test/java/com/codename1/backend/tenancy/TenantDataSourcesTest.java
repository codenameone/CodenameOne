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
package com.codename1.backend.tenancy;

import com.codename1.backend.DataSource;
import com.codename1.orm.session.MissingTenantException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TenantDataSourcesTest {
    @TempDir
    File dir;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private static long count(DataSource pool, String table) throws Exception {
        List rows = pool.query("SELECT COUNT(*) AS n FROM " + table, new Object[0]);
        return ((Number) ((Map) rows.get(0)).get("n")).longValue();
    }

    @Test
    void eachTenantGetsItsOwnDatabaseAndInitStatement() throws Exception {
        String url = new File(dir, "t_${tenant}.db").getAbsolutePath();
        TenantDataSources tenants = new TenantDataSources(url,
                "CREATE TABLE IF NOT EXISTS opened_${tenant} (x INTEGER)", 1, 2);
        try {
            assertThrows(MissingTenantException.class, tenants::current);
            TenantContext.set("acme");
            DataSource acme = tenants.current();
            acme.execute("CREATE TABLE t (x INTEGER)", new Object[0]);
            acme.execute("INSERT INTO t VALUES (1)", new Object[0]);
            assertSame(acme, tenants.forTenant("acme"));
            assertEquals(0, count(acme, "opened_acme"), "init statement ran on the tenant's connection");

            DataSource globex = tenants.forTenant("globex");
            assertNotSame(acme, globex);
            globex.execute("CREATE TABLE t (x INTEGER)", new Object[0]);
            assertEquals(0, count(globex, "t"));
            assertEquals(1, count(acme, "t"));
            assertTrue(new File(dir, "t_acme.db").isFile());
            assertTrue(new File(dir, "t_globex.db").isFile());

            // A third tenant evicts the least recently used pool (acme was touched last).
            tenants.forTenant("acme");
            tenants.forTenant("initech");
            assertNotSame(globex, tenants.forTenant("globex"), "evicted pool is reopened");
            // A pool evicted while lending a connection stays open for its holder.
            DataSource reopened = tenants.forTenant("globex");
            com.codename1.backend.Database held = reopened.borrow();
            tenants.forTenant("acme");
            tenants.forTenant("initech");
            assertEquals(1, held.query("SELECT 1 AS n", new Object[0]).size());
            reopened.release(held);
            assertThrows(IllegalArgumentException.class, () -> tenants.forTenant("../etc"));
        } finally {
            tenants.close();
        }
    }

    @Test
    void aTemplateThatIgnoresTheTenantIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TenantDataSources("shared.db", null, 1, 4));
        new TenantDataSources("shared.db", "SET search_path TO tenant_${tenant}", 1, 4).close();
        // A bare ${tenant} would let a tenant named "public" or "postgres" pick a shared schema.
        assertThrows(IllegalArgumentException.class,
                () -> new TenantDataSources("shared.db", "SET search_path TO ${tenant}", 1, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new TenantDataSources("postgres://db/${tenant}", null, 1, 4));
    }
}
