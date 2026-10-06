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
package org.junit.jupiter.api;

import java.util.function.Supplier;
import org.opentest4j.TestAbortedException;

/// Assumptions for compiled backend tests: a test whose assumption does not hold is
/// reported as skipped, not failed.
///
/// Part of the subset of JUnit 5's API that a compiled backend test translates
/// against; see `Test` for why it exists.
public final class Assumptions {
    private Assumptions() {
    }

    public static void assumeTrue(boolean assumption) {
        assumeTrue(assumption, (String) null);
    }

    public static void assumeTrue(boolean assumption, String message) {
        if (!assumption) {
            throw new TestAbortedException("Assumption failed: "
                    + (message == null ? "assumption is not true" : message));
        }
    }

    public static void assumeTrue(boolean assumption, Supplier<String> message) {
        if (!assumption) {
            assumeTrue(false, message == null ? null : message.get());
        }
    }

    public static void assumeFalse(boolean assumption) {
        assumeFalse(assumption, (String) null);
    }

    public static void assumeFalse(boolean assumption, String message) {
        if (assumption) {
            throw new TestAbortedException("Assumption failed: "
                    + (message == null ? "assumption is not false" : message));
        }
    }

    public static void assumeFalse(boolean assumption, Supplier<String> message) {
        if (assumption) {
            assumeFalse(true, message == null ? null : message.get());
        }
    }

    public static <V> V abort(String message) {
        throw new TestAbortedException(message);
    }
}
