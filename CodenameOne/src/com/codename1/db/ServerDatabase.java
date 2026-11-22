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
package com.codename1.db;

import com.codename1.db.sql.Dialect;
import com.codename1.db.sql.QueryRows;
import com.codename1.db.sql.ServerSession;
import com.codename1.ui.Display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// A [Database] on a database server rather than a local SQLite file: the base
/// of [PostgresDatabase], [MySqlDatabase], [SqlServerDatabase] and
/// [OracleDatabase].
///
/// Pick the engine by constructing its class. Only the engines an application
/// names are in it; there is no driver lookup and no URL to parse.
///
/// ```java
/// String password = Preferences.get("dbPassword", "");
/// Database db = new PostgresDatabase("db.example.com", 5432, "inventory", "app", password);
/// try {
///     Cursor c = db.executeQuery("SELECT id, name FROM item WHERE price < ?", 10.0);
///     while (c.next()) {
///         Row r = c.getRow();
///         Log.p(r.getLong(0) + " " + r.getString(1));
///     }
///     c.close();
/// } finally {
///     db.close();
/// }
/// ```
///
/// Every call waits on the network, so none may run on the EDT: they throw
/// there. Statements use `?` for parameters on every engine. Rows are read whole
/// when the query returns, so a cursor can move in both directions.
///
/// The connection is plain TCP. This platform's sockets have no TLS, so
/// `sslMode` must be `disable`, written out: the password and every row cross the
/// network unencrypted, so nothing chooses that for the caller. Any other mode,
/// `prefer` included, is refused rather than quietly downgraded. Reach a server over a network you trust, or
/// through a tunnel.
public class ServerDatabase extends Database {
    private final ServerSession session;

    /// Connects and signs in.
    ///
    /// #### Parameters
    ///
    /// - `connector`: the engine, for example `Postgres.CONNECTOR`
    ///
    /// - `sslMode`: `disable`; anything else is refused
    ///
    /// - `timeoutMillis`: how long connecting may take, and each wait for the server
    ///   after it (signing in, a query's reply); 0 for no limit
    protected ServerDatabase(ServerSession.Connector connector, String host, int port,
                             String database, String user, String password, String sslMode,
                             int timeoutMillis) throws IOException {
        refuseEdt();
        ClientSqlPlatform.install();
        this.session = connector.connect(host, port, database, user, password,
                plainMode(sslMode), null, timeoutMillis, timeoutMillis);
    }

    private static String plainMode(String sslMode) throws IOException {
        if (sslMode != null && ("disable".equalsIgnoreCase(sslMode.trim())
                || "disabled".equalsIgnoreCase(sslMode.trim()))) {
            return "disable";
        }
        throw new IOException("sslMode=" + sslMode + " asks for TLS, which this platform's "
                + "sockets do not provide. Pass \"disable\" to connect unencrypted, over a "
                + "network you trust or a tunnel.");
    }

    private static void refuseEdt() throws IOException {
        if (Display.isInitialized() && Display.getInstance().isEdt()) {
            throw new IOException("A server database waits on the network and cannot be "
                    + "used on the EDT; call it from a background thread");
        }
    }

    /// The SQL dialect of the server this session is connected to.
    public Dialect getDialect() {
        return session.dialect();
    }

    @Override
    public void beginTransaction() throws IOException {
        refuseEdt();
        session.begin();
        inTransaction = true;
    }

    @Override
    public void commitTransaction() throws IOException {
        refuseEdt();
        try {
            session.commit();
        } finally {
            // Whatever the server answered, this session's transaction is over:
            // a failed COMMIT has rolled back or lost the connection.
            inTransaction = false;
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        refuseEdt();
        try {
            session.rollback();
        } finally {
            // Whatever the server answered, this session's transaction is over:
            // a failed COMMIT has rolled back or lost the connection.
            inTransaction = false;
        }
    }

    @Override
    public void close() {
        session.close();
    }

    /// True once the session is closed or its connection was lost.
    public boolean isClosed() {
        return session.isClosed();
    }

    @Override
    public void execute(String sql) throws IOException {
        run(sql, null);
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        run(sql, params);
    }

    @Override
    public void execute(String sql, Object... params) throws IOException {
        run(sql, params);
    }

    /// Runs a statement and returns the number of rows it changed.
    public int executeUpdate(String sql, Object... params) throws IOException {
        return run(sql, params);
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        return query(sql, null);
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        return query(sql, params);
    }

    @Override
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        return query(sql, params);
    }

    private int run(String sql, Object[] params) throws IOException {
        refuseEdt();
        return session.execute(bind(sql, params), params);
    }

    private Cursor query(String sql, Object[] params) throws IOException {
        refuseEdt();
        return new ListCursor(session.query(bind(sql, params), params));
    }

