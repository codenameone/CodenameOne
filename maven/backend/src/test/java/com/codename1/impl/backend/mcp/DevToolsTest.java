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
package com.codename1.impl.backend.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which SQLite pragmas backend_sql runs without write=true. */
class DevToolsTest {

    @Test
    @DisplayName("only pragmas that read run unconfirmed, in either spelling")
    void pragmaAllowList() {
        assertTrue(DevTools.readOnlyPragma("PRAGMA table_info(notes)"));
        assertTrue(DevTools.readOnlyPragma("pragma main.table_list"));
        assertTrue(DevTools.readOnlyPragma("PRAGMA busy_timeout"));
        assertFalse(DevTools.readOnlyPragma("PRAGMA busy_timeout(0)"),
                "a setting given in parentheses was run unconfirmed");
        assertFalse(DevTools.readOnlyPragma("PRAGMA cache_size = 123"));
        assertFalse(DevTools.readOnlyPragma("PRAGMA writable_schema(ON)"));
        assertFalse(DevTools.readOnlyPragma("PRAGMA optimize"));
    }

    @Test
    @DisplayName("credentials in exporter headers and URL queries are masked")
    void credentialsInValuesAreMasked() {
        assertTrue(DevTools.secret("cn1.otel.headers", "api-key=abc123"));
        assertTrue(DevTools.secret("cn1.datasource.url",
                "postgres://db.example/app?user=app&password=hunter2"));
        assertTrue(DevTools.secret("DATABASE_URL", "postgres://app:hunter2@db/app"));
        assertFalse(DevTools.secret("cn1.server.port", "8080"));
        assertFalse(DevTools.secret("cn1.datasource.url", "sqlite:app.db"));
    }
}
