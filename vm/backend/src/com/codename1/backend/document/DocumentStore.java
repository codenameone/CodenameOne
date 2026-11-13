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
package com.codename1.backend.document;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/// The operations [DocumentSession] needs of a document database.
///
/// A document is a `Map` of strings, numbers, booleans, dates and byte arrays.
/// Its identifier is held in the field named when the collection is opened, and
/// is a `String` or a `Number`.
public interface DocumentStore extends Closeable {
    /// Prepares a collection keyed by `idField`; an existing one is kept.
    void ensureCollection(String collection, String idField) throws IOException;

    /// Inserts a new document; returns false when one with the same id exists.
    boolean insert(String collection, String idField, Map document) throws IOException;

    /// Replaces the document with the same id. When `versionField` is not null it
    /// replaces only a document whose version is `expectedVersion`. Returns
    /// whether a document was replaced.
    boolean replace(String collection, String idField, Map document, String versionField,
            Object expectedVersion) throws IOException;

    /// The document with this id, or null.
    Map get(String collection, String idField, Object id) throws IOException;

    /// Deletes the document with this id; returns whether there was one. A
    /// non-null `versionField` deletes only the version `expectedVersion`.
    boolean delete(String collection, String idField, Object id, String versionField,
            Object expectedVersion) throws IOException;

    /// The documents matching every criterion, ordered by `sortField` (null for
    /// the store's order), skipping `offset` and returning at most `limit`
    /// (negative for no limit).
    List find(String collection, String idField, List criteria, String sortField,
            boolean ascending, int offset, int limit) throws IOException;

    /// How many documents match every criterion.
    long count(String collection, String idField, List criteria) throws IOException;

    /// A new identifier for a document whose id is generated.
    String newId();

    /// Releases the store's connections.
    @Override
    void close();
}
