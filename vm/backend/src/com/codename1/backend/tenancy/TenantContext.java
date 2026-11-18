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

import com.codename1.impl.orm.TenantSource;

/// The tenant of the work running on the current thread, the backend's
/// counterpart of Hibernate's `CurrentTenantIdentifierResolver` backed by a
/// thread local.
///
/// A request gets its tenant from [TenantFilter]; a task submitted to an
/// executor carries it across through [#decorator]. Identifiers are 1 to 64
/// characters of `[A-Za-z0-9_.-]`, because they are written into SQL and into
/// database and schema names.
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<String>();
    private static final ThreadLocal<int[]> UNSCOPED = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> NATIVE = new ThreadLocal<int[]>();

    private TenantContext() {
    }

    /// The current tenant, or null when none is set.
    /// @return the tenant identifier
    public static String get() {
        return CURRENT.get();
    }

    /// Sets the current tenant. Prefer [#runAs], which cannot leak the tenant
    /// into the next request served by this thread.
    /// @param tenant the tenant identifier
    /// @throws IllegalArgumentException when the identifier is not valid
    public static void set(String tenant) {
        CURRENT.set(TenantSource.Ids.check(tenant));
    }

    /// Clears the current tenant.
    public static void clear() {
        CURRENT.remove();
    }

    /// Work run under a tenant.
    public interface Work {
        /// Runs the work.
        /// @return its result
        /// @throws Exception whatever the work throws
        Object run() throws Exception;
    }

    /// Runs `work` with `tenant` current, then restores what was current before.
    /// @param tenant the tenant identifier
    /// @param work the work
    /// @return what the work answered
    /// @throws Exception whatever the work throws
    public static Object runAs(String tenant, Work work) throws Exception {
        String previous = CURRENT.get();
        set(tenant);
        try {
            return work.run();
        } finally {
            restore(previous);
        }
    }

    /// Runs `work` with tenant filtering switched off, for a migration, a
    /// report across tenants or a job that provisions one. Entities saved
    /// inside keep whatever tenant they already name. There is deliberately no
    /// switch that turns filtering off for longer than one call.
    /// @param work the work
    /// @return what the work answered
    /// @throws Exception whatever the work throws
    public static Object runUnscoped(Work work) throws Exception {
        return nested(UNSCOPED, work);
    }

    /// True inside [#runUnscoped].
    /// @return whether filtering is off on this thread
    public static boolean isUnscoped() {
        return depth(UNSCOPED);
    }

    /// Runs `work` with native SQL allowed on sessions that map
    /// tenant-partitioned entities. Native statements are never rewritten, so
    /// outside this (or [#runUnscoped]) creating one on such a session throws
    /// `TenantNativeSqlException`. Inside it the statement must carry the
    /// tenant condition itself:
    ///
    /// ```java
    /// TenantContext.allowNativeSql(() -> session.createNativeQuery(
    ///         "SELECT COUNT(*) FROM invoice WHERE tenant_id = :t", Long.class)
    ///         .setParameter("t", TenantContext.get()).first());
    /// ```
    /// @param work the work
    /// @return what the work answered
    /// @throws Exception whatever the work throws
    public static Object allowNativeSql(Work work) throws Exception {
        return nested(NATIVE, work);
    }

    /// True inside [#allowNativeSql].
    /// @return whether native SQL is allowed on this thread
    public static boolean isNativeSqlAllowed() {
        return depth(NATIVE);
    }

    private static Object nested(ThreadLocal<int[]> flag, Work work) throws Exception {
        int[] depth = flag.get();
        if (depth == null) {
            depth = new int[1];
            flag.set(depth);
        }
        depth[0]++;
        try {
            return work.run();
        } finally {
            depth[0]--;
            if (depth[0] == 0) {
                flag.remove();
            }
        }
    }

    private static boolean depth(ThreadLocal<int[]> flag) {
        int[] depth = flag.get();
        return depth != null && depth[0] > 0;
    }

    /// A decorator that carries the submitting thread's tenant into the task,
    /// for `Tasks.executor(name).setTaskDecorator(...)`. An unscoped caller's
    /// task is NOT unscoped: that is a decision each piece of work makes.
    /// @return the decorator
    public static com.codename1.backend.TaskDecorator decorator() {
        return new com.codename1.backend.TaskDecorator() {
            @Override
            public Runnable decorate(final Runnable task) {
                final String tenant = CURRENT.get();
                return new Runnable() {
                    @Override
                    public void run() {
                        String previous = CURRENT.get();
                        if (tenant == null) {
                            CURRENT.remove();
                        } else {
                            CURRENT.set(tenant);
                        }
                        try {
                            task.run();
                        } finally {
                            restore(previous);
                        }
                    }
                };
            }
        };
    }

    private static void restore(String previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
