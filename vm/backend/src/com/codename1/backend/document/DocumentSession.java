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

import com.codename1.impl.backend.BackendAccess;
import com.codename1.backend.Config;
import com.codename1.backend.aws.Credentials;
import com.codename1.backend.dynamo.DynamoDb;
import com.codename1.backend.mongo.MongoClient;
import com.codename1.backend.search.SearchIndexer;
import com.codename1.impl.orm.Attribute;
import com.codename1.impl.orm.CommitObserver;
import com.codename1.impl.orm.EntityModel;
import com.codename1.impl.orm.Models;
import com.codename1.impl.orm.Values;
import com.codename1.orm.session.OptimisticLockException;
import com.codename1.orm.session.PersistenceException;
import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Persists `@Entity` classes in a [DocumentStore]: one document per entity,
/// one collection per entity table, each field under its column name.
///
/// ```java
/// DocumentSession s = DocumentSession.current();
/// s.persist(order);
/// List<Order> open = s.query(Order.class).eq("status", "OPEN").orderBy("placed", false).list();
/// ```
///
/// What the SQL session does that this one does not is listed in the package
/// description: relationships, numeric generated ids, identifiers of more than
/// two columns, an identity map and multi-call transactions.
public final class DocumentSession implements Closeable {
    private static final String RESOURCE = "cn1.document.session";
    private static final int PRE_PERSIST = 0;
    private static final int POST_PERSIST = 1;
    private static final int PRE_UPDATE = 2;
    private static final int POST_UPDATE = 3;
    private static final int PRE_REMOVE = 4;
    private static final int POST_REMOVE = 5;
    private static final int POST_LOAD = 6;

    private final DocumentStore store;
    // Keeps @Searchable entities indexed; null outside a server.
    private final CommitObserver indexer;

    /// A session over `store`; closing the session closes the store. Opened
    /// inside a server, it keeps each `@Searchable` entity's search index in
    /// step with every write that succeeds.
    public DocumentSession(DocumentStore store) {
        this.store = store;
        this.indexer = SearchIndexer.forServer();
    }

    private <T> void written(EntityModel<T> m, T entity, int event) {
        if (indexer != null) {
            indexer.committed(m, entity, event);
        }
    }

