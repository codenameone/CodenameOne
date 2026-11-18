/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.backend;

import com.codename1.backend.Config;
import com.codename1.backend.Tasks;
import com.codename1.backend.orm.EntityManager;
import com.codename1.backend.tenancy.TenantContext;
import com.codename1.backend.tenancy.TenantDataSources;
import com.codename1.backend.tenancy.TenantFilter;
import com.codename1.orm.session.Session;
import java.util.Arrays;

/** The multi-tenancy section's examples, compiled so they cannot drift. */
public final class TenancySnippets {

    /** Stands in for the application's own user lookup. */
    public interface Accounts {
        String tenantOf(String user);
    }

    private TenancySnippets() {
    }

    public static void filters() {
// tag::backend-tenancy-filter[]
// Tenant from the first label of the host name: acme.example.com is "acme".
new TenantFilter(TenantFilter.subdomain("example.com"), true);
// Or from a header that a gateway in front of the server sets.
new TenantFilter(TenantFilter.header("X-Tenant-ID"), true);
// end::backend-tenancy-filter[]
    }

    public static TenantFilter authenticated(Accounts accounts) {
        return
// tag::backend-tenancy-principal[]
// accounts.tenantOf is your own lookup, from a user table or a token claim.
new TenantFilter(TenantFilter.authenticated(auth -> accounts.tenantOf(auth.getName())), true);
// end::backend-tenancy-principal[]
    }

    public static Long nativeCount(Session session) throws Exception {
// tag::backend-tenancy-native[]
Long open = (Long) TenantContext.allowNativeSql(() -> session
        .createNativeQuery("SELECT COUNT(*) FROM invoice WHERE tenant_id = :t", Long.class)
        .setParameter("t", TenantContext.get())
        .first());
// end::backend-tenancy-native[]
        return open;
    }

    public static EntityManager perTenant(Config config) throws Exception {
// tag::backend-tenancy-datasources[]
TenantDataSources tenants = TenantDataSources.fromConfig(config);
tenants.migrateAll(Arrays.asList("acme", "globex"), config);
EntityManager entities = EntityManager.open(tenants.current());
// end::backend-tenancy-datasources[]
        return entities;
    }

    public static void background() {
// tag::backend-tenancy-decorator[]
Tasks.executor("mail").setTaskDecorator(TenantContext.decorator());
// end::backend-tenancy-decorator[]
    }
}
