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
package com.codename1.impl.ios;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import java.io.IOException;

/**
 * Implementation of the database SQL API
 *
 * @author Shai Almog
 */
class DatabaseImpl extends Database {
    private long peer;
    public DatabaseImpl(String dbName) {
        peer = IOSImplementation.nativeInstance.sqlDbCreateAndOpen(dbName);
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

    public void finalize() {
        try {
            super.finalize();
            close();
        } catch(Throwable t) {
        }
    }
    
    @Override
    public void close() throws IOException {
        if(peer != 0) {
            IOSImplementation.nativeInstance.sqlDbClose(peer);
            peer = 0;
        }
    }

    @Override
    public void execute(String sql) throws IOException {
        IOSImplementation.nativeInstance.sqlDbExec(peer, sql, null);
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        IOSImplementation.nativeInstance.sqlDbExec(peer, sql, params);
    }

    public void execute(String sql, Object [] params) throws IOException{
        // temporary workaround, this will probably fail with blobs
        String[] val = new String[params.length];
        for(int iter = 0 ; iter < val.length ; iter++) {
            if(params[iter] == null) {
                val[iter] = null;
            } else {
                val[iter] = "" + params[iter];
            }
        }
        execute(sql, val);
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        return new CursorImpl(IOSImplementation.nativeInstance.sqlDbExecQuery(peer, sql, params));
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        return new CursorImpl(IOSImplementation.nativeInstance.sqlDbExecQuery(peer, sql, null));
    }

    class CursorImpl implements Cursor, Row {
        private long peer;
        private int position = -1;
        public CursorImpl(long peer) {
            this.peer = peer;
        }
        
        @Override
        public boolean first() throws IOException {
            position = -1;
            return IOSImplementation.nativeInstance.sqlCursorFirst(peer);
        }

        @Override
        public boolean last() throws IOException {
            throw new IOException("Unsupported");
        }

        @Override
        public boolean next() throws IOException {
            if(IOSImplementation.nativeInstance.sqlCursorNext(peer)) {
                position++;
                return true;
            }
            return false;
        }

        @Override
        public boolean prev() throws IOException {
            throw new IOException("Unsupported");
        }

        @Override
        public int getColumnIndex(String columnName) throws IOException {
            for(int iter = 0 ; true ; iter++) {
                String n = getColumnName(iter);
                if(n == null) {
                    return -1;
                }
                if(n.equalsIgnoreCase(columnName)) {
                    return iter;
                }
            }
        }

        @Override
        public String getColumnName(int columnIndex) throws IOException {
            return IOSImplementation.nativeInstance.sqlGetColName(peer, columnIndex);
        }

        @Override
        public int getPosition() throws IOException {
            return position;
        }

        @Override
        public boolean position(int row) throws IOException {
            throw new IOException("Unsupported");
        }

        @Override
        public void close() throws IOException {
            if(peer != 0) {
                IOSImplementation.nativeInstance.sqlCursorCloseStatement(peer);
                peer = 0;
            }
        }

        public void finalize() {
            try {
                super.finalize();
                close();
            } catch(Throwable t) {
            }
        }
        
        @Override
        public Row getRow() throws IOException {
            return this;
        }

        @Override
        public byte[] getBlob(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnBlob(peer, index);
        }

        @Override
        public double getDouble(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnDouble(peer, index);
        }

        @Override
        public float getFloat(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnFloat(peer, index);
        }

        @Override
        public int getInteger(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnInteger(peer, index);
        }

        @Override
        public long getLong(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnLong(peer, index);
        }

        @Override
        public short getShort(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnShort(peer, index);
        }

        @Override
        public String getString(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnString(peer, index);
        }

        @Override
        public int getColumnCount() throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorGetColumnCount(peer);
        }
    }
}
