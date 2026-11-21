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
package com.codename1.backend.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Database;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/// An application carries one server engine, chosen when it is built. These
/// hold the two halves that make that safe: a URL is still recognised as
/// naming ANY engine, and one naming an engine that is not compiled in is
/// refused by name instead of being opened as a SQLite file.
class EnginesTest {
    @Test
    void namesTheEngineOfAUrlIgnoringCase() {
        assertEquals(Engines.POSTGRES, Engines.named("postgres://u@h/db"));
        assertEquals(Engines.POSTGRES, Engines.named("PostgreSQL://u@h/db"));
        assertEquals(Engines.MYSQL, Engines.named("mysql://u@h/db"));
        assertEquals(Engines.MYSQL, Engines.named("MariaDB://u@h/db"));
        assertEquals(Engines.SQLSERVER, Engines.named("sqlserver://u@h/db"));
        assertEquals(Engines.SQLSERVER, Engines.named("mssql://u@h/db"));
        assertEquals(Engines.ORACLE, Engines.named("oracle://u@h/svc"));
    }

    @Test
    void anythingElseIsASqlitePath() {
        assertNull(Engines.named(":memory:"));
        assertNull(Engines.named("data/app.db"));
        assertNull(Engines.named("postgres.db"));
        assertNull(Engines.named(null));
    }

    @Test
    void canonicalNamesAcceptTheAliases() {
        assertEquals(Engines.SQLITE, Engines.canonical("none"));
        assertEquals(Engines.SQLITE, Engines.canonical(" SQLite "));
        assertEquals(Engines.POSTGRES, Engines.canonical("postgresql"));
        assertEquals(Engines.MYSQL, Engines.canonical("MariaDB"));
        assertEquals(Engines.SQLSERVER, Engines.canonical("mssql"));
        assertEquals(Engines.ORACLE, Engines.canonical("oracle"));
        assertNull(Engines.canonical("db2"));
        assertNull(Engines.canonical(null));
    }

    @Test
    void defaultPorts() {
        assertEquals(5432, Engines.defaultPort(Engines.POSTGRES));
        assertEquals(3306, Engines.defaultPort(Engines.MYSQL));
        assertEquals(1433, Engines.defaultPort(Engines.SQLSERVER));
        assertEquals(1521, Engines.defaultPort(Engines.ORACLE));
    }

    @Test
    void anEngineThatIsNotCompiledInIsRefusedByName() throws IOException {
        Engines.require(Engines.MYSQL, Engines.MYSQL);
        IOException other = assertThrows(IOException.class,
                () -> Engines.require(Engines.POSTGRES, Engines.MYSQL));
        assertTrue(other.getMessage().contains("names postgres"), other.getMessage());
        assertTrue(other.getMessage().contains("built for mysql"), other.getMessage());
        assertTrue(other.getMessage().contains("cn1.backend.database=postgres"), other.getMessage());
        IOException alone = assertThrows(IOException.class,
                () -> Engines.require(Engines.POSTGRES, Engines.SQLITE));
        assertTrue(alone.getMessage().contains("SQLite alone"), alone.getMessage());
        IOException undeclared = assertThrows(IOException.class,
                () -> Engines.require(Engines.POSTGRES, null));
        assertTrue(undeclared.getMessage().contains("no database engine declared"),
                undeclared.getMessage());
    }

    /// The facade asks before it parses or connects: with no engine declared in
    /// this JVM, a server URL fails on the declaration and never on a socket.
    @Test
    void theFacadeRefusesBeforeConnecting() {
        if (ServerEngine.built() != null) {
            return;
        }
        IOException err = assertThrows(IOException.class,
                () -> Database.open("postgres://u:p@127.0.0.1:1/db"));
        assertTrue(err.getMessage().contains("cn1.backend.database=postgres"), err.getMessage());
    }
}
