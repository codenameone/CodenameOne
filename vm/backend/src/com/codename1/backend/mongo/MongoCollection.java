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
package com.codename1.backend.mongo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One collection: the CRUD operations, as MongoDB's own commands.
///
/// Documents, filters and updates are `Map`s; [Filters] and [Updates] build the
/// latter two. `insertOne` adds an `_id` of a new [ObjectId] to a document that
/// has none, as the official drivers do, so the caller can read it back.
public final class MongoCollection {
    private final MongoClient client;
    private final String database;
    private final String name;

    MongoCollection(MongoClient client, String database, String name) {
        this.client = client;
        this.database = database;
        this.name = name;
    }

    /// The collection's name.
    public String getName() {
        return name;
    }

    private Map command(Map cmd) throws IOException {
        Map reply = client.runCommand(database, cmd);
        Object writeErrors = reply.get("writeErrors");
        if (writeErrors instanceof List && !((List) writeErrors).isEmpty()) {
            Object first = ((List) writeErrors).get(0);
            if (first instanceof Map) {
                Map e = (Map) first;
                Object code = e.get("code");
                throw new MongoException(code instanceof Number ? ((Number) code).intValue() : 0,
                        null, String.valueOf(e.get("errmsg")));
            }
        }
        Object concern = reply.get("writeConcernError");
        if (concern instanceof Map) {
            throw MongoException.from((Map) concern);
        }
        return reply;
    }

    /// Inserts one document and returns its `_id`.
    public Object insertOne(Map document) throws IOException {
        List docs = new ArrayList();
        docs.add(document);
        insertMany(docs);
        return document.get("_id");
    }

    /// Inserts documents in order, stopping at the first that fails.
    public void insertMany(List documents) throws IOException {
        for (Object eachDocument : documents) {
            Map d = (Map) eachDocument;
            if (!d.containsKey("_id")) {
                d.put("_id", ObjectId.generate());
            }
        }
        Map cmd = new LinkedHashMap();
        cmd.put("insert", name);
        cmd.put("documents", documents);
        command(cmd);
    }

    /// Every document `filter` matches.
    public List find(Map filter) throws IOException {
        return find(filter, null, null, 0, 0);
    }

    /// The first document `filter` matches, or null.
    public Map findOne(Map filter) throws IOException {
        List l = find(filter, null, null, 0, 1);
        return l.isEmpty() ? null : (Map) l.get(0);
    }

    /// The documents `filter` matches, ordered by `sort` (null for none), with
    /// only the fields in `projection` (null for all), skipping `skip` and
    /// returning at most `limit` (zero for no limit).
    public List find(Map filter, Map sort, Map projection, int skip, int limit)
            throws IOException {
        Map cmd = new LinkedHashMap();
        cmd.put("find", name);
        cmd.put("filter", filter == null ? new LinkedHashMap() : filter);
        if (sort != null) {
            cmd.put("sort", sort);
        }
        if (projection != null) {
            cmd.put("projection", projection);
        }
        if (skip > 0) {
            cmd.put("skip", Integer.valueOf(skip));
        }
        if (limit > 0) {
            cmd.put("limit", Integer.valueOf(limit));
            cmd.put("singleBatch", Boolean.valueOf(limit == 1));
        }
        return drain(command(cmd), limit);
    }

    /// The results of an aggregation pipeline, a list of stage documents.
    public List aggregate(List pipeline) throws IOException {
        Map cmd = new LinkedHashMap();
        cmd.put("aggregate", name);
        cmd.put("pipeline", pipeline);
        cmd.put("cursor", new LinkedHashMap());
        return drain(command(cmd), 0);
    }

