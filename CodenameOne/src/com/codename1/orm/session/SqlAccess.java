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
package com.codename1.orm.session;

import java.io.IOException;
import java.util.List;

/// Runtime adapter. A session owns this adapter, never a global connection.
public interface SqlAccess {
    String dialect();
    /// Rows contain column name, declared SQL type, not-null flag, primary-key flag.
    List<Object[]> describe(String table) throws IOException;
    String quote(String name);
    String columnType(int kind);
    String generatedKeyColumn(int kind, String quotedName);
    String assignedKeyColumn(int kind);
    String insertDefaults(String quotedTable);
    String limit(int limit, int offset);
    String lockClause(LockMode mode);
    List<Object[]> query(String sql, Object[] params, int[] kinds) throws IOException;
    int execute(String sql, Object[] params) throws IOException;
    long insert(String sql, Object[] params, String keyColumn) throws IOException;
    void prepareGenerator(int strategy,String name) throws IOException;
    Object nextIdentifier(int strategy,String name,int kind) throws IOException;
    void begin() throws IOException;
    void commit() throws IOException;
    void rollback() throws IOException;
    void close() throws IOException;
}
