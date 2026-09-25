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

/// A mutable, parameterized query obtained from {@link Session#query(Class)}.
/// Uses Java attribute names and joins relationship paths as needed. Predicates are
/// combined with AND. Execution flushes pending changes in an active transaction.
/// Keep the owning session open; queries are not thread-safe. Applications should
/// not implement this interface.
/// @param <T> mapped entity type
public interface Query<T> {
    /// Adds an inner join on a relationship path.
    /// @param path Java relationship path
    /// @return this query
    Query<T> join(String path);

    /// Adds a left join before the path is used in predicates.
    /// @param path Java relationship path
    /// @return this query
    Query<T> leftJoin(String path);

    /// Initializes a direct relationship for returned entities, overriding lazy mapping.
    /// @param field Java relationship name
    /// @return this query
    Query<T> fetch(String field);

    /// Adds a scalar collection membership predicate without loading the collection.
    /// @param field Java element-collection name
    /// @param value element to match; null matches SQL NULL
    /// @return this query
    Query<T> containsElement(String field, Object value);

    /// Adds an equality predicate; null matches SQL NULL.
    /// @param field Java attribute name or joined path
    /// @param value bound comparison value
    /// @return this query
    Query<T> eq(String field, Object value);

    /// Adds an inequality predicate; null matches SQL IS NOT NULL.
    /// @param field Java attribute name or joined path
    /// @param value bound comparison value
    /// @return this query
    Query<T> ne(String field, Object value);

    /// Adds a greater-than predicate.
    /// @param field Java attribute name or joined path
    /// @param value bound comparison value
    /// @return this query
    Query<T> gt(String field, Object value);

    /// Adds a greater-than-or-equal predicate.
    /// @param field Java attribute name or joined path
    /// @param value bound comparison value
    /// @return this query
    Query<T> ge(String field, Object value);

    /// Adds a less-than predicate.
    /// @param field Java attribute name or joined path
    /// @param value bound comparison value
    /// @return this query
    Query<T> lt(String field, Object value);

    /// Adds a less-than-or-equal predicate.
    /// @param field Java attribute name or joined path
    /// @param value bound comparison value
    /// @return this query
    Query<T> le(String field, Object value);

    /// Adds a SQL LIKE predicate.
    /// @param field Java string attribute name or joined path
    /// @param pattern bound SQL pattern; percent and underscore are wildcards
    /// @return this query
    Query<T> like(String field, String pattern);

    /// Adds an IS NULL predicate.
    /// @param field Java attribute name or joined path
    /// @return this query
    Query<T> isNull(String field);

    /// Adds an IS NOT NULL predicate.
    /// @param field Java attribute name or joined path
    /// @return this query
    Query<T> isNotNull(String field);

    /// Adds an IN predicate; an empty array matches no rows.
    /// @param field Java attribute name or joined path
    /// @param values values bound as parameters
    /// @return this query
    Query<T> in(String field, Object... values);

    /// Appends an ordering term. Collection joins require ordering by root fields.
    /// @param field Java attribute name or joined path
    /// @param ascending true for ascending order, false for descending
    /// @return this query
    Query<T> orderBy(String field, boolean ascending);

    /// Sets the maximum number of results; zero returns no rows.
    /// @param value nonnegative result limit
    /// @return this query
    /// @throws IllegalArgumentException if value is negative
    Query<T> limit(int value);

    /// Sets the number of leading results to skip.
    /// @param value nonnegative result offset
    /// @return this query
    /// @throws IllegalArgumentException if value is negative
    Query<T> offset(int value);

    /// Executes the query, applying pagination and fetch requests.
    /// @return managed entities matching all predicates
    List<T> list();

    /// Executes the query for at most one result, respecting the offset.
    /// @return the first managed entity, or null if no match or the limit is zero
    T first();

    /// Counts matching root entities, ignoring ordering and pagination.
    /// @return number of distinct matching entities
    long count();

}
