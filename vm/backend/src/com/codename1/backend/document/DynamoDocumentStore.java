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

import com.codename1.backend.Crypto;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.dynamo.DynamoDb;
import com.codename1.backend.dynamo.DynamoDbException;
import com.codename1.backend.dynamo.Items;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// [DocumentStore] over DynamoDB: one table per collection, named
/// `tablePrefix + collection`, keyed by the partition key and, for a two-column
/// key, the sort key.
///
/// A lookup by the whole key is a `GetItem`. A query with an equality on the
/// partition key is a `Query` over that partition, with at most one comparison
/// on the sort key in the key condition and the other comparisons as a filter.
/// Anything else is a `Scan` with the comparisons as a filter expression.
/// `LIKE`, ordering and paging are applied in memory afterwards. Dates are
/// stored as epoch milliseconds, the form DynamoDB's TTL and numeric
/// comparisons expect.
public final class DynamoDocumentStore implements DocumentStore {
    private static final String[] OPS = {"=", "<>", ">", ">=", "<", "<="};
    private final DynamoDb db;
    private final String tablePrefix;

    /// A store over `db`, with every table name prefixed by `tablePrefix`
    /// (which may be empty) so environments can share an account.
    public DynamoDocumentStore(DynamoDb db, String tablePrefix) {
        this.db = db;
        this.tablePrefix = tablePrefix == null ? "" : tablePrefix;
    }

    private String table(String collection) {
        return tablePrefix + collection;
    }

    @Override
    public void ensureCollection(String collection, String[] keyFields, boolean[] numericKeys)
            throws IOException {
        boolean two = keyFields.length > 1;
        db.createTable(table(collection), keyFields[0], numericKeys[0],
                two ? keyFields[1] : null, two && numericKeys[1]);
    }

    private static Object plain(Object v) {
        return v instanceof Date ? Long.valueOf(((Date) v).getTime()) : v;
    }

