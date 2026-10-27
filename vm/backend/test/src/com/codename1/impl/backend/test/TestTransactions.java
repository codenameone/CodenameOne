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
package com.codename1.impl.backend.test;

import com.codename1.backend.Transactions;

/// Runs a test method in a transaction that never commits: what a
/// `@DataJpaTest` does to each of its tests, on the JVM and compiled alike.
public final class TestTransactions {
    private TestTransactions() {
    }

    /// The test method's body.
    public interface Body {
        /// Runs it.
        void run() throws Throwable;
    }

    /// Runs `body` in a new outermost transaction and rolls it back, whether the
    /// body returned or threw; what it threw is rethrown after the rollback.
    /// Work the body hands to another thread is not in the transaction, which is
    /// Spring's rule too.
    public static void rollingBack(final Body body) throws Throwable {
        final Throwable[] thrown = new Throwable[1];
        Transactions.execute(new Transactions.Work() {
            @Override
            public Object run() {
                try {
                    body.run();
                } catch (Throwable err) {
                    thrown[0] = err;
                }
                Transactions.setRollbackOnly();
                return null;
            }
        });
        if (thrown[0] != null) {
            throw thrown[0];
        }
    }
}