    /// The session of the calling thread's server, opened from `cn1.document.url`
    /// the first time it is asked for and closed when the server stops.
    public static DocumentSession current() throws IOException {
        return (DocumentSession) BackendAccess.get().serverResource(RESOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) throws IOException {
                        return DocumentSession.open(config);
                    }
                });
    }

    /// A session from `cn1.document.url`: a `mongodb://` connection string, or
    /// `dynamodb://REGION`, optionally with `?endpoint=URL` (DynamoDB Local) and
    /// `&prefix=` for the table names. Without the key, `cn1.mongodb.uri` and the
    /// Spring MongoDB properties are used.
    public static DocumentSession open(Config config) throws IOException {
        String url = config.get("cn1.document.url", null);
        if (url == null || url.length() == 0) {
            return new DocumentSession(new MongoDocumentStore(MongoClient.open(config), null, true));
        }
        return open(url);
    }

    /// A session from a document store URL; see [#open(Config)].
    public static DocumentSession open(String url) throws IOException {
        if (url.regionMatches(true, 0, "mongodb://", 0, 10)) {
            return new DocumentSession(new MongoDocumentStore(MongoClient.open(url), null, true));
        }
        if (!url.regionMatches(true, 0, "dynamodb://", 0, 11)) {
            throw new IOException("Unsupported document store URL scheme");
        }
        String rest = url.substring(11);
        String region = rest;
        String endpoint = null;
        String prefix = "";
        int q = rest.indexOf('?');
        if (q >= 0) {
            region = rest.substring(0, q);
            // String.split is not in the device class library; walk the pairs.
            String query = rest.substring(q + 1);
            int start = 0;
            while (start <= query.length()) {
                int amp = query.indexOf('&', start);
                String pair = query.substring(start, amp < 0 ? query.length() : amp);
                int eq = pair.indexOf('=');
                String k = eq < 0 ? pair : pair.substring(0, eq);
                String v = eq < 0 ? "" : pair.substring(eq + 1);
                if ("endpoint".equals(k)) {
                    endpoint = v;
                } else if ("prefix".equals(k)) {
                    prefix = v;
                }
                if (amp < 0) {
                    break;
                }
                start = amp + 1;
            }
        }
        if (region.endsWith("/")) {
            region = region.substring(0, region.length() - 1);
        }
        DynamoDb db = endpoint == null ? DynamoDb.forRegion(region)
                : DynamoDb.forEndpoint(Credentials.resolve(),
                        region.length() == 0 ? "us-east-1" : region, endpoint);
        return new DocumentSession(new DynamoDocumentStore(db, prefix));
    }

    /// The store this session writes to.
    public DocumentStore getStore() {
        return store;
    }

    @SuppressWarnings("unchecked")
    static <T> EntityModel<T> model(Class<T> type) {
        EntityModel<T> m = (EntityModel<T>) Models.snapshot().get(type.getName());
        if (m == null) {
            throw new PersistenceException("Not an entity: " + type.getName());
        }
        if (m.relationships().length > 0) {
            throw new PersistenceException("Relationships are not supported in a document store: "
                    + type.getName());
        }
        keyIndexes(m);
        return m;
    }

    /// The attributes of the store key: the id, or for a two-column id the
    /// partition key and then the `@SortKey` column.
    static int[] keyIndexes(EntityModel<?> m) {
        int[] ids = m.idIndexes();
        if (ids.length == 1) {
            return ids;
        }
        int sort = m.sortKeyIndex();
        if (ids.length != 2 || sort < 0) {
            throw new PersistenceException("A document entity has one id, or two with one of them "
                    + "marked @SortKey: " + m.type().getName());
        }
        return new int[] {ids[0] == sort ? ids[1] : ids[0], sort};
    }

    static String[] keyFields(EntityModel<?> m) {
        int[] k = keyIndexes(m);
        String[] out = new String[k.length];
        for (int i = 0; i < k.length; i++) {
            out[i] = column(m, k[i]);
        }
        return out;
    }

    /// An identifier, in any form the model accepts (a value, an array, an
    /// `@EmbeddedId` object), as a store key in partition-then-sort order.
    static Object[] key(EntityModel<?> m, Object id) {
        Object[] values = m.keyValues(id);
        int[] ids = m.idIndexes();
        int[] k = keyIndexes(m);
        Object[] out = new Object[k.length];
        for (int i = 0; i < k.length; i++) {
            for (int j = 0; j < ids.length; j++) {
                if (ids[j] == k[i]) {
                    out[i] = values[j];
                }
            }
        }
        return out;
    }

    static String column(EntityModel<?> m, int i) {
        return m.attributes()[i].column;
    }

    /// A value as it is written to a document. Strings, numbers, booleans, dates
    /// and bytes keep their own type so the database can index and compare them.
    static Object toDocument(Object v) {
        if (v == null || v instanceof String || v instanceof Long || v instanceof Double
                || v instanceof Boolean || v instanceof Date || v instanceof byte[]) {
            return v;
        }
        return Values.storage(v);
    }

    <T> Map toDocument(EntityModel<T> m, T entity) {
        Attribute[] attrs = m.attributes();
        Map doc = new LinkedHashMap();
        for (int i = 0; i < attrs.length; i++) {
            Object v = m.get(entity, i);
            if (m.mapping(i).length() > 0) {
                v = m.parameter(i, v);
            }
            // A key is written in its storage form, the form a lookup by id
            // builds, so a date or boolean key matches itself.
            doc.put(attrs[i].column, attrs[i].id ? Values.storage(v) : toDocument(v));
        }
        return doc;
    }

    /// A stored value in the form the generated model reads from a SQL row.
    private static Object toRow(Attribute a, Object v) {
        Object s = Values.storage(v);
        if (s instanceof Number && !(s instanceof Long) && !(s instanceof Double)) {
            s = Long.valueOf(((Number) s).longValue());
        }
        if (s instanceof Long && a.kind == Attribute.REAL) {
            return Double.valueOf(((Long) s).doubleValue());
        }
        if (s instanceof Double && a.kind != Attribute.REAL && a.kind != Attribute.TEXT) {
            return Long.valueOf(((Double) s).longValue());
        }
        return s;
    }

    <T> T fromDocument(EntityModel<T> m, Map doc) {
        Attribute[] attrs = m.attributes();
        Object[] row = new Object[attrs.length];
        for (int i = 0; i < attrs.length; i++) {
            row[i] = toRow(attrs[i], doc.get(attrs[i].column));
        }
        T entity;
        if (m.immutable()) {
            entity = m.hydrate(row);
        } else {
            entity = m.create();
            m.read(entity, row);
        }
        m.lifecycle(entity, POST_LOAD);
        return entity;
    }

    private static <T> T assign(EntityModel<T> m, T entity, int i, Object v) {
        if (m.immutable()) {
            return m.with(entity, i, v);
        }
        m.set(entity, i, v);
        return entity;
    }

    /// Creates the collection (or table) for each entity type, if missing.
    public void createCollections(Class... types) throws IOException {
        for (Class eachType : types) {
            EntityModel m = model(eachType);
            int[] k = keyIndexes(m);
            boolean[] numeric = new boolean[k.length];
            for (int i = 0; i < k.length; i++) {
                int kind = m.attributes()[k[i]].kind;
                if (kind == Attribute.BLOB) {
                    throw new PersistenceException("A document key cannot be binary: "
                            + m.type().getName());
                }
                numeric[i] = kind != Attribute.TEXT;
            }
            store.ensureCollection(m.table(), keyFields(m), numeric);
        }
    }

    /// Inserts `entity`. A null generated text id is assigned first; the entity
    /// returned carries it (a new instance for an immutable entity).
    public <T> T persist(T entity) throws IOException {
        @SuppressWarnings("unchecked")
        EntityModel<T> m = model((Class<T>) entity.getClass());
        int[] ids = m.idIndexes();
        int id = ids[0];
        Attribute idAttr = m.attributes()[id];
        if (ids.length == 1 && m.get(entity, id) == null) {
            if (!idAttr.generated) {
                throw new PersistenceException("Null id: " + m.type().getName());
            }
            if (idAttr.kind != Attribute.TEXT) {
                throw new PersistenceException("A document store generates text ids only: "
                        + m.type().getName() + "." + idAttr.field);
            }
            entity = assign(m, entity, id, store.newId());
        }
        int vi = m.versionIndex();
        if (vi >= 0 && m.get(entity, vi) == null) {
            entity = assign(m, entity, vi, Long.valueOf(0));
        }
        m.lifecycle(entity, PRE_PERSIST);
        if (!store.insert(m.table(), keyFields(m), toDocument(m, entity))) {
            throw new PersistenceException("Duplicate id: " + m.type().getName());
        }
        m.lifecycle(entity, POST_PERSIST);
        written(m, entity, CommitObserver.PERSISTED);
        return entity;
    }

    /// Replaces the stored document of `entity`. With a `@Version` field the
    /// write succeeds only if the stored version is the entity's, and the
    /// version is incremented; otherwise [OptimisticLockException] is thrown.
    public <T> T merge(T entity) throws IOException {
        @SuppressWarnings("unchecked")
        EntityModel<T> m = model((Class<T>) entity.getClass());
        m.lifecycle(entity, PRE_UPDATE);
        int vi = m.versionIndex();
        String versionColumn = null;
        Object expected = null;
        Long next = null;
        Map doc = toDocument(m, entity);
        if (vi >= 0) {
            versionColumn = column(m, vi);
            expected = Values.storage(m.get(entity, vi));
            next = Long.valueOf(expected instanceof Number ? ((Number) expected).longValue() + 1 : 1);
            // The entity keeps its version until the write succeeds, so a stale
            // entity stays stale after a failed merge.
            doc.put(versionColumn, next);
        }
        boolean ok = store.replace(m.table(), keyFields(m), doc, versionColumn, expected);
        if (!ok) {
            if (vi >= 0) {
                throw new OptimisticLockException("Stale " + m.type().getName());
            }
            throw new PersistenceException("No such " + m.type().getName());
        }
        if (next != null) {
            entity = assign(m, entity, vi, next);
        }
        m.lifecycle(entity, POST_UPDATE);
        written(m, entity, CommitObserver.UPDATED);
        return entity;
    }

    /// The entity with this id, or null.
    public <T> T find(Class<T> type, Object id) throws IOException {
        EntityModel<T> m = model(type);
        Map doc = store.get(m.table(), keyFields(m), key(m, id));
        return doc == null ? null : fromDocument(m, doc);
    }

    /// Deletes `entity`, honouring its `@Version` like [#merge(Object)].
    /// Returns false if there was nothing to delete and the entity has no version.
    public <T> boolean remove(T entity) throws IOException {
        @SuppressWarnings("unchecked")
        EntityModel<T> m = model((Class<T>) entity.getClass());
        m.lifecycle(entity, PRE_REMOVE);
        int vi = m.versionIndex();
        boolean ok = store.delete(m.table(), keyFields(m), key(m, m.identifier(entity)),
                vi < 0 ? null : column(m, vi), vi < 0 ? null : Values.storage(m.get(entity, vi)));
        if (!ok) {
            if (vi >= 0) {
                throw new OptimisticLockException("Stale " + m.type().getName());
            }
            return false;
        }
        m.lifecycle(entity, POST_REMOVE);
        written(m, entity, CommitObserver.REMOVED);
        return true;
    }

    /// A query over the entities of `type`.
    public <T> DocumentQuery<T> query(Class<T> type) {
        return new DocumentQuery<T>(this, model(type));
    }

    <T> List run(EntityModel<T> m, List criteria, String sort, boolean asc, int offset,
            int limit) throws IOException {
        return store.find(m.table(), keyFields(m), criteria, sort, asc, offset, limit);
    }

    <T> long count(EntityModel<T> m, List criteria) throws IOException {
        return store.count(m.table(), keyFields(m), criteria);
    }

    @Override
    public void close() {
        store.close();
    }
}
