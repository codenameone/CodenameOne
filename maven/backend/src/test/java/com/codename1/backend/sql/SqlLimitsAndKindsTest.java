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
package com.codename1.backend.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The sslmode predicates both drivers share, and the column kinds each driver
/// reports for the values that arrive as text.
class SqlLimitsAndKindsTest {
    @Test
    void demandsTlsOnlyForRequireAndVerifyCa() {
        assertTrue(SqlLimits.demandsTls("require"));
        assertTrue(SqlLimits.demandsTls("verify-ca"));
        assertFalse(SqlLimits.demandsTls("prefer"));
        assertFalse(SqlLimits.demandsTls("disable"));
    }

    @Test
    void verifiesHostForEveryModeButVerifyCa() {
        assertTrue(SqlLimits.verifiesHost("require"));
        assertTrue(SqlLimits.verifiesHost("prefer"));
        assertFalse(SqlLimits.verifiesHost("verify-ca"));
    }

    @Test
    void verifyCaNeedsItsOwnRoot() throws IOException {
        assertThrows(IOException.class, () -> SqlLimits.requireRootForVerifyCa("verify-ca", null));
        SqlLimits.requireRootForVerifyCa("verify-ca", "/ca.pem");
        SqlLimits.requireRootForVerifyCa("require", null);
    }

    @Test
    void driverSpellingsMatchTheUrlParser() throws IOException {
        String[] spellings = {"disable", "DISABLED", "false", "prefer", "PREFERRED", "require",
            "REQUIRED", "true", "verify-full", "VERIFY_IDENTITY", "verify-ca", "VERIFY_CA"};
        for (String s : spellings) {
            assertEquals(SqlLimitsAccess.urlCanonical(s), SqlLimits.canonicalSslMode(s), s);
        }
        assertNull(SqlLimits.canonicalSslMode(null));
        assertThrows(IOException.class, () -> SqlLimits.canonicalSslMode("allow"));
    }

    @Test
    void tlsRefusalNamesTheModeAsWritten() throws Exception {
        // A peer that answers the SSLRequest with 'N', as a server without TLS does.
        final java.net.ServerSocket peer = new java.net.ServerSocket(0, 1,
                java.net.InetAddress.getByName("127.0.0.1"));
        Thread answer = new Thread(() -> {
            try (java.net.Socket s = peer.accept()) {
                byte[] request = new byte[8];
                int read = 0;
                while (read < 8) {
                    int n = s.getInputStream().read(request, read, 8 - read);
                    if (n < 0) {
                        return;
                    }
                    read += n;
                }
                s.getOutputStream().write('N');
                s.getOutputStream().flush();
                s.getInputStream().read();
            } catch (IOException ignored) {
                // the client hung up, which is the expected end
            }
        });
        answer.start();
        try {
            IOException err = assertThrows(IOException.class, () -> Postgres.connect("127.0.0.1",
                    peer.getLocalPort(), "db", "u", "pw", "verify-full", null, 5000, 5000));
            assertTrue(err.getMessage().contains("sslmode=verify-full"), err.getMessage());
        } finally {
            peer.close();
            answer.join(5000);
        }
    }

    @Test
    void postgresKindsNameTheZonedTypesConsistently() {
        assertEquals("decimal", Postgres.kind(1700));
        assertEquals("date", Postgres.kind(1082));
        assertEquals("datetime", Postgres.kind(1114));
        assertEquals("datetimetz", Postgres.kind(1184));
        assertEquals("time", Postgres.kind(1083));
        assertEquals("timetz", Postgres.kind(1266));
        assertNull(Postgres.kind(25));
    }

    @Test
    void mySqlKinds() {
        assertEquals("decimal", MySql.kind(0xf6));
        assertEquals("date", MySql.kind(0x0a));
        assertEquals("datetime", MySql.kind(0x07));
        assertEquals("time", MySql.kind(0x0b));
        assertNull(MySql.kind(0x03));
    }
}
