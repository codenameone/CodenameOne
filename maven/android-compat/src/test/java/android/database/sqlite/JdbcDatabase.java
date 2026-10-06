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
package android.database.sqlite;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.db.RowExt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/// A Codename One database over a JDBC SQLite connection with typed
/// binding, the way the JavaSE port behaves, for running the
/// android.database layer against a real engine without a device.
class JdbcDatabase extends Database {

    private final Connection conn;

    JdbcDatabase() throws IOException {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        } catch (java.sql.SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void beginTransaction() throws IOException {
        execute("BEGIN");
    }

    @Override
    public void commitTransaction() throws IOException {
        execute("COMMIT");
    }

    @Override
    public void rollbackTransaction() throws IOException {
        execute("ROLLBACK");
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (java.sql.SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void execute(String sql) throws IOException {
        try {
            Statement s = conn.createStatement();
            try {
                s.execute(sql);
            } finally {
                s.close();
            }
        } catch (java.sql.SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        execute(sql, (Object[]) params);
    }

    @Override
    public void execute(String sql, Object... params) throws IOException {
        try {
            PreparedStatement ps = prepare(sql, params);
            try {
                ps.execute();
            } finally {
                ps.close();
            }
        } catch (java.sql.SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        return executeQuery(sql, (Object[]) params);
    }

    @Override
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        try {
            return new JdbcCursor(prepare(sql, params));
        } catch (java.sql.SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        return executeQuery(sql, new Object[0]);
    }

    private PreparedStatement prepare(String sql, Object[] params) throws java.sql.SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }
        return ps;
    }

    private static final class JdbcCursor implements Cursor, Row, RowExt {
        private final PreparedStatement ps;
        private ResultSet rs;
        private int pos = -1;
        private boolean exhausted;
        private boolean wasNull;

        JdbcCursor(PreparedStatement ps) throws java.sql.SQLException {
            this.ps = ps;
            this.rs = ps.executeQuery();
        }

        @Override
        public boolean first() throws IOException {
            return position(0);
        }

        @Override
        public boolean last() throws IOException {
            throw new IOException("unsupported");
        }

        /// Like Codename One's cursors, running off the end leaves the
        /// position past the last row, so a later seek rewinds.
        @Override
        public boolean next() throws IOException {
            try {
                if (exhausted) {
                    return false;
                }
                pos++;
                if (rs.next()) {
                    return true;
                }
                exhausted = true;
                return false;
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public boolean prev() throws IOException {
            return position(pos - 1);
        }

        @Override
        public int getColumnIndex(String columnName) throws IOException {
            try {
                return rs.findColumn(columnName) - 1;
            } catch (java.sql.SQLException e) {
                return -1;
            }
        }

        @Override
        public String getColumnName(int columnIndex) throws IOException {
            try {
                return rs.getMetaData().getColumnLabel(columnIndex + 1);
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public int getColumnCount() throws IOException {
            try {
                return rs.getMetaData().getColumnCount();
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public int getPosition() {
            return pos;
        }

        /// Going back re-runs the statement, like the device engines.
        @Override
        public boolean position(int row) throws IOException {
            try {
                if (row < pos || (exhausted && row >= pos)) {
                    rs.close();
                    rs = ps.executeQuery();
                    pos = -1;
                    exhausted = false;
                }
                while (pos < row) {
                    if (!next()) {
                        return false;
                    }
                }
                return row >= 0;
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public void close() throws IOException {
            try {
                rs.close();
                ps.close();
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public Row getRow() {
            return this;
        }

        @Override
        public byte[] getBlob(int index) throws IOException {
            try {
                byte[] b = rs.getBytes(index + 1);
                wasNull = rs.wasNull();
                return b;
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public double getDouble(int index) throws IOException {
            try {
                double d = rs.getDouble(index + 1);
                wasNull = rs.wasNull();
                return d;
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public float getFloat(int index) throws IOException {
            return (float) getDouble(index);
        }

        @Override
        public int getInteger(int index) throws IOException {
            return (int) getLong(index);
        }

        @Override
        public long getLong(int index) throws IOException {
            try {
                long l = rs.getLong(index + 1);
                wasNull = rs.wasNull();
                return l;
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public short getShort(int index) throws IOException {
            return (short) getLong(index);
        }

        @Override
        public String getString(int index) throws IOException {
            try {
                String s = rs.getString(index + 1);
                wasNull = rs.wasNull();
                return s;
            } catch (java.sql.SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public boolean wasNull() {
            return wasNull;
        }
    }
}
