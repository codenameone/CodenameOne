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
package com.codename1.impl.orm;

/// Implemented by a [SqlAccess] that knows the current tenant. A session over a
/// connection that does not implement it treats
/// a tenant field as an ordinary column. Only the backend implements it; the
/// tenant annotation, the exceptions and the context all live there.
@com.codename1.impl.SharedWithBackend
public interface TenantSource {
    /// The current tenant, or null when none is set.
    String currentTenant();

    /// True while the caller has explicitly asked to see every tenant, for a
    /// migration or a cross-tenant job.
    boolean isUnscoped();

    /// True while the caller has opted in to native SQL on a session that maps
    /// tenant-partitioned entities, taking on the tenant condition itself.
    boolean isNativeSqlAllowed();

    /// No tenant is current. See [#failure(int, String)].
    int MISSING = 0;
    /// A row or a session belongs to another tenant.
    int MISMATCH = 1;
    /// Native SQL without the opt-in.
    int NATIVE_SQL = 2;

    /// The exception to throw for a tenancy failure of `kind`. The exception
    /// types are backend classes, so the shared session asks for them here.
    ///
    /// @param kind [#MISSING], [#MISMATCH] or [#NATIVE_SQL]
    /// @param message failure description
    /// @return the exception to throw
    RuntimeException failure(int kind, String message);

    /// Tenant identifier rules shared by every layer.
    @com.codename1.impl.SharedWithBackend
    final class Ids {
        private Ids() {
        }

        /// Answers `tenant` when it is 1 to 64 characters of `[A-Za-z0-9_.-]`.
        /// The identifier is written into SQL as a literal and into schema and
        /// database names, so nothing else is accepted.
        ///
        /// @param tenant the identifier
        /// @return the same identifier
        /// @throws IllegalArgumentException when it does not qualify
        public static String check(String tenant) {
            int n = tenant == null ? 0 : tenant.length();
            if (n == 0 || n > 64) {
                throw new IllegalArgumentException("Tenant identifier must be 1 to 64 characters");
            }
            for (int i = 0; i < n; i++) {
                char c = tenant.charAt(i);
                boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' ||
                             c == '_' || c == '.' || c == '-';
                if (!ok) {
                    throw new IllegalArgumentException("Illegal character in tenant identifier: " + tenant);
                }
            }
            return tenant;
        }
    }
}
