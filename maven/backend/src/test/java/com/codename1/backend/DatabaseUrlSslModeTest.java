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
package com.codename1.backend;

import com.codename1.backend.sql.SqlLimitsAccess;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every accepted sslmode spelling and what it folds to. verify-full folding into
/// require is only safe while require checks the host name, so that is asserted
/// here beside the mapping.
class DatabaseUrlSslModeTest {
    @Test
    void canonicalSpellings() throws IOException {
        assertEquals("disable", Database.Url.canonicalSslMode("disable"));
        assertEquals("disable", Database.Url.canonicalSslMode("DISABLED"));
        assertEquals("disable", Database.Url.canonicalSslMode("false"));
        assertEquals("prefer", Database.Url.canonicalSslMode("prefer"));
        assertEquals("prefer", Database.Url.canonicalSslMode("PREFERRED"));
        assertEquals("require", Database.Url.canonicalSslMode("require"));
        assertEquals("require", Database.Url.canonicalSslMode("REQUIRED"));
        assertEquals("require", Database.Url.canonicalSslMode("true"));
        assertEquals("verify-ca", Database.Url.canonicalSslMode("verify-ca"));
        assertEquals("verify-ca", Database.Url.canonicalSslMode("VERIFY_CA"));
    }

    @Test
    void verifyFullFoldsIntoARequireThatChecksTheHost() throws IOException {
        assertEquals("require", Database.Url.canonicalSslMode("verify-full"));
        assertEquals("require", Database.Url.canonicalSslMode("VERIFY_IDENTITY"));
        assertTrue(SqlLimitsAccess.verifiesHost("require"));
    }

    @Test
    void unknownModeIsRefused() {
        assertThrows(IOException.class, () -> Database.Url.canonicalSslMode("allow"));
        assertThrows(IOException.class, () -> Database.Url.canonicalSslMode(null));
    }
}
