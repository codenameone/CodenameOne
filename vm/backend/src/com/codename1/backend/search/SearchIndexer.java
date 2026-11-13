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

import com.codename1.backend.log.Logger;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.orm.Attribute;
import com.codename1.impl.orm.CommitObserver;
import com.codename1.impl.orm.EntityModel;
import com.codename1.impl.orm.Values;
import java.util.LinkedHashMap;
import java.util.Map;

/// Keeps the index of each `@Searchable` entity in step with what a session
/// committed. The ORM installs one on every session it opens inside a server;
/// an application does not create it.
///
/// The index is written after the data is durable, so a failure here cannot
/// undo the commit: it is logged, and the entity is indexed again the next
/// time it is written.
public final class SearchIndexer implements CommitObserver {
    private static final Logger LOG = Logger.getLogger(SearchIndexer.class);

    // Null: the engine of the server whose thread commits.
    private final SearchEngine engine;

    private SearchIndexer(SearchEngine engine) {
        this.engine = engine;
    }

    /// The indexer for a session opened by the calling thread, or null outside
    /// a server, where there is no search engine to write to.
    public static SearchIndexer forServer() {
        return BackendAccess.get().currentConfig() == null ? null : new SearchIndexer(null);
    }

    /// An indexer writing to `engine`, for a session opened outside a server
    /// (`SessionImpl.setCommitObserver`). The caller keeps ownership of it.
    public static SearchIndexer to(SearchEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("engine");
        }
        return new SearchIndexer(engine);
    }

    @Override
    public void committed(EntityModel<?> model, Object entity, int event) {
        String index = model.searchIndex();
        if (index == null) {
            return;
        }
        String id = id(model, entity);
        try {
            SearchEngine target = engine != null ? engine : SearchEngines.current();
            if (event == REMOVED) {
                target.delete(index, id);
            } else {
                target.index(index, id, document(model, entity));
            }
        } catch (Exception e) {
            LOG.error("could not update search index {} for {}", index, id, e);
        }
    }

    /// The document id: the identifier's columns, joined by `:` when there
    /// are several.
    @SuppressWarnings({"unchecked", "rawtypes"})
    static String id(EntityModel model, Object entity) {
        Object[] key = model.keyValues(model.identifier(entity));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < key.length; i++) {
            if (i > 0) {
                b.append(':');
            }
            b.append(String.valueOf(key[i]));
        }
        return b.toString();
    }

    /// The search document: each indexed scalar attribute, keyed by column.
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Map document(EntityModel model, Object entity) {
        Map doc = new LinkedHashMap();
        Attribute[] attrs = model.attributes();
        for (int i = 0; i < attrs.length; i++) {
            if (!model.searchField(i)) {
                continue;
            }
            Object v = model.get(entity, i);
            if (model.mapping(i).length() > 0) {
                v = model.parameter(i, v);
            }
            v = Values.storage(v);
            // Binary and structured values have no meaning to a text index, and
            // a null is left out: each write replaces the whole document.
            if (v instanceof String || v instanceof Number || v instanceof Boolean) {
                doc.put(attrs[i].column, v);
            }
        }
        return doc;
    }
}
