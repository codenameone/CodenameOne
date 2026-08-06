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
package com.codename1.impl.javase;

import com.codename1.db.Database;
import com.codename1.impl.AbstractDBCursor;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Simulator cursor.
 *
 * The SQLite JDBC driver only produces TYPE_FORWARD_ONLY result sets, so seeking backwards is
 * implemented by re-executing the statement and stepping forward again. That is what gives the
 * simulator the same random access the device ports provide; previously first(), last(), prev()
 * and position() all threw here, which meant cursor code could not be developed in the simulator
 * at all.
 *
 * @author Chen
 */
public class SECursor extends AbstractDBCursor {

    private final SEDatabase owner;
    private final PreparedStatement statement;
    private ResultSet resultSet;

    SECursor(SEDatabase owner, PreparedStatement statement, ResultSet resultSet) {
        this.owner = owner;
        this.statement = statement;
        this.resultSet = resultSet;
    }

    @Override
    protected void rewind() throws IOException {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            resultSet = statement.executeQuery();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected boolean stepForward() throws IOException {
        try {
            return resultSet.next();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void closeImpl() throws IOException {
        if (owner != null) {
            owner.unregisterCursor(this);
        }
        SQLException failure = null;
        try {
            if (resultSet != null) {
                resultSet.close();
                resultSet = null;
            }
        } catch (SQLException ex) {
            failure = ex;
        }
        try {
            // The statement belongs to this cursor, not to the caller. Leaving it open leaked one
            // prepared statement per query.
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException ex) {
            if (failure == null) {
                failure = ex;
            }
        }
        if (failure != null) {
            throw new IOException(failure.getMessage(), failure);
        }
    }

    @Override
    protected int columnCount() throws IOException {
        try {
            return resultSet.getMetaData().getColumnCount();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected String columnLabel(int columnIndex) throws IOException {
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            if (Database.isLegacyBehavior()) {
                // The simulator used to report the underlying table column here while resolving
                // names by label, so an aliased column could not be looked up under the name
                // getColumnIndex had found it by.
                return meta.getColumnName(columnIndex + 1);
            }
            return meta.getColumnLabel(columnIndex + 1);
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected boolean isNullAt(int index) throws IOException {
        try {
            return resultSet.getObject(index + 1) == null;
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected String readString(int index) throws IOException {
        try {
            return resultSet.getString(index + 1);
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected byte[] readBlob(int index) throws IOException {
        try {
            return resultSet.getBytes(index + 1);
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected double readDouble(int index) throws IOException {
        try {
            return resultSet.getDouble(index + 1);
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected long readLong(int index) throws IOException {
        try {
            return resultSet.getLong(index + 1);
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected int legacyPositionOffset() {
        // The simulator exposed the JDBC row number directly, which counts from one.
        return Database.isLegacyBehavior() ? 1 : 0;
    }

    @Override
    protected void finalize() throws Throwable {
        if (resultSet != null) {
            System.out.println("**** WARNING! Cursor object was released by the GC without being closed first! *****");
        }
        super.finalize();
    }
}
