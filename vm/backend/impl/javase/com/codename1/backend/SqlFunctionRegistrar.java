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

import java.sql.Connection;
import java.sql.SQLException;

/// Registers a Java scalar function on a JDBC connection, for Db.createFunction.
/// JDBC has no API for that, so the one implementation, SqliteJdbcFunctions, is
/// written against sqlite-jdbc and lives in impl/javase-sqlite, compiled only
/// where that driver is on the class path.
interface SqlFunctionRegistrar {
    void register(Connection connection, String name, int argc,
            java.util.function.Function<Object[], Object> body) throws SQLException;

    /// The provider, looked up once; null when sqlite-jdbc is absent.
    final class Holder {
        static final SqlFunctionRegistrar INSTANCE = find();

        private Holder() {
        }

        private static SqlFunctionRegistrar find() {
            for (SqlFunctionRegistrar r : java.util.ServiceLoader.load(SqlFunctionRegistrar.class,
                    SqlFunctionRegistrar.class.getClassLoader())) {
                return r;
            }
            return null;
        }
    }
}
