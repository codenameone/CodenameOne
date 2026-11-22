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
package com.codename1.backend;

import com.codename1.db.sql.MySql;
import com.codename1.db.sql.Oracle;
import com.codename1.db.sql.Postgres;
import com.codename1.db.sql.SqlServer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Registers every server engine before each test class. An application gets
 * the engines its declared URLs name from generated code; these tests open
 * URLs from the environment, so they register all four themselves. Loaded
 * through JUnit's extension autodetection (junit-platform.properties).
 */
public class AllEnginesExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        Database.useEngine(Postgres.CONNECTOR);
        Database.useEngine(MySql.CONNECTOR);
        Database.useEngine(SqlServer.CONNECTOR);
        Database.useEngine(Oracle.CONNECTOR);
    }
}
