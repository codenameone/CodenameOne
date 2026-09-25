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

import java.util.List;

/// A mutable query obtained from {@link Session#createQuery(String, Class)}.
/// Supports entity and scalar selections, relationship joins, aggregates, grouping,
/// ordering, predicate subqueries, and transactional bulk update/delete statements.
/// This is a JPQL subset, not a complete JPA implementation; unsupported syntax is
/// rejected during query creation. Bind named parameters before execution.
///
/// Selects flush pending changes in an active transaction and return managed
/// entities or scalar values. Multiple selected values produce `Object[]` tuples.
/// Fetch joins initialize relationships separately. Keep the owning session open;
/// queries are not thread-safe. Applications should not implement this interface.
/// @param <T> result type
public interface JpqlQuery<T> {
    /// Binds a named parameter used by this statement.
    /// @param name parameter name without the colon
    /// @param value bound value; null is allowed
    /// @return this query
    /// @throws IllegalArgumentException if the statement does not use this parameter
    JpqlQuery<T> setParameter(String name, Object value);

    /// Sets the maximum number of selected results.
    /// @param value nonnegative maximum; zero returns no rows
    /// @return this query
    /// @throws IllegalArgumentException if value is negative
    JpqlQuery<T> limit(int value);

    /// Sets the number of selected results to skip.
    /// @param value nonnegative offset
    /// @return this query
    /// @throws IllegalArgumentException if value is negative
    JpqlQuery<T> offset(int value);

    /// Executes a select with the configured pagination.
    /// @return selected entities, scalars, or tuples
    /// @throws IllegalStateException if this is a bulk mutation
    /// @throws IllegalArgumentException if a named parameter is unbound
    /// @throws PersistenceException if a result does not match the requested type or SQL fails
    List<T> list();

    /// Executes a select for at most one result, respecting the offset.
    /// @return the first result, or null if none exists or the limit is zero
    /// @throws IllegalStateException if this is a bulk mutation
    /// @throws IllegalArgumentException if a named parameter is unbound
    T first();

    /// Executes a bulk update or delete in an active transaction.
    /// Flushes first, bypasses entity cascades and lifecycle callbacks, then clears
    /// the session so subsequent reads cannot reuse stale managed entities.
    /// @return number of affected rows
    /// @throws IllegalStateException if this is a select
    /// @throws IllegalArgumentException if pagination is set or a parameter is unbound
    /// @throws PersistenceException if no usable transaction is active or SQL fails
    int executeUpdate();

}
