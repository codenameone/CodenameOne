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
package com.codename1.backend.search;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/// A full-text search engine: documents in named indexes, found by text.
///
/// A document is a `Map` of strings, numbers, booleans, lists and nested maps;
/// every string in it is searchable. Index names are lowercase letters, digits,
/// `-` and `_`, the subset every engine accepts.
public interface SearchEngine extends Closeable {
    /// Adds a document under `id`, replacing any already there.
    void index(String index, String id, Map document) throws IOException;

    /// Removes a document; returns whether there was one.
    boolean delete(String index, String id) throws IOException;

    /// The document stored under `id`, or null.
    Map get(String index, String id) throws IOException;

    /// Runs a query.
    SearchResult search(String index, SearchQuery query) throws IOException;

    /// Makes every write so far visible to [#search]. Search clusters publish
    /// writes about once a second; tests and read-your-write flows call this.
    void refresh(String index) throws IOException;

    /// Removes an index and every document in it; a missing one is not an error.
    void deleteIndex(String index) throws IOException;

    /// Releases connections. An engine from [SearchEngines#current()] is closed
    /// by the server and must not be closed by the caller.
    @Override
    void close();
}
