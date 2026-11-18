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

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.Migrations;
import com.codename1.impl.orm.TenantSource;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/// One [DataSource] per tenant -- the backend's counterpart of Spring's
/// `AbstractRoutingDataSource` -- for a database, or a schema, per tenant.
///
/// Each pool opens on first use. `${tenant}` stands for the identifier in the
/// URL template and in the connection-init template:
///
/// - **A database per tenant:** `postgres://app:secret@db/app_${tenant}` or
///   `data/t_${tenant}.db`, no init statement.
/// - **A schema per tenant:** one URL and an init statement that pins every
///   connection of the tenant's pool to its schema, `SET search_path TO
///   tenant_${tenant}` on PostgreSQL or `USE tenant_${tenant}` on MySQL.
///
/// Pools can also be registered with [#register]. At most `maxPools` opened
/// pools stay open; the least recently used one is closed when another is
/// needed (its borrowed connections close as they come back). Registered pools
/// are never evicted.
///
/// ```java
/// TenantDataSources tenants = TenantDataSources.fromConfig(config);
/// tenants.migrateAll(Arrays.asList("acme", "globex"), config);
/// EntityManager entities = EntityManager.open(tenants.current());
/// ```
public final class TenantDataSources {
    private final String template;
    private final String initTemplate;
    private final int poolSize;
    private final int maxPools;
    private final Map<String, DataSource> registered = new LinkedHashMap<String, DataSource>();
    private final LinkedHashMap<String, DataSource> opened = new LinkedHashMap<String, DataSource>(16, 0.75f, true);
    /// Evicted pools still lending a connection. A caller may hold the
    /// DataSource it was handed, so closing one at eviction failed that
    /// caller's next borrow; it is closed once nothing is borrowed from it.
    private final java.util.List<DataSource> retired = new java.util.ArrayList<DataSource>();

    /// A database per tenant.
    /// @param template the URL with `${tenant}` in it, or null to accept only
    /// registered pools
    /// @param poolSize connections per tenant pool
    /// @param maxPools how many template pools stay open at once
    public TenantDataSources(String template, int poolSize, int maxPools) {
        this(template, null, poolSize, maxPools);
    }

    /// A database or a schema per tenant.
    /// @param template the URL, or null to accept only registered pools
    /// @param initTemplate the statement each connection runs as it opens, or null
    /// @param poolSize connections per tenant pool
    /// @param maxPools how many template pools stay open at once
    /// @throws IllegalArgumentException when neither template names `${tenant}`,
    /// which would hand every tenant the same database
    public TenantDataSources(String template, String initTemplate, int poolSize, int maxPools) {
        if (template != null && template.indexOf("${tenant}") < 0
                && (initTemplate == null || initTemplate.indexOf("${tenant}") < 0)) {
            throw new IllegalArgumentException("The URL or the init statement must contain ${tenant}");
        }
        requirePrefix(template);
        requirePrefix(initTemplate);
        if (poolSize < 0 || maxPools < 1) {
            throw new IllegalArgumentException("poolSize cannot be negative and maxPools must be positive");
        }
        this.template = template;
        this.initTemplate = initTemplate;
        this.poolSize = poolSize;
        this.maxPools = maxPools;
    }

    /// Every `${tenant}` must follow a letter, digit or underscore, as in
    /// `app_${tenant}` or `tenant_${tenant}`. A bare `${tenant}` lets an
    /// identifier name an existing database or schema -- `postgres`, `public`,
    /// `information_schema`, `mysql`, another application's -- and a tenant
    /// that arrived in a request header would then open a pool on it.
    private static void requirePrefix(String template) {
        if (template == null) {
            return;
        }
        for (int at = template.indexOf("${tenant}"); at >= 0; at = template.indexOf("${tenant}", at + 1)) {
            char before = at == 0 ? ' ' : template.charAt(at - 1);
            boolean prefixed = before >= 'a' && before <= 'z' || before >= 'A' && before <= 'Z'
                    || before >= '0' && before <= '9' || before == '_';
            if (!prefixed) {
                throw new IllegalArgumentException("Prefix ${tenant} so a tenant cannot name a shared database "
                        + "or schema, as in app_${tenant}: " + template);
            }
        }
    }

