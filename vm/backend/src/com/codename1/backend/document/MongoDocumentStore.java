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

import com.codename1.backend.mongo.MongoClient;
import com.codename1.backend.mongo.MongoCollection;
import com.codename1.backend.mongo.MongoDatabase;
import com.codename1.backend.mongo.MongoException;
import com.codename1.backend.mongo.ObjectId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// [DocumentStore] over MongoDB: the identifier is stored as `_id`, criteria
/// become a query filter, and sorting and paging run on the server.
public final class MongoDocumentStore implements DocumentStore {
    private final MongoClient client;
    private final MongoDatabase database;
    private final boolean ownsClient;

    /// A store over `database` of `client` (null for the database the connection
    /// string names); `ownsClient` closes the client with it.
    public MongoDocumentStore(MongoClient client, MongoDatabase database, boolean ownsClient) {
        this.client = client;
        this.database = database == null ? client.database() : database;
        this.ownsClient = ownsClient;
    }

    private MongoCollection c(String collection) {
        return database.collection(collection);
    }

    @Override
    public void ensureCollection(String collection, String[] keyFields, boolean[] numericKeys) {
        // MongoDB creates a collection on its first write, keyed by _id.
    }

    private static int keyIndex(String[] keyFields, Object field) {
        for (int i = 0; i < keyFields.length; i++) {
            if (keyFields[i].equals(field)) {
                return i;
            }
        }
        return -1;
    }

    /// Where a field lives in the stored document: a single key is `_id` itself,
    /// a two-column key is a sub-document of `_id`.
    static String path(String[] keyFields, String field) {
        int k = keyIndex(keyFields, field);
        if (k < 0) {
            return field;
        }
        return keyFields.length == 1 ? "_id" : "_id." + field;
    }

    /// The `_id` of a key. A compound `_id` compares field by field in order, so
    /// it is always built in the order of `keyFields`.
    private static Object id(String[] keyFields, Object[] key) {
        if (keyFields.length == 1) {
            return key[0];
        }
        Map id = new LinkedHashMap();
        for (int i = 0; i < keyFields.length; i++) {
            id.put(keyFields[i], key[i]);
        }
        return id;
    }

    private static Object[] keyOf(Map doc, String[] keyFields) {
        Object[] key = new Object[keyFields.length];
        for (int i = 0; i < keyFields.length; i++) {
            key[i] = doc.get(keyFields[i]);
        }
        return key;
    }

