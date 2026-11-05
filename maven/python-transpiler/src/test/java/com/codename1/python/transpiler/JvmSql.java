/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.transpiler;

import com.codename1.python.runtime.PySql;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

/// The `sqlite3` engine of the conformance runs: SQLite through its JDBC
/// driver, which reports each value in its storage class as the device
/// ports do. Relative names are files under the run's own directory.
public final class JvmSql extends PySql {
    private final String root;

    public JvmSql(String root) {
        this.root = root;
    }

    @Override
    public PySql.Connection open(String name) throws IOException {
        String url = ":memory:".equals(name) || name.isEmpty() ? "jdbc:sqlite::memory:"
                : "jdbc:sqlite:" + (new File(name).isAbsolute() ? name : new File(root, name).getPath());
        try {
            return new Conn(new org.sqlite.JDBC().connect(url, new Properties()));
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static final class Conn extends PySql.Connection {
        private final java.sql.Connection c;

        Conn(java.sql.Connection c) {
            this.c = c;
        }

        @Override
        public Result run(String sql, Object[] params, boolean query) throws IOException {
            Result r = new Result();
            try (PreparedStatement st = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    Object p = params[i];
                    if (p == null) {
                        st.setNull(i + 1, Types.NULL);
                    } else if (p instanceof Long) {
                        st.setLong(i + 1, (Long) p);
                    } else if (p instanceof Double) {
                        st.setDouble(i + 1, (Double) p);
                    } else if (p instanceof byte[]) {
                        st.setBytes(i + 1, (byte[]) p);
                    } else {
                        st.setString(i + 1, p.toString());
                    }
                }
                if (query) {
                    try (ResultSet rs = st.executeQuery()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int n = md.getColumnCount();
                        r.columns = new String[n];
                        for (int i = 0; i < n; i++) {
                            r.columns[i] = md.getColumnLabel(i + 1);
                        }
                        while (rs.next()) {
                            Object[] row = new Object[n];
                            for (int i = 0; i < n; i++) {
                                Object v = rs.getObject(i + 1);
                                row[i] = v instanceof Integer ? Long.valueOf((Integer) v) : v;
                            }
                            r.rows.add(row);
                        }
                    }
                    return r;
                }
                r.changes = st.executeUpdate();
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                r.lastRowId = rs.getLong(1);
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
            return r;
        }

        private void exec(String sql) throws IOException {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        @Override
        public void begin() throws IOException {
            exec("BEGIN");
        }

        @Override
        public void commit() throws IOException {
            exec("COMMIT");
        }

        @Override
        public void rollback() throws IOException {
            exec("ROLLBACK");
        }

        @Override
        public void close() throws IOException {
            try {
                c.close();
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }
}
