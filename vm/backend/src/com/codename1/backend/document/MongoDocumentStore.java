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
    public void ensureCollection(String collection, String idField) {
        // MongoDB creates a collection on its first write, keyed by _id.
    }

    private static Map toMongo(Map doc, String idField) {
        Map out = new LinkedHashMap();
        out.put("_id", doc.get(idField));
        Iterator it = doc.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (!idField.equals(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static Map fromMongo(Map doc, String idField) {
        if (doc == null) {
            return null;
        }
        Map out = new LinkedHashMap();
        Iterator it = doc.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            out.put("_id".equals(e.getKey()) ? idField : e.getKey(), e.getValue());
        }
        return out;
    }

    private static Map idFilter(Object id) {
        Map f = new LinkedHashMap();
        f.put("_id", id);
        return f;
    }

    @Override
    public boolean insert(String collection, String idField, Map document) throws IOException {
        try {
            c(collection).insertOne(toMongo(document, idField));
            return true;
        } catch (MongoException err) {
            if (err.isDuplicateKey()) {
                return false;
            }
            throw err;
        }
    }

    @Override
    public boolean replace(String collection, String idField, Map document, String versionField,
            Object expectedVersion) throws IOException {
        Map filter = idFilter(document.get(idField));
        if (versionField != null) {
            filter.put(versionField, expectedVersion);
        }
        Map body = toMongo(document, idField);
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
    public Map get(String collection, String idField, Object id) throws IOException {
        return fromMongo(c(collection).findOne(idFilter(id)), idField);
    }

    @Override
    public boolean delete(String collection, String idField, Object id, String versionField,
            Object expectedVersion) throws IOException {
        Map filter = idFilter(id);
        if (versionField != null) {
            filter.put(versionField, expectedVersion);
        }
        return c(collection).deleteOne(filter) > 0;
    }

    /// The MongoDB filter for a list of criteria.
    static Map filter(List criteria, String idField) {
        List clauses = new ArrayList();
        for (Object eachCriteriaItem : criteria) {
            Criterion cr = (Criterion) eachCriteriaItem;
            String field = idField.equals(cr.getField()) ? "_id" : cr.getField();
            Map inner = new LinkedHashMap();
            switch (cr.getOp()) {
                case Criterion.EQ:
                    inner.put("$eq", cr.getValue());
                    break;
                case Criterion.NE:
                    inner.put("$ne", cr.getValue());
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
                    for (Object eachValue : values) {
                        l.add(eachValue);
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
        return sb.append('$').toString();
    }

    @Override
    public List find(String collection, String idField, List criteria, String sortField,
            boolean ascending, int offset, int limit) throws IOException {
        if (limit == 0) {
            return new ArrayList();
        }
        Map sort = null;
        if (sortField != null) {
            sort = new LinkedHashMap();
            sort.put(idField.equals(sortField) ? "_id" : sortField,
                    Integer.valueOf(ascending ? 1 : -1));
        }
        List docs = c(collection).find(filter(criteria, idField), sort, null, offset,
                limit < 0 ? 0 : limit);
        List out = new ArrayList(docs.size());
        for (Object eachDoc : docs) {
            out.add(fromMongo((Map) eachDoc, idField));
        }
        return out;
    }

    @Override
    public long count(String collection, String idField, List criteria) throws IOException {
        return c(collection).countDocuments(filter(criteria, idField));
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