    private static Map toMongo(Map doc, String[] keyFields) {
        Map out = new LinkedHashMap();
        out.put("_id", id(keyFields, keyOf(doc, keyFields)));
        Iterator it = doc.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (keyIndex(keyFields, e.getKey()) < 0) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static Map fromMongo(Map doc, String[] keyFields) {
        if (doc == null) {
            return null;
        }
        Map out = new LinkedHashMap();
        Iterator it = doc.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (!"_id".equals(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            } else if (keyFields.length == 1) {
                out.put(keyFields[0], e.getValue());
            } else if (e.getValue() instanceof Map) {
                Map id = (Map) e.getValue();
                for (String eachKeyField : keyFields) {
                    out.put(eachKeyField, id.get(eachKeyField));
                }
            }
        }
        return out;
    }

    private static Map idFilter(String[] keyFields, Object[] key) {
        Map f = new LinkedHashMap();
        f.put("_id", id(keyFields, key));
        return f;
    }

    @Override
    public boolean insert(String collection, String[] keyFields, Map document) throws IOException {
        try {
            c(collection).insertOne(toMongo(document, keyFields));
            return true;
        } catch (MongoException err) {
            if (err.isDuplicateKey()) {
                return false;
            }
            throw err;
        }
    }

    @Override
    public boolean replace(String collection, String[] keyFields, Map document,
            String versionField, Object expectedVersion) throws IOException {
        Map filter = idFilter(keyFields, keyOf(document, keyFields));
        if (versionField != null) {
            filter.put(versionField, expectedVersion);
        }
        Map body = toMongo(document, keyFields);
        body.remove("_id");
        Map cmd = new LinkedHashMap();
        Map u = new LinkedHashMap();
        u.put("q", filter);
        u.put("u", body);
        List updates = new ArrayList();
        updates.add(u);
        cmd.put("update", collection);
        cmd.put("updates", updates);
        Object n = database.runCommand(cmd).get("n");
        return n instanceof Number && ((Number) n).longValue() > 0;
    }

    @Override
    public Map get(String collection, String[] keyFields, Object[] key) throws IOException {
        return fromMongo(c(collection).findOne(idFilter(keyFields, key)), keyFields);
    }

    @Override
    public boolean delete(String collection, String[] keyFields, Object[] key,
            String versionField, Object expectedVersion) throws IOException {
        Map filter = idFilter(keyFields, key);
        if (versionField != null) {
            filter.put(versionField, expectedVersion);
        }
        return c(collection).deleteOne(filter) > 0;
    }

    /// The MongoDB filter for a list of criteria.
    static Map filter(List criteria, String[] keyFields) {
        List clauses = new ArrayList();
        for (Object eachCriteriaItem : criteria) {
            Criterion cr = (Criterion) eachCriteriaItem;
            String field = path(keyFields, cr.getField());
            Map inner = new LinkedHashMap();
            switch (cr.getOp()) {
                case Criterion.EQ:
                    inner.put("$eq", cr.getValue());
                    break;
                case Criterion.NE:
                    // $ne alone also matches a missing or null field; SQL's <>
                    // and the in-memory matcher do not, so null is excluded too.
                    inner.put("$nin", java.util.Arrays.asList(new Object[] {cr.getValue(), null}));
                    break;
                case Criterion.GT:
                    inner.put("$gt", cr.getValue());
                    break;
                case Criterion.GE:
                    inner.put("$gte", cr.getValue());
                    break;
                case Criterion.LT:
                    inner.put("$lt", cr.getValue());
                    break;
                case Criterion.LE:
                    inner.put("$lte", cr.getValue());
                    break;
                case Criterion.LIKE:
                    inner.put("$regex", likeToRegex(String.valueOf(cr.getValue())));
                    inner.put("$options", "s");
                    break;
                default: {
                    Object[] values = (Object[]) cr.getValue();
                    List l = new ArrayList();
                    // A null element would make $in match a missing field; like
                    // SQL's IN (and the in-memory matcher), null matches nothing.
                    for (Object eachValue : values) {
                        if (eachValue != null) {
                            l.add(eachValue);
                        }
                    }
                    inner.put("$in", l);
                    break;
                }
            }
            Map clause = new LinkedHashMap();
            clause.put(field, inner);
            clauses.add(clause);
        }
        if (clauses.isEmpty()) {
            return new LinkedHashMap();
        }
        Map and = new LinkedHashMap();
        and.put("$and", clauses);
        return and;
    }

    /// A SQL LIKE pattern as an anchored regular expression, every other
    /// character matched literally.
    static String likeToRegex(String pattern) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '%') {
                sb.append(".*");
            } else if (c == '_') {
                sb.append('.');
            } else if ("\\^$.|?*+()[]{}".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        // \\z, not $: PCRE's $ also matches before a final newline, so
        // 'abc' would match "abc\n" here and nowhere else.
        return sb.append("\\z").toString();
    }

    @Override
    public List find(String collection, String[] keyFields, List criteria, String sortField,
            boolean ascending, int offset, int limit) throws IOException {
        if (limit == 0) {
            return new ArrayList();
        }
        Map sort = null;
        if (sortField != null) {
            sort = new LinkedHashMap();
            sort.put(path(keyFields, sortField), Integer.valueOf(ascending ? 1 : -1));
        }
        List docs = c(collection).find(filter(criteria, keyFields), sort, null, offset,
                limit < 0 ? 0 : limit);
        List out = new ArrayList(docs.size());
        for (Object eachDoc : docs) {
            out.add(fromMongo((Map) eachDoc, keyFields));
        }
        return out;
    }

    @Override
    public long count(String collection, String[] keyFields, List criteria) throws IOException {
        return c(collection).countDocuments(filter(criteria, keyFields));
    }

    @Override
    public String newId() {
        return ObjectId.generate().toString();
    }

    @Override
    public void close() {
        if (ownsClient) {
            client.close();
        }
    }
}
