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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeclaredCollationTest {
    @Test
    void postgresqlNullIsTheDefaultCollation() {
        assertFalse(SessionImpl.declaredCollation("C", null, true));
        assertTrue(SessionImpl.declaredCollation("default", null, true));
    }

    @Test
    void postgresqlComparesExactlyAndReadsTheNondeterministicMark() {
        assertTrue(SessionImpl.declaredCollation("und-x-icu", "nondeterministic und-x-icu", true));
        assertFalse(SessionImpl.declaredCollation("C", "c", true));
    }

    @Test
    void otherEnginesFoldCaseAndPassAnUnknownCollation() {
        assertTrue(SessionImpl.declaredCollation("utf8mb4_0900_ai_ci", "UTF8MB4_0900_AI_CI", false));
        assertFalse(SessionImpl.declaredCollation("utf8mb4_bin", "utf8mb4_0900_ai_ci", false));
        assertTrue(SessionImpl.declaredCollation("NOCASE", null, false));
    }
}
