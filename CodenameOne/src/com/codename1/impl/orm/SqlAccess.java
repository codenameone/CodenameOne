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
package com.codename1.impl.orm;

import com.codename1.orm.session.LockMode;

import java.io.IOException;
import java.util.List;

/// Runtime adapter. A session owns this adapter, never a global connection.
///
/// Internal ORM runtime; not an application API.
/// @hidden
public interface SqlAccess {
    /// Returns the dialect identifier used for schema and query choices.
    String dialect();
    /// Rows contain column name, declared SQL type, not-null flag, primary-key flag.
    List<Object[]> describe(String table) throws IOException;
    /// Quotes a single SQL identifier for this dialect.
    String quote(String name);
    /// Maps an Attribute storage-kind constant to its SQL type.
    String columnType(int kind);
    /// Returns the DDL for a generated key using an already quoted column name.
    String generatedKeyColumn(int kind, String quotedName);
    /// Returns the DDL type for an application-assigned key.
    String assignedKeyColumn(int kind);
    /// Returns an insert-default-values statement for an already quoted table name.
    String insertDefaults(String quotedTable);
    /// Returns the pagination clause; a negative limit means unlimited results.
    String limit(int limit, int offset);
    /// Applies ordering collation to a selected expression so DISTINCT can order it.
    String orderValue(String expression, int kind);
    /// Renders an ordering term with portable null placement and text collation.
    String orderBy(String expression, boolean ascending, int kind);
    /// Returns a portable LIKE operator with one pattern placeholder.
    String likeOperator(boolean escaped);
    /// Converts a bound LIKE pattern; null escape means no escape character.
    String likePattern(String pattern, String escape);
    /// Converts an unescaped SQL pattern expression for the dialect's LIKE operator.
    String likeExpression(String expression);
    /// Returns the lock clause, or rejects a mode unsupported by the database.
    String lockClause(LockMode mode);
    /// Executes a bound query and returns ordered rows matching the supplied storage kinds.
    List<Object[]> query(String sql, Object[] params, int[] kinds) throws IOException;
    /// Executes a bound mutation and returns the number of affected rows.
    int execute(String sql, Object[] params) throws IOException;
    /// Inserts a row and returns the generated integral key for the named key column.
    long insert(String sql, Object[] params, String keyColumn) throws IOException;
    /// Creates storage required by a generated identifier strategy.
    void prepareGenerator(int strategy, String name) throws IOException;
    /// Allocates the next identifier for the named generator and storage kind.
    Object nextIdentifier(int strategy, String name, int kind) throws IOException;
    /// Begins and reserves the session connection until commit or rollback.
    void begin() throws IOException;
    /// Commits the reserved connection and releases the transaction reservation.
    void commit() throws IOException;
    /// Rolls back the reserved connection and releases the transaction reservation.
    void rollback() throws IOException;
    /// Releases resources owned by this adapter without closing a manager-owned database.
    void close() throws IOException;
}