    private static Map plain(Map doc) {
        Map out = new LinkedHashMap();
        Iterator it = doc.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            out.put(e.getKey(), plain(e.getValue()));
        }
        return out;
    }

    private static Map key(String[] keyFields, Object[] key) {
        Map k = new LinkedHashMap();
        for (int i = 0; i < keyFields.length; i++) {
            k.put(keyFields[i], plain(key[i]));
        }
        return k;
    }

    @Override
    public boolean insert(String collection, String[] keyFields, Map document)
            throws IOException {
        Map names = new LinkedHashMap();
        names.put("#id", keyFields[0]);
        try {
            // An item exists exactly when its partition key attribute does.
            putWithNames(collection, plain(document), "attribute_not_exists(#id)", names, null);
            return true;
        } catch (DynamoDbException err) {
            if (err.isConditionalCheckFailed()) {
                return false;
            }
            throw err;
        }
    }

    private void putWithNames(String collection, Map item, String condition, Map names,
            Map values) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table(collection));
        req.put("Item", Items.toAttributes(item));
        req.put("ConditionExpression", condition);
        req.put("ExpressionAttributeNames", names);
        if (values != null) {
            req.put("ExpressionAttributeValues", Items.toAttributes(values));
        }
        db.call("PutItem", req);
    }

    @Override
    public boolean replace(String collection, String[] keyFields, Map document,
            String versionField, Object expectedVersion) throws IOException {
        Map names = new LinkedHashMap();
        names.put("#id", keyFields[0]);
        String condition = "attribute_exists(#id)";
        Map values = null;
        if (versionField != null) {
            names.put("#v", versionField);
            values = new LinkedHashMap();
            values.put(":v", plain(expectedVersion));
            condition = condition + " AND #v = :v";
        }
        try {
            putWithNames(collection, plain(document), condition, names, values);
            return true;
        } catch (DynamoDbException err) {
            if (err.isConditionalCheckFailed()) {
                return false;
            }
            throw err;
        }
    }

    @Override
    public Map get(String collection, String[] keyFields, Object[] key) throws IOException {
        return db.getItem(table(collection), key(keyFields, key));
    }

    @Override
    public boolean delete(String collection, String[] keyFields, Object[] key,
            String versionField, Object expectedVersion) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table(collection));
        req.put("Key", Items.toAttributes(key(keyFields, key)));
        Map names = new LinkedHashMap();
        names.put("#id", keyFields[0]);
        String condition = "attribute_exists(#id)";
        if (versionField != null) {
            names.put("#v", versionField);
            Map values = new LinkedHashMap();
            values.put(":v", plain(expectedVersion));
            req.put("ExpressionAttributeValues", Items.toAttributes(values));
            condition = condition + " AND #v = :v";
        }
        req.put("ConditionExpression", condition);
        req.put("ExpressionAttributeNames", names);
        try {
            db.call("DeleteItem", req);
            return true;
        } catch (DynamoDbException err) {
            if (err.isConditionalCheckFailed()) {
                return false;
            }
            throw err;
        }
    }

    /// The first criterion on `field` with operator `op`, or null; a null value
    /// never qualifies, since a key attribute is never null.
    private static Criterion find(List criteria, String field, boolean equalityOnly) {
        for (Object eachCriterion : criteria) {
            Criterion c = (Criterion) eachCriterion;
            if (!field.equals(c.getField()) || c.getValue() == null) {
                continue;
            }
            int op = c.getOp();
            if (op == Criterion.EQ || !equalityOnly && op >= Criterion.GT && op <= Criterion.LE) {
                return c;
            }
        }
        return null;
    }

    private static boolean isKey(String[] keyFields, String field) {
        for (String eachKeyField : keyFields) {
            if (eachKeyField.equals(field)) {
                return true;
            }
        }
        return false;
    }

    private List matching(String collection, String[] keyFields, List criteria)
            throws IOException {
        Criterion partition = find(criteria, keyFields[0], true);
        // A sort key condition exists only within a partition; without an
        // equality on the partition key the comparison is an ordinary filter.
        Criterion sort = partition != null && keyFields.length > 1
                ? find(criteria, keyFields[1], false) : null;
        if (partition != null && (keyFields.length == 1
                || sort != null && sort.getOp() == Criterion.EQ)) {
            // The whole key is known: a point read, not a scan.
            Object[] k = keyFields.length == 1 ? new Object[] {partition.getValue()}
                    : new Object[] {partition.getValue(), sort.getValue()};
            Map item = get(collection, keyFields, k);
            List out = new ArrayList();
            if (item != null && all(item, criteria)) {
                out.add(item);
            }
            return out;
        }
        StringBuilder filter = new StringBuilder();
        Map names = new LinkedHashMap();
        Map values = new LinkedHashMap();
        List inMemory = new ArrayList();
        String keyCondition = null;
        if (partition != null) {
            names.put("#k0", keyFields[0]);
            values.put(":k0", plain(partition.getValue()));
            keyCondition = "#k0 = :k0";
            if (sort != null) {
                names.put("#k1", keyFields[1]);
                values.put(":k1", plain(sort.getValue()));
                keyCondition = keyCondition + " AND #k1 " + OPS[sort.getOp()] + " :k1";
            }
        }
        for (int i = 0; i < criteria.size(); i++) {
            Criterion c = (Criterion) criteria.get(i); // NOPMD ForLoopCanBeForeach - i names the placeholders
            if (c == partition || c == sort) { // NOPMD CompareObjectsWithEquals - the very criteria used in the key condition
                continue;
            }
            // A Query refuses a key attribute in its filter expression, so any
            // further condition on one is answered in memory.
            String clause = keyCondition != null && isKey(keyFields, c.getField()) ? null
                    : clause(c, i, names, values);
            if (clause == null) {
                inMemory.add(c);
                continue;
            }
            if (filter.length() > 0) {
                filter.append(" AND ");
            }
            filter.append('(').append(clause).append(')');
        }
        String f = filter.length() == 0 ? null : filter.toString();
        List items = keyCondition != null
                ? db.query(table(collection), null, keyCondition, f, names, values, 0)
                : db.scan(table(collection), f, names, values, 0);
        if (inMemory.isEmpty()) {
            return items;
        }
        List out = new ArrayList();
        for (Object eachItem : items) {
            if (all((Map) eachItem, inMemory)) {
                out.add(eachItem);
            }
        }
        return out;
    }

    private static boolean all(Map item, List criteria) {
        Map plainItem = plain(item);
        for (Object eachCriteriaItem : criteria) {
            Criterion c = (Criterion) eachCriteriaItem;
            Object v = c.getValue();
            Criterion p = v instanceof Object[] ? plainIn(c) : new Criterion(c.getField(), c.getOp(),
                    plain(v));
            if (!p.matches(plainItem)) {
                return false;
            }
        }
        return true;
    }

    private static Criterion plainIn(Criterion c) {
        Object[] in = (Object[]) c.getValue();
        Object[] out = new Object[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = plain(in[i]);
        }
        return new Criterion(c.getField(), c.getOp(), out);
    }

    /// One criterion as a filter expression, or null to apply it in memory.
    private static String clause(Criterion c, int n, Map names, Map values) {
        String name = "#f" + n;
        String val = ":v" + n;
        Object v = c.getValue();
        switch (c.getOp()) {
            case Criterion.LIKE:
                return null;
            case Criterion.IN: {
                Object[] in = (Object[]) v;
                if (in.length == 0 || in.length > 100) {
                    return null;
                }
                names.put(name, c.getField());
                StringBuilder sb = new StringBuilder(name).append(" IN (");
                for (int i = 0; i < in.length; i++) {
                    if (in[i] == null) {
                        return null;
                    }
                    String vi = val + "_" + i;
                    values.put(vi, plain(in[i]));
                    sb.append(i == 0 ? "" : ", ").append(vi);
                }
                return sb.append(')').toString();
            }
            default:
                break;
        }
        names.put(name, c.getField());
        if (v == null) {
            if (c.getOp() != Criterion.EQ && c.getOp() != Criterion.NE) {
                // An ordering against null holds for nothing, as in SQL. DynamoDB
                // refuses an expression value no clause uses, so this one is
                // answered in memory instead.
                names.remove(name);
                return null;
            }
            values.put(":nullType", "NULL");
            if (c.getOp() == Criterion.EQ) {
                return "attribute_not_exists(" + name + ") OR attribute_type(" + name
                        + ", :nullType)";
            }
            if (c.getOp() == Criterion.NE) {
                return "attribute_exists(" + name + ") AND NOT attribute_type(" + name
                        + ", :nullType)";
            }
        }
        values.put(val, plain(v));
        return name + " " + OPS[c.getOp()] + " " + val;
    }

    @Override
    public List find(String collection, String[] keyFields, List criteria, String sortField,
            boolean ascending, int offset, int limit) throws IOException {
        List items = matching(collection, keyFields, criteria);
        if (sortField != null) {
            Collections.sort(items, new FieldOrder(sortField, ascending));
        }
        List out = new ArrayList();
        for (int i = Math.max(0, offset); i < items.size() && (limit < 0 || out.size() < limit);
                i++) {
            out.add(items.get(i));
        }
        return out;
    }

    private static final class FieldOrder implements Comparator {
        private final String field;
        private final boolean ascending;

        FieldOrder(String field, boolean ascending) {
            this.field = field;
            this.ascending = ascending;
        }

        @Override
        public int compare(Object a, Object b) {
            Object x = ((Map) a).get(field);
            Object y = ((Map) b).get(field);
            return ascending ? Criterion.compare(x, y) : Criterion.compare(y, x);
        }
    }

    @Override
    public long count(String collection, String[] keyFields, List criteria) throws IOException {
        return matching(collection, keyFields, criteria).size();
    }

    @Override
    public String newId() {
        try {
            return Aws.hex(Crypto.randomBytes(16));
        } catch (IOException err) {
            throw new IllegalStateException("No random source for a document id", err);
        }
    }

    @Override
    public void close() {
        // Every DynamoDB call is an independent HTTPS request.
    }
}
