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

/// The transaction every generated repository method runs in.
///
/// Spring Data runs each repository method REQUIRED: it joins the caller's
/// transaction or opens its own, read-only for a finder. A generated repository
/// does the same by bracketing every method with [#begin] and [#end]. The
/// client installs nothing, so there the calls are free and the session is used
/// as the caller left it; the backend installs a scope over its own
/// transactions when it starts.
@com.codename1.impl.SharedWithBackend
public final class RepositoryTransactions {
    private static Scope scope;

    private RepositoryTransactions() {
    }

    /// What the backend installs.
    public interface Scope {
        /// Opens or joins a transaction for a repository bound to `session`;
        /// the token is handed to `end`. Null leaves the call alone -- a
        /// session the application opened itself runs its own transaction and
        /// is not the scope's to manage.
        ///
        /// `propagation` is the ordinal of the backend's `Propagation`
        /// (REQUIRED unless `@Transactional` on the repository says otherwise),
        /// `timeoutSeconds` is -1 for none and `isolation` the name of an
        /// `Isolation` constant.
        Object begin(com.codename1.orm.session.Session session, int propagation, boolean readOnly,
                     int timeoutSeconds, String isolation);

        /// Commits the call when `succeeded`, otherwise rolls it back.
        void end(Object token, boolean succeeded);
    }

    /// Installs the scope; null removes it.
    public static synchronized void install(Scope value) {
        scope = value;
    }

    /// Called first by every generated method; null when nothing is installed.
    public static Object begin(com.codename1.orm.session.Session session, int propagation, boolean readOnly,
                               int timeoutSeconds, String isolation) {
        Scope s = scope;
        return s == null ? null : s.begin(session, propagation, readOnly, timeoutSeconds, isolation);
    }

    /// Called last by every generated method, in a finally block.
    public static void end(Object token, boolean succeeded) {
        Scope s = scope;
        if (s != null && token != null) {
            s.end(token, succeeded);
        }
    }
}