    /// Reads `cn1.tenancy.datasource.url` (falling back to `cn1.datasource.url`),
    /// `cn1.tenancy.datasource.init`, `cn1.tenancy.datasource.pool.size` and
    /// `cn1.tenancy.datasource.maxPools`.
    /// @param config the configuration
    /// @return the pools
    /// @throws IOException when no URL is configured
    public static TenantDataSources fromConfig(Config config) throws IOException {
        String url = config.get(Config.TENANCY_DATASOURCE_URL, null);
        if (url == null) {
            url = config.get(Config.DATASOURCE_URL, null);
        }
        if (url == null) {
            throw new IOException("No tenant database is configured. Set " + Config.TENANCY_DATASOURCE_URL
                    + " to a URL with ${tenant} in it, or " + Config.DATASOURCE_URL + " and "
                    + Config.TENANCY_CONNECTION_INIT + " for a schema per tenant.");
        }
        return new TenantDataSources(url, config.get(Config.TENANCY_CONNECTION_INIT, null),
                config.getInt(Config.TENANCY_POOL_SIZE, 0), config.getInt(Config.TENANCY_MAX_POOLS, 32));
    }

    /// Registers a pool someone else opened for `tenant`.
    /// @param tenant the tenant identifier
    /// @param pool its pool
    public synchronized void register(String tenant, DataSource pool) {
        registered.put(TenantSource.Ids.check(tenant), pool);
    }

    /// The pool of the current tenant.
    /// @return the pool
    /// @throws IOException when it cannot be opened
    /// @throws com.codename1.orm.session.MissingTenantException when no tenant is current
    public DataSource current() throws IOException {
        String tenant = TenantContext.get();
        if (tenant == null) {
            throw new com.codename1.orm.session.MissingTenantException("No current tenant to choose a database for");
        }
        return forTenant(tenant);
    }

    /// The pool of `tenant`, opening it when needed.
    /// @param tenant the tenant identifier
    /// @return the pool
    /// @throws IOException when it cannot be opened
    public synchronized DataSource forTenant(String tenant) throws IOException {
        TenantSource.Ids.check(tenant);
        DataSource pool = registered.get(tenant);
        if (pool != null) {
            return pool;
        }
        pool = opened.get(tenant);
        if (pool != null) {
            return pool;
        }
        if (template == null) {
            throw new IOException("No pool is registered for tenant " + tenant);
        }
        // Evict before opening, so the count of open pools never passes the cap.
        sweepRetired();
        while (opened.size() >= maxPools) {
            Iterator<Map.Entry<String, DataSource>> eldest = opened.entrySet().iterator();
            DataSource evicted = eldest.next().getValue();
            eldest.remove();
            retired.add(evicted);
        }
        sweepRetired();
        // The identifier is restricted to [A-Za-z0-9_.-] by Ids.check, which is
        // what makes splicing it into a URL and a statement safe.
        pool = DataSource.open(template.replace("${tenant}", tenant), poolSize,
                initTemplate == null ? null : initTemplate.replace("${tenant}", tenant));
        opened.put(tenant, pool);
        return pool;
    }

    /// Runs the application's migrations against every tenant in `tenants`, one
    /// after another; the first failure stops the run and names the tenant.
    /// @param tenants the tenants
    /// @param config the configuration the migrations read
    /// @return the total number of migrations that ran
    /// @throws IOException when a database fails
    public int migrateAll(Iterable<String> tenants, Config config) throws IOException {
        int ran = 0;
        for (String tenant : tenants) {
            try {
                ran += Migrations.migrate(forTenant(tenant), config);
            } catch (IOException e) {
                throw new IOException("Migrating tenant " + tenant + " failed: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Migrating tenant " + tenant + " failed: " + e.getMessage(), e);
            }
        }
        return ran;
    }

    /// Closes the retired pools nothing is borrowed from. One that is still
    /// lending stays usable for its holder and is looked at again next time.
    private void sweepRetired() {
        for (Iterator<DataSource> it = retired.iterator(); it.hasNext();) {
            DataSource pool = it.next();
            if (pool.getOpenCount() == pool.getIdleCount()) {
                pool.close();
                it.remove();
            }
        }
    }

    /// Closes every pool this object opened. Registered pools stay open.
    public synchronized void close() {
        for (DataSource pool : opened.values()) {
            pool.close();
        }
        for (DataSource pool : retired) {
            pool.close();
        }
        opened.clear();
        retired.clear();
    }
}