    /// Reads a cursor reply and every `getMore` batch after it.
    private List drain(Map reply, int limit) throws IOException {
        List out = new ArrayList();
        Map cursor = (Map) reply.get("cursor");
        String batchField = "firstBatch";
        while (cursor != null) {
            Object batch = cursor.get(batchField);
            if (batch instanceof List) {
                out.addAll((List) batch);
            }
            Object id = cursor.get("id");
            long cursorId = id instanceof Number ? ((Number) id).longValue() : 0L;
            if (cursorId == 0L) {
                break;
            }
            if (limit > 0 && out.size() >= limit) {
                Map kill = new LinkedHashMap();
                kill.put("killCursors", name);
                List ids = new ArrayList();
                ids.add(Long.valueOf(cursorId));
                kill.put("cursors", ids);
                client.runCommand(database, kill);
                break;
            }
            Map more = new LinkedHashMap();
            more.put("getMore", Long.valueOf(cursorId));
            more.put("collection", name);
            cursor = (Map) client.runCommand(database, more).get("cursor");
            batchField = "nextBatch";
        }
        while (limit > 0 && out.size() > limit) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    /// How many documents `filter` matches.
    public long countDocuments(Map filter) throws IOException {
        List pipeline = new ArrayList();
        Map match = new LinkedHashMap();
        match.put("$match", filter == null ? new LinkedHashMap() : filter);
        pipeline.add(match);
        Map group = new LinkedHashMap();
        Map g = new LinkedHashMap();
        g.put("_id", Integer.valueOf(1));
        Map sum = new LinkedHashMap();
        sum.put("$sum", Integer.valueOf(1));
        g.put("n", sum);
        group.put("$group", g);
        pipeline.add(group);
        List r = aggregate(pipeline);
        if (r.isEmpty()) {
            return 0;
        }
        Object n = ((Map) r.get(0)).get("n");
        return n instanceof Number ? ((Number) n).longValue() : 0;
    }

    /// Applies `update` to the first matching document; returns how many matched.
    public long updateOne(Map filter, Map update) throws IOException {
        return update(filter, update, false, false);
    }

    /// Applies `update` to every matching document; returns how many matched.
    public long updateMany(Map filter, Map update) throws IOException {
        return update(filter, update, true, false);
    }

    /// Replaces the first matching document, inserting it when none matches.
    public void replaceOne(Map filter, Map replacement, boolean upsert) throws IOException {
        update(filter, replacement, false, upsert);
    }

    /// Applies `update` to the first matching document, or inserts one when none
    /// matches.
    public void upsert(Map filter, Map update) throws IOException {
        update(filter, update, false, true);
    }

    private long update(Map filter, Map update, boolean multi, boolean upsert)
            throws IOException {
        Map u = new LinkedHashMap();
        u.put("q", filter == null ? new LinkedHashMap() : filter);
        u.put("u", update);
        u.put("multi", Boolean.valueOf(multi));
        u.put("upsert", Boolean.valueOf(upsert));
        List updates = new ArrayList();
        updates.add(u);
        Map cmd = new LinkedHashMap();
        cmd.put("update", name);
        cmd.put("updates", updates);
        Object n = command(cmd).get("n");
        return n instanceof Number ? ((Number) n).longValue() : 0;
    }

    /// Deletes the first matching document; returns how many were deleted.
    public long deleteOne(Map filter) throws IOException {
        return delete(filter, 1);
    }

    /// Deletes every matching document; returns how many were deleted.
    public long deleteMany(Map filter) throws IOException {
        return delete(filter, 0);
    }

    private long delete(Map filter, int limit) throws IOException {
        Map d = new LinkedHashMap();
        d.put("q", filter == null ? new LinkedHashMap() : filter);
        d.put("limit", Integer.valueOf(limit));
        List deletes = new ArrayList();
        deletes.add(d);
        Map cmd = new LinkedHashMap();
        cmd.put("delete", name);
        cmd.put("deletes", deletes);
        Object n = command(cmd).get("n");
        return n instanceof Number ? ((Number) n).longValue() : 0;
    }

    /// Creates an index over `keys` (for example `{"email": 1}`) unless one
    /// exists; `unique` makes a second document with the same key a
    /// [MongoException#isDuplicateKey()] failure. Returns the index name.
    public String createIndex(Map keys, boolean unique) throws IOException {
        StringBuilder n = new StringBuilder();
        java.util.Iterator it = keys.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (n.length() > 0) {
                n.append('_');
            }
            n.append(e.getKey()).append('_').append(e.getValue());
        }
        Map index = new LinkedHashMap();
        index.put("key", keys);
        index.put("name", n.toString());
        if (unique) {
            index.put("unique", Boolean.TRUE);
        }
        List indexes = new ArrayList();
        indexes.add(index);
        Map cmd = new LinkedHashMap();
        cmd.put("createIndexes", name);
        cmd.put("indexes", indexes);
        command(cmd);
        return n.toString();
    }

    /// Drops the collection; a missing one is not an error.
    public void drop() throws IOException {
        Map cmd = new LinkedHashMap();
        cmd.put("drop", name);
        try {
            client.runCommand(database, cmd);
        } catch (MongoException err) {
            if (err.getCode() != 26) { // NamespaceNotFound
                throw err;
            }
        }
    }
}
