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

/// Db.createFunction on sqlite-jdbc: each call is handed to the Java function
/// with its arguments converted the way the packaged binary converts them.
/// Found by ServiceLoader (see SqlFunctionRegistrar), so the JavaSE twin
/// compiles and runs without sqlite-jdbc.
public final class SqliteJdbcFunctions implements SqlFunctionRegistrar {
    @Override
    public void register(Connection connection, String name, int argc,
            java.util.function.Function<Object[], Object> body) throws SQLException {
        org.sqlite.Function.create(connection, name, new SqlFunction(body), argc, 0);
    }

    private static final class SqlFunction extends org.sqlite.Function {
        /// SQLite's value type codes, SQLITE_INTEGER through SQLITE_BLOB.
        private static final int TYPE_INTEGER = 1;
        private static final int TYPE_FLOAT = 2;
        private static final int TYPE_TEXT = 3;
        private static final int TYPE_BLOB = 4;

        private final java.util.function.Function<Object[], Object> body;

        SqlFunction(java.util.function.Function<Object[], Object> body) {
            this.body = body;
        }

        @Override
        protected void xFunc() throws SQLException {
            int argc = args();
            Object[] values = new Object[argc];
            for (int i = 0; i < argc; i++) {
                switch (value_type(i)) {
                    case TYPE_INTEGER:
                        values[i] = Long.valueOf(value_long(i));
                        break;
                    case TYPE_FLOAT:
                        values[i] = Double.valueOf(value_double(i));
                        break;
                    case TYPE_TEXT:
                        values[i] = value_text(i);
                        break;
                    case TYPE_BLOB:
                        values[i] = value_blob(i);
                        break;
                    default:
                        values[i] = null;
                        break;
                }
            }
            Object r;
            try {
                r = body.apply(values);
            } catch (RuntimeException err) {
                String message = err.getMessage();
                error(message == null ? err.toString() : message);
                return;
            }
            if (r == null) {
                result();
            } else if (r instanceof Double || r instanceof Float) {
                result(((Number) r).doubleValue());
            } else if (r instanceof Number) {
                result(((Number) r).longValue());
            } else if (r instanceof Boolean) {
                result(((Boolean) r).booleanValue() ? 1 : 0);
            } else if (r instanceof byte[]) {
                result((byte[]) r);
            } else {
                result(r.toString());
            }
        }
    }
}