    private String bind(String sql, Object[] params) throws IOException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                if (p != null && !(p instanceof String) && !(p instanceof byte[])
                        && !(p instanceof Long) && !(p instanceof Integer)
                        && !(p instanceof Short) && !(p instanceof Byte)
                        && !(p instanceof Double) && !(p instanceof Float)
                        && !(p instanceof Boolean)) {
                    throw new IOException("Parameter " + (i + 1) + " is a "
                            + p.getClass().getName() + "; bind a String, a number, a "
                            + "Boolean, a byte[] or null");
                }
            }
        }
        return session.dialect().bind(sql, params == null ? 0 : params.length);
    }

    /// A cursor over rows already read.
    private static final class ListCursor implements Cursor, CursorExt {
        private final List rows;
        private final String[] names;
        private int position = -1;
        private boolean closed;

        ListCursor(List rows) throws IOException {
            this.rows = rows;
            List<String> columns = new ArrayList<String>();
            String[] labels = rows instanceof QueryRows ? ((QueryRows) rows).getColumnNames() : null;
            if (labels != null) {
                for (String label : labels) {
                    if (columns.contains(label)) {
                        // A row is a map by label, so a repeated label keeps one value.
                        throw new IOException("The query returns two columns labelled "
                                + label + "; give them distinct aliases");
                    }
                    columns.add(label);
                }
            } else if (!rows.isEmpty()) {
                Iterator it = ((Map) rows.get(0)).keySet().iterator();
                while (it.hasNext()) {
                    columns.add(String.valueOf(it.next()));
                }
            }
            names = columns.toArray(new String[columns.size()]);
        }

        private void open() throws IOException {
            if (closed) {
                throw new IOException("The cursor is closed");
            }
        }

        @Override
        public boolean first() throws IOException {
            return position(0);
        }

        @Override
        public boolean last() throws IOException {
            return position(rows.size() - 1);
        }

        @Override
        public boolean next() throws IOException {
            open();
            if (position < rows.size()) {
                position++;
            }
            return position < rows.size();
        }

        @Override
        public boolean prev() throws IOException {
            open();
            if (position >= 0) {
                position--;
            }
            return position >= 0;
        }

        @Override
        public int getColumnIndex(String columnName) throws IOException {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(columnName)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public String getColumnName(int columnIndex) throws IOException {
            if (columnIndex < 0 || columnIndex >= names.length) {
                throw new IOException("No column " + columnIndex);
            }
            return names[columnIndex];
        }

        @Override
        public int getColumnCount() {
            return names.length;
        }

        @Override
        public int getPosition() {
            return position;
        }

        @Override
        public boolean position(int row) throws IOException {
            open();
            if (row < 0 || row >= rows.size()) {
                return false;
            }
            position = row;
            return true;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public Row getRow() throws IOException {
            open();
            if (position < 0 || position >= rows.size()) {
                throw new IOException("The cursor is not on a row");
            }
            return new MapRow((Map) rows.get(position), names);
        }

        @Override
        public void beforeFirst() {
            position = -1;
        }

        @Override
        public int getCount() {
            return rows.size();
        }
    }

    private static final class MapRow implements Row, RowExt {
        private final Map row;
        private final String[] names;
        private boolean lastNull;

        MapRow(Map row, String[] names) {
            this.row = row;
            this.names = names;
        }

        private Object value(int index) throws IOException {
            if (index < 0 || index >= names.length) {
                throw new IOException("No column " + index);
            }
            Object v = row.get(names[index]);
            lastNull = v == null;
            return v;
        }

        private Number number(int index) throws IOException {
            Object v = value(index);
            if (v == null) {
                return Long.valueOf(0);
            }
            if (v instanceof Number) {
                return (Number) v;
            }
            if (v instanceof String) {
                String s = ((String) v).trim();
                try {
                    if (s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
                        return Double.valueOf(Double.parseDouble(s));
                    }
                    return Long.valueOf(Long.parseLong(s));
                } catch (NumberFormatException e) {
                    IOException io = new IOException("Column " + names[index] + " is not a number: " + s);
                    io.initCause(e);
                    throw io;
                }
            }
            throw new IOException("Column " + names[index] + " is not a number");
        }

        @Override
        public byte[] getBlob(int index) throws IOException {
            Object v = value(index);
            if (v == null || v instanceof byte[]) {
                return (byte[]) v;
            }
            return String.valueOf(v).getBytes("UTF-8");
        }

        @Override
        public double getDouble(int index) throws IOException {
            return number(index).doubleValue();
        }

        @Override
        public float getFloat(int index) throws IOException {
            return number(index).floatValue();
        }

        @Override
        public int getInteger(int index) throws IOException {
            return number(index).intValue();
        }

        @Override
        public long getLong(int index) throws IOException {
            return number(index).longValue();
        }

        @Override
        public short getShort(int index) throws IOException {
            return number(index).shortValue();
        }

        @Override
        public String getString(int index) throws IOException {
            Object v = value(index);
            if (v == null) {
                return null;
            }
            if (v instanceof byte[]) {
                return new String((byte[]) v, "UTF-8");
            }
            return String.valueOf(v);
        }

        @Override
        public int getColumnType(int index) throws IOException {
            Object v = value(index);
            if (v == null) {
                return Database.COLUMN_NULL;
            }
            if (v instanceof Long || v instanceof Integer) {
                return Database.COLUMN_INTEGER;
            }
            if (v instanceof Double || v instanceof Float) {
                return Database.COLUMN_FLOAT;
            }
            if (v instanceof byte[]) {
                return Database.COLUMN_BLOB;
            }
            return Database.COLUMN_TEXT;
        }

        @Override
        public boolean wasNull() {
            return lastNull;
        }
    }
}
